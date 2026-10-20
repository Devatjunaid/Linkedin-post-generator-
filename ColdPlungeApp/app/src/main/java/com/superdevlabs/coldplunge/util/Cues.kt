package com.superdevlabs.coldplunge.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Audio + haptic cues for the timer. Respects the user's sound / vibration settings. */
class Cues(context: Context) {
    var soundEnabled = true
    var vibrationEnabled = true

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    private val tone: ToneGenerator? = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 90) }.getOrNull()

    fun countdown(secondsLeft: Int) {
        beep(if (secondsLeft == 10) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP, 140)
        vibrate(longArrayOf(0, 60))
    }

    fun phaseStart() {
        beep(ToneGenerator.TONE_PROP_ACK, 450)
        vibrate(longArrayOf(0, 260, 120, 260))
    }

    fun minuteMark() = vibrate(longArrayOf(0, 45))

    fun finished() {
        beep(ToneGenerator.TONE_PROP_PROMPT, 700)
        vibrate(longArrayOf(0, 400, 150, 400, 150, 600))
    }

    fun release() {
        tone?.release()
    }

    private fun beep(type: Int, durationMs: Int) {
        if (soundEnabled) runCatching { tone?.startTone(type, durationMs) }
    }

    private fun vibrate(pattern: LongArray) {
        if (!vibrationEnabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        runCatching { v.vibrate(VibrationEffect.createWaveform(pattern, -1)) }
    }
}
