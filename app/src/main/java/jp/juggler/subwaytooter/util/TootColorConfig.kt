package jp.juggler.subwaytooter.util

/**
 * Toot background colors by visibility type.
 * Set by ActMainStyle.reloadColors(), read by StatusComposables.
 */
object TootColorConfig {
    var toot_color_unlisted: Int = 0
    var toot_color_follower: Int = 0
    var toot_color_direct_user: Int = 0
    var toot_color_direct_me: Int = 0
}
