package jp.juggler.subwaytooter.dialog

import android.app.Dialog
import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.util.coroutine.cancellationException
import jp.juggler.util.data.notEmpty
import jp.juggler.util.ui.dismissSafe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

@Suppress("MatchingDeclarationName")
class ActionsDialogInitializer(
    val title: CharSequence? = null,
) {
    class Action(val caption: CharSequence, val action: suspend () -> Unit)

    val list = ArrayList<Action>()

    fun action(caption: CharSequence, action: suspend () -> Unit) {
        list.add(Action(caption, action))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun showSuspend(context: Context): Action =
        suspendCancellableCoroutine { cont ->
            val dialog = Dialog(context)
            val composeView = ComposeView(context).apply {
                if (context is androidx.lifecycle.LifecycleOwner) {
                    setViewTreeLifecycleOwner(context as androidx.lifecycle.LifecycleOwner)
                }
                if (context is androidx.savedstate.SavedStateRegistryOwner) {
                    setViewTreeSavedStateRegistryOwner(context as androidx.savedstate.SavedStateRegistryOwner)
                }
                setContent {
                    StThemedContent {
                        ActionsDialogContent(
                            title = title,
                            items = list,
                            onSelect = { action ->
                                if (cont.isActive) cont.resume(action) { _, _, _ -> }
                                dialog.dismissSafe()
                            },
                            onCancel = {
                                dialog.dismissSafe()
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
            dialog.setOnDismissListener {
                if (cont.isActive) cont.resumeWithException(cancellationException())
            }
            cont.invokeOnCancellation { dialog.dismissSafe() }
            dialog.show()
        }
}

@Composable
private fun ActionsDialogContent(
    title: CharSequence?,
    items: List<ActionsDialogInitializer.Action>,
    onSelect: (ActionsDialogInitializer.Action) -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            title?.notEmpty()?.let {
                Text(
                    text = it.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                items.forEach { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(action) }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = action.caption.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // spacer
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onCancel) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        }
    }
}

suspend fun Context.actionsDialog(
    title: String? = null,
    init: suspend ActionsDialogInitializer.() -> Unit,
) {
    ActionsDialogInitializer(title)
        .apply { init() }
        .showSuspend(this)
        .action.invoke()
}
