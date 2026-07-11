package com.ujwal.halter.service

import android.content.Context
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

object BrowserUrlWatcher {

    private const val DEBOUNCE_MS = 400L
    private val lastCheck = mutableMapOf<String, Long>()
    private var lastDomainSeen: String? = null

    private val RELEVANT_EVENT_TYPES = setOf(
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
        AccessibilityEvent.TYPE_VIEW_FOCUSED
    )

    /**
     * Call this from the existing AccessibilityService.onAccessibilityEvent.
     * onDomainDetected is invoked whenever a domain change is detected.
     */
    fun onEvent(
        event: AccessibilityEvent,
        context: Context,
        rootProvider: () -> android.view.accessibility.AccessibilityNodeInfo?,
        onDomainDetected: (String) -> Unit
    ) {
        val pkg = event.packageName?.toString() ?: return
        if (event.eventType !in RELEVANT_EVENT_TYPES) return
        if (!BrowserRegistry.isBrowser(context, pkg)) return
        if (!shouldProcess(pkg)) return

        val root = rootProvider() ?: return
        try {
            val rawUrl = UrlBarLocator.findUrlText(root, pkg) ?: return
            val domain = DomainExtractor.extract(rawUrl) ?: return

            if (domain == lastDomainSeen) return
            lastDomainSeen = domain

            onDomainDetected(domain)
        } finally {
            root.recycle()
        }
    }

    private fun shouldProcess(pkg: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        val last = lastCheck[pkg] ?: 0L
        return if (now - last > DEBOUNCE_MS) {
            lastCheck[pkg] = now
            true
        } else false
    }
}
