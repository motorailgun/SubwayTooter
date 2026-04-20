package es.ariaontheplanet.quasar.actmutedword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.ariaontheplanet.quasar.table.MutedWord
import es.ariaontheplanet.quasar.table.daoMutedWord
import jp.juggler.util.coroutine.AppDispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MutedWordUiState(
    val items: List<MutedWord> = emptyList(),
)

class MutedWordViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MutedWordUiState())
    val uiState: StateFlow<MutedWordUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            val list = withContext(AppDispatchers.IO) { daoMutedWord.listAll() }
            _uiState.update { it.copy(items = list) }
        }
    }

    suspend fun delete(item: MutedWord) {
        withContext(AppDispatchers.IO) { daoMutedWord.delete(item.name) }
        _uiState.update { state ->
            state.copy(items = state.items.filterNot { it.name == item.name })
        }
    }
}
