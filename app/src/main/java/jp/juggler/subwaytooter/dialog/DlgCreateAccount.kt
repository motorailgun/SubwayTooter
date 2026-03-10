package jp.juggler.subwaytooter.dialog

import android.app.Dialog
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.auth.CreateUserParams
import jp.juggler.subwaytooter.api.entity.Host
import jp.juggler.subwaytooter.api.entity.TootInstance
import jp.juggler.subwaytooter.util.DecodeOptions
import jp.juggler.subwaytooter.util.LinkHelper
import jp.juggler.subwaytooter.util.openCustomTab
import jp.juggler.util.data.*
import jp.juggler.util.log.showToast

class DlgCreateAccount(
    val activity: AppCompatActivity,
    val apiHost: Host,
    val onClickOk: (dialog: Dialog, params: CreateUserParams) -> Unit,
) {

    companion object {
        fun AppCompatActivity.showUserCreateDialog(
            apiHost: Host,
            onClickOk: (dialog: Dialog, params: CreateUserParams) -> Unit,
        ) = DlgCreateAccount(this, apiHost, onClickOk).show()
    }

    private val density = activity.resources.displayMetrics.density
    private val dp12 = (12 * density + 0.5f).toInt()

    private val etUserName: EditText
    private val etEmail: EditText
    private val etPassword: EditText
    private val cbAgreement: CheckBox
    private val etReason: EditText

    private val dialog = Dialog(activity)

    private val rootView: ScrollView

    init {
        val ll = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun addLabel(textResId: Int) {
            ll.addView(
                TextView(activity).apply {
                    setText(textResId)
                    setPadding(dp12, dp12, dp12, 0)
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        fun addText(text: CharSequence) {
            ll.addView(
                TextView(activity).apply {
                    this.text = text
                    setPadding(dp12, 0, dp12, 0)
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        fun addEditText(hintResId: Int, inputType: Int = android.text.InputType.TYPE_CLASS_TEXT): EditText {
            val et = EditText(activity).apply {
                setHint(hintResId)
                importantForAutofill = EditText.IMPORTANT_FOR_AUTOFILL_NO
                this.inputType = inputType
                setPadding(dp12, 0, dp12, 0)
            }
            ll.addView(
                et,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
            return et
        }

        // Instance 
        addLabel(R.string.instance)
        addText(apiHost.pretty)

        // Description
        addLabel(R.string.description)
        val instanceInfo = TootInstance.getCached(apiHost)
        val descriptionText = DecodeOptions(
            activity,
            linkHelper = LinkHelper.create(
                apiHost,
                misskeyVersion = instanceInfo?.misskeyVersionMajor ?: 0
            ),
        ).decodeHTML(
            instanceInfo?.description?.notBlank()
                ?: instanceInfo?.descriptionOld?.notBlank()
                ?: TootInstance.DESCRIPTION_DEFAULT
        ).neatSpaces()
        addText(descriptionText)

        // User name
        addLabel(R.string.user_name)
        etUserName = addEditText(R.string.user_name_hint)

        // Email
        addLabel(R.string.email)
        etEmail = addEditText(R.string.email_hint)

        // Password
        addLabel(R.string.password)
        etPassword = addEditText(
            R.string.password_hint,
            android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD,
        )

        // Reason (conditional)
        val showReason = instanceInfo?.approval_required ?: false
        val tvReasonCaption = TextView(activity).apply {
            setText(R.string.reason_create_account)
            setPadding(dp12, dp12, dp12, 0)
            visibility = if (showReason) View.VISIBLE else View.GONE
        }
        ll.addView(
            tvReasonCaption,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        etReason = EditText(activity).apply {
            importantForAutofill = EditText.IMPORTANT_FOR_AUTOFILL_NO
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(dp12, 0, dp12, 0)
            visibility = if (showReason) View.VISIBLE else View.GONE
        }
        ll.addView(
            etReason,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // Rules button
        ll.addView(
            Button(activity).apply {
                setText(R.string.instance_rules)
                isAllCaps = false
                setPadding(dp12, 0, dp12, 0)
                setOnClickListener { activity.openCustomTab("https://$apiHost/about/more") }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp12 },
        )

        // Terms button
        ll.addView(
            Button(activity).apply {
                setText(R.string.privacy_policy)
                isAllCaps = false
                setPadding(dp12, 0, dp12, 0)
                setOnClickListener { activity.openCustomTab("https://$apiHost/terms") }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // Agreement checkbox
        cbAgreement = CheckBox(activity).apply {
            setText(R.string.agree_terms)
            setPadding(dp12, 0, dp12, 0)
        }
        ll.addView(
            cbAgreement,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // Button bar
        val btnCancel = Button(activity, null, android.R.attr.buttonBarButtonStyle).apply {
            setText(R.string.cancel)
            setOnClickListener { dialog.cancel() }
        }
        val btnOk = Button(activity, null, android.R.attr.buttonBarButtonStyle).apply {
            setText(R.string.ok)
            setOnClickListener { onClick() }
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

        rootView = ScrollView(activity).apply {
            addView(ll)
        }
    }

    fun show() {
        dialog.setContentView(rootView)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }

    private fun onClick() {
        val username = etUserName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        when {
            username.isEmpty() ->
                activity.showToast(true, R.string.username_empty)

            email.isEmpty() ->
                activity.showToast(true, R.string.email_empty)

            password.isEmpty() ->
                activity.showToast(true, R.string.password_empty)

            username.contains("/") || username.contains("@") ->
                activity.showToast(true, R.string.username_not_need_atmark)

            else -> onClickOk(
                dialog,
                CreateUserParams(
                    username = username,
                    email = email,
                    password = password,
                    agreement = cbAgreement.isChecked,
                    reason = when (etReason.visibility) {
                        View.VISIBLE -> etReason.text.toString().trim()
                        else -> null
                    },
                )
            )
        }
    }
}
