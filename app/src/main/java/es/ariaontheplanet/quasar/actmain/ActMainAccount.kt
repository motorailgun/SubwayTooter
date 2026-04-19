package es.ariaontheplanet.quasar.actmain

import es.ariaontheplanet.quasar.ActMain
import es.ariaontheplanet.quasar.column.fireShowColumnHeader
import es.ariaontheplanet.quasar.pref.PrefL
import es.ariaontheplanet.quasar.table.SavedAccount
import es.ariaontheplanet.quasar.table.daoSavedAccount

// デフォルトの投稿先アカウントを探す。アカウント選択が必要な状況ならnull
val ActMain.currentPostTarget: SavedAccount?
    get() {
        val dbId = PrefL.lpDefaultPostAccount.value
        if (dbId != -1L) {
            val a = daoSavedAccount.loadAccount(dbId)
            if (a != null && !a.isPseudo) return a
        }
        
        val current = viewModel.currentPage.value
        val vr = viewModel.visibleRange.value
        
        if (vr.first == -1) {
            val c = appState.column(current)
            return if (c == null || c.accessInfo.isPseudo) null else c.accessInfo
        } else {
            val accounts = ArrayList<SavedAccount>()
            for (i in vr.first..vr.last) {
                appState.column(i)?.let { c ->
                    val a = c.accessInfo
                    if (a.isPseudo) return null
                    if (accounts.none { it == a }) accounts.add(a)
                }
            }
            return if (accounts.size == 1) accounts.first() else null
        }
    }

fun ActMain.reloadAccountSetting(
    newAccounts: List<SavedAccount>,
) {
    for (column in appState.columnList) {
        val a = column.accessInfo
        val b = newAccounts.find { it.acct == a.acct }
        if (!a.isNA && b != null) daoSavedAccount.reloadSetting(a, b)
        column.fireShowColumnHeader()
    }
}

fun ActMain.reloadAccountSetting(account: SavedAccount) {
    val newData = daoSavedAccount.loadAccount(account.db_id)
        ?: return
    for (column in appState.columnList) {
        val a = column.accessInfo
        if (a.acct != newData.acct) continue
        if (!a.isNA) daoSavedAccount.reloadSetting(a, newData)
        column.fireShowColumnHeader()
    }
}
