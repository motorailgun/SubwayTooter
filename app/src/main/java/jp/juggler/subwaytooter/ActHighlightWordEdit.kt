package jp.juggler.subwaytooter

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.dialog.dialogColorPicker
import jp.juggler.subwaytooter.compose.StScreen
import jp.juggler.subwaytooter.table.HighlightWord
import jp.juggler.subwaytooter.table.daoHighlightWord
import jp.juggler.util.backPressed
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.data.decodeJsonObject
import jp.juggler.util.data.mayUri
import jp.juggler.util.data.notEmpty
import jp.juggler.util.data.notZero
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.long
import jp.juggler.util.string
import jp.juggler.util.ui.ActivityResultHandler
import jp.juggler.util.ui.decodeRingtonePickerResult

class ActHighlightWordEdit : ComponentActivity() {

    companion object {

        internal val log = LogCategory("ActHighlightWordEdit")

        private const val STATE_ITEM = "item"
        private const val EXTRA_ITEM_ID = "itemId"
        private const val EXTRA_INITIAL_TEXT = "initialText"

        fun createIntent(activity: Activity, itemId: Long) =
            Intent(activity, ActHighlightWordEdit::class.java).apply {
                putExtra(EXTRA_ITEM_ID, itemId)
            }

        fun createIntent(activity: Activity, initialText: String) =
            Intent(activity, ActHighlightWordEdit::class.java).apply {
                putExtra(EXTRA_INITIAL_TEXT, initialText)
            }
    }

    internal lateinit var item: HighlightWord

    private val nameState = mutableStateOf("")
    private val colorFgState = mutableIntStateOf(0)
    private val colorBgState = mutableIntStateOf(0)
    private val soundEnabledState = mutableStateOf(false)
    private val speechEnabledState = mutableStateOf(false)

    private val arNotificationSound = ActivityResultHandler(log) { r ->
        r.decodeRingtonePickerResult?.let { uri ->
            item.sound_uri = uri.toString()
            item.sound_type = HighlightWord.SOUND_TYPE_CUSTOM
            syncFromItem()
        }
    }

    private fun syncFromItem() {
        soundEnabledState.value = item.sound_type != HighlightWord.SOUND_TYPE_NONE
        speechEnabledState.value = item.speech != 0
        colorFgState.intValue = item.color_fg
        colorBgState.intValue = item.color_bg
    }

    private fun syncToItem() {
        item.name = nameState.value.trim { it <= ' ' || it == '　' }
        item.sound_type = when {
            !soundEnabledState.value -> HighlightWord.SOUND_TYPE_NONE
            item.sound_uri?.notEmpty() == null -> HighlightWord.SOUND_TYPE_DEFAULT
            else -> HighlightWord.SOUND_TYPE_CUSTOM
        }
        item.speech = when (speechEnabledState.value) {
            false -> 0
            else -> 1
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backPressed {
            AlertDialog.Builder(this)
                .setCancelable(true)
                .setMessage(R.string.discard_changes)
                .setPositiveButton(R.string.no, null)
                .setNegativeButton(R.string.yes) { _, _ -> finish() }
                .show()
        }

        arNotificationSound.register(this)
        App1.setActivityTheme(this)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        setResult(RESULT_CANCELED)

        launchAndShowError {
            fun loadData(): HighlightWord? {
                savedInstanceState?.getString(STATE_ITEM)
                    ?.decodeJsonObject()
                    ?.let { return HighlightWord(it) }
                intent?.string(EXTRA_INITIAL_TEXT)
                    ?.let { return HighlightWord(it) }
                intent?.long(EXTRA_ITEM_ID)
                    ?.let { return daoHighlightWord.load(it) }
                return null
            }

            val item = loadData()
            if (item == null) {
                log.d("missing source data")
                finish()
                return@launchAndShowError
            }

            this@ActHighlightWordEdit.item = item
            nameState.value = item.name ?: ""
            syncFromItem()

            setContent {
                StScreen(
                    title = stringResource(R.string.highlight_word),
                    onBack = { finish() },
                ) { contentPadding ->
                    HighlightEditContent(Modifier.padding(contentPadding))
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        try {
            syncToItem()
        } catch (ex: Throwable) {
            log.e(ex, "syncToItem failed.")
        }
        item.encodeJson().toString().let { outState.putString(STATE_ITEM, it) }
    }

    @Composable
    private fun HighlightEditContent(modifier: Modifier) {
        Column(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Keyword
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

                // Text color
                HorizontalDivider()
                Text(stringResource(R.string.text_color))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        launchAndShowError {
                            item.color_fg = Color.BLACK or dialogColorPicker(
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

                // Background color
                HorizontalDivider()
                Text(stringResource(R.string.background_color))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        launchAndShowError {
                            item.color_bg = dialogColorPicker(
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

                // Notification sound
                HorizontalDivider()
                Text(stringResource(R.string.notification_sound))
                val soundEnabled by soundEnabledState
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = {
                        soundEnabledState.value = it
                        if (!it) {
                            item.sound_type = HighlightWord.SOUND_TYPE_NONE
                        } else if (item.sound_uri?.notEmpty() == null) {
                            item.sound_type = HighlightWord.SOUND_TYPE_DEFAULT
                        } else {
                            item.sound_type = HighlightWord.SOUND_TYPE_CUSTOM
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
                        syncFromItem()
                    }) { Text(stringResource(R.string.reset)) }
                    Button(onClick = {
                        ActHighlightWordList.sound(this@ActHighlightWordEdit, item)
                    }) { Text(stringResource(R.string.test)) }
                }

                // Speech
                HorizontalDivider()
                Text(stringResource(R.string.enable_speech))
                val speechEnabled by speechEnabledState
                Switch(
                    checked = speechEnabled,
                    onCheckedChange = { speechEnabledState.value = it },
                )
            }

            // Bottom button bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TextButton(
                    onClick = { finish() },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.discard)) }
                TextButton(
                    onClick = { save() },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.save)) }
            }
        }
    }

    private fun openNotificationSoundPicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, R.string.notification_sound)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)

        item.sound_uri.mayUri()?.let { uri ->
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri)
        }

        val chooser = Intent.createChooser(intent, getString(R.string.notification_sound))
        arNotificationSound.launch(chooser)
    }

    private fun save() {
        launchAndShowError {
            syncToItem()
            val name = item.name

            if (name.isNullOrBlank()) {
                showToast(true, R.string.cant_leave_empty_keyword)
                return@launchAndShowError
            }

            val other = daoHighlightWord.load(name)
            if (other != null && other.id != item.id) {
                showToast(true, R.string.cant_save_duplicated_keyword)
                return@launchAndShowError
            }

            daoHighlightWord.save(applicationContext, item)
            App1.getAppState(applicationContext).enableSpeech()
            showToast(false, R.string.saved)
            setResult(RESULT_OK)
            finish()
        }
    }
}
