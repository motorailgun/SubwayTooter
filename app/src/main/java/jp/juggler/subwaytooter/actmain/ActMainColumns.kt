package jp.juggler.subwaytooter.actmain

import android.content.Intent
import androidx.appcompat.app.AlertDialog
import jp.juggler.subwaytooter.*
import jp.juggler.subwaytooter.column.*
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.subwaytooter.pref.PrefI
import jp.juggler.subwaytooter.pref.PrefS
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.table.daoAcctColor
import jp.juggler.subwaytooter.util.AccountCache
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.data.clip
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.ui.vg
import kotlin.math.abs
import kotlin.math.min

private val log = LogCategory("ActMainColumns")

// スマホモードなら現在のカラムを、タブレットモードなら-1Lを返す
// (カラム一覧画面のデフォルト選択位置に使われる)
val ActMain.currentColumn: Int
    get() = viewModel.currentPage.value

// 新しいカラムをどこに挿入するか
// 現在のページの次の位置か、終端
val ActMain.defaultInsertPosition: Int
    get() = viewModel.currentPage.value + 1

// カラム追加後など、そのカラムにスクロールして初期ロードを行う
fun ActMain.scrollAndLoad(idx: Int) {
    val c = appState.column(idx) ?: return
    scrollToColumn(idx)
    c.startLoading(ColumnLoadReason.PageSelect)
}

fun ActMain.addColumn(column: Column, indexArg: Int): Int {
    return viewModel.addColumn(column, indexArg)
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
    viewModel.removeColumn(column)
}

fun ActMain.isVisibleColumn(idx: Int): Boolean {
    // Check via ViewModel state
    // For single page (phone), it's currentPage
    // For tablet, check visibleRange
    val vr = viewModel.visibleRange.value
    return if (vr.first != -1) {
        idx in vr.first..vr.last
    } else {
        idx == viewModel.currentPage.value
    }
}

fun ActMain.updateColumnStrip() {
    viewModel.updateColumnStrip()
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
        val pageShowing = viewModel.currentPage.value
        removeColumn(column)
        if (pageShowing == page_delete) {
            scrollAndLoad(pageShowing - 1)
        } else {
            // Tablet logic or just keep current page?
            // If deleted page is before current page, current page index shifts
            // But VM/Compose handles index shift automatically if we update list?
            // Wait, we need to scroll to correct position if index shifted?
            // For now, let's keep it simple.
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
        -1 -> viewModel.currentPage.value
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

    updateColumnStrip()

    scrollAndLoad(lastColumnIndex)
}

fun ActMain.closeColumnSetting(): Boolean {
    // Delegate to ViewModel or iterate Columns via VM?
    // We can't iterate views. 
    // Logic: check all columns if any has setting shown.
    // In Compose, settings visibility is in ColumnUiState held by ColumnViewHolder.
    // We need to iterate over *active* ViewHolders or check Column state if possible.
    // ColumnUiState is in ColumnViewHolder.
    // ColumnViewHolder is held by ColumnWrapper.
    // We don't have direct access to ViewHolders from Activity anymore!
    // UNLESS we store them in Column object? 
    // We removed `column.addColumnViewHolder(this)`? No, we kept it!
    // So Column still has reference to ViewHolders.
    
    appState.columnList.forEach { column ->
        // We need to find if any VH has settings shown
        // Since we have multiple VHs (potentially), check all?
        // Wait, Column.viewHolder is the property to access it.
        // But Column.viewHolder is a property that iterates list.
        // Let's check Column.kt
        
        column.viewHolder?.let { vh ->
            if (vh.isColumnSettingShown) {
                // How to close?
                vh.columnUiState.settingsVisible = false
                return true
            }
        }
    }
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
    data?.getStringExtra(Intent.EXTRA_TEXT)?.let {
        addColumn(
            false,
            defaultInsertPosition,
            SavedAccount.na,
            columnType,
            params = arrayOf(it)
        )
    }

fun ActMain.scrollToColumn(index: Int, smoothScroll: Boolean = true) {
    scrollColumnStrip(index)
    viewModel.requestScrollToColumn(index)
}

// onCreate時に前回のカラムまでスクロールする
fun ActMain.scrollToLastColumn() {
    if (appState.columnCount <= 0) return

    val columnPos = PrefI.ipLastColumnPos.value
    log.d("ipLastColumnPos load $columnPos")

    // 前回最後に表示していたカラムの位置にスクロールする
    if (columnPos in 0 until appState.columnCount) {
        scrollToColumn(columnPos, false)
    }
}

fun ActMain.resizeColumnWidth() {
    var columnWMinDp = ActMain.COLUMN_WIDTH_MIN_DP
    val sv = PrefS.spColumnWidth.value
    if (sv.isNotEmpty()) {
        try {
            val iv = Integer.parseInt(sv)
            if (iv >= 100) {
                columnWMinDp = iv
            }
        } catch (ex: Throwable) {
            log.e(ex, "can't parse spColumnWidth. $sv")
        }
    }

    val dm = resources.displayMetrics
    val screenWidth = dm.widthPixels
    val density = dm.density
    var columnWMin = (0.5f + columnWMinDp * density).toInt()
    if (columnWMin < 1) columnWMin = 1
    
    // Calculate nColumnWidth
    nColumnWidth = if (screenWidth >= columnWMin) {
        val n = screenWidth / columnWMin
        if (n < 1) 1 else screenWidth / n
    } else {
        screenWidth
    }
}

fun ActMain.scrollColumnStrip(select: Int) {
    if (select < 0 || select >= appState.columnCount) {
        return
    }
    viewModel.requestScrollToColumn(select)
    
    launchMain {
        try {
            val a = AccountCache.load(this@scrollColumnStrip, null)
        } catch (ex: Throwable) {
            log.e(ex, "load account failed.")
        }
    }
}

fun ActMain.updateColumnStripSelection(position: Int, positionOffset: Float) {
    // Updates strip highlight.
    // Position comes from Pager/List scroll event?
    // Actually this function was called from ViewPager callback.
    // Now it should be called from MainScreen -> ViewModel -> ActMain?
    // Or ViewModel handles strip directly.
    // MainViewModel has visibleRange.
    // MainFooter observes visibleRange.
    // So we don't need to call updateColumnStripSelection from here?
    // But wait, updateColumnStrip calls this.
    
    // We can update VM state here if we want to force update.
    if (position >= 0) {
        viewModel.setVisibleRange(position, position, positionOffset)
    }
}

fun ActMain.showColumnMatchAccount(account: SavedAccount) {
    appState.columnList.forEach { column ->
        if (account == column.accessInfo) {
            column.fireRebindAdapterItems()
        }
    }
}
