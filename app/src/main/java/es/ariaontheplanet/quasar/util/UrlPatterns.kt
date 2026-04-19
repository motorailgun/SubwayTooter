package es.ariaontheplanet.quasar.util

/**
 * Regex to detect GIF URLs (used by ActMediaViewer).
 */
val reUrlGif by lazy {
    """\.gif(?:\z|\?|#)"""
        .toRegex(RegexOption.IGNORE_CASE)
}
