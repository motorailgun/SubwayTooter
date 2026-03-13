package jp.juggler.subwaytooter.actmain

import android.content.Context
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.subwaytooter.pref.PrefS
import jp.juggler.subwaytooter.column.Column
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class ActMainComposeState(
    val activity: ActMain,
    val drawerState: DrawerState,
    val pagerState: PagerState,
    val tabletListState: LazyListState,
    val stripScrollState: ScrollState,
    val scope: CoroutineScope,
    val isTablet: Boolean,
    val columnWidth: Int,
) {
    var columnList by mutableStateOf<List<Column>>(activity.appState.columnList.toList())
        private set

    init {
        // Update activity fields for compatibility if needed
        activity.nColumnWidth = columnWidth
    }

    fun refreshColumnList() {
        columnList = activity.appState.columnList.toList()
    }

    fun openDrawer() {
        scope.launch { drawerState.open() }
    }

    fun closeDrawer() {
        scope.launch { drawerState.close() }
    }

    fun scrollToColumn(index: Int, smoothScroll: Boolean = true) {
        scope.launch {
            // Scroll the strip to show the icon
            // Each icon is 40.dp wide. We want to center it or just ensure visible.
            // Simple approach: scroll to index * 40.dp
            // Note: 40.dp in pixels needs density.
            val density = activity.resources.displayMetrics.density
            val iconWidthPx = (40 * density).toInt()
            val scrollPos = index * iconWidthPx
            
            if (smoothScroll) {
                stripScrollState.animateScrollTo(scrollPos)
            } else {
                stripScrollState.scrollTo(scrollPos)
            }

            if (isTablet) {
                 // Tablet logic might be different (using LazyRow)
                 // Need to find the item position.
                 // For now, simple scroll
                 if (smoothScroll) {
                     tabletListState.animateScrollToItem(index)
                 } else {
                     tabletListState.scrollToItem(index)
                 }
            } else {
                if (smoothScroll) {
                    pagerState.animateScrollToPage(index)
                } else {
                    pagerState.scrollToPage(index)
                }
            }
        }
    }
}

@Composable
fun rememberActMainComposeState(
    activity: ActMain,
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    pagerState: PagerState = rememberPagerState(pageCount = { activity.appState.columnCount }),
    tabletListState: LazyListState = rememberLazyListState(),
    stripScrollState: ScrollState = rememberScrollState(),
    scope: CoroutineScope = rememberCoroutineScope(),
): ActMainComposeState {
    val configuration = LocalConfiguration.current
    val screenState = remember(activity, configuration) {
        calculateScreenState(activity)
    }
    return remember(activity, drawerState, pagerState, tabletListState, stripScrollState, scope, screenState) {
        ActMainComposeState(
            activity,
            drawerState,
            pagerState,
            tabletListState,
            stripScrollState,
            scope,
            screenState.first,
            screenState.second
        )
    }
}

/**
 * Calculate whether the device is in tablet mode and column width based on screen width.
 * Returns Pair(isTablet, columnWidth)
 */
private fun calculateScreenState(activity: ActMain): Pair<Boolean, Int> {
    // Get the column width setting, or use default
    val columnWMinDp = ActMain.COLUMN_WIDTH_MIN_DP.toFloat()
    val columnWidthPref = PrefS.spColumnWidth.value
    val columnWDp = columnWidthPref
        .toFloatOrNull()
        ?.takeIf { it.isFinite() && it >= 100f }
        ?: columnWMinDp

    // Convert DP to pixels
    val density = activity.resources.displayMetrics.density
    val columnWMin = (columnWDp * density + 0.5f).toInt()

    // Get screen width in pixels
    val screenWidthPx = activity.resources.displayMetrics.widthPixels

    // Tablet mode if tablet mode is enabled AND screen width is at least 2 columns wide
    val disableTabletMode = PrefB.bpDisableTabletMode.value
    val isTablet = !disableTabletMode && screenWidthPx >= columnWMin * 2
    
    val nScreenColumn = if (isTablet) screenWidthPx / columnWMin else 1
    val columnWidth = if (isTablet) screenWidthPx / nScreenColumn else screenWidthPx
    
    return Pair(isTablet, columnWidth)
}
