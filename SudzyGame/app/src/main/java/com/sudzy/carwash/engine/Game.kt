package com.sudzy.carwash.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import com.sudzy.carwash.scenes.SettingsOverlay
import com.sudzy.carwash.scenes.WashScene
import kotlin.math.sin

/**
 * The whole game lives in a virtual canvas that is always 1080 units wide;
 * height follows the device aspect ratio.
 */
class Game(val context: Context, val platform: Platform) {
    var w = 1080f; var h = 1920f; var scale = 1f
    /** Horizontal letterbox offset (px) used on tablets / landscape where the screen is wider than a phone. */
    private var offX = 0f
    private var pxW = 1080; private var pxH = 1920
    val save = Save(context)
    val audio: Audio get() = platform.audio

    var scene: Scene? = null; private set
    var overlay: SettingsOverlay? = null
    private var pending: Scene? = null
    private var wipeT = -1f
    private var shake = 0f
    private var shakeT = 0f
    var paused = false

    fun resize(pxW: Int, pxH: Int) {
        if (pxW <= 0 || pxH <= 0) return
        this.pxW = pxW; this.pxH = pxH
        val viewW = minOf(pxW.toFloat(), pxH * 0.62f)
        offX = (pxW - viewW) / 2f
        scale = viewW / 1080f
        w = 1080f; h = pxH / scale
    }

    fun start(first: Scene) {
        audio.setMusicEnabled(save.music); audio.setSfxEnabled(save.sound)
        scene = first; first.onEnter(); audio.music(first.music)
    }

    /** Switch scene behind a rising wall of foam. */
    fun go(next: Scene, wipe: Boolean = true) {
        if (!wipe) { swap(next); return }
        if (wipeT >= 0f) return
        pending = next; wipeT = 0f
        audio.sfx("whoosh", 0.8f)
    }

    private fun swap(next: Scene) {
        scene?.onExit(); scene = next; next.onEnter()
        audio.music(next.music)
    }

    fun shake(amount: Float) { shake = maxOf(shake, amount) }

    fun vibrate(ms: Long, strength: Int = 120) { if (save.vibration) platform.vibrate(ms, strength) }

    fun openSettings() { if (overlay == null) { overlay = SettingsOverlay(this) ; audio.sfx("pop") } }

    /** Every new level starts with an interstitial (except the very first wash, our tutorial). */
    fun startLevel(index: Int) {
        val launch = { go(WashScene(this, index.coerceIn(0, 29))) }
        if (save.levelsPlayed == 0) launch() else {
            audio.music(null)
            platform.showInterstitial { launch() }
        }
    }

    fun update(dtIn: Float) {
        if (paused) return
        val dt = dtIn.coerceIn(0f, 0.05f)
        scene?.update(dt)
        overlay?.let { it.update(dt); if (it.closed) overlay = null }
        if (wipeT >= 0f) {
            val before = wipeT
            wipeT += dt / WIPE
            if (before < 0.5f && wipeT >= 0.5f) pending?.let { swap(it); pending = null }
            if (wipeT >= 1f) wipeT = -1f
        }
        shakeT += dt
        shake = (shake - dt * 3f).coerceAtLeast(0f)
    }

    fun draw(c: Canvas) {
        if (offX > 0f) c.drawColor(0xFF0B1A4A.toInt())
        c.save()
        c.translate(offX, 0f)
        if (offX > 0f) c.clipRect(0f, 0f, w * scale, pxH.toFloat())
        c.scale(scale, scale)
        if (shake > 0f) c.translate(sin(shakeT * 70f) * shake * 18f, sin(shakeT * 53f) * shake * 12f)
        scene?.draw(c)
        overlay?.draw(c)
        if (wipeT >= 0f) drawWipe(c, wipeT)
        c.restore()
    }

    private val wipePath = Path()
    private fun drawWipe(c: Canvas, t: Float) {
        val top: Float; val bottom: Float
        if (t < 0.5f) { val k = Ease.inOutSine(t * 2); top = h + 220 - k * (h + 440); bottom = h + 600 }
        else { val k = Ease.inOutSine((t - 0.5f) * 2); top = -600f; bottom = h + 220 - k * (h + 440) }
        c.drawRect(0f, top, w, bottom, Gfx.vGrad(top, bottom, 0xFFEAF8FF.toInt(), 0xFFBFE6FF.toInt()))
        for (edge in floatArrayOf(top, bottom)) {
            var x = -40f; var i = 0
            while (x < w + 80) {
                val r = 70f + 40f * sin(i * 1.7f) + 20f * sin(i * 3.1f)
                c.drawCircle(x, edge + sin(i * 2.3f) * 30f, r, Gfx.p(0xFFF4FBFF.toInt()))
                c.drawCircle(x, edge + sin(i * 2.3f) * 30f, r, Gfx.s(0xFFA9DCFF.toInt(), 6f))
                x += r * 1.1f; i++
            }
        }
        val rng = Rng(7)
        repeat(40) { Gfx.bubble(c, rng.range(0f, w), rng.range(top, bottom.coerceAtMost(h + 100)), rng.range(14f, 60f), 0.9f) }
        if (t > 0.35f && t < 0.65f) {
            val a = 1f - kotlin.math.abs(t - 0.5f) / 0.15f
            Gfx.outlined(c, "SPLOOSH!", w / 2, h / 2, 120f, Col.CYAN, Col.NAVY, alpha = a)
        }
    }

    fun touch(action: Int, px: Float, py: Float) {
        if (wipeT >= 0f) return
        val t = Touch(action, (px - offX) / scale, py / scale)
        overlay?.let { it.onTouch(t); return }
        scene?.onTouch(t)
    }

    /** @return false when the app should close. */
    fun back(): Boolean {
        overlay?.let { if (!it.onBack()) it.close(); return true }
        if (wipeT >= 0f) return true
        return scene?.onBack() ?: false
    }

    companion object { const val WIPE = 0.9f }
}
