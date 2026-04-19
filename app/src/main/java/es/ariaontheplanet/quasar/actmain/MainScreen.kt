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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import es.ariaontheplanet.quasar.ActMain
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.compose.ColumnWrapper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    sideMenuAdapter: SideMenuAdapter,
    onClickAccountIcon: () -> Unit,
    onSubmitQuickPost: () -> Unit,
    onExpandQuickPost: () -> Unit,
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
                topBar = {
                    QuickPostTopBar(
                        onClickHamburger = { scope.launch { drawerState.open() } },
                        onClickPostPlaceholder = { viewModel.openQuickPostSheet() },
                        onClickAccountIcon = onClickAccountIcon,
                    )
                },
                bottomBar = { MainFooter(viewModel = viewModel) },
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

    // Quick Post Sheet (replaces FAB + QuickTootMenu)
    val sheetShown by viewModel.isQuickPostSheetShown.collectAsState()
    if (sheetShown) {
        val text by viewModel.quickPostText.collectAsState()
        val cwEnabled by viewModel.quickPostCwEnabled.collectAsState()
        val cwText by viewModel.quickPostCwText.collectAsState()
        val visibility by viewModel.quickPostVisibility.collectAsState()
        val sending by viewModel.quickPostSending.collectAsState()

        QuickPostSheet(
            text = text,
            cwEnabled = cwEnabled,
            cwText = cwText,
            visibility = visibility,
            sending = sending,
            onTextChange = { viewModel.setQuickPostText(it) },
            onCwEnabledChange = { viewModel.setQuickPostCwEnabled(it) },
            onCwTextChange = { viewModel.setQuickPostCwText(it) },
            onVisibilityChange = { viewModel.setQuickPostVisibility(it) },
            onDismiss = { viewModel.closeQuickPostSheet() },
            onClickSend = onSubmitQuickPost,
            onClickExpand = onExpandQuickPost,
        )
    }

    // Back Press Dialog — confirm exit
    val backDialogState by viewModel.showBackDialog.collectAsState()
    if (backDialogState != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBackDialog() },
            title = { Text(stringResource(R.string.confirm)) },
            text = { Text(stringResource(R.string.app_exit)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmFinish() }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissBackDialog() }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
