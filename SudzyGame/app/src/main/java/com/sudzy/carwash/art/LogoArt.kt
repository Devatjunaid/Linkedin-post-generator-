package com.sudzy.carwash.art

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import com.sudzy.carwash.engine.Col
import com.sudzy.carwash.engine.Ease
import com.sudzy.carwash.engine.Fonts
import com.sudzy.carwash.engine.Gfx
import kotlin.math.cos
import kotlin.math.sin

/**
 * "Sudzy" — the mascot: a goofy front-facing car with googly windshield eyes,
 * a big grin and a pile of foam on its roof, living inside a rainbow soap bubble.
 */
object LogoArt {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val st = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND }
    private val path = Path()
    private val r = RectF()
    private const val OUT = 0xFF0B2A6E.toInt()

    private fun f(c: Int) = p.apply { shader = null; color = c }
    private fun s(c: Int, w: Float) = st.apply { shader = null; color = c; strokeWidth = w }

    private val cabin = Path().apply {
        moveTo(-60f, -74f); quadTo(0f, -84f, 60f, -74f); quadTo(74f, -72f, 80f, -58f); lineTo(100f, -10f)
        lineTo(-100f, -10f); lineTo(-80f, -58f); quadTo(-74f, -72f, -60f, -74f); close()
    }
    private val glass = Path().apply {
        moveTo(-52f, -64f); lineTo(52f, -64f); quadTo(61f, -64f, 65f, -55f); lineTo(80f, -18f)
        lineTo(-80f, -18f); lineTo(-65f, -55f); quadTo(-61f, -64f, -52f, -64f); close()
    }

    /** Mascot centred at (0,0), roughly 240 wide. [look] moves pupils, [blink] 0..1 closes eyes. */
    fun mascot(c: Canvas, time: Float, color: Int = Col.RED, look: Float = 0f, blink: Float = 0f, grin: Float = 1f) {
        val light = Col.shade(color, 0.45f); val dark = Col.shade(color, -0.45f)
        // wheels + mirrors
        c.drawRoundRect(-104f, 40f, -56f, 92f, 16f, 16f, f(0xFF23232E.toInt()))
        c.drawRoundRect(56f, 40f, 104f, 92f, 16f, 16f, f(0xFF23232E.toInt()))
        c.drawOval(-128f, -44f, -96f, -18f, f(color)); c.drawOval(-128f, -44f, -96f, -18f, s(OUT, 5f))
        c.drawOval(96f, -44f, 128f, -18f, f(color)); c.drawOval(96f, -44f, 128f, -18f, s(OUT, 5f))
        // cabin
        p.shader = LinearGradient(0f, -84f, 0f, -10f, light, color, Shader.TileMode.CLAMP); p.color = Color.WHITE
        c.drawPath(cabin, p); c.drawPath(cabin, s(OUT, 6f))
        p.shader = LinearGradient(-60f, -64f, 60f, -18f, 0xFFE3F8FF.toInt(), 0xFF6FB6F0.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(glass, p); c.drawPath(glass, s(OUT, 4.5f))
        // eyes on the windshield
        for (side in intArrayOf(-1, 1)) {
            val ex = side * 30f; val ey = -40f
            val open = 1f - blink
            c.drawOval(ex - 24f, ey - 21f * open - 1f, ex + 24f, ey + 21f * open + 1f, f(Color.WHITE))
            c.drawOval(ex - 24f, ey - 21f * open - 1f, ex + 24f, ey + 21f * open + 1f, s(OUT, 4.5f))
            if (open > 0.3f) {
                val px = ex + 6f + look * 6f; val py = ey + 2f
                c.drawCircle(px, py, 12f * open, f(0xFF1A1030.toInt()))
                c.drawCircle(px - 4f, py - 5f, 4.5f, f(Color.WHITE))
                c.drawCircle(px + 4f, py + 4f, 2f, f(Color.WHITE))
            }
            // eyebrow
            c.drawLine(ex - 16f, ey - 32f - 3f * grin, ex + 14f, ey - 36f - 2f * grin * side, s(OUT, 6f))
        }
        // lower body
        p.shader = LinearGradient(0f, -22f, 0f, 62f, intArrayOf(light, color, dark), floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP)
        p.color = Color.WHITE
        r.set(-118f, -22f, 118f, 60f); c.drawRoundRect(r, 34f, 34f, p); c.drawRoundRect(r, 34f, 34f, s(OUT, 6f))
        // headlights
        for (side in intArrayOf(-1, 1)) {
            c.drawCircle(side * 86f, 4f, 16f, f(0xFFFFF4A3.toInt())); c.drawCircle(side * 86f, 4f, 16f, s(OUT, 4f))
            c.drawCircle(side * 86f - 5f, -1f, 5f, f(Color.WHITE))
            c.drawOval(side * 62f - 13f, 26f, side * 62f + 13f, 36f, f(0x66FF5D8A))
        }
        // grin
        val g = 0.85f + 0.15f * grin
        path.reset(); path.moveTo(-52f, 10f); path.quadTo(0f, 10f + 56f * g, 52f, 10f); path.quadTo(0f, 22f, -52f, 10f); path.close()
        c.drawPath(path, f(0xFF7A1530.toInt()))
        c.save(); c.clipPath(path)
        c.drawOval(-26f, 26f + 10f * g, 26f, 66f, f(0xFFFF6F8F.toInt()))
        c.drawRect(-60f, 6f, 60f, 20f, f(Color.WHITE))
        c.restore()
        c.drawPath(path, s(OUT, 5f))
        // bumper
        c.drawRoundRect(-122f, 46f, 122f, 66f, 10f, 10f, f(0xFFDDE6F5.toInt())); c.drawRoundRect(-122f, 46f, 122f, 66f, 10f, 10f, s(OUT, 5f))
        // gloss
        path.reset(); path.moveTo(-96f, -8f); path.quadTo(-100f, -16f, -88f, -16f); path.lineTo(-30f, -16f)
        c.drawPath(path, s(0x99FFFFFF.toInt(), 6f))
        // foam hat
        val wob = sin(time * 3f) * 3f
        val foam = floatArrayOf(-50f, -86f, 22f, -18f, -98f, 28f, 20f, -96f, 25f, 50f, -86f, 20f, -2f, -112f, 22f, 30f, -118f, 14f, -34f, -108f, 13f)
        for (i in foam.indices step 3) c.drawCircle(foam[i], foam[i + 1] + wob * (i % 2), foam[i + 2] + 5f, f(OUT))
        for (i in foam.indices step 3) {
            c.drawCircle(foam[i], foam[i + 1] + wob * (i % 2), foam[i + 2], f(Color.WHITE))
            c.drawCircle(foam[i] - foam[i + 2] * 0.35f, foam[i + 1] + wob * (i % 2) - foam[i + 2] * 0.35f, foam[i + 2] * 0.25f, f(0xFFDFF4FF.toInt()))
        }
    }

    /** Rainbow soap bubble emblem holding the mascot. */
    fun emblem(c: Canvas, time: Float, radius: Float = 170f, color: Int = Col.RED) {
        c.drawCircle(0f, 0f, radius, Gfx.radial(-radius * 0.3f, -radius * 0.35f, radius * 1.15f, 0x55FFFFFF, 0x4489D8FF))
        p.shader = SweepGradient(0f, 0f, intArrayOf(0xFFFF8AD8.toInt(), 0xFFFFE14D.toInt(), 0xFF7DFF7A.toInt(), 0xFF5EF0FF.toInt(), 0xFFA46BFF.toInt(), 0xFFFF8AD8.toInt()), null)
        st.shader = p.shader; st.strokeWidth = 10f; st.color = Color.WHITE
        c.save(); c.rotate(time * 25f); c.drawCircle(0f, 0f, radius - 5f, st); c.restore()
        st.shader = null
        c.drawCircle(0f, 0f, radius, s(Color.WHITE, 5f))
        c.save(); c.scale(radius / 170f, radius / 170f); c.translate(0f, 18f)
        mascot(c, time, color, look = sin(time * 1.3f) * 0.8f, blink = blinkAt(time))
        c.restore()
        r.set(-radius * 0.78f, -radius * 0.78f, radius * 0.78f, radius * 0.78f)
        c.drawArc(r, 200f, 55f, false, s(0xDDFFFFFF.toInt(), radius * 0.07f))
        c.drawCircle(-radius * 0.38f, -radius * 0.74f, radius * 0.045f, f(Color.WHITE))
        c.drawArc(r, 20f, 30f, false, s(0x88FFFFFF.toInt(), radius * 0.035f))
        for (i in 0 until 5) {
            val a = time * 0.7f + i * 1.25f
            val rr = radius * (1.12f + 0.06f * sin(time * 2 + i))
            Gfx.bubble(c, cos(a) * rr, sin(a) * rr, radius * (0.07f + 0.03f * (i % 3)))
        }
    }

    fun blinkAt(time: Float): Float {
        val t = time % 3.7f
        return if (t > 3.55f) sin((t - 3.55f) / 0.15f * Math.PI.toFloat()) else 0f
    }

    private val LETTERS = arrayOf("S", "U", "D", "Z", "Y", "!")
    private val COLORS = intArrayOf(0xFFFFE14D.toInt(), 0xFFFFFFFF.toInt(), 0xFF5EF0FF.toInt(), 0xFFFF6FB5.toInt(), 0xFF7DFF7A.toInt(), 0xFFFFC21A.toInt())
    private val TILT = floatArrayOf(-9f, 5f, -4f, 7f, -6f, 11f)
    private val LIFT = floatArrayOf(0f, -10f, 6f, -8f, 4f, -14f)

    /**
     * Bubbly wordmark centred on (0, baseline). [appear] gives each letter's drop-in progress.
     */
    fun wordmark(c: Canvas, size: Float, time: Float, appear: (Int) -> Float = { 1f }) {
        val tp = Gfx.textPaint(size, Fonts.title, Paint.Align.LEFT)
        val widths = LETTERS.map { tp.measureText(it) }
        val gap = -size * 0.02f
        val total = widths.sum() + gap * (LETTERS.size - 1)
        var x = -total / 2
        for (i in LETTERS.indices) {
            val k = appear(i)
            val cx = x + widths[i] / 2
            x += widths[i] + gap
            if (k <= 0f) continue
            val drop = (1f - Ease.outBounce(k)) * -size * 2.2f
            val squash = if (k in 0.35f..0.75f) 1f - 0.18f * sin((k - 0.35f) / 0.4f * Math.PI.toFloat()) else 1f
            val bob = sin(time * 3f + i * 0.9f) * size * 0.025f
            c.save()
            c.translate(cx, LIFT[i] * size / 190f + drop + bob)
            c.rotate(TILT[i] + sin(time * 2f + i) * 2f)
            c.scale(1f + (1f - squash) * 0.6f, squash)
            drawLetter(c, LETTERS[i], size, COLORS[i], i)
            c.restore()
        }
    }

    private val textPath = Path()
    private fun drawLetter(c: Canvas, ch: String, size: Float, col: Int, idx: Int) {
        val tp = Gfx.textPaint(size, Fonts.title, Paint.Align.CENTER)
        tp.getTextPath(ch, 0, 1, 0f, 0f, textPath)
        val b = RectF(); Gfx.bounds(textPath, b)
        // 3D extrusion + outline
        tp.style = Paint.Style.FILL_AND_STROKE; tp.strokeJoin = Paint.Join.ROUND; tp.strokeWidth = size * 0.17f
        tp.color = 0xFF061B4F.toInt(); c.drawText(ch, 0f, size * 0.1f, tp)
        tp.color = OUT; c.drawText(ch, 0f, 0f, tp)
        // foam cap outline (only on letters that have a flat-ish top)
        val caps = idx != 5
        val capY = b.top
        if (caps) {
            for (k in 0 until 3) c.drawCircle(b.left + b.width() * (0.2f + 0.3f * k), capY + size * 0.02f, size * (0.1f + 0.02f * (k % 2)) + size * 0.06f, f(OUT))
        }
        // fill with vertical candy gradient
        p.shader = LinearGradient(0f, b.top, 0f, b.bottom, Col.shade(col, 0.3f), Col.shade(col, -0.04f), Shader.TileMode.CLAMP)
        p.color = Color.WHITE
        c.drawPath(textPath, p)
        // gloss band
        c.save(); c.clipPath(textPath)
        c.drawOval(b.left - b.width() * 0.2f, b.top - b.height() * 0.5f, b.right + b.width() * 0.2f, b.top + b.height() * 0.42f, f(0x55FFFFFF))
        c.restore()
        if (caps) {
            for (k in 0 until 3) {
                val x = b.left + b.width() * (0.2f + 0.3f * k); val rr = size * (0.1f + 0.02f * (k % 2))
                c.drawCircle(x, capY + size * 0.02f, rr, f(Color.WHITE))
                c.drawCircle(x - rr * 0.3f, capY - rr * 0.3f + size * 0.02f, rr * 0.28f, f(0xFFDFF4FF.toInt()))
            }
            // a little drip
            if (idx % 2 == 0) {
                val dx = b.left + b.width() * 0.62f
                c.drawRoundRect(dx - size * 0.035f, capY, dx + size * 0.035f, capY + size * 0.22f, size * 0.035f, size * 0.035f, f(Color.WHITE))
                c.drawCircle(dx, capY + size * 0.22f, size * 0.05f, f(Color.WHITE))
            }
        } else {
            // the "!" dot becomes a bubble
            Gfx.bubble(c, b.centerX() + size * 0.02f, b.bottom - size * 0.06f, size * 0.09f)
        }
    }

    /** Pink banner under the wordmark. */
    fun ribbon(c: Canvas, text: String, w: Float, h: Float, appear: Float = 1f) {
        if (appear <= 0f) return
        c.save(); c.scale(Ease.outBack(appear, 2.5f), 1f)
        c.rotate(-3f)
        for (side in intArrayOf(-1, 1)) {
            path.reset()
            val x0 = side * (w / 2 - 20f); val x1 = side * (w / 2 + 60f)
            path.moveTo(x0, -h * 0.25f); path.lineTo(x1, -h * 0.25f); path.lineTo(x1 - side * 30f, h * 0.2f); path.lineTo(x1, h * 0.65f); path.lineTo(x0, h * 0.65f); path.close()
            c.drawPath(path, f(0xFFB0124F.toInt()))
        }
        Gfx.candy(c, -w / 2, -h / 2, w / 2, h / 2, 0xFFFF6FAA.toInt(), 0xFFE0237A.toInt(), 18f, border = 6f, depth = 10f)
        Gfx.outlined(c, text, 0f, h * 0.18f, h * 0.52f, Color.WHITE, 0xFF8C0F47.toInt(), strokeMul = 0.12f, depth = 0.06f)
        c.restore()
    }

    /** Full lockup: emblem above wordmark above ribbon. */
    fun full(c: Canvas, time: Float, emblem: Float = 1f, letters: (Int) -> Float = { 1f }, ribbon: Float = 1f, color: Int = Col.RED) {
        if (emblem > 0f) {
            c.save(); c.translate(0f, -200f); val sc = Ease.outElastic(emblem); c.scale(sc, sc); emblem(c, time, 170f, color); c.restore()
        }
        c.save(); c.translate(0f, 110f); wordmark(c, 190f, time, letters); c.restore()
        c.save(); c.translate(0f, 210f); ribbon(c, "WASH  •  DETAIL  •  SHINE", 640f, 86f, ribbon); c.restore()
    }
}
