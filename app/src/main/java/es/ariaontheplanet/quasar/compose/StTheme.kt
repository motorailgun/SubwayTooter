package es.ariaontheplanet.quasar.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import es.ariaontheplanet.quasar.R

// Material3 ColorScheme derived from the active [UiTheme].
// Kept alongside stExtendedColors() so both theme halves read the same source.
// M3 color tokens still use the framework defaults — Phase 1c/later will
// derive seed colors from StColorTokens once a user-facing theme pref exists.
@Composable
internal fun stColorScheme(): ColorScheme = when (currentUiTheme()) {
    UiTheme.Light -> lightColorScheme()
    UiTheme.Dark, UiTheme.Mastodon -> darkColorScheme()
}

/**
 * Common themed screen wrapper.
 * Provides MaterialTheme + extended colors + Scaffold + TopAppBar with back navigation.
 * Automatically observes the UI theme preference — no colorScheme parameter needed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StScreen(
    title: String = "",
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    MaterialTheme(colorScheme = stColorScheme()) {
        CompositionLocalProvider(LocalStExtendedColors provides stExtendedColors()) {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    TopAppBar(
                        title = { Text(title) },
                        navigationIcon = {
                            if (onBack != null) {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = stringResource(R.string.close),
                                    )
                                }
                            }
                        },
                        actions = actions,
                        scrollBehavior = scrollBehavior,
                    )
                },
                content = content,
            )
        }
    }
}

/**
 * Themed content without TopAppBar (for dialogs or custom headers).
 * Automatically observes the UI theme preference.
 */
@Composable
fun StThemedContent(
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = stColorScheme()) {
        CompositionLocalProvider(LocalStExtendedColors provides stExtendedColors()) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewStScreen() {
    StScreen(
        title = "Preview Title",
        onBack = {},
        content = { Text("Content") }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewStThemedContent() {
    StThemedContent {
        Text("Content")
    }
}
