package com.ujwal.halter.service

import android.content.Context
import android.graphics.PixelFormat
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.lifecycle.ViewModelStore
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner


private class FeedGuardOverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val viewModelStore = ViewModelStore()
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun performRestore() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }
}

class FeedGuardOverlay(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private var overlayOwner: FeedGuardOverlayLifecycleOwner? = null


    fun isShowing() = composeView != null

    fun show(totalSeconds: Int) {
        if (isShowing()) return
        if (!android.provider.Settings.canDrawOverlays(context)) return

        val owner = FeedGuardOverlayLifecycleOwner().also { it.performRestore() }
        overlayOwner = owner

        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)

            setContent {
                val secondsLeft = remember { mutableStateOf(totalSeconds) }
                FeedGuardWarningUI(secondsLeft = secondsLeft.value)
            }
        }
        composeView = view

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(view, params)
    }

    fun updateSecondsLeft(seconds: Int) {
        composeView?.setContent { FeedGuardWarningUI(secondsLeft = seconds) }
    }

    fun hide() {
        composeView?.let { windowManager.removeView(it) }
        composeView = null
        overlayOwner?.destroy()
        overlayOwner = null
    }
}

@Composable
private fun FeedGuardWarningUI(secondsLeft: Int) {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.TopCenter
    ) {
        androidx.compose.material3.Surface(
            tonalElevation = 8.dp,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            modifier = androidx.compose.ui.Modifier.padding(top = 48.dp)
        ) {
            androidx.compose.material3.Text(
                text = "Leave this feed — $secondsLeft s",
                modifier = androidx.compose.ui.Modifier.padding(16.dp)
            )
        }
    }
}
