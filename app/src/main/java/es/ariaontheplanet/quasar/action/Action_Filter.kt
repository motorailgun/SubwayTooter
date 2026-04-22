package es.ariaontheplanet.quasar.action

import es.ariaontheplanet.quasar.ActMain
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.ui.keywordFilter.openKeywordFilter
import es.ariaontheplanet.quasar.api.ApiPath
import es.ariaontheplanet.quasar.api.TootApiClient
import es.ariaontheplanet.quasar.api.TootApiResult
import es.ariaontheplanet.quasar.api.TootApiResultException
import es.ariaontheplanet.quasar.api.entity.EntityId
import es.ariaontheplanet.quasar.api.entity.TootFilter
import es.ariaontheplanet.quasar.api.runApiTask2
import es.ariaontheplanet.quasar.column.onFilterDeleted
import es.ariaontheplanet.quasar.dialog.DlgConfirm.confirm
import es.ariaontheplanet.quasar.dialog.actionsDialog
import es.ariaontheplanet.quasar.table.SavedAccount
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.log.showToast

// private val log = LogCategory("Action_Filter")

fun ActMain.openFilterMenu(accessInfo: SavedAccount, item: TootFilter?) {
    item ?: return
    val activity = this
    launchAndShowError {
        actionsDialog(getString(R.string.filter_of, item.displayString)) {
            action(getString(R.string.edit)) {
                openKeywordFilter(activity, accessInfo, item.id)
            }
            action(getString(R.string.delete)) {
                filterDelete(accessInfo, item)
            }
        }
    }
}

suspend fun TootApiClient.filterDelete(filterId: EntityId): TootApiResult {
    for (path in arrayOf(
        "/api/v2/filters/${filterId}",
        "/api/v1/filters/${filterId}",
    )) {
        try {
            return requestOrThrow(path = path)
        } catch (ex: TootApiResultException) {
            when (ex.result?.response?.code) {
                404 -> continue
                else -> throw ex
            }
        }
    }
    error("missing filter APIs.")
}

suspend fun TootApiClient.filterLoad(): List<TootFilter> {
    for (path in arrayOf(
        ApiPath.PATH_FILTERS_V2,
        ApiPath.PATH_FILTERS_V1,
    )) {
        try {
            val jsonArray = requestOrThrow(path).jsonArray
                ?: error("API response has no jsonArray.")
            return TootFilter.parseList(jsonArray)
                ?: error("TootFilter.parseList returns null.")
        } catch (ex: TootApiResultException) {
            when (ex.result?.response?.code) {
                404 -> continue
                else -> throw ex
            }
        }
    }
    error("missing filter APIs.")
}

fun ActMain.filterDelete(
    accessInfo: SavedAccount,
    filter: TootFilter,
) = launchAndShowError {
    confirm(R.string.filter_delete_confirm, filter.displayString)
    val newFilters = runApiTask2(accessInfo) { client ->
        client.filterDelete(filter.id)
        client.filterLoad()
    }
    showToast(false, R.string.delete_succeeded)
    for (column in appState.columnList) {
        if (column.accessInfo == accessInfo) {
            column.onFilterDeleted(filter, newFilters)
        }
    }
}
