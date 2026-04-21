package es.ariaontheplanet.quasar

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import es.ariaontheplanet.quasar.nav.AppNavHost
import es.ariaontheplanet.quasar.nav.Route

// Single-Activity host for Compose routes. Callers launch it via
// [createIntent] with a target route; each Activity that's migrated off
// its raw Intent adds a composable<Route.X> entry to AppNavHost and loses
// its own Activity + Manifest entry.
class RootActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App1.setActivityTheme(this)
        val start = Route.fromKey(intent?.getStringExtra(Route.EXTRA_START_KEY))
            ?: Route.ExitReasons
        setContent { AppNavHost(startDestination = start) }
    }

    companion object {
        fun createIntent(context: Context, route: Route): Intent =
            Intent(context, RootActivity::class.java).apply {
                Route.keyOf(route)?.let { putExtra(Route.EXTRA_START_KEY, it) }
            }
    }
}
