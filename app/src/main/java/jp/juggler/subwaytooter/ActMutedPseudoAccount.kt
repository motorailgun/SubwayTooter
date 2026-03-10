package jp.juggler.subwaytooter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import jp.juggler.subwaytooter.compose.StScreen
import jp.juggler.subwaytooter.dialog.DlgConfirm.confirm
import jp.juggler.subwaytooter.table.UserRelation
import jp.juggler.subwaytooter.table.daoUserRelation
import jp.juggler.subwaytooter.util.getStColorTheme
import jp.juggler.util.backPressed
import jp.juggler.util.coroutine.AppDispatchers
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.log.LogCategory
import kotlinx.coroutines.withContext

class ActMutedPseudoAccount : ComponentActivity() {

    companion object {
        private val log = LogCategory("ActMutedPseudoAccount")
    }

    private val items = mutableStateListOf<UserRelation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backPressed {
            setResult(RESULT_OK)
            finish()
        }
        App1.setActivityTheme(this)
        val colorScheme = getStColorTheme()
        setContent {
            StScreen(
                colorScheme = colorScheme,
                title = getString(R.string.muted_users_from_pseudo_account),
                onBack = {
                    setResult(RESULT_OK)
                    finish()
                },
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    items(items, key = { it.id }) { item ->
                        MuteItemRow(item.whoId) { delete(item) }
                    }
                }
            }
        }
        launchAndShowError {
            val list = withContext(AppDispatchers.IO) {
                daoUserRelation.listPseudoMuted()
            }
            items.clear()
            items.addAll(list)
        }
    }

    private fun delete(item: UserRelation) {
        launchAndShowError {
            confirm(R.string.delete_confirm, item.whoId)
            daoUserRelation.deletePseudo(item.id)
            items.remove(item)
        }
    }
}
