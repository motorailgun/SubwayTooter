package es.ariaontheplanet.quasar.ui.highlightWord

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.ariaontheplanet.quasar.ActHighlightWordEdit
import es.ariaontheplanet.quasar.App1
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.acthighlightwordlist.HighlightWordListViewModel
import es.ariaontheplanet.quasar.dialog.DlgConfirm.confirm
import es.ariaontheplanet.quasar.services.DedupMode
import es.ariaontheplanet.quasar.table.HighlightWord
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.data.notBlank
import jp.juggler.util.data.notZero

@Composable
fun HighlightWordListScreen() {
    val activity = LocalActivity.current as? ComponentActivity
    val context = LocalContext.current
    val viewModel: HighlightWordListViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val editLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { r ->
        if (r.resultCode == Activity.RESULT_OK) viewModel.reload()
    }

    // Stop the ringtone when the screen leaves composition.
    DisposableEffect(Unit) {
        onDispose { stopLastHighlightRingtone() }
    }

    Column(modifier = Modifier) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.items, key = { it.id }) { item ->
                HighlightWordRow(
                    item = item,
                    onOpenEdit = {
                        activity?.let {
                            editLauncher.launch(ActHighlightWordEdit.createIntent(it, item.id))
                        }
                    },
                    onSpeech = {
                        item.name?.notBlank()?.let { name ->
                            App1.getAppState(context)
                                .addSpeech(name, dedupMode = DedupMode.None)
                        }
                    },
                    onPlaySound = { playHighlightSound(context, item) },
                    onDelete = {
                        activity ?: return@HighlightWordRow
                        activity.launchAndShowError {
                            activity.confirm(activity.getString(R.string.delete_confirm, item.name))
                            viewModel.delete(item)
                        }
                    },
                )
            }
        }
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
            IconButton(onClick = {
                activity?.let {
                    editLauncher.launch(ActHighlightWordEdit.createIntent(it, ""))
                }
            }) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.new_item),
                )
            }
        }
    }
}

@Composable
private fun HighlightWordRow(
    item: HighlightWord,
    onOpenEdit: () -> Unit,
    onSpeech: () -> Unit,
    onPlaySound: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenEdit() }
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
                    if (bgColor != Color.Transparent) Modifier.padding(4.dp) else Modifier
                ),
            style = LocalTextStyle.current.copy(background = bgColor),
        )
        IconButton(
            onClick = onSpeech,
            enabled = item.speech != 0,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_comment),
                contentDescription = stringResource(R.string.speech),
            )
        }
        IconButton(
            onClick = onPlaySound,
            enabled = item.sound_type != HighlightWord.SOUND_TYPE_NONE,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_volume_up),
                contentDescription = stringResource(R.string.check_sound),
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.delete),
            )
        }
    }
}
