package es.ariaontheplanet.quasar.nav

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import es.ariaontheplanet.quasar.actdrawablelist.DrawableListScreen
import es.ariaontheplanet.quasar.compose.StThemedContent
import es.ariaontheplanet.quasar.ui.exitReasons.ExitReasonsScreen
import es.ariaontheplanet.quasar.ui.ossLicense.OssLicenseScreen
import org.koin.core.context.GlobalContext

// Hosts the app's NavController and wires it to NavigatorImpl so non-UI
// callers can request navigation. Start destination defaults to ExitReasons
// for the Phase 5b pilot; later slices pick it from the launching Intent.
@Composable
fun AppNavHost(
    startDestination: Route = Route.ExitReasons,
) {
    val navController = rememberNavController()
    val navigator = remember { GlobalContext.get().get<NavigatorImpl>() }
    val activity = LocalActivity.current

    DisposableEffect(navController) {
        navigator.bind(navController)
        onDispose { navigator.bind(null) }
    }

    LaunchedEffect(navigator) {
        navigator.events.collect { event: NavEvent ->
            when (event) {
                is NavEvent.Navigate -> navController.navigate(event.route)
                NavEvent.Back -> if (!navController.popBackStack()) activity?.finish()
            }
        }
    }

    val popOrFinish: () -> Unit = {
        if (!navController.popBackStack()) activity?.finish()
    }

    StThemedContent {
        NavHost(
            navController = navController,
            startDestination = startDestination,
        ) {
            composable<Route.DrawableList> { DrawableListScreen() }
            composable<Route.ExitReasons> { ExitReasonsScreen() }
            composable<Route.OssLicense> { OssLicenseScreen(onClose = popOrFinish) }
        }
    }
}
