package com.sudzy.carwash.scenes

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.sudzy.carwash.art.LogoArt
import com.sudzy.carwash.engine.Button
import com.sudzy.carwash.engine.ButtonGroup
import com.sudzy.carwash.engine.Col
import com.sudzy.carwash.engine.Ease
import com.sudzy.carwash.engine.Fonts
import com.sudzy.carwash.engine.Game
import com.sudzy.carwash.engine.Gfx
import com.sudzy.carwash.engine.PType
import com.sudzy.carwash.engine.Particles
import com.sudzy.carwash.engine.Scene
import com.sudzy.carwash.engine.Touch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Free daily prize wheel (coins only, no purchases). */
class SpinScene(game: Game) : Scene(game) {
    private val ui = ButtonGroup()
    private val fx = Particles(400)
    private val prizes = intArrayOf(50, 100, 25, 200, 75, 500, 40, 150)
    private val colors = intArrayOf(0xFFFF4F7A.toInt(), 0xFFFFC928.toInt(), 0xFF3FD17A.toInt(), 0xFF3FA9FF.toInt(), 0xFFA46BFF.toInt(), 0xFFFF8A2B.toInt(), 0xFF2FD2C4.toInt(), 0xFFFF6FB5.toInt())
    private var angle = 0f
    private var spinning = false
    private var spinT = 0f
    private var startAngle = 0f; private var endAngle = 0f
    private var won = -1
    private var wonT = 0f
    private var lastTick = 0
    private lateinit var spinBtn: Button
    private val cy get() = H * 0.46f
    private val R = 420f

    companion object {
        fun today() = System.currentTimeMillis() / 86_400_000L
        fun available(game: Game) = game.save.lastSpinDay != today()
    }

    override fun onEnter() {
        ui.add(backButton { if (!spinning) game.go(HomeScene(game)) })
        spinBtn = ui.add(Button(W / 2 - 280f, H - 420f, 560f, 190f, Col.LIME, 0xFF22C13A.toInt(), pulse = true, art = { c, _, _ ->
            Gfx.outlined(c, "SPIN!", 0f, 40f, 120f, Color.WHITE, 0xFF0F7A22.toInt())
        }) { spin() })
        spinBtn.visible = available(game)
    }

    private fun spin() {
        if (spinning || !available(game)) return
        game.save.lastSpinDay = today()
        spinBtn.visible = false
        won = Random.nextInt(prizes.size)
        val seg = 360f / prizes.size
        startAngle = angle % 360f
        // pointer is at the top (-90°); segment i spans [i*seg, (i+1)*seg) from angle 0
        val target = 270f - (won + 0.5f) * seg
        endAngle = startAngle + 360f * 6 + ((target - startAngle) % 360f + 360f) % 360f
        spinning = true; spinT = 0f
        game.audio.sfx("whoosh")
    }

    override fun update(dt: Float) {
        super.update(dt); ui.update(dt); fx.update(dt)
        if (spinning) {
            spinT += dt / 4.2f
            angle = startAngle + (endAngle - startAngle) * Ease.outCubic(spinT)
            val tick = (angle / (360f / prizes.size)).toInt()
            if (tick != lastTick) { lastTick = tick; game.audio.sfx("tick", 0.5f); game.vibrate(5, 40) }
            if (spinT >= 1f) {
                spinning = false; wonT = 0.0001f
                game.save.coins = game.save.coins + prizes[won]
                game.audio.sfx("tada"); game.vibrate(60, 200)
                fx.confetti(W, 120)
                fx.burst(PType.COIN, W / 2, cy, 24, 1300f, 34f, 1.4f, gravity = 1500f)
            }
        }
        if (wonT > 0f) wonT += dt
    }

    override fun onTouch(t: Touch): Boolean = ui.touch(t, game)
    override fun onBack(): Boolean { if (!spinning) game.go(HomeScene(game)); return true }

    private val path = Path()
    override fun draw(c: Canvas) {
        MenuBg.draw(c, W, H, time, 0xFFFFB3E0.toInt(), 0xFFFF6FB5.toInt(), 0xFF8A1E9E.toInt(), cy)
        Gfx.outlined(c, "DAILY SPIN", W / 2 + 40f, 150f, 110f, Col.YELLOW, 0xFF8C0F47.toInt())
        Gfx.pill(c, W / 2 - 170f, 220f, 340f, 100f, "${game.save.coins}") { cv, x, y, r -> Gfx.coin(cv, x, y, r) }
        val cx = W / 2
        c.drawCircle(cx, cy + 20f, R + 40f, Gfx.p(0x55000000))
        c.drawCircle(cx, cy, R + 40f, Gfx.vGrad(cy - R, cy + R, 0xFFFFE36B.toInt(), 0xFFFFB000.toInt()))
        for (i in 0 until 16) {
            val a = Math.toRadians((i * 22.5).toDouble()).toFloat()
            val on = ((time * 6).toInt() + i) % 2 == 0
            c.drawCircle(cx + cos(a) * (R + 20f), cy + sin(a) * (R + 20f), 12f, Gfx.p(if (on) Color.WHITE else 0xFFFF8A2B.toInt()))
        }
        c.save(); c.rotate(angle, cx, cy)
        val seg = 360f / prizes.size
        val oval = RectF(cx - R, cy - R, cx + R, cy + R)
        for (i in prizes.indices) {
            c.drawArc(oval, i * seg, seg, true, Gfx.p(colors[i]))
            c.drawArc(oval, i * seg, seg, true, Gfx.s(Color.WHITE, 6f))
            c.save(); c.rotate(i * seg + seg / 2, cx, cy)
            Gfx.coin(c, cx + R * 0.8f, cy, 34f)
            c.save(); c.rotate(90f, cx + R * 0.55f, cy)
            Gfx.outlined(c, "${prizes[i]}", cx + R * 0.55f, cy + 22f, 66f, Color.WHITE, Col.shade(colors[i], -0.5f), strokeMul = 0.12f, depth = 0.04f, tf = Fonts.title)
            c.restore(); c.restore()
        }
        c.restore()
        c.drawCircle(cx, cy, 90f, Gfx.vGrad(cy - 90f, cy + 90f, Color.WHITE, 0xFFDDE7FF.toInt()))
        c.drawCircle(cx, cy, 90f, Gfx.s(0xFFFFB000.toInt(), 10f))
        c.save(); c.translate(cx, cy + 6f); c.scale(0.48f, 0.48f); LogoArt.mascot(c, time); c.restore()
        // pointer
        path.reset(); path.moveTo(cx - 50f, cy - R - 70f); path.lineTo(cx + 50f, cy - R - 70f); path.lineTo(cx, cy - R + 30f); path.close()
        c.drawPath(path, Gfx.p(0xFFFF3B3B.toInt())); c.drawPath(path, Gfx.s(Color.WHITE, 8f))
        if (wonT > 0f) {
            val k = Ease.outElastic(Ease.win(wonT, 0f, 0.8f))
            c.save(); c.scale(k, k, W / 2, H - 330f)
            Gfx.outlined(c, "+${prizes[won]}", W / 2 - 40f, H - 290f, 150f, Col.YELLOW, 0xFF8A5A00.toInt())
            Gfx.coin(c, W / 2 + 220f, H - 340f, 60f)
            c.restore()
        } else if (!spinBtn.visible && !spinning) {
            Gfx.outlined(c, "Come back tomorrow!", W / 2, H - 320f, 66f, Color.WHITE, 0xFF8A1E9E.toInt(), strokeMul = 0.1f)
            Gfx.plain(c, "A new free spin every day", W / 2, H - 240f, 42f, 0xDDFFFFFF.toInt(), Paint.Align.CENTER)
        }
        fx.draw(c)
        ui.draw(c, time)
    }
}
