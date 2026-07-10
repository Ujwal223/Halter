// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.savedstate.SavedStateRegistryOwner
import com.ujwal.halter.ui.theme.HalterTheme
import kotlin.math.abs

/**
 * Floating session timer chip. Only visible on the app whose session is active.
 * Long-press to drag; the chip can be positioned anywhere on screen edges.
 */
class HoverOverlay(private val context: Context) {

    private var currentView: View? = null
    private var currentParams: WindowManager.LayoutParams? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var windowManager: WindowManager? = null

    // Drag state — all managed at the FrameLayout level, NOT inside Compose.
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialX = 0
    private var initialY = 0
    private var isDragging = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var longPressTriggered = false
    private var longPressRunnable: Runnable? = null

    private var scrollCount by mutableIntStateOf(0)
    private var remainingSeconds by mutableIntStateOf(0)
    private var showScrolls by mutableStateOf(false)
    private var appName by mutableStateOf("")
    private var scrollLimit by mutableIntStateOf(0)
    private var boundPackage by mutableStateOf<String?>(null)
    private var chipVisible by mutableStateOf(true)

    /**
     * The package currently in the foreground. Set externally by HalterAccessibilityService
     * whenever the foreground app changes. The chip is hidden if [foregroundPackage] != [boundPackage].
     */
    var foregroundPackage: String? = null

    fun updateData(
        packageName: String,
        scrolls: Int,
        remainingSeconds: Int,
        showScrolls: Boolean,
        name: String,
        limitScrolls: Int = 0
    ) {
        if (boundPackage != packageName) {
            boundPackage = packageName
            chipVisible = true
        }
        scrollCount = scrolls
        this.remainingSeconds = remainingSeconds
        this.showScrolls = showScrolls
        appName = name
        scrollLimit = limitScrolls
        if (remainingSeconds > 0 || remainingSeconds == -1) chipVisible = true
    }

    fun hide() {
        currentView?.let {
            runCatching { windowManager?.removeView(it) }
        }
        cancelLongPress()
        lifecycleOwner?.onDestroy()
        currentView = null
        currentParams = null
        windowManager = null
        lifecycleOwner = null
        boundPackage = null
        chipVisible = true
    }

    fun show() {
        if (!Settings.canDrawOverlays(context)) return

        // B1 Fix: never show the chip on a different app or on Halter itself.
        val currentForeground = foregroundPackage
        val bound = boundPackage
        if (bound == null || (currentForeground != null && currentForeground != bound)) {
            if (currentView != null) hide()
            return
        }

        // Only show the chip when there is an active time limit with time remaining.
        if (remainingSeconds <= 0 || bound == null) {
            if (currentView != null) hide()
            return
        }
        if (currentView != null) return
        try {
            val owner = OverlayLifecycleOwner().apply {
                onCreate()
                onStart()
                onResume()
            }
            lifecycleOwner = owner

            // B2 Fix: wrap ComposeView in a DraggableFrameLayout that intercepts touch events
            // BEFORE Compose can consume them. ComposeView.setOnTouchListener() fires AFTER
            // Compose's internal pointer input, so long-press never triggered. By overriding
            // dispatchTouchEvent in the parent FrameLayout we get first-access to raw events.
            val composeView = ComposeView(context)
            wireLifecycleOwners(composeView, owner)
            composeView.setContent {
                HalterTheme(com.ujwal.halter.settings.HalterSettings()) {
                    val alpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (chipVisible) 1f else 0f,
                        animationSpec = tween(220),
                        label = "chipAlpha"
                    )
                    val showChip = remainingSeconds > 0
                    if (chipVisible && showChip) {
                        Box(Modifier.alpha(alpha)) {
                            OverlayChip(
                                scrollCount = scrollCount,
                                remainingSeconds = remainingSeconds,
                                showScrolls = showScrolls,
                                limitScrolls = scrollLimit
                            )
                        }
                    }
                }
            }

            val container = object : FrameLayout(context) {
                override fun dispatchTouchEvent(event: MotionEvent): Boolean {
                    // Handle drag at the container level before Compose sees the event.
                    val handled = this@HoverOverlay.onTouch(event)
                    // While dragging, consume all events so Compose doesn't interfere.
                    if (isDragging) return true
                    return if (handled) true else super.dispatchTouchEvent(event)
                }
            }
            container.addView(composeView)
            wireLifecycleOwners(container, owner)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = savedX.coerceAtLeast(0)
                y = savedY.coerceAtLeast(50)
            }

            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            currentView = container
            currentParams = params
            windowManager?.addView(container, params)
        } catch (e: Exception) {
            android.util.Log.e("HoverOverlay", "Failed to show", e)
        }
    }

    /** Refresh visibility after data changes (e.g. timer tick). */
    fun refreshVisibility() {
        // B1 Fix: also hide if the chip's app is not the current foreground.
        val currentForeground = foregroundPackage
        val bound = boundPackage
        val isTimeLimit = remainingSeconds >= 0
        if ((isTimeLimit && remainingSeconds <= 0) || bound == null ||
            (currentForeground != null && currentForeground != bound)
        ) {
            hide()
        }
    }

    private fun wireLifecycleOwners(view: View, owner: OverlayLifecycleOwner) {
        try {
            Class.forName("androidx.lifecycle.ViewTreeLifecycleOwner")
                .getMethod("set", View::class.java, androidx.lifecycle.LifecycleOwner::class.java)
                .invoke(null, view, owner)
        } catch (e: Exception) {
            android.util.Log.w("HoverOverlay", "Failed to set ViewTreeLifecycleOwner", e)
        }
        try {
            Class.forName("androidx.savedstate.ViewTreeSavedStateRegistryOwner")
                .getMethod(
                    "set",
                    View::class.java,
                    SavedStateRegistryOwner::class.java
                )
                .invoke(null, view, owner)
        } catch (e: Exception) {
            android.util.Log.w("HoverOverlay", "Failed to set ViewTreeSavedStateRegistryOwner", e)
        }
        try {
            Class.forName("androidx.lifecycle.ViewTreeViewModelStoreOwner")
                .getMethod(
                    "set",
                    View::class.java,
                    androidx.lifecycle.ViewModelStoreOwner::class.java
                )
                .invoke(null, view, owner)
        } catch (e: Exception) {
            android.util.Log.w("HoverOverlay", "Failed to set ViewTreeViewModelStoreOwner", e)
        }
    }

    private fun onTouch(event: MotionEvent): Boolean {
        val params = currentParams ?: return false
        val view = currentView ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                initialX = params.x
                initialY = params.y
                isDragging = false
                longPressTriggered = false
                scheduleLongPress()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dxFloat = event.rawX - initialTouchX
                val dyFloat = event.rawY - initialTouchY
                if (!longPressTriggered && (abs(dxFloat) > touchSlop || abs(dyFloat) > touchSlop)) {
                    cancelLongPress()
                }
                if (!longPressTriggered) return true
                isDragging = true
                params.x = initialX + (event.rawX - initialTouchX).toInt()
                params.y = initialY + (event.rawY - initialTouchY).toInt()
                windowManager?.updateViewLayout(view, params)
                return true
            }
            MotionEvent.ACTION_UP -> {
                cancelLongPress()
                if (isDragging) {
                    // Snap to nearest edge on release
                    snapToEdge(params)
                    savedX = params.x
                    savedY = params.y
                    windowManager?.updateViewLayout(view, params)
                    isDragging = false
                    longPressTriggered = false
                    return true
                }
                // Short tap or long-press-without-drag: do nothing (chip is not dismissible by user)
                longPressTriggered = false
                return false
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelLongPress()
                isDragging = false
            }
        }
        return false
    }

    private fun scheduleLongPress() {
        cancelLongPress()
        longPressRunnable = Runnable {
            longPressTriggered = true
            // Haptic feedback so users know drag mode is now active
            currentView?.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        }.also {
            mainHandler.postDelayed(it, longPressTimeout)
        }
    }

    private fun cancelLongPress() {
        longPressRunnable?.let(mainHandler::removeCallbacks)
        longPressRunnable = null
    }

    private fun snapToEdge(params: WindowManager.LayoutParams) {
        val display = context.resources.displayMetrics
        val screenWidth = display.widthPixels
        // Use measured chip width if available; fall back to 200px
        val chipWidth = currentView?.width?.takeIf { it > 0 } ?: 200
        params.x = if (params.x + chipWidth / 2 < screenWidth / 2) 0 else screenWidth - chipWidth
        params.y = params.y.coerceIn(50, display.heightPixels - 150)
    }

    companion object {
        private var savedX = 0
        private var savedY = 200
    }
}

@Composable
private fun OverlayChip(
    scrollCount: Int,
    remainingSeconds: Int,
    showScrolls: Boolean,
    limitScrolls: Int
) {
    val isTimeLimit = remainingSeconds >= 0
    val timeLabel = if (isTimeLimit) {
        when {
            remainingSeconds >= 3600 -> {
                val hours = remainingSeconds / 3600
                val mins = (remainingSeconds % 3600) / 60
                val secs = remainingSeconds % 60
                buildString {
                    append(if (hours == 1) "1h" else "${hours}h")
                    if (mins > 0) append(" ${mins}m")
                    if (secs > 0 && mins == 0) append(" ${secs}s")
                }
            }
            remainingSeconds >= 60 -> {
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60
                if (seconds == 0) "${minutes}m" else "${minutes}m ${seconds}s"
            }
            remainingSeconds > 0 -> "${remainingSeconds}s"
            else -> "0s"
        }
    } else {
        ""
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp,
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        modifier = Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            shape = RoundedCornerShape(24.dp)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isTimeLimit) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.Timer,
                        contentDescription = "Remaining time",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        timeLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
