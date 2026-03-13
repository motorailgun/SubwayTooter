package jp.juggler.subwaytooter.actmain

import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.column.fireShowColumnHeader
import jp.juggler.subwaytooter.pref.PrefL
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.table.daoSavedAccount

// デフォルトの投稿先アカウントを探す。アカウント選択が必要な状況ならnull
val ActMain.currentPostTarget: SavedAccount?
    get() {
        val dbId = PrefL.lpDefaultPostAccount.value
        if (dbId != -1L) {
            val a = daoSavedAccount.loadAccount(dbId)
            if (a != null && !a.isPseudo) return a
        }

        if (!isComposeStateInitialized()) return null
        
        if (composeState.isTablet) {
            val visibleIndices = composeState.tabletListState.layoutInfo.visibleItemsInfo.map { it.index }
            val accounts = ArrayList<SavedAccount>()
            for (i in visibleIndices) {
                appState.column(i)?.accessInfo?.let { a ->
                    if (a.isPseudo) return null // Force selection if pseudo account is visible
                    if (accounts.none { it == a }) accounts.add(a)
                }
            }
            return if (accounts.size == 1) accounts.first() else null
        } else {
            val c = appState.column(composeState.pagerState.currentPage)
            return c?.accessInfo?.takeIf { !it.isPseudo }
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
