package jp.juggler.subwaytooter.view

import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import jp.juggler.subwaytooter.R
import jp.juggler.util.ui.attrColor
import jp.juggler.util.ui.textOrGone

class ActionBarCustomTitle(
    val root: LinearLayout,
    val tvTitle: TextView,
    val tvSubtitle: TextView,
)

fun AppCompatActivity.wrapTitleTextView(
    title: CharSequence? = null,
    subtitle: CharSequence? = null,
): ActionBarCustomTitle {
    val textColor = attrColor(R.attr.colorTextContent)
    val tvTitle = TextView(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        gravity = android.view.Gravity.CENTER_VERTICAL
        includeFontPadding = false
        setTextColor(textColor)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
    }
    val tvSubtitle = TextView(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = (4 * resources.displayMetrics.density + 0.5f).toInt() }
        gravity = android.view.Gravity.CENTER_VERTICAL
        includeFontPadding = false
        setTextColor(textColor)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        visibility = android.view.View.GONE
        setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM)
    }
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(tvTitle)
        addView(tvSubtitle)
    }

    tvTitle.textOrGone = (title ?: this.title)
    tvSubtitle.textOrGone = subtitle
    root.setTag(supportActionBar)

    supportActionBar?.apply {
        // 通常のタイトル表示をOFF
        setDisplayShowTitleEnabled(false)
        // カスタムビューの表示を許可する
        setDisplayOptions(
            ActionBar.DISPLAY_SHOW_CUSTOM, // bits
            ActionBar.DISPLAY_SHOW_CUSTOM, // mask
        )
        // カスタムビューをセット
        setCustomView(
            root,
            ActionBar.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        )
    }

    return ActionBarCustomTitle(root, tvTitle, tvSubtitle)
}

var ActionBarCustomTitle.title: CharSequence?
    get() = tvTitle.textOrGone ?: (root.getTag() as? ActionBar)?.title
    set(value) {
        tvTitle.textOrGone = value
    }

var ActionBarCustomTitle.subtitle: CharSequence?
    get() = tvSubtitle.textOrGone ?: (root.getTag() as? ActionBar)?.subtitle
    set(value) {
        tvSubtitle.textOrGone = value
    }
