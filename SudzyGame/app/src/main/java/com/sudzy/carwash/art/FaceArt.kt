package com.sudzy.carwash.art

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.sudzy.carwash.engine.Gfx

enum class Mood { WORRIED, HAPPY, WOW }

/** Customer characters; each world has its own look (alien drivers in space!). */
object FaceArt {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val s = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val path = Path()
    private fun f(c: Int) = p.apply { color = c; shader = null }
    private fun l(c: Int, w: Float) = s.apply { color = c; strokeWidth = w }

    private val SKINS = intArrayOf(0xFFFFCF9E.toInt(), 0xFFA8724A.toInt(), 0xFFE8B48A.toInt(), 0xFF7A4A2A.toInt(), 0xFFFFDDB8.toInt())
    private val HAIR = intArrayOf(0xFF3B2414.toInt(), 0xFF1E120A.toInt(), 0xFFE8B030.toInt(), 0xFFC0392B.toInt(), 0xFF2A2A2A.toInt())

    /** Face fitting a 100x100 box centred at (cx, cy) scaled to [size]. */
    fun draw(c: Canvas, world: Int, variant: Int, mood: Mood, cx: Float, cy: Float, size: Float, time: Float = 0f) {
        c.save(); c.translate(cx - size / 2, cy - size / 2); c.scale(size / 100f, size / 100f)
        val alien = world == 5
        val skin = if (alien) intArrayOf(0xFF7DFF7A.toInt(), 0xFF5EF0FF.toInt(), 0xFFFF8AD8.toInt())[variant % 3] else SKINS[variant % SKINS.size]
        val hair = HAIR[(variant + world) % HAIR.size]
        val out = 0xFF5A3A22.toInt()
        if (alien) {
            c.drawLine(36f, 22f, 28f, 4f, l(0xFF3A2A60.toInt(), 3f)); c.drawLine(64f, 22f, 72f, 4f, l(0xFF3A2A60.toInt(), 3f))
            c.drawCircle(28f, 4f, 5f, f(0xFFFFE14D.toInt())); c.drawCircle(72f, 4f, 5f, f(0xFFFFE14D.toInt()))
        }
        c.drawCircle(50f, 54f, 38f, f(skin)); c.drawCircle(50f, 54f, 38f, l(if (alien) 0xFF2A4A6A.toInt() else out, 3f))
        if (!alien) {
            path.reset(); path.moveTo(14f, 46f); path.quadTo(18f, 10f, 54f, 14f); path.quadTo(86f, 16f, 86f, 46f); path.quadTo(76f, 30f, 60f, 32f); path.quadTo(40f, 22f, 14f, 46f); path.close()
            c.drawPath(path, f(hair))
        }
        // world accessory
        when (world) {
            1 -> { // straw cowboy hat
                c.drawOval(4f, 20f, 96f, 36f, f(0xFFE8C26A.toInt())); c.drawOval(4f, 20f, 96f, 36f, l(0xFF9A7020.toInt(), 3f))
                path.reset(); path.moveTo(26f, 28f); path.quadTo(28f, 2f, 50f, 4f); path.quadTo(72f, 2f, 74f, 28f); path.close()
                c.drawPath(path, f(0xFFF2D27E.toInt())); c.drawPath(path, l(0xFF9A7020.toInt(), 3f)); c.drawRect(27f, 20f, 73f, 26f, f(0xFFC0392B.toInt()))
            }
            3 -> { // bobble beanie
                path.reset(); path.moveTo(14f, 40f); path.quadTo(16f, 8f, 50f, 8f); path.quadTo(84f, 8f, 86f, 40f); path.close()
                c.drawPath(path, f(0xFF3FA9FF.toInt())); c.drawRect(13f, 34f, 87f, 44f, f(Color.WHITE)); c.drawCircle(50f, 6f, 9f, f(Color.WHITE))
            }
            4 -> { c.drawRoundRect(RectF(20f, 38f, 80f, 58f), 10f, 10f, f(0xFF1B0B44.toInt())); c.drawRoundRect(RectF(20f, 38f, 80f, 58f), 10f, 10f, l(0xFFFF4FD8.toInt(), 3f)) }
        }
        val visor = world == 4
        val eyeH = when (mood) { Mood.WOW -> 9f; Mood.HAPPY -> 7f; Mood.WORRIED -> 6f }
        if (!visor) {
            val blink = LogoArt.blinkAt(time + variant)
            val eh = eyeH * (1f - blink) + 0.6f
            if (world == 2) { // sunglasses
                c.drawRoundRect(RectF(24f, 46f, 47f, 60f), 6f, 6f, f(0xFF1A1A2A.toInt())); c.drawRoundRect(RectF(53f, 46f, 76f, 60f), 6f, 6f, f(0xFF1A1A2A.toInt()))
                c.drawLine(47f, 50f, 53f, 50f, l(0xFF1A1A2A.toInt(), 3f)); c.drawLine(28f, 49f, 34f, 49f, l(0x88FFFFFF.toInt(), 2f))
            } else {
                c.drawOval(31f, 54f - eh, 43f, 54f + eh, f(0xFF2A1A10.toInt())); c.drawOval(57f, 54f - eh, 69f, 54f + eh, f(0xFF2A1A10.toInt()))
                if (eh > 3f) { c.drawCircle(39f, 51f, 2f, f(Color.WHITE)); c.drawCircle(65f, 51f, 2f, f(Color.WHITE)) }
                if (mood == Mood.WORRIED) { c.drawLine(29f, 42f, 42f, 46f, l(out, 3f)); c.drawLine(71f, 42f, 58f, 46f, l(out, 3f)) }
            }
        } else {
            c.drawLine(28f, 48f, 72f, 48f, l(0xFF5EF0FF.toInt(), 4f))
        }
        c.drawOval(19f, 64f, 33f, 72f, f(0x99FF8A8A.toInt())); c.drawOval(67f, 64f, 81f, 72f, f(0x99FF8A8A.toInt()))
        when (mood) {
            Mood.WOW -> { c.drawOval(41f, 66f, 59f, 86f, f(0xFF7A1E1E.toInt())); c.drawOval(44f, 76f, 56f, 84f, f(0xFFFF6F7F.toInt())) }
            Mood.HAPPY -> { path.reset(); path.moveTo(35f, 70f); path.quadTo(50f, 88f, 65f, 70f); path.close(); c.drawPath(path, f(0xFF7A1E1E.toInt())) }
            Mood.WORRIED -> { path.reset(); path.moveTo(38f, 80f); path.quadTo(50f, 70f, 62f, 80f); c.drawPath(path, l(0xFF7A1E1E.toInt(), 4f)) }
        }
        if (mood == Mood.WORRIED) { // sweat drop
            path.reset(); path.moveTo(84f, 30f); path.quadTo(92f, 44f, 84f, 48f); path.quadTo(76f, 44f, 84f, 30f); path.close()
            c.drawPath(path, f(0xFF8FD8FF.toInt()))
        }
        c.restore()
    }

    /** White speech bubble with a face and a line of text. */
    fun speech(c: Canvas, world: Int, variant: Int, mood: Mood, text: String, x: Float, y: Float, maxW: Float, time: Float, appear: Float = 1f) {
        if (appear <= 0f) return
        val ts = 40f
        val tw = Gfx.measure(text, ts, com.sudzy.carwash.engine.Fonts.body).coerceAtMost(maxW - 150f)
        val w = tw + 150f; val h = 110f
        c.save(); val sc = com.sudzy.carwash.engine.Ease.outBack(appear, 2.5f); c.scale(sc, sc, x, y + h / 2)
        Gfx.roundRect(c, x, y + 10f, x + w, y + h + 10f, 55f, Gfx.p(0x553A5AB0))
        Gfx.roundRect(c, x, y, x + w, y + h, 55f, Gfx.p(Color.WHITE))
        draw(c, world, variant, mood, x + 58f, y + h / 2, 92f, time)
        Gfx.plain(c, text, x + 116f, y + h / 2 + 14f, ts, 0xFF3554A8.toInt(), Paint.Align.LEFT)
        c.restore()
    }
}
