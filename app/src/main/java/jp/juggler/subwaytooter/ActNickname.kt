package jp.juggler.subwaytooter

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jrummyapps.android.colorpicker.dialogColorPicker
import jp.juggler.subwaytooter.api.entity.Acct
import jp.juggler.subwaytooter.compose.StScreen
import jp.juggler.subwaytooter.table.AcctColor
import jp.juggler.subwaytooter.table.daoAcctColor
import jp.juggler.subwaytooter.util.getStColorTheme
import jp.juggler.util.backPressed
import jp.juggler.util.boolean
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.data.mayUri
import jp.juggler.util.data.notEmpty
import jp.juggler.util.data.notZero
import jp.juggler.util.log.LogCategory
import jp.juggler.util.string
import jp.juggler.util.ui.ActivityResultHandler
import jp.juggler.util.ui.decodeRingtonePickerResult

class ActNickname : ComponentActivity() {

    companion object {
        private val log = LogCategory("ActNickname")

        internal const val EXTRA_ACCT_ASCII = "acctAscii"
        internal const val EXTRA_ACCT_PRETTY = "acctPretty"
        internal const val EXTRA_SHOW_NOTIFICATION_SOUND = "show_notification_sound"

        fun createIntent(
            activity: Activity,
            fullAcct: Acct,
            bShowNotificationSound: Boolean,
        ) = Intent(activity, ActNickname::class.java).apply {
            putExtra(EXTRA_ACCT_ASCII, fullAcct.ascii)
            putExtra(EXTRA_ACCT_PRETTY, fullAcct.pretty)
            putExtra(EXTRA_SHOW_NOTIFICATION_SOUND, bShowNotificationSound)
        }
    }

    private val acctAscii by lazy {
        intent?.string(EXTRA_ACCT_ASCII)!!
    }
    private val acctPretty by lazy {
        intent?.string(EXTRA_ACCT_PRETTY)!!
    }
    private val showNotificationSound by lazy {
        intent?.boolean(EXTRA_SHOW_NOTIFICATION_SOUND) ?: false
    }

    private val nicknameState = mutableStateOf("")
    private val colorFgState = mutableIntStateOf(0)
    private val colorBgState = mutableIntStateOf(0)
    private var notificationSoundUri: String? = null

    private val arNotificationSound = ActivityResultHandler(log) { r ->
        r.decodeRingtonePickerResult?.let { uri ->
            notificationSoundUri = uri.toString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backPressed {
            setResult(RESULT_OK)
            finish()
        }
        arNotificationSound.register(this)
        App1.setActivityTheme(this)
        val stColorScheme = getStColorTheme()

        load()

        setContent {
            val subtitle = stringResource(
                when {
                    showNotificationSound -> R.string.nickname_and_color_and_notification_sound
                    else -> R.string.nickname_and_color
                }
            )
            StScreen(
                stColorScheme = stColorScheme,
                title = subtitle,
                onBack = {
                    setResult(RESULT_OK)
                    finish()
                },
            ) { contentPadding ->
                NicknameContent(Modifier.padding(contentPadding))
            }
        }
    }

    @Composable
    private fun NicknameContent(modifier: Modifier) {
        Column(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Preview
                Text(stringResource(R.string.preview))
                val nickname by nicknameState
                val fgColor = colorFgState.intValue
                val bgColor = colorBgState.intValue
                val previewText = nickname.trim().notEmpty() ?: acctPretty
                val textColor = fgColor.notZero()
                    ?.let { androidx.compose.ui.graphics.Color(it.toLong() or 0xFF000000L) }
                    ?: MaterialTheme.colorScheme.onSurface
                val bgComposeColor = when (bgColor) {
                    0 -> androidx.compose.ui.graphics.Color.Transparent
                    else -> androidx.compose.ui.graphics.Color(bgColor.toLong() or 0xFF000000L)
                }
                Text(
                    text = previewText,
                    fontSize = 20.sp,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = LocalTextStyle.current.copy(background = bgComposeColor),
                )

                // Acct
                HorizontalDivider()
                Text(stringResource(R.string.acct))
                Text(acctPretty)

                // Nickname
                HorizontalDivider()
                Text(stringResource(R.string.nickname))
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nicknameState.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Text color
                HorizontalDivider()
                Text(stringResource(R.string.text_color))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        launchAndShowError {
                            colorFgState.intValue = Color.BLACK or dialogColorPicker(
                                colorInitial = colorFgState.intValue.notZero(),
                                alphaEnabled = false,
                            )
                        }
                    }) { Text(stringResource(R.string.edit)) }
                    Button(onClick = {
                        colorFgState.intValue = 0
                    }) { Text(stringResource(R.string.reset)) }
                }

                // Background color
                HorizontalDivider()
                Text(stringResource(R.string.background_color))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        launchAndShowError {
                            colorBgState.intValue = Color.BLACK or dialogColorPicker(
                                colorInitial = colorBgState.intValue.notZero(),
                                alphaEnabled = false,
                            )
                        }
                    }) { Text(stringResource(R.string.edit)) }
                    Button(onClick = {
                        colorBgState.intValue = 0
                    }) { Text(stringResource(R.string.reset)) }
                }

                // Notification sound
                if (showNotificationSound) {
                    HorizontalDivider()
                    Text(stringResource(R.string.notification_sound))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { openNotificationSoundPicker() }) {
                            Text(stringResource(R.string.edit))
                        }
                        Button(onClick = { notificationSoundUri = "" }) {
                            Text(stringResource(R.string.reset))
                        }
                    }
                }
            }

            // Info text
            Text(
                text = stringResource(R.string.nickname_applied_after_reload),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            // Bottom button bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TextButton(
                    onClick = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.discard)) }
                TextButton(
                    onClick = {
                        save()
                        setResult(Activity.RESULT_OK)
                        finish()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.save)) }
            }
        }
    }

    private fun load() {
        val ac = daoAcctColor.load(acctAscii)
        colorBgState.intValue = ac.colorBg
        colorFgState.intValue = ac.colorFg
        nicknameState.value = ac.nickname ?: ""
        notificationSoundUri = ac.notificationSound
    }

    private fun save() {
        launchAndShowError {
            daoAcctColor.save(
                System.currentTimeMillis(),
                AcctColor(
                    acctAscii = acctAscii,
                    nicknameSave = nicknameState.value.trim { it <= ' ' },
                    colorFg = colorFgState.intValue,
                    colorBg = colorBgState.intValue,
                    notificationSoundSaved = notificationSoundUri ?: "",
                )
            )
        }
    }

    private fun openNotificationSoundPicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, R.string.notification_sound)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
        notificationSoundUri.mayUri()?.let {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it)
        }
        val chooser = Intent.createChooser(intent, getString(R.string.notification_sound))
        arNotificationSound.launch(chooser)
    }
}
