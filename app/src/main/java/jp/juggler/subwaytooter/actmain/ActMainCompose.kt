package jp.juggler.subwaytooter.actmain

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.App1
import jp.juggler.subwaytooter.R
import com.google.android.material.R as MR
import jp.juggler.subwaytooter.column.Column
import jp.juggler.subwaytooter.column.getColumnName
import jp.juggler.subwaytooter.column.getIconId
import jp.juggler.subwaytooter.column.getContentColor
import jp.juggler.subwaytooter.column.getHeaderBackgroundColor
import jp.juggler.subwaytooter.column.getHeaderNameColor
import jp.juggler.subwaytooter.compose.TimelineColumn
import jp.juggler.subwaytooter.compose.buildTimelineCallbacks
import jp.juggler.subwaytooter.compose.TimelineState
import jp.juggler.subwaytooter.table.daoAcctColor
import jp.juggler.util.ui.attrColor
import kotlinx.coroutines.launch

@Composable
fun ActMainScreen(activity: ActMain) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val appState = activity.appState
    val columnList = appState.columnList // This might need to be a State to trigger recomposition
    
    // For now, let's assume we can recompose when needed.
    // In a real app, you'd use a State or Flow.
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                AndroidView(
                    factory = { context: android.content.Context ->
                        activity.views.root.also { root ->
                            (root.parent as? android.view.ViewGroup)?.removeView(root)
                        }
                    },
                    modifier = Modifier.fillMaxHeight().width(300.dp)
                )
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                ActMainBottomAppBar(activity, onMenuClick = {
                    scope.launch { drawerState.open() }
                })
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
                    ActMainColumns(activity)
                }
            }
        }
    }
}

@Composable
fun ActMainColumns(activity: ActMain) {
    val columnList = activity.appState.columnList
    val pagerState = rememberPagerState(pageCount = { columnList.size })
    
    // Check if tablet mode
    val isTablet = activity.tabletViews != null

    if (isTablet) {
        val lazyListState = rememberLazyListState()
        LazyRow(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            itemsIndexed(columnList) { index, column ->
                Box(modifier = Modifier.width(activity.nColumnWidth.dp).fillMaxHeight()) {
                    TimelineView(activity, column)
                }
            }
        }
    } else {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            TimelineView(activity, columnList[page])
        }
    }
}

@Composable
fun TimelineView(activity: ActMain, column: Column) {
    // This should ideally use the already existing TimelineColumn and TimelineState
    // For the mechanical translation, we'll try to bridge it.
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
fun ActMainBottomAppBar(activity: ActMain, onMenuClick: () -> Unit) {
    val columnList = activity.appState.columnList
    
    BottomAppBar(
        modifier = Modifier.height(48.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(
                painter = painterResource(R.drawable.ic_hamburger),
                contentDescription = stringResource(R.string.menu)
            )
        }
        
        VerticalDivider(modifier = Modifier.width(1.dp).fillMaxHeight())
        
        // Column Strip
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            columnList.forEachIndexed { index, column ->
                ColumnIcon(activity, column, index)
            }
        }
        
        VerticalDivider(modifier = Modifier.width(1.dp).fillMaxHeight())
        
        IconButton(onClick = { activity.onClick(activity.views.btnToot) }) {
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = stringResource(R.string.toot)
            )
        }
    }
}

@Composable
fun ColumnIcon(activity: ActMain, column: Column, index: Int) {
    val iconSize = 32.dp
    val headerColor = Color(column.getHeaderBackgroundColor())
    val nameColor = Color(column.getHeaderNameColor())
    
    Column(
        modifier = Modifier
            .width(40.dp)
            .fillMaxHeight()
            .background(headerColor)
            .clickable { activity.scrollToColumn(index) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(column.getIconId()),
            contentDescription = column.getColumnName(true),
            tint = nameColor,
            modifier = Modifier.size(iconSize)
        )
        
        // Acct color bar
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
