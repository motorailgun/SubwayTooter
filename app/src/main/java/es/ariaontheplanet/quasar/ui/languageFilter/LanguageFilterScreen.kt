package es.ariaontheplanet.quasar.ui.languageFilter

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.RootActivity
import es.ariaontheplanet.quasar.dialog.DlgConfirm.confirm
import es.ariaontheplanet.quasar.dialog.actionsDialog
import es.ariaontheplanet.quasar.nav.Route
import es.ariaontheplanet.quasar.pref.FILE_PROVIDER_AUTHORITY
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.data.checkMimeTypeAndGrant
import jp.juggler.util.data.intentOpenDocument
import jp.juggler.util.int
import jp.juggler.util.log.showError
import jp.juggler.util.ui.ActivityResultHandler
import jp.juggler.util.ui.isOk
import jp.juggler.util.ui.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Caller helpers preserved from the old LanguageFilterActivity.Companion.
// Callers launch via the ActivityResultHandler and decode the column index
// from the returned Intent exactly as before.
fun openLanguageFilterActivity(
    launcher: ActivityResultHandler,
    columnIndex: Int,
) {
    val context = launcher.context
        ?: error("openLanguageFilterActivity: launcher is not registered.")
    RootActivity.createIntent(context, Route.LanguageFilter(columnIndex)).launch(launcher)
}

fun decodeLanguageFilterResult(r: ActivityResult): Int? = when {
    r.isOk -> r.data?.int(LanguageFilterViewModel.EXTRA_COLUMN_INDEX)
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageFilterScreen(columnIndex: Int) {
    val activity = LocalActivity.current as? ComponentActivity
    val viewModel: LanguageFilterViewModel = viewModel()
    val scope = rememberCoroutineScope()

    // Initialize once per (vm, columnIndex) — VM survives recomposition.
    LaunchedEffect(viewModel, columnIndex) {
        viewModel.restoreOrInitialize(columnIndex, savedInstanceState = null)
    }

    // Bubble errors through the activity's toast/error dialog.
    LaunchedEffect(viewModel, activity) {
        activity ?: return@LaunchedEffect
        viewModel.error.collect { ex ->
            ex ?: return@collect
            activity.showError(ex)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { r ->
        if (!r.isOk || activity == null) return@rememberLauncherForActivityResult
        r.data?.checkMimeTypeAndGrant(activity.contentResolver)
            ?.firstOrNull()?.uri?.let { viewModel.import2(it) }
    }

    BackHandler {
        scope.launch {
            try {
                if (viewModel.isLanguageListChanged()) {
                    activity?.confirm(R.string.language_filter_quit_waring)
                }
                activity?.finish()
            } catch (_: Throwable) {
                // confirm cancelled — stay on screen.
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(),
                    title = { Text(stringResource(R.string.language_filter)) },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                activity?.onBackPressedDispatcher?.onBackPressed()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.close),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            scope.launch {
                                try {
                                    edit(activity, viewModel, null)
                                } catch (ex: Throwable) {
                                    activity?.showError(ex)
                                }
                            }
                        }) {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = stringResource(R.string.add),
                            )
                        }
                        IconButton(onClick = {
                            activity ?: return@IconButton
                            activity.setResult(Activity.RESULT_OK, viewModel.save())
                            activity.finish()
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Save,
                                contentDescription = stringResource(R.string.close),
                            )
                        }
                        IconButton(onClick = {
                            activity ?: return@IconButton
                            scope.launch {
                                try {
                                    activity.actionsDialog {
                                        action(activity.getString(R.string.clear_all)) {
                                            viewModel.clearAllLanguage()
                                        }
                                        action(activity.getString(R.string.export)) {
                                            scope.launch {
                                                try {
                                                    exportLanguageFilter(activity, viewModel)
                                                } catch (ex: Throwable) {
                                                    activity.showError(ex)
                                                }
                                            }
                                        }
                                        action(activity.getString(R.string.import_)) {
                                            importLauncher.launch(intentOpenDocument("*/*"))
                                        }
                                    }
                                } catch (ex: Throwable) {
                                    activity.showError(ex)
                                }
                            }
                        }) {
                            Icon(
                                Icons.Outlined.MoreVert,
                                contentDescription = stringResource(R.string.more),
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            ScrollContent(
                innerPadding = innerPadding,
                viewModel = viewModel,
                onEdit = { item ->
                    scope.launch {
                        try {
                            edit(activity, viewModel, item)
                        } catch (ex: Throwable) {
                            activity?.showError(ex)
                        }
                    }
                },
            )
            val progressMessageState = viewModel.progressMessage.collectAsState()
            val progressMessage = progressMessageState.value?.let {
                stringResource(it.stringId, *it.args)
            }
            if (progressMessage != null) ProgressOverlay(progressMessage)
        }
    }
}

private suspend fun edit(
    activity: ComponentActivity?,
    viewModel: LanguageFilterViewModel,
    item: LanguageFilterItem?,
) {
    activity ?: return
    val result = activity.dialogLanguageFilterEdit(item, viewModel.languageNameMap)
    viewModel.handleEditResult(result)
}

private suspend fun exportLanguageFilter(
    activity: ComponentActivity,
    viewModel: LanguageFilterViewModel,
) {
    val file = withContext(Dispatchers.IO) { viewModel.createExportCacheFile() }
    val uri = FileProvider.getUriForFile(activity, FILE_PROVIDER_AUTHORITY, file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = activity.contentResolver.getType(uri)
        putExtra(Intent.EXTRA_SUBJECT, "SubwayTooter language filter data")
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }
    activity.startActivity(intent)
}

@Composable
private fun ProgressOverlay(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable { /* swallow taps to the screen behind */ }
            .background(
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.75f),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(160.dp))
        Spacer(modifier = Modifier.size(20.dp))
        Text(message)
    }
}

@Composable
private fun ScrollContent(
    innerPadding: PaddingValues,
    viewModel: LanguageFilterViewModel,
    onEdit: (LanguageFilterItem) -> Unit,
) {
    val state = viewModel.languageList.collectAsState()
    LazyColumn(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize(),
    ) {
        items(state.value) { item ->
            LanguageItemCard(item, viewModel, onEdit)
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp,
            )
        }
    }
}

@Composable
private fun LanguageItemCard(
    item: LanguageFilterItem,
    viewModel: LanguageFilterViewModel,
    onEdit: (LanguageFilterItem) -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(item) },
    ) {
        Text(
            modifier = Modifier
                .requiredHeightIn(min = 56.dp)
                .wrapContentHeight()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            text = "${item.code} ${
                context.langDesc(item.code, viewModel.languageNameMap)
            } : ${
                stringResource(
                    if (item.allow) R.string.language_show else R.string.language_hide,
                )
            }",
            color = if (item.allow) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.error,
        )
    }
}
