// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.service

import android.content.Context
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.ujwal.halter.data.JournalEntry
import com.ujwal.halter.data.JournalReason
import com.ujwal.halter.data.LimitType
import com.ujwal.halter.data.MonitoredApp
import com.ujwal.halter.settings.HalterSettings
import com.ujwal.halter.ui.components.LazyColumnScrollbar
import com.ujwal.halter.ui.theme.HalterTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import android.view.HapticFeedbackConstants
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

internal class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    fun onCreate() { savedStateController.performAttach(); savedStateController.performRestore(null); lifecycleRegistry.currentState = Lifecycle.State.CREATED }
    fun onStart() { lifecycleRegistry.currentState = Lifecycle.State.STARTED }
    fun onResume() { lifecycleRegistry.currentState = Lifecycle.State.RESUMED }
    fun onDestroy() { lifecycleRegistry.currentState = Lifecycle.State.DESTROYED; viewModelStore.clear() }
}

class OverlayController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onJournalEntry: ((JournalEntry) -> Unit)? = null
) {
    private var currentView: View? = null
    private var currentLifecycleOwner: OverlayLifecycleOwner? = null
    private var currentSettings: HalterSettings? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioManager: AudioManager? by lazy { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    /** Whether the currently-showing overlay is interactive (has text fields / captures input).
     *  The accessibility service checks this to avoid killing the overlay when the IME opens. */
    var isShowingInteractiveOverlay: Boolean = false
        private set

    /** Whether ANY overlay is currently displayed. The accessibility service uses this
     *  to avoid killing overlays on system UI / IME window-state changes. */
    var isShowingAnyOverlay: Boolean = false
        private set

    private var showingBlockKey: String? = null

    fun canShowOverlays(): Boolean = Settings.canDrawOverlays(context)

    constructor(context: Context) : this(context, context.getSystemService(WindowManager::class.java), null)

    fun showBlock(decision: BlockDecision, settings: HalterSettings? = null, onDismiss: () -> Unit = { hide() }) {
        if (!Settings.canDrawOverlays(context)) return
        val blockKey = "${decision.appName}|${decision.reason}"
        if (isShowingAnyOverlay && showingBlockKey == blockKey) return
        showingBlockKey = blockKey
        currentSettings = settings
        isShowingAnyOverlay = true
        showCompose(focusable = false) {
            val s = remember { currentSettings ?: HalterSettings() }
            HalterTheme(s) { BlockOverlay(decision = decision, onDismiss = onDismiss) }
        }
    }

    fun showBreathingThenPicker(
        app: MonitoredApp, settings: HalterSettings,
        appUsageTodayMinutes: Int,
        totalUsageTodayMinutes: Int,
        onSessionChosen: (Int?, LimitType, String?) -> Unit
    ) {
        if (!Settings.canDrawOverlays(context)) return
        currentSettings = settings
        isShowingInteractiveOverlay = true
        isShowingAnyOverlay = true
        showCompose(focusable = true) {
            HalterTheme(settings) {
                var breathingDone by remember { mutableStateOf(false) }
                var reflectionText by remember { mutableStateOf("") }
                if (!breathingDone) {
                    BreathingOverlay(
                        settings = settings,
                        appUsageTodayMinutes = appUsageTodayMinutes,
                        totalUsageTodayMinutes = totalUsageTodayMinutes,
                        dailyLimitMinutes = app.dailyTimeLimitMinutes,
                        onDone = { breathingDone = true },
                        onSkip = { breathingDone = true }
                    )
                } else {
                    SessionPickerOverlay(
                        app = app, settings = settings,
                        reflectionText = reflectionText,
                        onReflectionChange = { reflectionText = it }
                    ) { limit, type ->
                        onSessionChosen(limit, type, reflectionText.ifBlank { null })
                        hide()
                    }
                }
            }
        }
    }

    fun showDeepFocusEndPrompt(
        settings: HalterSettings,
        sessionMinutes: Int,
        earlyEndMinutes: Int,
        onReasonProvided: (String) -> Unit
    ) {
        if (!Settings.canDrawOverlays(context)) return
        currentSettings = settings
        isShowingInteractiveOverlay = true
        isShowingAnyOverlay = true
        showCompose(focusable = true) {
            HalterTheme(settings) {
                var breathingDone by remember { mutableStateOf(false) }
                var reasonText by remember { mutableStateOf("") }
                if (!breathingDone) {
                    BreathingOverlay(
                        settings = settings,
                        appUsageTodayMinutes = 0,
                        totalUsageTodayMinutes = 0,
                        dailyLimitMinutes = null,
                        onDone = { breathingDone = true },
                        onSkip = { breathingDone = true }
                    )
                } else {
                    DeepFocusExitOverlay(
                        sessionMinutes = sessionMinutes,
                        earlyEndMinutes = earlyEndMinutes,
                        reasonText = reasonText,
                        onReasonChange = { reasonText = it },
                        onConfirm = {
                            if (reasonText.isNotBlank()) {
                                onReasonProvided(reasonText.trim())
                                hide()
                            }
                        }
                    )
                }
            }
        }
    }

    fun showWarningOverlay(
        title: String,
        message: String,
        warningSeconds: Int = 3,
        onTimeout: () -> Unit
    ) {
        if (!Settings.canDrawOverlays(context)) {
            onTimeout()
            return
        }
        currentSettings = HalterSettings()
        isShowingInteractiveOverlay = false
        isShowingAnyOverlay = true
        showCompose(focusable = false) {
            val settings = currentSettings ?: HalterSettings()
            HalterTheme(settings) {
                WarningOverlay(
                    title = title,
                    message = message,
                    warningSeconds = warningSeconds,
                    onTimeout = onTimeout
                )
            }
        }
    }

    private fun showCompose(focusable: Boolean, content: @Composable () -> Unit) {
        hide()
        try {
            val owner = OverlayLifecycleOwner()
            owner.onCreate(); owner.onStart(); owner.onResume()

            // ── IMPORTANT: wire lifecycle owners BEFORE setContent ──
            // If setContent runs first, Compose starts without a LifecycleOwner and only
            // recomposes on touch events (freezing timers/animations until clicked).
            val view = ComposeView(context)
            try { Class.forName("androidx.lifecycle.ViewTreeLifecycleOwner").getMethod("set", View::class.java, LifecycleOwner::class.java).invoke(null, view, owner) }
            catch (_: Exception) { android.util.Log.w("OverlayController", "ViewTreeLifecycleOwner.set not available") }
            try { Class.forName("androidx.savedstate.ViewTreeSavedStateRegistryOwner").getMethod("set", View::class.java, SavedStateRegistryOwner::class.java).invoke(null, view, owner) }
            catch (_: Exception) { android.util.Log.w("OverlayController", "ViewTreeSavedStateRegistryOwner.set not available") }
            try { Class.forName("androidx.lifecycle.ViewTreeViewModelStoreOwner").getMethod("set", View::class.java, ViewModelStoreOwner::class.java).invoke(null, view, owner) }
            catch (_: Exception) { android.util.Log.w("OverlayController", "ViewTreeViewModelStoreOwner.set not available") }
            // Now safe to attach content — Compose will find the lifecycle owner immediately
            view.setContent(content)

            val flags = if (focusable) {
                // Focusable overlay: allow text input + IME, but don't let touches outside dismiss it
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            } else {
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
                // Soft input mode: adjust to avoid IME overlap
                if (focusable) {
                    softInputMode = android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                }
            }
            // Request audio focus to pause media playback in blocked app
            if (!focusable) {
                requestAudioFocus()
            }
            currentView = view; currentLifecycleOwner = owner
            windowManager.addView(view, params)
        } catch (e: Exception) {
            currentView = null; currentLifecycleOwner = null
            android.util.Log.e("OverlayController", "Failed to show overlay", e)
        }
    }

    fun hide() {
        currentView?.let { runCatching { windowManager.removeView(it) } }
        currentLifecycleOwner?.onDestroy()
        abandonAudioFocus()
        currentView = null; currentLifecycleOwner = null
        isShowingInteractiveOverlay = false
        isShowingAnyOverlay = false
        showingBlockKey = null
    }

    private fun requestAudioFocus() {
        audioManager?.let { am ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val attrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                        .setAudioAttributes(attrs)
                        .setOnAudioFocusChangeListener { }
                        .build()
                    audioFocusRequest = request
                    am.requestAudioFocus(request)
                } else {
                    @Suppress("DEPRECATION")
                    am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                }
            } catch (_: Exception) { }
        }
    }

    private fun abandonAudioFocus() {
        audioManager?.let { am ->
            try {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            } catch (_: Exception) { }
        }
    }
}

// ═══════════════════ Overlay Composables ═══════════════════

// ── Breathing Overlay (phase-locked inhale / hold / exhale animation) ──

@Composable
private fun BreathingOverlay(
    settings: HalterSettings,
    appUsageTodayMinutes: Int,
    totalUsageTodayMinutes: Int,
    dailyLimitMinutes: Int?,
    onDone: () -> Unit,
    onSkip: () -> Unit
) {
    val baseDuration = settings.breathingTotalDurationSeconds.coerceAtLeast(4)
    val usageAddition = (totalUsageTodayMinutes / 15).coerceIn(0, 15)
    val limitAddition = if (dailyLimitMinutes != null && dailyLimitMinutes > 0) {
        val ratio = appUsageTodayMinutes.toFloat() / dailyLimitMinutes.toFloat()
        when {
            ratio >= 0.9f -> 20
            ratio >= 0.75f -> 10
            else -> 0
        }
    } else {
        0
    }
    val totalDuration = baseDuration + usageAddition + limitAddition

    var phaseIndex by remember { mutableIntStateOf(0) }
    var secondsRemaining by remember { mutableIntStateOf(totalDuration) }
    val view = LocalView.current

    val phases = remember(totalDuration) { buildBreathingPhases(totalDuration) }
    if (phases.isEmpty()) return

    val currentPhase = phases[phaseIndex % phases.size]
    val scale = remember { Animatable(phases.first().startScale) }
    val clickScope = rememberCoroutineScope()
    val donateVisibleState = remember { mutableStateOf(false) }
    val donateScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        donateScope.launch {
            try {
                val recentMillis = totalUsageTodayMinutes * 60_000L
                if (com.ujwal.halter.utils.DonationManager.shouldShowNow(view.context, recentMillis)) {
                    donateVisibleState.value = true
                }
            } catch (_: Exception) { }
        }
    }

    // Phase-specific haptics so the user can use the breathing gate eyes-closed.
    // Index 0 = Breathe In, 1 = Hold (in), 2 = Breathe Out, 3 = Hold (out)
    LaunchedEffect(phaseIndex) {
        if (settings.hapticsEnabled) {
            view.isHapticFeedbackEnabled = true
            val constant = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                when (phaseIndex % phases.size) {
                    0 -> HapticFeedbackConstants.GESTURE_START
                    1 -> HapticFeedbackConstants.LONG_PRESS
                    2 -> HapticFeedbackConstants.GESTURE_END
                    else -> HapticFeedbackConstants.CLOCK_TICK
                }
            } else {
                when (phaseIndex % phases.size) {
                    0 -> HapticFeedbackConstants.VIRTUAL_KEY
                    1 -> HapticFeedbackConstants.LONG_PRESS
                    2 -> HapticFeedbackConstants.CLOCK_TICK
                    else -> HapticFeedbackConstants.CLOCK_TICK
                }
            }
            view.performHapticFeedback(constant)
            // Vibrator as reliable backup — overlay views may lack window focus
            val ctx = view.context
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                (ctx.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                val effect = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    when (phaseIndex % phases.size) {
                        0 -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                        1 -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                        2 -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                        else -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    VibrationEffect.createOneShot(40L, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vibrator.vibrate(effect)
            }
        }
    }

    LaunchedEffect(totalDuration) {
        var elapsedSeconds = 0
        var idx = 0
        while (elapsedSeconds < totalDuration) {
            val phase = phases[idx % phases.size]
            phaseIndex = idx
            scale.snapTo(phase.startScale)
            launch {
                scale.animateTo(
                    phase.endScale,
                    tween(durationMillis = phase.duration * 1000, easing = LinearEasing)
                )
            }
            repeat(phase.duration) {
                if (elapsedSeconds >= totalDuration) return@repeat
                secondsRemaining = (totalDuration - elapsedSeconds).coerceAtLeast(0)
                delay(1000L)
                elapsedSeconds++
            }
            idx++
        }
        secondsRemaining = 0
        // Fire a distinct "session complete" haptic so the user knows eyes can open
        if (settings.hapticsEnabled) {
            val endConstant = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
            view.performHapticFeedback(endConstant)
        }
        onDone()
    }

    OverlaySurface(settings) {
        if (donateVisibleState.value) {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f))) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Support development", style = MaterialTheme.typography.titleSmall)
                        Text("Like Halter? Consider donating — it helps keep the app alive.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick = {
                        com.ujwal.halter.utils.DonationManager.openDonateUrl(view.context)
                        donateScope.launch { com.ujwal.halter.utils.DonationManager.recordShown(view.context) }
                        donateVisibleState.value = false
                    }) { Text("Donate") }
                    TextButton(onClick = {
                        donateScope.launch { com.ujwal.halter.utils.DonationManager.dismissForDays(view.context, 3) }
                        donateVisibleState.value = false
                    }) { Text("Close") }
                }
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!scale.isRunning) {
                        clickScope.launch {
                            scale.animateTo(currentPhase.endScale, tween(durationMillis = 250, easing = LinearEasing))
                        }
                    }
                }
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val auraRotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(12000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            // Outer glass ring
            Box(
                Modifier
                    .size(220.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        rotationZ = auraRotation
                    }
                    .border(
                        width = 1.5.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Soft breathing gradient background
            Box(
                Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        scaleX = scale.value * 0.95f
                        scaleY = scale.value * 0.95f
                        rotationZ = -auraRotation
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Dynamic breathing flower structure
            val primaryColor = MaterialTheme.colorScheme.primary
            val tertiaryColor = MaterialTheme.colorScheme.tertiary
            Canvas(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        rotationZ = auraRotation * 0.5f
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension / 3f
                val petalColor = primaryColor.copy(alpha = 0.45f)
                val secondaryPetalColor = tertiaryColor.copy(alpha = 0.3f)

                // 6 overlapping petals
                for (i in 0 until 6) {
                    val angle = i * (360f / 6)
                    rotate(angle, pivot = center) {
                        drawOval(
                            color = if (i % 2 == 0) petalColor else secondaryPetalColor,
                            topLeft = Offset(center.x - baseRadius * 0.6f, center.y - baseRadius * 1.2f),
                            size = Size(baseRadius * 1.2f, baseRadius * 2.0f),
                            style = Fill
                        )
                    }
                }

                // Outer circle of core
                drawCircle(
                    color = primaryColor.copy(alpha = 0.2f),
                    radius = baseRadius * 0.8f,
                    center = center
                )

                // Glowing center
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            primaryColor.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    ),
                    radius = baseRadius * 0.5f,
                    center = center
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            currentPhase.label,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "${secondsRemaining}s",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (usageAddition > 0 || limitAddition > 0) {
            val reasons = mutableListOf<String>()
            if (usageAddition > 0) reasons.add("+${usageAddition}s screen time")
            if (limitAddition > 0) reasons.add("+${limitAddition}s near limit")
            Text(
                "Adjusted: ${reasons.joinToString(" · ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        if (settings.allowSkipBreathing) {
            FilledTonalButton(onClick = onSkip) { Text("Skip") }
        }
    }
}

private data class Phase(val label: String, val duration: Int, val startScale: Float, val endScale: Float)

/** Distributes total breathing time across inhale / hold / exhale / hold using a 4:2:4:2 ratio. */
private fun buildBreathingPhases(totalSeconds: Int): List<Phase> {
    val ratios = listOf(4, 2, 4, 2)
    val labels = listOf("Breathe In", "Hold", "Breathe Out", "Hold")
    val scales = listOf(
        0.72f to 1.18f,
        1.18f to 1.18f,
        1.18f to 0.72f,
        0.72f to 0.72f
    )
    val totalRatio = ratios.sum()
    val durations = ratios.map { (totalSeconds * it / totalRatio).coerceAtLeast(1) }.toMutableList()
    val remainder = totalSeconds - durations.sum()
    if (remainder != 0) {
        durations[durations.lastIndex] = (durations[durations.lastIndex] + remainder).coerceAtLeast(1)
    }
    return durations.mapIndexed { index, duration ->
        Phase(labels[index], duration, scales[index].first, scales[index].second)
    }.filter { it.duration > 0 }
}

// ── Session Picker (Apple Clock-style list) ──

@Composable
private fun SessionPickerOverlay(
    app: MonitoredApp, settings: HalterSettings,
    reflectionText: String, onReflectionChange: (String) -> Unit,
    onConfirm: (Int?, LimitType) -> Unit
) {
    // Determine if this is a scrollable/short-video app (built-in OR custom)
    val isShortVideoApp = com.ujwal.halter.service.KnownScrollApps.all.contains(app.packageName) ||
        settings.customScrollPackages.split(",").map { it.trim() }.filter { it.isNotEmpty() }.contains(app.packageName)

    // For short-video apps the user can toggle between Time and Scroll limits;
    // non-scroll apps always use time.
    var selectedType by remember {
        mutableStateOf(LimitType.TIME)
    }

    val unit = if (selectedType == LimitType.TIME) "min" else "scrolls"

    // StayFree-style presets for TIME: 1, 2, 3, 5, 10, 15, 30, 60
    val timePresets = listOf(1, 2, 3, 5, 10, 15, 30, 60)
    // Scroll count presets: 10, 25, 50, 100, 250, 500
    val scrollPresets = listOf(10, 25, 50, 100, 250, 500)
    val presets = if (selectedType == LimitType.TIME) timePresets else scrollPresets

    val allOptions = presets.map { Option("${it}", it, false) } +
        if (settings.allowCustomSessionLimit) listOf(Option("Custom", null, true)) else emptyList()

    var selected by remember { mutableIntStateOf(presets.firstOrNull() ?: 0) }
    var showCustom by remember { mutableStateOf(false) }
    var customValue by remember { mutableStateOf("") }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = 0
    )
    val snapFling = rememberSnapFlingBehavior(lazyListState = listState)

    // Reset wheel & selection whenever the user switches between Time and Scroll mode
    LaunchedEffect(selectedType) {
        selected = presets.firstOrNull() ?: 0
        showCustom = false
        customValue = ""
        listState.scrollToItem(0)
    }

    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.totalItemsCount == 0) 0
            else {
                val center = layoutInfo.viewportStartOffset +
                    (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
                layoutInfo.visibleItemsInfo.minByOrNull {
                    kotlin.math.abs((it.offset + it.size / 2) - center)
                }?.index ?: listState.firstVisibleItemIndex
            }
        }
    }

    LaunchedEffect(listState, allOptions.size) {
        snapshotFlow { centeredIndex }
            .distinctUntilChanged()
            .collect { index ->
                val option = allOptions.getOrNull(index) ?: return@collect
                if (!option.isCustom) {
                    selected = option.value!!
                    showCustom = false
                } else if (settings.allowCustomSessionLimit) {
                    showCustom = true
                }
            }
    }

    OverlaySurface(settings) {
        Text(app.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        // Limit-type toggle row removed since scroll tracking is disabled.
        // if (isShortVideoApp) { ... }

        Text(
            "Set your $unit limit",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.92f)
                    .height(52.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        RoundedCornerShape(12.dp)
                    )
            )
            Row(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    flingBehavior = snapFling,
                    contentPadding = PaddingValues(vertical = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    itemsIndexed(allOptions, key = { _, option -> option.label }) { index, option ->
                        val isCentered = index == centeredIndex
                        val distance = kotlin.math.abs(index - centeredIndex).coerceAtMost(3)
                        val itemAlpha = when (distance) {
                            0 -> 1f
                            1 -> 0.55f
                            else -> 0.28f
                        }
                        val itemScale = when (distance) {
                            0 -> 1.08f
                            1 -> 0.94f
                            else -> 0.86f
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .graphicsLayer {
                                    alpha = itemAlpha
                                    scaleX = itemScale
                                    scaleY = itemScale
                                }
                                .clickable {
                                    kotlinx.coroutines.MainScope().launch {
                                        listState.animateScrollToItem(index)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (option.isCustom) "Custom" else "${option.label} $unit",
                                style = if (isCentered) {
                                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                } else {
                                    MaterialTheme.typography.bodyLarge
                                },
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                LazyColumnScrollbar(
                    listState = listState,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        // Custom value input (appears below the wheel when Custom is selected)
        if (showCustom) {
            OutlinedTextField(
                value = customValue,
                onValueChange = { customValue = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Enter $unit") }
            )
        }

        // Reflection / journal prompt — shown for ALL limit types when enabled.
        // Must be >10 characters before the user can confirm.
        val journalValid = !settings.journalPromptEnabled || reflectionText.trim().length > 10
        if (settings.journalPromptEnabled) {
            val charCount = reflectionText.trim().length
            OutlinedTextField(
                value = reflectionText,
                onValueChange = onReflectionChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                placeholder = { Text("What do you actually need this app for?") },
                supportingText = {
                    val remaining = (11 - charCount).coerceAtLeast(0)
                    if (!journalValid) {
                        Text(
                            if (charCount == 0) "Required — describe your intention"
                            else "${remaining} more character${if (remaining == 1) "" else "s"} needed",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("${charCount} chars ✓", color = MaterialTheme.colorScheme.primary)
                    }
                },
                isError = !journalValid && charCount > 0
            )
            android.util.Log.d("HalterJournal", "Journal field updated: $charCount chars, valid=$journalValid")
        }

        Button(
            onClick = {
                if (!journalValid) return@Button
                val limit = if (showCustom && customValue.isNotBlank()) customValue.toIntOrNull() else selected
                onConfirm(limit, selectedType)
            },
            enabled = journalValid,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (!journalValid) "Write your intention first" else "Start session") }
    }
}

@Composable
private fun DeepFocusExitOverlay(
    sessionMinutes: Int,
    earlyEndMinutes: Int,
    reasonText: String,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    OverlaySurface(HalterSettings()) {
        Text("End Deep Focus", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Take a moment before ending early. This helps you stay mindful.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            "Session: $sessionMinutes min${if (earlyEndMinutes > 0) " · ended early $earlyEndMinutes min" else ""}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = reasonText,
            onValueChange = onReasonChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            placeholder = { Text("Why are you stopping early?") },
            supportingText = {
                val remaining = (11 - reasonText.trim().length).coerceAtLeast(0)
                if (reasonText.trim().length < 11) {
                    Text(
                        if (reasonText.trim().isEmpty()) "Required — describe why you're ending focus early"
                        else "$remaining more character${if (remaining == 1) "" else "s"} needed",
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text("${reasonText.trim().length} chars ✓", color = MaterialTheme.colorScheme.primary)
                }
            },
            isError = reasonText.trim().length in 1..10
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onConfirm,
            enabled = reasonText.trim().length > 10,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("End Focus")
        }
    }
}

private data class Option(val label: String, val value: Int?, val isCustom: Boolean)

// ── Block Overlay (modern) ──

@Composable
private fun BlockOverlay(decision: BlockDecision, onDismiss: () -> Unit) {
    val isCooldown = decision.reason.contains("session limit is over", ignoreCase = true)
    OverlaySurface(HalterSettings()) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
        Text(
            decision.appName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            if (isCooldown) "Session ended" else "is blocked",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            decision.reason,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (!decision.strict) {
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(if (isCooldown) "Dismiss & close app" else "Dismiss")
            }
        }
    }
}

@Composable
private fun WarningOverlay(
    title: String,
    message: String,
    warningSeconds: Int,
    onTimeout: () -> Unit
) {
    var remainingSeconds by remember { mutableStateOf(warningSeconds) }
    LaunchedEffect(warningSeconds) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
        }
        onTimeout()
    }

    OverlaySurface(HalterSettings()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Close this site in time or Halter will force close the browser",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.error)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "$remainingSeconds",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }

            Box(
                Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$remainingSeconds",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError
                )
            }

            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

// ── OverlaySurface (background scrim + card) ──

@Composable
private fun OverlaySurface(settings: HalterSettings, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.extraLarge, tonalElevation = 4.dp) {
            Column(Modifier.padding(24.dp).fillMaxWidth(0.86f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}
