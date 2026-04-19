package es.ariaontheplanet.quasar.api.entity

import android.view.Gravity

class TootMessageHolder(
	val text: String,
	val gravity: Int = Gravity.CENTER_HORIZONTAL,
) : TimelineItem()
