package es.ariaontheplanet.quasar.acthighlightwordlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.ariaontheplanet.quasar.App1
import es.ariaontheplanet.quasar.table.HighlightWord
import es.ariaontheplanet.quasar.table.daoHighlightWord
import jp.juggler.util.coroutine.AppDispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HighlightWordListUiState(
    val items: List<HighlightWord> = emptyList(),
)

class HighlightWordListViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(HighlightWordListUiState())
    val uiState: StateFlow<HighlightWordListUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            val list = withContext(AppDispatchers.IO) { daoHighlightWord.listAll() }
            _uiState.update { it.copy(items = list) }
        }
    }

    suspend fun delete(item: HighlightWord) {
        val ctx = getApplication<Application>()
        withContext(AppDispatchers.IO) {
            daoHighlightWord.delete(ctx, item)
        }
        _uiState.update { state ->
            state.copy(items = state.items.filterNot { it == item })
        }
        App1.getAppState(ctx).enableSpeech()
    }
}
