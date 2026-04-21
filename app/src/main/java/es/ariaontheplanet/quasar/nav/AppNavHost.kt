package es.ariaontheplanet.quasar.nav

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import es.ariaontheplanet.quasar.actaccountsetting.AccountSettingRoute
import es.ariaontheplanet.quasar.actdrawablelist.DrawableListScreen
import es.ariaontheplanet.quasar.actfavmute.FavMuteScreen
import es.ariaontheplanet.quasar.actmutedapp.MutedAppScreen
import es.ariaontheplanet.quasar.actmutedpseudoaccount.MutedPseudoAccountScreen
import es.ariaontheplanet.quasar.actmutedword.MutedWordScreen
import es.ariaontheplanet.quasar.compose.StThemedContent
import es.ariaontheplanet.quasar.ui.about.AboutScreen
import es.ariaontheplanet.quasar.ui.alert.AlertScreen
import es.ariaontheplanet.quasar.ui.exitReasons.ExitReasonsScreen
import es.ariaontheplanet.quasar.ui.highlightWord.HighlightWordListScreen
import es.ariaontheplanet.quasar.ui.languageFilter.LanguageFilterScreen
import es.ariaontheplanet.quasar.ui.nickname.NicknameScreen
import es.ariaontheplanet.quasar.ui.ossLicense.OssLicenseScreen
import es.ariaontheplanet.quasar.ui.pushMessageList.PushMessageListScreen
import androidx.navigation.toRoute
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
            composable<Route.About> { AboutScreen() }
            composable<Route.AccountSettings> { entry ->
                val r = entry.toRoute<Route.AccountSettings>()
                AccountSettingRoute(accountDbId = r.accountDbId, onBack = popOrFinish)
            }
            composable<Route.Alert> { entry ->
                val r = entry.toRoute<Route.Alert>()
                AlertScreen(title = r.title, message = r.message)
            }
            composable<Route.DrawableList> { DrawableListScreen() }
            composable<Route.ExitReasons> { ExitReasonsScreen() }
            composable<Route.FavMute> { FavMuteScreen() }
            composable<Route.HighlightWordList> { HighlightWordListScreen() }
            composable<Route.LanguageFilter> { entry ->
                val r = entry.toRoute<Route.LanguageFilter>()
                LanguageFilterScreen(columnIndex = r.columnIndex)
            }
            composable<Route.MutedApp> { MutedAppScreen() }
            composable<Route.MutedPseudoAccount> { MutedPseudoAccountScreen() }
            composable<Route.MutedWord> { MutedWordScreen() }
            composable<Route.Nickname> { entry ->
                val r = entry.toRoute<Route.Nickname>()
                NicknameScreen(
                    acctAscii = r.acctAscii,
                    acctPretty = r.acctPretty,
                    showNotificationSound = r.showNotificationSound,
                )
            }
            composable<Route.OssLicense> { OssLicenseScreen(onClose = popOrFinish) }
            composable<Route.PushMessageList> { PushMessageListScreen() }
        }
    }
}
