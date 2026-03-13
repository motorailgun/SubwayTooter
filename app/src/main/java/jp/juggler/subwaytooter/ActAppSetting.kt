package jp.juggler.subwaytooter

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.util.JsonWriter
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.WorkerThread
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import jp.juggler.subwaytooter.appsetting.AppSettingItem
import jp.juggler.subwaytooter.appsetting.SettingType
import jp.juggler.subwaytooter.appsetting.appSettingRoot
import jp.juggler.subwaytooter.compose.ColorPickerDialog
import jp.juggler.subwaytooter.dialog.DlgAppPicker
import jp.juggler.subwaytooter.notification.restartAllWorker
import jp.juggler.subwaytooter.pref.FILE_PROVIDER_AUTHORITY
import jp.juggler.subwaytooter.pref.impl.BooleanPref
import jp.juggler.subwaytooter.pref.impl.FloatPref
import jp.juggler.subwaytooter.pref.impl.IntPref
import jp.juggler.subwaytooter.pref.impl.StringPref
import jp.juggler.subwaytooter.pref.lazyPref
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.subwaytooter.table.daoAcctColor
import jp.juggler.subwaytooter.table.daoLogData
import jp.juggler.subwaytooter.util.CustomShare
import jp.juggler.subwaytooter.util.CustomShareTarget
import jp.juggler.subwaytooter.util.cn
import jp.juggler.util.backPressed
import jp.juggler.util.coroutine.launchAndShowError
import jp.juggler.util.coroutine.launchProgress
import jp.juggler.util.data.cast
import jp.juggler.util.data.checkMimeTypeAndGrant
import jp.juggler.util.data.defaultLocale
import jp.juggler.util.data.intentOpenDocument
import jp.juggler.util.data.notEmpty
import jp.juggler.util.data.notZero
import jp.juggler.util.getPackageInfoCompat
import jp.juggler.util.log.LogCategory
import jp.juggler.util.log.dialogOrToast
import jp.juggler.util.log.showToast
import jp.juggler.util.log.withCaption
import jp.juggler.util.queryIntentActivitiesCompat
import jp.juggler.util.ui.ActivityResultHandler
import jp.juggler.util.ui.isNotOk
import jp.juggler.util.ui.launch
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStreamWriter
import java.text.NumberFormat
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.abs

class ActAppSetting : ComponentActivity() {

    companion object {
        private const val STATE_CHOOSE_INTENT_TARGET = "customShareTarget"

        val reLinefeed = Regex("[\\x0d\\x0a]+")

        internal val log = LogCategory("ActAppSetting")

        fun ActivityResultHandler.launchAppSetting() =
            launch(Intent(context!!, ActAppSetting::class.java))
    }

    // ---- Compose state ----
    private val revision = mutableIntStateOf(0)
    private val currentSection = mutableStateOf<AppSettingItem?>(null)
    private val activeQuery = mutableStateOf<String?>(null)
    private val searchInput = mutableStateOf("")

    // ---- State ----
    private var customShareTarget: CustomShareTarget? = null
    lateinit var handler: Handler

    val defaultLineSpacingExtra = HashMap<String, Float>()
    val defaultLineSpacingMultiplier = HashMap<String, Float>()

    // Divider sentinel for item list
    private val divider = Any()

    // Color picker dialog state
    private val colorPickerItem = mutableStateOf<AppSettingItem?>(null)

    fun refreshUi() {
        revision.intValue++
    }

    // ---- Activity Result Handlers ----

    private val arNoop = ActivityResultHandler(log) { }

    private val arImportAppData = ActivityResultHandler(log) { r ->
        if (r.isNotOk) return@ActivityResultHandler
        r.data?.checkMimeTypeAndGrant(contentResolver)
            ?.firstOrNull()
            ?.uri?.let { importAppData2(false, it) }
    }

    val arTimelineFont = ActivityResultHandler(log) { r ->
        if (r.isNotOk) return@ActivityResultHandler
        r.data?.let { handleFontResult(AppSettingItem.TIMELINE_FONT, it, "TimelineFont") }
    }

    val arTimelineFontBold = ActivityResultHandler(log) { r ->
        if (r.isNotOk) return@ActivityResultHandler
        r.data?.let {
            handleFontResult(AppSettingItem.TIMELINE_FONT_BOLD, it, "TimelineFontBold")
        }
    }

    private val arSaveAppData = ActivityResultHandler(log) { r ->
        launchAndShowError {
            if (r.resultCode != RESULT_OK) return@launchAndShowError
            val outUri = r.data?.data ?: error("missing result.data.data")
            launchProgress(
                "save app data",
                doInBackground = {
                    val tempFile = encodeAppData()
                    try {
                        FileInputStream(tempFile).use { inStream ->
                            (contentResolver.openOutputStream(outUri)
                                ?: error("contentResolver.openOutputStream returns null : $outUri"))
                                .use { inStream.copyTo(it) }
                        }
                    } finally {
                        tempFile.delete()
                    }
                }
            )
        }
    }

    // ---- Lifecycle ----

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backPressed { handleBack() }

        arNoop.register(this)
        arImportAppData.register(this)
        arTimelineFont.register(this)
        arTimelineFontBold.register(this)
        arSaveAppData.register(this)

        App1.setActivityTheme(this)
        handler = App1.getAppState(this).handler

        if (savedInstanceState != null) {
            try {
                savedInstanceState.getString(STATE_CHOOSE_INTENT_TARGET)?.let { target ->
                    customShareTarget = CustomShareTarget.entries.find { it.name == target }
                }
            } catch (ex: Throwable) {
                log.e(ex, "can't restore customShareTarget.")
            }
        }

        removeDefaultPref()

        setContent {
            AppSettingContent(modifier = Modifier)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        customShareTarget?.name?.let {
            outState.putString(STATE_CHOOSE_INTENT_TARGET, it)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent) = try {
        super.dispatchKeyEvent(event)
    } catch (ex: Throwable) {
        log.e(ex, "dispatchKeyEvent error")
        false
    }

    override fun onStop() {
        super.onStop()
        restartAllWorker(context = this)
    }

    // ---- Navigation ----

    private fun handleBack() {
        when {
            activeQuery.value != null -> {
                activeQuery.value = null
                searchInput.value = ""
            }
            currentSection.value != null -> currentSection.value = null
            else -> finish()
        }
    }

    /**
     * Navigate to a section. Called from AppSettingItem lambda.
     */
    fun load(section: AppSettingItem?, @Suppress("UNUSED_PARAMETER") query: String?) {
        if (section != null) {
            currentSection.value = section
            activeQuery.value = null
            searchInput.value = ""
        } else {
            currentSection.value = null
            activeQuery.value = null
            searchInput.value = ""
        }
    }

    private fun buildItemsList(
        section: AppSettingItem?,
        query: String?,
    ): List<Any> = buildList {
        var lastPath: String? = null

        fun addParentPath(item: AppSettingItem) {
            add(divider)
            val pathList = ArrayList<String>()
            var parent = item.parent
            while (parent != null) {
                if (parent.caption != 0) pathList.add(0, getString(parent.caption))
                parent = parent.parent
            }
            val path = pathList.joinToString("/")
            if (path != lastPath) {
                lastPath = path
                add(path)
                add(divider)
            }
        }

        fun queryRecursive(item: AppSettingItem, q: String) {
            if (item.caption == 0) return
            when (item.type) {
                SettingType.Section ->
                    item.items.forEach { queryRecursive(it, q) }

                SettingType.Group -> {
                    if (item.match(this@ActAppSetting, q) ||
                        item.items.any { it.match(this@ActAppSetting, q) }
                    ) {
                        addParentPath(item)
                        add(item)
                        addAll(item.items)
                    }
                }

                else -> {
                    if (item.match(this@ActAppSetting, q)) {
                        addParentPath(item)
                        add(item)
                    }
                    item.items.forEach { queryRecursive(it, q) }
                }
            }
        }

        fun addSectionItems(sec: AppSettingItem?) {
            sec ?: return
            for (item in sec.items) {
                add(divider)
                add(item)
                if (item.items.isNotEmpty()) {
                    when (item.type) {
                        SettingType.Group -> addAll(item.items)
                        else -> addSectionItems(item)
                    }
                }
            }
        }

        when {
            query?.isNotEmpty() == true -> queryRecursive(appSettingRoot, query)
            section != null -> addSectionItems(section)
            else -> {
                for (child in appSettingRoot.items) {
                    add(divider)
                    add(child)
                }
            }
        }
        if (isNotEmpty()) add(divider)
    }

    // ---- Main Composable ----

    @Composable
    private fun AppSettingContent(modifier: Modifier) {
        val rev = revision.intValue
        val section = currentSection.value
        val query = activeQuery.value

        // Debounced search
        val inputText = searchInput.value
        LaunchedEffect(inputText) {
            if (inputText.isEmpty()) {
                activeQuery.value = null
            } else {
                delay(166)
                activeQuery.value = inputText
            }
        }

        val items = remember(section, query, rev) {
            buildItemsList(section, query)
        }

        // Color picker dialog
        colorPickerItem.value?.let { item ->
            val ip = item.pref.cast<IntPref>() ?: return@let
            ColorPickerDialog(
                colorInitial = ip.value.notZero() ?: Color.BLACK,
                alphaEnabled = item.type == SettingType.ColorAlpha,
                onDismiss = { colorPickerItem.value = null },
                onColorSelected = { newColor ->
                    val c = when (item.type) {
                        SettingType.ColorAlpha -> newColor.notZero() ?: 1
                        else -> newColor or Color.BLACK
                    }
                    ip.value = c
                    item.changed(this@ActAppSetting)
                    colorPickerItem.value = null
                    refreshUi()
                },
            )
        }

        Column(modifier.fillMaxSize()) {
            // Search bar
            SearchBar()

            // Settings list
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                itemsIndexed(
                    items,
                    contentType = { _, item ->
                        when (item) {
                            divider -> "divider"
                            is String -> "path"
                            is AppSettingItem -> item.type.id
                            else -> "unknown"
                        }
                    },
                ) { _, item ->
                    when (item) {
                        divider -> HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        is String -> Text(
                            text = item,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 3.dp),
                        )
                        is AppSettingItem -> SettingItemComposable(item)
                    }
                }
            }
        }
    }

    @Composable
    private fun SearchBar() {
        val keyboardController = LocalSoftwareKeyboardController.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = searchInput.value,
                onValueChange = { searchInput.value = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.search)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
            )
            if (searchInput.value.isNotEmpty()) {
                IconButton(onClick = {
                    searchInput.value = ""
                    activeQuery.value = null
                    keyboardController?.hide()
                }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.reset))
                }
            }
        }
    }

    // ---- Setting item composables ----

    @Composable
    private fun SettingItemComposable(item: AppSettingItem) {
        val name = if (item.caption == 0) "" else stringResource(item.caption)

        Column(modifier = Modifier.fillMaxWidth()) {
            when (item.type) {
                SettingType.Section -> SectionItem(item, name)
                SettingType.Action -> ActionItem(item, name)
                SettingType.Switch -> SwitchItem(item, name)
                SettingType.CheckBox -> CheckboxItem(item, name)
                SettingType.EditText -> EditTextItem(item, name)
                SettingType.Spinner -> SpinnerItem(item, name)
                SettingType.ColorOpaque, SettingType.ColorAlpha -> ColorItem(item, name)
                SettingType.Group -> GroupItem(name)
                SettingType.TextWithSelector -> TextWithSelectorItem(item, name)
                SettingType.Sample -> SampleItem(item)
                else -> {}
            }

            // Description
            if (item.desc != 0) {
                val descText = stringResource(item.desc)
                Text(
                    text = descText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(start = 32.dp, top = 2.dp)
                        .then(
                            if (item.descClickSet) {
                                Modifier.clickable { item.descClick.invoke(this@ActAppSetting) }
                            } else Modifier
                        ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    private fun CaptionText(
        name: String,
        item: AppSettingItem? = null,
    ) {
        if (name.isEmpty()) return
        val fontSize = item?.captionFontSize?.invoke(this@ActAppSetting)
        val spacing = item?.captionSpacing?.invoke(this@ActAppSetting)
        val lineHeight = if (spacing != null && spacing.isFinite()) {
            (14f * spacing).sp
        } else TextUnit.Unspecified

        Text(
            text = name,
            fontSize = fontSize?.sp ?: 14.sp,
            lineHeight = lineHeight,
        )
    }

    @Composable
    private fun SectionItem(item: AppSettingItem, name: String) {
        Button(
            onClick = {
                currentSection.value = item
                activeQuery.value = null
                searchInput.value = ""
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = item.enabled,
        ) {
            Text(name)
        }
    }

    @Composable
    private fun ActionItem(item: AppSettingItem, name: String) {
        Button(
            onClick = { item.action(this@ActAppSetting) },
            modifier = Modifier.fillMaxWidth(),
            enabled = item.enabled,
        ) {
            Text(name)
        }
    }

    @Composable
    private fun SwitchItem(item: AppSettingItem, name: String) {
        val bp = item.pref.cast<BooleanPref>() ?: return
        CaptionText(name, item)
        Row(
            modifier = Modifier.padding(start = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(
                checked = bp.value,
                onCheckedChange = {
                    bp.value = it
                    item.changed(this@ActAppSetting)
                    refreshUi()
                },
                enabled = item.enabled,
            )
        }
    }

    @Composable
    private fun CheckboxItem(item: AppSettingItem, name: String) {
        val bp = item.pref.cast<BooleanPref>() ?: return
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = item.enabled) {
                    bp.value = !bp.value
                    item.changed(this@ActAppSetting)
                    refreshUi()
                }
                .padding(start = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = bp.value,
                onCheckedChange = null,
                enabled = item.enabled,
            )
            Spacer(Modifier.width(8.dp))
            Text(name)
        }
    }

    @Composable
    private fun EditTextItem(item: AppSettingItem, name: String) {
        CaptionText(name, item)

        val pi = item.pref
        val currentText = when (pi) {
            is FloatPref -> item.fromFloat.invoke(this@ActAppSetting, pi.value)
            is StringPref -> pi.value
            else -> ""
        }

        var text by remember(revision.intValue, item.id) { mutableStateOf(currentText) }
        val error = item.getError.invoke(this@ActAppSetting, text)

        TextField(
            value = text,
            onValueChange = { newText ->
                val filtered = item.filter.invoke(newText)
                text = filtered
                when (pi) {
                    is StringPref -> pi.value = filtered
                    is FloatPref -> {
                        val fv = item.toFloat.invoke(this@ActAppSetting, filtered)
                        if (fv.isFinite()) pi.value = fv
                        else pi.removeValue()
                    }
                }
                item.changed(this@ActAppSetting)
                refreshUi()
            },
            modifier = Modifier.padding(start = 32.dp).fillMaxWidth(),
            singleLine = true,
            isError = error != null,
            supportingText = if (error != null) {
                { Text(error, color = MaterialTheme.colorScheme.error) }
            } else null,
            placeholder = item.hint?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = when {
                    item.inputType and InputType.TYPE_CLASS_NUMBER != 0 -> KeyboardType.Number
                    item.inputType and InputType.TYPE_NUMBER_FLAG_DECIMAL != 0 -> KeyboardType.Decimal
                    else -> KeyboardType.Text
                },
                imeAction = ImeAction.Next,
            ),
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SpinnerItem(item: AppSettingItem, name: String) {
        CaptionText(name, item)

        val pi = item.pref
        if (pi is IntPref) {
            // Simple spinner with IntPref
            val options = item.spinnerArgs?.map { stringResource(it) }
                ?: item.spinnerArgsProc.invoke(this@ActAppSetting)

            if (options.isEmpty()) return

            var expanded by remember { mutableStateOf(false) }
            val selectedIndex = pi.value.coerceIn(0, options.size - 1)

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.padding(start = 32.dp).fillMaxWidth(),
            ) {
                TextField(
                    value = options.getOrElse(selectedIndex) { "" },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    options.forEachIndexed { index, text ->
                        DropdownMenuItem(
                            text = { Text(text) },
                            onClick = {
                                expanded = false
                                pi.value = index
                                item.changed(this@ActAppSetting)
                                refreshUi()
                            },
                        )
                    }
                }
            }
        } else {
            // Complex spinner with custom initializer — use AndroidView
            AndroidView(
                factory = { ctx ->
                    Spinner(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                        item.spinnerInitializer.invoke(this@ActAppSetting, this)
                        onItemSelectedListener =
                            object : android.widget.AdapterView.OnItemSelectedListener {
                                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                                override fun onItemSelected(
                                    parent: android.widget.AdapterView<*>?,
                                    view: View?,
                                    position: Int,
                                    id: Long,
                                ) {
                                    item.spinnerOnSelected.invoke(
                                        this@ActAppSetting,
                                        this@apply,
                                        position,
                                    )
                                    item.changed(this@ActAppSetting)
                                    refreshUi()
                                }
                            }
                    }
                },
                modifier = Modifier.padding(start = 32.dp).fillMaxWidth(),
            )
        }
    }

    @Composable
    private fun ColorItem(item: AppSettingItem, name: String) {
        val ip = item.pref.cast<IntPref>() ?: return
        CaptionText(name, item)

        Row(
            modifier = Modifier.padding(start = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { colorPickerItem.value = item },
                enabled = item.enabled,
            ) {
                Text(stringResource(R.string.edit))
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = {
                    ip.removeValue()
                    item.changed(this@ActAppSetting)
                    refreshUi()
                },
                enabled = item.enabled,
            ) {
                Text(stringResource(R.string.reset))
            }
            Spacer(Modifier.width(8.dp))
            // Color swatch
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(androidx.compose.ui.graphics.Color(ip.value)),
            )
        }
    }

    @Composable
    private fun GroupItem(name: String) {
        if (name.isNotEmpty()) {
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }

    @Composable
    private fun TextWithSelectorItem(item: AppSettingItem, name: String) {
        CaptionText(name, item)

        Row(
            modifier = Modifier.padding(start = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Use AndroidView for the text display since showTextView may set
            // compound drawables and typeface
            AndroidView(
                factory = { ctx ->
                    TextView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                    }
                },
                update = { tv ->
                    item.showTextView.invoke(this@ActAppSetting, tv)
                },
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { item.onClickEdit.invoke(this@ActAppSetting) }) {
                Text(stringResource(R.string.edit))
            }
            Spacer(Modifier.width(4.dp))
            OutlinedButton(onClick = {
                item.onClickReset.invoke(this@ActAppSetting)
                refreshUi()
            }) {
                Text(stringResource(R.string.reset))
            }
        }
    }

    @Composable
    private fun SampleItem(item: AppSettingItem) {
        AndroidView(
            factory = { ctx ->
                LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                    item.sampleViewCreator?.invoke(this)
                }
            },
            update = { view ->
                item.sampleUpdate.invoke(this@ActAppSetting, view)
            },
            modifier = Modifier.padding(start = 32.dp).fillMaxWidth(),
        )
    }

    // ---- Pref cleanup ----

    private fun removeDefaultPref() {
        val e = lazyPref.edit()
        var changed = false
        appSettingRoot.scan {
            when {
                (it.pref as? IntPref)?.noRemove == true -> Unit
                it.pref?.removeDefault(lazyPref, e) == true -> changed = true
            }
        }
        if (changed) e.apply()
    }

    // ---- Methods called from AppSettingItem lambdas ----

    /**
     * Stub type returned by findItemViewHolder. Always null in Compose.
     * Exists so that AppSettingItem lambdas calling
     * findItemViewHolder(item)?.updateCaption() still compile.
     */
    class ViewHolderStub {
        fun updateCaption() {}
        fun showColor() {}
    }

    /**
     * No ViewHolders in Compose. Returns null; callers null-check.
     * Recomposition handles UI updates via refreshUi().
     */
    fun findItemViewHolder(item: AppSettingItem?): ViewHolderStub? = null

    /**
     * Triggers recomposition to update sample views.
     */
    fun showSample(item: AppSettingItem?) {
        refreshUi()
    }

    /**
     * No-op in Material3 Compose — switches use theme colors.
     */
    fun setSwitchColor() {}

    // Overload to accept any View — also no-op.
    @Suppress("UNUSED_PARAMETER")
    fun setSwitchColor(root: View?) {}

    fun initSpinner(spinner: Spinner, captions: List<String>) {
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            captions.toTypedArray()
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    // ---- Font size formatting ----

    fun formatFontSize(fv: Float): String =
        when {
            fv.isFinite() -> String.format(defaultLocale(this), "%.1f", fv)
            else -> ""
        }

    fun parseFontSize(src: String): Float {
        try {
            if (src.isNotEmpty()) {
                val f = NumberFormat.getInstance(defaultLocale(this)).parse(src)?.toFloat()
                return when {
                    f == null -> Float.NaN
                    f.isNaN() -> Float.NaN
                    f < 0f -> 0f
                    f > 999f -> 999f
                    else -> f
                }
            }
        } catch (ex: Throwable) {
            log.e(ex, "parseFontSize failed.")
        }
        return Float.NaN
    }

    // ---- Font handling ----

    private fun handleFontResult(item: AppSettingItem?, data: Intent, fileName: String) {
        item ?: error("handleFontResult : setting item is null")
        data.checkMimeTypeAndGrant(contentResolver).firstOrNull()?.uri?.let {
            val file = saveTimelineFont(it, fileName)
            if (file != null) {
                (item.pref as? StringPref)?.value = file.absolutePath
                refreshUi()
            }
        }
    }

    fun showTimelineFont(item: AppSettingItem?) {
        refreshUi()
    }

    fun showTimelineFont(item: AppSettingItem, tv: TextView) {
        try {
            item.pref.cast<StringPref>()?.value.notEmpty()?.let { url ->
                tv.typeface = Typeface.DEFAULT
                val face = Typeface.createFromFile(url)
                tv.typeface = face
                tv.text = url
                return
            }
        } catch (ex: Throwable) {
            log.e(ex, "showTimelineFont failed.")
        }
        tv.text = getString(R.string.not_selected_2)
        tv.typeface = Typeface.DEFAULT
    }

    private fun saveTimelineFont(uri: Uri?, fileName: String): File? {
        try {
            if (uri == null) {
                showToast(false, "missing uri.")
                return null
            }

            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val dir = filesDir
            dir.mkdir()

            val tmpFile = File(dir, "$fileName.tmp")

            val source: InputStream? = contentResolver.openInputStream(uri)
            if (source == null) {
                showToast(false, "openInputStream returns null. uri=$uri")
                return null
            } else {
                source.use { inStream ->
                    FileOutputStream(tmpFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
            }

            val face = Typeface.createFromFile(tmpFile)
            if (face == null) {
                showToast(false, "Typeface.createFromFile() failed.")
                return null
            }

            val file = File(dir, fileName)
            if (!tmpFile.renameTo(file)) {
                showToast(false, "File operation failed.")
                return null
            }

            return file
        } catch (ex: Throwable) {
            log.e(ex, "saveTimelineFont failed.")
            showToast(ex, "saveTimelineFont failed.")
            return null
        }
    }

    // ---- App data export/import ----

    @Suppress("BlockingMethodInNonBlockingContext")
    fun sendAppData() {
        val activity = this
        launchProgress(
            "export app data",
            doInBackground = { encodeAppData() },
            afterProc = {
                try {
                    val uri = FileProvider.getUriForFile(activity, FILE_PROVIDER_AUTHORITY, it)
                    Intent(Intent.ACTION_SEND).apply {
                        type = contentResolver.getType(uri)
                        putExtra(Intent.EXTRA_SUBJECT, "SubwayTooter app data")
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }.launch(arNoop)
                } catch (ex: Throwable) {
                    log.e(ex, "exportAppData failed.")
                    dialogOrToast(ex.withCaption(getString(R.string.missing_app_can_receive_action_send)))
                }
            }
        )
    }

    fun saveAppData() {
        try {
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/zip"
                putExtra(Intent.EXTRA_TITLE, "SubwayTooter app data.zip")
            }.launch(arSaveAppData)
        } catch (ex: Throwable) {
            log.e(ex, "can't find app that can handle ACTION_CREATE_DOCUMENT.")
            dialogOrToast(ex.withCaption("can't find app that can handle ACTION_CREATE_DOCUMENT."))
        }
    }

    @WorkerThread
    private fun encodeAppData(): File {
        val activity = this
        val cacheDir = externalCacheDir ?: cacheDir ?: error("missing cache directory")
        cacheDir.mkdirs()

        val name = "SubwayTooter.${android.os.Process.myPid()}.${android.os.Process.myTid()}.zip"
        val file = File(cacheDir, name)

        ZipOutputStream(FileOutputStream(file)).use { zipStream ->
            zipStream.putNextEntry(ZipEntry("AppData.json"))
            try {
                val jw = JsonWriter(OutputStreamWriter(zipStream, "UTF-8"))
                jp.juggler.subwaytooter.appsetting.AppDataExporter.encodeAppData(activity, jw)
                jw.flush()
            } finally {
                zipStream.closeEntry()
            }
            val appState = App1.getAppState(activity)
            for (column in appState.columnList) {
                jp.juggler.subwaytooter.appsetting.AppDataExporter.saveBackgroundImage(
                    activity,
                    zipStream,
                    column,
                )
            }
        }

        return file
    }

    fun importAppData1() {
        try {
            val intent = intentOpenDocument("*/*")
            arImportAppData.launch(intent)
        } catch (ex: Throwable) {
            showToast(ex, "importAppData(1) failed.")
        }
    }

    private fun importAppData2(bConfirm: Boolean, uri: Uri) {
        val type = contentResolver.getType(uri)
        log.d("importAppData type=$type")

        if (!bConfirm) {
            android.app.AlertDialog.Builder(this)
                .setMessage(getString(R.string.app_data_import_confirm))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _, _ -> importAppData2(true, uri) }
                .show()
            return
        }

        val data = Intent()
        data.data = uri
        setResult(ActMain.RESULT_APP_DATA_IMPORT, data)
        finish()
    }

    // ---- Custom share ----

    fun openCustomShareChooser(appSettingItem: AppSettingItem, target: CustomShareTarget) {
        try {
            val rv = DlgAppPicker(
                this,
                intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getString(R.string.content_sample))
                },
                addCopyAction = true
            ) { setCustomShare(appSettingItem, target, it) }
                .show()
            if (!rv) showToast(true, "share target app is not installed.")
        } catch (ex: Throwable) {
            log.e(ex, "openCustomShareChooser failed.")
            showToast(ex, "openCustomShareChooser failed.")
        }
    }

    fun setCustomShare(appSettingItem: AppSettingItem, target: CustomShareTarget, value: String) {
        val sp: StringPref = appSettingItem.pref.cast() ?: error("$target: not StringPref")
        sp.value = value
        refreshUi()
    }

    fun showCustomShareIcon(tv: TextView?, target: CustomShareTarget) {
        tv ?: return
        val cn = target.customShareComponentName
        val (label, icon) = CustomShare.getInfo(this, cn)
        tv.text = label ?: getString(R.string.not_selected_2)
        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null)
        tv.compoundDrawablePadding = (resources.displayMetrics.density * 4f + 0.5f).toInt()
    }

    fun openWebBrowserChooser(
        appSettingItem: AppSettingItem,
        intent: Intent,
        filter: (ResolveInfo) -> Boolean,
    ) {
        try {
            val rv = DlgAppPicker(
                this,
                intent = intent,
                filter = filter,
                addCopyAction = false
            ) { setWebBrowser(appSettingItem, it) }
                .show()
            if (!rv) showToast(true, "share target app is not installed.")
        } catch (ex: Throwable) {
            log.e(ex, "openCustomShareChooser failed.")
            showToast(ex, "openCustomShareChooser failed.")
        }
    }

    private fun setWebBrowser(appSettingItem: AppSettingItem, value: String) {
        val sp: StringPref = appSettingItem.pref.cast()
            ?: error("${getString(appSettingItem.caption)}: not StringPref")
        sp.value = value
        refreshUi()
    }

    fun showWebBrowser(tv: TextView?, prefValue: String) {
        tv ?: return
        val cn = prefValue.cn()
        val (label, icon) = CustomShare.getInfo(this, cn)
        tv.text = label ?: getString(R.string.not_selected_2)
        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null)
        tv.compoundDrawablePadding = (resources.displayMetrics.density * 4f + 0.5f).toInt()
    }

    // ---- Log export ----

    fun exportLog() {
        val context = this
        launchAndShowError {
            val logZipFile = daoLogData.createLogFile(context)
            val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, logZipFile)

            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("tateisu@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "SubwayTooter bug report")
                val soc = if (Build.VERSION.SDK_INT >= 31) {
                    "manufacturer=${Build.SOC_MANUFACTURER} product=${Build.SOC_MODEL}"
                } else {
                    "(no information)"
                }
                val text = """
                    |Please write about the problem.
                    |…
                    |…
                    |…
                    |…
                    |    
                    |Don't rewrite below lines.
                    |SubwayTooter version: $currentVersion $packageName
                    |Android version: ${Build.VERSION.RELEASE}
                    |Device: manufacturer=${Build.MANUFACTURER} product=${Build.PRODUCT} model=${Build.MODEL} device=${Build.DEVICE}
                    |$soc
                    """.trimMargin("|")

                putExtra(Intent.EXTRA_TEXT, text)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(uri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooserIntent = Intent.createChooser(intent, null)
            grantFileProviderUri(intent, uri)
            grantFileProviderUri(chooserIntent, uri)
            startActivity(chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private val currentVersion: String
        get() = try {
            packageManager.getPackageInfoCompat(packageName)?.versionName
        } catch (ignored: Throwable) {
            null
        } ?: "??"

    private fun Context.grantFileProviderUri(
        intent: Intent,
        uri: Uri,
        permission: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION,
    ) {
        try {
            intent.addFlags(permission)
            packageManager.queryIntentActivitiesCompat(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            ).forEach {
                grantUriPermission(it.activityInfo.packageName, uri, permission)
            }
        } catch (ex: Throwable) {
            log.e(ex, "grantFileProviderUri failed.")
        }
    }

    // ---- Inner adapters (used by AndroidView Spinners) ----

    inner class AccountAdapter(val list: List<SavedAccount>) : BaseAdapter() {

        override fun getCount() = 1 + list.size
        override fun getItemId(position: Int) = 0L
        override fun getItem(position: Int) = if (position == 0) null else list[position - 1]

        override fun getView(position: Int, viewOld: View?, parent: ViewGroup): View {
            val view = viewOld ?: TextView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                minimumHeight = parent.context.resources.displayMetrics.density.let {
                    (48 * it + 0.5f).toInt()
                }
                val pad = parent.context.resources.displayMetrics.density.let {
                    (4 * it + 0.5f).toInt()
                }
                setPadding(pad, pad, pad, pad)
                id = android.R.id.text1
            }
            view.findViewById<TextView>(android.R.id.text1).text = when (position) {
                0 -> getString(R.string.default_post_account_default_action)
                else -> daoAcctColor.getNickname(list[position - 1])
            }
            return view
        }

        override fun getDropDownView(position: Int, viewOld: View?, parent: ViewGroup): View {
            val view =
                viewOld ?: layoutInflater.inflate(
                    android.R.layout.simple_spinner_dropdown_item,
                    parent,
                    false,
                )
            view.findViewById<TextView>(android.R.id.text1).text = when (position) {
                0 -> getString(R.string.default_post_account_default_action)
                else -> daoAcctColor.getNickname(list[position - 1])
            }
            return view
        }

        internal fun getIndexFromId(dbId: Long): Int =
            1 + list.indexOfFirst { it.db_id == dbId }

        internal fun getIdFromIndex(position: Int): Long =
            if (position > 0) list[position - 1].db_id else -1L
    }

    private class TimeZoneItem(
        val id: String,
        val caption: String,
        val offset: Int,
    )

    inner class TimeZoneAdapter internal constructor() : BaseAdapter() {

        private val list = ArrayList<TimeZoneItem>()

        init {
            for (id in TimeZone.getAvailableIDs()) {
                val tz = TimeZone.getTimeZone(id)

                if (!when {
                        !tz.id.contains('/') -> false
                        tz.id == "Etc/GMT+12" -> true
                        tz.id.startsWith("Etc/") -> false
                        else -> true
                    }
                ) continue

                var offset = tz.rawOffset.toLong()
                val caption = when (offset) {
                    0L -> "(UTC\u00B100:00) ${tz.id} ${tz.displayName}"
                    else -> {
                        val format = when {
                            offset > 0 -> "(UTC+%02d:%02d) %s %s"
                            else -> "(UTC-%02d:%02d) %s %s"
                        }
                        offset = abs(offset)
                        val hours = TimeUnit.MILLISECONDS.toHours(offset)
                        val minutes =
                            TimeUnit.MILLISECONDS.toMinutes(offset) - TimeUnit.HOURS.toMinutes(hours)
                        String.format(format, hours, minutes, tz.id, tz.displayName)
                    }
                }
                if (list.none { it.caption == caption }) {
                    list.add(TimeZoneItem(id, caption, tz.rawOffset))
                }
            }

            list.sortWith { a, b ->
                (a.offset - b.offset).notZero() ?: a.caption.compareTo(b.caption)
            }

            list.add(0, TimeZoneItem("", getString(R.string.device_timezone), 0))
        }

        override fun getCount() = list.size
        override fun getItem(position: Int): Any = list[position]
        override fun getItemId(position: Int) = 0L

        override fun getView(position: Int, viewOld: View?, parent: ViewGroup): View {
            val view = viewOld ?: layoutInflater.inflate(
                android.R.layout.simple_spinner_item,
                parent,
                false,
            )
            view.findViewById<TextView>(android.R.id.text1).text = list[position].caption
            return view
        }

        override fun getDropDownView(position: Int, viewOld: View?, parent: ViewGroup): View {
            val view = viewOld ?: layoutInflater.inflate(
                android.R.layout.simple_spinner_dropdown_item,
                parent,
                false,
            )
            view.findViewById<TextView>(android.R.id.text1).text = list[position].caption
            return view
        }

        internal fun getIndexFromId(tzId: String): Int {
            val index = list.indexOfFirst { it.id == tzId }
            return if (index == -1) 0 else index
        }

        internal fun getIdFromIndex(position: Int): String = list[position].id
    }
}
