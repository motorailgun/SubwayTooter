package es.ariaontheplanet.quasar.nav

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Type-safe routes for Navigation-Compose. Each Activity destination will be
// replaced (or wrapped) by a composable<Route.X> in AppNavHost. Activities that
// are not yet extracted stay Intent-launched via Navigator.launchIntent.
//
// The sealed interface is @Serializable so kotlinx.serialization can round-trip
// any route (including parameterized ones) through an Intent extra.
@Serializable
sealed interface Route {

    @Serializable data object AppSettings : Route
    @Serializable data object ColumnList : Route
    @Serializable data object DrawableList : Route
    @Serializable data object ExitReasons : Route
    @Serializable data object FavMute : Route
    @Serializable data object HighlightWordList : Route
    @Serializable data object MutedApp : Route
    @Serializable data object MutedPseudoAccount : Route
    @Serializable data object MutedWord : Route
    @Serializable data object OssLicense : Route
    @Serializable data object About : Route
    @Serializable data object PushMessageList : Route
    @Serializable data object Text : Route

    @Serializable data class AccountSettings(val accountDbId: Long) : Route
    @Serializable data class Alert(val title: String, val message: String) : Route
    @Serializable data class HighlightWordEdit(
        val itemId: Long = -1L,
        val initialText: String = "",
    ) : Route
    @Serializable data class LanguageFilter(val columnIndex: Int) : Route
    @Serializable data class Nickname(
        val acctAscii: String,
        val acctPretty: String,
        val showNotificationSound: Boolean,
    ) : Route

    companion object {
        const val EXTRA_ROUTE_JSON = "nav.route_json"

        private val json = Json { ignoreUnknownKeys = true }

        fun encode(route: Route): String = json.encodeToString(serializer(), route)

        fun decode(encoded: String?): Route? = encoded?.let {
            runCatching { json.decodeFromString(serializer(), it) }.getOrNull()
        }
    }
}
