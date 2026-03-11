package jp.juggler.subwaytooter.columnviewholder

import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.column.*
import jp.juggler.util.data.notZero
import jp.juggler.util.log.LogCategory
import jp.juggler.util.ui.AdapterChange

private val log = LogCategory("ColumnViewHolderShow")

// カラムヘッダなど、負荷が低い部分の表示更新
fun ColumnViewHolder.showColumnHeader() {
    activity.handler.removeCallbacks(procShowColumnHeader)
    activity.handler.postDelayed(procShowColumnHeader, 50L)
}

fun ColumnViewHolder.showColumnStatus() {
    activity.handler.removeCallbacks(procShowColumnStatus)
    activity.handler.postDelayed(procShowColumnStatus, 50L)
}

fun ColumnViewHolder.showColumnColor() {
    val column = this.column
    if (column == null || column.isDispose.get()) return

    val ui = columnUiState

    // カラムヘッダ背景色
    val headerBgColor = column.getHeaderBackgroundColor()
    ui.headerBgColor = headerBgColor
    ui.headerBgColorIsDefault = (column.headerBgColor == 0)

    // カラムヘッダ文字色(A) - column name / icons
    ui.headerNameColor = column.getHeaderNameColor()

    // カラムヘッダ文字色(B) - page number / status
    ui.headerPageNumberColor = column.getHeaderPageNumberColor()

    // カラム内部の背景色
    ui.columnBgColor = column.columnBgColor.notZero() ?: Column.defaultColorContentBg

    // カラム内部の背景画像
    ui.columnBgImageAlpha = column.columnBgImageAlpha
    loadBackgroundImage(column.columnBgImage)

    // エラー表示のテキストカラー
    ui.contentColor = column.getContentColor()

    // カラム色を変更したらクイックフィルタの色も変わる場合がある
    showQuickFilter()

    showAnnouncements(force = false)
}

fun ColumnViewHolder.showError(message: String) {
    hideRefreshError()

    val ui = columnUiState
    ui.isRefreshing = false
    ui.showRefreshLayout = false
    ui.showLoading = true
    ui.loadingMessage = message
    ui.showConfirmMail = (column?.accessInfo?.isConfirmed == false)
}

fun ColumnViewHolder.showColumnCloseButton() {
    column?.dontClose?.let { columnUiState.closeButtonEnabled = !it }
}

internal fun ColumnViewHolder.showContent(
    reason: String,
    changeList: List<AdapterChange>? = null,
    reset: Boolean = false,
) {
    // Notify Compose TimelineState of data changes
    try {
        val column = this.column
        val ts = this.timelineState
        if (column != null && ts != null) {
            ts.notifyChange(column, reason, changeList, reset)
        }
    } catch (ex: Throwable) {
        log.e(ex, "notifyChange failed.")
    }

    showColumnHeader()
    showColumnStatus()

    val column = this.column
    if (column == null || column.isDispose.get()) {
        showError("column was disposed.")
        return
    }

    if (!column.bFirstInitialized) {
        showError("initializing")
        return
    }

    if (column.bInitialLoading) {
        var message: String? = column.taskProgress
        if (message == null) message = "loading?"
        showError(message)
        return
    }

    val initialLoadingError = column.mInitialLoadingError
    if (initialLoadingError.isNotEmpty()) {
        showError(initialLoadingError)
        return
    }

    val ts = this.timelineState
    if (ts == null || (ts.items.isEmpty() && column.listData.isEmpty())) {
        showError(activity.getString(R.string.list_empty))
        return
    }

    // Clear loading/error state in TimelineState
    ts.isLoading = false
    ts.errorMessage = null

    val ui = columnUiState
    ui.showLoading = false
    ui.showRefreshLayout = true

    if (column.bRefreshLoading) {
        hideRefreshError()
    } else {
        ui.isRefreshing = false
        showRefreshError()
    }
    procRestoreScrollPosition.run()
}

fun ColumnViewHolder.showColumnSetting(show: Boolean): Boolean {
    columnUiState.settingsVisible = show
    return show
}

fun ColumnViewHolder.showRefreshError() {
    val column = column
    if (column == null) {
        hideRefreshError()
        return
    }

    val refreshError = column.mRefreshLoadingError
    if (refreshError.isEmpty()) {
        hideRefreshError()
        return
    }

    val ui = columnUiState
    ui.refreshErrorText = refreshError
    ui.refreshErrorSingleLine = when (column.mRefreshLoadingErrorPopupState) {
        0 -> false // initially expanded
        1 -> true  // tap to minimize
        else -> false
    }

    if (!bRefreshErrorWillShown) {
        bRefreshErrorWillShown = true
        ui.refreshErrorVisible = true
    }
}

fun ColumnViewHolder.hideRefreshError() {
    if (!bRefreshErrorWillShown) return
    bRefreshErrorWillShown = false
    columnUiState.refreshErrorVisible = false
}
