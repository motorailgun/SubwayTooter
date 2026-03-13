package jp.juggler.subwaytooter.columnviewholder

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.text.SpannableStringBuilder
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.column.*
import jp.juggler.subwaytooter.compose.ColumnCallbacks
import jp.juggler.subwaytooter.compose.ColumnScreen
import jp.juggler.subwaytooter.compose.ColumnUiState
import jp.juggler.subwaytooter.compose.TimelineCallbacks
import jp.juggler.subwaytooter.compose.TimelineState
import jp.juggler.subwaytooter.compose.buildTimelineCallbacks
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.subwaytooter.streaming.StreamStatus
import jp.juggler.subwaytooter.table.daoAcctColor
import jp.juggler.subwaytooter.util.NetworkEmojiInvalidator
import jp.juggler.subwaytooter.util.ScrollPosition
import jp.juggler.subwaytooter.appendColorShadeIcon
import jp.juggler.subwaytooter.generateLayoutParamsEx
import jp.juggler.subwaytooter.streaming.getStreamingStatus
import jp.juggler.util.data.notZero
import jp.juggler.util.log.LogCategory
import jp.juggler.util.ui.attrColor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.google.android.material.R as MR

/**
 * Thin wrapper around a ComposeView that hosts ColumnScreen.
 *
 * Public fields that external code references are preserved:
 *   - viewRoot, column, pageIdx, scrollPosition, isColumnSettingShown,
 *     isPageDestroyed, lastAnnouncementShown, bindingBusy
 */
@SuppressLint("ClickableViewAccessibility")
class ColumnViewHolder(
    val activity: ActMain,
    parent: ViewGroup,
) {

    companion object {
        val log = LogCategory("ColumnViewHolder")


    }

    // ──────── Core state ────────
    var column: Column? = null
    var pageIdx: Int = 0

    // ──────── Compose state ────────
    var composeView: ComposeView? = null
    var timelineState: TimelineState? = null
    var lazyListState: LazyListState? = null
    val columnUiState = ColumnUiState()
    var columnCallbacks = ColumnCallbacks()
    var timelineCallbacks: TimelineCallbacks = buildTimelineCallbacks(activity)



    // ──────── Bitmap / background image state ────────
    var lastImageUri: String? = null
    var lastImageBitmap: Bitmap? = null
    var lastImageTask: Job? = null

    // ──────── Announcement state ────────
    var lastAnnouncementShown = 0L
    val extraInvalidatorList = ArrayList<NetworkEmojiInvalidator>()
    val emojiQueryInvalidatorList = ArrayList<NetworkEmojiInvalidator>()



    // ──────── Misc state ────────
    var bindingBusy: Boolean = false
    var bRefreshErrorWillShown = false

    // ──────── Cached theme colors ────────
    val colorOnSurface = activity.attrColor(MR.attr.colorOnSurface)
    val colorSurfaceContainerLow = activity.attrColor(MR.attr.colorSurfaceContainerLow)
    val colorSurfaceContainerHigh = activity.attrColor(MR.attr.colorSurfaceContainerHigh)

    // ──────── The actual view root ────────
    val viewRoot: View = createViewRoot(parent)

    // ──────── Derived properties ────────

    val scrollPosition: ScrollPosition
        get() = ScrollPosition(this)

    val isColumnSettingShown: Boolean
        get() = columnUiState.settingsVisible

    val isPageDestroyed: Boolean
        get() = column == null || activity.isFinishing

    // ──────── Runnables ────────

    private val procLoadByContentInvalidated: Runnable = Runnable {
        if (bindingBusy || isPageDestroyed) return@Runnable
        column?.startLoading(ColumnLoadReason.ContentInvalidated)
    }

    val procShowColumnHeader: Runnable = Runnable {
        val column = this.column
        if (column == null || column.isDispose.get()) return@Runnable

        val ac = daoAcctColor.load(column.accessInfo)

        columnUiState.columnContext = ac.nickname
        columnUiState.columnContextColorFg =
            ac.colorFg.notZero() ?: activity.attrColor(MR.attr.colorOnSurfaceVariant)
        columnUiState.columnContextColorBg = ac.colorBg
        columnUiState.columnContextPadLr = activity.acctPadLr

        columnUiState.columnName = column.getColumnName(false)

        columnUiState.closeButtonEnabled = !(column.dontClose)

        showAnnouncements(force = false)
    }

    val procRestoreScrollPosition = object : Runnable {
        override fun run() {
            activity.handler.removeCallbacks(this)

            if (isPageDestroyed) {
                log.d("restoreScrollPosition [%d], page is destroyed.")
                return
            }

            val column = this@ColumnViewHolder.column
            if (column == null) {
                log.d("restoreScrollPosition [$pageIdx], column==null")
                return
            }

            if (column.isDispose.get()) {
                log.d("restoreScrollPosition [$pageIdx], column is disposed")
                return
            }

            if (column.hasMultipleViewHolder()) {
                log.d("restoreScrollPosition [$pageIdx] ${column.getColumnName(true)}, column has multiple view holder. retry later.")
                activity.handler.postDelayed(this, 100L)
                return
            }

            val sp = column.scrollSave ?: run {
                log.d("restoreScrollPosition [$pageIdx] ${column.getColumnName(true)} , column has no saved scroll position.")
                return
            }

            column.scrollSave = null

            val lls = lazyListState
            if (lls != null) {
                log.d("restoreScrollPosition [$pageIdx] ${column.getColumnName(true)} , Compose restore ${sp.adapterIndex},${sp.offset}")
                activity.lifecycleScope.launch {
                    try {
                        lls.scrollToItem(sp.adapterIndex, sp.offset)
                    } catch (ex: Throwable) {
                        log.e(ex, "Compose scrollToItem failed.")
                    }
                }
            }
        }
    }

    val procShowColumnStatus: Runnable = Runnable {
        val column = this.column
        if (column == null || column.isDispose.get()) return@Runnable

        val sb = SpannableStringBuilder()
        try {
            val task = column.lastTask
            if (task != null) {
                sb.append(task.ctType.marker)
                sb.append(
                    when {
                        task.isCancelled -> "~"
                        task.ctClosed.get() -> "!"
                        task.ctStarted.get() -> ""
                        else -> "?"
                    }
                )
            }
            val streamStatus = column.getStreamingStatus()
            log.d(
                "procShowColumnStatus: streamStatus=$streamStatus, column=${column.accessInfo.acct}/${
                    column.getColumnName(true)
                }"
            )

            when (streamStatus) {
                StreamStatus.Missing, StreamStatus.Closed, StreamStatus.ClosedNoRetry -> Unit

                StreamStatus.Connecting, StreamStatus.Open -> {
                    sb.appendColorShadeIcon(activity, R.drawable.ic_pulse, "Streaming")
                    sb.append("?")
                }

                StreamStatus.Subscribed -> {
                    sb.appendColorShadeIcon(activity, R.drawable.ic_pulse, "Streaming")
                }
            }
        } finally {
            log.d("showColumnStatus $sb")
            columnUiState.columnStatus = sb
        }
    }

    // ──────── View creation ────────

    private fun createViewRoot(parent: ViewGroup): View {
        val composeView = ComposeView(activity).apply {
            setViewTreeLifecycleOwner(activity)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }
        this.composeView = composeView

        // Wrap in a FrameLayout to satisfy ViewPager's layout requirements
        val root = android.widget.FrameLayout(activity).apply {
            val lp = parent.generateLayoutParamsEx()
            if (lp != null) {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                lp.height = ViewGroup.LayoutParams.MATCH_PARENT
                if (lp is ViewGroup.MarginLayoutParams) {
                    lp.setMargins(0, 0, 0, 0)
                }
                layoutParams = lp
            }
            addView(
                composeView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        return root
    }

    // ──────── Helper ────────

    fun delayLoadByContentInvalidated() {
        activity.appState.saveColumnList()
        activity.handler.removeCallbacks(procLoadByContentInvalidated)
        activity.handler.postDelayed(procLoadByContentInvalidated, 666L)
    }

    /**
     * Set the ComposeView content to ColumnScreen.
     * Called from onPageCreate after state is initialized.
     */
    fun setComposeContent() {
        composeView?.setContent {
            ColumnScreen(
                activity = activity,
                column = column ?: return@setContent,
                uiState = columnUiState,
                timelineState = timelineState ?: return@setContent,
                timelineCallbacks = timelineCallbacks,
                columnCallbacks = columnCallbacks,
                bSimpleList = !(column?.isConversation ?: true) && PrefB.bpSimpleList.value,
                lazyListState = lazyListState ?: return@setContent,
            )
        }
    }
}
