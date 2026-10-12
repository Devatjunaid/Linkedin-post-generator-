package com.sudzy.carwash.art

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.sudzy.carwash.engine.Gfx

enum class Icon {
    HOSE, FOAM, SPONGE, BRUSH, DRYER, WAX, POLISH, TIRE, STICKER,
    GEAR, PLAY, PAUSE, BACK, CLOSE, HOME, GARAGE, SPIN, MUSIC, SOUND, VIBE, STAR, SHARE, MAIL, SHIELD, LOCK, CHECK, CHEST, NEXT
}

/** Hand-made vector icons drawn in a 64x64 box. */
object IconArt {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val s = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val path = Path()
    private val r = RectF()

    private fun f(c: Int) = p.apply { color = c; shader = null }
    private fun l(c: Int, w: Float) = s.apply { color = c; strokeWidth = w; shader = null }
    private fun rr(c: Canvas, l: Float, t: Float, rt: Float, b: Float, rad: Float, paint: Paint) { r.set(l, t, rt, b); c.drawRoundRect(r, rad, rad, paint) }

    /** Draw [icon] centred at (cx, cy) with the given size; [tint] recolours white UI glyphs. */
    fun draw(c: Canvas, icon: Icon, cx: Float, cy: Float, size: Float, tint: Int = Color.WHITE) {
        c.save(); c.translate(cx - size / 2, cy - size / 2); c.scale(size / 64f, size / 64f)
        val W = tint
        when (icon) {
            Icon.HOSE -> {
                path.reset(); path.moveTo(6f, 24f); path.lineTo(36f, 24f); path.lineTo(45f, 17f); path.lineTo(45f, 43f); path.lineTo(36f, 36f); path.lineTo(6f, 36f); path.close()
                c.drawPath(path, f(0xFFFF7A1A.toInt())); c.drawPath(path, l(0xFF8A3A00.toInt(), 3f))
                path.reset(); path.moveTo(14f, 36f); path.lineTo(9f, 56f); path.lineTo(20f, 56f); path.lineTo(25f, 36f); path.close()
                c.drawPath(path, f(0xFF3B3F58.toInt())); c.drawPath(path, l(0xFF1C1F30.toInt(), 3f))
                c.drawLine(48f, 20f, 60f, 14f, l(0xFF2BB7FF.toInt(), 5f)); c.drawLine(49f, 30f, 62f, 30f, l(0xFF2BB7FF.toInt(), 5f)); c.drawLine(48f, 40f, 60f, 46f, l(0xFF2BB7FF.toInt(), 5f))
            }
            Icon.FOAM -> {
                val bs = floatArrayOf(40f, 36f, 15f, 22f, 40f, 12f, 30f, 22f, 10f, 48f, 52f, 7f, 14f, 54f, 6f)
                for (i in bs.indices step 3) { c.drawCircle(bs[i], bs[i + 1], bs[i + 2], f(Color.WHITE)); c.drawCircle(bs[i], bs[i + 1], bs[i + 2], l(0xFF71C4FF.toInt(), 3f)) }
                c.drawCircle(35f, 31f, 4f, f(0xFFCFEEFF.toInt())); c.drawCircle(26f, 18f, 3f, f(0xFFCFEEFF.toInt()))
            }
            Icon.SPONGE -> {
                rr(c, 8f, 16f, 56f, 50f, 11f, f(0xFFFFD23F.toInt())); rr(c, 8f, 16f, 56f, 50f, 11f, l(0xFFB07C00.toInt(), 3f))
                for (h in arrayOf(floatArrayOf(20f, 28f, 4f), floatArrayOf(36f, 24f, 3f), floatArrayOf(44f, 38f, 4.5f), floatArrayOf(26f, 40f, 3f))) c.drawCircle(h[0], h[1], h[2], f(0xFFE3A800.toInt()))
                c.drawCircle(50f, 16f, 6f, f(Color.WHITE)); c.drawCircle(50f, 16f, 6f, l(0xFF71C4FF.toInt(), 2f))
            }
            Icon.BRUSH -> {
                rr(c, 25f, 4f, 39f, 32f, 6f, f(0xFF8A5CFF.toInt())); rr(c, 25f, 4f, 39f, 32f, 6f, l(0xFF3F22A8.toInt(), 3f))
                rr(c, 10f, 28f, 54f, 42f, 5f, f(0xFF4B2FC4.toInt())); rr(c, 10f, 28f, 54f, 42f, 5f, l(0xFF2A1580.toInt(), 3f))
                var x = 15f; while (x <= 50f) { c.drawLine(x, 45f, x, 58f, l(0xFFFFC928.toInt(), 4f)); x += 7f }
            }
            Icon.DRYER -> {
                path.reset(); path.moveTo(6f, 22f); path.lineTo(36f, 22f); path.addArc(RectF(28f, 6f, 44f, 22f), 90f, -270f)
                c.drawPath(path, l(0xFF3FC8FF.toInt(), 6f))
                path.reset(); path.moveTo(6f, 34f); path.lineTo(48f, 34f); path.addArc(RectF(40f, 34f, 56f, 50f), -90f, 270f)
                c.drawPath(path, l(0xFF7AD9FF.toInt(), 6f))
                c.drawLine(6f, 48f, 28f, 48f, l(0xFF3FC8FF.toInt(), 6f))
            }
            Icon.WAX -> {
                rr(c, 10f, 24f, 54f, 52f, 7f, f(0xFFFF4FA3.toInt())); rr(c, 10f, 24f, 54f, 52f, 7f, l(0xFFA1135A.toInt(), 3f))
                c.drawRect(11f, 34f, 53f, 41f, f(0xB3FFFFFF.toInt()))
                c.drawOval(10f, 17f, 54f, 31f, f(0xFFFFD3EA.toInt())); c.drawOval(10f, 17f, 54f, 31f, l(0xFFA1135A.toInt(), 3f))
                Gfx.sparkle(c, 52f, 12f, 9f, 0xFFFFE45C.toInt())
            }
            Icon.POLISH -> {
                c.drawCircle(32f, 38f, 21f, f(Color.WHITE)); c.drawCircle(32f, 38f, 21f, l(0xFFB9B6FF.toInt(), 4f))
                c.drawArc(RectF(20f, 26f, 44f, 50f), 180f, 270f, false, l(0xFF8E86FF.toInt(), 3f))
                c.drawArc(RectF(26f, 32f, 38f, 44f), 0f, 250f, false, l(0xFF8E86FF.toInt(), 3f))
                rr(c, 25f, 4f, 39f, 20f, 5f, f(0xFF6C5CE7.toInt())); rr(c, 25f, 4f, 39f, 20f, 5f, l(0xFF3B2EA8.toInt(), 3f))
            }
            Icon.TIRE -> {
                c.drawCircle(32f, 34f, 25f, f(0xFF2B2B38.toInt())); c.drawCircle(32f, 34f, 25f, l(0xFF111111.toInt(), 3f))
                c.drawCircle(32f, 34f, 12f, f(0xFFD5DDE8.toInt())); c.drawCircle(32f, 34f, 12f, l(0xFF7D8796.toInt(), 2f))
                c.drawArc(RectF(10f, 12f, 54f, 56f), 190f, 70f, false, l(Color.WHITE, 5f))
                Gfx.sparkle(c, 50f, 12f, 9f, 0xFFFFFBD0.toInt())
            }
            Icon.STICKER -> {
                Gfx.starPath(32f, 34f, 26f, 11f, path); c.drawPath(path, f(0xFFFFCC00.toInt())); c.drawPath(path, l(0xFFE07000.toInt(), 3f))
                c.drawCircle(26f, 28f, 3f, f(Color.WHITE))
            }
            Icon.GEAR -> {
                for (i in 0 until 8) { c.save(); c.rotate(i * 45f, 32f, 32f); rr(c, 27f, 5f, 37f, 20f, 3f, f(W)); c.restore() }
                c.drawCircle(32f, 32f, 17f, f(W)); c.drawCircle(32f, 32f, 7f, f(0xFF2D6CF0.toInt()))
            }
            Icon.PLAY, Icon.NEXT -> {
                path.reset(); path.moveTo(20f, 12f); path.lineTo(50f, 32f); path.lineTo(20f, 52f); path.close()
                c.drawPath(path, f(W)); c.drawPath(path, l(W, 5f))
            }
            Icon.PAUSE -> { rr(c, 17f, 14f, 28f, 50f, 4f, f(W)); rr(c, 36f, 14f, 47f, 50f, 4f, f(W)) }
            Icon.BACK -> { path.reset(); path.moveTo(39f, 13f); path.lineTo(20f, 32f); path.lineTo(39f, 51f); c.drawPath(path, l(W, 9f)) }
            Icon.CLOSE -> { c.drawLine(18f, 18f, 46f, 46f, l(W, 9f)); c.drawLine(46f, 18f, 18f, 46f, l(W, 9f)) }
            Icon.HOME -> {
                path.reset(); path.moveTo(8f, 30f); path.lineTo(32f, 9f); path.lineTo(56f, 30f); path.lineTo(50f, 30f); path.lineTo(50f, 54f); path.lineTo(14f, 54f); path.lineTo(14f, 30f); path.close()
                c.drawPath(path, f(W)); rr(c, 26f, 36f, 38f, 54f, 3f, f(0xFF3F6FD8.toInt()))
            }
            Icon.GARAGE -> {
                path.reset(); path.moveTo(6f, 28f); path.lineTo(32f, 9f); path.lineTo(58f, 28f); path.lineTo(58f, 56f); path.lineTo(6f, 56f); path.close()
                c.drawPath(path, f(W)); c.drawRect(15f, 32f, 49f, 56f, f(0xFF3F6FD8.toInt()))
                for (y in intArrayOf(38, 44, 50)) c.drawLine(15f, y.toFloat(), 49f, y.toFloat(), l(W, 2.5f))
            }
            Icon.SPIN -> {
                val cols = intArrayOf(0xFFFF4F7A.toInt(), 0xFFFFC928.toInt(), 0xFF3FD17A.toInt(), 0xFF3FA9FF.toInt(), 0xFFA46BFF.toInt(), 0xFFFF8A2B.toInt())
                c.drawCircle(32f, 32f, 26f, f(Color.WHITE))
                for (i in 0 until 6) c.drawArc(RectF(9f, 9f, 55f, 55f), i * 60f, 60f, true, f(cols[i]))
                c.drawCircle(32f, 32f, 7f, f(Color.WHITE))
            }
            Icon.MUSIC -> {
                path.reset(); path.moveTo(24f, 46f); path.lineTo(24f, 16f); path.lineTo(50f, 10f); path.lineTo(50f, 40f); c.drawPath(path, l(W, 5f))
                c.drawCircle(18f, 46f, 8f, f(W)); c.drawCircle(44f, 40f, 8f, f(W))
            }
            Icon.SOUND -> {
                path.reset(); path.moveTo(9f, 24f); path.lineTo(20f, 24f); path.lineTo(34f, 12f); path.lineTo(34f, 52f); path.lineTo(20f, 40f); path.lineTo(9f, 40f); path.close()
                c.drawPath(path, f(W))
                c.drawArc(RectF(30f, 18f, 48f, 46f), -55f, 110f, false, l(W, 5f)); c.drawArc(RectF(30f, 10f, 56f, 54f), -55f, 110f, false, l(W, 5f))
            }
            Icon.VIBE -> {
                rr(c, 22f, 9f, 42f, 55f, 6f, f(W))
                for (sx in intArrayOf(-1, 1)) {
                    val x0 = if (sx < 0) 13f else 51f
                    path.reset(); path.moveTo(x0, 20f); path.lineTo(x0 + sx * -4f, 26f); path.lineTo(x0, 32f); path.lineTo(x0 + sx * -4f, 38f); path.lineTo(x0, 44f)
                    c.drawPath(path, l(W, 4f))
                }
            }
            Icon.STAR -> { Gfx.starPath(32f, 33f, 27f, 12f, path); c.drawPath(path, f(W)) }
            Icon.SHARE -> {
                c.drawLine(46f, 16f, 18f, 32f, l(W, 5f)); c.drawLine(18f, 32f, 46f, 48f, l(W, 5f))
                c.drawCircle(46f, 16f, 8f, f(W)); c.drawCircle(18f, 32f, 8f, f(W)); c.drawCircle(46f, 48f, 8f, f(W))
            }
            Icon.MAIL -> {
                rr(c, 8f, 16f, 56f, 50f, 6f, f(W))
                path.reset(); path.moveTo(11f, 20f); path.lineTo(32f, 36f); path.lineTo(53f, 20f); c.drawPath(path, l(0xFF5D6A91.toInt(), 4f))
            }
            Icon.SHIELD -> {
                path.reset(); path.moveTo(32f, 6f); path.lineTo(52f, 14f); path.lineTo(52f, 30f); path.quadTo(52f, 50f, 32f, 58f); path.quadTo(12f, 50f, 12f, 30f); path.lineTo(12f, 14f); path.close()
                c.drawPath(path, f(W))
                path.reset(); path.moveTo(23f, 31f); path.lineTo(30f, 38f); path.lineTo(42f, 24f); c.drawPath(path, l(0xFF5D6A91.toInt(), 5f))
            }
            Icon.LOCK -> {
                c.drawArc(RectF(22f, 12f, 42f, 32f), 180f, 180f, false, l(W, 6f)); c.drawLine(22f, 22f, 22f, 29f, l(W, 6f)); c.drawLine(42f, 22f, 42f, 29f, l(W, 6f))
                rr(c, 15f, 28f, 49f, 54f, 6f, f(W)); c.drawCircle(32f, 40f, 4.5f, f(0xFF55617F.toInt()))
            }
            Icon.CHECK -> { path.reset(); path.moveTo(15f, 34f); path.lineTo(26f, 45f); path.lineTo(49f, 20f); c.drawPath(path, l(W, 9f)) }
            Icon.CHEST -> {
                rr(c, 8f, 26f, 56f, 54f, 4f, f(0xFFC97A2B.toInt())); rr(c, 8f, 26f, 56f, 54f, 4f, l(0xFF6B3A0C.toInt(), 3f))
                c.drawArc(RectF(8f, 14f, 56f, 42f), 180f, 180f, true, f(0xFFE2913A.toInt())); c.drawArc(RectF(8f, 14f, 56f, 42f), 180f, 180f, true, l(0xFF6B3A0C.toInt(), 3f))
                c.drawLine(8f, 34f, 56f, 34f, l(0xFFFFD23F.toInt(), 3f))
                rr(c, 27f, 28f, 37f, 41f, 2f, f(0xFFFFD23F.toInt())); rr(c, 27f, 28f, 37f, 41f, 2f, l(0xFF6B3A0C.toInt(), 2f))
            }
        }
        c.restore()
    }
}
