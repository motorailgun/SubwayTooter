package jp.juggler.subwaytooter.actpost

import jp.juggler.subwaytooter.R

data class AttachmentSlotUi(
    val visible: Boolean = false,
    val previewUrl: String? = null,
    val fallbackIconRes: Int = R.drawable.ic_clip,
)
