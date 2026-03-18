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
}
