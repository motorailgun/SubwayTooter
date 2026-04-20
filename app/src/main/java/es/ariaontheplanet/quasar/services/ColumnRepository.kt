package es.ariaontheplanet.quasar.services

import es.ariaontheplanet.quasar.column.Column
import es.ariaontheplanet.quasar.table.SavedAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Holds the in-memory column list and the currently-active account.
// Koin-managed single; AppState forwards to this so callers don't change.
class ColumnRepository {

    private val _columnList = ArrayList<Column>()

    private val _currentAccount = MutableStateFlow<SavedAccount?>(null)
    val currentAccount: StateFlow<SavedAccount?> = _currentAccount.asStateFlow()

    fun setCurrentAccount(account: SavedAccount?) {
        _currentAccount.value = account
    }

    // Returns a shallow snapshot — safe to iterate without holding the lock.
    val columnList: List<Column>
        get() = synchronized(_columnList) { ArrayList(_columnList) }

    val columnCount: Int
        get() = synchronized(_columnList) { _columnList.size }

    fun column(i: Int): Column? =
        synchronized(_columnList) { _columnList.elementAtOrNull(i) }

    fun columnIndex(column: Column?): Int? =
        synchronized(_columnList) { _columnList.indexOf(column).takeIf { it != -1 } }

    fun editColumnList(block: (ArrayList<Column>) -> Unit) {
        synchronized(_columnList) { block(_columnList) }
    }
}
