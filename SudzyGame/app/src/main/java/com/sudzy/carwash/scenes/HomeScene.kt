package com.sudzy.carwash.scenes

import android.graphics.Canvas
import android.graphics.Color
import com.sudzy.carwash.art.CarArt
import com.sudzy.carwash.art.Icon
import com.sudzy.carwash.art.IconArt
import com.sudzy.carwash.art.LogoArt
import com.sudzy.carwash.engine.Button
import com.sudzy.carwash.engine.ButtonGroup
import com.sudzy.carwash.engine.Col
import com.sudzy.carwash.engine.Ease
import com.sudzy.carwash.engine.Game
import com.sudzy.carwash.engine.Gfx
import com.sudzy.carwash.engine.PType
import com.sudzy.carwash.engine.Particles
import com.sudzy.carwash.engine.Scene
import com.sudzy.carwash.engine.Touch
import com.sudzy.carwash.game.Levels
import kotlin.math.sin

class HomeScene(game: Game) : Scene(game) {
    private val ui = ButtonGroup()
    private val fx = Particles(300)
    private val showcase = Levels.get((game.save.unlocked - 1).coerceAtLeast(0)).let {
        if (game.save.unlocked == 0) com.sudzy.carwash.art.CarModel(com.sudzy.carwash.art.CarType.SEDAN, Col.RED, Color.WHITE) else it.model()
    }
    private var hop = 0f
    private val carY get() = H * 0.555f

    override fun onEnter() {
        val spinReady = SpinScene.available(game)
        ui.add(Button(W / 2 - 300f, H - 560f, 600f, 200f, Col.LIME, 0xFF22C13A.toInt(), pulse = true, art = { c, _, _ ->
            IconArt.draw(c, Icon.PLAY, -150f, 0f, 100f)
            Gfx.outlined(c, "PLAY", 40f, 40f, 120f, Color.WHITE, 0xFF0F7A22.toInt())
        }) { game.go(MapScene(game)) })
        val labels = listOf(Triple(Icon.GARAGE, "Garage", 0xFFFF9D2B.toInt()), Triple(Icon.SPIN, "Spin", 0xFFFF4F7A.toInt()), Triple(Icon.STAR, "Road Trip", 0xFF2FD2C4.toInt()))
        val bw = 280f; val gap = (W - bw * 3) / 4
        labels.forEachIndexed { i, (icon, text, col) ->
            ui.add(Button(gap + i * (bw + gap), H - 300f, bw, 220f, Col.shade(col, 0.15f), Col.shade(col, -0.2f), badge = i == 1 && spinReady, art = { c, _, _ ->
                IconArt.draw(c, icon, 0f, -24f, 100f)
                Gfx.outlined(c, text, 0f, 72f, 46f, Color.WHITE, Col.shade(col, -0.5f), strokeMul = 0.1f, depth = 0.04f)
            }) {
                when (i) { 0 -> game.go(GarageScene(game)); 1 -> game.go(SpinScene(game)); else -> game.go(MapScene(game)) }
            })
        }
        ui.add(Button(W - 160f, 40f, 120f, 120f, 0xFF5AA8FF.toInt(), 0xFF2D6CF0.toInt(), round = true,
            art = { c, _, _ -> IconArt.draw(c, Icon.GEAR, 0f, 0f, 76f) }) { game.openSettings() })
        ui.list.forEachIndexed { i, b -> b.appear = -i * 0.12f }
    }

    override fun update(dt: Float) {
        super.update(dt)
        ui.list.forEach { it.appear = (it.appear + dt * 2.5f).coerceAtMost(1f) }
        ui.update(dt); fx.update(dt)
        hop = (hop - dt * 2f).coerceAtLeast(0f)
        if ((time * 10).toInt() % 7 == 0 && Math.random() < 0.3) {
            fx.spawn(PType.SPARKLE, W / 2 + (Math.random().toFloat() - 0.5f) * 800f, carY + (Math.random().toFloat() - 0.5f) * 250f, 0f, -30f, 20f + Math.random().toFloat() * 16f, 0.9f, if (Math.random() < 0.5) Color.WHITE else 0xFFFFF6B0.toInt())
        }
    }

    override fun onTouch(t: Touch): Boolean {
        if (ui.touch(t, game)) return true
        if (t.action == Touch.UP && t.y in carY - 200f..carY + 200f) {
            hop = 1f; game.audio.sfx("honk"); game.vibrate(20)
            fx.burst(PType.BUBBLE, t.x, t.y, 10, 500f, 28f, 1.2f)
            return true
        }
        return false
    }

    override fun onBack(): Boolean = false

    override fun draw(c: Canvas) {
        MenuBg.draw(c, W, H, time)
        Gfx.pill(c, 40f, 50f, 300f, 100f, "${game.save.coins}") { cv, x, y, r -> Gfx.coin(cv, x, y, r) }
        // logo
        c.save(); c.translate(W / 2, 560f + sin(time * 1.6f) * 10f); c.scale(0.92f, 0.92f)
        LogoArt.full(c, time)
        c.restore()
        // podium + car
        val py = carY + 170f
        c.drawOval(W / 2 - 420f, py - 70f, W / 2 + 420f, py + 70f, Gfx.radial(W / 2, py, 420f, 0xCCFFFFFF.toInt(), 0x0078DCFF))
        c.drawOval(W / 2 - 360f, py - 30f + 26f, W / 2 + 360f, py + 70f + 26f, Gfx.p(0xFF4C78C4.toInt()))
        c.drawOval(W / 2 - 360f, py - 30f, W / 2 + 360f, py + 70f, Gfx.vGrad(py - 30f, py + 70f, 0xFFE6F4FF.toInt(), 0xFF8FB5E8.toInt()))
        c.drawOval(W / 2 - 360f, py - 30f, W / 2 + 360f, py + 70f, Gfx.s(Color.WHITE, 8f))
        val q = 2.0f
        val jump = sin(hop * Math.PI.toFloat()) * 80f
        c.save(); c.translate(W / 2 - 200f * q, carY - 180f * q + 175f - jump)
        c.scale(q, q)
        c.scale(1f + hop * 0.06f, 1f - hop * 0.06f, 200f, 180f)
        CarArt.draw(c, showcase, 1f, time, 0f, 1f)
        c.restore()
        fx.draw(c)
        ui.draw(c, time)
        val glow = Ease.win(time, 0.5f, 0.5f)
        if (glow > 0f && game.save.unlocked == 0) Gfx.plain(c, "Tap PLAY to start washing!", W / 2, H - 600f, 42f, Col.alpha(Color.WHITE, glow))
    }
}
