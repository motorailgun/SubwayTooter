package es.ariaontheplanet.quasar.actfavmute

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.ariaontheplanet.quasar.api.entity.Acct
import es.ariaontheplanet.quasar.table.daoFavMute
import jp.juggler.util.coroutine.AppDispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FavMuteItem(val id: Long, val acct: Acct)

data class FavMuteUiState(
    val items: List<FavMuteItem> = emptyList(),
)

class FavMuteViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FavMuteUiState())
    val uiState: StateFlow<FavMuteUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            val list = withContext(AppDispatchers.IO) {
                daoFavMute.listAll().map { FavMuteItem(id = it.id, acct = Acct.parse(it.acct)) }
            }
            _uiState.update { it.copy(items = list) }
        }
    }

    suspend fun delete(item: FavMuteItem) {
        withContext(AppDispatchers.IO) { daoFavMute.delete(item.acct) }
        _uiState.update { state ->
            state.copy(items = state.items.filterNot { it.id == item.id })
        }
    }
}
