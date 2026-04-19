package es.ariaontheplanet.quasar.compose

import androidx.compose.runtime.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import es.ariaontheplanet.quasar.ActMain
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.column.Column
import es.ariaontheplanet.quasar.column.ColumnType
import es.ariaontheplanet.quasar.columnviewholder.ColumnViewHolder
import es.ariaontheplanet.quasar.columnviewholder.onPageCreate
import es.ariaontheplanet.quasar.columnviewholder.onPageDestroy
import es.ariaontheplanet.quasar.pref.PrefB

@Composable
fun ColumnWrapper(
    activity: ActMain,
    column: Column,
    pageIdx: Int,
    pageCount: Int,
) {
    val themeColors = rememberThemeColors()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Hold the presenter state
    // We use remember(column) so that if column instance changes, we recreate the holder.
    // In practice, column instances in the list are stable until the list changes.
    val presenter = remember(column) { 
        ColumnViewHolder(
            appState = activity.appState,
            handler = activity.handler,
            themeColors = themeColors,
            coroutineScope = lifecycleOwner.lifecycleScope,
            acctPadLr = activity.acctPadLr,
            activity = activity,
            column = column
        )
    }
    
    // Track initialization state to trigger recomposition after onPageCreate
    var isReady by remember(column) { mutableStateOf(false) }

    DisposableEffect(column) {
        activity.viewModel.registerViewHolder(column, presenter)
        presenter.onPageCreate(column, pageIdx, pageCount)
        isReady = true
        onDispose {
            presenter.onPageDestroy(pageIdx)
            activity.viewModel.unregisterViewHolder(column)
        }
    }
    
    // Update index label if position changes
    LaunchedEffect(pageIdx, pageCount) {
        if (isReady) {
            presenter.pageIdx = pageIdx
            presenter.columnUiState.columnIndex = activity.getString(R.string.column_index, pageIdx + 1, pageCount)
        }
    }
    
    // UI Rendering
    if (isReady) {
        val timelineState = presenter.timelineState
        val lazyListState = presenter.lazyListState
        
        if (timelineState != null && lazyListState != null) {
            ColumnScreen(
                activity = activity,
                column = column,
                uiState = presenter.columnUiState,
                timelineState = timelineState,
                timelineCallbacks = presenter.timelineCallbacks,
                columnCallbacks = presenter.columnCallbacks,
                bSimpleList = !(column.type == ColumnType.CONVERSATION || column.type == ColumnType.CONVERSATION_WITH_REFERENCE) && PrefB.bpSimpleList.value,
                lazyListState = lazyListState,
            )
        }
    }
}
