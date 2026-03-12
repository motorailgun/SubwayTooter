package jp.juggler.subwaytooter.dialog

import android.app.Dialog
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.view.NetworkEmojiView
import jp.juggler.util.ui.dismissSafe

fun ComponentActivity.showEmojiDetailDialog(
    detail: String,
    initialzeNiv: (NetworkEmojiView.() -> Unit)? = null,
    initializeImage: (AppCompatImageView.() -> Unit)? = null,
    initializeText: (AppCompatTextView.() -> Unit)? = null,
) {
    val dialog = Dialog(this)
    val density = resources.displayMetrics.density
    val dp12 = (12 * density + 0.5f).toInt()
    val dp200 = (200 * density + 0.5f).toInt()

    val innerLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp12, dp12, dp12, dp12)
    }

    // NetworkEmojiView (conditional)
    if (initialzeNiv != null) {
        val nivEmoji = NetworkEmojiView(this).apply {
            initialzeNiv()
        }
        innerLayout.addView(
            nivEmoji,
            LinearLayout.LayoutParams(dp200, dp200).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )
    }

    // ImageView (conditional)
    if (initializeImage != null) {
        val ivEmoji = AppCompatImageView(this).apply {
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            initializeImage()
        }
        innerLayout.addView(
            ivEmoji,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp200,
            ),
        )
    }

    // Text (conditional)
    if (initializeText != null) {
        val tvEmoji = AppCompatTextView(this).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            textSize = 200f
            initializeText()
        }
        innerLayout.addView(
            tvEmoji,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    // JSON detail
    val etJson = EditText(this).apply {
        setText(detail)
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        inputType = android.text.InputType.TYPE_NULL
        minLines = 3
    }
    innerLayout.addView(
        etJson,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ),
    )

    val scrollView = ScrollView(this).apply {
        addView(innerLayout)
    }

    val btnOk = Button(this, null, android.R.attr.buttonBarButtonStyle).apply {
        setText(R.string.ok)
        setOnClickListener { dialog.dismissSafe() }
    }
    val buttonBar = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(btnOk, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    }

    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(scrollView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f,
        ))
        addView(buttonBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
    }

    dialog.setTitle(R.string.emoji_detail)
    dialog.setContentView(root)
    dialog.window?.setLayout(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT
    )
    dialog.show()
}
