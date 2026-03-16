package jp.juggler.subwaytooter.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import jp.juggler.subwaytooter.util.stColorScheme

@Composable
fun StThemedContent(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalStExtendedColors provides stExtendedColors()) {
        MaterialTheme(
            colorScheme = stColorScheme(),
            content = content,
        )
    }
}
