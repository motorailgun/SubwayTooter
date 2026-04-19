package es.ariaontheplanet.quasar.actmain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.api.entity.TootVisibility

private val quickPostVisibilityChoices = listOf(
    TootVisibility.Public to R.string.visibility_public,
    TootVisibility.UnlistedHome to R.string.visibility_unlisted,
    TootVisibility.PrivateFollowers to R.string.visibility_private,
    TootVisibility.DirectSpecified to R.string.visibility_direct,
)

private fun visibilityLabelRes(v: TootVisibility): Int =
    quickPostVisibilityChoices.firstOrNull { it.first == v }?.second
        ?: R.string.visibility_public

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPostSheet(
    text: String,
    cwEnabled: Boolean,
    cwText: String,
    visibility: TootVisibility,
    sending: Boolean,
    onTextChange: (String) -> Unit,
    onCwEnabledChange: (Boolean) -> Unit,
    onCwTextChange: (String) -> Unit,
    onVisibilityChange: (TootVisibility) -> Unit,
    onDismiss: () -> Unit,
    onClickSend: () -> Unit,
    onClickExpand: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // CW row + visibility dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Checkbox(
                    checked = cwEnabled,
                    onCheckedChange = onCwEnabledChange,
                )
                Text(
                    text = stringResource(R.string.content_warning),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Box(modifier = Modifier.weight(1f))

                var showVisibilityMenu by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { showVisibilityMenu = true }) {
                        Text(stringResource(visibilityLabelRes(visibility)))
                    }
                    DropdownMenu(
                        expanded = showVisibilityMenu,
                        onDismissRequest = { showVisibilityMenu = false },
                    ) {
                        quickPostVisibilityChoices.forEach { (v, labelRes) ->
                            DropdownMenuItem(
                                text = { Text(stringResource(labelRes)) },
                                onClick = {
                                    showVisibilityMenu = false
                                    onVisibilityChange(v)
                                },
                            )
                        }
                    }
                }
            }

            if (cwEnabled) {
                OutlinedTextField(
                    value = cwText,
                    onValueChange = onCwTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.content_warning_hint)) },
                    singleLine = true,
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp),
                placeholder = { Text(stringResource(R.string.toot)) },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClickExpand, enabled = !sending) {
                    Icon(
                        imageVector = Icons.Filled.OpenInFull,
                        contentDescription = stringResource(R.string.post),
                    )
                }
                IconButton(onClick = onClickSend, enabled = !sending) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = stringResource(R.string.send),
                    )
                }
            }
        }
    }
}
