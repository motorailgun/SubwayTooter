package es.ariaontheplanet.quasar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import es.ariaontheplanet.quasar.nav.AppNavHost

// Single-Activity host for Compose routes. Phase 5b seeds it with one pilot
// destination; each subsequent Activity migration adds a composable<Route.X>
// entry to AppNavHost and deletes its Activity + Manifest entry.
class RootActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App1.setActivityTheme(this)
        setContent { AppNavHost() }
    }
}
