package com.sudzy.carwash.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Typefaces are injected at startup (from assets on device, from files in previews). */
object Fonts {
    var title: Typeface = Typeface.DEFAULT_BOLD
    var body: Typeface = Typeface.DEFAULT_BOLD
}

object Col {
    const val NAVY = 0xFF0B3A9E.toInt()
    const val DEEP = 0xFF1A2560.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
    const val YELLOW = 0xFFFFE14D.toInt()
    const val GOLD = 0xFFFFC21A.toInt()
    const val ORANGE = 0xFFFF8A2B.toInt()
    const val PINK = 0xFFFF5D9E.toInt()
    const val RED = 0xFFFF3B6B.toInt()
    const val GREEN = 0xFF2FD27A.toInt()
    const val LIME = 0xFF7DFF5C.toInt()
    const val CYAN = 0xFF5EF0FF.toInt()
    const val SKY = 0xFF2AA8FF.toInt()
    const val BLUE = 0xFF2D6CF0.toInt()
    const val PURPLE = 0xFFA46BFF.toInt()
    const val INK = 0xFF3554A8.toInt()

    fun shade(c: Int, a: Float): Int {
        val f = { v: Int -> (if (a > 0) v + (255 - v) * a else v * (1 + a)).toInt().coerceIn(0, 255) }
        return Color.argb(Color.alpha(c), f(Color.red(c)), f(Color.green(c)), f(Color.blue(c)))
    }

    fun alpha(c: Int, a: Float): Int = (c and 0x00FFFFFF) or ((a.coerceIn(0f, 1f) * 255).toInt() shl 24)

    fun lerp(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        return Color.argb(
            (Color.alpha(a) + (Color.alpha(b) - Color.alpha(a)) * u).toInt(),
            (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt(),
        )
    }
}

object Gfx {
    val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()

    fun p(color: Int): Paint = fill.apply { shader = null; this.color = color; alpha = Color.alpha(color) }
    fun s(color: Int, w: Float): Paint = stroke.apply { shader = null; this.color = color; strokeWidth = w }

    fun vGrad(top: Float, bottom: Float, c1: Int, c2: Int): Paint = fill.apply {
        color = Color.WHITE
        shader = LinearGradient(0f, top, 0f, bottom, c1, c2, Shader.TileMode.CLAMP)
    }

    fun vGrad3(top: Float, bottom: Float, c1: Int, c2: Int, c3: Int, mid: Float = 0.45f): Paint = fill.apply {
        color = Color.WHITE
        shader = LinearGradient(0f, top, 0f, bottom, intArrayOf(c1, c2, c3), floatArrayOf(0f, mid, 1f), Shader.TileMode.CLAMP)
    }

    fun radial(cx: Float, cy: Float, r: Float, c1: Int, c2: Int): Paint = fill.apply {
        color = Color.WHITE
        shader = RadialGradient(cx, cy, max(r, 1f), c1, c2, Shader.TileMode.CLAMP)
    }

    // ---------------- text ----------------
    fun textPaint(size: Float, tf: Typeface = Fonts.title, align: Paint.Align = Paint.Align.CENTER): Paint =
        text.apply { reset(); isAntiAlias = true; typeface = tf; textSize = size; textAlign = align }

    fun measure(t: String, size: Float, tf: Typeface = Fonts.title): Float = textPaint(size, tf).measureText(t)

    /** Chunky cartoon text: drop shadow, thick outline, fill, optional gloss. */
    fun outlined(
        c: Canvas, t: String, x: Float, y: Float, size: Float, fillColor: Int, outline: Int,
        align: Paint.Align = Paint.Align.CENTER, strokeMul: Float = 0.16f, depth: Float = 0.09f,
        tf: Typeface = Fonts.title, alpha: Float = 1f,
    ) {
        val tp = textPaint(size, tf, align)
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        tp.style = Paint.Style.FILL_AND_STROKE; tp.strokeJoin = Paint.Join.ROUND
        tp.strokeWidth = size * strokeMul; tp.color = outline; tp.alpha = a
        c.drawText(t, x, y + size * depth, tp)
        c.drawText(t, x, y, tp)
        tp.style = Paint.Style.FILL; tp.color = fillColor; tp.alpha = a
        c.drawText(t, x, y, tp)
    }

    fun plain(c: Canvas, t: String, x: Float, y: Float, size: Float, color: Int,
              align: Paint.Align = Paint.Align.CENTER, tf: Typeface = Fonts.body) {
        val tp = textPaint(size, tf, align); tp.color = color
        c.drawText(t, x, y, tp)
    }

    // ---------------- shapes ----------------
    fun starPath(cx: Float, cy: Float, R: Float, r: Float, out: Path = Path()): Path {
        out.reset()
        for (i in 0 until 10) {
            val a = PI / 5 * i - PI / 2
            val rr = if (i % 2 == 0) R else r
            val x = cx + rr * cos(a).toFloat(); val y = cy + rr * sin(a).toFloat()
            if (i == 0) out.moveTo(x, y) else out.lineTo(x, y)
        }
        out.close(); return out
    }

    fun star(c: Canvas, cx: Float, cy: Float, R: Float, filled: Boolean, outline: Int = Color.WHITE) {
        val sp = starPath(cx, cy + R * 0.12f, R, R * 0.48f, path)
        c.drawPath(sp, p(if (filled) 0xFFC77800.toInt() else 0xFF4A5680.toInt()))
        starPath(cx, cy, R, R * 0.48f, path)
        c.drawPath(path, if (filled) vGrad(cy - R, cy + R, 0xFFFFF27A.toInt(), 0xFFFFB300.toInt()) else p(0xFF7D8DB0.toInt()))
        c.drawPath(path, s(outline, R * 0.14f))
        if (filled) {
            starPath(cx - R * 0.15f, cy - R * 0.2f, R * 0.32f, R * 0.14f, path)
            c.drawPath(path, p(0xCCFFFBD0.toInt()))
        }
    }

    /** Four point twinkle. */
    fun sparkle(c: Canvas, x: Float, y: Float, sz: Float, color: Int = Color.WHITE, a: Float = 1f) {
        if (sz <= 0.5f || a <= 0f) return
        path.reset()
        path.moveTo(x, y - sz); path.quadTo(x, y, x + sz, y); path.quadTo(x, y, x, y + sz)
        path.quadTo(x, y, x - sz, y); path.quadTo(x, y, x, y - sz); path.close()
        val pp = p(color); pp.alpha = (a.coerceIn(0f, 1f) * 255).toInt()
        c.drawPath(path, pp)
    }

    private var bubbleBmp: Bitmap? = null
    private fun bubbleSprite(): Bitmap {
        bubbleBmp?.let { return it }
        val n = 128
        val b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        val bc = Canvas(b)
        val r = n / 2f - 2
        bc.drawCircle(n / 2f, n / 2f, r, radial(n * 0.4f, n * 0.38f, r * 1.1f, 0x20D8F6FF, 0x55BFE9FF))
        bc.drawCircle(n / 2f, n / 2f, r - 2, s(0xBBFFFFFF.toInt(), 3f))
        val arc = RectF(n * 0.18f, n * 0.18f, n * 0.82f, n * 0.82f)
        bc.drawArc(arc, 200f, 60f, false, s(0xEEFFFFFF.toInt(), 7f))
        bc.drawCircle(n * 0.66f, n * 0.7f, 5f, p(0x99FFFFFF.toInt()))
        // faint rainbow rim
        bc.drawArc(RectF(4f, 4f, n - 4f, n - 4f), 20f, 70f, false, s(0x55FF8AD8, 4f))
        bc.drawArc(RectF(4f, 4f, n - 4f, n - 4f), 100f, 60f, false, s(0x557AF3FF, 4f))
        bubbleBmp = b
        return b
    }

    private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    fun bubble(c: Canvas, x: Float, y: Float, r: Float, a: Float = 1f) {
        if (r <= 0f) return
        rect.set(x - r, y - r, x + r, y + r)
        bmpPaint.alpha = (a.coerceIn(0f, 1f) * 255).toInt()
        c.drawBitmap(bubbleSprite(), null, rect, bmpPaint)
    }

    fun coin(c: Canvas, x: Float, y: Float, r: Float) {
        c.drawCircle(x, y + r * 0.1f, r, p(0xFFB36B00.toInt()))
        c.drawCircle(x, y, r, vGrad(y - r, y + r, 0xFFFFE066.toInt(), 0xFFFFB000.toInt()))
        c.drawCircle(x, y, r, s(0xFFB36B00.toInt(), r * 0.14f))
        c.drawCircle(x, y, r * 0.68f, s(0xFFE39A00.toInt(), r * 0.1f))
        starPath(x, y, r * 0.42f, r * 0.2f, path); c.drawPath(path, p(0xFFE39A00.toInt()))
        rect.set(x - r * 0.75f, y - r * 0.75f, x + r * 0.75f, y + r * 0.75f)
        c.drawArc(rect, 200f, 60f, false, s(0xCCFFFFFF.toInt(), r * 0.14f))
    }

    fun roundRect(c: Canvas, l: Float, t: Float, r: Float, b: Float, rad: Float, paint: Paint) {
        rect.set(l, t, r, b); c.drawRoundRect(rect, rad, rad, paint)
    }

    /** Candy button: drop shadow, vertical gradient, white border, top gloss. */
    fun candy(c: Canvas, l: Float, t: Float, r: Float, b: Float, top: Int, bottom: Int, rad: Float,
              border: Float = 8f, depth: Float = 14f) {
        roundRect(c, l, t + depth, r, b + depth, rad, p(Col.shade(bottom, -0.4f)))
        roundRect(c, l, t, r, b, rad, vGrad(t, b, top, bottom))
        val gl = Col.alpha(Color.WHITE, 0.28f)
        roundRect(c, l + border * 1.6f, t + border * 1.4f, r - border * 1.6f, t + (b - t) * 0.45f, rad * 0.7f, p(gl))
        if (border > 0) roundRect(c, l, t, r, b, rad, s(Color.WHITE, border))
    }

    fun pill(c: Canvas, x: Float, y: Float, w: Float, h: Float, label: String, icon: (Canvas, Float, Float, Float) -> Unit) {
        roundRect(c, x, y, x + w, y + h, h / 2, p(0x73001E5A))
        roundRect(c, x, y, x + w, y + h, h / 2, s(0x88FFFFFF.toInt(), 6f))
        icon(c, x + h * 0.35f, y + h / 2, h * 0.62f)
        outlined(c, label, x + h * 0.9f + (w - h * 0.9f) / 2, y + h * 0.68f, h * 0.5f, Color.WHITE, 0xFF0A2A70.toInt(), strokeMul = 0.1f, depth = 0.05f)
    }

    fun rays(c: Canvas, cx: Float, cy: Float, radius: Float, count: Int, rot: Float, color: Int) {
        path.reset()
        val step = 2 * PI / count
        for (i in 0 until count) {
            val a0 = rot + step * i; val a1 = a0 + step / 2
            path.moveTo(cx, cy)
            path.lineTo(cx + radius * cos(a0).toFloat(), cy + radius * sin(a0).toFloat())
            path.lineTo(cx + radius * cos(a1).toFloat(), cy + radius * sin(a1).toFloat())
            path.close()
        }
        c.drawPath(path, p(color))
    }

    /** Path bounds (the one-arg overload only exists from API 35). */
    @Suppress("DEPRECATION")
    fun bounds(path: Path, out: RectF): RectF { path.computeBounds(out, true); return out }

    fun clamp01(v: Float) = min(1f, max(0f, v))
}
