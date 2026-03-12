package jp.juggler.subwaytooter.dialog

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Typeface
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import jp.juggler.subwaytooter.R
import jp.juggler.util.coroutine.cancellationException
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.data.notEmpty
import jp.juggler.util.ui.dismissSafe
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

private fun ComponentActivity.createTextInputDialogViews(
    title: CharSequence,
    initialText: CharSequence?,
    inputType: Int?,
): Triple<LinearLayout, EditText, Pair<Button, Button>> {
    val density = resources.displayMetrics.density
    val dp6 = (6 * density + 0.5f).toInt()
    val dp12 = (12 * density + 0.5f).toInt()

    val tvCaption = TextView(this).apply {
        text = title
        setTypeface(null, Typeface.BOLD)
        setPadding(0, 0, 0, dp6)
    }

    val etInput = EditText(this).apply {
        importantForAutofill = EditText.IMPORTANT_FOR_AUTOFILL_NO
        imeOptions = EditorInfo.IME_ACTION_DONE
        this.inputType = inputType ?: android.text.InputType.TYPE_CLASS_TEXT
        gravity = Gravity.CENTER_VERTICAL
    }
    initialText?.notEmpty()?.let {
        etInput.setText(it)
        etInput.setSelection(it.length)
    }

    val innerLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp12, dp6, dp12, dp6)
        addView(tvCaption, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
        addView(etInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
    }

    val scrollView = ScrollView(this).apply {
        addView(innerLayout)
    }

    val btnCancel = Button(this, null, android.R.attr.buttonBarButtonStyle).apply {
        setText(R.string.cancel)
    }
    val btnOk = Button(this, null, android.R.attr.buttonBarButtonStyle).apply {
        setText(R.string.ok)
    }

    val buttonBar = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(btnCancel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
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

    return Triple(root, etInput, btnOk to btnCancel)
}

suspend fun ComponentActivity.showTextInputDialog(
    title: CharSequence,
    initialText: CharSequence?,
    allowEmpty: Boolean = false,
    inputType: Int? = null,
    onEmptyText: suspend () -> Unit,
    // returns true if we can close dialog
    onOk: suspend (String) -> Boolean,
) {
    val (root, etInput, buttons) = createTextInputDialogViews(title, initialText, inputType)
    val (btnOk, btnCancel) = buttons

    etInput.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            btnOk.performClick()
            true
        } else {
            false
        }
    }
    val dialog = Dialog(this)
    dialog.setContentView(root)
    dialog.window?.setLayout(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT
    )
    suspendCancellableCoroutine { cont ->
        btnOk.setOnClickListener {
            launchAndShowError {
                val text = etInput.text.toString().trim { it <= ' ' }
                if (text.isEmpty() && !allowEmpty) {
                    onEmptyText()
                } else if (onOk(text)) {
                    if (cont.isActive) cont.resume(Unit) { _, _, _ -> }
                    dialog.dismissSafe()
                }
            }
        }
        btnCancel.setOnClickListener { dialog.cancel() }
        dialog.setOnDismissListener {
            if (cont.isActive) cont.resumeWithException(cancellationException())
        }
        cont.invokeOnCancellation { dialog.dismissSafe() }
        dialog.show()
    }
}

suspend fun ComponentActivity.showMediaDescEditDialog(
    title: CharSequence,
    initialText: CharSequence?,
    allowEmpty: Boolean = false,
    inputType: Int? = null,
    bitmap: Bitmap?,
    onEmptyText: suspend () -> Unit,
    // returns true if we can close dialog
    onOk: suspend (String) -> Boolean,
) {
    val (root, etInput, buttons) = createTextInputDialogViews(title, initialText, inputType)
    val (btnOk, btnCancel) = buttons

    // multiline input for media description
    etInput.inputType = inputType
        ?: (android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE)

    // Add bitmap preview if provided
    bitmap?.let { bmp ->
        val density = resources.displayMetrics.density
        val dp32 = (32 * density + 0.5f).toInt()
        val scrollView = root.getChildAt(0) as? ScrollView
        val innerLayout = scrollView?.getChildAt(0) as? LinearLayout
        innerLayout?.addView(
            ImageView(this).apply {
                setImageBitmap(bmp)
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_START
                setPadding(dp32, dp32, dp32, dp32)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    etInput.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            btnOk.performClick()
            true
        } else {
            false
        }
    }
    val dialog = Dialog(this)
    dialog.setContentView(root)
    dialog.window?.setLayout(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT
    )
    suspendCancellableCoroutine { cont ->
        btnOk.setOnClickListener {
            launchAndShowError {
                val text = etInput.text.toString().trim { it <= ' ' }
                if (text.isEmpty() && !allowEmpty) {
                    onEmptyText()
                } else if (onOk(text)) {
                    if (cont.isActive) cont.resume(Unit) { _, _, _ -> }
                    dialog.dismissSafe()
                }
            }
        }
        btnCancel.setOnClickListener { dialog.cancel() }
        dialog.setOnDismissListener {
            if (cont.isActive) cont.resumeWithException(cancellationException())
        }
        cont.invokeOnCancellation { dialog.dismissSafe() }
        dialog.show()
    }
}
