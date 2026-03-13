package jp.juggler.subwaytooter.actmain

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.column.Column
import jp.juggler.subwaytooter.column.ColumnLoadReason
import jp.juggler.subwaytooter.column.startLoading
import jp.juggler.subwaytooter.column.getColumnName
import jp.juggler.subwaytooter.column.getIconId
import jp.juggler.subwaytooter.column.getHeaderBackgroundColor
import jp.juggler.subwaytooter.column.getHeaderNameColor
import jp.juggler.subwaytooter.compose.TimelineColumn
import jp.juggler.subwaytooter.compose.buildTimelineCallbacks
import jp.juggler.subwaytooter.compose.TimelineState
import jp.juggler.subwaytooter.table.daoAcctColor
import jp.juggler.util.ui.attrColor
import com.google.android.material.R as MR


@Composable
fun ActMainScreen(
    activity: ActMain,
    composeState: ActMainComposeState
) {
    val columnList = composeState.columnList
    val pagerState = composeState.pagerState
    val isTablet = composeState.isTablet
    val stripScrollState = composeState.stripScrollState

    // Sync strip scroll with pager page
    LaunchedEffect(pagerState) {
        if (!isTablet) {
            snapshotFlow { pagerState.currentPage }.collect { page ->
                // Calculate position: page * 40.dp
                val density = activity.resources.displayMetrics.density
                val iconWidthPx = (40 * density).toInt()
                stripScrollState.animateScrollTo(page * iconWidthPx)
            }
        }
    }

    // Handle page selection effects (load column data)
    LaunchedEffect(pagerState, isTablet) {
        if (!isTablet) {
            snapshotFlow { pagerState.currentPage }.collect { page ->
                activity.appState.column(page)?.let { column ->
                     column.startLoading(ColumnLoadReason.PageSelect)
                     activity.completionHelper.setInstance(column.accessInfo.takeIf { !it.isNA })
                }
            }
        } else {
             // For tablet, maybe track visible items?
             // composeState.tabletListState.layoutInfo.visibleItemsInfo ...
             // But existing logic was onPageSelected which is for Pager.
             // Tablet usually loads all visible columns.
        }
    }

    // Handle drawer state change effects
    LaunchedEffect(composeState.drawerState) {
        snapshotFlow { composeState.drawerState.currentValue }.collect {
             activity.completionHelper.closeAcctPopup()
        }
    }
    
    ModalNavigationDrawer(
        drawerState = composeState.drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                // Use the refactored SideMenuAdapter content
                activity.sideMenuAdapter.SideMenuContent(
                    closeDrawer = { composeState.closeDrawer() }
                )
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                ActMainBottomAppBar(
                    activity = activity,
                    composeState = composeState,
                    onMenuClick = { composeState.openDrawer() }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                if (columnList.isEmpty()) {
                    Text(
                        text = stringResource(R.string.column_empty),
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(activity.attrColor(MR.attr.colorOnSurface))
                    )
                } else {
                    ActMainColumns(activity, composeState)
                }
            }
        }
    }
}

@Composable
fun ActMainColumns(
    activity: ActMain,
    composeState: ActMainComposeState
) {
    val columnList = composeState.columnList
    val isTablet = composeState.isTablet

    if (isTablet) {
        LazyRow(
            state = composeState.tabletListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            itemsIndexed(columnList) { index, column ->
                // Use columnWidth from state (calculated in pixels, convert to dp)
                val density = activity.resources.displayMetrics.density
                val widthDp = (composeState.columnWidth / density).dp
                Box(modifier = Modifier.width(widthDp).fillMaxHeight()) {
                    TimelineView(activity, column)
                }
            }
        }
    } else {
        HorizontalPager(
            state = composeState.pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (page < columnList.size) {
                TimelineView(activity, columnList[page])
            }
        }
    }
}

@Composable
fun TimelineView(activity: ActMain, column: Column) {
    val timelineState = remember { TimelineState() }
    val callbacks = remember { buildTimelineCallbacks(activity) }

    TimelineColumn(
        activity = activity,
        column = column,
        timelineState = timelineState,
        bSimpleList = false,
        callbacks = callbacks,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ActMainBottomAppBar(
    activity: ActMain,
    composeState: ActMainComposeState,
    onMenuClick: () -> Unit
) {
    val columnList = composeState.columnList
    
    BottomAppBar(
        modifier = Modifier.height(48.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = stringResource(R.string.menu)
            )
        }
        
        VerticalDivider(modifier = Modifier.width(1.dp).fillMaxHeight())
        
        // Column Strip
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(composeState.stripScrollState)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            columnList.forEachIndexed { index, column ->
                ColumnIcon(
                    activity = activity,
                    composeState = composeState,
                    column = column,
                    index = index
                )
            }
        }
        
        VerticalDivider(modifier = Modifier.width(1.dp).fillMaxHeight())
        
        IconButton(onClick = { 
            // Workaround to trigger existing toot action
            val v = View(activity).apply { id = R.id.btnToot }
            activity.onClick(v)
        }) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.toot)
            )
        }
    }
}

@Composable
fun ColumnIcon(
    activity: ActMain,
    composeState: ActMainComposeState,
    column: Column,
    index: Int
) {
    val iconSize = 32.dp
    val headerColor = Color(column.getHeaderBackgroundColor())
    val nameColor = Color(column.getHeaderNameColor())
    
    Column(
        modifier = Modifier
            .width(40.dp)
            .fillMaxHeight()
            .background(headerColor)
            .clickable { composeState.scrollToColumn(index) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(column.getIconId()),
            contentDescription = column.getColumnName(true),
            tint = nameColor,
            modifier = Modifier.size(iconSize)
        )
        
        val ac = daoAcctColor.load(column.accessInfo)
        if (daoAcctColor.hasColorForeground(ac)) {
            Box(
                modifier = Modifier
                    .width(iconSize)
                    .height(2.dp)
                    .background(Color(ac.colorFg))
            )
        }
    }
}

@Composable
fun VerticalDivider(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.outlineVariant))
}
