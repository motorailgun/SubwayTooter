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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.subwaytooter.table.SavedAccount

class DlgConfirmMail(
    val activity: ComponentActivity,
    val accessInfo: SavedAccount,
    val onClickOk: (email: String?) -> Unit,
) {
    private val dialog = Dialog(activity)

    init {
        val composeView = ComposeView(activity).apply {
            setContent {
                StThemedContent {
                    Surface {
                        DlgConfirmMailContent(
                            accessInfo = accessInfo,
                            onCancel = { dialog.cancel() },
                            onOk = { email -> 
                                onClickOk(email)
                                dialog.dismiss()
                            }
                        )
                    }
                }
            }
        }
        dialog.setContentView(composeView)
    }

    fun show() {
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }
}

@Composable
private fun DlgConfirmMailContent(
    accessInfo: SavedAccount,
    onCancel: () -> Unit,
    onOk: (String?) -> Unit
) {
    var updateMailAddress by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }

    val instanceText = if (accessInfo.apiHost != accessInfo.apDomain) {
        "${accessInfo.apiHost.pretty} (${accessInfo.apDomain.pretty})"
    } else {
        accessInfo.apiHost.pretty
    }
    
    val userName = accessInfo.acct.pretty

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(id = R.string.instance),
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp)
        )
        Text(
            text = instanceText,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Text(
            text = stringResource(id = R.string.user_name),
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp)
        )
        Text(
            text = userName,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 0.dp, top = 8.dp, end = 12.dp)
        ) {
            Checkbox(
                checked = updateMailAddress,
                onCheckedChange = { updateMailAddress = it },
                modifier = Modifier.padding(start = 0.dp)
            )
            Text(text = stringResource(id = R.string.update_mail_address))
        }

        Text(
            text = stringResource(id = R.string.email),
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            enabled = updateMailAddress,
            placeholder = { Text(stringResource(id = R.string.email_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            singleLine = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(id = R.string.cancel))
            }
            TextButton(
                onClick = {
                    onOk(if (updateMailAddress) email.trim() else null)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(id = R.string.ok))
            }
        }

        Text(
            text = stringResource(id = R.string.confirm_mail_description),
            modifier = Modifier.padding(12.dp)
        )
    }
}
