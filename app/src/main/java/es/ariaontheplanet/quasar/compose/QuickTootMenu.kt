package es.ariaontheplanet.quasar.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.api.entity.TootVisibility
import es.ariaontheplanet.quasar.getVisibilityCaption
import es.ariaontheplanet.quasar.pref.PrefS

@Composable
fun QuickTootMenuDialog(
    visibility: TootVisibility,
    onVisibilityPick: (TootVisibility) -> Unit,
    onUseMacro: (String) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    
    // Load initial macros
    val initialStrings = remember { 
        PrefS.spQuickTootMacro.value.split("\n")
            .let { list ->
                (0..5).map { list.elementAtOrNull(it) ?: "" }
            }
    }
    
    // Create separate state for each macro input
    val macroStates = remember {
        initialStrings.map { mutableStateOf(it) }
    }

    // Save on dismiss/close
    fun save() {
        val newString = macroStates.joinToString("\n") { 
            it.value.replace("\n", " ") 
        }
        PrefS.spQuickTootMacro.value = newString
    }

    Dialog(onDismissRequest = {
        save()
        onClose()
    }) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Visibility section
                Text(
                    text = stringResource(R.string.visibility),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                
                var showVisibilityMenu by remember { mutableStateOf(false) }
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showVisibilityMenu = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    ) {
                        Text(getVisibilityCaption(context, false, visibility).toString())
                    }
                    
                    DropdownMenu(
                        expanded = showVisibilityMenu,
                        onDismissRequest = { showVisibilityMenu = false },
                    ) {
                        val visibilityList = listOf(
                            TootVisibility.AccountSetting,
                            TootVisibility.WebSetting,
                            TootVisibility.Public,
                            TootVisibility.UnlistedHome,
                            TootVisibility.PrivateFollowers,
                            TootVisibility.DirectSpecified,
                        )
                        visibilityList.forEach { vis ->
                            DropdownMenuItem(
                                text = {
                                    Text(getVisibilityCaption(context, false, vis).toString())
                                },
                                onClick = {
                                    showVisibilityMenu = false
                                    onVisibilityPick(vis)
                                }
                            )
                        }
                    }
                }

                // Fixed phrase section
                Text(
                    text = stringResource(R.string.fixed_phrase),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                // Render each macro row
                macroStates.forEachIndexed { _, state ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = state.value,
                            onValueChange = { state.value = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = { 
                                save()
                                onUseMacro(state.value) 
                            },
                        ) {
                            Text(stringResource(R.string.input))
                        }
                    }
                }

                // Close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = {
                        save()
                        onClose()
                    }) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
    }
}
