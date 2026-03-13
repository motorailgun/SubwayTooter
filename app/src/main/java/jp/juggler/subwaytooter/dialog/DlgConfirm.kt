package jp.juggler.subwaytooter.dialog

import android.app.Activity
import android.app.Dialog
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.util.ui.dismissSafe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object DlgConfirm {

    @Composable
    private fun ConfirmContent(
        message: CharSequence,
        title: CharSequence? = null,
        showSkipNext: Boolean = false,
        showOkOnly: Boolean = false,
        onOk: (skipNext: Boolean) -> Unit,
        onCancel: () -> Unit,
    ) {
        val skipNextState = remember { mutableStateOf(false) }
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                if (title != null) {
                    Text(
                        text = title.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (showOkOnly) {
                        // For okDialog: render URLs as clickable links
                        val linkColor = MaterialTheme.colorScheme.primary
                        val textColor = MaterialTheme.colorScheme.onSurface
                        val text = message.toString()
                        val urlRegex = remember {
                            Regex("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+")
                        }
                        val annotated = remember(text, linkColor) {
                            buildAnnotatedString {
                                append(text)
                                urlRegex.findAll(text).forEach { match ->
                                    addStyle(
                                        SpanStyle(
                                            color = linkColor,
                                            textDecoration = TextDecoration.Underline,
                                        ),
                                        match.range.first,
                                        match.range.last + 1,
                                    )
                                    addStringAnnotation(
                                        tag = "URL",
                                        annotation = match.value,
                                        start = match.range.first,
                                        end = match.range.last + 1,
                                    )
                                }
                            }
                        }
                        val uriHandler = LocalUriHandler.current
                        @Suppress("DEPRECATION")
                        ClickableText(
                            text = annotated,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = textColor,
                                fontSize = 16.sp,
                            ),
                            onClick = { offset ->
                                annotated.getStringAnnotations("URL", offset, offset)
                                    .firstOrNull()?.let { uriHandler.openUri(it.item) }
                            },
                        )
                    } else {
                        Text(
                            text = message.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 16.sp,
                        )
                    }
                }
                if (showSkipNext) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Checkbox(
                            checked = skipNextState.value,
                            onCheckedChange = { skipNextState.value = it },
                        )
                        Text(
                            text = stringResource(R.string.dont_confirm_again),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (!showOkOnly) {
                        TextButton(onClick = onCancel) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                    Button(
                        onClick = { onOk(skipNextState.value) },
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text(stringResource(R.string.ok))
                    }
                }
            }
        }
    }

    private fun Activity.showComposeDialog(
        message: CharSequence,
        title: CharSequence? = null,
        showSkipNext: Boolean = false,
        showOkOnly: Boolean = false,
        onOk: (skipNext: Boolean) -> Unit,
        onCancel: () -> Unit,
    ): Dialog {
        val dialog = Dialog(this)
        val composeView = ComposeView(this).apply {
            if (this@showComposeDialog is androidx.lifecycle.LifecycleOwner) {
                setViewTreeLifecycleOwner(this@showComposeDialog)
            }
            if (this@showComposeDialog is androidx.savedstate.SavedStateRegistryOwner) {
                setViewTreeSavedStateRegistryOwner(this@showComposeDialog)
            }
            setContent {
                StThemedContent {
                    ConfirmContent(
                        message = message,
                        title = title,
                        showSkipNext = showSkipNext,
                        showOkOnly = showOkOnly,
                        onOk = onOk,
                        onCancel = {
                            dialog.dismissSafe()
                            onCancel()
                        },
                    )
                }
            }
        }
        dialog.setContentView(composeView)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
        return dialog
    }

    suspend fun Activity.confirm(
        message: String,
        isConfirmEnabled: Boolean,
        setConfirmEnabled: (newConfirmEnabled: Boolean) -> Unit,
    ) {
        if (!isConfirmEnabled) return
        val skipNext = suspendCancellableCoroutine { cont ->
            try {
                val dialog = showComposeDialog(
                    message = message,
                    showSkipNext = true,
                    onOk = { skipNext ->
                        if (cont.isActive) cont.resume(skipNext)
                    },
                    onCancel = {
                        if (cont.isActive) cont.resumeWithException(CancellationException("dialog cancelled."))
                    },
                )
                dialog.setOnDismissListener {
                    if (cont.isActive) cont.resumeWithException(CancellationException("dialog cancelled."))
                }
                dialog.show()
                cont.invokeOnCancellation { dialog.dismissSafe() }
            } catch (ex: Throwable) {
                cont.resumeWithException(ex)
            }
        }
        if (skipNext) setConfirmEnabled(false)
    }

    suspend fun Activity.confirm(@StringRes messageId: Int, vararg args: Any?) =
        confirm(getString(messageId, *args))

    suspend fun Activity.confirm(message: CharSequence, title: CharSequence? = null) {
        suspendCancellableCoroutine { cont ->
            try {
                val dialog = showComposeDialog(
                    message = message,
                    title = title,
                    onOk = { if (cont.isActive) cont.resume(Unit) },
                    onCancel = {
                        if (cont.isActive) cont.resumeWithException(CancellationException("dialog closed."))
                    },
                )
                dialog.setOnDismissListener {
                    if (cont.isActive) cont.resumeWithException(CancellationException("dialog closed."))
                }
                dialog.show()
                cont.invokeOnCancellation { dialog.dismissSafe() }
            } catch (ex: Throwable) {
                cont.resumeWithException(ex)
            }
        }
    }

    suspend fun Activity.okDialog(@StringRes messageId: Int, vararg args: Any?) =
        okDialog(getString(messageId, *args))

    suspend fun Activity.okDialog(message: CharSequence, title: CharSequence? = null) {
        suspendCancellableCoroutine { cont ->
            try {
                val dialog = showComposeDialog(
                    message = message,
                    title = title,
                    showOkOnly = true,
                    onOk = { if (cont.isActive) cont.resume(Unit) },
                    onCancel = {},
                )
                dialog.setOnDismissListener {
                    if (cont.isActive) cont.resumeWithException(CancellationException("dialog closed."))
                }
                dialog.show()
                cont.invokeOnCancellation { dialog.dismissSafe() }
            } catch (ex: Throwable) {
                cont.resumeWithException(ex)
            }
        }
    }
}
