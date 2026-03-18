package jp.juggler.subwaytooter.actmain

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    contentView: View,
    sideMenuAdapter: SideMenuAdapter,
    onDrawerClosed: () -> Unit = {},
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Sync Drawer State with ViewModel
    LaunchedEffect(viewModel.drawerControl) {
        viewModel.drawerControl.collectLatest { isOpen ->
            if (isOpen) drawerState.open() else drawerState.close()
        }
    }
    
    LaunchedEffect(drawerState.isOpen) {
        viewModel.setDrawerOpen(drawerState.isOpen)
        if (!drawerState.isOpen) {
            onDrawerClosed()
        }
    }

    // Handle Back Press to close Drawer
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                sideMenuAdapter.SideMenuContent(closeDrawer = {
                    scope.launch { drawerState.close() }
                })
            }
        },
        content = {
            AndroidView(
                factory = { contentView },
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
            )
        }
    )
}
