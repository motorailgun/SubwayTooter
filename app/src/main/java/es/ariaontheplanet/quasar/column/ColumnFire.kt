package es.ariaontheplanet.quasar.column

import es.ariaontheplanet.quasar.columnviewholder.*
import jp.juggler.util.coroutine.isMainThread
import jp.juggler.util.ui.AdapterChange

// Extension property to get the active viewHolder for this column
// Uses the MainViewModel registry instead of the deprecated LinkedList
val Column.viewHolder: ColumnViewHolder?
    get() = when {
        isDispose.get() -> null
        else -> appState.mainViewModel?.getViewHolder(this)
    }

fun Column.fireShowContent(
    reason: String,
    changeList: List<AdapterChange>? = null,
    reset: Boolean = false,
) {
    if (!isMainThread) error("fireShowContent: not on main thread.")
    emitListDataSnapshot()
    viewHolder?.showContent(reason, changeList, reset)
}

fun Column.fireShowColumnHeader() {
    if (!isMainThread) error("fireShowColumnHeader: not on main thread.")
    viewHolder?.showColumnHeader()
}

fun Column.fireShowColumnStatus() {
    if (!isMainThread) error("fireShowColumnStatus: not on main thread.")
    viewHolder?.showColumnStatus()
}

fun Column.fireColumnColor() {
    if (!isMainThread) error("fireColumnColor: not on main thread.")
    viewHolder?.showColumnColor()
}

fun Column.fireRelativeTime() {
    if (!isMainThread) error("fireRelativeTime: not on main thread.")
    viewHolder?.updateRelativeTime()
}

fun Column.fireRebindAdapterItems() {
    if (!isMainThread) error("fireRelativeTime: not on main thread.")
    viewHolder?.rebindAdapterItems()
}
