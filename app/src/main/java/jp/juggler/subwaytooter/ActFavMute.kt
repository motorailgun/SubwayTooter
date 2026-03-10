package jp.juggler.subwaytooter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.api.entity.Acct
import jp.juggler.subwaytooter.compose.StScreen
import jp.juggler.subwaytooter.dialog.DlgConfirm.confirm
import jp.juggler.subwaytooter.table.daoFavMute
import jp.juggler.subwaytooter.util.getStColorTheme
import jp.juggler.util.backPressed
import jp.juggler.util.coroutine.AppDispatchers
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.log.LogCategory
import kotlinx.coroutines.withContext

class ActFavMute : ComponentActivity() {

    companion object {
        private val log = LogCategory("ActFavMute")
    }

    internal class MyItem(val id: Long, val acct: Acct)

    private val items = mutableStateListOf<MyItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backPressed {
            setResult(RESULT_OK)
            finish()
        }
        App1.setActivityTheme(this)
        val stColorScheme = getStColorTheme()
        setContent {
            StScreen(
                stColorScheme = stColorScheme,
                title = getString(R.string.fav_muted_user),
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
                        MuteItemRow(item.acct.pretty) { delete(item) }
                    }
                    item {
                        Text(
                            text = stringResource(R.string.fav_muted_user_desc),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
                        )
                    }
                }
            }
        }
        loadData()
    }

    private fun loadData() {
        launchAndShowError {
            val list = withContext(AppDispatchers.IO) {
                daoFavMute.listAll().map {
                    MyItem(
                        id = it.id,
                        acct = Acct.parse(it.acct),
                    )
                }
            }
            items.clear()
            items.addAll(list)
        }
    }

    private fun delete(item: MyItem) {
        launchAndShowError {
            confirm(R.string.delete_confirm, item.acct.pretty)
            daoFavMute.delete(item.acct)
            items.remove(item)
        }
    }
}
