package es.ariaontheplanet.quasar.ui.nickname

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.actnickname.NicknameViewModel
import es.ariaontheplanet.quasar.dialog.dialogColorPicker
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.data.mayUri
import jp.juggler.util.data.notEmpty
import jp.juggler.util.data.notZero
import jp.juggler.util.ui.decodeRingtonePickerResult

@Composable
fun NicknameScreen(
    acctAscii: String,
    acctPretty: String,
    showNotificationSound: Boolean,
) {
    val activity = LocalActivity.current as? ComponentActivity

    val viewModel: NicknameViewModel = viewModel(
        key = acctAscii,
        factory = viewModelFactory {
            initializer { NicknameViewModel(acctAscii) }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { r ->
        r.decodeRingtonePickerResult?.let { uri ->
            viewModel.setNotificationSoundUri(uri.toString())
        }
    }

    fun openRingtonePicker() {
        activity ?: return
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, R.string.notification_sound)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
            state.notificationSoundUri.mayUri()?.let {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it)
            }
        }
        val chooser = Intent.createChooser(intent, activity.getString(R.string.notification_sound))
        ringtoneLauncher.launch(chooser)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Preview
            Text(stringResource(R.string.preview))
            val previewText = state.nickname.trim().notEmpty() ?: acctPretty
            val textColor = state.colorFg.notZero()
                ?.let { ComposeColor(it.toLong() or 0xFF000000L) }
                ?: MaterialTheme.colorScheme.onSurface
            val bgComposeColor = when (state.colorBg) {
                0 -> ComposeColor.Transparent
                else -> ComposeColor(state.colorBg.toLong() or 0xFF000000L)
            }
            Text(
                text = previewText,
                fontSize = 20.sp,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = LocalTextStyle.current.copy(background = bgComposeColor),
            )

            HorizontalDivider()
            Text(stringResource(R.string.acct))
            Text(acctPretty)

            HorizontalDivider()
            Text(stringResource(R.string.nickname))
            OutlinedTextField(
                value = state.nickname,
                onValueChange = { viewModel.setNickname(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            HorizontalDivider()
            Text(stringResource(R.string.text_color))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    activity ?: return@Button
                    activity.launchAndShowError {
                        viewModel.setColorFg(
                            Color.BLACK or activity.dialogColorPicker(
                                colorInitial = state.colorFg.notZero(),
                                alphaEnabled = false,
                            )
                        )
                    }
                }) { Text(stringResource(R.string.edit)) }
                Button(onClick = { viewModel.setColorFg(0) }) {
                    Text(stringResource(R.string.reset))
                }
            }

            HorizontalDivider()
            Text(stringResource(R.string.background_color))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    activity ?: return@Button
                    activity.launchAndShowError {
                        viewModel.setColorBg(
                            Color.BLACK or activity.dialogColorPicker(
                                colorInitial = state.colorBg.notZero(),
                                alphaEnabled = false,
                            )
                        )
                    }
                }) { Text(stringResource(R.string.edit)) }
                Button(onClick = { viewModel.setColorBg(0) }) {
                    Text(stringResource(R.string.reset))
                }
            }

            if (showNotificationSound) {
                HorizontalDivider()
                Text(stringResource(R.string.notification_sound))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { openRingtonePicker() }) {
                        Text(stringResource(R.string.edit))
                    }
                    Button(onClick = { viewModel.setNotificationSoundUri("") }) {
                        Text(stringResource(R.string.reset))
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.nickname_applied_after_reload),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TextButton(
                onClick = {
                    activity?.setResult(Activity.RESULT_CANCELED)
                    activity?.finish()
                },
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.discard)) }
            TextButton(
                onClick = {
                    activity ?: return@TextButton
                    activity.launchAndShowError {
                        viewModel.save()
                        activity.setResult(Activity.RESULT_OK)
                        activity.finish()
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.save)) }
        }
    }
}
