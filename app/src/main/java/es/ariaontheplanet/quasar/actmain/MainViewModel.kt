package es.ariaontheplanet.quasar.actmain

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import es.ariaontheplanet.quasar.App1
import es.ariaontheplanet.quasar.table.SavedAccount
import es.ariaontheplanet.quasar.table.daoAcctColor
import es.ariaontheplanet.quasar.column.Column
import es.ariaontheplanet.quasar.column.getIconId
import es.ariaontheplanet.quasar.column.getHeaderNameColor
import es.ariaontheplanet.quasar.column.getHeaderBackgroundColor
import es.ariaontheplanet.quasar.column.getColumnName
import es.ariaontheplanet.quasar.column.viewHolder
import es.ariaontheplanet.quasar.columnviewholder.ColumnViewHolder

import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.pref.PrefI
import es.ariaontheplanet.quasar.pref.PrefB

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Back Press Handling — fixed-columns refactor reduces this to "close drawer or
    // confirm exit". Closing or reordering columns is no longer a back-press gesture.
    sealed class BackPressEffect {
        object Finish : BackPressEffect()
        data class ShowToast(val textId: Int, val isError: Boolean = false) : BackPressEffect()
    }

    private val _backPressEffect = Channel<BackPressEffect>(Channel.CONFLATED)
    val backPressEffect = _backPressEffect.receiveAsFlow()

    // The confirm-exit dialog; null when hidden.
    data class BackDialogState(val unused: Unit = Unit)
    private val _showBackDialog = MutableStateFlow<BackDialogState?>(null)
    val showBackDialog = _showBackDialog.asStateFlow()

    fun onBackPressed() {
        if (_isDrawerOpen.value) {
            closeDrawer()
            return
        }

        // Check if any column setting is open
        var settingClosed = false
        appState.columnList.forEach { column ->
            column.viewHolder?.let { vh ->
                if (vh.isColumnSettingShown) {
                    vh.columnUiState.settingsVisible = false
                    settingClosed = true
                }
            }
        }
        if (settingClosed) return

        when (PrefI.ipBackButtonAction.value) {
            PrefI.BACK_EXIT_APP -> _backPressEffect.trySend(BackPressEffect.Finish)
            else -> _showBackDialog.value = BackDialogState()
        }
    }

    fun confirmFinish() {
        _backPressEffect.trySend(BackPressEffect.Finish)
        _showBackDialog.value = null
    }

    fun dismissBackDialog() {
        _showBackDialog.value = null
    }

    // Drawer state control (true=open, false=close)
    private val _drawerControl = Channel<Boolean>(Channel.CONFLATED)
    val drawerControl = _drawerControl.receiveAsFlow()

    // Current drawer state (reported by UI)
    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen = _isDrawerOpen.asStateFlow()

    // Request to open drawer
    fun openDrawer() {
        _drawerControl.trySend(true)
    }

    // Request to close drawer
    fun closeDrawer() {
        _drawerControl.trySend(false)
    }

    // UI reports drawer state change
    fun setDrawerOpen(isOpen: Boolean) {
        _isDrawerOpen.value = isOpen
    }

    // Current page index (for ViewPager/RecyclerView)
    private val _currentPage = MutableStateFlow(0)
    val currentPage = _currentPage.asStateFlow()

    fun setCurrentPage(page: Int) {
        _currentPage.value = page
    }

    // Column Strip Logic
    data class ColumnUiState(
        val index: Int,
        val iconId: Int,
        val acctColor: Int, // 0 if transparent/none
        val headerNameColor: Int,
        val headerBackgroundColor: Int,
        val contentDescription: String,
    )

    private val _columnList = MutableStateFlow<List<ColumnUiState>>(emptyList())
    val columnList = _columnList.asStateFlow()

    data class VisibleRange(
        val first: Int = -1,
        val last: Int = -1,
        val slideRatio: Float = 0f
    )
    private val _visibleRange = MutableStateFlow(VisibleRange())
    val visibleRange = _visibleRange.asStateFlow()

    private val _scrollToColumn = Channel<Int>(Channel.CONFLATED)
    val scrollToColumn = _scrollToColumn.receiveAsFlow()

    fun setVisibleRange(first: Int, last: Int, slideRatio: Float) {
        _visibleRange.value = VisibleRange(first, last, slideRatio)
    }

    fun requestScrollToColumn(index: Int) {
        _scrollToColumn.trySend(index)
    }
    
    // Note: The caller (ActMain) is responsible for mapping Column to ColumnUiState
    // because Column depends on many Android/App specific things that are easier to access there
    // or we can move the mapping logic here if we have access to daoAcctColor etc.
    fun setColumns(columns: List<ColumnUiState>) {
        _columnList.value = columns
    }
    
    // Actual Column objects for the Pager/List
    private val _columnObjects = MutableStateFlow<List<es.ariaontheplanet.quasar.column.Column>>(emptyList())
    val columnObjects = _columnObjects.asStateFlow()

    fun setColumnObjects(columns: List<es.ariaontheplanet.quasar.column.Column>) {
        _columnObjects.value = columns
    }

    // AppState access
    private val appState get() = es.ariaontheplanet.quasar.App1.getAppState(getApplication())

    // ──────── ViewHolder Registry ────────
    // Manages active ColumnViewHolder instances for each Column
    private val _activeViewHolders = MutableStateFlow<Map<Column, ColumnViewHolder>>(emptyMap())
    
    fun registerViewHolder(column: Column, holder: ColumnViewHolder) {
        _activeViewHolders.value = _activeViewHolders.value + (column to holder)
    }
    
    fun unregisterViewHolder(column: Column) {
        _activeViewHolders.value = _activeViewHolders.value - column
    }
    
    fun getViewHolder(column: Column): ColumnViewHolder? {
        return _activeViewHolders.value[column]
    }

    // Column Management

    fun updateColumnStrip() {
        val list = appState.columnList
        setColumnObjects(list.toList())

        val uiList = list.mapIndexed { index, column ->
            val ac = es.ariaontheplanet.quasar.table.daoAcctColor.load(column.accessInfo)
            val acctColor = if (es.ariaontheplanet.quasar.table.daoAcctColor.hasColorForeground(ac)) ac.colorFg else 0
            
            ColumnUiState(
                index = index,
                iconId = column.getIconId(),
                acctColor = acctColor,
                headerNameColor = column.getHeaderNameColor(),
                headerBackgroundColor = column.getHeaderBackgroundColor(),
                contentDescription = column.getColumnName(true) ?: ""
            )
        }
        setColumns(uiList)
        
        // notify update (if needed)
    }

    fun addColumn(column: es.ariaontheplanet.quasar.column.Column, indexArg: Int): Int {
        val index = indexArg.coerceIn(0, appState.columnCount)
        appState.editColumnList {
            it.add(index, column)
        }
        updateColumnStrip()
        return index
    }

    fun removeColumn(column: es.ariaontheplanet.quasar.column.Column) {
        val idxColumn = appState.columnIndex(column) ?: return
        appState.editColumnList {
            it.removeAt(idxColumn).dispose()
        }
        updateColumnStrip()
    }
    
    fun removeColumnAt(index: Int) {
         appState.editColumnList {
            if (index in it.indices) {
                it.removeAt(index).dispose()
            }
        }
        updateColumnStrip()
    }

    /**
     * Swap the current account for [account], rebuild the 4 fixed columns bound to
     * it, and push the new column list to the UI. Called from the drawer's account
     * picker.
     */
    fun switchAccount(account: SavedAccount) {
        appState.setCurrentAccount(account)
        appState.rebuildFixedColumns(account)
        updateColumnStrip()
    }

    // Quick Post Sheet State (top app bar tap → ModalBottomSheet)
    private val _isQuickPostSheetShown = MutableStateFlow(false)
    val isQuickPostSheetShown = _isQuickPostSheetShown.asStateFlow()

    private val _quickPostText = MutableStateFlow("")
    val quickPostText = _quickPostText.asStateFlow()

    private val _quickPostCwEnabled = MutableStateFlow(false)
    val quickPostCwEnabled = _quickPostCwEnabled.asStateFlow()

    private val _quickPostCwText = MutableStateFlow("")
    val quickPostCwText = _quickPostCwText.asStateFlow()

    private val _quickPostVisibility =
        MutableStateFlow(es.ariaontheplanet.quasar.api.entity.TootVisibility.Public)
    val quickPostVisibility = _quickPostVisibility.asStateFlow()

    private val _quickPostSending = MutableStateFlow(false)
    val quickPostSending = _quickPostSending.asStateFlow()

    fun openQuickPostSheet() {
        _isQuickPostSheetShown.value = true
    }

    fun closeQuickPostSheet() {
        _isQuickPostSheetShown.value = false
    }

    fun setQuickPostText(text: String) {
        _quickPostText.value = text
    }

    fun setQuickPostCwEnabled(enabled: Boolean) {
        _quickPostCwEnabled.value = enabled
    }

    fun setQuickPostCwText(text: String) {
        _quickPostCwText.value = text
    }

    fun setQuickPostVisibility(visibility: es.ariaontheplanet.quasar.api.entity.TootVisibility) {
        _quickPostVisibility.value = visibility
    }

    fun setQuickPostSending(sending: Boolean) {
        _quickPostSending.value = sending
    }

    fun resetQuickPost() {
        _quickPostText.value = ""
        _quickPostCwEnabled.value = false
        _quickPostCwText.value = ""
        _quickPostSending.value = false
    }
}
