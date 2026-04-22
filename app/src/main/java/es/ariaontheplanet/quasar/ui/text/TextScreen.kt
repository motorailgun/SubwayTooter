package es.ariaontheplanet.quasar.ui.text

import android.app.SearchManager
import android.content.Intent
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.ariaontheplanet.quasar.ActMain
import es.ariaontheplanet.quasar.ui.keywordFilter.openKeywordFilter
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.RootActivity
import es.ariaontheplanet.quasar.api.entity.TootAccount
import es.ariaontheplanet.quasar.api.entity.TootStatus
import es.ariaontheplanet.quasar.compose.StScreen
import es.ariaontheplanet.quasar.dialog.pickAccount
import es.ariaontheplanet.quasar.nav.Route
import es.ariaontheplanet.quasar.table.SavedAccount
import es.ariaontheplanet.quasar.table.daoMutedWord
import es.ariaontheplanet.quasar.table.daoSavedAccount
import es.ariaontheplanet.quasar.util.CustomShare
import es.ariaontheplanet.quasar.util.CustomShareTarget
import es.ariaontheplanet.quasar.util.TootTextEncoder
import es.ariaontheplanet.quasar.util.copyToClipboard
import jp.juggler.util.coroutine.AppDispatchers
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.data.notEmpty
import jp.juggler.util.int
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.long
import jp.juggler.util.string
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val log = LogCategory("TextScreen")

internal const val EXTRA_TEXT = "text"
internal const val EXTRA_CONTENT_START = "content_start"
internal const val EXTRA_CONTENT_END = "content_end"
internal const val EXTRA_ACCOUNT_DB_ID = "account_db_id"

const val RESULT_SEARCH_NOTESTOCK = android.app.Activity.RESULT_FIRST_USER + 3

fun createTextIntent(
    activity: ActMain,
    accessInfo: SavedAccount,
    status: TootStatus,
): Intent = RootActivity.createIntent(activity, Route.Text).apply {
    putExtra(EXTRA_ACCOUNT_DB_ID, accessInfo.db_id)
    TootTextEncoder.encodeStatus(this, activity, accessInfo, status)
}

fun createTextIntent(
    activity: ActMain,
    accessInfo: SavedAccount,
    who: TootAccount,
): Intent = RootActivity.createIntent(activity, Route.Text).apply {
    putExtra(EXTRA_ACCOUNT_DB_ID, accessInfo.db_id)
    TootTextEncoder.encodeAccount(this, activity, accessInfo, who)
}

private class SearchResult(
    val items: List<IntRange> = emptyList(),
    val hasMore: Boolean = false,
    val error: String? = null,
) {
    val size = items.size

    fun findNext(curPos: Int, allowEqual: Boolean = false): IntRange? {
        var start = 0
        var end = items.size
        while (end > start) {
            val mid = (end + start) shr 1
            val item = items[mid]
            if (curPos in item) return when {
                allowEqual -> item
                else -> items.elementAtOrNull(mid + 1)
            }
            items.elementAtOrNull(mid - 1)?.let { prev ->
                if (curPos in prev.last + 1 until item.first) return item
            }
            when {
                curPos > item.first -> start = mid + 1
                else -> end = mid
            }
        }
        return null
    }

    fun findPrev(curPos: Int, allowEqual: Boolean = false): IntRange? {
        var start = 0
        var end = items.size
        while (end > start) {
            val mid = (end + start) shr 1
            val item = items[mid]
            if (curPos in item) return when {
                allowEqual -> item
                else -> items.elementAtOrNull(mid - 1)
            }
            items.elementAtOrNull(mid + 1)?.let { next ->
                if (curPos in item.last + 1 until next.first) return item
            }
            when {
                curPos > item.first -> start = mid + 1
                else -> end = mid
            }
        }
        return null
    }

    fun index(curPos: Int): Int? {
        var start = 0
        var end = items.size
        while (end > start) {
            val mid = (end + start) shr 1
            val item = items[mid]
            when {
                curPos in item -> return mid
                curPos > item.first -> start = mid + 1
                else -> end = mid
            }
        }
        return null
    }
}

@Composable
fun TextScreen() {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val intent = activity.intent
    val fullText = remember { intent.string(EXTRA_TEXT) ?: "" }
    val contentStart = remember { intent.int(EXTRA_CONTENT_START) ?: 0 }
    val contentEnd = remember { intent.int(EXTRA_CONTENT_END) ?: fullText.length }

    var account by remember { mutableStateOf<SavedAccount?>(null) }
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                annotatedString = AnnotatedString(fullText),
                selection = TextRange(
                    contentStart.coerceIn(0, fullText.length),
                    contentEnd.coerceIn(0, fullText.length),
                ),
            )
        )
    }
    var searchQuery by remember { mutableStateOf("") }
    var useRegex by remember { mutableStateOf(false) }
    var searchResult by remember { mutableStateOf(SearchResult()) }
    var currentMatchIndex by remember { mutableIntStateOf(-1) }
    var searchErrorText by remember { mutableStateOf<String?>(null) }

    val colorMatchBg = MaterialTheme.colorScheme.surfaceVariant
    val colorHighlightBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val colorSearchFormBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val colorTextContent = MaterialTheme.colorScheme.onSurface
    val colorErrorText = MaterialTheme.colorScheme.error

    val searchChannel = remember { Channel<Long>(capacity = Channel.CONFLATED) }

    val selectionOrAll: () -> String = {
        val sel = textFieldValue.selection
        when {
            sel.collapsed -> fullText
            else -> fullText.substring(
                sel.min.coerceIn(0, fullText.length),
                sel.max.coerceIn(0, fullText.length),
            )
        }
    }

    fun applyNewPos(newPos: IntRange?) {
        currentMatchIndex = newPos?.let { searchResult.index(it.first) } ?: -1
        val styledText = buildAnnotatedString {
            append(fullText)
            searchResult.items.forEach { range ->
                val bgColor = if (range == newPos) colorHighlightBg else colorMatchBg
                val end = (range.last + 1).coerceAtMost(fullText.length)
                val start = range.first.coerceAtMost(end)
                addStyle(SpanStyle(background = bgColor), start, end)
            }
        }
        val selection = if (newPos != null) {
            TextRange(
                newPos.first.coerceIn(0, fullText.length),
                (newPos.last + 1).coerceIn(0, fullText.length),
            )
        } else {
            textFieldValue.selection
        }
        textFieldValue = TextFieldValue(annotatedString = styledText, selection = selection)
    }

    suspend fun runSearch() {
        val keyword = searchQuery
        val content = fullText
        val regexMode = useRegex
        val result = withContext(AppDispatchers.IO) {
            try {
                val limit = 1000
                var hasMore = false
                val items = buildList {
                    when {
                        keyword.isEmpty() -> Unit
                        regexMode -> {
                            val re = keyword.toRegex(RegexOption.IGNORE_CASE)
                            var nextStart = 0
                            while (nextStart < content.length) {
                                val mr = re.find(content, startIndex = nextStart) ?: break
                                if (size >= limit) { hasMore = true; break }
                                add(mr.range)
                                nextStart = mr.range.last + 1
                            }
                        }
                        else -> {
                            var nextStart = 0
                            while (nextStart < content.length) {
                                val pos = content.indexOf(
                                    keyword,
                                    startIndex = nextStart,
                                    ignoreCase = true,
                                )
                                if (pos == -1) break
                                if (size >= limit) { hasMore = true; break }
                                val end = pos + keyword.length
                                add(pos until end)
                                nextStart = end
                            }
                        }
                    }
                }
                SearchResult(items = items, hasMore = hasMore)
            } catch (ex: Throwable) {
                log.e(ex, "search error.")
                SearchResult(error = ex.message)
            }
        }
        searchResult = result
        searchErrorText = result.error
        val newPos = result.findNext(textFieldValue.selection.start, allowEqual = true)
            ?: result.items.firstOrNull()
        applyNewPos(newPos)
    }

    fun postSearch() {
        coroutineScope.launch {
            runCatching { searchChannel.send(SystemClock.elapsedRealtime()) }
        }
    }

    LaunchedEffect(Unit) {
        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        account = intent.long(EXTRA_ACCOUNT_DB_ID)?.let { daoSavedAccount.loadAccount(it) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                searchChannel.receive()
                runSearch()
            } catch (ex: Throwable) {
                if (ex is CancellationException) break
                log.e(ex, "searchChannel failed.")
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { searchChannel.close() }
    }

    var menuExpanded by remember { mutableStateOf(false) }

    StScreen(
        title = stringResource(R.string.select_and_copy),
        onBack = { activity.finish() },
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.copy_st)) },
                    onClick = { menuExpanded = false; selectionOrAll().copyToClipboard(context) },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.send)) },
                    onClick = {
                        menuExpanded = false
                        selectionOrAll().trim().notEmpty()?.let { sendPlain(activity, it) }
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.search_web)) },
                    onClick = {
                        menuExpanded = false
                        selectionOrAll().trim().notEmpty()?.let { searchWeb(activity, it) }
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.toot_search_notestock)) },
                    onClick = {
                        menuExpanded = false
                        selectionOrAll().trim().notEmpty()?.let {
                            activity.setResult(
                                RESULT_SEARCH_NOTESTOCK,
                                Intent().putExtra(Intent.EXTRA_TEXT, it),
                            )
                            activity.finish()
                        }
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.translate)) },
                    onClick = {
                        menuExpanded = false
                        CustomShare.invokeText(
                            CustomShareTarget.Translate,
                            activity,
                            selectionOrAll(),
                        )
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.mute_word)) },
                    onClick = {
                        menuExpanded = false
                        activity.launchAndShowError {
                            selectionOrAll().trim().notEmpty()?.let {
                                daoMutedWord.save(it)
                                es.ariaontheplanet.quasar.App1
                                    .getAppState(activity).onMuteUpdated()
                                activity.showToast(false, R.string.word_was_muted)
                            }
                        }
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.keyword_filter)) },
                    onClick = {
                        menuExpanded = false
                        selectionOrAll().trim().notEmpty()?.let { text ->
                            val acc = account
                            if (acc?.isPseudo == false && acc.isMastodon) {
                                openKeywordFilter(activity, acc, initialPhrase = text)
                            } else {
                                launchMain {
                                    activity.pickAccount(
                                        bAllowPseudo = false,
                                        bAllowMisskey = false,
                                        bAllowMastodon = true,
                                        bAuto = false,
                                    )?.let {
                                        openKeywordFilter(activity, it, initialPhrase = text)
                                    }
                                }
                            }
                        }
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.highlight_word)) },
                    onClick = {
                        menuExpanded = false
                        selectionOrAll().trim().notEmpty()?.let {
                            activity.startActivity(
                                RootActivity.createIntent(
                                    activity,
                                    Route.HighlightWordEdit(initialText = it),
                                )
                            )
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Search bar
            val hasKeyword = searchQuery.isNotEmpty()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorSearchFormBg)
                    .padding(horizontal = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            postSearch()
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.search)) },
                        singleLine = true,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = useRegex,
                            onCheckedChange = {
                                useRegex = it
                                postSearch()
                            },
                        )
                        Text(
                            text = stringResource(R.string.toggle_regexp),
                            style = TextStyle(fontSize = 14.sp),
                        )
                    }
                    IconButton(
                        onClick = { searchQuery = ""; postSearch() },
                        enabled = hasKeyword,
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.clear))
                    }
                }

                val error = searchErrorText
                if (hasKeyword && !error.isNullOrBlank()) {
                    Text(
                        text = error,
                        color = colorErrorText,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (hasKeyword) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = activity.getString(
                                R.string.search_result,
                                if (currentMatchIndex >= 0) currentMatchIndex + 1 else 0,
                                searchResult.size,
                                if (searchResult.hasMore) "+" else "",
                            ),
                        )
                        IconButton(
                            onClick = {
                                val newPos = searchResult.findPrev(
                                    textFieldValue.selection.start,
                                    allowEqual = false,
                                ) ?: searchResult.items.lastOrNull()
                                applyNewPos(newPos)
                            },
                            enabled = searchResult.size > 1,
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = stringResource(R.string.previous),
                            )
                        }
                        IconButton(
                            onClick = {
                                val newPos = searchResult.findNext(
                                    textFieldValue.selection.start,
                                    allowEqual = false,
                                ) ?: searchResult.items.firstOrNull()
                                applyNewPos(newPos)
                            },
                            enabled = searchResult.size > 1,
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.next),
                            )
                        }
                    }
                }
            }

            // Read-only long-text selection area
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    // Read-only: only accept selection changes.
                    textFieldValue = TextFieldValue(
                        annotatedString = textFieldValue.annotatedString,
                        selection = newValue.selection,
                    )
                },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp),
                textStyle = TextStyle(color = colorTextContent),
                cursorBrush = SolidColor(colorTextContent),
            )
        }
    }
}

private fun sendPlain(activity: ComponentActivity, text: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        activity.startActivity(intent)
    } catch (ex: Throwable) {
        log.e(ex, "send failed.")
        activity.showToast(ex, "send failed.")
    }
}

private fun searchWeb(activity: ComponentActivity, text: String) {
    try {
        val intent = Intent(Intent.ACTION_WEB_SEARCH)
            .putExtra(SearchManager.QUERY, text)
        if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivity(intent)
        }
    } catch (ex: Throwable) {
        log.e(ex, "search failed.")
        activity.showToast(ex, "search failed.")
    }
}
