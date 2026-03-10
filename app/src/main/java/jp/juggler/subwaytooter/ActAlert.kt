package jp.juggler.subwaytooter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import jp.juggler.subwaytooter.compose.StScreen
import jp.juggler.util.data.encodePercent
import jp.juggler.util.data.notEmpty

class ActAlert : ComponentActivity() {
    companion object {
        private const val EXTRA_MESSAGE = "message"
        private const val EXTRA_TITLE = "title"

        fun Context.intentActAlert(
            tag: String,
            message: String,
            title: String,
        ) = Intent(this, ActAlert::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            data = "app://error/${tag.encodePercent()}".toUri()
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_TITLE, title)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val title = intent?.getStringExtra(EXTRA_TITLE).notEmpty() ?: ""
        val message = intent?.getStringExtra(EXTRA_MESSAGE).notEmpty() ?: ""
        super.onCreate(savedInstanceState)
        App1.setActivityTheme(this)
        setContent {
            AlertScreen(title, message)
        }
    }

    @Composable
    private fun AlertScreen(
        title: String,
        message: String,
    ) {
        StScreen(
            title = title,
            onBack = { finish() },
        ) { innerPadding ->
            SelectionContainer {
                Text(
                    text = message,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                )
            }
        }
    }
}
