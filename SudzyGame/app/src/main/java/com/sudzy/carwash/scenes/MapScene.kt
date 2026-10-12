package com.sudzy.carwash.scenes

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import com.sudzy.carwash.art.CarArt
import com.sudzy.carwash.art.Icon
import com.sudzy.carwash.art.IconArt
import com.sudzy.carwash.art.WorldArt
import com.sudzy.carwash.engine.ButtonGroup
import com.sudzy.carwash.engine.Col
import com.sudzy.carwash.engine.Game
import com.sudzy.carwash.engine.Gfx
import com.sudzy.carwash.engine.Scene
import com.sudzy.carwash.engine.Touch
import com.sudzy.carwash.game.Levels
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin

/** The road trip: 30 stops winding up through six worlds. Players never see numbers. */
class MapScene(game: Game) : Scene(game) {
    private val zoneH = 1250f
    private val contentH = zoneH * 6
    private val ui = ButtonGroup()
    private var scroll = 0f
    private var vel = 0f
    private var dragY = 0f; private var dragStart = 0f; private var dragging = false; private var moved = false
    private val road = Path()
    private val dash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 10f; color = 0xFFFFE14D.toInt(); pathEffect = DashPathEffect(floatArrayOf(40f, 40f), 0f)
    }
    private val current get() = game.save.unlocked.coerceAtMost(Levels.COUNT - 1)

    private fun nodeX(i: Int) = W / 2 + sin(i * 1.05f + 0.4f) * 300f
    private fun nodeY(i: Int): Float {
        val w = i / 5; val k = i % 5
        return contentH - w * zoneH - (k + 0.5f) * zoneH / 5f
    }

    override fun onEnter() {
        ui.add(backButton { game.go(HomeScene(game)) })
        road.reset()
        road.moveTo(nodeX(0), contentH + 200f)
        var px = nodeX(0); var py = contentH + 200f
        for (i in 0 until Levels.COUNT) {
            val x = nodeX(i); val y = nodeY(i)
            road.quadTo(px, py, (px + x) / 2, (py + y) / 2)
            px = x; py = y
        }
        road.lineTo(nodeX(29), -200f)
        scroll = (nodeY(current) - H * 0.6f).coerceIn(0f, contentH - H)
    }

    override fun update(dt: Float) {
        super.update(dt); ui.update(dt)
        if (!dragging) {
            scroll += vel * dt; vel *= 1f / (1f + 4f * dt)
            if (abs(vel) < 5f) vel = 0f
        }
        scroll = scroll.coerceIn(0f, (contentH - H).coerceAtLeast(0f))
    }

    override fun onTouch(t: Touch): Boolean {
        if (ui.touch(t, game)) return true
        when (t.action) {
            Touch.DOWN -> { dragging = true; moved = false; dragY = t.y; dragStart = t.y; vel = 0f }
            Touch.MOVE -> if (dragging) {
                val dy = t.y - dragY; scroll -= dy; vel = -dy * 60f; dragY = t.y
                if (abs(t.y - dragStart) > 20f) moved = true
            }
            Touch.UP -> {
                dragging = false
                if (!moved) {
                    val cy = t.y + scroll
                    for (i in 0..current) if (hypot(t.x - nodeX(i), cy - nodeY(i)) < 110f) {
                        game.audio.sfx("pop"); game.startLevel(i); return true
                    }
                    for (i in current + 1 until Levels.COUNT) if (hypot(t.x - nodeX(i), cy - nodeY(i)) < 90f) { game.audio.sfx("boing"); return true }
                }
            }
        }
        return true
    }

    override fun onBack(): Boolean { game.go(HomeScene(game)); return true }

    override fun draw(c: Canvas) {
        c.save(); c.translate(0f, -scroll)
        for (w in 0 until 6) {
            val top = contentH - (w + 1) * zoneH
            if (top > scroll + H || top + zoneH < scroll) continue
            WorldArt.zone(c, w, top, zoneH, W, time)
        }
        c.drawPath(road, Gfx.s(0xFF2C2C3E.toInt(), 130f))
        c.drawPath(road, Gfx.s(0xFF4B4B63.toInt(), 106f))
        c.drawPath(road, dash)
        for (w in 0 until 6) {
            val top = contentH - (w + 1) * zoneH
            val left = w % 2 == 0
            val sx = if (left) 40f else W - 420f
            c.save(); c.rotate(if (left) -4f else 4f, sx + 190f, top + 80f)
            Gfx.candy(c, sx, top + 40f, sx + 380f, top + 130f, Color.WHITE, 0xFFDCE6FF.toInt(), 30f, border = 0f, depth = 10f)
            Gfx.outlined(c, Levels.worlds[w].name, sx + 190f, top + 104f, 50f, Col.INK, Color.WHITE, strokeMul = 0.08f, depth = 0f)
            c.restore()
        }
        val cur = current
        for (i in 0 until Levels.COUNT) {
            val x = nodeX(i); val y = nodeY(i)
            if (y < scroll - 200f || y > scroll + H + 200f) continue
            when {
                i == cur -> drawCurrent(c, i, x, y)
                i < cur -> {
                    Gfx.candy(c, x - 70f, y - 70f, x + 70f, y + 70f, Col.LIME, 0xFF22C13A.toInt(), 70f, border = 8f, depth = 12f)
                    IconArt.draw(c, Icon.CHECK, x, y, 90f)
                    val st = game.save.stars(i)
                    for (k in 0 until 3) Gfx.star(c, x - 52f + k * 52f, y + 98f, 24f, k < st)
                }
                else -> {
                    val chest = i % 5 == 4
                    if (chest) {
                        c.drawCircle(x, y, 80f, Gfx.radial(x, y, 80f, 0xFFFFF6B0.toInt(), 0xFFFFB000.toInt()))
                        c.drawCircle(x, y, 80f, Gfx.s(Color.WHITE, 8f))
                        IconArt.draw(c, Icon.CHEST, x, y, 110f)
                    } else {
                        Gfx.candy(c, x - 62f, y - 62f, x + 62f, y + 62f, 0xFF9AA6C4.toInt(), 0xFF5D6789.toInt(), 62f, border = 8f, depth = 10f)
                        IconArt.draw(c, Icon.LOCK, x, y, 70f)
                    }
                }
            }
        }
        c.restore()
        // header
        c.drawRect(0f, 0f, W, 190f, Gfx.vGrad(0f, 190f, 0x88000022.toInt(), 0x00000022))
        Gfx.pill(c, W - 330f, 50f, 290f, 100f, "${game.save.coins}") { cv, x, y, r -> Gfx.coin(cv, x, y, r) }
        Gfx.pill(c, W - 590f, 50f, 230f, 100f, "${game.save.totalStars()}") { cv, x, y, r -> Gfx.star(cv, x, y, r * 0.9f, true) }
        ui.draw(c, time)
    }

    private fun drawCurrent(c: Canvas, i: Int, x: Float, y: Float) {
        val pulse = 1f + 0.06f * sin(time * 5f)
        c.drawCircle(x, y, 140f * pulse, Gfx.radial(x, y, 150f, 0xAAFFE14D.toInt(), 0x00FFE14D))
        c.save(); c.scale(pulse, pulse, x, y)
        c.drawCircle(x, y + 12f, 100f, Gfx.p(0xFFC77800.toInt()))
        c.drawCircle(x, y, 100f, Gfx.vGrad(y - 100f, y + 100f, 0xFFFFE36B.toInt(), 0xFFFFB000.toInt()))
        c.drawCircle(x, y, 100f, Gfx.s(Color.WHITE, 10f))
        c.save(); c.translate(x - 85f, y - 50f); c.scale(0.42f, 0.42f)
        CarArt.draw(c, Levels.get(i).model(), 0.5f, time)
        c.restore()
        c.restore()
        val bob = sin(time * 4f) * 10f
        val fy = y - 190f + bob
        Gfx.candy(c, x - 90f, fy - 50f, x + 90f, fy + 50f, 0xFFFF6B8F.toInt(), 0xFFFF3B6B.toInt(), 30f, border = 7f, depth = 8f)
        Gfx.outlined(c, "GO!", x, fy + 22f, 64f, Color.WHITE, 0xFFA1133F.toInt(), strokeMul = 0.1f)
        val tri = Path().apply { moveTo(x - 20f, fy + 52f); lineTo(x + 20f, fy + 52f); lineTo(x, fy + 76f); close() }
        c.drawPath(tri, Gfx.p(Color.WHITE))
    }
}
