package jp.juggler.subwaytooter.columnviewholder

import androidx.lifecycle.lifecycleScope
import jp.juggler.subwaytooter.column.getColumnName
import jp.juggler.subwaytooter.column.toListIndex
import jp.juggler.subwaytooter.util.ScrollPosition
import jp.juggler.util.log.LogCategory
import kotlinx.coroutines.launch

private val log = LogCategory("ColumnViewHolderLoading")

// 特定の要素が特定の位置に来るようにスクロール位置を調整する
fun ColumnViewHolder.setListItemTop(listIndex: Int, yArg: Int) {
    val lls = lazyListState ?: return
    activity.lifecycleScope.launch {
        try {
            lls.scrollToItem(listIndex, -yArg)
        } catch (ex: Throwable) {
            ColumnViewHolder.log.e(ex, "Compose scrollToItem failed.")
        }
    }
}

// この関数は scrollToPositionWithOffset 用のオフセットを返す
fun ColumnViewHolder.getListItemOffset(listIndex: Int): Int {
    val lls = lazyListState ?: return 0
    return if (lls.firstVisibleItemIndex == listIndex) {
        lls.firstVisibleItemScrollOffset
    } else {
        0
    }
}

fun ColumnViewHolder.findFirstVisibleListItem(): Int {
    val lls = lazyListState
    if (lls != null) {
        return lls.firstVisibleItemIndex
    }
    return column?.toListIndex(0) ?: throw IndexOutOfBoundsException()
}

fun ColumnViewHolder.scrollToTop() {
    val lls = lazyListState ?: return
    activity.lifecycleScope.launch {
        try {
            lls.scrollToItem(0, 0)
        } catch (ex: Throwable) {
            ColumnViewHolder.log.e(ex, "Compose scrollToItem failed.")
        }
    }
}

fun ColumnViewHolder.scrollToTop2() {
    if (bindingBusy) return
    val ts = this.timelineState
    if (ts != null) {
        if (ts.items.isNotEmpty()) scrollToTop()
        return
    }
}

fun ColumnViewHolder.saveScrollPosition(): Boolean {
    val column = this.column
    when {
        column == null ->
            ColumnViewHolder.log.d("saveScrollPosition [$pageIdx] , column==null")

        column.isDispose.get() ->
            ColumnViewHolder.log.d("saveScrollPosition [$pageIdx] , column is disposed")

        else -> {
            val lls = lazyListState
            if (lls != null) {
                val scrollSave = ScrollPosition(
                    lls.firstVisibleItemIndex,
                    lls.firstVisibleItemScrollOffset
                )
                column.scrollSave = scrollSave
                ColumnViewHolder.log.d(
                    "saveScrollPosition [$pageIdx] ${column.getColumnName(true)} , Compose save ${scrollSave.adapterIndex},${scrollSave.offset}"
                )
                return true
            }

            // LazyListState not ready yet - save default position
            val scrollSave = ScrollPosition()
            column.scrollSave = scrollSave
            ColumnViewHolder.log.d(
                "saveScrollPosition [$pageIdx] ${column.getColumnName(true)} , lazyListState not ready, save ${scrollSave.adapterIndex},${scrollSave.offset}"
            )
            return true
        }
    }
    return false
}

fun ColumnViewHolder.setScrollPosition(sp: ScrollPosition, deltaDp: Float = 0f) {
    val lls = lazyListState ?: return
    if (column == null) return
    activity.lifecycleScope.launch {
        try {
            lls.scrollToItem(sp.adapterIndex, sp.offset)
            if (deltaDp != 0f) {
                val dy = (deltaDp * activity.density + 0.5f).toInt()
                lls.scrollToItem(
                    lls.firstVisibleItemIndex,
                    lls.firstVisibleItemScrollOffset + dy
                )
            }
        } catch (ex: Throwable) {
            log.e(ex, "Compose setScrollPosition failed.")
        }
    }
}

// 相対時刻を更新する
fun ColumnViewHolder.updateRelativeTime() = rebindAdapterItems()

fun ColumnViewHolder.rebindAdapterItems() {
    val ts = timelineState ?: return
    ts.forceRecompose()
}
