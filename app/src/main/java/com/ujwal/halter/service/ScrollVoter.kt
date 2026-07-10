// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.service

import android.util.Log

enum class SignalType {
    /** Raw touch swipe gesture (API 33+ only, not currently used). */
    MOTION,
    /** Accessibility tree content change detected on a known feed surface. */
    CONTENT_CHANGE,
    /** Brief audio silence detected between clip transitions. */
    AUDIO_GAP
}

/**
 * Confidence-voting scroll counter.
 *
 * ## How it works
 * Each signal type (CONTENT_CHANGE, AUDIO_GAP) is individually reliable under different
 * conditions but prone to occasional false positives when used alone:
 *
 * - CONTENT_CHANGE fires on *any* accessibility tree mutation, including ad refreshes,
 *   notification badges, and subtitles updating.
 * - AUDIO_GAP fires on playback state transitions, which can also happen when the user
 *   pauses manually, seeks, or opens a menu that pauses the video.
 *
 * Requiring TWO distinct signals to agree within [VOTE_WINDOW_MS] filters out most
 * false positives, since genuinely independent signals coinciding within 800ms strongly
 * suggests an actual video swap occurred.
 *
 * ## Scoring
 * Signals are not equally reliable. A single AUDIO_GAP within the window counts as
 * confidence-score 2 (very reliable — false gap rate is low). CONTENT_CHANGE alone
 * counts as score 1. A scroll is triggered when total score >= [CONFIDENCE_THRESHOLD].
 * This means:
 * - AUDIO_GAP alone → score 2 → triggers (strong signal, audio swap is highly specific).
 * - CONTENT_CHANGE alone → score 1 → does NOT trigger alone.
 * - CONTENT_CHANGE + CONTENT_CHANGE → impossible (deduped by type).
 * - CONTENT_CHANGE + AUDIO_GAP → score 3 → triggers (belt-and-suspenders confirmation).
 *
 * ## Cooldown
 * After a scroll is counted [COOLDOWN_MS] must elapse before the next one. This prevents
 * a single swipe from generating two counts when both signals arrive close together and
 * then the voter re-fires on a stale pending signal.
 */
class ScrollVoter(
    /**
     * Kept for API compatibility but no longer affects voting logic — motion events
     * are permanently disabled to prevent system input contention.
     */
    @Suppress("UNUSED_PARAMETER") val supportsMotionEvents: Boolean,
    private val onScrollDetected: (timestamp: Long) -> Unit
) {
    /** Deduped list of signals seen within the current vote window. */
    private val pendingSignals = mutableListOf<Pair<SignalType, Long>>()
    private var lastCountedTime = 0L

    @Synchronized
    fun registerSignal(type: SignalType, timestamp: Long) {
        // 1. Evict signals that are older than the vote window.
        pendingSignals.removeAll { timestamp - it.second > VOTE_WINDOW_MS }

        // 2. Deduplicate: only one signal per type per window.
        if (pendingSignals.none { it.first == type }) {
            pendingSignals.add(type to timestamp)
            Log.d(TAG, "Signal $type accepted at $timestamp (pending=${pendingSignals.map { it.first }})")
        } else {
            Log.d(TAG, "Signal $type deduplicated")
            return
        }

        // 3. Compute confidence score from pending signals.
        val score = pendingSignals.sumOf { (signalType, _) ->
            when (signalType) {
                SignalType.AUDIO_GAP -> SCORE_AUDIO_GAP
                SignalType.CONTENT_CHANGE -> SCORE_CONTENT_CHANGE
                SignalType.MOTION -> SCORE_MOTION
            }
        }

        // 4. Fire if score crosses threshold AND cooldown has elapsed.
        if (score >= CONFIDENCE_THRESHOLD && timestamp - lastCountedTime > COOLDOWN_MS) {
            lastCountedTime = timestamp
            val votedTypes = pendingSignals.map { it.first }
            pendingSignals.clear()
            Log.d(TAG, "✓ Scroll counted at $timestamp — votes=$votedTypes score=$score")
            onScrollDetected(timestamp)
        }
    }

    @Synchronized
    fun reset() {
        pendingSignals.clear()
        Log.d(TAG, "ScrollVoter reset")
    }

    /**
     * Registers a scroll that is already confirmed (e.g. TYPE_VIEW_SCROLLED on a verified
     * short-video feed container). Bypasses the confidence-vote system entirely and fires
     * [onScrollDetected] immediately, subject only to [COOLDOWN_MS].
     *
     * This is the correct path for YouTube Shorts and Instagram Reels where ViewPager2
     * fires reliable scroll events but only produces a single signal type.
     */
    @Synchronized
    fun registerDirectScroll(timestamp: Long) {
        if (timestamp - lastCountedTime > COOLDOWN_MS) {
            lastCountedTime = timestamp
            pendingSignals.clear()
            Log.d(TAG, "✓ Direct scroll counted at $timestamp (bypassing voter)")
            onScrollDetected(timestamp)
        } else {
            Log.d(TAG, "Direct scroll suppressed — cooldown active (${timestamp - lastCountedTime}ms since last)")
        }
    }

    companion object {
        private const val TAG = "ScrollVoter"

        /**
         * How long (ms) distinct signals can be apart and still count as the same scroll.
         * 800ms covers slow-network clip transitions where audio gap fires late.
         */
        const val VOTE_WINDOW_MS = 800L

        /**
         * Minimum time (ms) between two counted scrolls. Prevents double-counting
         * when multiple signals arrive at nearly the same time for a single swipe.
         */
        const val COOLDOWN_MS = 1_200L

        /** Score needed to fire onScrollDetected. */
        const val CONFIDENCE_THRESHOLD = 2

        // Signal weights
        const val SCORE_AUDIO_GAP = 2      // Very reliable alone — audio swap is highly specific
        const val SCORE_CONTENT_CHANGE = 1  // Needs corroboration
        const val SCORE_MOTION = 1          // Unused but kept for future
    }
}
