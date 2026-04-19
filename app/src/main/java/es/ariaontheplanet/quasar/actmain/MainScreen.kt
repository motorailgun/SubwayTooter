package es.ariaontheplanet.quasar.actmain

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import es.ariaontheplanet.quasar.ActMain
import es.ariaontheplanet.quasar.compose.ColumnWrapper
import es.ariaontheplanet.quasar.compose.QuickTootMenuDialog
import es.ariaontheplanet.quasar.action.openPost
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    sideMenuAdapter: SideMenuAdapter,
    onClickMenu: () -> Unit,
    onClickToot: () -> Unit,
    onLongClickToot: () -> Unit,
    onClickColumn: (Int) -> Unit,
    onDrawerClosed: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as ActMain
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val isTablet = screenWidthDp >= 600.dp // Simple tablet check

    val columnObjects by viewModel.columnObjects.collectAsState()
    val pagerState = rememberPagerState { columnObjects.size }
    val lazyListState = rememberLazyListState()

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

    // Handle Scroll Requests
    LaunchedEffect(viewModel.scrollToColumn) {
        viewModel.scrollToColumn.collectLatest { index ->
            if (index in columnObjects.indices) {
                if (isTablet) {
                    lazyListState.animateScrollToItem(index)
                } else {
                    pagerState.animateScrollToPage(index)
                }
            }
        }
    }

    // Sync Pager State with ViewModel
    LaunchedEffect(pagerState.currentPage) {
        if (!isTablet) {
            viewModel.setCurrentPage(pagerState.currentPage)
        }
    }
    
    // Sync List State with ViewModel (Tablet)
    LaunchedEffect(lazyListState.firstVisibleItemIndex) {
        if (isTablet) {
            viewModel.setVisibleRange(
                first = lazyListState.firstVisibleItemIndex,
                last = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1,
                slideRatio = 0f
            )
            // Also update current page to first visible
            viewModel.setCurrentPage(lazyListState.firstVisibleItemIndex)
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
            Scaffold(
                bottomBar = {
                    MainFooter(
                        viewModel = viewModel,
                        onClickMenu = onClickMenu,
                        onClickToot = onClickToot,
                        onLongClickToot = onLongClickToot,
                        onClickColumn = onClickColumn,
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    if (isTablet) {
                        LazyRow(state = lazyListState) {
                            itemsIndexed(columnObjects) { index, column ->
                                // Use ActMain's calculated column width or default
                                val colWidth = with(density) {
                                    if (activity.nColumnWidth > 0) activity.nColumnWidth.toDp() else 300.dp
                                }
                                Box(modifier = Modifier.width(colWidth)) {
                                    ColumnWrapper(activity, column, index, columnObjects.size)
                                }
                            }
                        }
                    } else {
                        HorizontalPager(state = pagerState) { page ->
                            val column = columnObjects.getOrNull(page)
                            if (column != null) {
                                ColumnWrapper(activity, column, page, columnObjects.size)
                            }
                        }
                    }
                }
            }
        }
    )

    // Quick Toot Menu
    val isQuickTootMenuShown by viewModel.isQuickTootMenuShown.collectAsState()
    val quickTootVisibility by viewModel.quickTootVisibility.collectAsState()

    if (isQuickTootMenuShown) {
        QuickTootMenuDialog(
            visibility = quickTootVisibility,
            onVisibilityPick = { viewModel.setQuickTootVisibility(it) },
            onUseMacro = { text ->
                viewModel.closeQuickTootMenu()
                activity.openPost(text)
            },
            onClose = { viewModel.closeQuickTootMenu() }
        )
    }
    
    // Back Press Dialog
    val backDialogState by viewModel.showBackDialog.collectAsState()
    backDialogState?.let { state ->
        BackPressDialog(
            state = state,
            onCloseColumn = { viewModel.confirmCloseColumn(it) },
            onOpenColumnList = { viewModel.confirmOpenColumnList() },
            onFinish = { viewModel.confirmFinish() },
            onDismiss = { viewModel.dismissBackDialog() }
        )
    }
}

@Composable
fun BackPressDialog(
    state: MainViewModel.BackDialogState,
    onCloseColumn: (es.ariaontheplanet.quasar.column.Column) -> Unit,
    onOpenColumnList: () -> Unit,
    onFinish: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(androidx.compose.ui.res.stringResource(es.ariaontheplanet.quasar.R.string.confirm)) },
        text = {
            androidx.compose.foundation.layout.Column {
                if (state.closableColumn != null) {
                    androidx.compose.material3.TextButton(onClick = { onCloseColumn(state.closableColumn) }) {
                        Text(androidx.compose.ui.res.stringResource(es.ariaontheplanet.quasar.R.string.close_column))
                    }
                }
                androidx.compose.material3.TextButton(onClick = onOpenColumnList) {
                    Text(androidx.compose.ui.res.stringResource(es.ariaontheplanet.quasar.R.string.open_column_list))
                }
                androidx.compose.material3.TextButton(onClick = onFinish) {
                    Text(androidx.compose.ui.res.stringResource(es.ariaontheplanet.quasar.R.string.app_exit))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(android.R.string.cancel))
            }
        }
    )
}

