package jp.juggler.subwaytooter.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.util.stColorScheme

/**
 * Common themed screen wrapper.
 * Provides MaterialTheme + Scaffold + TopAppBar with back navigation.
 * Automatically observes the UI theme preference — no colorScheme parameter needed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StScreen(
    title: String = "",
    onBack: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    MaterialTheme(colorScheme = stColorScheme()) {
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
                    scrollBehavior = scrollBehavior,
                )
            },
            content = content,
        )
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
        content()
    }
}
