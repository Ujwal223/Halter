package com.ujwal.halter.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

class AudioGuard(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var wasMutedByUs = false
    private var active = false

    fun engage() {
        if (active) return
        active = true

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setAudioAttributes(attrs)
            .setAcceptsDelayedFocusGain(false)
            .build()
        focusRequest = request
        audioManager.requestAudioFocus(request)

        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) > 0) {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_MUTE,
                0
            )
            wasMutedByUs = true
        }
    }

    fun release() {
        if (!active) return
        active = false

        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null

        if (wasMutedByUs) {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_UNMUTE,
                0
            )
            wasMutedByUs = false
        }
    }

    fun isActive() = active
}
