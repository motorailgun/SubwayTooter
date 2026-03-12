package jp.juggler.subwaytooter.ui.languageFilter

import android.app.Dialog
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.subwaytooter.dialog.actionsDialog
import jp.juggler.util.coroutine.cancellationException
import jp.juggler.util.ui.dismissSafe
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

sealed interface LanguageFilterEditResult {
    class Update(val code: String, val allow: Boolean) : LanguageFilterEditResult
    class Delete(val code: String) : LanguageFilterEditResult
}

@Composable
private fun LanguageFilterEditContent(
    item: LanguageFilterItem?,
    nameMap: Map<String, LanguageInfo>,
    onOk: (code: String, allow: Boolean) -> Unit,
    onDelete: ((code: String) -> Unit)?,
    onCancel: () -> Unit,
    onPresetsClick: (onSelect: (String) -> Unit) -> Unit,
) {
    var code by remember { mutableStateOf(item?.code ?: "") }
    var isAllow by remember { mutableStateOf(item?.allow ?: true) }
    val isEditing = item != null
    val focusRequester = remember { FocusRequester() }

    val displayName = nameMap[code.trim()]?.displayName
        ?: stringResource(R.string.custom)

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            // Language label + input
            Text(
                text = stringResource(R.string.language),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    enabled = !isEditing,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    label = { Text(stringResource(R.string.language_code_hint)) },
                )
                if (!isEditing) {
                    IconButton(
                        onClick = {
                            onPresetsClick { selected ->
                                code = selected
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = stringResource(R.string.presets),
                        )
                    }
                }
            }

            // Language description
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Show/Hide label + radio buttons
            Text(
                text = stringResource(R.string.show_hide),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = isAllow,
                        onClick = { isAllow = true },
                    )
                    Text(stringResource(R.string.language_show))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !isAllow,
                        onClick = { isAllow = false },
                    )
                    Text(stringResource(R.string.language_hide))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (onDelete != null) {
                    OutlinedButton(onClick = { onDelete(code.trim()) }) {
                        Text(stringResource(R.string.delete))
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                }
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.size(8.dp))
                Button(onClick = { onOk(code.trim(), isAllow) }) {
                    Text(stringResource(R.string.ok))
                }
            }
        }
    }

    if (!isEditing) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

/**
 * 言語コード1つを追加/編集/削除するダイアログ
 */
suspend fun ComponentActivity.dialogLanguageFilterEdit(
    // 既存項目の編集時は非null
    item: LanguageFilterItem?,
    // 言語コード→表示名のマップ
    nameMap: Map<String, LanguageInfo>,
): LanguageFilterEditResult = suspendCancellableCoroutine { cont ->
    val dialog = Dialog(this)
    val composeView = ComposeView(this).apply {
        setContent {
            val scope = rememberCoroutineScope()
            StThemedContent {
                LanguageFilterEditContent(
                    item = item,
                    nameMap = nameMap,
                    onOk = { code, allow ->
                        if (cont.isActive) cont.resume(
                            LanguageFilterEditResult.Update(code, allow)
                        ) { _, _, _ -> }
                        dialog.dismissSafe()
                    },
                    onDelete = if (item != null && item.code != TootStatus.LANGUAGE_CODE_DEFAULT) {
                        { code ->
                            if (cont.isActive) cont.resume(
                                LanguageFilterEditResult.Delete(code)
                            ) { _, _, _ -> }
                            dialog.dismissSafe()
                        }
                    } else null,
                    onCancel = { dialog.cancel() },
                    onPresetsClick = { onSelect ->
                        scope.launch {
                            actionsDialog(getString(R.string.presets)) {
                                val languageList = nameMap.map {
                                    LanguageFilterItem(it.key, true)
                                }.sortedWith(languageFilterItemComparator)
                                for (a in languageList) {
                                    action("${a.code} ${langDesc(a.code, nameMap)}") {
                                        onSelect(a.code)
                                    }
                                }
                            }
                        }
                    },
                )
            }
        }
    }
    dialog.setContentView(composeView)
    dialog.setOnDismissListener {
        if (cont.isActive) cont.resumeWithException(cancellationException())
    }
    cont.invokeOnCancellation { dialog.dismissSafe() }
    dialog.show()
}
