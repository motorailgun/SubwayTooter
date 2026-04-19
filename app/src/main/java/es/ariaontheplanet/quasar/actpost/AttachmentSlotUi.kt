package es.ariaontheplanet.quasar.actpost

import es.ariaontheplanet.quasar.R

data class AttachmentSlotUi(
    val visible: Boolean = false,
    val previewUrl: String? = null,
    val fallbackIconRes: Int = R.drawable.ic_clip,
)
