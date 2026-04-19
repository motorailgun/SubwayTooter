package es.ariaontheplanet.quasar.emoji

import es.ariaontheplanet.quasar.emoji.CustomEmoji

sealed interface PickerItem {
    val key: String
}

data class PickerItemUnicode(
    val unicodeEmoji: UnicodeEmoji,
    val skinTone: UnicodeEmoji? = null,
) : PickerItem {
    override val key: String get() = "u:${unicodeEmoji.unifiedCode}:${skinTone?.unifiedCode}"
    val emoji: UnicodeEmoji get() = skinTone ?: unicodeEmoji
}

data class PickerItemCustom(
    val customEmoji: CustomEmoji
) : PickerItem {
    override val key: String get() = "c:${customEmoji.shortcode}"
}

data class SkinTone(
    val codeInt: Int
) {
    val code: String = StringBuilder().apply { appendCodePoint(codeInt) }.toString()
}
