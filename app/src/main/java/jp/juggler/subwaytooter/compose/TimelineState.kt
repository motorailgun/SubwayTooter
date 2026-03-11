package jp.juggler.subwaytooter.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import jp.juggler.subwaytooter.api.entity.TimelineItem
import jp.juggler.subwaytooter.column.Column
import jp.juggler.util.log.LogCategory
import jp.juggler.util.ui.AdapterChange
import jp.juggler.util.ui.AdapterChangeType

/**
 * State holder that bridges Column.listData to Compose state.
 * This replaces the role of ItemListAdapter for data management.
 */
class TimelineState {
    companion object {
        private val log = LogCategory("TimelineState")
    }

    /**
     * The list of items to display, as Compose state.
     */
    val items = mutableStateListOf<TimelineItem>()

    /**
     * Revision counter to force recomposition when content changes
     * but item identity is the same (e.g., content updates).
     */
    var revision by mutableIntStateOf(0)

    /**
     * Force recomposition by incrementing the revision counter.
     */
    fun forceRecompose() {
        revision++
    }

    /**
     * Whether the column is currently loading.
     */
    var isLoading by mutableStateOf(false)

    /**
     * Whether the column is refreshing (pull-to-refresh).
     */
    var isRefreshing by mutableStateOf(false)

    /**
     * Error message to display, if any.
     */
    var errorMessage by mutableStateOf<String?>(null)

    /**
     * Empty state message.
     */
    var emptyMessage by mutableStateOf<String?>(null)

    /**
     * Update the items list from column data.
     * This is called from the existing showContent / notifyChange path.
     *
     * @param column the Column whose listData to snapshot
     * @param changeList optional incremental changes (for efficiency)
     * @param reset if true, full reset of the list
     */
    fun notifyChange(
        column: Column,
        reason: String,
        changeList: List<AdapterChange>? = null,
        reset: Boolean = false,
    ) {
        log.d("notifyChange: reason=$reason, changeList=${changeList?.size}, reset=$reset")

        val newList = ArrayList<TimelineItem>(column.listData.size)
        newList.addAll(column.listData)

        when {
            changeList != null -> {
                // Apply incremental changes
                for (c in changeList) {
                    when (c.type) {
                        AdapterChangeType.RangeInsert -> {
                            val insertItems = newList.subList(
                                c.listIndex,
                                (c.listIndex + c.count).coerceAtMost(newList.size)
                            )
                            if (c.listIndex <= items.size) {
                                items.addAll(c.listIndex, insertItems)
                            }
                        }

                        AdapterChangeType.RangeRemove -> {
                            val endIdx = (c.listIndex + c.count).coerceAtMost(items.size)
                            if (c.listIndex < items.size) {
                                for (i in (endIdx - 1) downTo c.listIndex) {
                                    if (i < items.size) items.removeAt(i)
                                }
                            }
                        }

                        AdapterChangeType.RangeChange -> {
                            // Update changed items
                            for (i in 0 until c.count) {
                                val idx = c.listIndex + i
                                if (idx < items.size && idx < newList.size) {
                                    items[idx] = newList[idx]
                                }
                            }
                        }
                    }
                }
                revision++
            }

            reset -> {
                items.clear()
                items.addAll(newList)
                revision++
            }

            else -> {
                // Smart diff: replace items that changed
                items.clear()
                items.addAll(newList)
                revision++
            }
        }
    }

    /**
     * Sync items from column data (full replacement).
     */
    fun syncFromColumn(column: Column) {
        items.clear()
        items.addAll(column.listData)
        revision++
    }
}
