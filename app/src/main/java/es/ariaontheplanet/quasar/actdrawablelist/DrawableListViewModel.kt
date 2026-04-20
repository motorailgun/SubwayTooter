package es.ariaontheplanet.quasar.actdrawablelist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.ariaontheplanet.quasar.R
import jp.juggler.util.coroutine.AppDispatchers
import jp.juggler.util.log.LogCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DrawableItem(val id: Int, val name: String)

data class DrawableListUiState(
    val items: List<DrawableItem> = emptyList(),
)

class DrawableListViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(DrawableListUiState())
    val uiState: StateFlow<DrawableListUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            try {
                val rePackageSpec = """.+/""".toRegex()
                val reSkipName =
                    """^(m3_|abc_|avd_|btn_checkbox_|btn_radio_|googleg_|ic_keyboard_arrow_|ic_menu_arrow_|notification_|common_|emj_|cpv_|design_|exo_|mtrl_|ic_mtrl_)"""
                        .toRegex()
                val resources = getApplication<Application>().resources
                val list = withContext(AppDispatchers.IO) {
                    R.drawable::class.java.fields
                        .mapNotNull {
                            val id = it.get(null) as? Int ?: return@mapNotNull null
                            val name = resources.getResourceName(id).replaceFirst(rePackageSpec, "")
                            if (reSkipName.containsMatchIn(name)) return@mapNotNull null
                            DrawableItem(id, name)
                        }
                        .sortedBy { it.name }
                }
                _uiState.update { it.copy(items = list) }
            } catch (ex: Throwable) {
                log.e(ex, "load failed.")
            }
        }
    }

    companion object {
        private val log = LogCategory("DrawableListViewModel")
    }
}
