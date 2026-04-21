package es.ariaontheplanet.quasar.ui.alert

import android.content.Context
import android.content.Intent
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
import es.ariaontheplanet.quasar.RootActivity
import es.ariaontheplanet.quasar.nav.Route
import jp.juggler.util.data.encodePercent

// Builds a PendingIntent-friendly launch for the Alert route.
// `tag` becomes the Intent data URI so multiple concurrent alerts stay
// distinct under PendingIntent.filterEquals.
fun Context.intentActAlert(
    tag: String,
    message: String,
    title: String,
): Intent = RootActivity.createIntent(this, Route.Alert(title = title, message = message)).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK
    data = "app://error/${tag.encodePercent()}".toUri()
}

@Composable
fun AlertScreen(@Suppress("UNUSED_PARAMETER") title: String, message: String) {
    SelectionContainer {
        Text(
            text = message,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
        )
    }
}
