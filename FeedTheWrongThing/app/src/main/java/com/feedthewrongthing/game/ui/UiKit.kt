package com.feedthewrongthing.game.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class Icon { PLAY, PAUSE, GEAR, BOOK, SOUND, MUTE, MUSIC, HOME, REPLAY, GRID, BULB, STAR, LOCK, GIFT, LEFT, RIGHT, CLOSE, CHECK, VIDEO, HEART, EXIT, HANGER, VIBRATE }

/** Drawing helpers for the chunky, toy-like 2D interface. All sizes are in pixels. */
object UiKit {
    const val INK = 0xFF2B2340.toInt()
    const val SUN = 0xFFFFC21F.toInt()
    const val CORAL = 0xFFFF6B6B.toInt()
    const val GRAPE = 0xFF8E6CF0.toInt()
    const val SKY = 0xFF5B8CFF.toInt()
    const val GO = 0xFF3FC48A.toInt()
    const val CREAM = 0xFFFFF6E5.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
    const val MUTED = 0xFF5E557A.toInt()

    val heavy: Typeface = if (Build.VERSION.SDK_INT >= 28) Typeface.create(Typeface.SANS_SERIF, 900, false)
        else Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    val bold: Typeface = if (Build.VERSION.SDK_INT >= 28) Typeface.create(Typeface.SANS_SERIF, 700, false)
        else Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = heavy; textAlign = Paint.Align.CENTER }
    val bmp = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val path = Path()
    private val r = RectF()
    private val r2 = RectF()
    private val grayFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f); setScale(0.25f, 0.22f, 0.35f, 1f) })

    fun darken(c: Int, k: Float): Int {
        val a = Color.alpha(c)
        return Color.argb(a, (Color.red(c) * k).toInt(), (Color.green(c) * k).toInt(), (Color.blue(c) * k).toInt())
    }

    fun lighten(c: Int, k: Float): Int {
        val a = Color.alpha(c)
        return Color.argb(a, (Color.red(c) + (255 - Color.red(c)) * k).toInt(),
            (Color.green(c) + (255 - Color.green(c)) * k).toInt(), (Color.blue(c) + (255 - Color.blue(c)) * k).toInt())
    }

    fun withAlpha(c: Int, a: Float): Int = (c and 0x00FFFFFF) or ((a.coerceIn(0f, 1f) * 255).toInt() shl 24)

    /** Soft card with drop shadow. */
    fun panel(c: Canvas, rect: RectF, color: Int, radius: Float, shadow: Float = radius * 0.25f) {
        fill.shader = null
        r.set(rect); r.offset(0f, shadow)
        fill.color = 0x40140A30
        c.drawRoundRect(r, radius, radius, fill)
        fill.color = color
        c.drawRoundRect(rect, radius, radius, fill)
    }

    /** Big chunky "3D" pill button. */
    fun button(c: Canvas, rect: RectF, base: Int, pressed: Boolean, label: String?, icon: Icon? = null,
               textColor: Int = WHITE, size: Float = rect.height() * 0.42f) {
        val depth = rect.height() * 0.11f
        val off = if (pressed) depth * 0.75f else 0f
        val rad = rect.height() / 2f
        r.set(rect.left, rect.top + depth + depth * 0.6f, rect.right, rect.bottom + depth + depth * 0.6f)
        fill.shader = null
        fill.color = 0x38140A30
        c.drawRoundRect(r, rad, rad, fill)
        r.set(rect.left, rect.top + depth, rect.right, rect.bottom + depth)
        fill.color = darken(base, 0.72f)
        c.drawRoundRect(r, rad, rad, fill)
        r.set(rect); r.offset(0f, off)
        fill.shader = LinearGradient(0f, r.top, 0f, r.bottom, lighten(base, 0.35f), base, Shader.TileMode.CLAMP)
        c.drawRoundRect(r, rad, rad, fill)
        fill.shader = null
        r2.set(r.left + rad * 0.5f, r.top + r.height() * 0.08f, r.right - rad * 0.5f, r.top + r.height() * 0.32f)
        fill.color = 0x55FFFFFF
        c.drawRoundRect(r2, r2.height() / 2f, r2.height() / 2f, fill)
        val cy = r.centerY()
        var cx = r.centerX()
        if (label != null) {
            text.textSize = size
            val tw = text.measureText(label)
            if (icon != null) {
                val iconW = size * 1.05f
                val total = iconW + size * 0.3f + tw
                val ix = cx - total / 2f + iconW / 2f
                drawIcon(c, icon, ix, cy, iconW * 0.5f, textColor)
                cx = ix + iconW / 2f + size * 0.3f + tw / 2f
            }
            label(c, label, cx, cy + size * 0.36f, size, textColor, if (textColor == WHITE) darken(base, 0.5f) else 0)
        } else if (icon != null) {
            drawIcon(c, icon, cx, cy, rect.height() * 0.28f, textColor)
        }
    }

    /** Round glossy icon button. */
    fun roundButton(c: Canvas, cx: Float, cy: Float, radius: Float, base: Int, icon: Icon, pressed: Boolean, iconColor: Int = WHITE) {
        val depth = radius * 0.16f
        val off = if (pressed) depth * 0.75f else 0f
        fill.shader = null
        fill.color = 0x38140A30
        c.drawCircle(cx, cy + depth * 1.8f, radius, fill)
        fill.color = darken(base, 0.7f)
        c.drawCircle(cx, cy + depth, radius, fill)
        fill.shader = RadialGradient(cx - radius * 0.3f, cy + off - radius * 0.35f, radius * 1.4f,
            lighten(base, 0.45f), base, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy + off, radius, fill)
        fill.shader = null
        fill.color = 0x40FFFFFF
        r.set(cx - radius * 0.55f, cy + off - radius * 0.85f, cx + radius * 0.35f, cy + off - radius * 0.45f)
        c.drawOval(r, fill)
        drawIcon(c, icon, cx, cy + off, radius * 0.5f, iconColor)
    }

    /** Text with a dark outline so it reads on any background. */
    fun label(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, outline: Int = 0,
              align: Paint.Align = Paint.Align.CENTER, face: Typeface = heavy) {
        text.textSize = size
        text.textAlign = align
        text.typeface = face
        text.shader = null
        if (outline != 0) {
            text.style = Paint.Style.STROKE
            text.strokeWidth = size * 0.16f
            text.strokeJoin = Paint.Join.ROUND
            text.color = outline
            c.drawText(s, x, y, text)
            text.style = Paint.Style.FILL
        }
        text.color = color
        c.drawText(s, x, y, text)
        text.textAlign = Paint.Align.CENTER
        text.typeface = heavy
    }

    /** Big extruded cartoon title text. */
    fun title(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, depth: Int = 6, align: Paint.Align = Paint.Align.CENTER) {
        text.textSize = size
        text.textAlign = align
        text.typeface = heavy
        text.style = Paint.Style.FILL
        val step = size * 0.022f
        fill.color = 0x30140A30
        text.color = 0x35140A30
        c.drawText(s, x, y + step * (depth + 4), text)
        for (k in depth downTo 1) {
            text.color = darken(color, 0.86f - k * 0.05f)
            c.drawText(s, x, y + step * k, text)
        }
        text.style = Paint.Style.STROKE
        text.strokeWidth = size * 0.06f
        text.color = darken(color, 0.7f)
        c.drawText(s, x, y, text)
        text.style = Paint.Style.FILL
        text.shader = LinearGradient(0f, y - size * 0.8f, 0f, y, lighten(color, 0.35f), color, Shader.TileMode.CLAMP)
        c.drawText(s, x, y, text)
        text.shader = null
        text.textAlign = Paint.Align.CENTER
    }

    fun starShape(cx: Float, cy: Float, outer: Float, inner: Float = outer * 0.48f): Path {
        path.reset()
        for (i in 0 until 10) {
            val rr = if (i % 2 == 0) outer else inner
            val a = -Math.PI / 2 + i * Math.PI / 5
            val px = cx + (rr * cos(a)).toFloat()
            val py = cy + (rr * sin(a)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        return path
    }

    fun star(c: Canvas, cx: Float, cy: Float, radius: Float, filled: Boolean) {
        val p = starShape(cx, cy, radius)
        stroke.pathEffect = android.graphics.CornerPathEffect(radius * 0.18f)
        fill.pathEffect = android.graphics.CornerPathEffect(radius * 0.18f)
        if (filled) {
            fill.shader = RadialGradient(cx - radius * 0.3f, cy - radius * 0.3f, radius * 1.4f, 0xFFFFF4B0.toInt(), 0xFFFFB800.toInt(), Shader.TileMode.CLAMP)
        } else {
            fill.shader = null
            fill.color = 0xFFE6E0F2.toInt()
        }
        c.drawPath(p, fill)
        fill.shader = null
        stroke.color = if (filled) 0xFFC47F00.toInt() else 0xFFB9B0D3.toInt()
        stroke.strokeWidth = radius * 0.12f
        c.drawPath(p, stroke)
        stroke.pathEffect = null
        fill.pathEffect = null
    }

    fun toggle(c: Canvas, rect: RectF, on: Boolean) {
        val rad = rect.height() / 2f
        fill.shader = null
        fill.color = if (on) GO else 0xFFD8D1EA.toInt()
        c.drawRoundRect(rect, rad, rad, fill)
        stroke.color = INK
        stroke.strokeWidth = rect.height() * 0.08f
        c.drawRoundRect(rect, rad, rad, stroke)
        val kx = if (on) rect.right - rad else rect.left + rad
        fill.color = WHITE
        c.drawCircle(kx, rect.centerY(), rad * 0.72f, fill)
        c.drawCircle(kx, rect.centerY(), rad * 0.72f, stroke)
    }

    fun bitmap(c: Canvas, b: Bitmap?, cx: Float, cy: Float, size: Float, silhouette: Boolean = false, alpha: Int = 255) {
        if (b == null) {
            fill.shader = null
            fill.color = 0x30FFFFFF
            c.drawCircle(cx, cy, size * 0.3f, fill)
            return
        }
        r.set(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f)
        bmp.colorFilter = if (silhouette) grayFilter else null
        bmp.alpha = alpha
        c.drawBitmap(b, null, r, bmp)
        bmp.colorFilter = null
        bmp.alpha = 255
    }

    /** Stylised hand used in the tutorial. */
    fun hand(c: Canvas, x: Float, y: Float, s: Float, pressed: Boolean) {
        fill.shader = null
        val k = if (pressed) 0.9f else 1f
        fill.color = 0x40140A30
        c.drawCircle(x + s * 0.1f, y + s * 0.95f, s * 0.42f * k, fill)
        fill.color = 0xFFFFE0C2.toInt()
        stroke.color = INK
        stroke.strokeWidth = s * 0.07f
        r.set(x - s * 0.13f, y - s * 0.05f, x + s * 0.13f, y + s * 0.7f)
        c.drawRoundRect(r, s * 0.13f, s * 0.13f, fill)
        c.drawRoundRect(r, s * 0.13f, s * 0.13f, stroke)
        r.set(x - s * 0.36f, y + s * 0.42f, x + s * 0.4f, y + s * 1.1f)
        c.drawRoundRect(r, s * 0.25f, s * 0.25f, fill)
        c.drawRoundRect(r, s * 0.25f, s * 0.25f, stroke)
        if (pressed) {
            stroke.color = 0xAAFFFFFF.toInt()
            c.drawCircle(x, y, s * 0.35f, stroke)
        }
    }

    fun drawIcon(c: Canvas, icon: Icon, cx: Float, cy: Float, s: Float, color: Int) {
        fill.shader = null
        fill.color = color
        stroke.color = color
        stroke.strokeWidth = s * 0.28f
        path.reset()
        when (icon) {
            Icon.PLAY -> {
                path.moveTo(cx - s * 0.55f, cy - s * 0.8f); path.lineTo(cx + s * 0.85f, cy); path.lineTo(cx - s * 0.55f, cy + s * 0.8f); path.close()
                fill.pathEffect = android.graphics.CornerPathEffect(s * 0.2f)
                c.drawPath(path, fill)
                fill.pathEffect = null
            }
            Icon.PAUSE -> {
                r.set(cx - s * 0.65f, cy - s * 0.75f, cx - s * 0.18f, cy + s * 0.75f); c.drawRoundRect(r, s * 0.15f, s * 0.15f, fill)
                r.set(cx + s * 0.18f, cy - s * 0.75f, cx + s * 0.65f, cy + s * 0.75f); c.drawRoundRect(r, s * 0.15f, s * 0.15f, fill)
            }
            Icon.GEAR -> {
                for (k in 0 until 8) {
                    val a = k * Math.PI / 4
                    c.save(); c.rotate(Math.toDegrees(a).toFloat(), cx, cy)
                    r.set(cx - s * 0.2f, cy - s * 0.98f, cx + s * 0.2f, cy - s * 0.5f); c.drawRoundRect(r, s * 0.08f, s * 0.08f, fill)
                    c.restore()
                }
                c.drawCircle(cx, cy, s * 0.66f, fill)
                fill.color = 0x66000000; c.drawCircle(cx, cy, s * 0.26f, fill)
            }
            Icon.BOOK -> {
                path.moveTo(cx, cy - s * 0.55f)
                path.quadTo(cx - s * 0.5f, cy - s * 0.85f, cx - s * 0.95f, cy - s * 0.6f)
                path.lineTo(cx - s * 0.95f, cy + s * 0.75f)
                path.quadTo(cx - s * 0.5f, cy + s * 0.5f, cx, cy + s * 0.8f)
                path.quadTo(cx + s * 0.5f, cy + s * 0.5f, cx + s * 0.95f, cy + s * 0.75f)
                path.lineTo(cx + s * 0.95f, cy - s * 0.6f)
                path.quadTo(cx + s * 0.5f, cy - s * 0.85f, cx, cy - s * 0.55f)
                c.drawPath(path, fill)
                stroke.color = 0x55000000; stroke.strokeWidth = s * 0.1f
                c.drawLine(cx, cy - s * 0.5f, cx, cy + s * 0.75f, stroke)
            }
            Icon.SOUND, Icon.MUTE -> {
                path.moveTo(cx - s * 0.9f, cy - s * 0.3f); path.lineTo(cx - s * 0.45f, cy - s * 0.3f); path.lineTo(cx + s * 0.05f, cy - s * 0.75f)
                path.lineTo(cx + s * 0.05f, cy + s * 0.75f); path.lineTo(cx - s * 0.45f, cy + s * 0.3f); path.lineTo(cx - s * 0.9f, cy + s * 0.3f); path.close()
                c.drawPath(path, fill)
                stroke.strokeWidth = s * 0.17f
                if (icon == Icon.SOUND) {
                    r.set(cx - s * 0.2f, cy - s * 0.45f, cx + s * 0.6f, cy + s * 0.45f); c.drawArc(r, -50f, 100f, false, stroke)
                    r.set(cx - s * 0.2f, cy - s * 0.85f, cx + s * 0.95f, cy + s * 0.85f); c.drawArc(r, -50f, 100f, false, stroke)
                } else {
                    c.drawLine(cx + s * 0.3f, cy - s * 0.35f, cx + s * 0.95f, cy + s * 0.35f, stroke)
                    c.drawLine(cx + s * 0.95f, cy - s * 0.35f, cx + s * 0.3f, cy + s * 0.35f, stroke)
                }
            }
            Icon.MUSIC -> {
                stroke.strokeWidth = s * 0.18f
                c.drawLine(cx - s * 0.2f, cy + s * 0.55f, cx - s * 0.2f, cy - s * 0.7f, stroke)
                c.drawLine(cx + s * 0.75f, cy + s * 0.35f, cx + s * 0.75f, cy - s * 0.9f, stroke)
                c.drawLine(cx - s * 0.2f, cy - s * 0.7f, cx + s * 0.75f, cy - s * 0.9f, stroke)
                r.set(cx - s * 0.75f, cy + s * 0.3f, cx - s * 0.1f, cy + s * 0.8f); c.drawOval(r, fill)
                r.set(cx + s * 0.2f, cy + s * 0.1f, cx + s * 0.85f, cy + s * 0.6f); c.drawOval(r, fill)
            }
            Icon.HOME -> {
                path.moveTo(cx - s * 0.9f, cy - s * 0.05f); path.lineTo(cx, cy - s * 0.85f); path.lineTo(cx + s * 0.9f, cy - s * 0.05f)
                path.lineTo(cx + s * 0.62f, cy - s * 0.05f); path.lineTo(cx + s * 0.62f, cy + s * 0.8f); path.lineTo(cx + s * 0.18f, cy + s * 0.8f)
                path.lineTo(cx + s * 0.18f, cy + s * 0.3f); path.lineTo(cx - s * 0.18f, cy + s * 0.3f); path.lineTo(cx - s * 0.18f, cy + s * 0.8f)
                path.lineTo(cx - s * 0.62f, cy + s * 0.8f); path.lineTo(cx - s * 0.62f, cy - s * 0.05f); path.close()
                fill.pathEffect = android.graphics.CornerPathEffect(s * 0.12f)
                c.drawPath(path, fill)
                fill.pathEffect = null
            }
            Icon.REPLAY -> {
                stroke.strokeWidth = s * 0.26f
                r.set(cx - s * 0.7f, cy - s * 0.7f, cx + s * 0.7f, cy + s * 0.7f)
                c.drawArc(r, -60f, 290f, false, stroke)
                path.moveTo(cx + s * 0.2f, cy - s * 1.0f); path.lineTo(cx + s * 0.85f, cy - s * 0.65f); path.lineTo(cx + s * 0.25f, cy - s * 0.2f); path.close()
                c.drawPath(path, fill)
            }
            Icon.GRID -> {
                val q = s * 0.38f
                for (gx in intArrayOf(-1, 1)) for (gy in intArrayOf(-1, 1)) {
                    r.set(cx + gx * s * 0.47f - q, cy + gy * s * 0.47f - q, cx + gx * s * 0.47f + q, cy + gy * s * 0.47f + q)
                    c.drawRoundRect(r, q * 0.35f, q * 0.35f, fill)
                }
            }
            Icon.BULB -> {
                c.drawCircle(cx, cy - s * 0.2f, s * 0.62f, fill)
                r.set(cx - s * 0.3f, cy + s * 0.2f, cx + s * 0.3f, cy + s * 0.6f); c.drawRect(r, fill)
                r.set(cx - s * 0.3f, cy + s * 0.65f, cx + s * 0.3f, cy + s * 0.85f); c.drawRoundRect(r, s * 0.1f, s * 0.1f, fill)
                fill.color = 0x88FFFFFF.toInt(); c.drawCircle(cx - s * 0.2f, cy - s * 0.4f, s * 0.15f, fill)
            }
            Icon.STAR -> c.drawPath(starShape(cx, cy, s), fill)
            Icon.LOCK -> {
                stroke.strokeWidth = s * 0.22f
                r.set(cx - s * 0.45f, cy - s * 0.95f, cx + s * 0.45f, cy + s * 0.05f)
                c.drawArc(r, 180f, 180f, false, stroke)
                c.drawLine(cx - s * 0.45f, cy - s * 0.45f, cx - s * 0.45f, cy - s * 0.1f, stroke)
                c.drawLine(cx + s * 0.45f, cy - s * 0.45f, cx + s * 0.45f, cy - s * 0.1f, stroke)
                r.set(cx - s * 0.72f, cy - s * 0.2f, cx + s * 0.72f, cy + s * 0.85f); c.drawRoundRect(r, s * 0.18f, s * 0.18f, fill)
            }
            Icon.GIFT -> {
                r.set(cx - s * 0.85f, cy - s * 0.25f, cx + s * 0.85f, cy + s * 0.85f); c.drawRoundRect(r, s * 0.12f, s * 0.12f, fill)
                r.set(cx - s * 0.95f, cy - s * 0.55f, cx + s * 0.95f, cy - s * 0.15f); c.drawRoundRect(r, s * 0.1f, s * 0.1f, fill)
                stroke.strokeWidth = s * 0.16f
                r.set(cx - s * 0.6f, cy - s * 1.0f, cx, cy - s * 0.5f); c.drawOval(r, stroke)
                r.set(cx, cy - s * 1.0f, cx + s * 0.6f, cy - s * 0.5f); c.drawOval(r, stroke)
                fill.color = 0x44000000; r.set(cx - s * 0.12f, cy - s * 0.55f, cx + s * 0.12f, cy + s * 0.85f); c.drawRect(r, fill)
            }
            Icon.LEFT, Icon.RIGHT -> {
                stroke.strokeWidth = s * 0.32f
                val d = if (icon == Icon.LEFT) 1f else -1f
                path.moveTo(cx + d * s * 0.35f, cy - s * 0.7f); path.lineTo(cx - d * s * 0.35f, cy); path.lineTo(cx + d * s * 0.35f, cy + s * 0.7f)
                stroke.style = Paint.Style.STROKE
                c.drawPath(path, stroke)
            }
            Icon.CLOSE -> {
                stroke.strokeWidth = s * 0.3f
                c.drawLine(cx - s * 0.6f, cy - s * 0.6f, cx + s * 0.6f, cy + s * 0.6f, stroke)
                c.drawLine(cx + s * 0.6f, cy - s * 0.6f, cx - s * 0.6f, cy + s * 0.6f, stroke)
            }
            Icon.CHECK -> {
                stroke.strokeWidth = s * 0.3f
                path.moveTo(cx - s * 0.7f, cy); path.lineTo(cx - s * 0.15f, cy + s * 0.55f); path.lineTo(cx + s * 0.75f, cy - s * 0.55f)
                c.drawPath(path, stroke)
            }
            Icon.VIDEO -> {
                r.set(cx - s * 0.9f, cy - s * 0.55f, cx + s * 0.35f, cy + s * 0.55f); c.drawRoundRect(r, s * 0.2f, s * 0.2f, fill)
                path.moveTo(cx + s * 0.3f, cy); path.lineTo(cx + s * 0.95f, cy - s * 0.45f); path.lineTo(cx + s * 0.95f, cy + s * 0.45f); path.close()
                c.drawPath(path, fill)
            }
            Icon.HEART -> {
                path.moveTo(cx, cy + s * 0.8f)
                path.cubicTo(cx - s * 1.3f, cy - s * 0.1f, cx - s * 0.6f, cy - s * 1.0f, cx, cy - s * 0.35f)
                path.cubicTo(cx + s * 0.6f, cy - s * 1.0f, cx + s * 1.3f, cy - s * 0.1f, cx, cy + s * 0.8f)
                c.drawPath(path, fill)
            }
            Icon.EXIT -> {
                stroke.strokeWidth = s * 0.22f
                r.set(cx - s * 0.85f, cy - s * 0.8f, cx + s * 0.15f, cy + s * 0.8f); c.drawRoundRect(r, s * 0.12f, s * 0.12f, stroke)
                c.drawLine(cx - s * 0.2f, cy, cx + s * 0.9f, cy, stroke)
                c.drawLine(cx + s * 0.55f, cy - s * 0.35f, cx + s * 0.9f, cy, stroke)
                c.drawLine(cx + s * 0.55f, cy + s * 0.35f, cx + s * 0.9f, cy, stroke)
            }
            Icon.HANGER -> {
                stroke.strokeWidth = s * 0.18f
                path.moveTo(cx - s * 0.95f, cy + s * 0.6f); path.lineTo(cx, cy - s * 0.05f); path.lineTo(cx + s * 0.95f, cy + s * 0.6f); path.close()
                c.drawPath(path, stroke)
                r.set(cx - s * 0.25f, cy - s * 0.75f, cx + s * 0.25f, cy - s * 0.25f); c.drawArc(r, 180f, 270f, false, stroke)
            }
            Icon.VIBRATE -> {
                r.set(cx - s * 0.35f, cy - s * 0.75f, cx + s * 0.35f, cy + s * 0.75f); c.drawRoundRect(r, s * 0.15f, s * 0.15f, fill)
                stroke.strokeWidth = s * 0.14f
                c.drawLine(cx - s * 0.65f, cy - s * 0.35f, cx - s * 0.65f, cy + s * 0.35f, stroke)
                c.drawLine(cx + s * 0.65f, cy - s * 0.35f, cx + s * 0.65f, cy + s * 0.35f, stroke)
            }
        }
        fill.color = color
    }

    fun clampRect(out: RectF, cx: Float, cy: Float, w: Float, h: Float): RectF {
        out.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
        return out
    }

    @Suppress("unused")
    fun minOf(a: Float, b: Float) = min(a, b)
}
