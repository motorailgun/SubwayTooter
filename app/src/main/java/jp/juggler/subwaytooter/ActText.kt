package jp.juggler.subwaytooter

import android.app.SearchManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.get
import androidx.core.view.size
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import jp.juggler.subwaytooter.api.entity.TootAccount
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.dialog.pickAccount
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.table.daoMutedWord
import jp.juggler.subwaytooter.table.daoSavedAccount
import jp.juggler.subwaytooter.util.CustomShare
import jp.juggler.subwaytooter.util.CustomShareTarget
import jp.juggler.subwaytooter.util.TootTextEncoder
import jp.juggler.subwaytooter.util.copyToClipboard
import jp.juggler.subwaytooter.view.MyEditText
import jp.juggler.subwaytooter.view.wrapTitleTextView
import jp.juggler.util.*
import jp.juggler.util.coroutine.AppDispatchers
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.data.*
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.ui.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActText : AppCompatActivity() {

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

    private class SearchResultSpan(color: Int) : BackgroundColorSpan(color)

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

    private lateinit var toolbar: Toolbar
    private lateinit var etText: MyEditText
    private lateinit var etSearch: MyEditText
    private lateinit var btnSearchClear: ImageButton
    private lateinit var btnSearchPrev: ImageButton
    private lateinit var btnSearchNext: ImageButton
    private lateinit var btnToggleRegex: ToggleButton
    private lateinit var llSearchResult: LinearLayout
    private lateinit var tvSearchCount: TextView
    private lateinit var tvSearchError: TextView
    private lateinit var rootView: LinearLayout

    private val searchTextChannel = Channel<Long>(capacity = Channel.CONFLATED)

    private var account: SavedAccount? = null

    private var searchResult = SearchResult()

    private fun createViews() {
        val pad12 = dp(12)
        val size48 = dp(48)

        toolbar = Toolbar(this).apply {
            val tv = android.util.TypedValue()
            theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(tv.resourceId)
            )
            setBackgroundResource(R.drawable.action_bar_bg)
            elevation = dpFloat(4)
        }

        etSearch = MyEditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            setHint(R.string.search)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }

        btnToggleRegex = ToggleButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            minimumWidth = size48
            minimumHeight = size48
            elevation = 0f
            stateListAnimator = null
            textOn = getString(R.string.toggle_regexp)
            textOff = getString(R.string.toggle_regexp)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 14f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        btnSearchClear = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(size48, size48)
            contentDescription = getString(R.string.clear)
            setImageDrawable(
                resDrawable(R.drawable.ic_close).wrapAndTint(attrColor(R.attr.colorTextContent))
            )
        }

        val searchRow = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            isBaselineAligned = false
            addView(etSearch)
            addView(btnToggleRegex)
            addView(btnSearchClear)
        }

        tvSearchError = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setTextColor(attrColor(R.attr.colorRegexFilterError))
        }

        tvSearchCount = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            gravity = android.view.Gravity.END
            includeFontPadding = false
        }

        btnSearchPrev = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(size48, size48)
            setBackgroundResource(R.drawable.btn_bg_transparent_round6dp)
            contentDescription = getString(R.string.previous)
            setImageDrawable(
                resDrawable(R.drawable.ic_arrow_drop_up).wrapAndTint(attrColor(R.attr.colorTextContent))
            )
        }

        btnSearchNext = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(size48, size48)
            setBackgroundResource(R.drawable.btn_bg_transparent_round6dp)
            contentDescription = getString(R.string.next)
            setImageDrawable(
                resDrawable(R.drawable.ic_arrow_drop_down).wrapAndTint(attrColor(R.attr.colorTextContent))
            )
        }

        llSearchResult = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            isBaselineAligned = false
            addView(tvSearchCount)
            addView(btnSearchPrev)
            addView(btnSearchNext)
        }

        val searchArea = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(attrColor(R.attr.colorSearchFormBackground))
            setPadding(pad12, 0, pad12, 0)
            addView(searchRow)
            addView(tvSearchError)
            addView(llSearchResult)
        }

        etText = MyEditText(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            inputType = android.text.InputType.TYPE_NULL
            setPadding(pad12, pad12, pad12, pad12)
            setTextColor(attrColor(R.attr.colorTextContent))
        }

        val contentFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setBackgroundColor(attrColor(R.attr.colorMainBackground))
            addView(etText)
        }

        rootView = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            addView(toolbar)
            addView(searchArea)
            addView(contentFrame)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App1.setActivityTheme(this)
        createViews()
        setContentViewAndInsets(rootView)
        setSupportActionBar(toolbar)
        wrapTitleTextView()
        setNavigationBack(toolbar)
        fixHorizontalMargin(etText)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        etSearch.addTextChangedListener { postSearchText() }
        btnSearchClear.setOnClickListener { etSearch.setText("") }
        btnSearchPrev.setOnClickListener { searchPrev() }
        btnSearchNext.setOnClickListener { searchNext() }
        btnToggleRegex.setOnCheckedChangeListener { _, _ -> postSearchText() }

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

        launchAndShowError {
            account = intent.long(EXTRA_ACCOUNT_DB_ID)
                ?.let { daoSavedAccount.loadAccount(it) }

            if (savedInstanceState == null) {
                showSearchResult(null)

                val sv = intent.string(EXTRA_TEXT) ?: ""
                val contentStart = intent.int(EXTRA_CONTENT_START) ?: 0
                val contentEnd = intent.int(EXTRA_CONTENT_END) ?: sv.length
                etText.setText(sv)

                // Android 9 以降ではフォーカスがないとsetSelectionできない
                if (Build.VERSION.SDK_INT >= 28) {
                    etText.requestFocus()
                    etText.hideKeyboard()
                }

                etText.setSelection(contentStart, contentEnd)
            }
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        postSearchText()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.act_text, menu)
        for (i in 0 until menu.size) {
            val menuItem = menu[i]
            menuItem.icon = menuItem.icon?.wrapAndTint(
                color = attrColor(R.attr.colorTextContent),
            )
        }
        super.onCreateOptionsMenu(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miCopy -> selectionOrAll.copyToClipboard(this)
            R.id.miSearch -> search()
            R.id.miSend -> send()
            R.id.miMuteWord -> muteWord()
            R.id.miSearchNotestock -> searchToot(RESULT_SEARCH_NOTESTOCK)
            R.id.miKeywordFilter -> keywordFilter()
            R.id.miHighlight -> highlight()
            // MSP検索ボタン -> searchToot(RESULT_SEARCH_MSP)
            // R.id.btnSearchTS -> searchToot(RESULT_SEARCH_TS)

            R.id.miTranslate -> CustomShare.invokeText(
                CustomShareTarget.Translate,
                this,
                selectionOrAll,
            )

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    /**
     * 選択範囲、またはテキスト全体
     */
    private val selectionOrAll: String
        get() {
            val et = etText
            val s = et.selectionStart
            val e = et.selectionEnd
            val text = et.text.toString()
            return when (s) {
                e -> text
                else -> text.substring(s, e)
            }
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

    private fun search() {
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
        val keyword = etSearch.text?.toString() ?: ""
        val content = etText.text?.toString() ?: ""
        val useRegex = btnToggleRegex.isChecked
        this.searchResult = withContext(AppDispatchers.IO) {
            try {
                val limit = 1000
                var hasMore = false
                val items = buildList {
                    when {
                        // 空欄
                        keyword.isEmpty() -> Unit

                        // 正規表現
                        useRegex -> {
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
                        // 生テキスト
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
        showSearchResult {
            searchResult.findNext(etText.selectionStart, allowEqual = true)
                ?: searchResult.items.firstOrNull()
        }
    }

    private fun searchNext() {
        showSearchResult {
            searchResult.findNext(etText.selectionStart, allowEqual = false)
                ?: searchResult.items.firstOrNull()
        }
    }

    private fun searchPrev() {
        showSearchResult {
            searchResult.findPrev(etText.selectionStart, allowEqual = false)
                ?: searchResult.items.lastOrNull()
        }
    }

    private fun showSearchResult(newPosFinder: (() -> IntRange?)?) {
        val newPos: IntRange? = try {
            newPosFinder?.invoke()
        } catch (ex: Throwable) {
            log.e(ex, "newPosFinder failed.")
            null
        }
        val hasKeyword = !etSearch.text.isNullOrEmpty()

        btnSearchClear.isEnabledAlpha = hasKeyword

        llSearchResult.vg(hasKeyword)?.let {
            btnSearchPrev.isEnabledAlpha = searchResult.size > 1
            btnSearchNext.isEnabledAlpha = searchResult.size > 1

            val idx = newPos?.let {
                val end = etText.text?.length ?: 0
                etText.setSelection(
                    it.first.clip(0, end),
                    (it.last + 1).clip(0, end),
                )
                searchResult.index(it.first)
            }

            tvSearchCount.text = getString(
                R.string.search_result,
                idx?.plus(1) ?: 0,
                searchResult.size,
                if (searchResult.hasMore) "+" else ""
            )
        }

        val error = searchResult.error
        tvSearchError.vg(hasKeyword && !error.isNullOrBlank())
            ?.text = error

        etText.text?.let { e ->
            for (span in e.getSpans(0, e.length, SearchResultSpan::class.java)) {
                try {
                    e.removeSpan(span)
                } catch (ignored: Throwable) {
                }
            }
            for (pos in searchResult.items) {
                val attrId = when (newPos) {
                    pos -> R.attr.colorSearchFormBackground
                    else -> R.attr.colorButtonBgCw
                }
                e.setSpan(
                    SearchResultSpan(attrColor(attrId)),
                    pos.first,
                    pos.last + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
    }
}
