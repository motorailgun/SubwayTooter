package jp.juggler.subwaytooter.itemviewholder

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.entity.TootNotification
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.column.Column
import jp.juggler.subwaytooter.pref.PrefI
import jp.juggler.util.data.*
import jp.juggler.util.log.*
import jp.juggler.util.ui.*
import org.jetbrains.anko.matchParent
import kotlin.math.max

internal class StatusButtonsPopup(
    private val activity: ActMain,
    column: Column,
    bSimpleList: Boolean,
    itemViewHolder: ItemViewHolder,
) {

    companion object {

        @Suppress("unused")
        private var log = LogCategory("StatusButtonsPopup")

        var lastPopupClose = 0L
    }

    private val ivTriangleTop: AppCompatImageView
    private val ivTriangleBottom: AppCompatImageView
    private val llBarPlaceHolder: LinearLayout
    private val root: LinearLayout
    private val buttonsForStatus: StatusButtons
    private var window: PopupWindow? = null

    init {
        val activity = this.activity
        ivTriangleTop = AppCompatImageView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { gravity = Gravity.CENTER_HORIZONTAL }
            setBackgroundResource(R.drawable.list_item_popup_triangle)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        llBarPlaceHolder = LinearLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                activity.dp(300),
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            setBackgroundResource(R.drawable.list_item_popup_bg)
            backgroundTintMode = android.graphics.PorterDuff.Mode.SRC_IN
            orientation = LinearLayout.HORIZONTAL
        }
        ivTriangleBottom = AppCompatImageView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { gravity = Gravity.CENTER_HORIZONTAL }
            setBackgroundResource(R.drawable.list_item_popup_triangle_bottom)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            visibility = View.GONE
        }
        root = LinearLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            orientation = LinearLayout.VERTICAL
            addView(ivTriangleTop)
            addView(llBarPlaceHolder)
            addView(ivTriangleBottom)
        }

        @SuppressLint("InflateParams")
        val statusButtonsViewHolder = StatusButtonsViewHolder(activity, matchParent, 0f)
        llBarPlaceHolder.addView(statusButtonsViewHolder.viewRoot)
        this.buttonsForStatus = StatusButtons(
            activity,
            column,
            bSimpleList,
            statusButtonsViewHolder,
            itemViewHolder
        )
    }

    fun dismiss() {
        val window = this.window
        if (window != null && window.isShowing) {
            window.dismiss()
        }
    }

    @SuppressLint("RtlHardcoded", "ClickableViewAccessibility")
    fun show(
        listView: RecyclerView,
        anchor: View,
        status: TootStatus,
        notification: TootNotification?,
    ) {
        val window = PopupWindow(activity)
        this.window = window

        window.width = WindowManager.LayoutParams.WRAP_CONTENT
        window.height = WindowManager.LayoutParams.WRAP_CONTENT
        window.contentView = root
        window.setBackgroundDrawable(ColorDrawable(0x00000000))
        window.isTouchable = true
        window.isOutsideTouchable = true
        window.setTouchInterceptor { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                // ポップアップの外側をタッチしたらポップアップを閉じる
                // また、そのタッチイベントがlistViewに影響しないようにする
                window.dismiss()
                lastPopupClose = SystemClock.elapsedRealtime()
                true
            } else {
                false
            }
        }

        buttonsForStatus.bind(status, notification)
        buttonsForStatus.closeWindow = window

        val bgColor = PrefI.ipPopupBgColor.value
            .notZero() ?: activity.attrColor(R.attr.colorStatusButtonsPopupBg)
        val bgColorState = ColorStateList.valueOf(bgColor)
        ivTriangleTop.backgroundTintList = bgColorState
        ivTriangleBottom.backgroundTintList = bgColorState
        llBarPlaceHolder.backgroundTintList = bgColorState

        val density = activity.density
        fun Int.dp() = (this * density + 0.5f).toInt()

        // popupの大きさ
        root.measure(
            View.MeasureSpec.makeMeasureSpec(listView.width, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(listView.height, View.MeasureSpec.AT_MOST)
        )
        val popupWidth = root.measuredWidth
        val popupHeight = root.measuredHeight

        val location = IntArray(2)

        listView.getLocationInWindow(location)
        val listviewTop = location[1]
        val clipTop = listviewTop + 8.dp()
        val clipBottom = listviewTop + listView.height - 8.dp()

        anchor.getLocationInWindow(location)
        val anchorLeft = location[0]
        val anchorTop = location[1]
        val anchorWidth = anchor.width
        val anchorHeight = anchor.height

        // ポップアップウィンドウの左上（基準は親ウィンドウの左上)
        val popupX = anchorLeft + max(0, (anchorWidth - popupWidth) / 2)
        var popupY = anchorTop + anchorHeight / 2
        if (popupY < clipTop) {
            // 画面外のは画面内にする
            popupY = clipTop
        } else if (popupY + popupHeight > clipBottom) {
            // 画面外のは画面内にする
            if (popupY > clipBottom) popupY = clipBottom

            // 画面の下側にあるならポップアップの吹き出しが下から出ているように見せる
            ivTriangleTop.visibility = View.GONE
            ivTriangleBottom.visibility = View.VISIBLE
            popupY -= popupHeight
        }

        window.showAtLocation(listView, Gravity.LEFT or Gravity.TOP, popupX, popupY)
    }
}
