package es.ariaontheplanet.quasar.actmutedword

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.dialog.DlgConfirm.confirm
import es.ariaontheplanet.quasar.table.MutedWord
import es.ariaontheplanet.quasar.ui.common.MuteItemRow
import jp.juggler.util.coroutine.launchAndShowError

@Composable
fun MutedWordScreen() {
    val activity = LocalActivity.current as? ComponentActivity
    val viewModel: MutedWordViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(state.items, key = { it.name }) { item ->
            MuteItemRow(item.name) {
                onDelete(activity, viewModel, item)
            }
        }
        item {
            Text(
                text = stringResource(R.string.refresh_after_ummute),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontSize = 12.sp,
            )
        }
    }
}

private fun onDelete(
    activity: ComponentActivity?,
    viewModel: MutedWordViewModel,
    item: MutedWord,
) {
    activity ?: return
    activity.launchAndShowError {
        activity.confirm(R.string.delete_confirm, item.name)
        viewModel.delete(item)
    }
}
