package com.sudzy.carwash.scenes

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.sudzy.carwash.art.Icon
import com.sudzy.carwash.art.IconArt
import com.sudzy.carwash.engine.Button
import com.sudzy.carwash.engine.ButtonGroup
import com.sudzy.carwash.engine.Col
import com.sudzy.carwash.engine.Ease
import com.sudzy.carwash.engine.Game
import com.sudzy.carwash.engine.Gfx
import com.sudzy.carwash.engine.Touch
import kotlin.math.sin
import kotlin.random.Random

/**
 * Settings popup: music / sound / vibration toggles plus review, share, support and privacy.
 * Anything that leaves the app sits behind a simple grown-ups-only gate (Families policy friendly).
 */
class SettingsOverlay(private val game: Game) {
    private var t = 0f
    private var closing = -1f
    var closed = false; private set
    private val ui = ButtonGroup()
    private val gate = ButtonGroup()
    private var gateAction: (() -> Unit)? = null
    private var gateA = 0; private var gateB = 0
    private var gateShake = 0f
    private val W get() = game.w
    private val H get() = game.h
    private val panelTop get() = H / 2 - 700f

    init { build() }

    private fun build() {
        ui.list.clear()
        val save = game.save
        val rows = listOf(
            Triple(Icon.MUSIC, "Music", 0xFFA46BFF.toInt()),
            Triple(Icon.SOUND, "Sound FX", 0xFFFF9D2B.toInt()),
            Triple(Icon.VIBE, "Vibration", 0xFF2FD2C4.toInt()),
        )
        rows.forEachIndexed { i, (icon, label, col) ->
            val y = panelTop + 190f + i * 170f
            ui.add(Button(110f, y, W - 220f, 140f, Color.WHITE, 0xFFF0F4FF.toInt(), art = { c, w, _ ->
                c.drawRoundRect(-w / 2 + 24f, -48f, -w / 2 + 120f, 48f, 26f, 26f, Gfx.p(col))
                IconArt.draw(c, icon, -w / 2 + 72f, 0f, 64f)
                Gfx.plain(c, label, -w / 2 + 150f, 16f, 54f, Col.INK, Paint.Align.LEFT, com.sudzy.carwash.engine.Fonts.title)
                val on = when (i) { 0 -> save.music; 1 -> save.sound; else -> save.vibration }
                val tx = w / 2 - 200f
                c.drawRoundRect(tx, -40f, tx + 170f, 40f, 40f, 40f, if (on) Gfx.vGrad(-40f, 40f, Col.LIME, 0xFF22C13A.toInt()) else Gfx.p(0xFFC3CBE0.toInt()))
                c.drawCircle(if (on) tx + 130f else tx + 40f, 0f, 32f, Gfx.p(Color.WHITE))
                Gfx.outlined(c, if (on) "ON" else "OFF", if (on) tx + 58f else tx + 112f, 12f, 32f, Color.WHITE, 0x55000000, strokeMul = 0.05f, depth = 0f)
            }) {
                when (i) {
                    0 -> { save.music = !save.music; game.audio.setMusicEnabled(save.music) }
                    1 -> { save.sound = !save.sound; game.audio.setSfxEnabled(save.sound) }
                    else -> { save.vibration = !save.vibration; game.vibrate(60) }
                }
            })
        }
        val by = panelTop + 720f
        ui.add(Button(110f, by, W - 220f, 170f, 0xFFFFE36B.toInt(), 0xFFFFB000.toInt(), pulse = true, art = { c, _, _ ->
            for (k in 0 until 5) Gfx.star(c, -120f + k * 60f, -34f, 26f, true)
            Gfx.outlined(c, "REVIEW THIS APP", 0f, 54f, 58f, Color.WHITE, 0xFFB05C00.toInt())
        }) { askGrownUp { game.platform.openStoreReview() } })
        ui.add(Button(110f, by + 210f, W - 220f, 120f, 0xFF5AA8FF.toInt(), 0xFF2D6CF0.toInt(), art = { c, w, _ ->
            IconArt.draw(c, Icon.SHARE, -w / 2 + 80f, 0f, 60f); Gfx.outlined(c, "Share With Friends", 30f, 16f, 50f, Color.WHITE, 0xFF1A3F9E.toInt(), strokeMul = 0.1f)
        }) { askGrownUp { game.platform.shareApp() } })
        ui.add(Button(110f, by + 370f, W - 220f, 120f, 0xFF2FD2C4.toInt(), 0xFF0D8F84.toInt(), art = { c, w, _ ->
            IconArt.draw(c, Icon.MAIL, -w / 2 + 80f, 0f, 60f); Gfx.outlined(c, "Contact Support", 30f, 16f, 50f, Color.WHITE, 0xFF086159.toInt(), strokeMul = 0.1f)
        }) { askGrownUp { game.platform.contactSupport() } })
        ui.add(Button(110f, by + 530f, W - 220f, 100f, 0xFFA3B0D6.toInt(), 0xFF7D8DB0.toInt(), art = { c, w, _ ->
            IconArt.draw(c, Icon.SHIELD, -w / 2 + 70f, 0f, 50f); Gfx.outlined(c, "Privacy Policy", 20f, 14f, 42f, Color.WHITE, 0xFF4A5680.toInt(), strokeMul = 0.1f)
        }) { askGrownUp { game.platform.openPrivacyPolicy() } })
        ui.add(Button(W - 190f, panelTop - 40f, 130f, 130f, 0xFFFF6B6B.toInt(), 0xFFE0243A.toInt(), round = true,
            art = { c, _, _ -> IconArt.draw(c, Icon.CLOSE, 0f, 0f, 64f) }) { close() })
    }

    private fun askGrownUp(action: () -> Unit) {
        gateAction = action
        gateA = Random.nextInt(3, 10); gateB = Random.nextInt(3, 10)
        val answer = gateA * gateB
        val options = mutableSetOf(answer)
        while (options.size < 4) options += (answer + Random.nextInt(-12, 13)).coerceAtLeast(1)
        gate.list.clear()
        options.shuffled().forEachIndexed { i, v ->
            val x = 150f + (i % 2) * (W / 2 - 110f); val y = H / 2 + 20f + (i / 2) * 200f
            gate.add(Button(x, y, W / 2 - 190f, 160f, 0xFF5AA8FF.toInt(), 0xFF2D6CF0.toInt(), art = { c, _, _ ->
                Gfx.outlined(c, "$v", 0f, 30f, 90f, Color.WHITE, 0xFF1A3F9E.toInt())
            }) {
                if (v == answer) { val a = gateAction; gateAction = null; a?.invoke() }
                else { gateShake = 1f; game.audio.sfx("boing"); askGrownUp(gateAction ?: {}) }
            })
        }
        gate.add(Button(W - 180f, H / 2 - 480f, 120f, 120f, 0xFFFF6B6B.toInt(), 0xFFE0243A.toInt(), round = true,
            art = { c, _, _ -> IconArt.draw(c, Icon.CLOSE, 0f, 0f, 60f) }) { gateAction = null })
    }

    fun close() { if (closing < 0f) { closing = 0f; game.audio.sfx("whoosh", 0.6f) } }

    fun update(dt: Float) {
        t += dt
        gateShake = (gateShake - dt * 3f).coerceAtLeast(0f)
        if (closing >= 0f) { closing += dt; if (closing > 0.25f) closed = true }
        ui.update(dt); gate.update(dt)
    }

    fun onTouch(e: Touch): Boolean {
        if (closing >= 0f) return true
        if (gateAction != null) { gate.touch(e, game); return true }
        ui.touch(e, game); return true
    }

    fun onBack(): Boolean { if (gateAction != null) { gateAction = null; return true }; return false }

    fun draw(c: Canvas) {
        val k = if (closing >= 0f) 1f - closing / 0.25f else Ease.win(t, 0f, 0.3f)
        c.drawRect(0f, 0f, W, H, Gfx.p(Col.alpha(0xFF061030.toInt(), 0.7f * k)))
        c.save()
        val sc = if (closing >= 0f) k else Ease.outBack(k, 2f)
        c.scale(sc, sc, W / 2, H / 2)
        val top = panelTop
        Gfx.roundRect(c, 70f, top + 20f, W - 70f, top + 1480f + 20f, 70f, Gfx.p(0xFF8AA3DC.toInt()))
        Gfx.roundRect(c, 70f, top, W - 70f, top + 1480f, 70f, Gfx.vGrad(top, top + 1480f, Color.WHITE, 0xFFE3ECFF.toInt()))
        Gfx.roundRect(c, 70f, top, W - 70f, top + 1480f, 70f, Gfx.s(Color.WHITE, 10f))
        Gfx.candy(c, W / 2 - 300f, top - 60f, W / 2 + 300f, top + 80f, 0xFF5AA8FF.toInt(), 0xFF2D6CF0.toInt(), 40f)
        Gfx.outlined(c, "SETTINGS", W / 2, top + 40f, 92f, Color.WHITE, 0xFF1A3F9E.toInt())
        ui.draw(c, t)
        Gfx.plain(c, "Sudzy! v1.0  •  Made with bubbles", W / 2, top + 1435f, 34f, 0xFF8C9BC4.toInt())
        c.restore()
        if (gateAction != null) drawGate(c)
    }

    private fun drawGate(c: Canvas) {
        c.drawRect(0f, 0f, W, H, Gfx.p(0xCC061030.toInt()))
        c.save(); c.translate(sin(gateShake * 40f) * 30f * gateShake, 0f)
        val top = H / 2 - 420f
        Gfx.roundRect(c, 90f, top, W - 90f, top + 900f, 70f, Gfx.vGrad(top, top + 900f, Color.WHITE, 0xFFE3ECFF.toInt()))
        Gfx.roundRect(c, 90f, top, W - 90f, top + 900f, 70f, Gfx.s(Color.WHITE, 10f))
        Gfx.outlined(c, "GROWN-UPS ONLY", W / 2, top + 140f, 76f, 0xFFFFC21A.toInt(), 0xFFB05C00.toInt())
        Gfx.plain(c, "Please answer to continue:", W / 2, top + 230f, 44f, Col.INK)
        Gfx.outlined(c, "$gateA  ×  $gateB  =  ?", W / 2, top + 360f, 100f, Col.CYAN, Col.NAVY)
        gate.draw(c, t)
        c.restore()
    }
}
