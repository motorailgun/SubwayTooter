package jp.juggler.subwaytooter.dialog

import android.app.Dialog
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.api.entity.Host
import jp.juggler.subwaytooter.api.entity.TootInstance
import jp.juggler.subwaytooter.api.getApiHostFromWebFinger
import jp.juggler.subwaytooter.api.runApiTask2
import jp.juggler.subwaytooter.compose.StThemedContent
import jp.juggler.subwaytooter.util.DecodeOptions
import jp.juggler.subwaytooter.util.LinkHelper
import jp.juggler.util.coroutine.AppDispatchers
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.coroutine.launchMain
import jp.juggler.util.data.notBlank
import jp.juggler.util.data.notEmpty
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.showToast
import jp.juggler.util.ui.ProgressDialogEx
import jp.juggler.util.ui.dismissSafe
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.IDN

class LoginForm(
    val activity: ComponentActivity,
    val onClickOk: (
        dialog: Dialog,
        apiHost: Host,
        serverInfo: TootInstance?,
        action: Action,
    ) -> Unit,
) {
    companion object {
        private val log = LogCategory("LoginForm")

        @Suppress("RegExpSimplifiable")
        val reBadChars = """([^\p{L}\p{N}A-Za-z0-9:;._-]+)""".toRegex()

        fun ComponentActivity.showLoginForm(
            onClickOk: (
                dialog: Dialog,
                apiHost: Host,
                serverInfo: TootInstance?,
                action: Action,
            ) -> Unit,
        ) = LoginForm(this, onClickOk)
    }

    enum class Action(
        @StringRes val idName: Int,
        @StringRes val idDesc: Int,
    ) {
        Login(R.string.existing_account, R.string.existing_account_desc),
        Pseudo(R.string.pseudo_account, R.string.pseudo_account_desc),
        Token(R.string.input_access_token, R.string.input_access_token_desc),
    }

    val dialog = Dialog(activity)

    // Compose UI state
    private val currentPage = mutableIntStateOf(0)
    private val serverInput = mutableStateOf("")
    private val errorText = mutableStateOf("")
    private val inputValid = mutableStateOf(false)
    private val serverHostText = mutableStateOf("")
    private val serverDescText = mutableStateOf("")
    private val serverDescIsError = mutableStateOf(false)
    private val instanceList = mutableStateOf<List<String>>(emptyList())
    private val suggestionsExpanded = mutableStateOf(false)

    private var targetServer: Host? = null
    private var targetServerInfo: TootInstance? = null

    init {
        validateAndShow()
        val composeView = ComposeView(activity).apply {
            setContent {
                StThemedContent {
                    LoginFormContent()
                }
            }
        }
        dialog.setContentView(composeView)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
        dialog.show()
        initServerNameList()
    }

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    @Composable
    private fun LoginFormContent() {
        val keyboardController = LocalSoftwareKeyboardController.current
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
            ) {
                // Header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            if (currentPage.intValue == 0) R.string.server_host_name
                            else R.string.authentication_select
                        ),
                        modifier = Modifier.weight(1f),
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(onClick = { dialog.cancel() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.cancel),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                // Page 0: input server name
                AnimatedVisibility(visible = currentPage.intValue == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    ) {
                        // Server name input with autocomplete
                        val allInstances = instanceList.value
                        val input = serverInput.value
                        val filtered = if (input.isBlank() || allInstances.isEmpty()) {
                            emptyList()
                        } else {
                            val key = input.lowercase()
                            allInstances.filter { it.contains(key) }.take(20)
                        }

                        ExposedDropdownMenuBox(
                            expanded = suggestionsExpanded.value && filtered.isNotEmpty(),
                            onExpandedChange = { suggestionsExpanded.value = it },
                        ) {
                            OutlinedTextField(
                                value = input,
                                onValueChange = {
                                    serverInput.value = it
                                    suggestionsExpanded.value = true
                                    validateAndShow()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                                label = { Text(stringResource(R.string.instance_hint)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Done,
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        keyboardController?.hide()
                                        suggestionsExpanded.value = false
                                        nextPage()
                                    },
                                ),
                            )
                            ExposedDropdownMenu(
                                expanded = suggestionsExpanded.value && filtered.isNotEmpty(),
                                onDismissRequest = { suggestionsExpanded.value = false },
                            ) {
                                filtered.forEach { suggestion ->
                                    DropdownMenuItem(
                                        text = { Text(suggestion) },
                                        onClick = {
                                            serverInput.value = suggestion
                                            suggestionsExpanded.value = false
                                            validateAndShow()
                                        },
                                    )
                                }
                            }
                        }

                        // Error + Next row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            val error = errorText.value
                            if (error.isNotEmpty()) {
                                Text(
                                    text = error,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            } else {
                                androidx.compose.foundation.layout.Spacer(
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Button(
                                onClick = {
                                    keyboardController?.hide()
                                    suggestionsExpanded.value = false
                                    nextPage()
                                },
                                enabled = inputValid.value,
                            ) {
                                Text(stringResource(R.string.next_step))
                            }
                        }

                        // Description text
                        Text(
                            text = stringResource(R.string.input_server_name_desc),
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                // Page 1: select action
                AnimatedVisibility(visible = currentPage.intValue == 1) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    ) {
                        // Server host + edit button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = serverHostText.value,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 50.dp),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            IconButton(onClick = { showPage(0) }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_edit),
                                    contentDescription = stringResource(R.string.previous),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        // Server description
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Text(
                                text = serverDescText.value,
                                modifier = Modifier.padding(2.dp),
                                color = if (serverDescIsError.value) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }

                        // Auth type description
                        Text(
                            text = stringResource(R.string.authentication_select_desc),
                            modifier = Modifier.padding(top = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )

                        // Action buttons
                        for (a in Action.entries) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp),
                            ) {
                                Button(
                                    onClick = { onAuthTypeSelect(a) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(stringResource(a.idName))
                                }
                                Text(
                                    text = stringResource(a.idDesc),
                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initServerNameList() {
        val progress = ProgressDialogEx(activity)
        progress.setMessageEx(activity.getString(R.string.autocomplete_list_loading))
        progress.show()
        launchMain {
            try {
                val list = HashSet<String>().apply {
                    try {
                        withContext(AppDispatchers.IO) {
                            activity.resources.openRawResource(R.raw.server_list).use { inStream ->
                                val br = BufferedReader(InputStreamReader(inStream, "UTF-8"))
                                while (true) {
                                    (br.readLine() ?: break)
                                        .trim { it <= ' ' }
                                        .notEmpty()
                                        ?.lowercase()
                                        ?.let {
                                            add(it)
                                            add(IDN.toASCII(it, IDN.ALLOW_UNASSIGNED))
                                            add(IDN.toUnicode(it, IDN.ALLOW_UNASSIGNED))
                                        }
                                }
                            }
                        }
                    } catch (ex: Throwable) {
                        log.e(ex, "can't load server list.")
                    }
                }.toList().sorted()
                instanceList.value = list
            } catch (ex: Throwable) {
                activity.showToast(ex, "initServerNameList failed.")
            } finally {
                progress.dismissSafe()
            }
        }
    }

    // return validated name. else null
    private fun validateAndShow(): String? {
        val s = serverInput.value.trim()
        if (s.isEmpty()) {
            inputValid.value = false
            errorText.value = activity.getString(R.string.instance_not_specified)
            return null
        }

        arrayOf("http://", "https://").forEach {
            if (s.contains(it)) {
                inputValid.value = false
                errorText.value =
                    activity.getString(R.string.server_host_name_cant_contains_it, it)
                return null
            }
        }
        if (s.contains("/") || s.contains("@")) {
            inputValid.value = false
            errorText.value = activity.getString(R.string.instance_not_need_slash)
            return null
        }

        reBadChars.findAll(s).joinToString("") { it.value }.notEmpty()?.let {
            inputValid.value = false
            errorText.value =
                activity.getString(R.string.server_host_name_cant_contains_it, it)
            return null
        }
        inputValid.value = true
        errorText.value = ""
        return s
    }

    private fun showPage(n: Int) {
        currentPage.intValue = n
    }

    private fun nextPage() {
        activity.run {
            launchAndShowError {
                var host = Host.parse(validateAndShow() ?: return@launchAndShowError)
                var error: String? = null
                val tootInstance = runApiTask2(host) { client ->
                    try {
                        client.getApiHostFromWebFinger(host)?.let {
                            if (it != host) {
                                host = it
                                client.apiHost = it
                            }
                        }
                        TootInstance.getExOrThrow(client, forceUpdate = true)
                    } catch (ex: Throwable) {
                        error = ex.message
                        null
                    }
                }
                if (isDestroyed || isFinishing) return@launchAndShowError
                targetServer = host
                targetServerInfo = tootInstance
                serverHostText.value = tootInstance?.apDomain?.pretty ?: host.pretty
                when (tootInstance) {
                    null -> {
                        serverDescIsError.value = true
                        serverDescText.value = error ?: ""
                    }

                    else -> {
                        serverDescIsError.value = false
                        serverDescText.value = (tootInstance.description.notBlank()
                            ?: tootInstance.descriptionOld.notBlank()
                            ?: "(empty server description)")
                            .let {
                                DecodeOptions(
                                    applicationContext,
                                    LinkHelper.create(tootInstance),
                                    forceHtml = true,
                                    short = true,
                                ).decodeHTML(it)
                            }.replace("""\n[\s\n]+""".toRegex(), "\n")
                            .trim()
                            .toString()
                    }
                }
                showPage(1)
            }
        }
    }

    private fun onAuthTypeSelect(action: Action) {
        targetServer?.let { onClickOk(dialog, it, targetServerInfo, action) }
    }
}
