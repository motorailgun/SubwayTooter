package jp.juggler.subwaytooter.column

import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.columnviewholder.*
import jp.juggler.util.coroutine.isMainThread
import jp.juggler.util.ui.AdapterChange

fun Column.removeColumnViewHolder(cvh: ColumnViewHolder) {
    val it = listViewHolder.iterator()
    while (it.hasNext()) {
        if (cvh == it.next()) it.remove()
    }
}

fun Column.removeColumnViewHolderByActivity(activity: ActMain) {
    val it = listViewHolder.iterator()
    while (it.hasNext()) {
        val cvh = it.next()
        if (cvh.activity == activity) {
            it.remove()
        }
    }
}

// 複数のリスナがある場合、最も新しいものを返す
val Column.viewHolder: ColumnViewHolder?
    get() = when {
        isDispose.get() -> null
        else -> listViewHolder.firstOrNull()
    }

fun Column.fireShowContent(
    reason: String,
    changeList: List<AdapterChange>? = null,
    reset: Boolean = false,
) {
    if (!isMainThread) error("fireShowContent: not on main thread.")
    timelineState?.notifyChange(this, reason, changeList, reset)
    viewHolder?.showContent(reason, changeList, reset)
}

fun Column.fireShowColumnHeader() {
    if (!isMainThread) error("fireShowColumnHeader: not on main thread.")
    timelineState?.forceRecompose()
    viewHolder?.showColumnHeader()
}

fun Column.fireShowColumnStatus() {
    if (!isMainThread) error("fireShowColumnStatus: not on main thread.")
    timelineState?.let {
        it.isLoading = bInitialLoading || bRefreshLoading
        it.errorMessage = mInitialLoadingError.takeIf { it.isNotEmpty() }
            ?: mRefreshLoadingError.takeIf { it.isNotEmpty() }
        // forceRecompose is not needed as isLoading/errorMessage are backed by MutableState
    }
    viewHolder?.showColumnStatus()
}

fun Column.fireColumnColor() {
    if (!isMainThread) error("fireColumnColor: not on main thread.")
    timelineState?.forceRecompose()
    viewHolder?.showColumnColor()
}

fun Column.fireRelativeTime() {
    if (!isMainThread) error("fireRelativeTime: not on main thread.")
    timelineState?.forceRecompose()
    viewHolder?.updateRelativeTime()
}

fun Column.fireRebindAdapterItems() {
    if (!isMainThread) error("fireRelativeTime: not on main thread.")
    timelineState?.forceRecompose()
    viewHolder?.rebindAdapterItems()
}
