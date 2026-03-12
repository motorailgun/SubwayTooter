package jp.juggler.subwaytooter.actpost

import android.content.Intent
import android.net.Uri
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import jp.juggler.subwaytooter.ActPost
import jp.juggler.subwaytooter.R
import jp.juggler.util.log.LogCategory
import jp.juggler.util.ui.attrColor
import com.google.android.material.R as MR

private val log = LogCategory("ActPostMushroom")

fun ActPost.resetMushroom() {
    states.mushroomInput = 0
    states.mushroomStart = 0
    states.mushroomEnd = 0
}

fun ActPost.openPluginList() {
    val url = "https://github.com/tateisu/SubwayTooter/wiki/Simeji-Mushroom-Plugins"
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

fun ActPost.showRecommendedPlugin(@StringRes titleId: Int) {
    val linkCaption = getString(R.string.plugin_app_intro)
    val linkSpan = object : ClickableSpan() {
        override fun onClick(view: View) = openPluginList()
        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.color = attrColor(androidx.appcompat.R.attr.colorPrimary)
        }
    }

    val density = resources.displayMetrics.density
    val dp12 = (12 * density + 0.5f).toInt()

    val tvText = TextView(this).apply {
        movementMethod = LinkMovementMethod.getInstance()
        textSize = 16f
        setPadding(dp12, dp12, dp12, dp12)
        text = SpannableStringBuilder().apply {
            val spanStart = length
            append(linkCaption)
            val spanEnd = length
            setSpan(linkSpan, spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    val scrollView = ScrollView(this).apply {
        addView(tvText)
    }

    AlertDialog.Builder(this).apply {
        setTitle(titleId)
        setView(scrollView)
        setCancelable(true)
        setPositiveButton(R.string.ok, null)
    }.show()
}

fun ActPost.openMushroom() {
    try {
        val (mushroomInput: Int, et: TextEditState) = when (focusedEditField) {
            1 -> 1 to views.etContentWarning
            in 2..5 -> focusedEditField to etChoices[focusedEditField - 2]
            else -> 0 to views.etContent
        }
        states.mushroomInput = mushroomInput
        val text = prepareMushroomText(et)

        val intent = Intent("com.adamrocker.android.simeji.ACTION_INTERCEPT")
        intent.addCategory("com.adamrocker.android.simeji.REPLACE")
        intent.putExtra("replace_key", text)

        val chooser = Intent.createChooser(intent, getString(R.string.select_plugin))

        if (intent.resolveActivity(packageManager) == null) {
            showRecommendedPlugin(R.string.plugin_not_installed)
            return
        }

        arMushroom.launch(chooser)
    } catch (ex: Throwable) {
        log.e(ex, "openMushroom failed.")
        showRecommendedPlugin(R.string.plugin_not_installed)
    }
}

fun ActPost.prepareMushroomText(et: TextEditState): String {
    states.mushroomStart = et.selectionStart
    states.mushroomEnd = et.selectionEnd
    return when {
        states.mushroomStart >= states.mushroomEnd -> ""
        else -> et.text.toString().substring(states.mushroomStart, states.mushroomEnd)
    }
}

fun ActPost.applyMushroomText(et: TextEditState, text: String) {
    val src = et.text.toString()
    if (states.mushroomStart > src.length) states.mushroomStart = src.length
    if (states.mushroomEnd > src.length) states.mushroomEnd = src.length

    val sb = StringBuilder()
    sb.append(src.substring(0, states.mushroomStart))
    sb.append(text)
    val newSelEnd = sb.length
    sb.append(src.substring(states.mushroomEnd))
    et.setText(sb)
    et.setSelection(newSelEnd)
}
