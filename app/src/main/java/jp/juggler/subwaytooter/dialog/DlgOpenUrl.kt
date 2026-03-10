package jp.juggler.subwaytooter.dialog

import android.app.Activity
import android.app.Dialog
import android.content.ClipboardManager
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.postDelayed
import jp.juggler.subwaytooter.R
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.systemService
import jp.juggler.util.ui.isEnabledAlpha

object DlgOpenUrl {
    private val log = LogCategory("DlgOpenUrl")

    fun show(
        activity: Activity,
        onEmptyError: () -> Unit = { activity.showToast(false, R.string.url_empty) },
        onOK: (Dialog, String) -> Unit,
    ) {

        val allowEmpty = false
        val density = activity.resources.displayMetrics.density
        val dp4 = (4 * density + 0.5f).toInt()
        val dp12 = (12 * density + 0.5f).toInt()
        val dp48 = (48 * density + 0.5f).toInt()

        val clipboard: ClipboardManager? = systemService(activity)

        // Label + paste button row
        val tvLabel = TextView(activity).apply {
            setText(R.string.url_of_user_or_status)
            setPadding(dp12, dp12, dp12, 0)
        }
        val btnPaste = ImageButton(activity).apply {
            setImageResource(R.drawable.ic_paste)
            contentDescription = activity.getString(android.R.string.paste)
        }
        val headerRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp12, 0, dp12, 0)
            addView(tvLabel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                gravity = Gravity.CENTER_VERTICAL
            })
            addView(btnPaste, LinearLayout.LayoutParams(dp48, dp48).apply {
                marginStart = dp4
                gravity = Gravity.CENTER_VERTICAL
            })
        }

        // Input field
        val etInput = EditText(activity).apply {
            imeOptions = EditorInfo.IME_ACTION_DONE
            importantForAutofill = EditText.IMPORTANT_FOR_AUTOFILL_NO
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            minimumHeight = dp48
            setPadding(dp12, 0, dp12, 0)
        }

        // Button bar
        val btnCancel = Button(activity, null, android.R.attr.buttonBarButtonStyle).apply {
            setText(R.string.cancel)
        }
        val btnOk = Button(activity, null, android.R.attr.buttonBarButtonStyle).apply {
            setText(R.string.ok)
        }
        val buttonBar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(btnCancel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(btnOk, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(headerRow, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ))
            addView(etInput, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ))
            addView(buttonBar, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ))
        }

        etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnOk.performClick()
                true
            } else {
                false
            }
        }

        val dialog = Dialog(activity)
        dialog.setContentView(root)
        btnCancel.setOnClickListener { dialog.cancel() }
        btnPaste.setOnClickListener { pasteTo(clipboard, etInput) }
        btnOk.setOnClickListener {
            val token = etInput.text.toString().trim { it <= ' ' }
            if (token.isEmpty() && !allowEmpty) {
                onEmptyError()
            } else {
                onOK(dialog, token)
            }
        }

        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
            showPasteButton(clipboard, btnPaste)
        }
        clipboard?.addPrimaryClipChangedListener(clipboardListener)
        dialog.setOnDismissListener {
            clipboard?.removePrimaryClipChangedListener(clipboardListener)
        }
        root.postDelayed(100L) {
            showPasteButton(clipboard, btnPaste)
            pasteTo(clipboard, etInput)
        }
        dialog.show()
    }

    private fun showPasteButton(clipboard: ClipboardManager?, btnPaste: ImageButton) {
        btnPaste.isEnabledAlpha = when {
            clipboard == null -> false
            !clipboard.hasPrimaryClip() -> false
            clipboard.primaryClipDescription?.hasMimeType("text/plain") != true -> false
            else -> true
        }
    }

    private fun pasteTo(clipboard: ClipboardManager?, et: EditText) {
        val text = clipboard?.getUrlFromClipboard()
            ?: return
        val ss = et.selectionStart
        val se = et.selectionEnd
        et.text.replace(ss, se, text)
        et.setSelection(ss, ss + text.length)
    }

    private fun ClipboardManager.getUrlFromClipboard(): String? {
        try {
            val item = primaryClip?.getItemAt(0)
            item?.uri?.toString()?.let { return it }
            item?.text?.toString()?.let { return it }
            log.w("clip has nor uri or text.")
        } catch (ex: Throwable) {
            log.w(ex, "getUrlFromClipboard failed.")
        }
        return null
    }
}
