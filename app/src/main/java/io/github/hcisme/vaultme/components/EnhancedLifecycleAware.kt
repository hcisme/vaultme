package io.github.hcisme.vaultme.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * 增强版生命周期监听器，提供更具体的回调
 */
@Composable
fun EnhancedLifecycleAware(
    onCreated: (() -> Unit)? = null,
    onStarted: (() -> Unit)? = null,
    onResumed: (() -> Unit)? = null,
    onPaused: (() -> Unit)? = null,
    onStopped: (() -> Unit)? = null,
    onDestroyed: (() -> Unit)? = null,
    onAnyEvent: ((Lifecycle.Event) -> Unit)? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            onAnyEvent?.invoke(event)

            when (event) {
                Lifecycle.Event.ON_CREATE -> onCreated?.invoke()
                Lifecycle.Event.ON_START -> onStarted?.invoke()
                Lifecycle.Event.ON_RESUME -> onResumed?.invoke()
                Lifecycle.Event.ON_PAUSE -> onPaused?.invoke()
                Lifecycle.Event.ON_STOP -> onStopped?.invoke()
                Lifecycle.Event.ON_DESTROY -> onDestroyed?.invoke()
                Lifecycle.Event.ON_ANY -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
