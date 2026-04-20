package es.ariaontheplanet.quasar.actmutedpseudoaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.ariaontheplanet.quasar.table.UserRelation
import es.ariaontheplanet.quasar.table.daoUserRelation
import jp.juggler.util.coroutine.AppDispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MutedPseudoAccountUiState(
    val items: List<UserRelation> = emptyList(),
)

class MutedPseudoAccountViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MutedPseudoAccountUiState())
    val uiState: StateFlow<MutedPseudoAccountUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            val list = withContext(AppDispatchers.IO) {
                daoUserRelation.listPseudoMuted()
            }
            _uiState.update { it.copy(items = list) }
        }
    }

    suspend fun delete(item: UserRelation) {
        withContext(AppDispatchers.IO) {
            daoUserRelation.deletePseudo(item.id)
        }
        _uiState.update { state ->
            state.copy(items = state.items.filterNot { it.id == item.id })
        }
    }
}
