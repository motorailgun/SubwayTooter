package jp.juggler.subwaytooter.dialog

import android.app.Dialog
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.auth.CreateUserParams
import jp.juggler.subwaytooter.api.entity.Host
import jp.juggler.subwaytooter.api.entity.TootInstance
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.subwaytooter.util.DecodeOptions
import jp.juggler.subwaytooter.util.LinkHelper
import jp.juggler.subwaytooter.util.openCustomTab
import jp.juggler.util.data.notBlank
import jp.juggler.util.data.neatSpaces
import jp.juggler.util.log.showToast
import jp.juggler.util.ui.dismissSafe

class DlgCreateAccount(
    val activity: ComponentActivity,
    val apiHost: Host,
    val onClickOk: (dialog: Dialog, params: CreateUserParams) -> Unit,
) {

    companion object {
        fun ComponentActivity.showUserCreateDialog(
            apiHost: Host,
            onClickOk: (dialog: Dialog, params: CreateUserParams) -> Unit,
        ) = DlgCreateAccount(this, apiHost, onClickOk).show()
    }

    fun show() {
        val instanceInfo = TootInstance.getCached(apiHost)
        val showReason = instanceInfo?.approval_required ?: false
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
        ).neatSpaces().toString()

        val dialog = Dialog(activity)
        val composeView = ComposeView(activity).apply {
            setContent {
                StThemedContent {
                    CreateAccountContent(
                        apiHostPretty = apiHost.pretty,
                        description = descriptionText,
                        showReason = showReason,
                        onOk = { username, email, password, agreement, reason ->
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
                                        agreement = agreement,
                                        reason = reason,
                                    )
                                )
                            }
                        },
                        onCancel = { dialog.cancel() },
                        onShowRules = { activity.openCustomTab("https://$apiHost/about/more") },
                        onShowTerms = { activity.openCustomTab("https://$apiHost/terms") },
                    )
                }
            }
        }
        dialog.setContentView(composeView)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
        dialog.show()
    }
}

@Composable
private fun CreateAccountContent(
    apiHostPretty: String,
    description: String,
    showReason: Boolean,
    onOk: (username: String, email: String, password: String, agreement: Boolean, reason: String?) -> Unit,
    onCancel: () -> Unit,
    onShowRules: () -> Unit,
    onShowTerms: () -> Unit,
) {
    val username = remember { mutableStateOf("") }
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val agreement = remember { mutableStateOf(false) }
    val reason = remember { mutableStateOf("") }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Instance
                Text(
                    text = stringResource(R.string.instance),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = apiHostPretty,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                // Description
                Text(
                    text = stringResource(R.string.description),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                // Username
                OutlinedTextField(
                    value = username.value,
                    onValueChange = { username.value = it },
                    label = { Text(stringResource(R.string.user_name)) },
                    placeholder = { Text(stringResource(R.string.user_name_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )

                // Email
                OutlinedTextField(
                    value = email.value,
                    onValueChange = { email.value = it },
                    label = { Text(stringResource(R.string.email)) },
                    placeholder = { Text(stringResource(R.string.email_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )

                // Password
                OutlinedTextField(
                    value = password.value,
                    onValueChange = { password.value = it },
                    label = { Text(stringResource(R.string.password)) },
                    placeholder = { Text(stringResource(R.string.password_hint)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )

                // Reason (conditional)
                if (showReason) {
                    OutlinedTextField(
                        value = reason.value,
                        onValueChange = { reason.value = it },
                        label = { Text(stringResource(R.string.reason_create_account)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                }

                // Rules & Terms buttons
                TextButton(
                    onClick = onShowRules,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.instance_rules))
                }
                TextButton(
                    onClick = onShowTerms,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.privacy_policy))
                }

                // Agreement checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Checkbox(
                        checked = agreement.value,
                        onCheckedChange = { agreement.value = it },
                    )
                    Text(
                        text = stringResource(R.string.agree_terms),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // Button bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        onOk(
                            username.value.trim(),
                            email.value.trim(),
                            password.value.trim(),
                            agreement.value,
                            if (showReason) reason.value.trim() else null,
                        )
                    },
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        }
    }
}
