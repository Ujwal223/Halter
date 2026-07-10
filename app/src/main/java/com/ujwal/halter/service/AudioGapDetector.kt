// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.service

import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Handler
import android.util.Log

/**
 * Detects brief media playback gaps that occur when short-form video feeds swap sources.
 *
 * Short-form video apps (TikTok, Instagram Reels, YouTube Shorts, Snapchat Spotlight) all
 * exhibit a characteristic brief audio silence when the player hands off from one clip to the
 * next. This detector captures that pause/resume cycle and emits a signal within [GAP_MIN_MS]
 * to [GAP_MAX_MS].
 *
 * Design notes:
 * - The gap window is intentionally wide (10–600ms) to cover both fast pre-buffered swipes and
 *   slower network-loaded transitions.
 * - Callbacks to [onGapDetected] are delivered on the [Handler] provided to [start], which
 *   should be the main thread handler to stay consistent with accessibility callbacks.
 * - [onSubPanelActive] must be set externally so we skip audio gaps that coincide with UI
 *   overlay events (e.g. comment sheet opening can also cause a brief audio pause).
 */
class AudioGapDetector(
    private val audioManager: AudioManager,
    private val onGapDetected: (timestamp: Long) -> Unit
) {
    private var lastSilenceStartMs = 0L
    private var wasPlaying = false
    private var registered = false

    /**
     * External gate — set to true when a sub-panel (comments/share/description) is visible.
     * Audio gaps while a panel is open are NOT emitted to avoid false positives from
     * the brief audio pause that can happen when overlay UI is shown.
     */
    var isSubPanelActive: () -> Boolean = { false }

    private val playbackCallback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<android.media.AudioPlaybackConfiguration>) {
            val isPlaying = configs.any {
                val usage = it.audioAttributes.usage
                usage == AudioAttributes.USAGE_MEDIA || usage == AudioAttributes.USAGE_GAME
            }
            val nowMs = System.currentTimeMillis()

            when {
                wasPlaying && !isPlaying -> {
                    // Audio stopped — record when silence began
                    lastSilenceStartMs = nowMs
                }
                !wasPlaying && isPlaying && lastSilenceStartMs > 0L -> {
                    val gapMs = nowMs - lastSilenceStartMs
                    lastSilenceStartMs = 0L
                    if (gapMs in GAP_MIN_MS..GAP_MAX_MS) {
                        if (isSubPanelActive()) {
                            Log.d(TAG, "Audio gap ${gapMs}ms suppressed — sub-panel active")
                        } else {
                            Log.d(TAG, "Audio gap ${gapMs}ms → AUDIO_GAP signal")
                            onGapDetected(nowMs)
                        }
                    } else {
                        Log.d(TAG, "Audio gap ${gapMs}ms outside window [$GAP_MIN_MS..$GAP_MAX_MS], ignored")
                    }
                }
                else -> {
                    // No state change of interest; reset silence start if audio is still playing
                    if (isPlaying) lastSilenceStartMs = 0L
                }
            }
            wasPlaying = isPlaying
        }
    }

    fun start(handler: Handler) {
        if (registered) return
        try {
            audioManager.registerAudioPlaybackCallback(playbackCallback, handler)
            registered = true
            Log.d(TAG, "Audio gap detection started")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register audio playback callback", e)
        }
    }

    fun stop() {
        if (!registered) return
        try {
            audioManager.unregisterAudioPlaybackCallback(playbackCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister audio playback callback", e)
        }
        registered = false
        wasPlaying = false
        lastSilenceStartMs = 0L
        Log.d(TAG, "Audio gap detection stopped")
    }

    companion object {
        private const val TAG = "AudioGapDetector"

        /**
         * Minimum gap length to consider as a video swap signal.
         * 10ms floor: avoids counting codec micro-pauses during playback of a single video.
         */
        const val GAP_MIN_MS = 10L

        /**
         * Maximum gap length to consider as a video swap signal.
         * 600ms ceiling: longer gaps are likely the user deliberately pausing, seeking, or
         * the app buffering after a manual scrub rather than an automatic feed swap.
         */
        const val GAP_MAX_MS = 600L
    }
}
