package es.ariaontheplanet.quasar.nav

import kotlinx.serialization.Serializable

// Type-safe routes for Navigation-Compose. Each Activity destination will be
// replaced (or wrapped) by a composable<Route.X> in AppNavHost. Activities that
// are not yet extracted stay Intent-launched via Navigator.launchIntent.
sealed interface Route {

    @Serializable data object AppSettings : Route
    @Serializable data object ColumnList : Route
    @Serializable data object ExitReasons : Route
    @Serializable data object OssLicense : Route
    @Serializable data object About : Route

    @Serializable data class AccountSettings(val accountDbId: Long) : Route
    @Serializable data class LanguageFilter(val columnIndex: Int) : Route

    companion object {
        const val EXTRA_START_KEY = "nav.start_route_key"

        // Parameterless-route key mapping for cross-Activity launches.
        // Parameterized routes use Navigator.navigate inside an already-running
        // RootActivity; they don't need an intent extra here.
        fun keyOf(route: Route): String? = when (route) {
            AppSettings -> "app_settings"
            ColumnList -> "column_list"
            ExitReasons -> "exit_reasons"
            OssLicense -> "oss_license"
            About -> "about"
            else -> null
        }

        fun fromKey(key: String?): Route? = when (key) {
            "app_settings" -> AppSettings
            "column_list" -> ColumnList
            "exit_reasons" -> ExitReasons
            "oss_license" -> OssLicense
            "about" -> About
            else -> null
        }
    }
}
