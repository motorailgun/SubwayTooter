package jp.juggler.subwaytooter.dialog

import android.app.Activity
import android.app.Dialog
import android.content.ClipboardManager
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.systemService
import kotlinx.coroutines.delay

object DlgOpenUrl {
    private val log = LogCategory("DlgOpenUrl")

    fun show(
        activity: Activity,
        onEmptyError: () -> Unit = { activity.showToast(false, R.string.url_empty) },
        onOK: (Dialog, String) -> Unit,
    ) {
        val dialog = Dialog(activity)

        val composeView = ComposeView(activity).apply {
            setContent {
                StThemedContent {
                    Surface {
                        DlgOpenUrlContent(
                            activity = activity,
                            onCancel = { dialog.cancel() },
                            onOk = { token ->
                                if (token.isEmpty()) {
                                    onEmptyError()
                                } else {
                                    onOK(dialog, token)
                                }
                            }
                        )
                    }
                }
            }
        }
        dialog.setContentView(composeView)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
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

    @Composable
    private fun DlgOpenUrlContent(
        activity: Activity,
        onCancel: () -> Unit,
        onOk: (String) -> Unit
    ) {
        var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
        var isPasteEnabled by remember { mutableStateOf(false) }
        val clipboard: ClipboardManager? = remember { systemService(activity) }

        val updatePasteState = {
            isPasteEnabled = when {
                clipboard == null -> false
                !clipboard.hasPrimaryClip() -> false
                clipboard.primaryClipDescription?.hasMimeType("text/plain") != true -> false
                else -> true
            }
        }

        val doPaste = {
            val clipText = clipboard?.getUrlFromClipboard()
            if (clipText != null) {
                val text = textFieldValue.text
                val selection = textFieldValue.selection
                val newText = text.substring(0, selection.min) + clipText + text.substring(selection.max)
                val newSelection = selection.min + clipText.length
                textFieldValue = TextFieldValue(text = newText, selection = TextRange(newSelection))
            }
        }

        DisposableEffect(clipboard) {
            val listener = ClipboardManager.OnPrimaryClipChangedListener {
                updatePasteState()
            }
            clipboard?.addPrimaryClipChangedListener(listener)
            onDispose {
                clipboard?.removePrimaryClipChangedListener(listener)
            }
        }

        LaunchedEffect(Unit) {
            delay(100)
            updatePasteState()
            doPaste()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.url_of_user_or_status),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { doPaste() },
                    enabled = isPasteEnabled,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_paste),
                        contentDescription = stringResource(id = android.R.string.paste)
                    )
                }
            }

            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onOk(textFieldValue.text.trim())
                    }
                ),
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(id = R.string.cancel))
                }
                TextButton(
                    onClick = { onOk(textFieldValue.text.trim()) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(id = R.string.ok))
                }
            }
        }
    }
}
