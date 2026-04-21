package es.ariaontheplanet.quasar.ui.highlightWord

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.ariaontheplanet.quasar.App1
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.dialog.dialogColorPicker
import es.ariaontheplanet.quasar.table.HighlightWord
import es.ariaontheplanet.quasar.table.daoHighlightWord
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.data.mayUri
import jp.juggler.util.data.notEmpty
import jp.juggler.util.data.notZero
import jp.juggler.util.log.showToast
import jp.juggler.util.ui.decodeRingtonePickerResult

@Composable
fun HighlightWordEditScreen(itemId: Long, initialText: String) {
    val activity = LocalActivity.current as? ComponentActivity
    val item = remember { mutableStateOf<HighlightWord?>(null) }

    LaunchedEffect(itemId, initialText) {
        val loaded = when {
            itemId > 0L -> daoHighlightWord.load(itemId)
            initialText.isNotEmpty() -> HighlightWord(initialText)
            else -> null
        }
        if (loaded == null) {
            activity?.finish()
            return@LaunchedEffect
        }
        item.value = loaded
    }

    val current = item.value ?: return
    HighlightWordEditContent(activity = activity, item = current)
}

@Composable
private fun HighlightWordEditContent(
    activity: ComponentActivity?,
    item: HighlightWord,
) {
    val nameState = remember(item) { mutableStateOf(item.name ?: "") }
    val colorFgState = remember(item) { mutableIntStateOf(item.color_fg) }
    val colorBgState = remember(item) { mutableIntStateOf(item.color_bg) }
    val soundEnabledState = remember(item) {
        mutableStateOf(item.sound_type != HighlightWord.SOUND_TYPE_NONE)
    }
    val speechEnabledState = remember(item) { mutableStateOf(item.speech != 0) }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { r ->
        r.decodeRingtonePickerResult?.let { uri ->
            item.sound_uri = uri.toString()
            item.sound_type = HighlightWord.SOUND_TYPE_CUSTOM
            soundEnabledState.value = true
        }
    }

    BackHandler {
        activity ?: return@BackHandler
        AlertDialog.Builder(activity)
            .setCancelable(true)
            .setMessage(R.string.discard_changes)
            .setPositiveButton(R.string.no, null)
            .setNegativeButton(R.string.yes) { _, _ -> activity.finish() }
            .show()
    }

    fun syncToItem() {
        item.name = nameState.value.trim { it <= ' ' || it == '　' }
        item.sound_type = when {
            !soundEnabledState.value -> HighlightWord.SOUND_TYPE_NONE
            item.sound_uri?.notEmpty() == null -> HighlightWord.SOUND_TYPE_DEFAULT
            else -> HighlightWord.SOUND_TYPE_CUSTOM
        }
        item.speech = if (speechEnabledState.value) 1 else 0
    }

    fun openNotificationSoundPicker() {
        activity ?: return
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, R.string.notification_sound)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
            item.sound_uri.mayUri()?.let {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it)
            }
        }
        val chooser = Intent.createChooser(intent, activity.getString(R.string.notification_sound))
        ringtoneLauncher.launch(chooser)
    }

    fun save() {
        activity ?: return
        activity.launchAndShowError {
            syncToItem()
            val name = item.name
            if (name.isNullOrBlank()) {
                activity.showToast(true, R.string.cant_leave_empty_keyword)
                return@launchAndShowError
            }
            val other = daoHighlightWord.load(name)
            if (other != null && other.id != item.id) {
                activity.showToast(true, R.string.cant_save_duplicated_keyword)
                return@launchAndShowError
            }
            daoHighlightWord.save(activity.applicationContext, item)
            App1.getAppState(activity.applicationContext).enableSpeech()
            activity.showToast(false, R.string.saved)
            activity.setResult(Activity.RESULT_OK)
            activity.finish()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HorizontalDivider()
            Text(stringResource(R.string.keyword))
            val name by nameState
            val fgColor = colorFgState.intValue
            val bgColor = colorBgState.intValue
            val textColor = fgColor.notZero()
                ?.let { androidx.compose.ui.graphics.Color(it.toLong() or 0xFF000000L) }
                ?: MaterialTheme.colorScheme.onSurface
            val bgComposeColor = when (bgColor) {
                0 -> androidx.compose.ui.graphics.Color.Transparent
                else -> androidx.compose.ui.graphics.Color(bgColor.toLong() or 0xFF000000L)
            }
            OutlinedTextField(
                value = name,
                onValueChange = { nameState.value = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedContainerColor = bgComposeColor,
                    unfocusedContainerColor = bgComposeColor,
                ),
            )

            HorizontalDivider()
            Text(stringResource(R.string.text_color))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    activity ?: return@Button
                    activity.launchAndShowError {
                        item.color_fg = Color.BLACK or activity.dialogColorPicker(
                            colorInitial = item.color_fg.notZero(),
                            alphaEnabled = false,
                        )
                        colorFgState.intValue = item.color_fg
                    }
                }) { Text(stringResource(R.string.edit)) }
                Button(onClick = {
                    item.color_fg = 0
                    colorFgState.intValue = 0
                }) { Text(stringResource(R.string.reset)) }
            }

            HorizontalDivider()
            Text(stringResource(R.string.background_color))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    activity ?: return@Button
                    activity.launchAndShowError {
                        item.color_bg = activity.dialogColorPicker(
                            colorInitial = item.color_bg.notZero(),
                            alphaEnabled = true,
                        ).notZero() ?: 0x01000000
                        colorBgState.intValue = item.color_bg
                    }
                }) { Text(stringResource(R.string.edit)) }
                Button(onClick = {
                    item.color_bg = 0
                    colorBgState.intValue = 0
                }) { Text(stringResource(R.string.reset)) }
            }

            HorizontalDivider()
            Text(stringResource(R.string.notification_sound))
            val soundEnabled by soundEnabledState
            Switch(
                checked = soundEnabled,
                onCheckedChange = {
                    soundEnabledState.value = it
                    item.sound_type = when {
                        !it -> HighlightWord.SOUND_TYPE_NONE
                        item.sound_uri?.notEmpty() == null -> HighlightWord.SOUND_TYPE_DEFAULT
                        else -> HighlightWord.SOUND_TYPE_CUSTOM
                    }
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { openNotificationSoundPicker() }) {
                    Text(stringResource(R.string.edit))
                }
                Button(onClick = {
                    item.sound_uri = null
                    item.sound_type = when {
                        soundEnabled -> HighlightWord.SOUND_TYPE_DEFAULT
                        else -> HighlightWord.SOUND_TYPE_NONE
                    }
                    soundEnabledState.value = item.sound_type != HighlightWord.SOUND_TYPE_NONE
                }) { Text(stringResource(R.string.reset)) }
                Button(onClick = {
                    activity?.let { playHighlightSound(it, item) }
                }) { Text(stringResource(R.string.test)) }
            }

            HorizontalDivider()
            Text(stringResource(R.string.enable_speech))
            val speechEnabled by speechEnabledState
            Switch(
                checked = speechEnabled,
                onCheckedChange = { speechEnabledState.value = it },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TextButton(
                onClick = { activity?.finish() },
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.discard)) }
            TextButton(
                onClick = { save() },
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.save)) }
        }
    }
}
