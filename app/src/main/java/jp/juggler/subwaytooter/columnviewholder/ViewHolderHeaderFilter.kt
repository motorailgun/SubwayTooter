package jp.juggler.subwaytooter.columnviewholder

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import jp.juggler.subwaytooter.ActKeywordFilter
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.R
import jp.juggler.util.ui.dp

internal class ViewHolderHeaderFilter(
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
), View.OnClickListener {

    private val btnCreate: Button

    init {
        val root = itemView as LinearLayout
        root.tag = this

        btnCreate = Button(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val p = context.dp(3)
            setPadding(0, p, 0, p)
            text = activity.getString(R.string.keyword_filter_new)
            setOnClickListener(this@ViewHolderHeaderFilter)
        }
        root.addView(btnCreate)
    }

    override fun showColor() {
    }

    override fun onViewRecycled() {
    }

    override fun onClick(v: View?) {
        ActKeywordFilter.open(activity, column.accessInfo)
    }
}
