// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import com.ujwal.halter.data.ContentType
import com.ujwal.halter.data.HalterRepository
import com.ujwal.halter.data.JournalEntry
import com.ujwal.halter.data.JournalReason
import com.ujwal.halter.data.LimitType
import com.ujwal.halter.settings.HalterSettings
import com.ujwal.halter.settings.SettingsRepository
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class HalterAccessibilityService : AccessibilityService() {
    private val repository: HalterRepository by inject()
    private val blockDecisionEngine: BlockDecisionEngine by inject()
    private val scrollDetector: ScrollDetector by inject()
    private val overlayController: OverlayController by inject()
    private val hoverOverlay: HoverOverlay by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var foregroundPackage: String? = null
    private var sessionTickerJob: Job? = null
    private var customScrollPackages = setOf<String>()
    private var lastVideoSignature = ""
    /**
     * True when a secondary overlay window (comment sheet, share sheet, description panel, etc.)
     * is visible on top of the video feed. While true all scroll signals are suppressed.
     */
    private var subPanelWindowActive = false

    private val supportsMotionEvents = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    private val audioHandler = Handler(Looper.getMainLooper())
    private var audioGapDetector: AudioGapDetector? = null

    private var swipeStartY = 0f
    private var swipeStartX = 0f
    private var swipeStartTime = 0L

    private lateinit var scrollVoter: ScrollVoter

    private var foregroundPartialShortVideoBlocked = false
    private var lastFeedGuardCheckMillis = 0L
    private var lastSiteKeywordCheckMillis = 0L

    private var foregroundJob: Job? = null
    private var contentChangeJob: Job? = null
    private var serviceConnectedAtMillis = 0L
    private var lastForceHomeMillis = 0L
    /** Last class name seen in TYPE_WINDOW_STATE_CHANGED — used for activity-based Feed Guard. */
    private var lastWindowClassName: String? = null

    companion object {
        private const val CONTENT_CHANGE_DEBOUNCE_MS = 400L
        private const val FORCE_HOME_COOLDOWN_MS = 2_500L
        private const val ENFORCEMENT_GRACE_MS = 3_000L
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceConnectedAtMillis = System.currentTimeMillis()
        // Scroll voter disabled — scroll counting is commented out pending refactor
        // scrollVoter = ScrollVoter(supportsMotionEvents = false) { timestamp ->
        //     foregroundPackage?.let { handleScrollCounted(it, timestamp) }
        // }
        val info = serviceInfo ?: return
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON
        serviceInfo = info

        // Observe settings changes to dynamically toggle Greyscale Mode
        scope.launch {
            settingsRepository.settings.collect { settings ->
                updateGrayscaleState(settings)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        
        // Feed Guard: Check immediately on any relevant window or view event
        if (foregroundPackage == packageName && foregroundPartialShortVideoBlocked) {
            checkFeedGuardThrottled(packageName)
        }

        // Site/Keyword Blocking: Check immediately on any interaction
        checkSiteAndKeywordBlockingThrottled(packageName)

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                lastWindowClassName = event.className?.toString()
                updateSubPanelWindowState()
                onForegroundChanged(packageName)
            }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> updateSubPanelWindowState()
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> scheduleContentChangeSignal(packageName)
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> handleViewScrolled(packageName, event)
        }
    }

    /**
     * Checks all currently visible AccessibilityWindowInfo objects. If any secondary window
     * (TYPE_ACCESSIBILITY_OVERLAY or unnamed auxiliary window) is present alongside the main app
     * window, we mark subPanelWindowActive = true to suppress scroll counting.
     *
     * This catches comment sheets, share sheets, and description overlays even when they don't
     * expose readable view IDs in the accessibility tree (since they live in a separate window).
     */
    private fun updateSubPanelWindowState() {
        val pkg = foregroundPackage ?: return
        val windows = windows ?: return
        var overlayWindowSeen = false
        for (window in windows) {
            val root = window.root
            if (root != null) {
                val winPkg = root.packageName?.toString()
                if (winPkg == pkg) {
                    val type = try { window.type } catch (_: Exception) { -1 }
                    val isAccessibilityOverlay = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY
                    val isSystemWindow = type == AccessibilityWindowInfo.TYPE_SYSTEM
                    if (isAccessibilityOverlay || isSystemWindow) {
                        overlayWindowSeen = true
                    }
                }
                root.recycle()
            }
        }
        subPanelWindowActive = overlayWindowSeen
    }

    override fun onMotionEvent(event: MotionEvent) = Unit

    override fun onInterrupt() = Unit

    override fun onCreate() {
        super.onCreate()
        // Audio gap detector disabled — tied to scroll tracking which is commented out
        // val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
        // if (audioManager != null) { audioGapDetector = AudioGapDetector(...) }
        scope.launch {
            val settings = blockDecisionEngine.settings()
            customScrollPackages = settings.customScrollPackages
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
            scrollDetector.setCustomPackages(customScrollPackages)
        }
    }

    override fun onDestroy() {
        sessionTickerJob?.cancel()
        foregroundJob?.cancel()
        contentChangeJob?.cancel()
        stopAudioGapDetection()
        scope.cancel()
        super.onDestroy()
    }

    private val launcherPackages = setOf(
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.oneplus.launcher",
        "com.miui.home",
        "com.sec.android.app.launcher",
        "com.huawei.android.launcher",
        "com.oppo.launcher",
        "com.realme.launcher",
        "com.teslacoilsw.launcher",
        "com.microsoft.launcher",
        "com.actionlauncher.playstore",
        "com.niagara.launcher",
        "com.lge.launcher3"
    )

    private val safeSystemPackages = setOf(
        "com.android.systemui",
        "com.android.inputmethod.latin",
        "com.google.android.inputmethod.latin",
        "com.samsung.android.honeyboard",
        "com.touchtype.swiftkey",
        "com.microsoft.ime",
        "org.pocketworkstation.pckeyboard",
        "com.grammarly.android.keyboard",
        "com.anysoftkeyboard",
        "com.android.systemui.gesture"
    ) + launcherPackages

    private fun isLauncher(packageName: String) = packageName in launcherPackages

    private fun isWatchedPackage(packageName: String): Boolean =
        packageName in FeedFingerprints.WATCHED_PACKAGES || packageName in customScrollPackages

    private fun scheduleContentChangeSignal(packageName: String) {
        if (overlayController.isShowingAnyOverlay) return
        if (foregroundPackage != packageName) return
        if (!isWatchedPackage(packageName)) return

        contentChangeJob?.cancel()
        contentChangeJob = scope.launch {
            delay(CONTENT_CHANGE_DEBOUNCE_MS)
            // Scroll tracking disabled — only run Feed Guard check
            checkFeedGuardThrottled(packageName)
        }
    }

    @Suppress("UNUSED")
    private fun processContentChangeSignal(packageName: String) {
        /* Scroll tracking disabled. Feed Guard is handled by checkFeedGuard() instead.
         * Original signature-based scroll counting kept here for future re-enablement. */
        if (overlayController.isShowingAnyOverlay) return
        if (foregroundPackage != packageName) return
        if (!isWatchedPackage(packageName)) return

        // Gate 1: window-level — a bottom sheet / share panel / dialog opened as a separate window.
        if (subPanelWindowActive) {
            lastVideoSignature = ""
            return
        }

        val root = rootInActiveWindow ?: return
        try {
            // Gate 2: app-specific blocking overlay IDs (comments_bottom_sheet, share_sheet, etc.)
            if (FeedFingerprints.isBlockingOverlayPresent(root, packageName)) {
                lastVideoSignature = ""
                return
            }

            // Gate 3: generic text/view-id heuristic sub-view check (about section, reply panel, etc.)
            if (scrollDetector.isSubViewActive(root)) {
                lastVideoSignature = ""
                return
            }

            // Signature-change path: only fires when on a clean feed screen.
            // We additionally verify isFeedScreenActive before trusting a signature diff,
            // so that opening a description panel (which changes visible text) doesn't count.
            val signature = scrollDetector.getVideoSignature(root)
            if (signature.isNotEmpty()) {
                if (lastVideoSignature.isNotEmpty() && lastVideoSignature != signature) {
                    // Only count if we can confirm the feed container is actually on screen.
                    val onFeed = FeedFingerprints.isFeedScreenActive(root, packageName) ||
                        scrollDetector.detectContentType(packageName, root).isShortVideo() ||
                        (packageName in customScrollPackages)
                    if (onFeed) {
                        lastVideoSignature = signature
                        android.util.Log.d("HalterAccessibility", "Signature scroll counted for $packageName")
                        // Reset voter so any concurrent audio gap doesn't double-count this swipe.
                        if (::scrollVoter.isInitialized) scrollVoter.reset()
                        handleScrollCounted(packageName, System.currentTimeMillis())
                        return
                    } else {
                        // Signature changed but no feed surface visible — probably description/about panel.
                        // Update the stored signature but do NOT count a scroll.
                        android.util.Log.d("HalterAccessibility", "Signature changed but no feed surface — suppressed")
                        lastVideoSignature = signature
                        return
                    }
                }
                lastVideoSignature = signature
            }

            if (FeedFingerprints.isFeedScreenActive(root, packageName)) {
                scrollVoter.registerSignal(SignalType.CONTENT_CHANGE, System.currentTimeMillis())
            } else {
                val contentType = scrollDetector.detectContentType(packageName, root)
                if (contentType.isShortVideo() || (packageName in customScrollPackages && contentType != ContentType.UNKNOWN)) {
                    scrollVoter.registerSignal(SignalType.CONTENT_CHANGE, System.currentTimeMillis())
                }
            }
        } finally {
            root.recycle()
        }
    }

    private fun handleViewScrolled(packageName: String, event: AccessibilityEvent) {
        /* Scroll tracking disabled — TYPE_VIEW_SCROLLED no longer used for counting.
         * Feed Guard detection is driven by TYPE_WINDOW_CONTENT_CHANGED → checkFeedGuard(). */
        return
        @Suppress("UNREACHABLE_CODE")
        if (overlayController.isShowingAnyOverlay) return
        if (foregroundPackage != packageName) return
        if (!isWatchedPackage(packageName)) return
        if (subPanelWindowActive) return

        val root = rootInActiveWindow ?: return
        try {
            if (FeedFingerprints.isBlockingOverlayPresent(root, packageName)) return
            if (scrollDetector.isSubViewActive(root)) return

            // Check if we are on a confirmed short-video feed surface.
            val onFeed = FeedFingerprints.isFeedScreenActive(root, packageName) ||
                scrollDetector.detectContentType(packageName, root).isShortVideo() ||
                (packageName in customScrollPackages)

            if (!onFeed) return

            // Use direct-scroll path for confirmed feed surfaces.
            // YouTube Shorts and Instagram Reels both use ViewPager2, which does NOT update
            // event.fromIndex between pages — so the old fromIndex guard was always blocking
            // valid scroll counts. registerDirectScroll() is gated only by COOLDOWN_MS.
            android.util.Log.d("HalterAccessibility", "TYPE_VIEW_SCROLLED direct scroll for $packageName")
            if (::scrollVoter.isInitialized) {
                scrollVoter.registerDirectScroll(System.currentTimeMillis())
            }
        } finally {
            root.recycle()
        }
    }

    @Suppress("UNUSED")
    private fun handleScrollCounted(packageName: String, @Suppress("UNUSED_PARAMETER") timestamp: Long) {
        /* Scroll tracking disabled — scroll voter no longer fires this callback. */
    }

    private fun startAudioGapDetection() {
        // Disabled — audio gap detection tied to scroll tracking (commented out)
        // audioGapDetector?.start(audioHandler)
    }

    private fun stopAudioGapDetection() {
        // Disabled — audio gap detection tied to scroll tracking (commented out)
        // audioGapDetector?.stop()
        // if (::scrollVoter.isInitialized) scrollVoter.reset()
    }

    private fun updateGrayscaleState(settings: HalterSettings) {
        val shouldBeGrayscale = settings.greyscaleEnabled || (settings.bedtimeEnabled && isCurrentlyBedtime(settings))
        val currentStatus = try {
            android.provider.Settings.Secure.getInt(contentResolver, "accessibility_display_daltonizer_enabled") == 1
        } catch (_: Exception) { false }

        if (shouldBeGrayscale != currentStatus) {
            try {
                if (shouldBeGrayscale) {
                    android.provider.Settings.Secure.putInt(contentResolver, "accessibility_display_daltonizer", 0)
                    android.provider.Settings.Secure.putInt(contentResolver, "accessibility_display_daltonizer_enabled", 1)
                } else {
                    android.provider.Settings.Secure.putInt(contentResolver, "accessibility_display_daltonizer_enabled", 0)
                }
            } catch (e: Exception) {
                android.util.Log.e("HalterGrayscale", "Failed to write secure settings for grayscale", e)
            }
        }
    }

    private fun isCurrentlyBedtime(settings: HalterSettings): Boolean {
        val now = java.time.Instant.ofEpochMilli(System.currentTimeMillis()).atZone(java.time.ZoneId.systemDefault())
        val minute = now.hour * 60 + now.minute
        val startMinute = settings.bedtimeStartHour * 60 + settings.bedtimeStartMinute
        val endMinute = settings.bedtimeEndHour * 60 + settings.bedtimeEndMinute
        return ScheduleRules.isMinuteInWindow(minute, startMinute, endMinute)
    }

    private fun checkSiteAndKeywordBlockingThrottled(packageName: String) {
        val now = System.currentTimeMillis()
        if (now - lastSiteKeywordCheckMillis < 300L) return
        lastSiteKeywordCheckMillis = now
        scope.launch {
            checkSiteAndKeywordBlocking(packageName)
        }
    }

    private suspend fun checkSiteAndKeywordBlocking(packageName: String) {
        val settings = settingsRepository.settings.first()
        val siteEnabled = settings.siteBlockingEnabled
        val keywordEnabled = settings.keywordBlockingEnabled
        if (!siteEnabled && !keywordEnabled) return

        val root = rootInActiveWindow ?: return
        try {
            val siteList = if (siteEnabled) {
                settings.siteBlockedList.split(",")
                    .map { it.trim().lowercase() }
                    .filter { it.isNotEmpty() }
            } else emptyList()

            val keywordList = if (keywordEnabled) {
                settings.keywordBlockedList.split(",")
                    .map { it.trim().lowercase() }
                    .filter { it.isNotEmpty() }
            } else emptyList()

            if (siteList.isEmpty() && keywordList.isEmpty()) return

            val queue = ArrayDeque<AccessibilityNodeInfo>()
            val rootCopy = AccessibilityNodeInfo.obtain(root)
            queue.add(rootCopy)
            var checked = 0
            var matchedKeyword: String? = null
            var matchedSite: String? = null

            // Browsers: any app whose package name matches common browser patterns,
            // OR any app with a URL bar node — so niche forks (Palladium, Cromite, etc.) also work.
            val isBrowser = packageName in setOf(
                "com.android.chrome",
                "com.chrome.beta",
                "com.chrome.dev",
                "com.chrome.canary",
                "org.mozilla.firefox",
                "org.mozilla.fenix",
                "org.mozilla.focus",
                "com.microsoft.emmx",
                "com.brave.browser",
                "com.brave.browser_beta",
                "com.opera.browser",
                "com.opera.mini.native",
                "com.sec.android.app.sbrowser",
                "com.android.browser",
                "com.vivaldi.browser",
                "org.torproject.torbrowser",
                "com.kiwibrowser.browser",
                "org.bromite.bromite",
                "org.cromite.cromite",
                "com.duckduckgo.mobile.android",
                "com.ecosia.android",
                "com.github.kiwibrowser"
            )

            while (queue.isNotEmpty() && checked < 500) {
                val node = queue.removeFirst()
                checked++

                val text = node.text?.toString()?.lowercase()
                val contentDesc = node.contentDescription?.toString()?.lowercase()
                val viewId = node.viewIdResourceName
                val isEditableOrFocused = node.isEditable || node.isFocused

                if (keywordEnabled && keywordList.isNotEmpty() && !isEditableOrFocused) {
                    if (text != null) {
                        for (keyword in keywordList) {
                            if (text.contains(keyword)) {
                                matchedKeyword = keyword
                                break
                            }
                        }
                    }
                    if (matchedKeyword == null && contentDesc != null) {
                        for (keyword in keywordList) {
                            if (contentDesc.contains(keyword)) {
                                matchedKeyword = keyword
                                break
                            }
                        }
                    }
                }

                if (siteEnabled && siteList.isNotEmpty()) {
                    // Extract URL-like text from any node — works for ALL browsers including
                    // niche Chromium forks where we may not know the URL bar resource ID.
                    val isUrlBarNode = viewId != null && (
                        viewId.contains("url_bar", ignoreCase = true) ||
                        viewId.contains("address_bar", ignoreCase = true) ||
                        viewId.contains("search_box", ignoreCase = true) ||
                        viewId.contains("omnibox", ignoreCase = true)
                    )
                    // Skip checking active input/editing nodes to prevent false-positives while typing
                    val urlCandidate = if (isUrlBarNode && !isEditableOrFocused && text != null) text
                    else if (text != null && looksLikeUrl(text) && !isEditableOrFocused) text
                    else null

                    if (urlCandidate != null) {
                        val domain = extractDomain(urlCandidate)
                        for (site in siteList) {
                            if (domain == site || domain.endsWith(".$site")) {
                                matchedSite = site
                                break
                            }
                        }
                    }
                }

                if (matchedKeyword != null || matchedSite != null) {
                    break
                }

                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { queue.add(it) }
                }
                node.recycle()
            }

            while (queue.isNotEmpty()) {
                queue.removeFirst().recycle()
            }

            if (matchedKeyword != null || matchedSite != null) {
                val reason = if (matchedKeyword != null) "keyword '$matchedKeyword'" else "site '$matchedSite'"
                android.util.Log.d("HalterBlock", "Blocking due to $reason")
                
                Handler(Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(
                        this@HalterAccessibilityService,
                        "Blocked by Halter: $reason",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        } catch (e: Exception) {
            android.util.Log.e("HalterBlock", "Error during site/keyword check", e)
        } finally {
            root.recycle()
        }
    }

    private fun checkFeedGuardThrottled(packageName: String) {
        val now = System.currentTimeMillis()
        if (now - lastFeedGuardCheckMillis < 200L) return
        lastFeedGuardCheckMillis = now
        checkFeedGuard(packageName)
    }

    /**
     * Feed Guard — presses BACK when the foreground app is showing a vertical short-form
     * video feed (Reels, Shorts, TikTok etc.) AND the user has enabled "Feed Guard"
     * (partialShortVideoBlocked) for that app in its settings.
     *
     * Uses node-traversal via [FeedFingerprints] — no heuristics or scroll events needed.
     */
    private fun checkFeedGuard(packageName: String) {
        if (overlayController.isShowingAnyOverlay) return
        if (foregroundPackage != packageName) return
        if (!FeedFingerprints.WATCHED_PACKAGES.contains(packageName)) return
        if (!foregroundPartialShortVideoBlocked) return

        val root = rootInActiveWindow ?: return
        try {
            if (subPanelWindowActive) return
            if (FeedFingerprints.isBlockingOverlayPresent(root, packageName)) return
            // Cross-reference both FeedFingerprints and ScrollDetector for high-confidence detection
            val isFeed = FeedFingerprints.isFeedScreenActive(root, packageName, lastWindowClassName) ||
                scrollDetector.detectContentType(packageName, root).isShortVideo()
            if (!isFeed) return

            android.util.Log.d("HalterFeedGuard", "Blocking vertical feed in $packageName")
            // HOME is more reliable than BACK — TikTok intercepts BACK presses
            performGlobalAction(GLOBAL_ACTION_HOME)
        } finally {
            root.recycle()
        }
    }

    private fun onForegroundChanged(packageName: String) {
        val selfPackageName = this.packageName
        if (packageName == selfPackageName) return

        // If an interactive overlay is active (text input / IME), avoid changing
        // foreground handling. Non-interactive overlays (block UI) should still
        // allow foreground change processing so they can be cleared when the
        // user leaves the app.
        if (overlayController.isShowingInteractiveOverlay) {
            return
        }

        if (packageName in safeSystemPackages || packageName.startsWith("android")) {
            if (isLauncher(packageName)) {
                hoverOverlay.foregroundPackage = packageName
                scope.launch {
                    foregroundPackage?.let { repository.pauseSession(it) }
                    hoverOverlay.hide()
                    stopSessionTicker()
                    stopAudioGapDetection()
                    foregroundPartialShortVideoBlocked = false // Reset
                }
            }
            return
        }

        foregroundJob?.cancel()
        foregroundJob = scope.launch {
            hoverOverlay.foregroundPackage = packageName
            handleForegroundChanged(packageName, selfPackageName)
        }
    }

    private suspend fun handleForegroundChanged(packageName: String, selfPackageName: String) {
        if (foregroundPackage != packageName) {
            overlayController.hide()
            foregroundPackage?.let { previousPackage ->
                repository.pauseSession(previousPackage)
            }
            foregroundPackage = packageName
            lastVideoSignature = ""
            foregroundPartialShortVideoBlocked = false // reset until fetched

            if (isWatchedPackage(packageName)) {
                startAudioGapDetection()
            } else {
                stopAudioGapDetection()
            }
            if (::scrollVoter.isInitialized) scrollVoter.reset()

            val monitored = repository.getMonitoredApp(packageName)
            foregroundPartialShortVideoBlocked = monitored?.partialShortVideoBlocked == true

            if (monitored != null) {
                val effectiveMonitored = clearExpiredCooldown(monitored)
                val settings = blockDecisionEngine.settings()
                val activeSession = repository.activeSessionFor(packageName)

                if (activeSession != null) {
                    repository.resumeSession(packageName)
                    if (handleSessionTimeout(effectiveMonitored, settings)) return
                    updateHoverOverlay(effectiveMonitored)
                    startSessionTicker(effectiveMonitored)
                    return
                }

                if (effectiveMonitored.cooldownUntilEpochMillis != null &&
                    effectiveMonitored.cooldownUntilEpochMillis > System.currentTimeMillis()
                ) {
                    hoverOverlay.hide()
                    stopSessionTicker()
                    enforceBlock(
                        blockDecisionEngine.decisionForForeground(packageName),
                        settings
                    )
                    return
                }

                if (effectiveMonitored.isFlaggedHarmful) {
                    val preCheck = blockDecisionEngine.decisionForForeground(packageName)
                    if (preCheck.blocked) {
                        enforceBlock(preCheck, settings)
                        return
                    }
                    hoverOverlay.hide()
                    stopSessionTicker()
                    scope.launch {
                        val appUsageTodayMinutes = (repository.totalUsageMillisToday(packageName) / 60000L).toInt()
                        val monitoredApps = repository.observeMonitoredApps().first()
                        var totalUsageTodayMinutes = 0
                        for (mApp in monitoredApps) {
                            totalUsageTodayMinutes += (repository.totalUsageMillisToday(mApp.packageName) / 60000L).toInt()
                        }
                        withContext(Dispatchers.Main) {
                            overlayController.showBreathingThenPicker(
                                app = effectiveMonitored,
                                settings = settings,
                                appUsageTodayMinutes = appUsageTodayMinutes,
                                totalUsageTodayMinutes = totalUsageTodayMinutes
                            ) { chosenLimit, limitType, reflectionText ->
                                scope.launch {
                                    repository.startSession(packageName, chosenLimit, limitType)
                                    updateHoverOverlay(effectiveMonitored)
                                    startSessionTicker(effectiveMonitored)
                                    if (!reflectionText.isNullOrBlank()) {
                                        repository.recordJournal(
                                            JournalEntry(
                                                packageName = packageName,
                                                timestampEpochMillis = System.currentTimeMillis(),
                                                reason = JournalReason.ACTUAL_NEED
                                            )
                                        )
                                    }
                                    val postCheck = blockDecisionEngine.decisionForForeground(packageName)
                                    if (postCheck.blocked) enforceBlock(postCheck, settings)
                                }
                            }
                        }
                    }
                } else {
                    repository.startSession(packageName, effectiveMonitored.sessionTimeLimitMinutes, LimitType.TIME)
                    updateHoverOverlay(effectiveMonitored)
                    startSessionTicker(effectiveMonitored)
                }
            } else {
                hoverOverlay.hide()
                stopSessionTicker()
                stopAudioGapDetection()
            }
        } else {
            val monitored = repository.getMonitoredApp(packageName)
            if (monitored != null) {
                updateHoverOverlay(monitored)
            } else {
                hoverOverlay.hide()
            }
            if (packageName != selfPackageName) {
                monitored?.let {
                    val settings = blockDecisionEngine.settings()
                    if (handleSessionTimeout(it, settings)) return
                }
                val decision = blockDecisionEngine.decisionForForeground(packageName)
                if (decision.blocked) {
                    enforceBlock(decision, blockDecisionEngine.settings())
                }
            }
        }
    }

    private suspend fun clearExpiredCooldown(monitored: com.ujwal.halter.data.MonitoredApp): com.ujwal.halter.data.MonitoredApp {
        return if (monitored.cooldownUntilEpochMillis != null && monitored.cooldownUntilEpochMillis <= System.currentTimeMillis()) {
            monitored.copy(cooldownUntilEpochMillis = null).also { repository.saveMonitoredApp(it) }
        } else {
            monitored
        }
    }

    private fun startSessionTicker(monitored: com.ujwal.halter.data.MonitoredApp) {
        sessionTickerJob?.cancel()
        sessionTickerJob = scope.launch {
            while (isActive) {
                if (foregroundPackage != monitored.packageName) break
                val settings = blockDecisionEngine.settings()
                if (handleSessionTimeout(monitored, settings)) break
                updateHoverOverlay(monitored)
                val activeSession = repository.activeSessionFor(monitored.packageName)
                val remaining = remainingSessionSeconds(monitored, activeSession) ?: break
                if (remaining <= 0) break
                delay(1_000L)
            }
        }
    }

    private fun stopSessionTicker() {
        sessionTickerJob?.cancel()
        sessionTickerJob = null
    }

    private suspend fun updateHoverOverlay(monitored: com.ujwal.halter.data.MonitoredApp) {
        if (foregroundPackage != monitored.packageName) {
            hoverOverlay.hide()
            return
        }
        val activeSession = repository.activeSessionFor(monitored.packageName)
        if (activeSession == null) {
            hoverOverlay.hide()
            return
        }

        val sessionScrolls = activeSession.scrollsUsed
        val scrollLimit = if (activeSession.limitType == LimitType.SCROLL_COUNT) {
            activeSession.chosenSessionLimit ?: 0
        } else {
            monitored.scrollLimitPerSession ?: monitored.scrollLimitPerDay ?: 0
        }

        val remainingSeconds = if (activeSession.limitType == LimitType.TIME) {
            remainingSessionSeconds(monitored, activeSession) ?: 0
        } else {
            -1
        }

        if (activeSession.limitType == LimitType.TIME && remainingSeconds <= 0) {
            hoverOverlay.hide()
            return
        }

        hoverOverlay.updateData(
            packageName = monitored.packageName,
            scrolls = sessionScrolls,
            remainingSeconds = remainingSeconds,
            showScrolls = isWatchedPackage(monitored.packageName) || activeSession.limitType == LimitType.SCROLL_COUNT,
            name = monitored.displayName,
            limitScrolls = scrollLimit
        )
        hoverOverlay.show()
        hoverOverlay.refreshVisibility()
    }

    private suspend fun remainingSessionSeconds(
        monitored: com.ujwal.halter.data.MonitoredApp,
        session: com.ujwal.halter.data.UsageSession?
    ): Int? {
        val activeSession = session ?: repository.activeSessionFor(monitored.packageName) ?: return null
        if (activeSession.limitType != LimitType.TIME) return null
        val limitMinutes = activeSession.chosenSessionLimit ?: monitored.sessionTimeLimitMinutes ?: return null
        val limitMillis = TimeUnit.MINUTES.toMillis(limitMinutes.toLong())
        val consumedMillis = repository.sessionConsumedMillis(activeSession)
        return ((limitMillis - consumedMillis).coerceAtLeast(0L) / 1000L).toInt()
    }

    private suspend fun handleSessionTimeout(
        monitored: com.ujwal.halter.data.MonitoredApp,
        settings: HalterSettings
    ): Boolean {
        val activeSession = repository.activeSessionFor(monitored.packageName) ?: return false
        val limitReached = when (activeSession.limitType) {
            LimitType.TIME -> {
                val remainingSeconds = remainingSessionSeconds(monitored, activeSession) ?: return false
                remainingSeconds <= 0
            }
            LimitType.SCROLL_COUNT -> {
                val limit = activeSession.chosenSessionLimit ?: monitored.scrollLimitPerSession ?: 0
                limit > 0 && activeSession.scrollsUsed >= limit
            }
        }
        if (!limitReached) return false

        repository.closeActiveSessionFor(monitored.packageName)
        hoverOverlay.hide()
        stopSessionTicker()

        val cooldownUntil = if (settings.sessionCooldownEnabled && settings.sessionCooldownMinutes > 0) {
            System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(settings.sessionCooldownMinutes.toLong())
        } else {
            null
        }
        repository.saveMonitoredApp(monitored.copy(cooldownUntilEpochMillis = cooldownUntil))
        enforceBlock(blockDecisionEngine.decisionForForeground(monitored.packageName), settings)
        return true
    }

    private fun enforceBlock(decision: BlockDecision, settings: HalterSettings) {
        if (overlayController.canShowOverlays()) {
            showBlockWithForceClose(decision, settings)
        } else {
            // Without overlay permission, forceHome would trap the user with no visible UI.
            forceHomeIfNeeded()
        }
    }

    private fun showBlockWithForceClose(decision: BlockDecision, settings: HalterSettings) {
        forceHomeIfNeeded()
        overlayController.showBlock(decision, settings) {
            forceHomeIfNeeded()
            overlayController.hide()
        }
    }

    private fun forceHomeIfNeeded() {
        if (System.currentTimeMillis() - serviceConnectedAtMillis < ENFORCEMENT_GRACE_MS) return
        val currentForeground = foregroundPackage
        if (currentForeground != null && isLauncher(currentForeground)) return

        val now = System.currentTimeMillis()
        if (now - lastForceHomeMillis < FORCE_HOME_COOLDOWN_MS) return
        lastForceHomeMillis = now
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /** Returns true if [text] looks like a URL — prevents false-positive blocks on normal text. */
    private fun looksLikeUrl(text: String): Boolean {
        if (text.startsWith("http://") || text.startsWith("https://")) return true
        val trimmed = text.trim()
        return trimmed.length < 200 &&
            Regex("^(?:www\\.)?[a-zA-Z0-9][a-zA-Z0-9\\-]*\\.[a-zA-Z]{2,}(/.*)?$").matches(trimmed)
    }

    /** Extracts the lowercase hostname from a URL, stripping www. */
    private fun extractDomain(url: String): String {
        return try {
            val full = if (url.startsWith("http")) url else "https://$url"
            java.net.URI(full).host?.lowercase()?.removePrefix("www.") ?: url.lowercase()
        } catch (_: Exception) {
            url.lowercase().removePrefix("www.").substringBefore("/").substringBefore("?")
        }
    }
}

