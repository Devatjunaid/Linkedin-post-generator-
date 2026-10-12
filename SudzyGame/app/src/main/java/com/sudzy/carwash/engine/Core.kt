package com.sudzy.carwash.engine

import android.graphics.Canvas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

object Ease {
    fun outBack(t: Float, s: Float = 1.7f): Float { val u = t.coerceIn(0f, 1f) - 1; return u * u * ((s + 1) * u + s) + 1 }
    fun outElastic(t: Float): Float {
        val u = t.coerceIn(0f, 1f)
        if (u == 0f || u == 1f) return u
        return (2.0.pow(-10.0 * u) * sin((u * 10 - 0.75) * (2 * PI) / 3) + 1).toFloat()
    }
    fun outCubic(t: Float): Float { val u = 1 - t.coerceIn(0f, 1f); return 1 - u * u * u }
    fun inOutSine(t: Float): Float = (-(cos(PI * t.coerceIn(0f, 1f)) - 1) / 2).toFloat()
    fun outBounce(t: Float): Float {
        var x = t.coerceIn(0f, 1f); val n = 7.5625f; val d = 2.75f
        return when {
            x < 1 / d -> n * x * x
            x < 2 / d -> { x -= 1.5f / d; n * x * x + 0.75f }
            x < 2.5 / d -> { x -= 2.25f / d; n * x * x + 0.9375f }
            else -> { x -= 2.625f / d; n * x * x + 0.984375f }
        }
    }
    /** progress of [t] inside window [a, a+len] */
    fun win(t: Float, a: Float, len: Float): Float = ((t - a) / len).coerceIn(0f, 1f)
}

/** Small deterministic RNG so art is stable between frames. */
class Rng(seed: Long) {
    private var s = seed xor 0x5DEECE66DL
    fun next(): Float { s = (s * 6364136223846793005L + 1442695040888963407L); return ((s ushr 40) and 0xFFFFFF).toFloat() / 0x1000000 }
    fun range(a: Float, b: Float) = a + (b - a) * next()
    fun int(n: Int) = (next() * n).toInt().coerceAtMost(n - 1)
    fun chance(p: Float) = next() < p
}

data class Touch(val action: Int, val x: Float, val y: Float) {
    companion object { const val DOWN = 0; const val MOVE = 1; const val UP = 2 }
}

/** Everything the game needs from the outside world (ads, links, audio, haptics). */
interface Platform {
    val audio: Audio
    fun vibrate(ms: Long, strength: Int = 120)
    fun showInterstitial(onDone: () -> Unit)
    fun openStoreReview()
    fun shareApp()
    fun contactSupport()
    fun openPrivacyPolicy()
    fun exitApp()
}

interface Audio {
    fun sfx(name: String, volume: Float = 1f, rate: Float = 1f)
    fun startLoop(name: String)
    fun stopLoop(name: String)
    fun music(track: String?)
    fun setMusicEnabled(on: Boolean)
    fun setSfxEnabled(on: Boolean)
}

object NoAudio : Audio {
    override fun sfx(name: String, volume: Float, rate: Float) {}
    override fun startLoop(name: String) {}
    override fun stopLoop(name: String) {}
    override fun music(track: String?) {}
    override fun setMusicEnabled(on: Boolean) {}
    override fun setSfxEnabled(on: Boolean) {}
}

abstract class Scene(val game: Game) {
    var time = 0f
    open val music: String? = "menu"
    open fun onEnter() {}
    open fun onExit() {}
    open fun update(dt: Float) { time += dt }
    abstract fun draw(c: Canvas)
    open fun onTouch(t: Touch): Boolean = false
    /** @return true if handled */
    open fun onBack(): Boolean = false
    val W get() = game.w
    val H get() = game.h
}
