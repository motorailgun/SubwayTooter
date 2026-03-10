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
import jp.juggler.subwaytooter.compose.StScreen
import jp.juggler.subwaytooter.dialog.DlgConfirm.confirm
import jp.juggler.subwaytooter.table.MutedApp
import jp.juggler.subwaytooter.table.appDatabase
import jp.juggler.subwaytooter.util.getStColorTheme
import jp.juggler.util.backPressed
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.log.LogCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActMutedApp : ComponentActivity() {

    companion object {
        private val log = LogCategory("ActMutedApp")
    }

    private val items = mutableStateListOf<MutedApp>()
    private val daoMutedApp by lazy { MutedApp.Access(appDatabase) }

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
                title = getString(R.string.muted_app),
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
                    items(items, key = { it.name }) { item ->
                        MuteItemRow(item.name) { delete(item) }
                    }
                }
            }
        }
        loadData()
    }

    private fun loadData() {
        launchAndShowError {
            val list = withContext(Dispatchers.IO) {
                daoMutedApp.listAll()
            }
            items.clear()
            items.addAll(list)
        }
    }

    private fun delete(item: MutedApp) {
        launchAndShowError {
            confirm(R.string.delete_confirm, item.name)
            daoMutedApp.delete(item.name)
            items.remove(item)
        }
    }
}
