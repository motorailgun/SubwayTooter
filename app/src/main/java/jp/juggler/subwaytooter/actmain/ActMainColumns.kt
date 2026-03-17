package jp.juggler.subwaytooter.actmain

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import jp.juggler.subwaytooter.*
import jp.juggler.subwaytooter.column.*
import jp.juggler.subwaytooter.columnviewholder.TabletColumnViewHolder
import jp.juggler.subwaytooter.columnviewholder.scrollToTop2
import jp.juggler.subwaytooter.columnviewholder.showColumnSetting
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.subwaytooter.pref.PrefI
import jp.juggler.subwaytooter.pref.PrefS
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.table.daoAcctColor
import jp.juggler.subwaytooter.util.AccountCache
import jp.juggler.util.*
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.data.clip
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.ui.getAdaptiveRippleDrawableRound
import jp.juggler.util.ui.vg
import android.view.Gravity
import kotlin.math.abs
import kotlin.math.min

private val log = LogCategory("ActMainColumns")

// スマホモードなら現在のカラムを、タブレットモードなら-1Lを返す
// (カラム一覧画面のデフォルト選択位置に使われる)
val ActMain.currentColumn: Int
    get() = if (!isTablet) {
        composePagerState?.currentPage ?: -1
    } else {
        -1
    }

// 新しいカラムをどこに挿入するか
// 現在のページの次の位置か、終端
val ActMain.defaultInsertPosition: Int
    get() = if (!isTablet) {
        (composePagerState?.currentPage ?: -1) + 1
    } else {
        Integer.MAX_VALUE
    }

// カラム追加後など、そのカラムにスクロールして初期ロードを行う
fun ActMain.scrollAndLoad(idx: Int) {
    val c = appState.column(idx) ?: return
    scrollToColumn(idx)
    c.startLoading(ColumnLoadReason.PageSelect)
}

fun ActMain.addColumn(column: Column, indexArg: Int): Int {
    val index = indexArg.clip(0, appState.columnCount)

    appState.editColumnList {
        it.add(index, column)
    }

    // Compose handles updates reactively via StateFlow/State
    // No explicit adapter notification needed for Compose

    updateColumnStrip()

    return index
}

fun ActMain.addColumn(
    allowColumnDuplication: Boolean,
    indexArg: Int,
    ai: SavedAccount,
    type: ColumnType,
    protect: Boolean = false,
    params: Array<out Any> = emptyArray(),
): Column {
    if (!allowColumnDuplication) {
        // 既に同じカラムがあればそこに移動する
        appState.columnList.forEachIndexed { i, column ->
            if (ColumnSpec.isSameSpec(column, ai, type, params)) {
                scrollToColumn(i)
                return column
            }
        }
    }

    //
    val col = Column(appState, ai, type.id, params)
    if (protect) col.dontClose = true
    val index = addColumn(col, indexArg)
    scrollAndLoad(index)
    return col
}

fun ActMain.addColumn(
    indexArg: Int,
    ai: SavedAccount,
    type: ColumnType,
    protect: Boolean = false,
    params: Array<out Any> = emptyArray(),
): Column = addColumn(
    PrefB.bpAllowColumnDuplication.value,
    indexArg,
    ai,
    type,
    protect = protect,
    params = params,
)

fun ActMain.removeColumn(column: Column) {
    val idxColumn = appState.columnIndex(column) ?: return

    appState.editColumnList {
        it.removeAt(idxColumn).dispose()
    }

    updateColumnStrip()
}

fun ActMain.isVisibleColumn(idx: Int): Boolean {
    if (!isTablet) {
        return composePagerState?.currentPage == idx
    } else {
        val layoutInfo = composeTabletListState?.layoutInfo ?: return false
        val visibleItems = layoutInfo.visibleItemsInfo
        return visibleItems.any { it.index == idx }
    }
}

fun ActMain.updateColumnStrip() {
    // No-op for Compose. The ColumnStrip is now a Composable (ActMainBottomAppBar)
    // observing the column list and pager state directly.
}

fun ActMain.closeColumn(column: Column, bConfirmed: Boolean = false) {

    if (column.dontClose) {
        showToast(false, R.string.column_has_dont_close_option)
        return
    }

    if (!bConfirmed && !PrefB.bpDontConfirmBeforeCloseColumn.value) {
        AlertDialog.Builder(this)
            .setMessage(R.string.confirm_close_column)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ -> closeColumn(column, bConfirmed = true) }
            .show()
        return
    }

    appState.columnIndex(column)?.let { page_delete ->
        if (!isTablet) {
            val pageShowing = composePagerState?.currentPage ?: -1
            removeColumn(column)
            if (pageShowing == page_delete) {
                // If we closed the current column, ensure we land on a valid one
                // Compose Pager handles bounds, but we might want to trigger load
                val newIndex = (pageShowing - 1).coerceAtLeast(0)
                // scrollAndLoad(newIndex)
            }
        } else {
            removeColumn(column)
            // Tablet mode: list just updates. Maybe scroll to adjacent?
            val newIndex = (page_delete - 1).coerceAtLeast(0)
            // scrollAndLoad(newIndex) // Optional for tablet list
        }
    }
}

fun ActMain.closeColumnAll(oldColumnIndex: Int = -1, bConfirmed: Boolean = false) {
    if (!bConfirmed) {
        AlertDialog.Builder(this)
            .setMessage(R.string.confirm_close_column_all)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ -> closeColumnAll(oldColumnIndex, true) }
            .show()
        return
    }

    var lastColumnIndex = if (oldColumnIndex != -1) {
         oldColumnIndex
    } else if (!isTablet) {
         composePagerState?.currentPage ?: 0
    } else {
         0
    }

    appState.editColumnList { list ->
        for (i in list.indices.reversed()) {
            val column = list[i]
            if (column.dontClose) continue
            list.removeAt(i).dispose()
            if (lastColumnIndex >= i) --lastColumnIndex
        }
    }

    updateColumnStrip()

    scrollAndLoad(lastColumnIndex.coerceAtLeast(0))
}

fun ActMain.closeColumnSetting(): Boolean {
    // This functionality (closing setting view in legacy holder)
    // needs to be mapped to closing the bottom sheet or similar in Compose.
    // For now, return false as we don't have a direct equivalent of "isColumnSettingShown" exposed globally yet,
    // or it's handled by local state in Compose.
    return false
}

// 新しいカラムをどこに挿入するか
// カラムの次の位置か、現在のページの次の位置か、終端
fun ActMain.nextPosition(column: Column?): Int =
    appState.columnIndex(column)?.let { it + 1 } ?: defaultInsertPosition

fun ActMain.isOrderChanged(newOrder: List<Int>): Boolean {
    if (newOrder.size != appState.columnCount) return true
    for (i in newOrder.indices) {
        if (newOrder[i] != i) return true
    }
    return false
}

fun ActMain.setColumnsOrder(newOrder: List<Int>) {

    appState.editColumnList { list ->
        // columns with new order
        val tmpList = newOrder.mapNotNull { i -> list.elementAtOrNull(i) }
        val usedSet = newOrder.toSet()
        list.forEachIndexed { i, v ->
            if (!usedSet.contains(i)) v.dispose()
        }
        list.clear()
        list.addAll(tmpList)
    }

    appState.saveColumnList()
    updateColumnStrip()
}

fun ActMain.searchFromActivityResult(data: Intent?, columnType: ColumnType) =
    data?.string(Intent.EXTRA_TEXT)?.let {
        addColumn(
            false,
            defaultInsertPosition,
            SavedAccount.na,
            columnType,
            params = arrayOf(it)
        )
    }

fun ActMain.scrollToColumn(index: Int, smoothScroll: Boolean = true) {
    if (index < 0 || index >= appState.columnCount) return

    if (!isTablet) {
        log.d("scrollToColumn phone index=$index")
        launchMain {
             if (smoothScroll) {
                 composePagerState?.animateScrollToPage(index)
             } else {
                 composePagerState?.scrollToPage(index)
             }
        }
    } else {
        log.d("scrollToColumn tablet index=$index")
        launchMain {
            if (smoothScroll) {
                composeTabletListState?.animateScrollToItem(index)
            } else {
                composeTabletListState?.scrollToItem(index)
            }
        }
    }
}



@SuppressLint("NotifyDataSetChanged")
fun ActMain.resizeColumnWidth() {
    // Logic for calculating column width is now handled in Compose (ActMainColumns composable)
    // or by passing density/screen width to a logic class.
    // For now, we can leave this as a no-op or remove it if not called.
    // If nScreenColumn etc are used elsewhere, we might need to update them.
}

fun ActMain.scrollColumnStrip(select: Int) {
   // No-op: Compose handles tab scrolling via ScrollableTabRow or similar
}

fun ActMain.updateColumnStripSelection(position: Int, positionOffset: Float) {
    // No-op: Compose state observation handles this
}

fun ActMain.showColumnMatchAccount(account: SavedAccount) {
    appState.columnList.forEach { column ->
        if (account == column.accessInfo) {
            column.fireRebindAdapterItems()
        }
    }
}

