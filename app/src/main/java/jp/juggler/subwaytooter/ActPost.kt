package jp.juggler.subwaytooter

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import jp.juggler.subwaytooter.compose.NetworkImage
import jp.juggler.subwaytooter.compose.StThemedContent
import com.google.android.flexbox.FlexboxLayout
import jp.juggler.subwaytooter.action.saveWindowSize
import jp.juggler.subwaytooter.actpost.ActPostStates
import jp.juggler.subwaytooter.actpost.FeaturedTagCache
import jp.juggler.subwaytooter.actpost.TextEditState
import jp.juggler.subwaytooter.actpost.addAttachment
import jp.juggler.subwaytooter.actpost.applyMushroomText
import jp.juggler.subwaytooter.actpost.onPickCustomThumbnailImpl
import jp.juggler.subwaytooter.actpost.onPostAttachmentCompleteImpl
import jp.juggler.subwaytooter.actpost.openAttachment
import jp.juggler.subwaytooter.actpost.openMushroom
import jp.juggler.subwaytooter.actpost.openEmojiPickerForContent
import jp.juggler.subwaytooter.actpost.openFeaturedTagList
import jp.juggler.subwaytooter.actpost.openVisibilityPicker
import jp.juggler.subwaytooter.actpost.performAccountChooser
import jp.juggler.subwaytooter.actpost.performAttachmentClick
import jp.juggler.subwaytooter.actpost.performMore
import jp.juggler.subwaytooter.actpost.performPost
import jp.juggler.subwaytooter.actpost.performSchedule
import jp.juggler.subwaytooter.actpost.rearrangeAttachments
import jp.juggler.subwaytooter.actpost.removeReply
import jp.juggler.subwaytooter.actpost.resetSchedule
import jp.juggler.subwaytooter.actpost.restoreState
import jp.juggler.subwaytooter.actpost.saveDraft
import jp.juggler.subwaytooter.actpost.saveState
import jp.juggler.subwaytooter.actpost.showContentWarningEnabled
import jp.juggler.subwaytooter.actpost.showMediaAttachment
import jp.juggler.subwaytooter.actpost.showMediaAttachmentProgress
import jp.juggler.subwaytooter.actpost.showPoll
import jp.juggler.subwaytooter.actpost.showQuotedRenote
import jp.juggler.subwaytooter.actpost.showReplyTo
import jp.juggler.subwaytooter.actpost.showVisibility
import jp.juggler.subwaytooter.actpost.updateText
import jp.juggler.subwaytooter.actpost.updateTextCount
import jp.juggler.subwaytooter.api.entity.TootScheduled
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.getVisibilityIconId
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.subwaytooter.pref.PrefI
import jp.juggler.subwaytooter.span.MyClickableSpan
import jp.juggler.subwaytooter.span.MyClickableSpanHandler
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.util.AttachmentPicker
import jp.juggler.subwaytooter.util.AttachmentUploader
import jp.juggler.subwaytooter.util.PostAttachment
import jp.juggler.subwaytooter.util.loadLanguageList
import jp.juggler.subwaytooter.util.openBrowser
import jp.juggler.subwaytooter.view.MyNetworkImageView
import jp.juggler.util.backPressed
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.coroutine.launchIO
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.data.UriAndType
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.string
import jp.juggler.util.ui.ActivityResultHandler
import jp.juggler.util.ui.attrColor
import jp.juggler.util.ui.dp
import jp.juggler.util.ui.isNotOk
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.lang.ref.WeakReference
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import com.google.android.material.R as MR

data class AttachmentSlotUi(
    val visible: Boolean = false,
    val previewUrl: String? = null,
    val fallbackIconRes: Int = R.drawable.ic_clip,
)

class ActPostViews(val activity: ActPost) {
    val etContentWarning get() = activity.etContentWarning
    val etContent get() = activity.etContent
    val etChoice1 get() = activity.etChoice1
    val etChoice2 get() = activity.etChoice2
    val etChoice3 get() = activity.etChoice3
    val etChoice4 get() = activity.etChoice4
    val etExpireDays get() = activity.etExpireDays
    val etExpireHours get() = activity.etExpireHours
    val etExpireMinutes get() = activity.etExpireMinutes
}

class ActPost : ComponentActivity(),
    PostAttachment.Callback,
    MyClickableSpanHandler {

    companion object {
        private val log = LogCategory("ActPost")

        var refActPost: WeakReference<ActPost>? = null

        const val EXTRA_POSTED_ACCT = "posted_acct"
        const val EXTRA_POSTED_STATUS_ID = "posted_status_id"
        const val EXTRA_POSTED_REPLY_ID = "posted_reply_id"
        const val EXTRA_POSTED_REDRAFT_ID = "posted_redraft_id"
        const val EXTRA_MULTI_WINDOW = "multiWindow"

        const val KEY_ACCOUNT_DB_ID = "account_db_id"
        const val KEY_REPLY_STATUS = "reply_status"
        const val KEY_REDRAFT_STATUS = "redraft_status"
        const val KEY_EDIT_STATUS = "edit_status"
        const val KEY_INITIAL_TEXT = "initial_text"
        const val KEY_SHARED_INTENT = "sent_intent"
        const val KEY_QUOTE = "quote"
        const val KEY_SCHEDULED_STATUS = "scheduled_status"

        const val STATE_ALL = "all"

        /////////////////////////////////////////////////

        fun createIntent(
            context: Context,
            accountDbId: Long,
            multiWindowMode: Boolean,
            // 再編集する投稿。アカウントと同一のタンスであること
            redraftStatus: TootStatus? = null,
            // 編集する投稿。アカウントと同一のタンスであること
            editStatus: TootStatus? = null,
            // 返信対象の投稿。同一タンス上に同期済みであること
            replyStatus: TootStatus? = null,
            //初期テキスト
            initialText: String? = null,
            // 外部アプリから共有されたインテント
            sharedIntent: Intent? = null,
            // 返信ではなく引用トゥートを作成する
            quote: Boolean = false,
            //(Mastodon) 予約投稿の編集
            scheduledStatus: TootScheduled? = null,
        ) = Intent(context, ActPost::class.java).apply {
            putExtra(EXTRA_MULTI_WINDOW, multiWindowMode)
            putExtra(KEY_ACCOUNT_DB_ID, accountDbId)
            initialText?.let { putExtra(KEY_INITIAL_TEXT, it) }
            redraftStatus?.let { putExtra(KEY_REDRAFT_STATUS, it.json.toString()) }
            editStatus?.let { putExtra(KEY_EDIT_STATUS, it.json.toString()) }
            replyStatus?.let {
                putExtra(KEY_REPLY_STATUS, it.json.toString())
                putExtra(KEY_QUOTE, quote)
            }
            sharedIntent?.let { putExtra(KEY_SHARED_INTENT, it) }
            scheduledStatus?.let { putExtra(KEY_SCHEDULED_STATUS, it.src.toString()) }
        }
    }

    // Text states
    val etContent = TextEditState()
    val etContentWarning = TextEditState()
    val etChoice1 = TextEditState()
    val etChoice2 = TextEditState()
    val etChoice3 = TextEditState()
    val etChoice4 = TextEditState()
    val etExpireDays = TextEditState("1")
    val etExpireHours = TextEditState()
    val etExpireMinutes = TextEditState()

    val views by lazy { ActPostViews(this) }
    val etChoices: List<TextEditState> get() = listOf(etChoice1, etChoice2, etChoice3, etChoice4)

    /** Which text field has focus: 0=content, 1=cw, 2-5=choice1-4. Used by Mushroom plugin. */
    var focusedEditField: Int = 0

    /** FocusRequester wired to etContent's BasicTextField. */
    val contentFocusRequester = FocusRequester()

    var charCountText by mutableStateOf("")
    var charCountColorArgb by mutableIntStateOf(0)
    var visibilityIconRes by mutableIntStateOf(R.drawable.ic_public)
    var scheduleText by mutableStateOf("")
    var pollTypeIndex by mutableIntStateOf(0)
    var pollMultipleChoiceChecked by mutableStateOf(false)
    var pollHideTotalsChecked by mutableStateOf(false)
    var nsfwChecked by mutableStateOf(false)
    var contentWarningChecked by mutableStateOf(false)
    var quoteChecked by mutableStateOf(false)
    var showQuoteOption by mutableStateOf(false)
    var showReplySection by mutableStateOf(false)
    var replyToText by mutableStateOf("")
    var accountButtonText by mutableStateOf("")
    var accountAvatarStaticUrl by mutableStateOf<String?>(null)
    var accountAvatarAnimatedUrl by mutableStateOf<String?>(null)
    var accountAvatarCorner by mutableStateOf(0f)
    var showAttachmentSection by mutableStateOf(false)
    var selectedLanguageIndex by mutableIntStateOf(0)
    var attachmentSlots by mutableStateOf(List(4) { AttachmentSlotUi() })
    var attachmentThumbCorner by mutableStateOf(0f)
    var showAttachmentRearrange by mutableStateOf(false)
    var attachmentProgressText by mutableStateOf("")

    lateinit var handler: Handler
    lateinit var appState: AppState
    lateinit var attachmentUploader: AttachmentUploader
    lateinit var attachmentPicker: AttachmentPicker

    var density: Float = 0f

    val languages by lazy {
        loadLanguageList()
    }

    private lateinit var progressChannel: Channel<Unit>

    ///////////////////////////////////////////////////

    // SavedAccount.acctAscii => FeaturedTagCache
    val featuredTagCache = ConcurrentHashMap<String, FeaturedTagCache>()

    // background job
    var jobFeaturedTag: WeakReference<Job>? = null
    var jobMaxCharCount: WeakReference<Job>? = null

    ///////////////////////////////////////////////////

    var states = ActPostStates()

    var accountList: List<SavedAccount> = emptyList()
    var account: SavedAccount? = null
    var attachmentList = ArrayList<PostAttachment>()
    var isPostComplete: Boolean = false
    var scheduledStatus: TootScheduled? = null

    /////////////////////////////////////////////////////////////////////

    val isMultiWindowPost: Boolean
        get() = intent.getBooleanExtra(EXTRA_MULTI_WINDOW, false)

    val arMushroom = ActivityResultHandler(log) { r ->
        if (r.isNotOk) return@ActivityResultHandler
        r.data?.string("replace_key")?.let { text ->
            when (states.mushroomInput) {
                0 -> applyMushroomText(views.etContent, text)
                1 -> applyMushroomText(views.etContentWarning, text)
                else -> for (i in 0..3) {
                    if (states.mushroomInput == i + 2) {
                        applyMushroomText(etChoices[i], text)
                    }
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backPressed {
            launchAndShowError {
                finish()
                // 戻るボタンを押したときとonPauseで2回保存することになるが、
                // 同じ内容はDB上は重複しないはず…
                saveDraft()
            }
        }
        if (isMultiWindowPost) ActMain.refActMain?.get()?.closeList?.add(WeakReference(this))
        appState = App1.getAppState(this)
        handler = appState.handler
        attachmentUploader = AttachmentUploader(this, handler)
        attachmentPicker = AttachmentPicker(this, object : AttachmentPicker.Callback {
            override suspend fun onPickAttachment(item: UriAndType) {
                addAttachment(item.uri, item.mimeType)
            }

            override suspend fun onPickCustomThumbnail(
                attachmentId: String?,
                src: UriAndType?,
            ) {
                src ?: return
                val pa = attachmentList.find { it.attachment?.id?.toString() == attachmentId }
                    ?: error("missing attachment for attachmentId=$attachmentId")
                onPickCustomThumbnailImpl(pa, src)
            }
        })

        density = resources.displayMetrics.density
        arMushroom.register(this)

        progressChannel = Channel(capacity = Channel.CONFLATED)

        charCountColorArgb = attrColor(android.R.attr.textColorPrimary)
        visibilityIconRes = (states.visibility ?: jp.juggler.subwaytooter.api.entity.TootVisibility.Public)
            .getVisibilityIconId(account?.isMisskey == true)
        scheduleText = getString(R.string.unspecified)
        pollTypeIndex = 0
        nsfwChecked = false
        contentWarningChecked = false
        quoteChecked = false
        showQuoteOption = false
        showReplySection = false
        replyToText = ""
        accountButtonText = getString(R.string.not_selected_2)
        accountAvatarStaticUrl = null
        accountAvatarAnimatedUrl = null
        accountAvatarCorner = calcIconRound(dp(32))
        showAttachmentSection = false
        selectedLanguageIndex = 0
        attachmentSlots = List(4) { AttachmentSlotUi() }
        attachmentThumbCorner = calcIconRound(dp(48))
        showAttachmentRearrange = false
        attachmentProgressText = ""

        App1.setActivityTheme(this)
        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.systemBars.union(WindowInsets.ime).asPaddingValues())
                    .onSizeChanged {
                        if (isMultiWindowPost) saveWindowSize()
                    }
            ) {
                if (PrefB.bpPostButtonBarTop.value) PostFooterBar()
                ActPostScreen(
                    activity = this@ActPost,
                    modifier = Modifier.weight(1f),
                )
                if (!PrefB.bpPostButtonBarTop.value) PostFooterBar()
            }
        }
        initUI()

        // 進捗表示チャネルの回収コルーチン
        launchAndShowError {
            try {
                while (true) {
                    progressChannel.receive()
                    showMediaAttachmentProgress()
                    delay(1000L)
                }
            } catch (ex: Throwable) {
                when (ex) {
                    is CancellationException, is ClosedReceiveChannelException -> Unit
                    else -> log.e(ex, "can't show media progress.")
                }
            }
        }

        // 初期化の続きをコルーチンでやる
        launchAndShowError {
            when (savedInstanceState) {
                null -> updateText(intent, saveDraft = false)
                else -> restoreState(savedInstanceState)
            }
        }
    }

    override fun onDestroy() {
        try {
            progressChannel.close()
        } catch (ex: Throwable) {
            log.e(ex, "progressChannel close failed.")
        }
        attachmentUploader.onActivityDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        showContentWarningEnabled()
        showMediaAttachment()
        showVisibility()
        updateTextCount()
        launchAndShowError { showReplyTo() }
        showPoll()
        showQuotedRenote()
    }

    override fun onResume() {
        super.onResume()
        refActPost = WeakReference(this)
    }

    override fun onPause() {
        super.onPause()
        if (!isPostComplete) launchMain {
            try {
                // 編集中にホーム画面を押したり他アプリに移動する場合は下書きを保存する
                // やや過剰な気がするが、自アプリに戻ってくるときにランチャーからアイコンタップされると
                // メイン画面より上にあるアクティビティはすべて消されてしまうので
                // このタイミングで保存するしかない
                saveDraft()
            } catch (ex: Throwable) {
                log.e(ex, "can't save draft.")
                showToast(ex, "can't save draft.")
            }
        }
    }

    override fun onKeyShortcut(keyCode: Int, event: KeyEvent?): Boolean {
        return when {
            super.onKeyShortcut(keyCode, event) -> true
            event?.isCtrlPressed == true && keyCode == KeyEvent.KEYCODE_T -> {
                performPost()
                true
            }

            else -> false
        }
    }

    override fun onMyClickableSpanClicked(viewClicked: View, span: MyClickableSpan) {
        openBrowser(span.linkInfo.url)
    }

    override fun onPostAttachmentProgress() {
        launchIO {
            try {
                progressChannel.send(Unit)
            } catch (ex: Throwable) {
                log.w(ex, "progressChannel send failed.")
            }
        }
    }

    override fun onPostAttachmentComplete(pa: PostAttachment) {
        onPostAttachmentCompleteImpl(pa)
    }

    @Composable
    private fun PostFooterBar() {
        val horizontalPadding = footerHorizontalPaddingDp()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = horizontalPadding),
        ) {
            FooterIconButton(R.drawable.ic_clip, getString(R.string.media_attachment)) {
                openAttachment()
            }
            FooterIconButton(
                visibilityIconRes,
                getString(R.string.visibility),
            ) {
                openVisibilityPicker()
            }
            FooterIconButton(R.drawable.ic_extension, getString(R.string.plugin_app_intro)) {
                launchAndShowError { openMushroom() }
            }
            FooterIconButton(R.drawable.ic_more, getString(R.string.more)) {
                performMore()
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = charCountText,
                color = Color(charCountColorArgb),
                modifier = Modifier
                    .padding(end = 4.dp)
                    .align(androidx.compose.ui.Alignment.CenterVertically),
            )
            FooterIconButton(R.drawable.ic_send, getString(R.string.toot)) {
                performPost()
            }
        }
    }

    @Composable
    private fun FooterIconButton(
        iconRes: Int,
        contentDescription: String,
        onClick: () -> Unit,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
            )
        }
    }

    internal fun footerHorizontalPaddingDp() = run {
        val dm = resources.displayMetrics
        val widthDp = dm.widthPixels / dm.density
        val basePx = if (
            widthDp >= 640f &&
            resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        ) {
            when (PrefI.ipJustifyWindowContentPortrait.value) {
                PrefI.JWCP_START,
                PrefI.JWCP_END,
                -> 0
                else -> kotlin.math.max(0, (dm.widthPixels - (0.5f + 460f * dm.density).toInt()) / 2)
            }
        } else {
            kotlin.math.max(0, (dm.widthPixels - (0.5f + 460f * dm.density).toInt()) / 2)
        }
        (basePx / dm.density).dp
    }

    fun initUI() {

        // Observe all text fields to update the character count
        launchMain {
            androidx.compose.runtime.snapshotFlow { views.etContent.fieldValue.text }
                .collectLatest { updateTextCount() }
        }
        launchMain {
            androidx.compose.runtime.snapshotFlow { views.etContentWarning.fieldValue.text }
                .collectLatest { updateTextCount() }
        }
        for (et in etChoices) {
            launchMain {
                androidx.compose.runtime.snapshotFlow { et.fieldValue.text }
                    .collectLatest { updateTextCount() }
            }
        }

    }
}
