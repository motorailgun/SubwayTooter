package jp.juggler.subwaytooter.actmain

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import jp.juggler.subwaytooter.App1
import jp.juggler.subwaytooter.table.daoAcctColor
import jp.juggler.subwaytooter.column.Column
import jp.juggler.subwaytooter.column.getIconId
import jp.juggler.subwaytooter.column.getHeaderNameColor
import jp.juggler.subwaytooter.column.getHeaderBackgroundColor
import jp.juggler.subwaytooter.column.getColumnName
import jp.juggler.subwaytooter.column.viewHolder
import jp.juggler.subwaytooter.columnviewholder.ColumnViewHolder

import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.pref.PrefI
import jp.juggler.subwaytooter.pref.PrefB

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Back Press Handling
    sealed class BackPressEffect {
        object Finish : BackPressEffect()
        object OpenColumnList : BackPressEffect()
        data class ShowToast(val textId: Int, val isError: Boolean = false) : BackPressEffect()
    }

    private val _backPressEffect = Channel<BackPressEffect>(Channel.CONFLATED)
    val backPressEffect = _backPressEffect.receiveAsFlow()

    data class BackDialogState(
        val closableColumn: Column?,
    )
    private val _showBackDialog = MutableStateFlow<BackDialogState?>(null)
    val showBackDialog = _showBackDialog.asStateFlow()

    fun onBackPressed() {
        if (_isDrawerOpen.value) {
            closeDrawer()
            return
        }

        if (appState.columnCount == 0) {
            _backPressEffect.trySend(BackPressEffect.Finish)
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

        // getClosableColumnList logic
        val visibleColumnList = ArrayList<Column>()
        val current = _currentPage.value
        val vr = _visibleRange.value
        
        if (vr.first != -1) {
            for (i in vr.first..vr.last) {
                appState.column(i)?.let { visibleColumnList.add(it) }
            }
        } else {
            appState.column(current)?.let { visibleColumnList.add(it) }
        }
        val closableColumnList = visibleColumnList.filter { !it.dontClose }

        when (PrefI.ipBackButtonAction.value) {
            PrefI.BACK_EXIT_APP -> _backPressEffect.trySend(BackPressEffect.Finish)
            PrefI.BACK_OPEN_COLUMN_LIST -> _backPressEffect.trySend(BackPressEffect.OpenColumnList)
            PrefI.BACK_CLOSE_COLUMN -> {
                when (closableColumnList.size) {
                    0 -> {
                        if (PrefB.bpExitAppWhenCloseProtectedColumn.value &&
                            PrefB.bpDontConfirmBeforeCloseColumn.value) {
                            _backPressEffect.trySend(BackPressEffect.Finish)
                        } else {
                             _backPressEffect.trySend(BackPressEffect.ShowToast(R.string.missing_closeable_column))
                        }
                    }
                    1 -> removeColumn(closableColumnList.first())
                    else -> _backPressEffect.trySend(BackPressEffect.ShowToast(R.string.cant_close_column_by_back_button_when_multiple_column_shown))
                }
            }
            else -> {
                // Ask Always
                _showBackDialog.value = BackDialogState(
                    closableColumn = if (closableColumnList.size == 1) closableColumnList.first() else null
                )
            }
        }
    }
    
    fun confirmCloseColumn(column: Column) {
        removeColumn(column)
        _showBackDialog.value = null
    }

    fun confirmOpenColumnList() {
        _backPressEffect.trySend(BackPressEffect.OpenColumnList)
         _showBackDialog.value = null
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
    private val _columnObjects = MutableStateFlow<List<jp.juggler.subwaytooter.column.Column>>(emptyList())
    val columnObjects = _columnObjects.asStateFlow()

    fun setColumnObjects(columns: List<jp.juggler.subwaytooter.column.Column>) {
        _columnObjects.value = columns
    }

    // AppState access
    private val appState get() = jp.juggler.subwaytooter.App1.getAppState(getApplication())

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
            val ac = jp.juggler.subwaytooter.table.daoAcctColor.load(column.accessInfo)
            val acctColor = if (jp.juggler.subwaytooter.table.daoAcctColor.hasColorForeground(ac)) ac.colorFg else 0
            
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

    fun addColumn(column: jp.juggler.subwaytooter.column.Column, indexArg: Int): Int {
        val index = indexArg.coerceIn(0, appState.columnCount)
        appState.editColumnList {
            it.add(index, column)
        }
        updateColumnStrip()
        return index
    }

    fun removeColumn(column: jp.juggler.subwaytooter.column.Column) {
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

    // Quick Toot Menu State
    private val _isQuickTootMenuShown = MutableStateFlow(false)
    val isQuickTootMenuShown = _isQuickTootMenuShown.asStateFlow()

    private val _quickTootVisibility = MutableStateFlow(jp.juggler.subwaytooter.api.entity.TootVisibility.Public)
    val quickTootVisibility = _quickTootVisibility.asStateFlow()

    fun toggleQuickTootMenu() {
        _isQuickTootMenuShown.value = !_isQuickTootMenuShown.value
    }

    fun setQuickTootVisibility(visibility: jp.juggler.subwaytooter.api.entity.TootVisibility) {
        _quickTootVisibility.value = visibility
    }

    fun closeQuickTootMenu() {
        _isQuickTootMenuShown.value = false
    }
}
