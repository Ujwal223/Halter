package com.ujwal.halter.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class FeedGuardStateMachine(
    private val context: Context,
    private val userAddedPackages: () -> Set<String>,
    private val isFeedGuardEnabledFor: (String) -> Boolean,
    private val onTimeoutStillOnFeed: (String) -> Unit
) {
    private enum class State { IDLE, WARNING_ACTIVE }
    private var state = State.IDLE
    private var currentPackage: String? = null
    private var secondsLeft = 0

    private val overlay = FeedGuardOverlay(context)
    private val audioGuard = AudioGuard(context)
    private val handler = Handler(Looper.getMainLooper())
    private var tickRunnable: Runnable? = null

    fun onEvent(event: AccessibilityEvent, rootProvider: () -> AccessibilityNodeInfo?) {
        val pkg = event.packageName?.toString() ?: return
        if (!isFeedGuardEnabledFor(pkg)) {
            if (state == State.WARNING_ACTIVE) reset()
            return
        }

        val root = rootProvider() ?: return
        val onFeed = try {
            FeedScreenDetector.isOnFeedScreen(root, pkg, userAddedPackages())
        } finally {
            root.recycle()
        }

        when (state) {
            State.IDLE -> if (onFeed) startWarning(pkg)
            State.WARNING_ACTIVE -> {
                if (!onFeed) {
                    reset()
                } else if (pkg != currentPackage) {
                    reset()
                }
            }
        }
    }

    private fun startWarning(pkg: String) {
        state = State.WARNING_ACTIVE
        currentPackage = pkg
        secondsLeft = 5

        audioGuard.engage()
        overlay.show(secondsLeft)

        tickRunnable = object : Runnable {
            override fun run() {
                secondsLeft -= 1
                if (secondsLeft <= 0) {
                    val pkgAtTimeout = currentPackage
                    reset()
                    if (pkgAtTimeout != null) onTimeoutStillOnFeed(pkgAtTimeout)
                } else {
                    overlay.updateSecondsLeft(secondsLeft)
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.postDelayed(tickRunnable!!, 1000)
    }

    fun reset() {
        tickRunnable?.let { handler.removeCallbacks(it) }
        tickRunnable = null
        overlay.hide()
        audioGuard.release()
        state = State.IDLE
        currentPackage = null
        secondsLeft = 0
    }
}
