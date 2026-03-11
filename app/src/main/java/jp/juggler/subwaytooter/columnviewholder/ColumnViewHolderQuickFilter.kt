package jp.juggler.subwaytooter.columnviewholder

import android.graphics.Color
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.column.*
import jp.juggler.subwaytooter.pref.PrefB
import jp.juggler.util.ui.applyAlphaMultiplier
import jp.juggler.util.ui.attrColor

fun ColumnViewHolder.clickQuickFilter(filter: Int) {
    column?.quickFilter = filter
    showQuickFilter()
    activity.appState.saveColumnList()
    column?.startLoading(ColumnLoadReason.SettingChange)
}

fun ColumnViewHolder.showQuickFilter() {
    val column = this.column ?: return
    val ui = columnUiState

    if (!column.isNotificationColumn) {
        ui.quickFilterVisible = false
        return
    }
    ui.quickFilterVisible = true

    ui.showQuickFilterReaction = column.isMisskey
    ui.showQuickFilterFavourite = !column.isMisskey

    val insideColumnSetting = PrefB.bpMoveNotificationsQuickFilter.value
    ui.quickFilterInsideSetting = insideColumnSetting

    if (insideColumnSetting) {
        val colorFg = activity.attrColor(R.attr.colorTextContent)
        val colorBgSelected = colorFg.applyAlphaMultiplier(0.25f)
        val colorBg = activity.attrColor(R.attr.colorColumnSettingBackground)
        ui.quickFilterFgColor = colorFg
        ui.quickFilterBgColor = colorBg
        ui.quickFilterSelectedBgColor = colorBgSelected
    } else {
        val colorBg = column.getHeaderBackgroundColor()
        val colorFg = column.getHeaderNameColor()
        val colorBgSelected = Color.rgb(
            (Color.red(colorBg) * 3 + Color.red(colorFg)) / 4,
            (Color.green(colorBg) * 3 + Color.green(colorFg)) / 4,
            (Color.blue(colorBg) * 3 + Color.blue(colorFg)) / 4
        )
        ui.quickFilterFgColor = colorFg
        ui.quickFilterBgColor = colorBg
        ui.quickFilterSelectedBgColor = colorBgSelected
    }

    ui.quickFilter = column.quickFilter
}
