package es.ariaontheplanet.quasar.actcolumnlist

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.ariaontheplanet.quasar.ActColumnList
import es.ariaontheplanet.quasar.AppState
import es.ariaontheplanet.quasar.api.entity.Acct
import es.ariaontheplanet.quasar.column.ColumnEncoder
import es.ariaontheplanet.quasar.column.ColumnType
import jp.juggler.util.coroutine.AppDispatchers
import jp.juggler.util.data.JsonObject
import jp.juggler.util.data.toJsonArray
import jp.juggler.util.log.LogCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ColumnListItem(
    val json: JsonObject,
    val id: Long,
    val name: String = json.optString(ColumnEncoder.KEY_COLUMN_NAME),
    val acct: Acct = Acct.parse(json.optString(ColumnEncoder.KEY_COLUMN_ACCESS_ACCT)),
    val acctName: String = json.optString(ColumnEncoder.KEY_COLUMN_ACCESS_STR),
    val oldIndex: Int = json.optInt(ColumnEncoder.KEY_OLD_INDEX),
    val type: ColumnType = ColumnType.parse(json.optInt(ColumnEncoder.KEY_TYPE)),
    val acctColorBg: Int = json.optInt(ColumnEncoder.KEY_COLUMN_ACCESS_COLOR_BG, 0),
    val acctColorFg: Int = json.optInt(ColumnEncoder.KEY_COLUMN_ACCESS_COLOR, 0),
    val columnColorFg: Int = json.optInt(ColumnEncoder.KEY_HEADER_TEXT_COLOR, 0),
    val columnColorBg: Int = json.optInt(ColumnEncoder.KEY_HEADER_BACKGROUND_COLOR, 0),
    val bOldSelection: Boolean = false,
)

data class ColumnListUiState(
    val items: List<ColumnListItem> = emptyList(),
    val oldSelection: Int = -1,
)

class ColumnListViewModel(
    app: Application,
    initialSelection: Int,
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(ColumnListUiState(oldSelection = initialSelection))
    val uiState: StateFlow<ColumnListUiState> = _uiState.asStateFlow()

    init {
        restore(initialSelection)
    }

    private fun restore(selection: Int) {
        viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext
            val loaded = withContext(AppDispatchers.IO) {
                try {
                    AppState.loadColumnList(ctx, ActColumnList.TMP_FILE_COLUMN_LIST)
                        ?.objectList()
                        ?.mapIndexedNotNull { index, src ->
                            runCatching {
                                val item = ColumnListItem(src, index.toLong())
                                item.copy(bOldSelection = selection == item.oldIndex)
                            }.onFailure { log.e(it, "restore: decode failed.") }
                                .getOrNull()
                        } ?: emptyList()
                } catch (ex: Throwable) {
                    log.e(ex, "restore failed.")
                    emptyList()
                }
            }
            _uiState.update { it.copy(items = loaded, oldSelection = selection) }
        }
    }

    fun moveItem(from: Int, to: Int) {
        val current = _uiState.value.items
        if (to < 0 || to >= current.size || from < 0 || from >= current.size) return
        val mutable = current.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        _uiState.update { it.copy(items = mutable) }
    }

    fun deleteItem(item: ColumnListItem) {
        _uiState.update { state ->
            state.copy(items = state.items.filterNot { it.id == item.id })
        }
    }

    suspend fun persistColumnsToTempFile() {
        val ctx = getApplication<Application>().applicationContext
        val array = _uiState.value.items.map { it.json }.toJsonArray()
        withContext(AppDispatchers.IO) {
            AppState.saveColumnList(ctx, ActColumnList.TMP_FILE_COLUMN_LIST, array)
        }
    }

    fun buildResult(newSelection: Int): Intent {
        val intent = Intent()
        val items = _uiState.value.items

        if (newSelection in items.indices) {
            intent.putExtra(ActColumnList.EXTRA_SELECTION, newSelection)
        } else {
            val oldSel = items.indexOfFirst { it.bOldSelection }
            if (oldSel >= 0) intent.putExtra(ActColumnList.EXTRA_SELECTION, oldSel)
        }

        val orderList = items.mapTo(ArrayList()) { it.oldIndex }
        intent.putExtra(ActColumnList.EXTRA_ORDER, orderList)
        return intent
    }

    companion object {
        private val log = LogCategory("ColumnListViewModel")
    }
}
