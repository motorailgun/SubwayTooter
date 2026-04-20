package es.ariaontheplanet.quasar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.ariaontheplanet.quasar.actmutedpseudoaccount.MutedPseudoAccountViewModel
import es.ariaontheplanet.quasar.dialog.DlgConfirm.confirm
import es.ariaontheplanet.quasar.table.UserRelation
import es.ariaontheplanet.quasar.util.provideViewModel
import jp.juggler.util.backPressed
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.log.LogCategory

class ActMutedPseudoAccount : ComponentActivity() {

    companion object {
        private val log = LogCategory("ActMutedPseudoAccount")
    }

    private val viewModel by lazy {
        provideViewModel(this) { MutedPseudoAccountViewModel() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backPressed {
            setResult(RESULT_OK)
            finish()
        }
        App1.setActivityTheme(this)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.items, key = { it.id }) { item ->
                    MuteItemRow(item.whoId) { onDelete(item) }
                }
            }
        }
    }

    private fun onDelete(item: UserRelation) {
        launchAndShowError {
            confirm(R.string.delete_confirm, item.whoId)
            viewModel.delete(item)
        }
    }
}
