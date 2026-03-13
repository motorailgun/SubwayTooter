package jp.juggler.subwaytooter

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.juggler.subwaytooter.api.entity.Acct
import jp.juggler.subwaytooter.api.showApiError
import jp.juggler.subwaytooter.column.ColumnEncoder
import jp.juggler.subwaytooter.column.ColumnType
import jp.juggler.subwaytooter.dialog.DlgConfirm.confirm
import jp.juggler.util.backPressed
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.data.JsonObject
import jp.juggler.util.data.toJsonArray
import jp.juggler.util.int
import jp.juggler.util.log.LogCategory

class ActColumnList : ComponentActivity() {

    companion object {

        private val log = LogCategory("ActColumnList")
        private const val TMP_FILE_COLUMN_LIST = "tmp_column_list"

        // リザルトに使うのでpublic
        const val EXTRA_ORDER = "order"
        const val EXTRA_SELECTION = "selection"

        fun createIntent(activity: ActMain, currentItem: Int) =
            Intent(activity, ActColumnList::class.java).apply {
                val array = activity.appState.encodeColumnList()
                AppState.saveColumnList(activity, TMP_FILE_COLUMN_LIST, array)
                putExtra(EXTRA_SELECTION, currentItem)
            }
    }

    // リスト要素のデータ
    internal class MyItem(
        val json: JsonObject,
        val id: Long,
        val name: String = json.optString(ColumnEncoder.KEY_COLUMN_NAME),
        val acct: Acct = Acct.parse(json.optString(ColumnEncoder.KEY_COLUMN_ACCESS_ACCT)),
        val acctName: String = json.optString(ColumnEncoder.KEY_COLUMN_ACCESS_STR),
        val oldIndex: Int = json.optInt(ColumnEncoder.KEY_OLD_INDEX),
        val type: ColumnType = ColumnType.parse(json.optInt(ColumnEncoder.KEY_TYPE)),
        val acctColorBg: Int = json.optInt(ColumnEncoder.KEY_COLUMN_ACCESS_COLOR_BG, 0),
        val acctColorFg: Int = json.optInt(ColumnEncoder.KEY_COLUMN_ACCESS_COLOR, 0),
        val columnColorFg: Int = json.optInt(ColumnEncoder.KEY_HEADER_TEXT_COLOR, 0),
        val columnColorBg: Int = json.optInt(ColumnEncoder.KEY_HEADER_BACKGROUND_COLOR, 0),
        var bOldSelection: Boolean = false,
    )

    private val columns = mutableStateListOf<MyItem>()
    private var oldSelection = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        backPressed {
            makeResult(-1)
            finish()
        }

        super.onCreate(savedInstanceState)
        App1.setActivityTheme(this)

        setContent { ColumnListScreen() }

        val selection = savedInstanceState?.int(EXTRA_SELECTION)
            ?: intent?.int(EXTRA_SELECTION)
            ?: -1
        restoreData(selection)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_SELECTION, oldSelection)
        val array = columns.map { it.json }.toJsonArray()
        AppState.saveColumnList(this, TMP_FILE_COLUMN_LIST, array)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ColumnListScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
            ) {
                itemsIndexed(columns, key = { _, item -> item.id }) { index, item ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                handleDelete(item, index)
                            }
                            false // don't auto-dismiss
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Text(
                                    text = stringResource(R.string.delete),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 20.sp,
                                )
                            }
                        },
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                        modifier = Modifier.animateItem(),
                    ) {
                        ColumnListItem(
                            item = item,
                            index = index,
                            onClick = { performItemSelected(item) },
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.column_list_desc),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }

    @Composable
    private fun ColumnListItem(
        item: MyItem,
        index: Int,
        onClick: () -> Unit,
    ) {
        val acctColorFg = if (item.acctColorFg != 0) Color(item.acctColorFg) else MaterialTheme.colorScheme.onSurfaceVariant
        val columnColorFg = if (item.columnColorFg != 0) Color(item.columnColorFg) else MaterialTheme.colorScheme.onSurface
        val columnColorBg = if (item.columnColorBg != 0) Color(item.columnColorBg) else MaterialTheme.colorScheme.surface

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(columnColorBg)
                .clickable(onClick = onClick),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(start = 12.dp, top = 3.dp, end = 0.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Account name
                    Text(
                        text = item.acctName,
                        color = acctColorFg,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .then(
                                if (item.acctColorBg != 0) Modifier.background(Color(item.acctColorBg))
                                else Modifier
                            )
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                    )
                    // Column icon + name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(item.type.iconId(item.acct)),
                            contentDescription = null,
                            tint = columnColorFg,
                            modifier = Modifier
                                .size(32.dp)
                                .padding(end = 4.dp),
                        )
                        Text(
                            text = item.name,
                            color = columnColorFg,
                            fontSize = 18.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Selection indicator
                if (item.bOldSelection) {
                    Icon(
                        painter = painterResource(R.drawable.ic_eye),
                        contentDescription = stringResource(R.string.last_selection),
                        tint = columnColorFg,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                // Reorder buttons
                Column(
                    modifier = Modifier
                        .width(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(start = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    IconButton(
                        onClick = { moveItem(index, index - 1) },
                        enabled = index > 0,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_drop_up),
                            contentDescription = stringResource(R.string.previous),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(
                        onClick = { moveItem(index, index + 1) },
                        enabled = index < columns.size - 1,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_drop_down),
                            contentDescription = stringResource(R.string.next),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }

    private fun moveItem(from: Int, to: Int) {
        if (to < 0 || to >= columns.size) return
        val item = columns.removeAt(from)
        columns.add(to, item)
    }

    private fun handleDelete(item: MyItem, index: Int) {
        launchMain {
            try {
                if (item.json.optBoolean(ColumnEncoder.KEY_DONT_CLOSE, false)) {
                    confirm(R.string.confirm_remove_column_mark_as_dont_close)
                }
                columns.remove(item)
            } catch (ex: Throwable) {
                showApiError(ex)
            }
        }
    }

    private fun restoreData(ivSelection: Int) {
        oldSelection = ivSelection
        columns.clear()
        try {
            AppState.loadColumnList(applicationContext, TMP_FILE_COLUMN_LIST)
                ?.objectList()
                ?.forEachIndexed { index, src ->
                    try {
                        val item = MyItem(src, index.toLong())
                        if (oldSelection == item.oldIndex) {
                            item.bOldSelection = true
                        }
                        columns.add(item)
                    } catch (ex: Throwable) {
                        log.e(ex, "restoreData: item decode failed.")
                    }
                }
        } catch (ex: Throwable) {
            log.e(ex, "restoreData failed.")
        }
    }

    private fun makeResult(newSelection: Int) {
        val intent = Intent()

        if (newSelection in 0 until columns.size) {
            intent.putExtra(EXTRA_SELECTION, newSelection)
        } else {
            columns.forEachIndexed { i, item ->
                if (item.bOldSelection) {
                    intent.putExtra(EXTRA_SELECTION, i)
                    return@forEachIndexed
                }
            }
        }

        val orderList = ArrayList<Int>()
        for (item in columns) {
            orderList.add(item.oldIndex)
        }
        intent.putExtra(EXTRA_ORDER, orderList)

        setResult(Activity.RESULT_OK, intent)
    }

    private fun performItemSelected(item: MyItem) {
        val idx = columns.indexOf(item)
        makeResult(idx)
        finish()
    }
}
