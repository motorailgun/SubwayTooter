package es.ariaontheplanet.quasar.actmutedpseudoaccount

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
import es.ariaontheplanet.quasar.table.UserRelation
import es.ariaontheplanet.quasar.ui.common.MuteItemRow
import jp.juggler.util.coroutine.launchAndShowError

@Composable
fun MutedPseudoAccountScreen() {
    val activity = LocalActivity.current as? androidx.activity.ComponentActivity
    val viewModel: MutedPseudoAccountViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(state.items, key = { it.id }) { item ->
            MuteItemRow(item.whoId) {
                onDelete(activity, viewModel, item)
            }
        }
    }
}

private fun onDelete(
    activity: androidx.activity.ComponentActivity?,
    viewModel: MutedPseudoAccountViewModel,
    item: UserRelation,
) {
    activity ?: return
    activity.launchAndShowError {
        activity.confirm(R.string.delete_confirm, item.whoId)
        viewModel.delete(item)
    }
}
