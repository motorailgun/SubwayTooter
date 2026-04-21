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
        // Settings-style screens historically set RESULT_OK on back so callers
        // refresh. Default to that; routes that need something else can change it.
        setResult(RESULT_OK)
        val start = Route.decode(intent?.getStringExtra(Route.EXTRA_ROUTE_JSON))
            ?: Route.ExitReasons
        setContent { AppNavHost(startDestination = start) }
    }

    companion object {
        fun createIntent(context: Context, route: Route): Intent =
            Intent(context, RootActivity::class.java).apply {
                putExtra(Route.EXTRA_ROUTE_JSON, Route.encode(route))
            }
    }
}
