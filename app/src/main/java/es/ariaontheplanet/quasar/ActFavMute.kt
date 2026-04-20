package es.ariaontheplanet.quasar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.ariaontheplanet.quasar.actfavmute.FavMuteItem
import es.ariaontheplanet.quasar.actfavmute.FavMuteViewModel
import es.ariaontheplanet.quasar.dialog.DlgConfirm.confirm
import jp.juggler.util.backPressed
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.log.LogCategory
import org.koin.androidx.viewmodel.ext.android.viewModel

class ActFavMute : ComponentActivity() {

    companion object {
        private val log = LogCategory("ActFavMute")
    }

    private val viewModel: FavMuteViewModel by viewModel()

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
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.items, key = { it.id }) { item ->
                    MuteItemRow(item.acct.pretty) { onDelete(item) }
                }
                item {
                    Text(
                        text = stringResource(R.string.fav_muted_user_desc),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }

    private fun onDelete(item: FavMuteItem) {
        launchAndShowError {
            confirm(R.string.delete_confirm, item.acct.pretty)
            viewModel.delete(item)
        }
    }
}
