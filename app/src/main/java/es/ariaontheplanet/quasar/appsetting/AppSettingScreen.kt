package es.ariaontheplanet.quasar.appsetting

import android.graphics.Color
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import es.ariaontheplanet.quasar.ActAppSetting
import es.ariaontheplanet.quasar.R
import es.ariaontheplanet.quasar.compose.ColorPickerDialog
import jp.juggler.util.data.cast
import jp.juggler.util.data.notEmpty
import jp.juggler.util.data.notZero
import es.ariaontheplanet.quasar.pref.impl.BooleanPref
import es.ariaontheplanet.quasar.pref.impl.FloatPref
import es.ariaontheplanet.quasar.pref.impl.IntPref
import es.ariaontheplanet.quasar.pref.impl.StringPref
import kotlinx.coroutines.delay

// Divider sentinel for item list
// private val divider = Any()

/**
 * Main entry point for App Setting Screen.
 * Accepts both ViewModel and Activity for pragmatic legacy support.
 */
@Composable
fun AppSettingScreen(
    viewModel: AppSettingViewModel,
    activity: ActAppSetting,
    modifier: Modifier = Modifier,
) {
    AppSettingContent(viewModel, activity, modifier)
}

/**
 * Stateful content composable that collects from ViewModel states.
 */
@Composable
fun AppSettingContent(
    viewModel: AppSettingViewModel,
    activity: ActAppSetting,
    modifier: Modifier = Modifier,
) {
    val section by viewModel.currentSection.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val revision by viewModel.revision.collectAsState()
    val colorPickerItem by viewModel.colorPickerItem.collectAsState()

    // State for search input (local)
    var searchInput by remember { mutableStateOf("") }
    var activeQuery by remember { mutableStateOf<String?>(null) }

    // Debounced search
    LaunchedEffect(searchInput) {
        if (searchInput.isEmpty()) {
            activeQuery = null
        } else {
            delay(166)
            activeQuery = searchInput
            viewModel.setSearchQuery(searchInput)
        }
    }

    val items = remember(section, activeQuery, revision) {
        viewModel.buildItemsList(section, activeQuery)
    }

    // Color picker dialog
    colorPickerItem?.let { item ->
        val ip = item.pref.cast<IntPref>() ?: return@let
        ColorPickerDialog(
            colorInitial = ip.value.notZero() ?: Color.BLACK,
            alphaEnabled = item.type == SettingType.ColorAlpha,
            onDismiss = { viewModel.closeColorPicker() },
            onColorSelected = { newColor ->
                val c = when (item.type) {
                    SettingType.ColorAlpha -> newColor.notZero() ?: 1
                    else -> newColor or Color.BLACK
                }
                ip.value = c
                item.changed(activity)
                viewModel.closeColorPicker()
                viewModel.refreshUi()
            },
        )
    }

    Column(modifier.fillMaxSize()) {
        // Search bar
        SearchBar(
            searchInput = searchInput,
            onSearchInputChange = { searchInput = it },
        )

        // Settings list
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            itemsIndexed(
                items,
                contentType = { _, item ->
                    when (item) {
                        Divider -> "divider"
                        is String -> "path"
                        is AppSettingItem -> item.type.id
                        else -> "unknown"
                    }
                },
            ) { _, item ->
                when (item) {
                    Divider -> HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    is String -> Text(
                        text = item,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 3.dp),
                    )

                    is AppSettingItem -> SettingItemComposable(
                        item = item,
                        activity = activity,
                        viewModel = viewModel,
                        revision = revision,
                    )
                }
            }
        }
    }
}

/**
 * Search bar composable with clear button.
 */
@Composable
fun SearchBar(
    searchInput: String,
    onSearchInputChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = searchInput,
            onValueChange = onSearchInputChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.search)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
        )
        if (searchInput.isNotEmpty()) {
            IconButton(onClick = {
                onSearchInputChange("")
                keyboardController?.hide()
            }) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.reset))
            }
        }
    }
}

/**
 * Main setting item composable that dispatches to specific item types.
 */
@Composable
fun SettingItemComposable(
    item: AppSettingItem,
    activity: ActAppSetting,
    viewModel: AppSettingViewModel,
    revision: Int,
) {
    val name = if (item.caption == 0) "" else stringResource(item.caption)

    Column(modifier = Modifier.fillMaxWidth()) {
        when (item.type) {
            SettingType.Section -> SectionItem(
                item = item,
                name = name,
                viewModel = viewModel,
            )

            SettingType.Action -> ActionItem(
                item = item,
                name = name,
                activity = activity,
            )

            SettingType.Switch -> SwitchItem(
                item = item,
                name = name,
                activity = activity,
                viewModel = viewModel,
            )

            SettingType.CheckBox -> CheckboxItem(
                item = item,
                name = name,
                activity = activity,
                viewModel = viewModel,
            )

            SettingType.EditText -> EditTextItem(
                item = item,
                name = name,
                activity = activity,
                viewModel = viewModel,
                revision = revision,
            )

            SettingType.Spinner -> SpinnerItem(
                item = item,
                name = name,
                activity = activity,
                viewModel = viewModel,
            )

            SettingType.ColorOpaque, SettingType.ColorAlpha -> ColorItem(
                item = item,
                name = name,
                activity = activity,
                viewModel = viewModel,
            )

            SettingType.Group -> GroupItem(name)

            SettingType.TextWithSelector -> TextWithSelectorItem(
                item = item,
                name = name,
                activity = activity,
                viewModel = viewModel,
            )

            SettingType.Sample -> SampleItem(
                item = item,
                activity = activity,
            )

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
                            Modifier.clickable { item.descClick.invoke(activity) }
                        } else Modifier
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Caption text composable for item labels.
 */
@Composable
fun CaptionText(
    name: String,
    item: AppSettingItem? = null,
    activity: ActAppSetting? = null,
) {
    if (name.isEmpty()) return
    
    val fontSize = if (item != null && activity != null) {
        item.captionFontSize?.invoke(activity)
    } else null
    
    val spacing = if (item != null && activity != null) {
        item.captionSpacing?.invoke(activity)
    } else null

    val lineHeight = if (spacing != null && spacing.isFinite()) {
        (14f * spacing).sp
    } else TextUnit.Unspecified

    Text(
        text = name,
        fontSize = fontSize?.sp ?: 14.sp,
        lineHeight = lineHeight,
        modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 4.dp),
        fontWeight = FontWeight.Medium,
    )
}

/**
 * Section item - navigates to a sub-section.
 */
@Composable
fun SectionItem(
    item: AppSettingItem,
    name: String,
    viewModel: AppSettingViewModel,
) {
    Button(
        onClick = {
            viewModel.setSection(item)
            viewModel.setSearchQuery("")
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = item.enabled,
    ) {
        Text(name)
    }
}

/**
 * Action item - executes an action when clicked.
 */
@Composable
fun ActionItem(
    item: AppSettingItem,
    name: String,
    activity: ActAppSetting,
) {
    Button(
        onClick = { item.action(activity) },
        modifier = Modifier.fillMaxWidth(),
        enabled = item.enabled,
    ) {
        Text(name)
    }
}

/**
 * Switch item - toggles a boolean preference.
 */
@Composable
fun SwitchItem(
    item: AppSettingItem,
    name: String,
    activity: ActAppSetting,
    viewModel: AppSettingViewModel,
) {
    val bp = item.pref.cast<BooleanPref>() ?: return
    CaptionText(name, item, activity)
    Row(
        modifier = Modifier.padding(start = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(
            checked = bp.value,
            onCheckedChange = {
                bp.value = it
                item.changed(activity)
                viewModel.refreshUi()
            },
            enabled = item.enabled,
        )
    }
}

/**
 * Checkbox item - toggles a boolean preference.
 */
@Composable
fun CheckboxItem(
    item: AppSettingItem,
    name: String,
    activity: ActAppSetting,
    viewModel: AppSettingViewModel,
) {
    val bp = item.pref.cast<BooleanPref>() ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.enabled) {
                bp.value = !bp.value
                item.changed(activity)
                viewModel.refreshUi()
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

/**
 * Edit text item - editable string or float preference.
 */
@Composable
fun EditTextItem(
    item: AppSettingItem,
    name: String,
    activity: ActAppSetting,
    viewModel: AppSettingViewModel,
    revision: Int,
) {
    CaptionText(name, item, activity)

    val pi = item.pref
    val currentText = when (pi) {
        is FloatPref -> item.fromFloat.invoke(activity, pi.value)
        is StringPref -> pi.value
        else -> ""
    }

    var text by remember(revision, item.id) { mutableStateOf(currentText) }
    val error = item.getError.invoke(activity, text)

    TextField(
        value = text,
        onValueChange = { newText ->
            val filtered = item.filter.invoke(newText)
            text = filtered
            when (pi) {
                is StringPref -> pi.value = filtered
                is FloatPref -> {
                    val fv = item.toFloat.invoke(activity, filtered)
                    if (fv.isFinite()) pi.value = fv
                    else pi.removeValue()
                }
            }
            item.changed(activity)
            viewModel.refreshUi()
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

/**
 * Spinner item - dropdown selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpinnerItem(
    item: AppSettingItem,
    name: String,
    activity: ActAppSetting,
    viewModel: AppSettingViewModel,
) {
    CaptionText(name, item, activity)

    val pi = item.pref
    if (pi is IntPref) {
        // Simple spinner with IntPref
        val options = item.spinnerArgs?.map { stringResource(it) }
            ?: item.spinnerArgsProc.invoke(activity)

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
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
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
                            item.changed(activity)
                            viewModel.refreshUi()
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
                    item.spinnerInitializer.invoke(activity, this)
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
                                    activity,
                                    this@apply,
                                    position,
                                )
                                item.changed(activity)
                                viewModel.refreshUi()
                            }
                        }
                }
            },
            modifier = Modifier.padding(start = 32.dp).fillMaxWidth(),
        )
    }
}

/**
 * Color item - color picker button.
 */
@Composable
fun ColorItem(
    item: AppSettingItem,
    name: String,
    activity: ActAppSetting,
    viewModel: AppSettingViewModel,
) {
    val ip = item.pref.cast<IntPref>() ?: return
    CaptionText(name, item, activity)

    Row(
        modifier = Modifier.padding(start = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = { viewModel.openColorPicker(item) },
            enabled = item.enabled,
        ) {
            Text(stringResource(R.string.edit))
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
            onClick = {
                ip.removeValue()
                item.changed(activity)
                viewModel.refreshUi()
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

/**
 * Group item - section header.
 */
@Composable
fun GroupItem(name: String) {
    if (name.isNotEmpty()) {
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * Text with selector item - displays text with edit/reset buttons.
 */
@Composable
fun TextWithSelectorItem(
    item: AppSettingItem,
    name: String,
    activity: ActAppSetting,
    viewModel: AppSettingViewModel,
) {
    CaptionText(name, item, activity)

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
                item.showTextView.invoke(activity, tv)
            },
            modifier = Modifier.weight(1f),
        )
        Button(onClick = { item.onClickEdit.invoke(activity) }) {
            Text(stringResource(R.string.edit))
        }
        Spacer(Modifier.width(4.dp))
        OutlinedButton(onClick = {
            item.onClickReset.invoke(activity)
            viewModel.refreshUi()
        }) {
            Text(stringResource(R.string.reset))
        }
    }
}

/**
 * Sample item - custom view.
 */
@Composable
fun SampleItem(
    item: AppSettingItem,
    activity: ActAppSetting,
) {
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
            item.sampleUpdate.invoke(activity, view)
        },
        modifier = Modifier.padding(start = 32.dp).fillMaxWidth(),
    )
}
