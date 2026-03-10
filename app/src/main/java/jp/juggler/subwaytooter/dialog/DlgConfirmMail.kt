package jp.juggler.subwaytooter.dialog

import android.app.Dialog
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.util.ui.isEnabledAlpha

class DlgConfirmMail(
    val activity: AppCompatActivity,
    val accessInfo: SavedAccount,
    val onClickOk: (email: String?) -> Unit,
) {
    private val dialog = Dialog(activity)

    private val density = activity.resources.displayMetrics.density
    private val dp12 = (12 * density + 0.5f).toInt()
    private val dp8 = (8 * density + 0.5f).toInt()

    private val cbUpdateMailAddress: CheckBox
    private val etEmail: EditText

    init {
        val root = ScrollView(activity)
        val ll = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(ll)

        // Instance label
        ll.addView(
            TextView(activity).apply {
                setText(R.string.instance)
                setPadding(dp12, dp12, dp12, 0)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // Instance value
        ll.addView(
            TextView(activity).apply {
                text = if (accessInfo.apiHost != accessInfo.apDomain) {
                    "${accessInfo.apiHost.pretty} (${accessInfo.apDomain.pretty})"
                } else {
                    accessInfo.apiHost.pretty
                }
                setPadding(dp12, 0, dp12, 0)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // User name label
        ll.addView(
            TextView(activity).apply {
                setText(R.string.user_name)
                setPadding(dp12, dp12, dp12, 0)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // User name value
        ll.addView(
            TextView(activity).apply {
                text = accessInfo.acct.pretty
                setPadding(dp12, 0, dp12, 0)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // Update mail address checkbox
        cbUpdateMailAddress = CheckBox(activity).apply {
            setText(R.string.update_mail_address)
            setPadding(dp12, dp8, dp12, 0)
        }
        ll.addView(
            cbUpdateMailAddress,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // Email label
        ll.addView(
            TextView(activity).apply {
                setText(R.string.email)
                setPadding(dp12, dp12, dp12, 0)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // Email input
        etEmail = EditText(activity).apply {
            setHint(R.string.email_hint)
            isEnabled = false
            importantForAutofill = EditText.IMPORTANT_FOR_AUTOFILL_NO
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(dp12, 0, dp12, 0)
        }
        ll.addView(
            etEmail,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        cbUpdateMailAddress.setOnCheckedChangeListener { _, isChecked ->
            etEmail.isEnabledAlpha = isChecked
        }

        // Button bar
        val btnCancel = Button(activity, null, android.R.attr.buttonBarButtonStyle).apply {
            setText(R.string.cancel)
            setOnClickListener { dialog.cancel() }
        }
        val btnOk = Button(activity, null, android.R.attr.buttonBarButtonStyle).apply {
            setText(R.string.ok)
            setOnClickListener {
                onClickOk(
                    if (cbUpdateMailAddress.isChecked) etEmail.text.toString().trim() else null
                )
            }
        }
        val buttonBar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(btnCancel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(btnOk, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        ll.addView(
            buttonBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // Description text
        ll.addView(
            TextView(activity).apply {
                setText(R.string.confirm_mail_description)
                setPadding(dp12, dp12, dp12, 0)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        dialog.setContentView(root)
    }

    fun show() {
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }
}
