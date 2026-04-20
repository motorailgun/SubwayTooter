package es.ariaontheplanet.quasar.util

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

fun <T : Any?> ComponentActivity.collectOnLifeCycle(
    flow: Flow<T>,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    block: suspend (T) -> Unit,
) = lifecycleScope.launch {
    lifecycle.repeatOnLifecycle(state = state) {
        flow.collect { block(it) }
    }
}
