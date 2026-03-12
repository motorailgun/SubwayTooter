package jp.juggler.subwaytooter.dialog

import android.app.Dialog
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.util.coroutine.cancellationException
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.ui.dismissSafe
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

private fun Int?.toKeyboardType(): KeyboardType {
    if (this == null) return KeyboardType.Text
    val clazz = this and android.text.InputType.TYPE_MASK_CLASS
    val variation = this and android.text.InputType.TYPE_MASK_VARIATION
    return when (clazz) {
        android.text.InputType.TYPE_CLASS_NUMBER -> {
            if ((this and android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL) != 0) {
                KeyboardType.Decimal
            } else {
                KeyboardType.Number
            }
        }
        android.text.InputType.TYPE_CLASS_PHONE -> KeyboardType.Phone
        android.text.InputType.TYPE_CLASS_DATETIME -> KeyboardType.Number
        android.text.InputType.TYPE_CLASS_TEXT -> {
            when (variation) {
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD,
                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD -> KeyboardType.Password
                android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> KeyboardType.Email
                android.text.InputType.TYPE_TEXT_VARIATION_URI -> KeyboardType.Uri
                else -> KeyboardType.Text
            }
        }
        else -> KeyboardType.Text
    }
}

private fun Int?.isMultiLine(): Boolean {
    if (this == null) return false
    return (this and android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
}

@Composable
private fun AppTheme(content: @Composable () -> Unit) {
    StThemedContent(content = content)
}

@Composable
private fun TextInputDialogContent(
    title: CharSequence,
    initialText: CharSequence?,
    inputType: Int?,
    bitmap: Bitmap? = null,
    onCancel: () -> Unit,
    onOk: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText?.toString() ?: "") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val isMultiline = inputType.isMultiLine()

    Surface {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = title.toString(),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = inputType.toKeyboardType(),
                        imeAction = if (isMultiline) ImeAction.Default else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!isMultiline) {
                                onOk(text)
                            }
                        }
                    ),
                    singleLine = !isMultiline,
                    maxLines = if (isMultiline) Int.MAX_VALUE else 1
                )

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Button(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onOk(text) }) {
                    Text(stringResource(R.string.ok))
                }
            }
        }
    }
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
    val dialog = Dialog(this)
    suspendCancellableCoroutine { cont ->
        val composeView = ComposeView(this).apply {
            setContent {
                AppTheme {
                    TextInputDialogContent(
                        title = title,
                        initialText = initialText,
                        inputType = inputType,
                        onCancel = { dialog.cancel() },
                        onOk = { text ->
                            launchAndShowError {
                                val trimmedText = text.trim { it <= ' ' }
                                if (trimmedText.isEmpty() && !allowEmpty) {
                                    onEmptyText()
                                } else if (onOk(trimmedText)) {
                                    if (cont.isActive) cont.resume(Unit) { _, _, _ -> }
                                    dialog.dismissSafe()
                                }
                            }
                        }
                    )
                }
            }
        }
        dialog.setContentView(composeView)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
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
    val dialog = Dialog(this)
    
    // multiline input for media description
    val actualInputType = inputType
        ?: (android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE)

    suspendCancellableCoroutine { cont ->
        val composeView = ComposeView(this).apply {
            setContent {
                AppTheme {
                    TextInputDialogContent(
                        title = title,
                        initialText = initialText,
                        inputType = actualInputType,
                        bitmap = bitmap,
                        onCancel = { dialog.cancel() },
                        onOk = { text ->
                            launchAndShowError {
                                val trimmedText = text.trim { it <= ' ' }
                                if (trimmedText.isEmpty() && !allowEmpty) {
                                    onEmptyText()
                                } else if (onOk(trimmedText)) {
                                    if (cont.isActive) cont.resume(Unit) { _, _, _ -> }
                                    dialog.dismissSafe()
                                }
                            }
                        }
                    )
                }
            }
        }
        dialog.setContentView(composeView)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.setOnDismissListener {
            if (cont.isActive) cont.resumeWithException(cancellationException())
        }
        cont.invokeOnCancellation { dialog.dismissSafe() }
        dialog.show()
    }
}
