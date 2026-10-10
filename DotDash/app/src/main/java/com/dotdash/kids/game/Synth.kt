package com.dotdash.kids.game

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

// Every sound in the game is synthesised here into 16-bit PCM, so the app
// ships no audio files. The Android side only plays the samples.

enum class Wave { SINE, TRIANGLE, SQUARE, SAW }
enum class FilterType { LOWPASS, BANDPASS, HIGHPASS }

class Filter(val type: FilterType, val freq: Float, val q: Float = 1f)

private class Biquad(private val rate: Int, private val type: FilterType, private val q: Float) {
    private var b0 = 1f; private var b1 = 0f; private var b2 = 0f; private var a1 = 0f; private var a2 = 0f
    private var x1 = 0f; private var x2 = 0f; private var y1 = 0f; private var y2 = 0f

    fun set(freq: Float) {
        val f = freq.coerceIn(20f, rate * 0.45f)
        val w = 2 * PI.toFloat() * f / rate
        val alpha = sin(w) / (2 * q)
        val cw = cos(w)
        val a0: Float
        when (type) {
            FilterType.LOWPASS -> { b0 = (1 - cw) / 2; b1 = 1 - cw; b2 = (1 - cw) / 2 }
            FilterType.HIGHPASS -> { b0 = (1 + cw) / 2; b1 = -(1 + cw); b2 = (1 + cw) / 2 }
            FilterType.BANDPASS -> { b0 = alpha; b1 = 0f; b2 = -alpha }
        }
        a0 = 1 + alpha
        a1 = -2 * cw / a0
        a2 = (1 - alpha) / a0
        b0 /= a0; b1 /= a0; b2 /= a0
    }

    fun run(x: Float): Float {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1; x1 = x; y2 = y1; y1 = y
        return y
    }
}

/** Offline renderer with the same building blocks the sounds were designed with. */
class Renderer(private val rate: Int) {
    private var buf = FloatArray(rate)
    private var used = 0
    private val rnd = Random(7)

    private fun ensure(len: Int) {
        if (len > buf.size) buf = buf.copyOf(len + rate / 2)
        if (len > used) used = len
    }

    // Exponential attack/decay envelope, like Web Audio's exponential ramps.
    private fun env(t: Float, dur: Float, vol: Float, attack: Float): Float = when {
        t < attack -> 0.0001f * (vol / 0.0001f).pow(t / attack)
        t < dur -> vol * (0.0001f / vol).pow((t - attack) / (dur - attack))
        else -> 0f
    }

    fun tone(
        freq: Float, dur: Float, wave: Wave = Wave.SINE, vol: Float = 0.3f, at: Float = 0f, attack: Float = 0.01f,
        glide: Float? = null, glideTime: Float? = null, vibrato: Float = 0f, vibratoRate: Float = 6f, filter: Filter? = null,
    ) {
        val start = (at * rate).toInt()
        val n = (dur * rate).toInt()
        ensure(start + n)
        val bq = filter?.let { Biquad(rate, it.type, it.q).apply { set(it.freq) } }
        val gt = glideTime ?: dur
        var phase = 0f
        for (i in 0 until n) {
            val t = i.toFloat() / rate
            var f = if (glide != null) freq * (glide / freq).pow(min(t, gt) / gt) else freq
            if (vibrato > 0f) f += vibrato * sin(2 * PI.toFloat() * vibratoRate * t)
            phase += f / rate
            phase -= floor(phase)
            var s = when (wave) {
                Wave.SINE -> sin(2 * PI.toFloat() * phase)
                Wave.TRIANGLE -> 4 * abs(phase - 0.5f) - 1
                Wave.SQUARE -> if (phase < 0.5f) 0.8f else -0.8f
                Wave.SAW -> (2 * phase - 1) * 0.8f
            }
            if (bq != null) s = bq.run(s)
            buf[start + i] += s * env(t, dur, vol, attack)
        }
    }

    fun noise(
        dur: Float, at: Float = 0f, vol: Float = 0.3f, type: FilterType = FilterType.BANDPASS, freq: Float = 1000f,
        to: Float? = null, q: Float = 1f, attack: Float = 0.01f,
    ) {
        val start = (at * rate).toInt()
        val n = (dur * rate).toInt()
        ensure(start + n)
        val bq = Biquad(rate, type, q)
        bq.set(freq)
        for (i in 0 until n) {
            val t = i.toFloat() / rate
            if (to != null && i % 64 == 0) bq.set(freq * (to / freq).pow(t / dur))
            val s = bq.run(rnd.nextFloat() * 2 - 1)
            buf[start + i] += s * env(t, dur, vol, attack)
        }
    }

    fun bell(freq: Float, at: Float = 0f, vol: Float = 0.25f) {
        tone(freq, 0.6f, Wave.SINE, vol, at)
        tone(freq * 2, 0.35f, Wave.TRIANGLE, vol * 0.35f, at)
        tone(freq * 3.01f, 0.2f, Wave.SINE, vol * 0.15f, at)
    }

    /** Soft-limited 16-bit PCM. */
    fun toPcm(gain: Float = 1f): ShortArray = ShortArray(used) { (tanh(buf[it] * gain * 1.3f) * 30000).toInt().toShort() }

    /** Folds everything past [len] samples back onto the start, for seamless loops. */
    fun toLoop(len: Int, gain: Float): ShortArray {
        ensure(len)
        val out = FloatArray(len)
        for (i in 0 until used) out[i % len] += buf[i]
        return ShortArray(len) { (tanh(out[it] * gain * 1.3f) * 30000).toInt().toShort() }
    }
}

private val SCALE = intArrayOf(0, 2, 4, 7, 9) // major pentatonic: every note sounds good together
private fun noteFreq(semis: Int, base: Float = 392f) = base * 2f.pow(semis / 12f)

/** Builds and caches every sound effect by name. */
class SoundBank(private val rate: Int) {
    private val cache = HashMap<String, ShortArray>()

    @Synchronized
    fun get(key: String): ShortArray = cache.getOrPut(key) { render(key) }

    private fun render(key: String): ShortArray {
        val r = Renderer(rate)
        val parts = key.split(':')
        when (parts[0]) {
            // Each dot plays the next note up, so the picture plays a little tune.
            "dot" -> {
                val step = parts[1].toInt() % 11
                r.bell(noteFreq(SCALE[step % 5] + 12 * (step / 5)), 0f, 0.28f)
            }
            "wrong" -> r.tone(320f, 0.32f, vol = 0.3f, glide = 140f, vibrato = 30f, vibratoRate = 18f)
            "pop" -> r.tone(500f, 0.09f, vol = 0.25f, glide = 900f)
            "tap" -> r.tone(700f, 0.07f, Wave.TRIANGLE, 0.18f, glide = 1100f)
            "whoosh" -> r.noise(0.5f, vol = 0.25f, freq = 300f, to = 3000f, q = 2f)
            "sparkle" -> sparkle(r, 0f)
            "star" -> r.bell(noteFreq(intArrayOf(0, 4, 7)[parts[1].toInt()] + 12, 523f), 0f, 0.3f)
            "fanfare" -> fanfare(r)
            "unlock" -> {
                r.tone(300f, 0.12f, Wave.SQUARE, 0.12f, glide = 900f, filter = Filter(FilterType.LOWPASS, 2000f))
                r.noise(0.18f, at = 0.08f, vol = 0.15f, type = FilterType.HIGHPASS, freq = 4000f)
                sparkle(r, 0.15f)
            }
            "gift" -> {
                r.noise(0.3f, vol = 0.2f, freq = 1500f, to = 5000f)
                fanfare(r)
            }
            "bye" -> listOf(7, 4, 0).forEachIndexed { k, s -> r.bell(noteFreq(s, 523f), k * 0.14f, 0.22f) }
            "voice" -> voice(r, parts[1], parts.getOrNull(2)?.toFloatOrNull() ?: 1f)
        }
        return r.toPcm()
    }

    private fun sparkle(r: Renderer, at: Float) =
        listOf(0, 4, 7, 12, 16).forEachIndexed { k, s -> r.bell(noteFreq(s + 12, 523f), at + k * 0.05f, 0.12f) }

    private fun fanfare(r: Renderer) {
        listOf(0, 4, 7, 12).forEachIndexed { k, s ->
            r.tone(noteFreq(s, 523f), 0.35f, Wave.TRIANGLE, 0.2f, k * 0.11f)
            r.tone(noteFreq(s, 262f), 0.35f, Wave.SINE, 0.15f, k * 0.11f)
        }
        r.tone(noteFreq(12, 523f), 0.9f, Wave.TRIANGLE, 0.22f, 0.46f, vibrato = 6f)
    }

    // The creature's own voice when the picture comes alive.
    private fun voice(r: Renderer, name: String, p: Float) {
        when (name) {
            "roar" -> {
                r.tone(140 * p, 1.1f, Wave.SAW, 0.35f, attack = 0.08f, glide = 55 * p, vibrato = 12f, vibratoRate = 22f, filter = Filter(FilterType.LOWPASS, 900f))
                r.tone(147 * p, 1.1f, Wave.SAW, 0.25f, attack = 0.08f, glide = 60 * p, filter = Filter(FilterType.LOWPASS, 700f))
                r.noise(1.0f, vol = 0.3f, freq = 500 * p, to = 200f, attack = 0.08f, q = 0.7f)
            }
            "growl" -> {
                r.tone(110 * p, 0.7f, Wave.SAW, 0.3f, attack = 0.05f, glide = 80 * p, vibrato = 15f, vibratoRate = 28f, filter = Filter(FilterType.LOWPASS, 600f))
                r.noise(0.6f, vol = 0.2f, freq = 300 * p, attack = 0.05f)
            }
            "trumpet" -> r.tone(300 * p, 0.9f, Wave.SAW, 0.28f, attack = 0.04f, glide = 560 * p, glideTime = 0.3f, vibrato = 18f, vibratoRate = 7f, filter = Filter(FilterType.BANDPASS, 1400 * p, 1.5f))
            "meow" -> {
                r.tone(500f, 0.6f, Wave.TRIANGLE, 0.3f, glide = 850f, glideTime = 0.25f, filter = Filter(FilterType.BANDPASS, 1200f, 2f))
                r.tone(850f, 0.35f, Wave.TRIANGLE, 0.2f, 0.25f, glide = 450f, filter = Filter(FilterType.BANDPASS, 1000f, 2f))
            }
            "woof" -> listOf(0f, 0.28f).forEach {
                r.tone(230f, 0.16f, Wave.SQUARE, 0.22f, it, glide = 140f, filter = Filter(FilterType.LOWPASS, 900f))
                r.noise(0.12f, at = it, vol = 0.2f, freq = 600f)
            }
            "quack" -> listOf(0f, 0.25f).forEach { r.tone(320f, 0.18f, Wave.SAW, 0.22f, it, glide = 220f, filter = Filter(FilterType.BANDPASS, 1300f, 3f)) }
            "oink" -> listOf(0f, 0.22f).forEach { r.tone(180f, 0.16f, Wave.SQUARE, 0.2f, it, glide = 130f, vibrato = 25f, vibratoRate = 30f, filter = Filter(FilterType.LOWPASS, 700f)) }
            "hoot" -> listOf(0f, 0.4f).forEach { r.tone(430f, 0.32f, Wave.SINE, 0.3f, it, attack = 0.05f, glide = 370f) }
            "squeak" -> listOf(0f, 0.15f, 0.3f).forEach { r.tone(1300f, 0.1f, Wave.SINE, 0.2f, it, glide = 1900f) }
            "neigh" -> r.tone(520f, 0.8f, Wave.SAW, 0.2f, glide = 380f, vibrato = 60f, vibratoRate = 16f, filter = Filter(FilterType.BANDPASS, 1200f, 2f))
            "ooh" -> listOf(0f, 0.18f, 0.36f, 0.5f).forEachIndexed { k, w ->
                r.tone(if (k < 2) 320f else 520f, 0.16f, Wave.TRIANGLE, 0.22f, w, glide = if (k < 2) 600f else 800f)
            }
            "bubble" -> bubble(r)
            "hiss" -> r.noise(0.9f, vol = 0.2f, type = FilterType.HIGHPASS, freq = 4000f, attack = 0.1f)
            "twinkle" -> listOf(12, 16, 19, 24, 19, 24).forEachIndexed { k, s -> r.bell(noteFreq(s, 523f), k * 0.08f, 0.12f) }
            "vroom" -> {
                r.tone(70 * p, 1.0f, Wave.SAW, 0.3f, glide = 260 * p, glideTime = 0.7f, vibrato = 10f, vibratoRate = 30f, filter = Filter(FilterType.LOWPASS, 1200f))
                r.noise(0.8f, vol = 0.12f, freq = 400f, to = 1500f)
            }
            "honk" -> listOf(0f, 0.3f).forEach {
                r.tone(350 * p, 0.22f, Wave.SQUARE, 0.14f, it, filter = Filter(FilterType.LOWPASS, 1800f))
                r.tone(440 * p, 0.22f, Wave.SQUARE, 0.12f, it, filter = Filter(FilterType.LOWPASS, 1800f))
            }
            "choo" -> listOf(0f, 0.45f).forEach {
                r.tone(620f, 0.35f, Wave.SINE, 0.22f, it, attack = 0.04f)
                r.tone(780f, 0.35f, Wave.SINE, 0.18f, it, attack = 0.04f)
                r.noise(0.3f, at = it, vol = 0.1f, freq = 2500f)
            }
            "whoosh" -> r.noise(0.9f, vol = 0.28f, freq = 250f, to = 3500f, q = 1.5f, attack = 0.2f)
            "blastoff" -> {
                r.noise(1.3f, vol = 0.3f, type = FilterType.LOWPASS, freq = 200f, to = 3000f, attack = 0.2f)
                r.tone(150f, 1.2f, Wave.SAW, 0.12f, glide = 1200f, filter = Filter(FilterType.LOWPASS, 1500f))
            }
            "whirr" -> repeat(10) { r.noise(0.07f, at = it * 0.08f, vol = 0.22f, type = FilterType.LOWPASS, freq = 500f) }
            "siren" -> repeat(4) { r.tone((if (it % 2 == 1) 900f else 650f) * p, 0.28f, Wave.TRIANGLE, 0.2f, it * 0.28f, attack = 0.03f) }
            "beam" -> r.tone(300f, 1.1f, Wave.SINE, 0.25f, glide = 1500f, vibrato = 40f, vibratoRate = 9f)
            "horn" -> {
                r.tone(140f, 1.0f, Wave.SAW, 0.25f, attack = 0.1f, filter = Filter(FilterType.LOWPASS, 700f))
                r.tone(176f, 1.0f, Wave.SAW, 0.15f, attack = 0.1f, filter = Filter(FilterType.LOWPASS, 700f))
            }
            "jingle" -> listOf(7, 12, 11, 12, 16, 12).forEachIndexed { k, s -> r.bell(noteFreq(s, 523f), k * 0.13f, 0.15f) }
            "ding" -> {
                r.bell(noteFreq(12, 523f), 0f, 0.3f)
                r.bell(noteFreq(7, 523f), 0.3f, 0.3f)
            }
            "splash" -> {
                r.noise(0.5f, vol = 0.25f, type = FilterType.HIGHPASS, freq = 1500f, attack = 0.02f)
                bubble(r)
            }
            "peep" -> listOf(0f, 0.14f, 0.28f).forEach { r.tone(1500f, 0.1f, Wave.SINE, 0.2f, it, glide = 2200f) }
            "rumble" -> {
                r.noise(1.4f, vol = 0.4f, type = FilterType.LOWPASS, freq = 160f, attack = 0.15f)
                r.tone(55f, 1.3f, Wave.SAW, 0.15f, vibrato = 4f, vibratoRate = 9f, filter = Filter(FilterType.LOWPASS, 300f))
            }
            "screech" -> r.tone(900 * p, 0.6f, Wave.SAW, 0.16f, glide = 1500 * p, glideTime = 0.2f, vibrato = 50f, vibratoRate = 20f, filter = Filter(FilterType.BANDPASS, 1800f, 2f))
            "tweet" -> listOf(0f, 0.13f, 0.26f).forEach { r.tone(2400f, 0.1f, Wave.SINE, 0.15f, it, glide = 3400f) }
            else -> listOf(0, 4, 7, 12).forEachIndexed { k, s -> r.tone(noteFreq(s, 523f), 0.15f, Wave.TRIANGLE, 0.2f, k * 0.08f) }
        }
    }

    private fun bubble(r: Renderer) {
        val rnd = Random(3)
        repeat(6) { r.tone(300 + rnd.nextFloat() * 300, 0.1f, Wave.SINE, 0.22f, it * 0.09f, glide = 900 + rnd.nextFloat() * 600) }
    }

    /** Background music: a gentle plucked I–V–vi–IV loop, one key per world. */
    fun music(world: Int): ShortArray = cache.getOrPut("music:$world") {
        val prog = listOf(intArrayOf(0, 4, 7), intArrayOf(7, 11, 14), intArrayOf(9, 12, 16), intArrayOf(5, 9, 12))
        val key = intArrayOf(0, 2, -3).getOrElse(world) { 0 }
        val stepSec = 60f / 100f / 2f // eighth notes at 100 bpm
        val pattern = intArrayOf(0, 1, 2, 1, 0, 1, 2, 1)
        val r = Renderer(rate)
        for (step in 0 until 32) {
            val chord = prog[step / 8]
            val beat = step % 8
            val at = step * stepSec
            val f = noteFreq(chord[pattern[beat]] + key, 262f)
            r.tone(f, 0.5f, Wave.TRIANGLE, 0.09f, at, attack = 0.005f)
            r.tone(f * 2, 0.25f, Wave.SINE, 0.027f, at, attack = 0.005f)
            if (beat == 0 || beat == 4) r.tone(noteFreq(chord[0] + key - 12, 131f), 0.6f, Wave.SINE, 0.12f, at)
        }
        r.toLoop((32 * stepSec * rate).toInt(), 0.32f)
    }
}

/** Mixes sound effects and the looping music into one output stream. */
class Mixer {
    private class Voice(val data: ShortArray, var pos: Int = 0)

    private val voices = ArrayList<Voice>()
    private var music: ShortArray? = null
    private var musicPos = 0

    @Synchronized
    fun play(data: ShortArray) {
        if (voices.size > 12) voices.removeAt(0)
        voices.add(Voice(data))
    }

    @Synchronized
    fun setMusic(data: ShortArray?) {
        if (data !== music) musicPos = 0
        music = data
    }

    @Synchronized
    fun mix(out: ShortArray) {
        val m = music
        for (i in out.indices) {
            var s = 0f
            if (m != null && m.isNotEmpty()) {
                s += m[musicPos]
                musicPos = (musicPos + 1) % m.size
            }
            for (v in voices) if (v.pos < v.data.size) s += v.data[v.pos++]
            out[i] = (tanh(s / 32768f) * 32000f).toInt().toShort()
        }
        voices.removeAll { it.pos >= it.data.size }
    }
}
