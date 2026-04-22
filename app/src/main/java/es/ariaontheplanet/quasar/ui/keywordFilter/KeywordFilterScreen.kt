package es.ariaontheplanet.quasar.ui.keywordFilter

import android.app.Activity
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.ariaontheplanet.quasar.App1
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.RootActivity
import es.ariaontheplanet.quasar.actkeywordfilter.KeywordEntry
import es.ariaontheplanet.quasar.actkeywordfilter.KeywordFilterViewModel
import es.ariaontheplanet.quasar.api.ApiPath
import es.ariaontheplanet.quasar.api.TootApiResult
import es.ariaontheplanet.quasar.api.entity.EntityId
import es.ariaontheplanet.quasar.api.entity.TootFilter
import es.ariaontheplanet.quasar.api.entity.TootFilterKeyword
import es.ariaontheplanet.quasar.api.entity.TootInstance
import es.ariaontheplanet.quasar.api.entity.TootStatus
import es.ariaontheplanet.quasar.api.runApiTask
import es.ariaontheplanet.quasar.column.ColumnType
import es.ariaontheplanet.quasar.nav.Route
import es.ariaontheplanet.quasar.table.SavedAccount
import es.ariaontheplanet.quasar.table.daoAcctColor
import es.ariaontheplanet.quasar.table.daoSavedAccount
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.data.notEmpty
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.network.toPostRequestBuilder
import jp.juggler.util.network.toPut
import jp.juggler.util.network.toRequestBody

private val log = LogCategory("KeywordFilterScreen")

private val expireDurationList = intArrayOf(
    -1, // don't change
    0, // unlimited
    1800,
    3600,
    3600 * 6,
    3600 * 12,
    86400,
    86400 * 7,
)

/** Launches the keyword-filter editor for the given account. */
fun openKeywordFilter(
    activity: Activity,
    ai: SavedAccount,
    filterId: EntityId? = null,
    initialPhrase: String? = null,
) {
    val intent = RootActivity.createIntent(
        activity,
        Route.KeywordFilter(
            accountDbId = ai.db_id,
            filterId = filterId?.toString(),
            initialPhrase = initialPhrase?.notEmpty(),
        ),
    )
    activity.startActivity(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeywordFilterScreen(
    accountDbId: Long,
    filterId: String?,
    initialPhrase: String?,
) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    val viewModel: KeywordFilterViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    BackHandler { viewModel.setShowBackDialog(true) }

    LaunchedEffect(viewModel, accountDbId, filterId, initialPhrase) {
        if (viewModel.account != null) return@LaunchedEffect
        viewModel.filterId = filterId?.notEmpty()?.let { EntityId(it) }

        activity.launchAndShowError {
            val a = daoSavedAccount.loadAccount(accountDbId)
            if (a == null) {
                activity.finish()
                return@launchAndShowError
            }
            viewModel.account = a
            viewModel.setAccountText(daoAcctColor.getNicknameWithColor(a.acct).toString())

            if (viewModel.filterId != null) {
                startLoading(activity, viewModel)
            } else {
                viewModel.setExpireSelection(1)
                val initialText = initialPhrase?.trim() ?: ""
                viewModel.setTitleText(initialText)
                viewModel.addKeyword(TootFilterKeyword(keyword = initialText))
            }
        }
    }

    if (state.showBackDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowBackDialog(false) },
            text = { Text(stringResource(R.string.keyword_filter_quit_waring)) },
            confirmButton = {
                TextButton(onClick = { activity.finish() }) {
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

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            HorizontalDivider()
            Text(stringResource(R.string.account), style = MaterialTheme.typography.labelLarge)
            Text(state.accountText)

            HorizontalDivider()
            Text(stringResource(R.string.filter_title), style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = state.titleText,
                onValueChange = { viewModel.setTitleText(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            HorizontalDivider()
            Text(stringResource(R.string.filter_phrase), style = MaterialTheme.typography.labelLarge)
            state.keywords.forEach { ks -> KeywordRow(viewModel, ks) }
            TextButton(onClick = { onAddKeyword(activity, viewModel) }) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.add_keyword_or_phrase))
            }

            HorizontalDivider()
            Text(stringResource(R.string.filter_action), style = MaterialTheme.typography.labelLarge)
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

            HorizontalDivider()
            Text(stringResource(R.string.filter_context), style = MaterialTheme.typography.labelLarge)
            ContextCheckbox(state.contextHome, R.string.filter_home, viewModel::setContextHome)
            ContextCheckbox(state.contextNotification, R.string.filter_notification, viewModel::setContextNotification)
            ContextCheckbox(state.contextPublic, R.string.filter_public, viewModel::setContextPublic)
            ContextCheckbox(state.contextThread, R.string.filter_thread, viewModel::setContextThread)
            ContextCheckbox(state.contextProfile, R.string.filter_profile, viewModel::setContextProfile)

            HorizontalDivider()
            Text(stringResource(R.string.filter_expires_at), style = MaterialTheme.typography.labelLarge)
            if (state.expireText.isNotEmpty()) Text(state.expireText)

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
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = expireOptions.getOrElse(state.expireSelection) { "" },
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

        Button(
            onClick = { save(activity, viewModel) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(stringResource(R.string.save))
        }
    }
}

@Composable
private fun KeywordRow(viewModel: KeywordFilterViewModel, ks: KeywordEntry) {
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
private fun ContextCheckbox(
    checked: Boolean,
    labelRes: Int,
    onChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(stringResource(labelRes))
    }
}

private fun onAddKeyword(activity: ComponentActivity, viewModel: KeywordFilterViewModel) {
    val a = viewModel.account ?: return
    val ti = TootInstance.getCached(a)
    val size = viewModel.uiState.value.keywords.size
    when {
        ti == null ->
            activity.showToast(true, "can't get server information")
        !ti.versionGE(TootInstance.VERSION_4_0_0) && size >= 1 ->
            activity.showToast(true, "before mastodon 4.0, allowed 1 keyword per 1 filter.")
        else -> viewModel.addKeyword(TootFilterKeyword(keyword = ""))
    }
}

private fun startLoading(activity: ComponentActivity, viewModel: KeywordFilterViewModel) {
    viewModel.setLoading(true)
    val account = viewModel.account ?: return
    val filterId = viewModel.filterId ?: return
    launchMain {
        var resultFilter: TootFilter? = null
        activity.runApiTask(account) { client ->
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
                    activity.showToast(true, result.error ?: "?")
                    activity.finish()
                }
                else -> onLoadComplete(activity, viewModel, filter)
            }
        }
        // cancelled → loading remains true
    }
}

private fun onLoadComplete(
    activity: ComponentActivity,
    viewModel: KeywordFilterViewModel,
    filter: TootFilter,
) {
    viewModel.applyLoaded(filter)
    val expireTextValue = if (filter.time_expires_at == 0L) {
        activity.getString(R.string.filter_expire_unlimited)
    } else {
        TootStatus.formatTime(activity, filter.time_expires_at, false)
    }
    viewModel.setExpireText(expireTextValue)
}

private fun save(activity: ComponentActivity, viewModel: KeywordFilterViewModel) {
    val state = viewModel.uiState.value
    if (state.loading) return

    when (val validation = viewModel.validateForSave()) {
        KeywordFilterViewModel.SaveValidation.KeywordEmpty -> {
            activity.showToast(true, R.string.filter_keyword_empty)
            return
        }
        KeywordFilterViewModel.SaveValidation.TitleEmpty -> {
            activity.showToast(true, R.string.filter_title_empty)
            return
        }
        is KeywordFilterViewModel.SaveValidation.Ok -> {
            launchMain {
                var result = saveV2(activity, viewModel, validation.title)
                if (result?.response?.code == 404) {
                    result = saveV1(activity, viewModel)
                }
                result ?: return@launchMain // cancelled

                val error = result.error
                if (error != null) {
                    activity.showToast(true, result.error)
                } else {
                    val appState = App1.prepare(activity.applicationContext, "KeywordFilterScreen.save")
                    for (column in appState.columnList) {
                        if (column.type == ColumnType.KEYWORD_FILTER &&
                            column.accessInfo == viewModel.account
                        ) {
                            column.filterReloadRequired = true
                        }
                    }
                    activity.finish()
                }
            }
        }
    }
}

private suspend fun saveV1(
    activity: ComponentActivity,
    viewModel: KeywordFilterViewModel,
): TootApiResult? {
    val account = viewModel.account ?: return null
    if (viewModel.uiState.value.keywords.size != 1) {
        return TootApiResult("V1 API allow only 1 keyword.")
    }
    val params = viewModel.buildV1Params(expireDurationList)
    return activity.runApiTask(account) { client ->
        if (viewModel.filterId == null) {
            client.request(ApiPath.PATH_FILTERS_V1, params.toPostRequestBuilder())
        } else {
            client.request(
                "${ApiPath.PATH_FILTERS_V1}/${viewModel.filterId}",
                params.toRequestBody().toPut(),
            )
        }
    }
}

private suspend fun saveV2(
    activity: ComponentActivity,
    viewModel: KeywordFilterViewModel,
    title: String,
): TootApiResult? {
    val account = viewModel.account ?: return null
    val params = viewModel.buildV2Params(expireDurationList, title)
    return activity.runApiTask(account) { client ->
        if (viewModel.filterId == null) {
            client.request(ApiPath.PATH_FILTERS_V2, params.toPostRequestBuilder())
        } else {
            client.request(
                "${ApiPath.PATH_FILTERS_V2}/${viewModel.filterId}",
                params.toRequestBody().toPut(),
            )
        }
    }
}
