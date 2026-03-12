package jp.juggler.subwaytooter.dialog

import android.app.Activity
import android.app.Dialog
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.entity.TootAccount
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.util.log.showToast

fun Activity.showReportDialog(
    accessInfo: SavedAccount,
    who: TootAccount,
    status: TootStatus?,
    canForward: Boolean,
    onClickOk: (dialog: Dialog, comment: String, forward: Boolean) -> Unit,
) {
    val dialog = Dialog(this)
    val composeView = ComposeView(this).apply {
        setContent {
            StThemedContent {
                var comment by remember { mutableStateOf("") }
                var forward by remember { mutableStateOf(true) }
                val buttonBgCw = MaterialTheme.colorScheme.surfaceVariant

                Surface {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(text = stringResource(R.string.user))
                            Text(
                                text = who.acct.pretty,
                                fontSize = 20.sp,
                                modifier = Modifier.padding(top = 3.dp)
                            )

                            if (status != null) {
                                Text(
                                    text = stringResource(R.string.status),
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                                Text(
                                    text = status.decoded_content.toString(),
                                    modifier = Modifier
                                        .padding(top = 3.dp)
                                        .background(buttonBgCw)
                                        .padding(6.dp)
                                )
                            }

                            Text(
                                text = stringResource(R.string.report_reason),
                                modifier = Modifier.padding(top = 24.dp)
                            )
                            
                            OutlinedTextField(
                                value = comment,
                                onValueChange = { comment = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 3.dp)
                                    .heightIn(min = 100.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                                maxLines = Int.MAX_VALUE
                            )

                            if (canForward) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = forward,
                                        onCheckedChange = { forward = it }
                                    )
                                    Text(
                                        text = getString(R.string.report_forward_to, who.apDomain.pretty),
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { dialog.cancel() }) {
                                Text(stringResource(R.string.cancel))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = {
                                val trimmed = comment.trim()
                                if (trimmed.isEmpty()) {
                                    showToast(true, R.string.comment_empty)
                                } else {
                                    onClickOk(dialog, trimmed, canForward && forward)
                                }
                            }) {
                                Text(stringResource(R.string.ok))
                            }
                        }
                    }
                }
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
