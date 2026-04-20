package es.ariaontheplanet.quasar.actmutedapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.ariaontheplanet.quasar.table.MutedApp
import es.ariaontheplanet.quasar.table.appDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MutedAppUiState(
    val items: List<MutedApp> = emptyList(),
)

class MutedAppViewModel(app: Application) : AndroidViewModel(app) {

    private val dao by lazy { MutedApp.Access(appDatabase) }

    private val _uiState = MutableStateFlow(MutedAppUiState())
    val uiState: StateFlow<MutedAppUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { dao.listAll() }
            _uiState.update { it.copy(items = list) }
        }
    }

    suspend fun delete(item: MutedApp) {
        withContext(Dispatchers.IO) { dao.delete(item.name) }
        _uiState.update { state ->
            state.copy(items = state.items.filterNot { it.name == item.name })
        }
    }
}
