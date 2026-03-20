package jp.juggler.subwaytooter.actmain

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {

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
}
