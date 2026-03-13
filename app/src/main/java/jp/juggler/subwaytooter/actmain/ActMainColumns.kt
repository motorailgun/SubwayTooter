package jp.juggler.subwaytooter.actmain

import android.content.Intent
import androidx.appcompat.app.AlertDialog
import jp.juggler.subwaytooter.*
import jp.juggler.subwaytooter.column.*
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.subwaytooter.pref.PrefI
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.util.data.clip
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.string
import kotlin.math.max

private val log = LogCategory("ActMainColumns")

val ActMain.currentColumn: Int
    get() = if (isComposeStateInitialized()) {
        if (composeState.isTablet) -1 else composeState.pagerState.currentPage
    } else {
        0
    }

val ActMain.defaultInsertPosition: Int
    get() = if (isComposeStateInitialized()) {
         if (composeState.isTablet) Integer.MAX_VALUE else composeState.pagerState.currentPage + 1
    } else {
        Integer.MAX_VALUE
    }

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
    
    if (isComposeStateInitialized()) {
        composeState.refreshColumnList()
    }
    
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
        appState.columnList.forEachIndexed { i, column ->
            if (ColumnSpec.isSameSpec(column, ai, type, params)) {
                scrollToColumn(i)
                return column
            }
        }
    }

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

    if (isComposeStateInitialized()) {
        composeState.refreshColumnList()
    }
}

fun ActMain.isVisibleColumn(idx: Int): Boolean {
    if (!isComposeStateInitialized()) return false
    
    if (composeState.isTablet) {
        return true 
    } else {
        return composeState.pagerState.currentPage == idx
    }
}

fun ActMain.updateColumnStrip() {
    if (isComposeStateInitialized()) {
        composeState.refreshColumnList()
    }
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
        val pageShowing = if (isComposeStateInitialized() && !composeState.isTablet) 
            composeState.pagerState.currentPage 
        else 
            -1
            
        removeColumn(column)
        
        if (pageShowing == page_delete && pageShowing > 0) {
            scrollAndLoad(pageShowing - 1)
        } else if (pageShowing != -1) {
            scrollAndLoad(max(0, page_delete - 1))
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

    var lastColumnIndex = when (oldColumnIndex) {
        -1 -> if (isComposeStateInitialized() && !composeState.isTablet) 
                composeState.pagerState.currentPage 
              else 
                0
        else -> oldColumnIndex
    }

    appState.editColumnList { list ->
        for (i in list.indices.reversed()) {
            val column = list[i]
            if (column.dontClose) continue
            list.removeAt(i).dispose()
            if (lastColumnIndex >= i) --lastColumnIndex
        }
    }

    if (isComposeStateInitialized()) {
        composeState.refreshColumnList()
    }

    scrollAndLoad(lastColumnIndex)
}

fun ActMain.closeColumnSetting(): Boolean {
    return false
}

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
        val tmpList = newOrder.mapNotNull { i -> list.elementAtOrNull(i) }
        val usedSet = newOrder.toSet()
        list.forEachIndexed { i, v ->
            if (!usedSet.contains(i)) v.dispose()
        }
        list.clear()
        list.addAll(tmpList)
    }

    if (isComposeStateInitialized()) {
        composeState.refreshColumnList()
    }

    appState.saveColumnList()
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
    if (isComposeStateInitialized()) {
        composeState.scrollToColumn(index, smoothScroll)
    }
}

fun ActMain.scrollToLastColumn() {
    if (appState.columnCount <= 0) return

    val columnPos = PrefI.ipLastColumnPos.value
    log.d("ipLastColumnPos load ")

    if (columnPos in 0 until appState.columnCount) {
        scrollToColumn(columnPos, false)
    }
}

fun ActMain.showColumnMatchAccount(account: SavedAccount) {
    appState.columnList.forEach { column ->
        if (account == column.accessInfo) {
            column.fireRebindAdapterItems()
        }
    }
}
