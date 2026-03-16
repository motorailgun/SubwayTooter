package jp.juggler.subwaytooter.actmain

import jp.juggler.subwaytooter.ActMain
import jp.juggler.subwaytooter.column.fireShowColumnHeader
import jp.juggler.subwaytooter.pref.PrefL
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.table.daoSavedAccount

// デフォルトの投稿先アカウントを探す。アカウント選択が必要な状況ならnull
val ActMain.currentPostTarget: SavedAccount?
    get() {
        // 1. Check default account preference
        val dbId = PrefL.lpDefaultPostAccount.value
        if (dbId != -1L) {
            val a = daoSavedAccount.loadAccount(dbId)
            if (a != null && !a.isPseudo) return a
        }

        // 2. Check current column(s)
        val statePhone = composePagerState
        val stateTablet = composeTabletListState
        
        // Tablet mode
        if (nScreenColumn > 1 && stateTablet != null) {
             val visibleIndices = stateTablet.layoutInfo.visibleItemsInfo.map { it.index }
             val accounts = ArrayList<SavedAccount>()
             for (idx in visibleIndices) {
                 val c = appState.columnList.getOrNull(idx) ?: continue
                 val a = c.accessInfo
                 if (a.isPseudo) {
                     // If any visible column is pseudo (e.g. settings), force selection
                     return null 
                 }
                 if (accounts.none { it.acct == a.acct }) accounts.add(a)
             }
             
             return if (accounts.size == 1) accounts.first() else null
        }
        
        // Phone mode
        if (statePhone != null) {
            val idx = statePhone.currentPage
            val c = appState.columnList.getOrNull(idx)
            return if (c != null && !c.accessInfo.isPseudo) {
                c.accessInfo
            } else {
                null
            }
        }

        return null
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
