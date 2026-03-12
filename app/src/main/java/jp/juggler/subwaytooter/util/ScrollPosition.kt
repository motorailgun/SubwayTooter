package jp.juggler.subwaytooter.util

import jp.juggler.subwaytooter.columnviewholder.ColumnViewHolder

class ScrollPosition {

    var adapterIndex: Int

    // 先頭要素のピクセルオフセット。
    val offset: Int

    val isHead: Boolean
        get() = adapterIndex == 0 && offset >= 0

    override fun toString(): String = "ScrlPos($adapterIndex,$offset)"

    constructor(adapterIndex: Int = 0) {
        this.adapterIndex = adapterIndex
        this.offset = 0
    }

    constructor(adapterIndex: Int, offset: Int) {
        this.adapterIndex = adapterIndex
        this.offset = offset
    }

    constructor(holder: ColumnViewHolder) {
        val lls = holder.lazyListState
        if (lls != null) {
            adapterIndex = lls.firstVisibleItemIndex
            offset = lls.firstVisibleItemScrollOffset
        } else {
            adapterIndex = 0
            offset = 0
        }
    }
}
