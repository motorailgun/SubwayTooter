package es.ariaontheplanet.quasar

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.ariaontheplanet.quasar.acthighlightwordlist.HighlightWordListViewModel
import es.ariaontheplanet.quasar.dialog.DlgConfirm.confirm
import es.ariaontheplanet.quasar.table.HighlightWord
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.data.mayUri
import jp.juggler.util.data.notBlank
import jp.juggler.util.data.notZero
import jp.juggler.util.log.LogCategory
import jp.juggler.util.ui.ActivityResultHandler
import jp.juggler.util.ui.isNotOk
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.lang.ref.WeakReference

class ActHighlightWordList : ComponentActivity() {

    companion object {
        private val log = LogCategory("ActHighlightWordList")

        private var lastRingtone: WeakReference<Ringtone>? = null

        private fun stopLastRingtone() {
            try {
                lastRingtone?.get()?.stop()
            } catch (ex: Throwable) {
                log.e(ex, "stopLastRingtone failed.")
            } finally {
                lastRingtone = null
            }
        }

        private fun tryRingTone(context: Context, uri: Uri?): Boolean {
            try {
                uri?.let { RingtoneManager.getRingtone(context, it) }
                    ?.let { ringtone ->
                        stopLastRingtone()
                        lastRingtone = WeakReference(ringtone)
                        ringtone.play()
                        return true
                    }
            } catch (ex: Throwable) {
                log.e(ex, "tryRingTone failed.")
            }
            return false
        }

        fun sound(context: Context, item: HighlightWord?) {
            if (lastRingtone?.get()?.isPlaying == true) {
                stopLastRingtone()
                return
            }

            item ?: return
            val soundType = item.sound_type
            when {
                soundType == HighlightWord.SOUND_TYPE_NONE -> Unit
                soundType == HighlightWord.SOUND_TYPE_CUSTOM && tryRingTone(
                    context,
                    item.sound_uri.mayUri()
                ) -> Unit
                else -> tryRingTone(
                    context,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                )
            }
        }
    }

    private val viewModel: HighlightWordListViewModel by viewModel()

    private val arEdit = ActivityResultHandler(log) { r ->
        if (r.isNotOk) return@ActivityResultHandler
        viewModel.reload()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arEdit.register(this)
        App1.setActivityTheme(this)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            Column(modifier = Modifier) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        HighlightWordRow(item)
                    }
                }
                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.highlight_desc),
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { create() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = stringResource(R.string.new_item),
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLastRingtone()
    }

    @Composable
    private fun HighlightWordRow(item: HighlightWord) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { edit(item) }
                .padding(horizontal = 12.dp, vertical = 3.dp)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val textColor = item.color_fg.notZero()
                ?.let { Color(it.toLong() or 0xFF000000L) }
                ?: MaterialTheme.colorScheme.onSurface
            val bgColor = when (item.color_bg) {
                0 -> Color.Transparent
                else -> Color(item.color_bg.toLong() or 0xFF000000L)
            }
            Text(
                text = item.name ?: "",
                fontSize = 20.sp,
                color = textColor,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (bgColor != Color.Transparent)
                            Modifier.padding(4.dp)
                        else Modifier
                    ),
                style = LocalTextStyle.current.copy(
                    background = bgColor,
                ),
            )
            IconButton(
                onClick = { speech(item) },
                enabled = item.speech != 0,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_comment),
                    contentDescription = stringResource(R.string.speech),
                )
            }
            IconButton(
                onClick = { sound(this@ActHighlightWordList, item) },
                enabled = item.sound_type != HighlightWord.SOUND_TYPE_NONE,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_volume_up),
                    contentDescription = stringResource(R.string.check_sound),
                )
            }
            IconButton(
                onClick = { onDelete(item) },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.delete),
                )
            }
        }
    }

    private fun create() {
        arEdit.launch(ActHighlightWordEdit.createIntent(this, ""))
    }

    private fun edit(item: HighlightWord?) {
        item ?: return
        arEdit.launch(ActHighlightWordEdit.createIntent(this, item.id))
    }

    private fun onDelete(item: HighlightWord?) {
        item ?: return
        launchAndShowError {
            confirm(getString(R.string.delete_confirm, item.name))
            viewModel.delete(item)
        }
    }

    private fun speech(item: HighlightWord?) {
        item?.name?.notBlank()?.let {
            App1.getAppState(this@ActHighlightWordList)
                .addSpeech(it, dedupMode = DedupMode.None)
        }
    }
}
