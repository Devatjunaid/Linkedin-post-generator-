package com.sudzy.carwash.scenes

import android.graphics.Canvas
import android.graphics.Color
import com.sudzy.carwash.art.CarArt
import com.sudzy.carwash.art.StickerArt
import com.sudzy.carwash.engine.ButtonGroup
import com.sudzy.carwash.engine.Col
import com.sudzy.carwash.engine.Game
import com.sudzy.carwash.engine.Gfx
import com.sudzy.carwash.engine.Scene
import com.sudzy.carwash.engine.Touch
import com.sudzy.carwash.game.Levels
import kotlin.math.abs
import kotlin.math.sin

/** Collection of every car you've washed; mystery silhouettes for the rest. */
class GarageScene(game: Game) : Scene(game) {
    private val ui = ButtonGroup()
    private val models = (0 until Levels.COUNT).map { Levels.get(it).model() }
    private val cardH = 420f
    private val top = 300f
    private var scroll = 0f; private var vel = 0f; private var lastY = 0f; private var dragging = false
    private val maxScroll get() = (top + (Levels.COUNT / 2) * (cardH + 30f) + 80f - H).coerceAtLeast(0f)

    override fun onEnter() { ui.add(backButton { game.go(HomeScene(game)) }) }

    override fun update(dt: Float) {
        super.update(dt); ui.update(dt)
        if (!dragging) { scroll += vel * dt; vel *= 1f / (1f + 4f * dt); if (abs(vel) < 5f) vel = 0f }
        scroll = scroll.coerceIn(0f, maxScroll)
    }

    override fun onTouch(t: Touch): Boolean {
        if (ui.touch(t, game)) return true
        when (t.action) {
            Touch.DOWN -> { dragging = true; lastY = t.y; vel = 0f }
            Touch.MOVE -> { val d = t.y - lastY; scroll -= d; vel = -d * 60f; lastY = t.y }
            Touch.UP -> dragging = false
        }
        return true
    }

    override fun onBack(): Boolean { game.go(HomeScene(game)); return true }

    override fun draw(c: Canvas) {
        MenuBg.draw(c, W, H, time, 0xFFFFC27A.toInt(), 0xFFFF8A2B.toInt(), 0xFFB8410A.toInt())
        val unlocked = game.save.unlocked
        val cw = (W - 90f) / 2
        c.save(); c.translate(0f, -scroll)
        for (i in 0 until Levels.COUNT) {
            val x = 30f + (i % 2) * (cw + 30f); val y = top + (i / 2) * (cardH + 30f)
            if (y > scroll + H || y + cardH < scroll) continue
            val have = i < unlocked
            val wc = intArrayOf(0xFF4CC86A.toInt(), 0xFF8A5A2B.toInt(), 0xFFFFB21F.toInt(), 0xFF5FB8FF.toInt(), 0xFFC04BFF.toInt(), 0xFFFF4F9E.toInt())[i / 5]
            Gfx.candy(c, x, y, x + cw, y + cardH, if (have) Col.shade(wc, 0.25f) else 0xFF4A5680.toInt(), if (have) Col.shade(wc, -0.2f) else 0xFF2A3263.toInt(), 50f, border = 7f, depth = 12f)
            val q = cw / 440f
            c.save(); c.translate(x + 10f * q + 10f, y + 70f); c.scale(q, q); c.translate(10f, 40f)
            if (have) {
                CarArt.shadow(c, models[i])
                c.save(); c.translate(0f, -models[i].lift); CarArt.body(c, models[i], 1f, time + i)
                game.save.stickers(i).split(";").filter { it.isNotBlank() }.map { it.split(",").map(String::toInt) }
                    .forEach { s -> if (s.size == 4) StickerArt.draw(c, s[0], s[1].toFloat(), s[2].toFloat(), 22f, s[3].toFloat()) }
                c.restore()
                CarArt.wheels(c, models[i], 0f, floatArrayOf(1f, 1f))
            } else CarArt.silhouette(c, models[i])
            c.restore()
            if (have) {
                val st = game.save.stars(i)
                for (k in 0 until 3) Gfx.star(c, x + cw / 2 - 80f + k * 80f, y + cardH - 70f, 32f, k < st)
            } else {
                Gfx.outlined(c, "?", x + cw / 2, y + cardH / 2 + 40f + sin(time * 3f + i) * 8f, 150f, 0xFF8C9BC4.toInt(), 0xFF1E2550.toInt())
            }
        }
        c.restore()
        c.drawRect(0f, 0f, W, 260f, Gfx.vGrad(0f, 260f, 0xFFB8410A.toInt(), 0x00B8410A))
        Gfx.outlined(c, "MY GARAGE", W / 2 + 40f, 140f, 110f, Color.WHITE, 0xFF8C2A00.toInt())
        ui.draw(c, time)
    }
}
