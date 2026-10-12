package com.sudzy.carwash.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import com.sudzy.carwash.art.CarModel
import com.sudzy.carwash.engine.Col
import com.sudzy.carwash.engine.Gfx
import com.sudzy.carwash.engine.Rng
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A paintable sheet laid over the car body (in car-local space, x 0..400, y 20..160).
 * Tracks coverage on a coarse grid so we know when a step is finished.
 */
class CoverLayer(val model: CarModel, q: Float, startFull: Boolean) {
    private val ox = 0f; private val oy = 20f; private val lw = 400f; private val lh = 140f
    val bmp: Bitmap = Bitmap.createBitmap(max(1, (lw * q).toInt()), max(1, (lh * q).toInt()), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp).apply { scale(q, q); translate(-ox, -oy); clipPath(model.body) }
    private val cols = 50; private val rows = 18
    private val cw = lw / cols; private val ch = lh / rows
    private val inside = BooleanArray(cols * rows)
    val value = FloatArray(cols * rows)
    private val total: Int
    var alpha = 1f
    private val dst = RectF(ox, oy, ox + lw, oy + lh)
    private val drawPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val erase = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) }

    init {
        var n = 0
        for (r in 0 until rows) for (c in 0 until cols) {
            val x = ox + (c + 0.5f) * cw; val y = oy + (r + 0.5f) * ch
            val ins = model.inside(x, y)
            inside[r * cols + c] = ins
            if (ins) { n++; value[r * cols + c] = if (startFull) 1f else 0f }
        }
        total = max(1, n)
    }

    fun cleared(): Float { var k = 0; for (i in value.indices) if (inside[i] && value[i] <= 0.08f) k++; return k.toFloat() / total }
    fun filled(): Float { var k = 0; for (i in value.indices) if (inside[i] && value[i] >= 0.9f) k++; return k.toFloat() / total }

    /** Change grid values inside a circle. @return amount of change, for combos. */
    private fun apply(x: Float, y: Float, r: Float, delta: Float): Float {
        var changed = 0f
        val c0 = max(0, ((x - r - ox) / cw).toInt()); val c1 = min(cols - 1, ((x + r - ox) / cw).toInt())
        val r0 = max(0, ((y - r - oy) / ch).toInt()); val r1 = min(rows - 1, ((y + r - oy) / ch).toInt())
        for (rr in r0..r1) for (cc in c0..c1) {
            val i = rr * cols + cc
            if (!inside[i]) continue
            val dx = ox + (cc + 0.5f) * cw - x; val dy = oy + (rr + 0.5f) * ch - y
            if (dx * dx + dy * dy > r * r) continue
            val nv = (value[i] + delta).coerceIn(0f, 1f)
            changed += kotlin.math.abs(nv - value[i]); value[i] = nv
        }
        return changed
    }

    fun eraseAt(x: Float, y: Float, r: Float, strength: Float): Float {
        val ch = apply(x, y, r, -strength)
        if (ch > 0f) { erase.alpha = (255 * min(1f, strength * 1.3f)).toInt(); canvas.drawCircle(x, y, r, erase) }
        return ch
    }

    fun fillAt(x: Float, y: Float, r: Float, strength: Float, painter: (Canvas) -> Unit): Float {
        val ch = apply(x, y, r, strength)
        if (ch > 0f) painter(canvas)
        return ch
    }

    /** Snap the whole layer to full (keeping the art) — used when a fill step completes. */
    fun completeFill(painter: (Canvas, Float, Float) -> Unit) {
        for (r in 0 until rows) for (c in 0 until cols) {
            val i = r * cols + c
            if (inside[i] && value[i] < 0.9f) painter(canvas, ox + (c + 0.5f) * cw, oy + (r + 0.5f) * ch)
            if (inside[i]) value[i] = 1f
        }
    }

    fun clearAll() { canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); for (i in value.indices) value[i] = 0f }

    fun draw(c: Canvas) {
        if (alpha <= 0f) return
        drawPaint.alpha = (alpha * 255).toInt()
        c.drawBitmap(bmp, null, dst, drawPaint)
    }

    fun recycle() = bmp.recycle()
}

object DirtPainter {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    /** World-specific grime: dust, mud, sand, snow, neon slime or cosmic goo. */
    fun paint(layer: CoverLayer, world: Int, w: World, seed: Long) {
        val c = layer.canvas
        val rng = Rng(seed)
        p.shader = null
        p.color = Col.alpha(w.dirt, if (world == 0) 0.82f else 0.94f)
        c.drawRect(0f, 0f, 400f, 200f, p)
        when (world) {
            3 -> { // snow: puffy white with blue shading and a snow cap
                repeat(70) { p.color = Col.alpha(if (rng.chance(0.5f)) Color.WHITE else w.dirt2, 0.9f); c.drawCircle(rng.range(0f, 400f), rng.range(20f, 160f), rng.range(8f, 26f), p) }
                repeat(26) { p.color = Color.WHITE; c.drawCircle(rng.range(60f, 340f), rng.range(30f, 60f), rng.range(14f, 30f), p) }
                repeat(40) { Gfx.sparkle(c, rng.range(0f, 400f), rng.range(30f, 150f), rng.range(3f, 6f), 0xFF9CD2FF.toInt()) }
            }
            4 -> { // neon slime with drips and glossy blobs
                repeat(60) { p.color = Col.alpha(if (rng.chance(0.5f)) w.dirt2 else 0xFFB8FF5A.toInt(), 0.9f); c.drawCircle(rng.range(0f, 400f), rng.range(20f, 160f), rng.range(8f, 24f), p) }
                repeat(16) {
                    val x = rng.range(30f, 370f); val y = rng.range(60f, 120f); val len = rng.range(20f, 50f)
                    p.color = w.dirt; c.drawRoundRect(x - 5f, y, x + 5f, y + len, 5f, 5f, p); c.drawCircle(x, y + len, 8f, p)
                }
                repeat(30) { p.color = 0x99FFFFFF.toInt(); c.drawCircle(rng.range(0f, 400f), rng.range(20f, 160f), rng.range(2f, 5f), p) }
            }
            5 -> { // cosmic goo with star speckles
                repeat(60) { p.color = Col.alpha(if (rng.chance(0.5f)) w.dirt2 else 0xFF5A1DBF.toInt(), 0.9f); c.drawCircle(rng.range(0f, 400f), rng.range(20f, 160f), rng.range(8f, 26f), p) }
                repeat(50) { Gfx.sparkle(c, rng.range(0f, 400f), rng.range(25f, 155f), rng.range(2f, 7f), if (rng.chance(0.5f)) Color.WHITE else 0xFFFFE14D.toInt()) }
            }
            else -> { // dust / mud / sand
                val n = if (world == 1) 110 else 80
                repeat(n) {
                    p.color = Col.alpha(if (rng.chance(0.5f)) w.dirt2 else Col.shade(w.dirt, -0.25f), rng.range(0.6f, 1f))
                    val x = rng.range(0f, 400f); val y = rng.range(20f, 160f); val r = rng.range(6f, 24f)
                    c.drawCircle(x, y, r, p)
                    if (world == 1 && rng.chance(0.35f)) { path.reset(); path.moveTo(x - 4f, y); path.quadTo(x, y + rng.range(18f, 40f), x + 4f, y); c.drawPath(path, p) }
                }
                repeat(if (world == 2) 160 else 50) { p.color = Col.alpha(Col.shade(w.dirt, if (rng.chance(0.5f)) 0.35f else -0.35f), 0.8f); c.drawCircle(rng.range(0f, 400f), rng.range(20f, 160f), rng.range(1.5f, 3.5f), p) }
            }
        }
    }

    fun foamBlob(c: Canvas, x: Float, y: Float, rng: Rng, size: Float = 1f) {
        repeat(3) {
            val bx = x + rng.range(-12f, 12f) * size; val by = y + rng.range(-12f, 12f) * size; val r = rng.range(7f, 15f) * size
            p.shader = null; p.style = Paint.Style.FILL; p.color = Color.WHITE
            c.drawCircle(bx, by, r, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 1.6f; p.color = 0xFFB5E3FF.toInt()
            c.drawCircle(bx, by, r, p)
            p.style = Paint.Style.FILL; p.color = 0xFFE3F6FF.toInt()
            c.drawCircle(bx - r * 0.35f, by - r * 0.35f, r * 0.25f, p)
        }
    }

    fun waxSmear(c: Canvas, x: Float, y: Float, rng: Rng) {
        p.shader = null; p.style = Paint.Style.FILL
        p.color = 0x40FFF6FF
        c.drawCircle(x + rng.range(-6f, 6f), y + rng.range(-6f, 6f), rng.range(18f, 26f), p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.5f; p.color = 0x55FFFFFF
        c.drawArc(x - 16f, y - 16f, x + 16f, y + 16f, rng.range(0f, 360f), 200f, false, p)
        p.style = Paint.Style.FILL
    }
}

/** Discrete bits of grime: leaves, bird mess, bugs, ice, graffiti, goo. */
class Spot(val kind: SpotKind, val x: Float, val y: Float, val r: Float, val tough: Boolean, val seed: Int) {
    var hp = 1f
    val rot = (seed * 37 % 360).toFloat()
}

object SpotArt {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val s = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val path = Path()

    fun draw(c: Canvas, sp: Spot) {
        if (sp.hp <= 0f) return
        val a = sp.hp.coerceIn(0f, 1f)
        val sc = 0.6f + 0.4f * a
        c.save(); c.translate(sp.x, sp.y); c.scale(sc, sc); c.rotate(sp.rot)
        val r = sp.r
        fun col(v: Int) = Col.alpha(v, a)
        when (sp.kind) {
            SpotKind.LEAF -> {
                path.reset(); path.moveTo(-r, 0f); path.quadTo(0f, -r * 0.9f, r, 0f); path.quadTo(0f, r * 0.9f, -r, 0f); path.close()
                p.color = col(if (sp.seed % 2 == 0) 0xFFFF9A2B.toInt() else 0xFF5FBF3A.toInt()); c.drawPath(path, p)
                s.color = col(0xFF7A4A10.toInt()); s.strokeWidth = 2f; c.drawLine(-r, 0f, r * 0.8f, 0f, s)
            }
            SpotKind.BIRD -> {
                p.color = col(Color.WHITE); c.drawCircle(0f, 0f, r * 0.8f, p); c.drawCircle(r * 0.6f, r * 0.4f, r * 0.4f, p); c.drawCircle(-r * 0.5f, r * 0.6f, r * 0.35f, p)
                p.color = col(0xFFD8D8C8.toInt()); c.drawCircle(-r * 0.15f, -r * 0.1f, r * 0.3f, p)
                p.color = col(0xFF6B5A3A.toInt()); c.drawCircle(r * 0.15f, r * 0.15f, r * 0.15f, p)
            }
            SpotKind.MUD -> {
                p.color = col(0xFF4A2A10.toInt()); c.drawCircle(0f, 0f, r * 0.8f, p)
                for (k in 0 until 6) { c.save(); c.rotate(k * 60f); c.drawCircle(r * 0.9f, 0f, r * 0.25f, p); c.restore() }
            }
            SpotKind.BUG -> {
                p.color = col(0xFFC8E040.toInt()); c.drawCircle(0f, 0f, r * 0.7f, p)
                for (k in 0 until 5) { c.save(); c.rotate(k * 72f + 10f); c.drawCircle(r * 0.8f, 0f, r * 0.2f, p); c.restore() }
                s.color = col(0xFF3A2A10.toInt()); s.strokeWidth = 2.5f
                c.drawLine(-r * 0.3f, -r * 0.3f, -r * 0.6f, -r * 0.9f, s); c.drawLine(r * 0.3f, -r * 0.3f, r * 0.6f, -r * 0.9f, s)
                p.color = col(0xFF3A2A10.toInt()); c.drawCircle(0f, 0f, r * 0.25f, p)
            }
            SpotKind.ICE -> {
                path.reset()
                for (k in 0 until 6) { val ang = k * 60f * Math.PI.toFloat() / 180f; val rr = if (k % 2 == 0) r else r * 0.75f
                    val x = rr * kotlin.math.cos(ang); val y = rr * kotlin.math.sin(ang); if (k == 0) path.moveTo(x, y) else path.lineTo(x, y) }
                path.close()
                p.color = col(0xFFBFE9FF.toInt()); c.drawPath(path, p)
                s.color = col(Color.WHITE); s.strokeWidth = 3f; c.drawPath(path, s)
                c.drawLine(-r * 0.4f, -r * 0.2f, r * 0.1f, -r * 0.5f, s)
            }
            SpotKind.GRAFFITI -> {
                s.strokeWidth = 6f; s.color = col(if (sp.seed % 2 == 0) 0xFFFF4FD8.toInt() else 0xFF5EF0FF.toInt())
                path.reset(); path.moveTo(-r, r * 0.3f); path.quadTo(-r * 0.5f, -r, 0f, 0f); path.quadTo(r * 0.5f, r, r, -r * 0.3f)
                c.drawPath(path, s)
                s.strokeWidth = 4f; s.color = col(0xFFFFE14D.toInt()); c.drawCircle(0f, -r * 0.2f, r * 0.3f, s)
            }
            SpotKind.GOO -> {
                p.color = col(if (sp.seed % 2 == 0) 0xFF7DFF5C.toInt() else 0xFFFF6FD8.toInt())
                c.drawCircle(0f, 0f, r * 0.8f, p); c.drawCircle(r * 0.5f, r * 0.5f, r * 0.4f, p)
                p.color = col(Color.WHITE); c.drawCircle(-r * 0.25f, -r * 0.1f, r * 0.22f, p); c.drawCircle(r * 0.25f, -r * 0.1f, r * 0.22f, p)
                p.color = col(0xFF1A1030.toInt()); c.drawCircle(-r * 0.2f, -r * 0.05f, r * 0.1f, p); c.drawCircle(r * 0.3f, -r * 0.05f, r * 0.1f, p)
            }
        }
        if (sp.tough) { s.color = col(0x99FFFFFF.toInt()); s.strokeWidth = 2f; c.drawCircle(0f, 0f, r * 1.05f, s) }
        c.restore()
    }

    fun dist(ax: Float, ay: Float, bx: Float, by: Float) = sqrt((ax - bx) * (ax - bx) + (ay - by) * (ay - by))
}
