package jp.juggler.subwaytooter.columnviewholder

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.column.Column
import jp.juggler.subwaytooter.column.getContentColor
import jp.juggler.subwaytooter.column.getHeaderDesc
import jp.juggler.subwaytooter.util.emojiSizeMode
import jp.juggler.subwaytooter.util.DecodeOptions
import jp.juggler.subwaytooter.view.MyLinkMovementMethod
import jp.juggler.util.ui.dp

internal class ViewHolderHeaderSearch(
    override val activity: ActMain,
    parent: ViewGroup,
) : ViewHolderHeaderBase(
    LinearLayout(parent.context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        orientation = LinearLayout.VERTICAL
        val pad = context.dp(12)
        setPaddingRelative(pad, 0, pad, 0)
    }
) {

    private val tvSearchDesc: TextView

    init {
        val root = itemView as LinearLayout
        root.tag = this

        tvSearchDesc = TextView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val p = context.dp(3)
            setPadding(0, p, 0, p)
            visibility = View.VISIBLE
            movementMethod = MyLinkMovementMethod
        }
        root.addView(tvSearchDesc)
    }

    override fun showColor() {
    }

    override fun bindData(column: Column) {
        super.bindData(column)
        tvSearchDesc.setTextColor(column.getContentColor())
        tvSearchDesc.text = DecodeOptions(
            activity, accessInfo, decodeEmoji = true,
            authorDomain = accessInfo,
            emojiSizeMode = accessInfo.emojiSizeMode(),
        ).decodeHTML(column.getHeaderDesc())
    }

    override fun onViewRecycled() {
    }
}
