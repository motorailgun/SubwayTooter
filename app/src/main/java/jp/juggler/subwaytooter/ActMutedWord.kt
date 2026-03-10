package jp.juggler.subwaytooter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.compose.StScreen
import jp.juggler.subwaytooter.dialog.DlgConfirm.confirm
import jp.juggler.subwaytooter.table.MutedWord
import jp.juggler.subwaytooter.table.daoMutedWord
import jp.juggler.subwaytooter.util.getStColorTheme
import jp.juggler.util.backPressed
import jp.juggler.util.coroutine.AppDispatchers
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.log.LogCategory
import kotlinx.coroutines.withContext

class ActMutedWord : ComponentActivity() {

    companion object {
        private val log = LogCategory("ActMutedWord")
    }

    private val items = mutableStateListOf<MutedWord>()

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
                title = getString(R.string.muted_word),
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
                    item {
                        Text(
                            text = stringResource(R.string.refresh_after_ummute),
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
                daoMutedWord.listAll()
            }
            items.clear()
            items.addAll(list)
        }
    }

    private fun delete(item: MutedWord) {
        launchAndShowError {
            confirm(R.string.delete_confirm, item.name)
            daoMutedWord.delete(item.name)
            items.remove(item)
        }
    }
}

@Composable
internal fun MuteItemRow(name: String, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete",
            )
        }
    }
}
