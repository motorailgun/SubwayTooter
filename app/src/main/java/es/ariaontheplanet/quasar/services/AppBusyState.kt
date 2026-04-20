package es.ariaontheplanet.quasar.services

import es.ariaontheplanet.quasar.api.entity.TootStatus
import es.ariaontheplanet.quasar.table.SavedAccount

// Tracks which (account, status) pairs have an in-flight favourite / bookmark /
// boost action so the UI can debounce taps. Koin-managed single.
class AppBusyState {

    private val mapBusyFav = HashSet<String>()
    private val mapBusyBookmark = HashSet<String>()
    private val mapBusyBoost = HashSet<String>()

    private fun key(account: SavedAccount, status: TootStatus): String =
        account.acct.ascii + ":" + status.busyKey

    fun isBusyFav(account: SavedAccount, status: TootStatus): Boolean =
        mapBusyFav.contains(key(account, status))

    fun setBusyFav(account: SavedAccount, status: TootStatus) {
        mapBusyFav.add(key(account, status))
    }

    fun resetBusyFav(account: SavedAccount, status: TootStatus) {
        mapBusyFav.remove(key(account, status))
    }

    fun isBusyBookmark(account: SavedAccount, status: TootStatus): Boolean =
        mapBusyBookmark.contains(key(account, status))

    fun setBusyBookmark(account: SavedAccount, status: TootStatus) {
        mapBusyBookmark.add(key(account, status))
    }

    fun resetBusyBookmark(account: SavedAccount, status: TootStatus) {
        mapBusyBookmark.remove(key(account, status))
    }

    fun isBusyBoost(account: SavedAccount, status: TootStatus): Boolean =
        mapBusyBoost.contains(key(account, status))

    fun setBusyBoost(account: SavedAccount, status: TootStatus) {
        mapBusyBoost.add(key(account, status))
    }

    fun resetBusyBoost(account: SavedAccount, status: TootStatus) {
        mapBusyBoost.remove(key(account, status))
    }
}
