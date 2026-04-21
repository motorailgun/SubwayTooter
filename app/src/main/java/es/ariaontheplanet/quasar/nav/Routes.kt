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

    // Parameterless Activities for now; parameterized variants follow as each
    // Activity is migrated off its raw Intent construction.
}
