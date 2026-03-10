package jp.juggler.subwaytooter.dialog

import android.app.Activity
import android.app.Dialog
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.entity.TootAccount
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.util.log.*
import jp.juggler.util.ui.attrColor

fun Activity.showReportDialog(
    accessInfo: SavedAccount,
    who: TootAccount,
    status: TootStatus?,
    canForward: Boolean,
    onClickOk: (dialog: Dialog, comment: String, forward: Boolean) -> Unit,
) {
    val dialog = Dialog(this)
    val density = resources.displayMetrics.density
    val dp3 = (3 * density + 0.5f).toInt()
    val dp6 = (6 * density + 0.5f).toInt()
    val dp12 = (12 * density + 0.5f).toInt()
    val dp24 = (24 * density + 0.5f).toInt()

    val innerLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp12, dp12, dp12, dp12)
    }

    // User label
    innerLayout.addView(
        TextView(this).apply { setText(R.string.user) },
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ),
    )

    // User value
    innerLayout.addView(
        TextView(this).apply {
            text = who.acct.pretty
            textSize = 20f
        },
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp3 },
    )

    // Status caption + value (conditional)
    if (status != null) {
        innerLayout.addView(
            TextView(this).apply { setText(R.string.status) },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp12 },
        )
        innerLayout.addView(
            TextView(this).apply {
                text = status.decoded_content
                setPadding(dp6, dp6, dp6, dp6)
                setBackgroundColor(attrColor(R.attr.colorButtonBgCw))
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp3 },
        )
    }

    // Comment label
    innerLayout.addView(
        TextView(this).apply { setText(R.string.report_reason) },
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp24 },
    )

    // Comment input
    val etComment = EditText(this).apply {
        importantForAutofill = EditText.IMPORTANT_FOR_AUTOFILL_NO
        inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        minLines = 3
    }
    val commentFrame = FrameLayout(this).apply {
        setBackgroundColor(attrColor(R.attr.colorPostFormBackground))
        addView(etComment)
    }
    innerLayout.addView(
        commentFrame,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp3 },
    )

    // Forward checkbox (conditional)
    val cbForward = CheckBox(this).apply {
        isChecked = true
        text = getString(R.string.report_forward_to, who.apDomain.pretty)
    }
    if (canForward) {
        innerLayout.addView(
            cbForward,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp12 },
        )
    }

    val scrollView = ScrollView(this).apply {
        addView(innerLayout)
    }

    // Button bar
    val btnCancel = Button(this, null, android.R.attr.buttonBarButtonStyle).apply {
        setText(R.string.cancel)
        setOnClickListener { dialog.cancel() }
    }
    val btnOk = Button(this, null, android.R.attr.buttonBarButtonStyle).apply {
        setText(R.string.ok)
        setOnClickListener {
            when (val comment = etComment.text?.toString()?.trim()) {
                null, "" -> showToast(true, R.string.comment_empty)
                else -> onClickOk(dialog, comment, canForward && cbForward.isChecked)
            }
        }
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

    dialog.apply {
        setContentView(root)
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
        show()
    }
}
