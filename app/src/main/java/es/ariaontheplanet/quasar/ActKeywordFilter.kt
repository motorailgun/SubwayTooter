package es.ariaontheplanet.quasar

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
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.ariaontheplanet.quasar.actkeywordfilter.KeywordEntry
import es.ariaontheplanet.quasar.actkeywordfilter.KeywordFilterViewModel
import es.ariaontheplanet.quasar.api.ApiPath
import es.ariaontheplanet.quasar.api.TootApiResult
import es.ariaontheplanet.quasar.api.auth.AuthRepo
import es.ariaontheplanet.quasar.api.entity.EntityId
import es.ariaontheplanet.quasar.api.entity.TootFilter
import es.ariaontheplanet.quasar.api.entity.TootFilterKeyword
import es.ariaontheplanet.quasar.api.entity.TootInstance
import es.ariaontheplanet.quasar.api.entity.TootStatus
import es.ariaontheplanet.quasar.api.runApiTask
import es.ariaontheplanet.quasar.column.ColumnType
import es.ariaontheplanet.quasar.table.SavedAccount
import es.ariaontheplanet.quasar.table.daoAcctColor
import es.ariaontheplanet.quasar.table.daoSavedAccount
import es.ariaontheplanet.quasar.util.provideViewModel
import jp.juggler.util.backPressed
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.coroutine.launchMain
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

    val authRepo by lazy { AuthRepo(this) }

    private val viewModel by lazy {
        provideViewModel(this) { KeywordFilterViewModel() }
    }

    ///////////////////////////////////////////////////

    override fun onCreate(savedInstanceState: Bundle?) {
        backPressed { viewModel.setShowBackDialog(true) }
        super.onCreate(savedInstanceState)
        App1.setActivityTheme(this)

        viewModel.filterId = EntityId.entityId(intent, EXTRA_FILTER_ID)

        setContent {
            FilterContent(modifier = Modifier)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            if (state.showBackDialog) {
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
            viewModel.account = a
            viewModel.setAccountText(daoAcctColor.getNicknameWithColor(a.acct).toString())

            if (viewModel.filterId != null) {
                startLoading()
            } else {
                viewModel.setExpireSelection(1)
                val initialText = intent.string(EXTRA_INITIAL_PHRASE)?.trim() ?: ""
                viewModel.setTitleText(initialText)
                viewModel.addKeyword(TootFilterKeyword(keyword = initialText))
            }
        }
    }

    @Composable
    private fun ConfirmBackDialog() {
        AlertDialog(
            onDismissRequest = { viewModel.setShowBackDialog(false) },
            text = { Text(stringResource(R.string.keyword_filter_quit_waring)) },
            confirmButton = {
                TextButton(onClick = { finish() }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowBackDialog(false) }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun FilterContent(modifier: Modifier) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
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
                Text(state.accountText)

                // Title
                HorizontalDivider()
                Text(
                    stringResource(R.string.filter_title),
                    style = MaterialTheme.typography.labelLarge,
                )
                OutlinedTextField(
                    value = state.titleText,
                    onValueChange = { viewModel.setTitleText(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Keywords
                HorizontalDivider()
                Text(
                    stringResource(R.string.filter_phrase),
                    style = MaterialTheme.typography.labelLarge,
                )
                state.keywords.forEach { ks ->
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
                        selected = !state.actionHide,
                        onClick = { viewModel.setActionHide(false) },
                    )
                    Text(
                        stringResource(R.string.filter_action_warn),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = state.actionHide,
                        onClick = { viewModel.setActionHide(true) },
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
                ContextCheckbox(state.contextHome, R.string.filter_home, viewModel::setContextHome)
                ContextCheckbox(state.contextNotification, R.string.filter_notification, viewModel::setContextNotification)
                ContextCheckbox(state.contextPublic, R.string.filter_public, viewModel::setContextPublic)
                ContextCheckbox(state.contextThread, R.string.filter_thread, viewModel::setContextThread)
                ContextCheckbox(state.contextProfile, R.string.filter_profile, viewModel::setContextProfile)

                // Expire
                HorizontalDivider()
                Text(
                    stringResource(R.string.filter_expires_at),
                    style = MaterialTheme.typography.labelLarge,
                )
                if (state.expireText.isNotEmpty()) {
                    Text(state.expireText)
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
                val selectedIndex = state.expireSelection
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
                                    viewModel.setExpireSelection(index)
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
    private fun KeywordRow(ks: KeywordEntry) {
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
                value = ks.keyword,
                onValueChange = { viewModel.updateKeyword(ks.stateId, keyword = it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Checkbox(
                    checked = ks.wholeWord,
                    onCheckedChange = { viewModel.updateKeyword(ks.stateId, wholeWord = it) },
                )
                Text(
                    stringResource(R.string.filter_word_match_long),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                )
                IconButton(onClick = { viewModel.deleteKeyword(ks) }) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.delete),
                    )
                }
            }
        }
    }

    @Composable
    private fun ContextCheckbox(checked: Boolean, labelRes: Int, onChange: (Boolean) -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = checked,
                onCheckedChange = onChange,
            )
            Text(stringResource(labelRes))
        }
    }

    private fun onAddKeyword() {
        val a = viewModel.account ?: return
        val ti = TootInstance.getCached(a)
        val size = viewModel.uiState.value.keywords.size
        when {
            ti == null ->
                showToast(true, "can't get server information")
            !ti.versionGE(TootInstance.VERSION_4_0_0) && size >= 1 ->
                showToast(true, "before mastodon 4.0, allowed 1 keyword per 1 filter.")
            else -> viewModel.addKeyword(TootFilterKeyword(keyword = ""))
        }
    }

    private fun startLoading() {
        viewModel.setLoading(true)
        val account = viewModel.account ?: return
        val filterId = viewModel.filterId ?: return
        launchMain {
            var resultFilter: TootFilter? = null
            runApiTask(account) { client ->

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
                when (val filter = resultFilter) {
                    null -> {
                        viewModel.setLoading(false)
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
        viewModel.applyLoaded(filter)
        val expireTextValue = if (filter.time_expires_at == 0L) {
            getString(R.string.filter_expire_unlimited)
        } else {
            TootStatus.formatTime(this, filter.time_expires_at, false)
        }
        viewModel.setExpireText(expireTextValue)
    }

    private fun save() {
        val state = viewModel.uiState.value
        if (state.loading) return

        when (val validation = viewModel.validateForSave()) {
            KeywordFilterViewModel.SaveValidation.KeywordEmpty -> {
                showToast(true, R.string.filter_keyword_empty)
                return
            }
            KeywordFilterViewModel.SaveValidation.TitleEmpty -> {
                showToast(true, R.string.filter_title_empty)
                return
            }
            is KeywordFilterViewModel.SaveValidation.Ok -> {
                launchMain {
                    var result = saveV2(validation.title)
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
                            if (column.type == ColumnType.KEYWORD_FILTER && column.accessInfo == viewModel.account) {
                                column.filterReloadRequired = true
                            }
                        }
                        finish()
                    }
                }
            }
        }
    }

    private suspend fun saveV1(): TootApiResult? {
        val account = viewModel.account ?: return null
        if (viewModel.uiState.value.keywords.size != 1) {
            return TootApiResult("V1 API allow only 1 keyword.")
        }
        val params = viewModel.buildV1Params(expireDurationList)
        return runApiTask(account) { client ->
            if (viewModel.filterId == null) {
                client.request(
                    ApiPath.PATH_FILTERS_V1,
                    params.toPostRequestBuilder()
                )
            } else {
                client.request(
                    "${ApiPath.PATH_FILTERS_V1}/${viewModel.filterId}",
                    params.toRequestBody().toPut()
                )
            }
        }
    }

    private suspend fun saveV2(title: String): TootApiResult? {
        val account = viewModel.account ?: return null
        val params = viewModel.buildV2Params(expireDurationList, title)
        return runApiTask(account) { client ->
            if (viewModel.filterId == null) {
                client.request(
                    ApiPath.PATH_FILTERS_V2,
                    params.toPostRequestBuilder()
                )
            } else {
                client.request(
                    "${ApiPath.PATH_FILTERS_V2}/${viewModel.filterId}",
                    params.toRequestBody().toPut()
                )
            }
        }
    }
}
