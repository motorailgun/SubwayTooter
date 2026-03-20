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
    get() = phoneTab(
        { it.pager.currentItem },
        { -1 }
    )

// 新しいカラムをどこに挿入するか
// 現在のページの次の位置か、終端
val ActMain.defaultInsertPosition: Int
    get() = phoneTab(
        { it.pager.currentItem + 1 },
        { Integer.MAX_VALUE }
    )

// カラム追加後など、そのカラムにスクロールして初期ロードを行う
fun ActMain.scrollAndLoad(idx: Int) {
    val c = appState.column(idx) ?: return
    scrollToColumn(idx)
    c.startLoading(ColumnLoadReason.PageSelect)
}

fun ActMain.addColumn(column: Column, indexArg: Int): Int {
    val index = indexArg.clip(0, appState.columnCount)

    phoneOnly { env -> env.pager.adapter = null }

    appState.editColumnList {
        it.add(index, column)
    }

    phoneTab(
        { env -> env.pager.adapter = env.pagerAdapter },
        { env -> resizeColumnWidth(env) }
    )

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

    phoneOnly { env -> env.pager.adapter = null }

    appState.editColumnList {
        it.removeAt(idxColumn).dispose()
    }

    phoneTab(
        { env -> env.pager.adapter = env.pagerAdapter },
        { env -> resizeColumnWidth(env) }
    )

    updateColumnStrip()
}

fun ActMain.isVisibleColumn(idx: Int) = phoneTab(
    { env -> env.pager.currentItem == idx },
    { env -> idx >= 0 && idx in env.visibleColumnsIndices },
)

fun ActMain.updateColumnStrip() {
    // views.tvEmpty.vg(appState.columnCount == 0)

    // Update ViewModel with column list
    val uiList = appState.columnList.mapIndexed { index, column ->
        val ac = daoAcctColor.load(column.accessInfo)
        val acctColor = if (daoAcctColor.hasColorForeground(ac)) ac.colorFg else 0
        
        MainViewModel.ColumnUiState(
            index = index,
            iconId = column.getIconId(),
            acctColor = acctColor,
            headerNameColor = column.getHeaderNameColor(),
            headerBackgroundColor = column.getHeaderBackgroundColor(),
            contentDescription = column.getColumnName(true) ?: ""
        )
    }
    viewModel.setColumns(uiList)
    
    // Legacy logic removed: view manipulation of llColumnStrip
    
    updateColumnStripSelection(-1, -1f)
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
        phoneTab({ env ->
            val pageShowing = env.pager.currentItem
            removeColumn(column)
            if (pageShowing == page_delete) {
                scrollAndLoad(pageShowing - 1)
            }
        }, {
            removeColumn(column)
            scrollAndLoad(page_delete - 1)
        })
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
        -1 -> phoneTab(
            { it.pager.currentItem },
            { 0 }
        )
        else -> oldColumnIndex
    }

    phoneOnly { env -> env.pager.adapter = null }

    appState.editColumnList { list ->
        for (i in list.indices.reversed()) {
            val column = list[i]
            if (column.dontClose) continue
            list.removeAt(i).dispose()
            if (lastColumnIndex >= i) --lastColumnIndex
        }
    }

    phoneTab(
        { env -> env.pager.adapter = env.pagerAdapter },
        { env -> resizeColumnWidth(env) }
    )

    updateColumnStrip()

    scrollAndLoad(lastColumnIndex)
}

fun ActMain.closeColumnSetting(): Boolean {
    phoneTab({ env ->
        val vh = env.pagerAdapter.getColumnViewHolder(env.pager.currentItem)
        if (vh?.isColumnSettingShown == true) {
            vh.showColumnSetting(false)
            return@closeColumnSetting true
        }
    }, { env ->
        for (i in 0 until env.tabletLayoutManager.childCount) {
            val columnViewHolder = when (val v = env.tabletLayoutManager.getChildAt(i)) {
                null -> null
                else -> (env.tabletPager.getChildViewHolder(v) as? TabletColumnViewHolder)?.columnViewHolder
            }
            if (columnViewHolder?.isColumnSettingShown == true) {
                columnViewHolder.showColumnSetting(false)
                return@closeColumnSetting true
            }
        }
    })
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

    phoneOnly { env -> env.pager.adapter = null }

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

    phoneTab(
        { env -> env.pager.adapter = env.pagerAdapter },
        { env -> resizeColumnWidth(env) }
    )

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
    scrollColumnStrip(index)
    phoneTab(
        // スマホはスムーススクロール基本ありだがたまにしない
        { env ->
            log.d("ipLastColumnPos beforeScroll=${env.pager.currentItem}")
            env.pager.setCurrentItem(index, smoothScroll)
        },
        // タブレットでスムーススクロールさせると頻繁にオーバーランするので絶対しない
        { env ->
            log.d("ipLastColumnPos beforeScroll=${env.visibleColumnsIndices.first}")
            env.tabletPager.scrollToPosition(index)
        }
    )
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

    // 表示位置に合わせたイベントを発行
    phoneTab(
        { env -> onPageSelected(env.pager.currentItem) },
        { env -> resizeColumnWidth(env) }
    )
}

@SuppressLint("NotifyDataSetChanged")
fun ActMain.resizeColumnWidth(views: ActMainTabletViews) {

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

    var columnW: Int

    if (screenWidth < columnWMin * 2) {
        // 最小幅で2つ表示できないのなら1カラム表示
        nScreenColumn = 1
        columnW = screenWidth
    } else {

        // カラム最小幅から計算した表示カラム数
        nScreenColumn = screenWidth / columnWMin
        if (nScreenColumn < 1) nScreenColumn = 1

        // データのカラム数より大きくならないようにする
        // (でも最小は1)
        val columnCount = appState.columnCount
        if (columnCount > 0 && columnCount < nScreenColumn) {
            nScreenColumn = columnCount
        }

        // 表示カラム数から計算したカラム幅
        columnW = screenWidth / nScreenColumn

        // 最小カラム幅の1.5倍よりは大きくならないようにする
        val columnWMax = (0.5f + columnWMin * 1.5f).toInt()
        if (columnW > columnWMax) {
            columnW = columnWMax
        }
    }

    nColumnWidth = columnW // dividerの幅を含む

    val dividerWidth = (0.5f + 1f * density).toInt()
    columnW -= dividerWidth
    views.tabletPagerAdapter.columnWidth = columnW // dividerの幅を含まない
    // env.tablet_snap_helper.columnWidth = column_w //使われていない

    saveContentTextWidth(columnW) // dividerの幅を含まない

    // 並べ直す
    views.tabletPagerAdapter.notifyDataSetChanged()
}

fun ActMain.scrollColumnStrip(select: Int) {
    if (select < 0 || select >= appState.columnCount) {
        return
    }

    // Update ViewModel to scroll the strip
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
    handler.post(Runnable {
        if (isFinishing) return@Runnable

        if (appState.columnCount == 0) {
            viewModel.setVisibleRange(-1, -1, 0f)
        } else {
            phoneTab({ env ->
                if (position >= 0) {
                    viewModel.setVisibleRange(position, position, positionOffset)
                } else {
                    val c = env.pager.currentItem
                    viewModel.setVisibleRange(c, c, 0f)
                }
            }, { env ->
                val vs = env.tabletLayoutManager.findFirstVisibleItemPosition()
                val ve = env.tabletLayoutManager.findLastVisibleItemPosition()
                val vr = if (vs == RecyclerView.NO_POSITION || ve == RecyclerView.NO_POSITION) {
                    IntRange(-1, -2) // empty and less than zero
                } else {
                    IntRange(vs, min(ve, vs + nScreenColumn - 1))
                }
                var slideRatio = 0f
                if (vr.first <= vr.last) {
                    val child = env.tabletLayoutManager.findViewByPosition(vr.first)
                    slideRatio =
                        (abs((child?.left ?: 0) / nColumnWidth.toFloat())).clip(0f, 1f)
                }

                viewModel.setVisibleRange(vr.first, vr.last, slideRatio)
            })
        }
    })
}

fun ActMain.showColumnMatchAccount(account: SavedAccount) {
    appState.columnList.forEach { column ->
        if (account == column.accessInfo) {
            column.fireRebindAdapterItems()
        }
    }
}
