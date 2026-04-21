package es.ariaontheplanet.quasar.nav

import android.content.Context
import android.content.Intent
import androidx.navigation.NavController
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// Thin façade over NavController so non-UI callers (ViewModels, services)
// can request navigation without pulling in androidx.navigation APIs.
// AppNavHost registers the current NavController via [bind]; issued events
// are consumed by a LaunchedEffect inside the host.
interface Navigator {
    fun navigate(route: Route)
    fun back()

    // Escape hatch for destinations that are still Intent-based.
    // Phase 5 follow-ups collapse these into routes.
    fun launchIntent(context: Context, intent: Intent)
}

class NavigatorImpl : Navigator {

    private val _events = MutableSharedFlow<NavEvent>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<NavEvent> = _events.asSharedFlow()

    @Volatile
    private var navController: NavController? = null

    fun bind(controller: NavController?) {
        navController = controller
    }

    override fun navigate(route: Route) {
        _events.tryEmit(NavEvent.Navigate(route))
    }

    override fun back() {
        _events.tryEmit(NavEvent.Back)
    }

    override fun launchIntent(context: Context, intent: Intent) {
        // Intents bypass the NavController. Add NEW_TASK when launching from a
        // non-Activity context (Services, BroadcastReceivers).
        val flagged = if (context !is android.app.Activity) {
            Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else intent
        context.startActivity(flagged)
    }
}

sealed interface NavEvent {
    data class Navigate(val route: Route) : NavEvent
    data object Back : NavEvent
}
