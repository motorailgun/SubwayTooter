package jp.juggler.subwaytooter.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.entity.TimelineItem
import jp.juggler.subwaytooter.column.Column
import jp.juggler.subwaytooter.column.getContentColor
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.subwaytooter.table.SavedAccount

/**
 * LazyColumn-based timeline content, replacing the RecyclerView + ItemListAdapter.
 *
 * @param activity the main activity
 * @param column the column being displayed
 * @param timelineState state holder for timeline items
 * @param bSimpleList whether to use simple list mode (no inline buttons)
 * @param callbacks interaction callbacks
 * @param lazyListState scroll state for save/restore
 * @param modifier Compose modifier
 */
@Composable
fun TimelineColumn(
    activity: ActMain,
    column: Column,
    timelineState: TimelineState,
    bSimpleList: Boolean,
    callbacks: TimelineCallbacks,
    lazyListState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
) {
    val accessInfo = column.accessInfo
    val contentColor = remember(column) { column.getContentColor() }
    val items = timelineState.items
    // Read revision to trigger recomposition on content updates
    val revision = timelineState.revision

    when {
        timelineState.isLoading -> {
            // Loading state
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Loading…",
                    color = Color(contentColor),
                )
            }
        }

        timelineState.errorMessage != null -> {
            // Error state
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = timelineState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        items.isEmpty() -> {
            // Empty state
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = timelineState.emptyMessage
                        ?: stringResource(R.string.list_empty),
                    color = Color(contentColor),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        else -> {
            // Timeline content
            val nestedScrollInterop = rememberNestedScrollInteropConnection()
            LazyColumn(
                state = lazyListState,
                modifier = modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollInterop),
            ) {
                itemsIndexed(
                    items = items,
                    key = { _, item -> item.listViewItemId },
                ) { index, item ->
                    TimelineItemContent(
                        activity = activity,
                        column = column,
                        accessInfo = accessInfo,
                        item = item,
                        bSimpleList = bSimpleList,
                        callbacks = callbacks,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Divider between items
                    HorizontalDivider(
                        color = Color(contentColor).copy(alpha = 0.2f),
                        thickness = 0.5.dp,
                    )
                }
            }
        }
    }
}

/**
 * Scroll position data for save/restore across column changes.
 */
data class ComposeScrollPosition(
    val firstVisibleItemIndex: Int = 0,
    val firstVisibleItemScrollOffset: Int = 0,
) {
    companion object {
        fun from(state: LazyListState): ComposeScrollPosition {
            return ComposeScrollPosition(
                firstVisibleItemIndex = state.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = state.firstVisibleItemScrollOffset,
            )
        }
    }

    suspend fun restore(state: LazyListState) {
        state.scrollToItem(firstVisibleItemIndex, firstVisibleItemScrollOffset)
    }
}
