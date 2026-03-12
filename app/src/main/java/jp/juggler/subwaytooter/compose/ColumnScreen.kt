package jp.juggler.subwaytooter.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.column.Column as AppColumn

/**
 * Top-level composable for a single column.
 * Replaces the entire Anko DSL layout tree in ColumnViewHolder.inflate().
 *
 * Layout order matches the original:
 *   1. Column header
 *   2. Column settings panel (expandable)
 *   3. Announcements box (expandable)
 *   4. Search bar (conditional)
 *   5. Agg boost bar (conditional)
 *   6. List bar (conditional)
 *   7. Quick filter bar (conditional)
 *   8. Column body (timeline + loading + errors)
 */
@Composable
fun ColumnScreen(
    activity: ActMain,
    column: AppColumn,
    uiState: ColumnUiState,
    timelineState: TimelineState,
    timelineCallbacks: TimelineCallbacks,
    columnCallbacks: ColumnCallbacks,
    bSimpleList: Boolean,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
) {
    StThemedContent {
        Column(modifier = modifier.fillMaxSize()) {
            // 1. Column header
            ColumnHeaderBar(
                uiState = uiState,
                callbacks = columnCallbacks,
                modifier = Modifier.fillMaxWidth(),
            )

            // 2. Column settings panel
            ColumnSettingsPanel(
                uiState = uiState,
                callbacks = columnCallbacks,
                modifier = Modifier.fillMaxWidth(),
            )

            // 3. Announcements box
            ColumnAnnouncementsBox(
                uiState = uiState,
                callbacks = columnCallbacks,
                modifier = Modifier.fillMaxWidth(),
            )

            // 4. Search bar
            ColumnSearchBar(
                uiState = uiState,
                callbacks = columnCallbacks,
                modifier = Modifier.fillMaxWidth(),
            )

            // 5. Agg boost bar
            ColumnAggBoostBar(
                uiState = uiState,
                callbacks = columnCallbacks,
                modifier = Modifier.fillMaxWidth(),
            )

            // 6. List bar
            ColumnListBar(
                uiState = uiState,
                callbacks = columnCallbacks,
                modifier = Modifier.fillMaxWidth(),
            )

            // 7. Quick filter bar (only if NOT inside settings)
            if (!uiState.quickFilterInsideSetting) {
                ColumnQuickFilterBar(
                    uiState = uiState,
                    callbacks = columnCallbacks,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 8. Column body (takes remaining space)
            ColumnBody(
                activity = activity,
                column = column,
                uiState = uiState,
                timelineState = timelineState,
                timelineCallbacks = timelineCallbacks,
                columnCallbacks = columnCallbacks,
                bSimpleList = bSimpleList,
                lazyListState = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}
