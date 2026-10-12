package com.sudzy.carwash.art

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.sudzy.carwash.engine.Col
import com.sudzy.carwash.engine.Gfx
import com.sudzy.carwash.engine.Rng
import kotlin.math.sin

/** Wash-bay backdrops for each world plus the road-trip map zones. */
object WorldArt {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val s = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; maskFilter = BlurMaskFilter(18f, BlurMaskFilter.Blur.NORMAL) }
    private val path = Path()
    private fun f(c: Int) = p.apply { color = c; shader = null }
    private fun l(c: Int, w: Float) = s.apply { color = c; strokeWidth = w; shader = null }

    val SIGNS = arrayOf("SUPER SUDS", "MUDDY BUDDY", "DUNE SPA", "FROSTY FOAM", "NEON SUDS", "GALAXY GLOSS")

    fun bay(c: Canvas, world: Int, W: Float, H: Float, floorY: Float, t: Float) {
        when (world) {
            0 -> {
                c.drawRect(0f, 0f, W, floorY, Gfx.vGrad(0f, floorY, 0xFFC6EDFF.toInt(), 0xFF86CDF6.toInt()))
                var x = 0f; while (x < W) { c.drawLine(x, 0f, x, floorY, l(0x88FFFFFF.toInt(), 5f)); x += 96f }
                var y = 0f; while (y < floorY) { c.drawLine(0f, y, W, y, l(0x88FFFFFF.toInt(), 5f)); y += 96f }
                rollers(c, W, floorY, t, 0xFFFF4F7A.toInt(), 0xFF3FA9FF.toInt())
            }
            1 -> {
                c.drawRect(0f, 0f, W, floorY, Gfx.vGrad(0f, floorY, 0xFFD9A066.toInt(), 0xFFA8693A.toInt()))
                var x = 0f; var i = 0
                while (x < W) { c.drawLine(x, 0f, x, floorY, l(0xFF7A4A22.toInt(), 6f)); c.drawCircle(x + 50f, 200f + (i * 137 % 500), 7f, f(0xFF7A4A22.toInt())); x += 120f; i++ }
                for (side in intArrayOf(0, 1)) {
                    val bx = if (side == 0) -30f else W - 230f
                    for (k in 0 until 2) {
                        val by = floorY - 150f - k * 140f
                        c.drawRoundRect(bx + k * 30f, by, bx + 230f + k * 30f, by + 150f, 24f, 24f, f(0xFFF2C94C.toInt()))
                        c.drawRoundRect(bx + k * 30f, by, bx + 230f + k * 30f, by + 150f, 24f, 24f, l(0xFFB8862A.toInt(), 6f))
                        for (ly in 1..3) c.drawLine(bx + 20f + k * 30f, by + ly * 37f, bx + 210f + k * 30f, by + ly * 37f, l(0xFFD4A53A.toInt(), 4f))
                    }
                }
            }
            2 -> {
                c.drawRect(0f, 0f, W, floorY, Gfx.vGrad(0f, floorY, 0xFFFFD9A0.toInt(), 0xFFE8A866.toInt()))
                val ax = W / 2; val aw = 520f; val atop = 420f; val abot = floorY - 380f
                path.reset(); path.moveTo(ax - aw / 2, abot); path.lineTo(ax - aw / 2, atop + aw / 2); path.arcTo(ax - aw / 2, atop, ax + aw / 2, atop + aw, 180f, 180f, false); path.lineTo(ax + aw / 2, abot); path.close()
                c.drawPath(path, Gfx.vGrad(atop, abot, 0xFF8FDCFF.toInt(), 0xFFFFE6B0.toInt()))
                c.save(); c.clipPath(path)
                c.drawCircle(ax + 120f, atop + 170f, 70f, f(0xFFFFF27A.toInt()))
                path.reset(); path.moveTo(ax - 300f, abot); path.quadTo(ax - 120f, abot - 160f, ax + 60f, abot - 40f); path.quadTo(ax + 200f, abot - 140f, ax + 300f, abot); path.close()
                c.drawPath(path, f(0xFFF0B866.toInt()))
                c.restore()
                c.drawPath(path.apply {
                    reset(); moveTo(ax - aw / 2, abot); lineTo(ax - aw / 2, atop + aw / 2); arcTo(ax - aw / 2, atop, ax + aw / 2, atop + aw, 180f, 180f, false); lineTo(ax + aw / 2, abot)
                }, l(0xFFB8733A.toInt(), 14f))
                cactus(c, 80f, floorY - 10f, 0.9f); cactus(c, W - 80f, floorY - 10f, 0.75f)
            }
            3 -> {
                c.drawRect(0f, 0f, W, floorY, Gfx.vGrad(0f, floorY, 0xFFEAF7FF.toInt(), 0xFF9FD3F5.toInt()))
                var x = 0f; while (x < W) { c.drawLine(x, 0f, x, floorY, l(0x55FFFFFF, 30f)); x += 140f }
                rollers(c, W, floorY, t, 0xFF5EB8FF.toInt(), 0xFFFFFFFF.toInt())
                val rng = Rng(31)
                for (k in 0 until 40) {
                    val sx = rng.range(0f, W); val sp = rng.range(40f, 90f)
                    val sy = (rng.range(0f, floorY) + t * sp) % floorY
                    Gfx.sparkle(c, sx + sin(t + k) * 20f, sy, rng.range(6f, 12f), Color.WHITE, 0.9f)
                }
            }
            4 -> {
                c.drawRect(0f, 0f, W, floorY, Gfx.vGrad(0f, floorY, 0xFF2A0E6B.toInt(), 0xFF140A45.toInt()))
                var y = 0f; var row = 0
                while (y < floorY) { c.drawLine(0f, y, W, y, l(0xFF3A1A80.toInt(), 4f)); var x = if (row % 2 == 0) 0f else 70f; while (x < W) { c.drawLine(x, y, x, y + 70f, l(0xFF3A1A80.toInt(), 4f)); x += 140f }; y += 70f; row++ }
                neonTube(c, 90f, 480f, 260f, 480f, 0xFFFF4FD8.toInt(), t)
                neonTube(c, W - 260f, 510f, W - 90f, 510f, 0xFF5EF0FF.toInt(), t + 1f)
                neonTube(c, 140f, floorY - 420f, 140f, floorY - 200f, 0xFF5EF0FF.toInt(), t + 2f)
                neonTube(c, W - 140f, floorY - 460f, W - 140f, floorY - 220f, 0xFFFFE14D.toInt(), t + 3f)
            }
            else -> {
                c.drawRect(0f, 0f, W, floorY, Gfx.vGrad(0f, floorY, 0xFF2B3158.toInt(), 0xFF171B38.toInt()))
                var x = 0f; while (x < W) { c.drawLine(x, 0f, x, floorY, l(0xFF0E1128.toInt(), 6f)); var y = 60f; while (y < floorY) { c.drawCircle(x + 20f, y, 5f, f(0xFF4A5390.toInt())); y += 180f }; x += 180f }
                val cx = W / 2; val cy = 700f; val r = 260f
                c.drawCircle(cx, cy, r + 26f, f(0xFF8A93C8.toInt()))
                c.drawCircle(cx, cy, r, f(0xFF0B0630.toInt()))
                c.save(); path.reset(); path.addCircle(cx, cy, r, Path.Direction.CW); c.clipPath(path)
                val rng = Rng(5); repeat(70) { c.drawCircle(cx + rng.range(-r, r), cy + rng.range(-r, r), rng.range(1f, 3.5f), f(Col.alpha(Color.WHITE, rng.range(0.4f, 1f)))) }
                c.drawCircle(cx + 90f, cy + 60f, 90f, f(0xFFFF7AC3.toInt()))
                c.drawOval(cx - 40f, cy + 35f, cx + 220f, cy + 85f, l(0xFFFFE14D.toInt(), 10f))
                c.drawCircle(cx - 120f, cy - 110f, 34f, f(0xFF5EF0FF.toInt()))
                c.restore()
                c.drawArc(cx - r + 30f, cy - r + 30f, cx + r - 30f, cy + r - 30f, 200f, 50f, false, l(0x66FFFFFF, 16f))
            }
        }
        // beam + sign
        c.drawRect(-10f, 300f, W + 10f, 340f, f(if (world == 1) 0xFF6B3A12.toInt() else 0xFF3B4A78.toInt()))
        if (world == 3) for (k in 0 until 14) {
            val ix = 40f + k * 76f; val len = 30f + (k * 53 % 50)
            path.reset(); path.moveTo(ix - 14f, 338f); path.lineTo(ix + 14f, 338f); path.lineTo(ix, 338f + len); path.close(); c.drawPath(path, f(0xEEFFFFFF.toInt()))
        }
        sign(c, SIGNS[world], W / 2, 360f, world, t)
        // floor
        c.drawRect(0f, floorY, W, H, Gfx.vGrad(floorY, H, floorColor(world), Col.shade(floorColor(world), -0.35f)))
        var sx = -40f + (0f); var i = 0
        while (sx < W + 40f) {
            path.reset(); path.moveTo(sx, floorY); path.lineTo(sx + 44f, floorY); path.lineTo(sx + 24f, floorY + 36f); path.lineTo(sx - 20f, floorY + 36f); path.close()
            c.drawPath(path, f(if (i % 2 == 0) 0xFFFFCC00.toInt() else 0xFF222233.toInt()))
            sx += 44f; i++
        }
        c.drawOval(80f, floorY + 160f, 400f, floorY + 210f, f(0x55A0DCFF))
        c.drawOval(620f, floorY + 230f, 900f, floorY + 270f, f(0x44A0DCFF))
    }

    private fun floorColor(world: Int) = when (world) {
        1 -> 0xFF8A6A48.toInt(); 2 -> 0xFFD9A867.toInt(); 3 -> 0xFFB8D4EA.toInt(); 4 -> 0xFF2B1660.toInt(); 5 -> 0xFF5A6290.toInt(); else -> 0xFF9AA7B8.toInt()
    }

    private fun rollers(c: Canvas, W: Float, floorY: Float, t: Float, c1: Int, c2: Int) {
        for (side in intArrayOf(0, 1)) {
            val x = if (side == 0) -40f else W - 100f
            val top = 410f; val bot = floorY - 20f
            c.save(); c.clipRect(x, top, x + 140f, bot)
            c.drawRoundRect(x, top, x + 140f, bot, 60f, 60f, f(Color.WHITE))
            var y = top - 100f + (t * 60f) % 100f
            while (y < bot) {
                path.reset(); path.moveTo(x, y); path.lineTo(x + 140f, y - 24f); path.lineTo(x + 140f, y + 16f); path.lineTo(x, y + 40f); path.close()
                c.drawPath(path, f(c1))
                path.reset(); path.moveTo(x, y + 50f); path.lineTo(x + 140f, y + 26f); path.lineTo(x + 140f, y + 62f); path.lineTo(x, y + 86f); path.close()
                c.drawPath(path, f(c2))
                y += 100f
            }
            c.restore()
            c.drawRoundRect(x, top, x + 140f, bot, 60f, 60f, l(0xFF3B4A78.toInt(), 10f))
        }
    }

    private fun cactus(c: Canvas, x: Float, base: Float, k: Float) {
        c.drawRoundRect(x - 26f * k, base - 220f * k, x + 26f * k, base, 26f * k, 26f * k, f(0xFF3AA35A.toInt()))
        c.drawRoundRect(x - 80f * k, base - 170f * k, x - 46f * k, base - 80f * k, 17f * k, 17f * k, f(0xFF3AA35A.toInt()))
        c.drawRect(x - 70f * k, base - 104f * k, x - 20f * k, base - 80f * k, f(0xFF3AA35A.toInt()))
        c.drawRoundRect(x - 40f * k, base - 40f * k, x + 40f * k, base + 10f * k, 10f, 10f, f(0xFFC9673A.toInt()))
    }

    private fun neonTube(c: Canvas, x0: Float, y0: Float, x1: Float, y1: Float, col: Int, t: Float) {
        val flick = 0.75f + 0.25f * sin(t * 7f)
        glow.color = Col.alpha(col, 0.8f * flick); glow.strokeWidth = 34f; c.drawLine(x0, y0, x1, y1, glow)
        c.drawLine(x0, y0, x1, y1, l(Col.shade(col, 0.5f), 12f))
    }

    fun sign(c: Canvas, text: String, cx: Float, y: Float, world: Int, t: Float) {
        val tw = Gfx.measure(text, 64f) + 90f
        val (bg, border, txt) = when (world) {
            1 -> Triple(0xFF9A5A22.toInt(), 0xFF5A300A.toInt(), 0xFFFFE6A0.toInt())
            2 -> Triple(0xFFE07A3A.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt())
            3 -> Triple(0xFF3FA9FF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt())
            else -> Triple(0xFF1B2350.toInt(), if (world == 4) 0xFFFF4FD8.toInt() else 0xFF57E6FF.toInt(), 0xFFAFF6FF.toInt())
        }
        c.drawLine(cx - tw / 2 + 60f, y - 30f, cx - tw / 2 + 60f, y, l(0xFF3B4A78.toInt(), 8f))
        c.drawLine(cx + tw / 2 - 60f, y - 30f, cx + tw / 2 - 60f, y, l(0xFF3B4A78.toInt(), 8f))
        if (world == 0 || world >= 4) { glow.color = Col.alpha(border, 0.6f + 0.2f * sin(t * 5f)); glow.strokeWidth = 30f; c.drawRoundRect(cx - tw / 2, y, cx + tw / 2, y + 110f, 30f, 30f, glow) }
        c.drawRoundRect(cx - tw / 2, y, cx + tw / 2, y + 110f, 30f, 30f, f(bg))
        c.drawRoundRect(cx - tw / 2, y, cx + tw / 2, y + 110f, 30f, 30f, l(border, 9f))
        Gfx.outlined(c, text, cx, y + 78f, 64f, txt, Col.shade(bg, -0.4f), strokeMul = 0.08f, depth = 0.04f)
    }

    // ---------------- map ----------------
    val ZONE_TOP = intArrayOf(0xFF7FE07A.toInt(), 0xFFA9D46A.toInt(), 0xFFFFD27A.toInt(), 0xFFE8F6FF.toInt(), 0xFF5B1A8F.toInt(), 0xFF1A0B4A.toInt())
    val ZONE_BOT = intArrayOf(0xFF4CC86A.toInt(), 0xFF7FA84A.toInt(), 0xFFFFB84D.toInt(), 0xFFBFE6FF.toInt(), 0xFF2A0E6B.toInt(), 0xFF0B0630.toInt())

    /** One world band of the road-trip map, from [top] to [top]+[h] (world 0 at the bottom). */
    fun zone(c: Canvas, world: Int, top: Float, h: Float, W: Float, t: Float) {
        c.drawRect(0f, top, W, top + h + 2f, Gfx.vGrad(top, top + h, if (world < 5) ZONE_BOT[world + 1] else ZONE_TOP[world], ZONE_BOT[world]))
        val rng = Rng(world * 101L + 7)
        when (world) {
            0 -> repeat(9) { i ->
                val x = rng.range(40f, W - 40f); val y = top + rng.range(80f, h - 60f)
                if (i % 3 == 0) house(c, x, y, rng) else tree(c, x, y, rng.range(0.8f, 1.3f))
            }
            1 -> {
                repeat(7) { val x = rng.range(20f, W - 120f); val y = top + rng.range(60f, h - 60f); c.drawOval(x, y, x + rng.range(120f, 220f), y + 50f, f(0xAA6B4220.toInt())) }
                barn(c, W - 190f, top + h * 0.35f); barn(c, 120f, top + h * 0.72f)
            }
            2 -> {
                for (k in 0 until 4) { path.reset(); val y = top + h * (0.3f + k * 0.2f); path.moveTo(-50f, y); path.quadTo(W * 0.3f, y - 120f, W * 0.6f, y - 10f); path.quadTo(W * 0.85f, y - 90f, W + 50f, y); path.lineTo(W + 50f, y + 60f); path.lineTo(-50f, y + 60f); path.close(); c.drawPath(path, f(Col.alpha(0xFFE89A3A.toInt(), 0.45f))) }
                repeat(5) { cactus(c, rng.range(60f, W - 60f), top + rng.range(150f, h - 40f), 0.5f) }
            }
            3 -> repeat(6) {
                val x = rng.range(60f, W - 60f); val y = top + rng.range(180f, h - 20f); val m = rng.range(120f, 220f)
                path.reset(); path.moveTo(x - m, y); path.lineTo(x, y - m * 1.1f); path.lineTo(x + m, y); path.close(); c.drawPath(path, f(0xFF8FB6E0.toInt()))
                path.reset(); path.moveTo(x - m * 0.38f, y - m * 0.68f); path.lineTo(x, y - m * 1.1f); path.lineTo(x + m * 0.38f, y - m * 0.68f); path.lineTo(x + m * 0.12f, y - m * 0.58f); path.lineTo(x - m * 0.1f, y - m * 0.72f); path.close(); c.drawPath(path, f(Color.WHITE))
            }
            4 -> repeat(8) {
                val x = rng.range(0f, W - 120f); val bw = rng.range(90f, 150f); val bh = rng.range(160f, 340f); val y = top + rng.range(bh, h)
                val cols = intArrayOf(0xFFFF4FD8.toInt(), 0xFF57E6FF.toInt(), 0xFFFFE14D.toInt())
                val col = cols[it % 3]
                c.drawRect(x, y - bh, x + bw, y, f(0xFF1B0B44.toInt())); c.drawRect(x, y - bh, x + bw, y, l(col, 5f))
                var wy = y - bh + 20f; while (wy < y - 20f) { c.drawRect(x + 16f, wy, x + bw - 16f, wy + 10f, f(Col.alpha(col, 0.6f + 0.3f * sin(t * 3 + wy)))); wy += 36f }
            }
            else -> {
                repeat(90) { c.drawCircle(rng.range(0f, W), top + rng.range(0f, h), rng.range(1f, 4f), f(Col.alpha(Color.WHITE, 0.4f + 0.6f * ((sin(t * 2 + it) + 1) / 2)))) }
                c.drawCircle(W - 170f, top + 220f, 80f, f(0xFFFF7AC3.toInt())); c.drawOval(W - 290f, top + 195f, W - 50f, top + 245f, l(0xFFFFE14D.toInt(), 10f))
                c.drawCircle(130f, top + h * 0.6f, 44f, f(0xFF5EF0FF.toInt())); c.drawCircle(118f, top + h * 0.6f - 10f, 10f, f(0x5523A0C0))
            }
        }
    }

    private fun tree(c: Canvas, x: Float, y: Float, k: Float) {
        c.drawRect(x - 9f * k, y - 10f, x + 9f * k, y + 40f * k, f(0xFF7A4A22.toInt()))
        c.drawCircle(x, y - 20f * k, 46f * k, f(0xFF2FB350.toInt())); c.drawCircle(x - 14f * k, y - 34f * k, 16f * k, f(0xFF5FD57A.toInt()))
    }

    private fun house(c: Canvas, x: Float, y: Float, rng: Rng) {
        val col = intArrayOf(0xFFFFD27A.toInt(), 0xFFFF9AB8.toInt(), 0xFF9AD8FF.toInt())[rng.int(3)]
        c.drawRect(x - 60f, y - 50f, x + 60f, y + 40f, f(col)); c.drawRect(x - 60f, y - 50f, x + 60f, y + 40f, l(Col.shade(col, -0.4f), 4f))
        path.reset(); path.moveTo(x - 76f, y - 46f); path.lineTo(x, y - 110f); path.lineTo(x + 76f, y - 46f); path.close(); c.drawPath(path, f(0xFFD63A2F.toInt()))
        c.drawRect(x - 14f, y - 4f, x + 14f, y + 40f, f(0xFF3F6FD8.toInt())); c.drawRect(x - 46f, y - 30f, x - 22f, y - 8f, f(0xFFBFF0FF.toInt()))
    }

    private fun barn(c: Canvas, x: Float, y: Float) {
        c.drawRect(x - 80f, y - 60f, x + 80f, y + 60f, f(0xFFD63A2F.toInt()))
        path.reset(); path.moveTo(x - 92f, y - 56f); path.lineTo(x, y - 130f); path.lineTo(x + 92f, y - 56f); path.close(); c.drawPath(path, f(0xFF8A1F18.toInt()))
        c.drawRect(x - 30f, y, x + 30f, y + 60f, f(Color.WHITE)); c.drawLine(x - 30f, y, x + 30f, y + 60f, l(0xFFD63A2F.toInt(), 6f)); c.drawLine(x + 30f, y, x - 30f, y + 60f, l(0xFFD63A2F.toInt(), 6f))
    }
}
