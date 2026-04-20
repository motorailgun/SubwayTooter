package es.ariaontheplanet.quasar.util

import androidx.activity.ComponentActivity

fun ComponentActivity.fireBackPressed() =
    onBackPressedDispatcher.onBackPressed()
