package es.ariaontheplanet.quasar.actmutedapp

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.dialog.DlgConfirm.confirm
import es.ariaontheplanet.quasar.table.MutedApp
import es.ariaontheplanet.quasar.ui.common.MuteItemRow
import jp.juggler.util.coroutine.launchAndShowError

@Composable
fun MutedAppScreen() {
    val activity = LocalActivity.current as? ComponentActivity
    val viewModel: MutedAppViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(state.items, key = { it.name }) { item ->
            MuteItemRow(item.name) {
                onDelete(activity, viewModel, item)
            }
        }
    }
}

private fun onDelete(
    activity: ComponentActivity?,
    viewModel: MutedAppViewModel,
    item: MutedApp,
) {
    activity ?: return
    activity.launchAndShowError {
        activity.confirm(R.string.delete_confirm, item.name)
        viewModel.delete(item)
    }
}
