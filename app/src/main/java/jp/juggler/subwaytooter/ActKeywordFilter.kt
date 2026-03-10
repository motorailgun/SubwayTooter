package jp.juggler.subwaytooter

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jp.juggler.subwaytooter.api.ApiPath
import jp.juggler.subwaytooter.api.TootApiResult
import jp.juggler.subwaytooter.api.auth.AuthRepo
import jp.juggler.subwaytooter.api.entity.EntityId
import jp.juggler.subwaytooter.api.entity.TootFilter
import jp.juggler.subwaytooter.api.entity.TootFilterContext
import jp.juggler.subwaytooter.api.entity.TootFilterKeyword
import jp.juggler.subwaytooter.api.entity.TootInstance
import jp.juggler.subwaytooter.api.entity.TootStatus
import jp.juggler.subwaytooter.api.runApiTask
import jp.juggler.subwaytooter.column.ColumnType
import jp.juggler.subwaytooter.compose.StScreen
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.table.daoAcctColor
import jp.juggler.subwaytooter.table.daoSavedAccount
import jp.juggler.subwaytooter.util.getStColorTheme
import jp.juggler.util.backPressed
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.data.JsonArray
import jp.juggler.util.data.buildJsonArray
import jp.juggler.util.data.buildJsonObject
import jp.juggler.util.data.notEmpty
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.long
import jp.juggler.util.network.toPostRequestBuilder
import jp.juggler.util.network.toPut
import jp.juggler.util.network.toRequestBody
import jp.juggler.util.string

class ActKeywordFilter : ComponentActivity() {

    companion object {

        private val log = LogCategory("ActKeywordFilter")

        private const val EXTRA_ACCOUNT_DB_ID = "account_db_id"
        private const val EXTRA_FILTER_ID = "filter_id"
        private const val EXTRA_INITIAL_PHRASE = "initial_phrase"

        fun open(
            activity: Activity,
            ai: SavedAccount,
            filterId: EntityId? = null,
            initialPhrase: String? = null,
        ) {
            val intent = Intent(activity, ActKeywordFilter::class.java)
            intent.putExtra(EXTRA_ACCOUNT_DB_ID, ai.db_id)
            filterId?.putTo(intent, EXTRA_FILTER_ID)
            initialPhrase?.notEmpty()?.let { intent.putExtra(EXTRA_INITIAL_PHRASE, it) }
            activity.startActivity(intent)
        }

        private val expireDurationList = intArrayOf(
            -1, // don't change
            0, // unlimited
            1800,
            3600,
            3600 * 6,
            3600 * 12,
            86400,
            86400 * 7
        )
    }

    private var account: SavedAccount? = null
    private var filterId: EntityId? = null
    private var filterExpire: Long = 0L
    private var loading = false
    private val deleteIds = mutableSetOf<String>()

    val authRepo by lazy { AuthRepo(this) }

    // Compose state
    private val accountText = mutableStateOf("")
    private val titleText = mutableStateOf("")
    private val keywords = mutableStateListOf<KeywordState>()
    private val actionHide = mutableStateOf(false)
    private val contextHome = mutableStateOf(true)
    private val contextNotification = mutableStateOf(true)
    private val contextPublic = mutableStateOf(true)
    private val contextThread = mutableStateOf(true)
    private val contextProfile = mutableStateOf(true)
    private val expireSelection = mutableIntStateOf(0)
    private val expireText = mutableStateOf("")
    private val showBackDialog = mutableStateOf(false)

    private var nextKeywordStateId = 0L

    private class KeywordState(
        val stateId: Long,
        val serverKeywordId: String?,
        keyword: String,
        wholeWord: Boolean,
    ) {
        val keyword = mutableStateOf(keyword)
        val wholeWord = mutableStateOf(wholeWord)
    }

    ///////////////////////////////////////////////////

    override fun onCreate(savedInstanceState: Bundle?) {
        backPressed { showBackDialog.value = true }
        super.onCreate(savedInstanceState)
        App1.setActivityTheme(this)
        val colorScheme = getStColorTheme()

        filterId = EntityId.entityId(intent, EXTRA_FILTER_ID)

        setContent {
            StScreen(
                colorScheme = colorScheme,
                title = stringResource(
                    if (filterId == null) R.string.keyword_filter_new
                    else R.string.keyword_filter_edit
                ),
                onBack = { showBackDialog.value = true },
            ) { innerPadding ->
                FilterContent(modifier = Modifier.padding(innerPadding))
            }
            if (showBackDialog.value) {
                ConfirmBackDialog()
            }
        }

        launchAndShowError {
            val a = intent.long(EXTRA_ACCOUNT_DB_ID)
                ?.let { daoSavedAccount.loadAccount(it) }
            if (a == null) {
                finish()
                return@launchAndShowError
            }
            account = a
            accountText.value =
                daoAcctColor.getNicknameWithColor(a.acct).toString()

            if (filterId != null) {
                startLoading()
            } else {
                expireSelection.intValue = 1
                val initialText = intent.string(EXTRA_INITIAL_PHRASE)?.trim() ?: ""
                titleText.value = initialText
                addKeyword(TootFilterKeyword(keyword = initialText))
            }
        }
    }

    @Composable
    private fun ConfirmBackDialog() {
        AlertDialog(
            onDismissRequest = { showBackDialog.value = false },
            text = { Text(stringResource(R.string.keyword_filter_quit_waring)) },
            confirmButton = {
                TextButton(onClick = { finish() }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackDialog.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun FilterContent(modifier: Modifier) {
        Column(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Account
                HorizontalDivider()
                Text(
                    stringResource(R.string.account),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(accountText.value)

                // Title
                HorizontalDivider()
                Text(
                    stringResource(R.string.filter_title),
                    style = MaterialTheme.typography.labelLarge,
                )
                OutlinedTextField(
                    value = titleText.value,
                    onValueChange = { titleText.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Keywords
                HorizontalDivider()
                Text(
                    stringResource(R.string.filter_phrase),
                    style = MaterialTheme.typography.labelLarge,
                )
                keywords.forEach { ks ->
                    KeywordRow(ks)
                }
                TextButton(onClick = { onAddKeyword() }) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.add_keyword_or_phrase))
                }

                // Filter action
                HorizontalDivider()
                Text(
                    stringResource(R.string.filter_action),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !actionHide.value,
                        onClick = { actionHide.value = false },
                    )
                    Text(
                        stringResource(R.string.filter_action_warn),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = actionHide.value,
                        onClick = { actionHide.value = true },
                    )
                    Text(
                        stringResource(R.string.filter_action_hide),
                        modifier = Modifier.weight(1f),
                    )
                }

                // Filter context
                HorizontalDivider()
                Text(
                    stringResource(R.string.filter_context),
                    style = MaterialTheme.typography.labelLarge,
                )
                ContextCheckbox(contextHome, R.string.filter_home)
                ContextCheckbox(contextNotification, R.string.filter_notification)
                ContextCheckbox(contextPublic, R.string.filter_public)
                ContextCheckbox(contextThread, R.string.filter_thread)
                ContextCheckbox(contextProfile, R.string.filter_profile)

                // Expire
                HorizontalDivider()
                Text(
                    stringResource(R.string.filter_expires_at),
                    style = MaterialTheme.typography.labelLarge,
                )
                val currentExpireText = expireText.value
                if (currentExpireText.isNotEmpty()) {
                    Text(currentExpireText)
                }

                // Expire dropdown
                val expireOptions = listOf(
                    stringResource(R.string.dont_change),
                    stringResource(R.string.filter_expire_unlimited),
                    stringResource(R.string.filter_expire_30min),
                    stringResource(R.string.filter_expire_1hour),
                    stringResource(R.string.filter_expire_6hour),
                    stringResource(R.string.filter_expire_12hour),
                    stringResource(R.string.filter_expire_1day),
                    stringResource(R.string.filter_expire_1week),
                )
                var expanded by remember { mutableStateOf(false) }
                val selectedIndex = expireSelection.intValue
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = expireOptions.getOrElse(selectedIndex) { "" },
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        expireOptions.forEachIndexed { index, label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    expireSelection.intValue = index
                                    expanded = false
                                },
                            )
                        }
                    }
                }

                HorizontalDivider()
                Spacer(Modifier.height(128.dp))
            }

            // Save button
            Button(
                onClick = { save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }

    @Composable
    private fun KeywordRow(ks: KeywordState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        ) {
            Text(
                stringResource(R.string.keyword_or_phrase),
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = ks.keyword.value,
                onValueChange = { ks.keyword.value = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Checkbox(
                    checked = ks.wholeWord.value,
                    onCheckedChange = { ks.wholeWord.value = it },
                )
                Text(
                    stringResource(R.string.filter_word_match_long),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                )
                IconButton(onClick = { deleteKeyword(ks) }) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.delete),
                    )
                }
            }
        }
    }

    @Composable
    private fun ContextCheckbox(state: MutableState<Boolean>, labelRes: Int) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.value,
                onCheckedChange = { state.value = it },
            )
            Text(stringResource(labelRes))
        }
    }

    private fun addKeyword(fk: TootFilterKeyword) {
        keywords.add(
            KeywordState(
                stateId = nextKeywordStateId++,
                serverKeywordId = fk.id?.toString()?.notEmpty(),
                keyword = fk.keyword.trim(),
                wholeWord = fk.whole_word,
            )
        )
    }

    private fun onAddKeyword() {
        val a = account ?: return
        val ti = TootInstance.getCached(a)
        when {
            ti == null ->
                showToast(true, "can't get server information")
            !ti.versionGE(TootInstance.VERSION_4_0_0) && keywords.size >= 1 ->
                showToast(true, "before mastodon 4.0, allowed 1 keyword per 1 filter.")
            else -> addKeyword(TootFilterKeyword(keyword = ""))
        }
    }

    private fun deleteKeyword(ks: KeywordState) {
        keywords.remove(ks)
        ks.serverKeywordId?.let { deleteIds.add(it) }
    }

    private fun startLoading() {
        loading = true
        launchMain {
            var resultFilter: TootFilter? = null
            runApiTask(account!!) { client ->

                // try v2
                var result = client.request("${ApiPath.PATH_FILTERS_V2}/$filterId")
                result?.jsonObject?.let {
                    try {
                        resultFilter = TootFilter(it)
                        return@runApiTask result
                    } catch (ex: Throwable) {
                        log.e(ex, "parse error.")
                    }
                }

                if (result?.response?.code == 404) {
                    // try v1
                    result = client.request("${ApiPath.PATH_FILTERS_V1}/$filterId")
                    result?.jsonObject?.let {
                        try {
                            resultFilter = TootFilter(it)
                            return@runApiTask result
                        } catch (ex: Throwable) {
                            log.e(ex, "parse error.")
                        }
                    }
                }

                result
            }?.let { result ->
                loading = false
                when (val filter = resultFilter) {
                    null -> {
                        showToast(true, result.error ?: "?")
                        finish()
                    }
                    else -> onLoadComplete(filter)
                }
            }
            // キャンセル時はloadingはtrueのまま
        }
    }

    private fun onLoadComplete(filter: TootFilter) {
        loading = false
        filterExpire = filter.time_expires_at

        contextHome.value = filter.hasContext(TootFilterContext.Home)
        contextNotification.value = filter.hasContext(TootFilterContext.Notifications)
        contextPublic.value = filter.hasContext(TootFilterContext.Public)
        contextThread.value = filter.hasContext(TootFilterContext.Thread)
        contextProfile.value = filter.hasContext(TootFilterContext.Account)

        actionHide.value = filter.hide

        val kws = filter.keywords.ifEmpty {
            listOf(TootFilterKeyword(keyword = ""))
        }
        kws.forEach { addKeyword(it) }

        titleText.value =
            filter.title.notEmpty() ?: filter.keywords.firstOrNull()?.keyword ?: ""

        expireText.value = if (filter.time_expires_at == 0L) {
            getString(R.string.filter_expire_unlimited)
        } else {
            TootStatus.formatTime(this, filter.time_expires_at, false)
        }
    }

    private fun save() {
        if (loading) return

        if (keywords.isEmpty() || keywords.any { it.keyword.value.trim().isEmpty() }) {
            showToast(true, R.string.filter_keyword_empty)
            return
        }

        val title = titleText.value.trim()
        if (title.isEmpty()) {
            showToast(true, R.string.filter_title_empty)
            return
        }

        launchMain {
            var result = saveV2(title)
            if (result?.response?.code == 404) {
                result = saveV1()
            }
            result ?: return@launchMain // cancelled

            val error = result.error
            if (error != null) {
                showToast(true, result.error)
            } else {
                val appState = App1.prepare(applicationContext, "ActKeywordFilter.save()")
                for (column in appState.columnList) {
                    if (column.type == ColumnType.KEYWORD_FILTER && column.accessInfo == account) {
                        column.filterReloadRequired = true
                    }
                }
                finish()
            }
        }
    }

    private fun filterParamBase() = buildJsonObject {
        fun JsonArray.putContextChecked(checked: Boolean, fc: TootFilterContext) {
            if (checked) add(fc.apiName)
        }

        put("context", JsonArray().apply {
            putContextChecked(contextHome.value, TootFilterContext.Home)
            putContextChecked(contextNotification.value, TootFilterContext.Notifications)
            putContextChecked(contextPublic.value, TootFilterContext.Public)
            putContextChecked(contextThread.value, TootFilterContext.Thread)
            putContextChecked(contextProfile.value, TootFilterContext.Account)
        })

        when (val seconds = expireDurationList
            .elementAtOrNull(expireSelection.intValue) ?: -1
        ) {
            // don't change
            -1 -> Unit

            // unlimited
            0 -> when {
                // already unlimited. don't change.
                filterExpire <= 0L -> Unit
                // XXX: currently there is no way to remove expires from existing filter.
                else -> put("expires_in", Int.MAX_VALUE)
            }

            // set seconds
            else -> put("expires_in", seconds)
        }
    }

    private suspend fun saveV1(): TootApiResult? {
        if (keywords.size != 1) return TootApiResult("V1 API allow only 1 keyword.")

        val params = filterParamBase().apply {
            put("irreversible", actionHide.value)
            val ks = keywords.first()
            put("phrase", ks.keyword.value.trim())
            put("whole_word", ks.wholeWord.value)
        }

        return runApiTask(account!!) { client ->
            if (filterId == null) {
                client.request(
                    ApiPath.PATH_FILTERS_V1,
                    params.toPostRequestBuilder()
                )
            } else {
                client.request(
                    "${ApiPath.PATH_FILTERS_V1}/$filterId",
                    params.toRequestBody().toPut()
                )
            }
        }
    }

    private suspend fun saveV2(title: String): TootApiResult? {
        val params = filterParamBase().apply {
            put("title", title)
            put(
                "filter_action",
                if (actionHide.value) "hide" else "warn"
            )
            put("keywords_attributes", buildJsonArray {
                keywords.forEach { ks ->
                    add(buildJsonObject {
                        put("keyword", ks.keyword.value.trim())
                        put("whole_word", ks.wholeWord.value)
                        ks.serverKeywordId?.let { put("id", it) }
                    })
                }
                deleteIds.forEach { id ->
                    add(buildJsonObject {
                        put("id", id)
                        put("_destroy", id)
                    })
                }
            })
        }
        return runApiTask(account!!) { client ->
            if (filterId == null) {
                client.request(
                    ApiPath.PATH_FILTERS_V2,
                    params.toPostRequestBuilder()
                )
            } else {
                client.request(
                    "${ApiPath.PATH_FILTERS_V2}/$filterId",
                    params.toRequestBody().toPut()
                )
            }
        }
    }
}
