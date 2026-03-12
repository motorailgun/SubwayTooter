package jp.juggler.subwaytooter.dialog

import android.app.Dialog
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.entity.TootVisibility
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.subwaytooter.getVisibilityCaption
import jp.juggler.subwaytooter.pref.PrefS
import jp.juggler.util.data.notEmpty
import jp.juggler.util.ui.dismissSafe
import java.lang.ref.WeakReference

class DlgQuickTootMenu(
    internal val activity: ActMain,
    internal val callback: Callback,
) {
    companion object {
        val visibilityList = arrayOf(
            TootVisibility.AccountSetting,
            TootVisibility.WebSetting,
            TootVisibility.Public,
            TootVisibility.UnlistedHome,
            TootVisibility.PrivateFollowers,
            TootVisibility.DirectSpecified,
        )
    }

    interface Callback {
        fun onMacro(text: String)
        var visibility: TootVisibility
    }

    private var refDialog: WeakReference<Dialog>? = null

    private fun loadStrings() =
        PrefS.spQuickTootMacro.value.split("\n")

    private fun saveStrings(newValue: String) {
        PrefS.spQuickTootMacro.value = newValue
    }

    private fun show() {
        val initialStrings = loadStrings()
        val dialog = Dialog(activity).also { refDialog = WeakReference(it) }

        // Mutable state holders for 6 macro text fields
        val macroStates = (0..5).map { i ->
            mutableStateOf(initialStrings.elementAtOrNull(i) ?: "")
        }
        val visibilityState = mutableStateOf(callback.visibility)

        dialog.setOnDismissListener {
            saveStrings(
                macroStates.map { it.value.replace("\n", " ") }
                    .joinToString("\n")
            )
        }

        val composeView = ComposeView(activity).apply {
            setContent {
                StThemedContent {
                    QuickTootMenuContent(
                        visibilityState = visibilityState.value,
                        visibilityCaption = getVisibilityCaption(
                            activity,
                            false,
                            visibilityState.value,
                        ),
                        macroValues = macroStates.map { it.value },
                        onMacroChange = { index, value ->
                            macroStates[index].value = value
                        },
                        onVisibilityPick = { newVisibility ->
                            callback.visibility = newVisibility
                            visibilityState.value = newVisibility
                        },
                        onUseMacro = { text ->
                            text.notEmpty()?.let {
                                dialog.dismissSafe()
                                callback.onMacro(it)
                            }
                        },
                        onClose = { dialog.dismissSafe() },
                    )
                }
            }
        }

        dialog.setContentView(composeView)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.apply {
            attributes = attributes.apply {
                gravity = Gravity.BOTTOM or Gravity.START
                flags = flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
            }
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
            )
        }
        dialog.show()
    }

    fun toggle() {
        val dialog = refDialog?.get()
        when {
            dialog?.isShowing == true -> dialog.dismissSafe()
            else -> show()
        }
    }
}

@Composable
private fun QuickTootMenuContent(
    visibilityState: TootVisibility,
    visibilityCaption: CharSequence,
    macroValues: List<String>,
    onMacroChange: (index: Int, value: String) -> Unit,
    onVisibilityPick: (TootVisibility) -> Unit,
    onUseMacro: (String) -> Unit,
    onClose: () -> Unit,
) {
    val showVisibilityMenu = remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Visibility section
            Text(
                text = stringResource(R.string.visibility),
                style = MaterialTheme.typography.labelLarge,
            )
            OutlinedButton(
                onClick = { showVisibilityMenu.value = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                Text(visibilityCaption.toString())
                DropdownMenu(
                    expanded = showVisibilityMenu.value,
                    onDismissRequest = { showVisibilityMenu.value = false },
                ) {
                    DlgQuickTootMenu.visibilityList.forEach { vis ->
                        DropdownMenuItem(
                            text = {
                                val context = androidx.compose.ui.platform.LocalContext.current
                                Text(
                                    getVisibilityCaption(
                                        context,
                                        false,
                                        vis,
                                    ).toString()
                                )
                            },
                            onClick = {
                                showVisibilityMenu.value = false
                                onVisibilityPick(vis)
                            },
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

            macroValues.forEachIndexed { index, value ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { onMacroChange(index, it) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = { onUseMacro(value) },
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
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}
