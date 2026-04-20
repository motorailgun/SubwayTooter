package es.ariaontheplanet.quasar

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.ariaontheplanet.quasar.actcolumnlist.ColumnListItem
import es.ariaontheplanet.quasar.actcolumnlist.ColumnListViewModel
import es.ariaontheplanet.quasar.api.showApiError
import es.ariaontheplanet.quasar.column.ColumnEncoder
import es.ariaontheplanet.quasar.dialog.DlgConfirm.confirm
import jp.juggler.util.backPressed
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.int
import jp.juggler.util.log.LogCategory
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ActColumnList : ComponentActivity() {

    companion object {

        private val log = LogCategory("ActColumnList")
        internal const val TMP_FILE_COLUMN_LIST = "tmp_column_list"

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

    private val initialSelection: Int by lazy {
        intent?.int(EXTRA_SELECTION) ?: -1
    }

    private val viewModel: ColumnListViewModel by viewModel { parametersOf(initialSelection) }

    override fun onCreate(savedInstanceState: Bundle?) {
        backPressed {
            makeResult(-1)
            finish()
        }

        super.onCreate(savedInstanceState)
        App1.setActivityTheme(this)

        setContent { ColumnListScreen() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_SELECTION, viewModel.uiState.value.oldSelection)
        launchAndShowError { viewModel.persistColumnsToTempFile() }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ColumnListScreen() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                handleDelete(item)
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
                        ColumnListItemRow(
                            item = item,
                            index = index,
                            total = state.items.size,
                            onClick = { performItemSelected(index) },
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
    private fun ColumnListItemRow(
        item: ColumnListItem,
        index: Int,
        total: Int,
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
                        onClick = { viewModel.moveItem(index, index - 1) },
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
                        onClick = { viewModel.moveItem(index, index + 1) },
                        enabled = index < total - 1,
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

    private fun handleDelete(item: ColumnListItem) {
        launchMain {
            try {
                if (item.json.optBoolean(ColumnEncoder.KEY_DONT_CLOSE, false)) {
                    confirm(R.string.confirm_remove_column_mark_as_dont_close)
                }
                viewModel.deleteItem(item)
            } catch (ex: Throwable) {
                showApiError(ex)
            }
        }
    }

    private fun makeResult(newSelection: Int) {
        val intent = viewModel.buildResult(newSelection)
        setResult(Activity.RESULT_OK, intent)
    }

    private fun performItemSelected(idx: Int) {
        makeResult(idx)
        finish()
    }
}
