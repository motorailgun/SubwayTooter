package jp.juggler.subwaytooter

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import jp.juggler.subwaytooter.api.entity.TootAccount
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.compose.StScreen
import jp.juggler.subwaytooter.dialog.pickAccount
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.table.daoMutedWord
import jp.juggler.subwaytooter.table.daoSavedAccount
import jp.juggler.subwaytooter.util.CustomShare
import jp.juggler.subwaytooter.util.CustomShareTarget
import jp.juggler.subwaytooter.util.TootTextEncoder
import jp.juggler.subwaytooter.util.copyToClipboard
import jp.juggler.util.backPressed
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
class ActText : ComponentActivity() {

    companion object {

        internal val log = LogCategory("ActText")

        // internal const val RESULT_SEARCH_MSP = RESULT_FIRST_USER + 1
        // internal const val RESULT_SEARCH_TS = RESULT_FIRST_USER + 2
        internal const val RESULT_SEARCH_NOTESTOCK = RESULT_FIRST_USER + 3

        internal const val EXTRA_TEXT = "text"
        internal const val EXTRA_CONTENT_START = "content_start"
        internal const val EXTRA_CONTENT_END = "content_end"
        internal const val EXTRA_ACCOUNT_DB_ID = "account_db_id"

        fun createIntent(
            activity: ActMain,
            accessInfo: SavedAccount,
            status: TootStatus,
        ) = Intent(activity, ActText::class.java).apply {
            putExtra(EXTRA_ACCOUNT_DB_ID, accessInfo.db_id)
            TootTextEncoder.encodeStatus(this, activity, accessInfo, status)
        }

        fun createIntent(
            activity: ActMain,
            accessInfo: SavedAccount,
            who: TootAccount,
        ) = Intent(activity, ActText::class.java).apply {
            putExtra(EXTRA_ACCOUNT_DB_ID, accessInfo.db_id)
            TootTextEncoder.encodeAccount(this, activity, accessInfo, who)
        }
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

    // State
    private var fullText = ""
    private var textFieldValue by mutableStateOf(TextFieldValue())
    private var searchQuery by mutableStateOf("")
    private var useRegex by mutableStateOf(false)
    private var searchResult by mutableStateOf(SearchResult())
    private var currentMatchIndex by mutableIntStateOf(-1)
    private var searchErrorText by mutableStateOf<String?>(null)

    private val searchTextChannel = Channel<Long>(capacity = Channel.CONFLATED)

    private var account: SavedAccount? = null

    // Theme colors, set from MaterialTheme.colorScheme in composable
    private var colorMatchBg = Color.Unspecified
    private var colorHighlightBg = Color.Unspecified
    private var colorSearchFormBg = Color.Unspecified
    private var colorTextContent = Color.Unspecified
    private var colorErrorText = Color.Unspecified

    /**
     * 選択範囲、またはテキスト全体
     */
    private val selectionOrAll: String
        get() {
            val sel = textFieldValue.selection
            return when {
                sel.collapsed -> fullText
                else -> fullText.substring(
                    sel.min.coerceIn(0, fullText.length),
                    sel.max.coerceIn(0, fullText.length),
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App1.setActivityTheme(this)
        backPressed { finish() }

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        setContent { ActTextScreen() }

        // Search channel processing
        lifecycleScope.launch {
            while (true) {
                try {
                    searchTextChannel.receive()
                    searchTextImpl()
                } catch (ex: Throwable) {
                    if (ex is CancellationException) break
                    log.e(ex, "searchTextChannel failed.")
                }
            }
        }

        // Load content from intent
        launchAndShowError {
            account = intent.long(EXTRA_ACCOUNT_DB_ID)
                ?.let { daoSavedAccount.loadAccount(it) }

            if (savedInstanceState == null) {
                val sv = intent.string(EXTRA_TEXT) ?: ""
                val contentStart = intent.int(EXTRA_CONTENT_START) ?: 0
                val contentEnd = intent.int(EXTRA_CONTENT_END) ?: sv.length
                fullText = sv
                textFieldValue = TextFieldValue(
                    annotatedString = AnnotatedString(sv),
                    selection = androidx.compose.ui.text.TextRange(
                        contentStart.coerceIn(0, sv.length),
                        contentEnd.coerceIn(0, sv.length),
                    ),
                )
            }
        }
    }

    @Composable
    private fun ActTextScreen() {
        // Capture theme colors for use in non-Composable code
        colorMatchBg = MaterialTheme.colorScheme.surfaceVariant
        colorHighlightBg = MaterialTheme.colorScheme.surfaceContainerHigh
        colorSearchFormBg = MaterialTheme.colorScheme.surfaceContainerHigh
        colorTextContent = MaterialTheme.colorScheme.onSurface
        colorErrorText = MaterialTheme.colorScheme.error

        var menuExpanded by remember { mutableStateOf(false) }

        StScreen(
            title = getString(R.string.select_and_copy),
            onBack = { finish() },
            actions = {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.more),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.copy_st)) },
                        onClick = { menuExpanded = false; copy() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.send)) },
                        onClick = { menuExpanded = false; send() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.search_web)) },
                        onClick = { menuExpanded = false; searchWeb() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.toot_search_notestock)) },
                        onClick = { menuExpanded = false; searchToot(RESULT_SEARCH_NOTESTOCK) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.translate)) },
                        onClick = {
                            menuExpanded = false
                            CustomShare.invokeText(
                                CustomShareTarget.Translate,
                                this@ActText,
                                selectionOrAll,
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.mute_word)) },
                        onClick = { menuExpanded = false; muteWord() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.keyword_filter)) },
                        onClick = { menuExpanded = false; keywordFilter() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.highlight_word)) },
                        onClick = { menuExpanded = false; highlight() },
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                SearchBar()
                TextContent(Modifier.weight(1f))
            }
        }
    }

    @Composable
    private fun SearchBar() {
        val hasKeyword = searchQuery.isNotEmpty()
        val result = searchResult

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorSearchFormBg)
                .padding(horizontal = 12.dp),
        ) {
            // Search input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        postSearchText()
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.search)) },
                    singleLine = true,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = useRegex,
                        onCheckedChange = {
                            useRegex = it
                            postSearchText()
                        },
                    )
                    Text(
                        text = stringResource(R.string.toggle_regexp),
                        style = TextStyle(fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)),
                    )
                }
                IconButton(
                    onClick = { searchQuery = ""; postSearchText() },
                    enabled = hasKeyword,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.clear),
                    )
                }
            }

            // Error row
            val error = searchErrorText
            if (hasKeyword && !error.isNullOrBlank()) {
                Text(
                    text = error,
                    color = colorErrorText,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Result count and navigation
            if (hasKeyword) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = getString(
                            R.string.search_result,
                            if (currentMatchIndex >= 0) currentMatchIndex + 1 else 0,
                            result.size,
                            if (result.hasMore) "+" else "",
                        ),
                    )
                    IconButton(
                        onClick = { searchPrev() },
                        enabled = result.size > 1,
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.previous),
                        )
                    }
                    IconButton(
                        onClick = { searchNext() },
                        enabled = result.size > 1,
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.next),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TextContent(modifier: Modifier) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                // Read-only: only accept selection changes
                textFieldValue = TextFieldValue(
                    annotatedString = textFieldValue.annotatedString,
                    selection = newValue.selection,
                )
            },
            readOnly = true,
            modifier = modifier
                .fillMaxWidth()
                .padding(12.dp),
            textStyle = TextStyle(color = colorTextContent),
            cursorBrush = SolidColor(colorTextContent),
        )
    }

    // =====================================================
    // Search logic
    // =====================================================

    private fun postSearchText() {
        lifecycleScope.launch {
            try {
                searchTextChannel.send(SystemClock.elapsedRealtime())
            } catch (ex: Throwable) {
                log.e(ex, "postSearchText failed.")
            }
        }
    }

    private suspend fun searchTextImpl() {
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
                                if (size >= limit) {
                                    hasMore = true
                                    break
                                }
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
                                    ignoreCase = true
                                )
                                if (pos == -1) break
                                if (size >= limit) {
                                    hasMore = true
                                    break
                                }
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
        updateHighlightsAndSelection(newPos)
    }

    private fun searchNext() {
        val newPos = searchResult.findNext(textFieldValue.selection.start, allowEqual = false)
            ?: searchResult.items.firstOrNull()
        updateHighlightsAndSelection(newPos)
    }

    private fun searchPrev() {
        val newPos = searchResult.findPrev(textFieldValue.selection.start, allowEqual = false)
            ?: searchResult.items.lastOrNull()
        updateHighlightsAndSelection(newPos)
    }

    private fun updateHighlightsAndSelection(newPos: IntRange?) {
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
            val end = fullText.length
            androidx.compose.ui.text.TextRange(
                newPos.first.coerceIn(0, end),
                (newPos.last + 1).coerceIn(0, end),
            )
        } else {
            textFieldValue.selection
        }

        textFieldValue = TextFieldValue(
            annotatedString = styledText,
            selection = selection,
        )
    }

    // =====================================================
    // Action handlers
    // =====================================================

    private fun copy() {
        selectionOrAll.copyToClipboard(this)
    }

    private fun send() {
        selectionOrAll.trim().notEmpty()?.let {
            try {
                val intent = Intent()
                intent.action = Intent.ACTION_SEND
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, it)
                startActivity(intent)
            } catch (ex: Throwable) {
                log.e(ex, "send failed.")
                showToast(ex, "send failed.")
            }
        }
    }

    private fun searchWeb() {
        selectionOrAll.trim().notEmpty()?.also {
            try {
                val intent = Intent(Intent.ACTION_WEB_SEARCH)
                intent.putExtra(SearchManager.QUERY, it)
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                }
            } catch (ex: Throwable) {
                log.e(ex, "search failed.")
                showToast(ex, "search failed.")
            }
        }
    }

    private fun searchToot(@Suppress("SameParameterValue") resultCode: Int) {
        selectionOrAll.trim().notEmpty()?.let {
            try {
                val data = Intent()
                data.putExtra(Intent.EXTRA_TEXT, it)
                setResult(resultCode, data)
                finish()
            } catch (ex: Throwable) {
                log.e(ex, "searchToot failed.")
                showToast(ex, "searchToot failed.")
            }
        }
    }

    private fun muteWord() {
        launchAndShowError {
            selectionOrAll.trim().notEmpty()?.let {
                daoMutedWord.save(it)
                App1.getAppState(this@ActText).onMuteUpdated()
                showToast(false, R.string.word_was_muted)
            }
        }
    }

    private fun keywordFilter() {
        selectionOrAll.trim().notEmpty()?.let { text ->
            val account = this.account
            if (account?.isPseudo == false && account.isMastodon) {
                ActKeywordFilter.open(this, account, initialPhrase = text)
            } else {
                launchMain {
                    pickAccount(
                        bAllowPseudo = false,
                        bAllowMisskey = false,
                        bAllowMastodon = true,
                        bAuto = false,
                    )?.let {
                        ActKeywordFilter.open(this@ActText, it, initialPhrase = text)
                    }
                }
            }
        }
    }

    private fun highlight() {
        selectionOrAll.trim().notEmpty()?.let {
            startActivity(ActHighlightWordEdit.createIntent(this, it))
        }
    }
}
