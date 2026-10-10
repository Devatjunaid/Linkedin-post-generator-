package com.harrycodess.drawingkids.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.harrycodess.drawingkids.data.Prefs
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Tiny sound effects synthesized in code - no audio files needed. */
object Sfx {
    private const val RATE = 22050
    private val tracks = HashMap<String, AudioTrack>()

    var enabled: Boolean
        get() = Prefs.soundOn
        set(value) {
            Prefs.soundOn = value
        }

    fun pop() = play("pop") { synth(90) { t -> sweep(t, 520.0, 980.0, 0.09) * exp(-t * 32) } }
    fun tick() = play("tick") { synth(45) { t -> sin(2 * PI * 1400 * t) * exp(-t * 80) * 0.6 } }
    fun bloop() = play("bloop") { synth(160) { t -> sweep(t, 260.0, 760.0, 0.16) * exp(-t * 14) } }
    fun stamp() = play("stamp") { synth(110) { t -> (sin(2 * PI * 330 * t) + 0.5 * sin(2 * PI * 660 * t)) * exp(-t * 30) * 0.7 } }
    fun cheer() = play("cheer") {
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.5)
        synth(520) { t ->
            val i = (t / 0.11).toInt().coerceAtMost(notes.size - 1)
            val local = t - i * 0.11
            sin(2 * PI * notes[i] * t) * exp(-local * 9) * 0.8
        }
    }

    private fun sweep(t: Double, from: Double, to: Double, length: Double): Double {
        val k = (to - from) / length
        return sin(2 * PI * (from * t + k * t * t / 2))
    }

    private fun synth(ms: Int, wave: (Double) -> Double): ShortArray {
        val n = RATE * ms / 1000
        return ShortArray(n) { i ->
            val t = i.toDouble() / RATE
            val fade = if (i > n - 200) (n - i) / 200.0 else 1.0
            (wave(t) * fade * 0.45 * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
    }

    private fun play(name: String, make: () -> ShortArray) {
        if (!enabled) return
        runCatching {
            val track = tracks.getOrPut(name) {
                val data = make()
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(data.size * 2)
                    .build()
                    .also { it.write(data, 0, data.size) }
            }
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.reloadStaticData()
            track.play()
        }
    }
}
