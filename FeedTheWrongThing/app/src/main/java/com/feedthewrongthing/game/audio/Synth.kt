package com.feedthewrongthing.game.audio

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/** Tiny additive / subtractive synthesizer used to create every sound in the game. */
object Synth {
    const val SR = 22050
    const val SINE = 0
    const val TRI = 1
    const val SQUARE = 2
    const val SAW = 3

    private const val TAU = PI * 2

    fun buf(seconds: Float) = FloatArray((seconds * SR).toInt() + 1)

    fun midi(n: Float): Float = (440.0 * 2.0.pow((n - 69.0) / 12.0)).toFloat()

    private fun wave(type: Int, ph: Double): Float {
        val s = sin(ph)
        return when (type) {
            TRI -> (2.0 / PI * kotlin.math.asin(s)).toFloat()
            SQUARE -> tanh(4.0 * s).toFloat() * 0.8f
            SAW -> {
                val x = (ph / TAU) % 1.0
                ((x * 2.0 - 1.0) * 0.7 + 0.3 * sin(ph * 2) * 0.5).toFloat()
            }
            else -> s.toFloat()
        }
    }

    /**
     * Adds a tone. Frequency glides exponentially from [f0] to [f1].
     * Envelope: linear attack, then (1-x)^[curve] decay. [trem] adds amplitude flutter.
     */
    fun tone(
        b: FloatArray, start: Float, dur: Float, f0: Float, f1: Float = f0, vol: Float = 0.5f,
        type: Int = SINE, curve: Float = 1.5f, attack: Float = 0.004f,
        vibHz: Float = 0f, vibDepth: Float = 0f, tremHz: Float = 0f, tremDepth: Float = 0f,
    ) {
        val s0 = (start * SR).toInt()
        val n = (dur * SR).toInt()
        var ph = 0.0
        val ratio = (f1 / f0).toDouble()
        for (i in 0 until n) {
            val idx = s0 + i
            if (idx < 0 || idx >= b.size) continue
            val t = i.toFloat() / SR
            val x = i.toDouble() / n
            var f = f0 * ratio.pow(x)
            if (vibHz > 0f) f *= 1.0 + vibDepth * sin(TAU * vibHz * t)
            ph += TAU * f / SR
            val att = if (t < attack) t / attack else 1f
            var env = att * (1.0 - x).pow(curve.toDouble()).toFloat()
            if (tremHz > 0f) env *= (1f - tremDepth * 0.5f * (1f + sin(TAU * tremHz * t).toFloat()))
            b[idx] += wave(type, ph) * env * vol
        }
    }

    /** Adds filtered noise. Low-pass coefficient glides from [lp0] to [lp1]. */
    fun noise(
        b: FloatArray, start: Float, dur: Float, vol: Float, lp0: Float = 0.5f, lp1: Float = lp0,
        curve: Float = 1.5f, swell: Boolean = false, seed: Int = 7,
    ) {
        val r = Random(seed)
        val s0 = (start * SR).toInt()
        val n = (dur * SR).toInt()
        var y = 0f
        for (i in 0 until n) {
            val idx = s0 + i
            if (idx < 0 || idx >= b.size) continue
            val x = i.toFloat() / n
            val a = lp0 + (lp1 - lp0) * x
            y += a * ((r.nextFloat() * 2f - 1f) - y)
            val env = if (swell) sin(PI * x).toFloat() else (1f - x).toDouble().pow(curve.toDouble()).toFloat()
            b[idx] += y * env * vol
        }
    }

    fun lowpass(b: FloatArray, a: Float) {
        var y = 0f
        for (i in b.indices) { y += a * (b[i] - y); b[i] = y }
    }

    /** Feedback echo; when [wrap] the tail folds back to the start so loops stay seamless. */
    fun echo(b: FloatArray, delaySec: Float, feedback: Float, mix: Float, wrap: Boolean) {
        val d = (delaySec * SR).toInt()
        val out = b.copyOf()
        val tap = FloatArray(b.size)
        for (pass in 0 until 2) {
            for (i in b.indices) {
                val j = i - d
                val prev = if (j >= 0) tap[j] else if (wrap) tap[j + b.size] else 0f
                tap[i] = b[i] + prev * feedback
            }
        }
        for (i in b.indices) {
            val j = i - d
            val prev = if (j >= 0) tap[j] else if (wrap) tap[j + b.size] else 0f
            out[i] = b[i] + prev * mix
        }
        System.arraycopy(out, 0, b, 0, b.size)
    }

    fun toPcm(b: FloatArray, peak: Float = 0.9f): ShortArray {
        var m = 1e-6f
        for (v in b) m = max(m, abs(v))
        val g = peak / m
        return ShortArray(b.size) { (b[it] * g * 32767f).toInt().coerceIn(-32767, 32767).toShort() }
    }

    fun writeWav(file: File, pcm: ShortArray) {
        val dataBytes = pcm.size * 2
        val bb = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray()); bb.putInt(36 + dataBytes); bb.put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray()); bb.putInt(16); bb.putShort(1); bb.putShort(1)
        bb.putInt(SR); bb.putInt(SR * 2); bb.putShort(2); bb.putShort(16)
        bb.put("data".toByteArray()); bb.putInt(dataBytes)
        for (s in pcm) bb.putShort(s)
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        FileOutputStream(tmp).use { it.write(bb.array()) }
        tmp.renameTo(file)
    }

    @Suppress("unused")
    fun decay(x: Float, k: Float) = exp(-x * k)
}

/** Every sound effect, each with its own recipe. */
enum class Sfx {
    POP, BOING, CHOMP, GULP, BURP, SNEEZE, WHOOSH, SPARKLE, CLICK, STAR, FANFARE, QUACK, GIGGLE,
    FREEZE, FLAME, HICCUP, SPLASH, BONK, POOF, SNORE, ALARM, TOOT, YAY, UNLOCK, BUBBLE, STINK,
    SQUEAK, SPIT, MUNCH, ROCKET, RAINBOW, DING, GROW, SHRINK, HEART, HULA, WOBBLE, MEH, DRUM;

    fun render(): FloatArray {
        val s = Synth
        return when (this) {
            POP -> s.buf(0.16f).also {
                s.tone(it, 0f, 0.13f, 950f, 260f, 0.9f, curve = 2.5f)
                s.noise(it, 0f, 0.02f, 0.3f, 0.6f)
            }
            BOING -> s.buf(0.6f).also {
                s.tone(it, 0f, 0.55f, 170f, 560f, 0.8f, curve = 1.4f, vibHz = 16f, vibDepth = 0.12f)
                s.tone(it, 0f, 0.5f, 340f, 1120f, 0.18f, curve = 2f, vibHz = 16f, vibDepth = 0.12f)
            }
            CHOMP -> s.buf(0.34f).also {
                for (k in 0..1) {
                    s.noise(it, k * 0.14f, 0.08f, 0.9f, 0.28f, 0.1f, curve = 3f, seed = k + 3)
                    s.tone(it, k * 0.14f, 0.09f, 150f, 70f, 0.7f, curve = 2f)
                }
            }
            GULP -> s.buf(0.3f).also {
                s.tone(it, 0f, 0.22f, 430f, 150f, 0.8f, vibHz = 30f, vibDepth = 0.05f)
                s.tone(it, 0.13f, 0.12f, 300f, 650f, 0.35f)
            }
            BURP -> s.buf(0.8f).also {
                s.tone(it, 0f, 0.75f, 125f, 82f, 0.8f, type = Synth.SAW, curve = 0.8f, attack = 0.03f,
                    vibHz = 26f, vibDepth = 0.09f, tremHz = 30f, tremDepth = 0.5f)
                s.noise(it, 0f, 0.7f, 0.25f, 0.08f, curve = 1f)
                s.lowpass(it, 0.3f)
            }
            SNEEZE -> s.buf(1.0f).also {
                s.tone(it, 0f, 0.22f, 330f, 480f, 0.45f, type = Synth.TRI, curve = 0.6f, attack = 0.04f)
                s.tone(it, 0.28f, 0.22f, 370f, 560f, 0.5f, type = Synth.TRI, curve = 0.6f, attack = 0.04f)
                s.noise(it, 0.58f, 0.34f, 1.0f, 0.9f, 0.3f, curve = 2f, seed = 11)
                s.tone(it, 0.58f, 0.25f, 720f, 280f, 0.35f, type = Synth.SQUARE, curve = 2f)
            }
            WHOOSH -> s.buf(0.4f).also { s.noise(it, 0f, 0.38f, 0.8f, 0.03f, 0.45f, swell = true, seed = 5) }
            SPARKLE -> s.buf(0.6f).also {
                val notes = floatArrayOf(1318f, 1568f, 1976f, 2637f)
                notes.forEachIndexed { i, f -> s.tone(it, i * 0.06f, 0.28f, f, f, 0.35f, curve = 3f) }
            }
            CLICK -> s.buf(0.06f).also { s.tone(it, 0f, 0.05f, 1800f, 1100f, 0.6f, curve = 3f) }
            STAR -> s.buf(0.8f).also {
                s.tone(it, 0f, 0.75f, 880f, 880f, 0.6f, curve = 2.2f)
                s.tone(it, 0f, 0.6f, 2428f, 2428f, 0.16f, curve = 3f)
                s.tone(it, 0f, 0.5f, 1760f, 1760f, 0.22f, curve = 2.5f)
            }
            FANFARE -> s.buf(1.3f).also {
                val a = floatArrayOf(523f, 659f, 784f)
                a.forEachIndexed { i, f -> s.tone(it, i * 0.12f, 0.2f, f, f, 0.32f, type = Synth.SQUARE, curve = 1.2f) }
                floatArrayOf(523f, 659f, 784f, 1046f).forEach { f ->
                    s.tone(it, 0.38f, 0.85f, f, f, 0.22f, type = Synth.TRI, curve = 1.4f)
                }
            }
            QUACK -> s.buf(0.48f).also {
                for (k in 0..1) s.tone(it, k * 0.22f, 0.17f, 540f, 380f, 0.7f, type = Synth.SAW, curve = 1.2f, attack = 0.01f)
                s.lowpass(it, 0.35f)
            }
            GIGGLE -> s.buf(0.65f).also {
                for (k in 0 until 6) {
                    val f = 760f + (k % 2) * 90f - k * 20f
                    s.tone(it, k * 0.095f, 0.085f, f, f + 120f, 0.45f, type = Synth.TRI, vibHz = 14f, vibDepth = 0.04f)
                }
            }
            FREEZE -> s.buf(0.75f).also {
                s.tone(it, 0f, 0.65f, 1600f, 650f, 0.35f, curve = 1.5f)
                s.noise(it, 0f, 0.6f, 0.3f, 0.95f, 0.7f, curve = 1.5f, seed = 9)
                for (k in 0 until 4) s.tone(it, 0.1f + k * 0.12f, 0.2f, 2400f + k * 300f, 2400f + k * 300f, 0.12f, curve = 3f)
            }
            FLAME -> s.buf(0.9f).also {
                s.noise(it, 0f, 0.85f, 0.9f, 0.1f, 0.18f, swell = true, seed = 21)
                val r = Random(4)
                for (k in 0 until 12) s.noise(it, r.nextFloat() * 0.8f, 0.012f, 0.6f, 0.9f, seed = k)
            }
            HICCUP -> s.buf(0.14f).also { s.tone(it, 0f, 0.11f, 420f, 950f, 0.7f, curve = 1.8f) }
            SPLASH -> s.buf(0.6f).also {
                s.noise(it, 0f, 0.48f, 0.9f, 0.6f, 0.08f, curve = 2f, seed = 13)
                val r = Random(2)
                for (k in 0 until 5) s.tone(it, 0.05f + r.nextFloat() * 0.4f, 0.06f, 500f, 1300f, 0.25f)
            }
            BONK -> s.buf(0.22f).also {
                s.tone(it, 0f, 0.2f, 250f, 90f, 0.85f, curve = 2f)
                s.tone(it, 0f, 0.05f, 900f, 900f, 0.25f, type = Synth.SQUARE, curve = 3f)
            }
            POOF -> s.buf(0.32f).also { s.noise(it, 0f, 0.3f, 0.6f, 0.18f, 0.06f, swell = true, seed = 17) }
            SNORE -> s.buf(1.1f).also {
                s.tone(it, 0f, 1.0f, 95f, 80f, 0.5f, type = Synth.SAW, curve = 0.5f, attack = 0.4f, tremHz = 22f, tremDepth = 0.4f)
                s.noise(it, 0f, 1.0f, 0.2f, 0.05f, swell = true)
                s.lowpass(it, 0.2f)
            }
            ALARM -> s.buf(0.75f).also {
                for (k in 0 until 8) {
                    val f = if (k % 2 == 0) 1046f else 1318f
                    s.tone(it, k * 0.085f, 0.07f, f, f, 0.35f, type = Synth.SQUARE, curve = 0.5f)
                }
            }
            TOOT -> s.buf(0.65f).also {
                s.tone(it, 0f, 0.6f, 349f, 349f, 0.55f, type = Synth.SQUARE, curve = 0.6f, attack = 0.03f, vibHz = 6f, vibDepth = 0.02f)
                s.tone(it, 0f, 0.6f, 698f, 698f, 0.18f, type = Synth.SAW, curve = 0.8f, attack = 0.03f)
                s.lowpass(it, 0.5f)
            }
            YAY -> s.buf(0.6f).also {
                s.tone(it, 0f, 0.55f, 330f, 450f, 0.6f, type = Synth.SAW, curve = 1f, attack = 0.03f, vibHz = 7f, vibDepth = 0.03f)
                s.tone(it, 0f, 0.55f, 660f, 900f, 0.25f, type = Synth.TRI, curve = 1f, attack = 0.03f)
                s.lowpass(it, 0.45f)
            }
            UNLOCK -> s.buf(1.0f).also {
                floatArrayOf(523f, 659f, 784f, 1046f, 1318f).forEachIndexed { i, f ->
                    s.tone(it, i * 0.07f, 0.35f, f, f, 0.32f, type = Synth.TRI, curve = 2f)
                }
                s.tone(it, 0.36f, 0.5f, 2093f, 2093f, 0.12f, curve = 3f)
            }
            BUBBLE -> s.buf(0.1f).also { s.tone(it, 0f, 0.08f, 500f, 1300f, 0.6f, curve = 1.5f) }
            STINK -> s.buf(0.7f).also {
                s.tone(it, 0f, 0.65f, 115f, 100f, 0.6f, type = Synth.SAW, curve = 0.9f, tremHz = 36f, tremDepth = 0.8f)
                s.noise(it, 0f, 0.6f, 0.2f, 0.1f)
                s.lowpass(it, 0.35f)
            }
            SQUEAK -> s.buf(0.5f).also { s.tone(it, 0f, 0.45f, 900f, 1550f, 0.45f, vibHz = 18f, vibDepth = 0.05f) }
            SPIT -> s.buf(0.16f).also {
                s.noise(it, 0f, 0.08f, 0.8f, 0.5f, curve = 3f)
                s.tone(it, 0f, 0.13f, 600f, 200f, 0.4f)
            }
            MUNCH -> s.buf(0.45f).also {
                s.tone(it, 0f, 0.42f, 210f, 255f, 0.55f, type = Synth.TRI, curve = 1f, attack = 0.03f, vibHz = 6f, vibDepth = 0.04f)
                s.tone(it, 0f, 0.42f, 420f, 510f, 0.15f, curve = 1f, attack = 0.03f)
            }
            ROCKET -> s.buf(1.1f).also {
                s.noise(it, 0f, 1.05f, 0.8f, 0.05f, 0.6f, swell = true, seed = 33)
                s.tone(it, 0f, 1.0f, 200f, 1200f, 0.22f, type = Synth.SAW, curve = 0.7f)
            }
            RAINBOW -> s.buf(0.9f).also {
                s.tone(it, 0f, 0.85f, 400f, 1600f, 0.45f, curve = 1f, tremHz = 12f, tremDepth = 0.4f)
                s.tone(it, 0f, 0.85f, 600f, 2400f, 0.15f, type = Synth.TRI, curve = 1f)
            }
            DING -> s.buf(0.45f).also {
                s.tone(it, 0f, 0.4f, 1046f, 1046f, 0.35f, type = Synth.SQUARE, curve = 2.5f)
                s.tone(it, 0.05f, 0.35f, 1568f, 1568f, 0.25f, curve = 2.5f)
            }
            GROW -> s.buf(0.95f).also {
                s.tone(it, 0f, 0.9f, 140f, 420f, 0.55f, type = Synth.TRI, curve = 0.8f, vibHz = 9f, vibDepth = 0.05f)
            }
            SHRINK -> s.buf(0.6f).also {
                s.tone(it, 0f, 0.55f, 950f, 300f, 0.45f, vibHz = 22f, vibDepth = 0.06f)
            }
            HEART -> s.buf(0.55f).also {
                s.tone(it, 0f, 0.25f, 660f, 660f, 0.4f, type = Synth.TRI, curve = 1.6f)
                s.tone(it, 0.18f, 0.35f, 880f, 880f, 0.4f, type = Synth.TRI, curve = 1.6f)
            }
            HULA -> s.buf(0.8f).also {
                for (k in 0 until 6) s.noise(it, k * 0.12f, 0.06f, 0.35f, 0.7f, curve = 2f, seed = k)
                s.tone(it, 0f, 0.75f, 330f, 330f, 0.3f, type = Synth.TRI, tremHz = 8f, tremDepth = 0.6f)
            }
            WOBBLE -> s.buf(0.8f).also {
                s.tone(it, 0f, 0.75f, 220f, 200f, 0.6f, vibHz = 9f, vibDepth = 0.25f)
            }
            MEH -> s.buf(0.4f).also {
                s.tone(it, 0f, 0.35f, 260f, 200f, 0.5f, type = Synth.TRI, curve = 1f, attack = 0.03f)
                s.lowpass(it, 0.4f)
            }
            DRUM -> s.buf(0.2f).also {
                s.tone(it, 0f, 0.18f, 130f, 50f, 0.9f, curve = 2f)
                s.noise(it, 0f, 0.03f, 0.3f, 0.5f)
            }
        }
    }
}

/** Background music tracks, composed note-by-note and rendered to seamless loops. */
enum class Music {
    HOME, PLAY, NIGHT;

    private class Note(val beat: Float, val midi: Int, val len: Float)
    private class Chord(val beat: Float, val root: Int, val minor: Boolean)

    fun render(): ShortArray {
        val bpm: Float
        val melody: List<Note>
        val chords: List<Chord>
        val drums: Boolean
        val transpose: Int
        when (this) {
            PLAY -> {
                bpm = 112f; drums = true; transpose = 0
                melody = notes(
                    "76,0,.5 79,.5,.5 84,1,1 79,2,.5 76,2.5,.5 79,3,1 " +
                        "81,4,.5 79,4.5,.5 76,5,1 72,6,.5 74,6.5,.5 76,7,1 " +
                        "77,8,.5 81,8.5,.5 84,9,1 81,10,.5 77,10.5,.5 81,11,1 " +
                        "79,12,.5 83,12.5,.5 86,13,1 83,14,1 79,15,1 " +
                        "76,16,.5 79,16.5,.5 84,17,.5 88,17.5,.5 86,18,1 84,19,1 " +
                        "81,20,.5 84,20.5,.5 88,21,1 86,22,.5 84,22.5,.5 81,23,1 " +
                        "77,24,.5 81,24.5,.5 84,25,1 83,26,.5 86,26.5,.5 79,27,1 " +
                        "84,28,1.5 79,29.5,.5 76,30,.5 74,30.5,.5 72,31,1",
                )
                chords = listOf(
                    Chord(0f, 48, false), Chord(4f, 45, true), Chord(8f, 41, false), Chord(12f, 43, false),
                    Chord(16f, 48, false), Chord(20f, 45, true), Chord(24f, 41, false), Chord(26f, 43, false),
                    Chord(28f, 48, false),
                )
            }
            HOME, NIGHT -> {
                bpm = if (this == HOME) 94f else 76f
                drums = this == HOME
                transpose = if (this == NIGHT) 5 else 0
                melody = notes(
                    "81,0,1 79,1,.5 77,1.5,.5 72,2,1 77,3,1 " +
                        "74,4,.5 77,4.5,.5 81,5,1.5 79,6.5,.5 77,7,1 " +
                        "82,8,1 81,9,.5 79,9.5,.5 77,10,1 74,11,1 " +
                        "72,12,.5 76,12.5,.5 79,13,1 84,14,1.5 " +
                        "77,16,.5 81,16.5,.5 84,17,1 81,18,.5 77,18.5,.5 79,19,1 " +
                        "77,20,1 76,21,.5 74,21.5,.5 69,22,1 74,23,1 " +
                        "77,24,1 79,25,.5 81,25.5,.5 79,26,1 76,27,1 " +
                        "77,28,2 72,30,1 77,31,1",
                )
                chords = listOf(
                    Chord(0f, 41, false), Chord(4f, 38, true), Chord(8f, 46, false), Chord(12f, 48, false),
                    Chord(16f, 41, false), Chord(20f, 38, true), Chord(24f, 46, false), Chord(26f, 48, false),
                    Chord(28f, 41, false),
                )
            }
        }
        val beats = 32f
        val spb = 60f / bpm
        val total = beats * spb
        val b = Synth.buf(total)
        val s = Synth
        // Chords: bass + marimba arpeggio
        for ((ci, c) in chords.withIndex()) {
            val end = if (ci + 1 < chords.size) chords[ci + 1].beat else beats
            val third = if (c.minor) 3 else 4
            var beat = c.beat
            while (beat < end - 0.01f) {
                val t = beat * spb
                val r = c.root + transpose
                val isDown = ((beat - c.beat) % 2f) < 0.01f
                val bassNote = if (isDown) r else r + 7
                s.tone(b, t, spb * 0.9f, s.midi(bassNote.toFloat()), s.midi(bassNote.toFloat()), 0.32f, type = Synth.TRI, curve = 1.2f, attack = 0.01f)
                val tones = intArrayOf(r + 12, r + 12 + third, r + 19, r + 24)
                for (k in 0 until 2) {
                    val n = tones[((beat * 2).toInt() + k) % tones.size]
                    val tt = t + k * spb * 0.5f
                    val f = s.midi((n + 12).toFloat())
                    s.tone(b, tt, spb * 0.45f, f, f, if (this == NIGHT) 0.1f else 0.12f, curve = 3f, attack = 0.002f)
                    s.tone(b, tt, spb * 0.2f, f * 4f, f * 4f, 0.02f, curve = 4f, attack = 0.002f)
                }
                beat += 1f
            }
        }
        // Melody
        for (n in melody) {
            val t = n.beat * spb
            val f = s.midi((n.midi + transpose).toFloat())
            val dur = n.len * spb * 0.95f
            if (this == NIGHT) {
                s.tone(b, t, dur + 0.4f, f * 2f, f * 2f, 0.22f, curve = 2.5f, attack = 0.002f)
                s.tone(b, t, dur, f * 4f, f * 4f, 0.04f, curve = 3f, attack = 0.002f)
            } else {
                s.tone(b, t, dur, f, f, 0.3f, type = Synth.TRI, curve = 0.9f, attack = 0.015f, vibHz = 5.5f, vibDepth = 0.008f)
                s.tone(b, t, dur * 0.6f, f * 2f, f * 2f, 0.06f, curve = 2f, attack = 0.005f)
            }
        }
        if (drums) {
            var beat = 0f
            while (beat < beats) {
                val t = beat * spb
                val inBar = beat % 4f
                if (inBar == 0f || inBar == 2f) {
                    s.tone(b, t, 0.13f, 120f, 45f, 0.45f, curve = 2f)
                }
                if (inBar == 1f || inBar == 3f) s.noise(b, t, 0.08f, 0.16f, 0.45f, curve = 2.5f, seed = beat.toInt())
                s.noise(b, t + spb * 0.5f, 0.03f, 0.07f, 0.95f, curve = 3f, seed = 100 + beat.toInt())
                s.noise(b, t, 0.025f, 0.05f, 0.95f, curve = 3f, seed = 200 + beat.toInt())
                beat += 1f
            }
        }
        s.echo(b, spb * 0.75f, 0.3f, 0.22f, wrap = true)
        return s.toPcm(b, 0.8f)
    }

    private fun notes(spec: String): List<Note> = spec.trim().split(Regex("\\s+")).map {
        val p = it.split(",")
        Note(p[1].toFloat(), p[0].toInt(), p[2].toFloat())
    }
}
