package com.sudzy.carwash.art

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Region
import android.graphics.Shader
import com.sudzy.carwash.engine.Col
import com.sudzy.carwash.engine.Gfx
import kotlin.math.sin

enum class CarType { SEDAN, HATCH, JEEP, PICKUP, SPORTS, MONSTER, ROCKET }

private fun svg(d: String): Path {
    // Tiny parser for the M/L/Q/Z commands used by our hand-drawn car outlines.
    val p = Path()
    val tok = Regex("[MLQZ]|-?\\d+(\\.\\d+)?").findAll(d).map { it.value }.toList()
    var i = 0; var cmd = 'M'
    fun n() = tok[i++].toFloat()
    while (i < tok.size) {
        val t = tok[i]
        if (t[0].isLetter()) { cmd = t[0]; i++; if (cmd == 'Z') { p.close(); continue } }
        when (cmd) {
            'M' -> { p.moveTo(n(), n()); cmd = 'L' }
            'L' -> p.lineTo(n(), n())
            'Q' -> p.quadTo(n(), n(), n(), n())
        }
    }
    return p
}

/**
 * Side-view car in a local 400x200 space (front faces right, ground at y=180).
 * All the in-game cars are built from these outlines.
 */
class CarModel(val type: CarType, val color: Int, val stripe: Int?) {
    val body: Path
    val windows: List<Path>
    val wheelX: FloatArray
    val wheelR: Float
    val lift: Float
    val headlight: FloatArray
    val taillight: FloatArray
    val doorX: Float
    val mask: Region

    init {
        when (type) {
            CarType.SEDAN -> {
                body = svg("M30 130 Q26 100 62 95 L112 90 Q142 46 192 42 L262 42 Q302 44 332 88 L360 95 Q388 101 384 130 Q384 149 364 150 L42 150 Q28 149 30 130 Z")
                windows = listOf(svg("M126 88 Q149 56 191 54 L224 54 L224 88 Z"), svg("M234 54 L259 54 Q290 56 313 88 L234 88 Z"))
                wheelX = floatArrayOf(105f, 300f); wheelR = 30f; lift = 0f
                headlight = floatArrayOf(372f, 112f); taillight = floatArrayOf(38f, 110f); doorX = 229f
            }
            CarType.HATCH -> {
                body = svg("M44 140 L44 76 Q46 44 82 42 L238 42 Q278 44 308 86 L354 96 Q384 102 382 130 Q382 149 362 150 L56 150 Q44 149 44 140 Z")
                windows = listOf(svg("M66 86 L68 62 Q70 54 84 54 L146 54 L146 86 Z"), svg("M156 54 L232 54 Q262 56 288 86 L156 86 Z"))
                wheelX = floatArrayOf(112f, 302f); wheelR = 30f; lift = 0f
                headlight = floatArrayOf(370f, 112f); taillight = floatArrayOf(50f, 100f); doorX = 151f
            }
            CarType.JEEP, CarType.MONSTER -> {
                body = svg("M28 140 L30 92 Q30 84 40 84 L80 84 L92 40 Q94 34 102 34 L262 34 Q270 34 274 42 L292 84 L370 86 Q384 88 384 104 L384 140 Q384 150 372 150 L40 150 Q28 150 28 140 Z")
                windows = listOf(svg("M104 44 L172 44 L172 82 L95 82 Z"), svg("M182 44 L260 44 L278 82 L182 82 Z"))
                wheelX = floatArrayOf(100f, 300f)
                wheelR = if (type == CarType.MONSTER) 52f else 34f
                lift = if (type == CarType.MONSTER) 40f else 0f
                headlight = floatArrayOf(376f, 100f); taillight = floatArrayOf(34f, 100f); doorX = 177f
            }
            CarType.PICKUP -> {
                body = svg("M24 138 L24 96 Q24 88 32 88 L188 88 L196 46 Q198 38 206 38 L270 38 Q282 38 290 48 L318 88 L360 94 Q384 98 384 124 L384 140 Q384 150 372 150 L36 150 Q24 150 24 138 Z")
                windows = listOf(svg("M208 48 L268 48 Q276 48 282 56 L302 84 L204 84 Z"))
                wheelX = floatArrayOf(96f, 306f); wheelR = 32f; lift = 0f
                headlight = floatArrayOf(374f, 110f); taillight = floatArrayOf(30f, 104f); doorX = 0f
            }
            CarType.SPORTS, CarType.ROCKET -> {
                body = svg("M24 128 Q22 108 50 104 L130 96 Q170 60 220 56 L262 56 Q300 58 334 92 L372 100 Q392 104 390 124 Q390 146 370 148 L40 148 Q24 147 24 128 Z")
                windows = listOf(svg("M150 94 Q180 66 222 64 L236 64 L236 94 Z"), svg("M246 64 L260 64 Q290 66 314 94 L246 94 Z"))
                wheelX = floatArrayOf(100f, 304f); wheelR = if (type == CarType.ROCKET) 26f else 29f; lift = 0f
                headlight = floatArrayOf(380f, 112f); taillight = floatArrayOf(32f, 114f); doorX = 241f
            }
        }
        mask = Region().apply { setPath(body, Region(-20, -20, 420, 220)) }
    }

    val light = Col.shade(color, 0.45f)
    val dark = Col.shade(color, -0.42f)
    val bodyShader: Shader = LinearGradient(0f, 30f, 0f, 152f, intArrayOf(light, color, dark), floatArrayOf(0f, 0.42f, 1f), Shader.TileMode.CLAMP)
    val windowShader: Shader = LinearGradient(120f, 40f, 300f, 100f, 0xFFD9F6FF.toInt(), 0xFF3B7FC9.toInt(), Shader.TileMode.CLAMP)
    fun wheelY() = 180f - wheelR
    fun inside(x: Float, y: Float) = mask.contains(x.toInt(), y.toInt())
}

object CarArt {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND }
    private val tmp = Path()
    private val rimShader = RadialGradient(-0.3f, -0.3f, 1.1f, Color.WHITE, 0xFF9AA6B8.toInt(), Shader.TileMode.CLAMP)

    private fun f(c: Int): Paint = fill.apply { shader = null; color = c }
    private fun l(c: Int, w: Float): Paint = line.apply { shader = null; color = c; strokeWidth = w }

    fun shadow(c: Canvas, m: CarModel) {
        c.drawOval(14f, 174f, 386f, 192f, f(0x48000000))
    }

    /** Body without wheels; caller is already in the car-lift frame. */
    fun body(c: Canvas, m: CarModel, shine: Float = 0f, time: Float = 0f) {
        val d = m.dark
        if (m.type == CarType.ROCKET) {
            // flame + fin behind the body
            val fl = 1f + 0.25f * sin(time * 30f)
            tmp.reset(); tmp.moveTo(28f, 112f); tmp.quadTo(-20f * fl, 102f, -46f * fl, 122f); tmp.quadTo(-16f * fl, 134f, 28f, 136f); tmp.close()
            c.drawPath(tmp, f(0xFFFF8A2B.toInt()))
            tmp.reset(); tmp.moveTo(28f, 116f); tmp.quadTo(-4f * fl, 112f, -22f * fl, 124f); tmp.quadTo(-4f * fl, 130f, 28f, 132f); tmp.close()
            c.drawPath(tmp, f(0xFFFFE14D.toInt()))
            tmp.reset(); tmp.moveTo(40f, 106f); tmp.lineTo(30f, 60f); tmp.lineTo(56f, 60f); tmp.lineTo(96f, 100f); tmp.close()
            c.drawPath(tmp, f(Col.shade(m.color, -0.2f))); c.drawPath(tmp, l(d, 5f))
        }
        if (m.type == CarType.JEEP || m.type == CarType.MONSTER) {
            c.drawCircle(22f, 112f, 26f, f(0xFF23232E.toInt())); c.drawCircle(22f, 112f, 13f, f(Col.shade(m.color, 0.2f)))
            c.drawLine(100f, 28f, 266f, 28f, l(0xFF3B3F58.toInt(), 6f))
            for (x in floatArrayOf(110f, 180f, 250f)) c.drawLine(x, 28f, x, 35f, l(0xFF3B3F58.toInt(), 5f))
        }
        fill.shader = m.bodyShader; fill.color = Color.WHITE
        c.drawPath(m.body, fill)
        c.save(); c.clipPath(m.body)
        for (x in m.wheelX) c.drawCircle(x, 150f + m.lift, m.wheelR + 8f, f(0xFF15151F.toInt()))
        c.drawRect(0f, 136f, 400f, 160f, f(Col.alpha(d, 0.5f)))
        m.stripe?.let { st ->
            tmp.reset(); tmp.moveTo(0f, 120f); tmp.lineTo(400f, 113f); tmp.lineTo(400f, 125f); tmp.lineTo(0f, 132f); tmp.close()
            c.drawPath(tmp, f(st))
        }
        if (m.type == CarType.PICKUP) c.drawLine(30f, 96f, 186f, 96f, l(Col.alpha(d, 0.6f), 4f))
        c.restore()
        c.drawPath(m.body, l(d, 5f))
        for (w in m.windows) {
            fill.shader = m.windowShader; fill.color = Color.WHITE; c.drawPath(w, fill)
            c.drawPath(w, l(d, 4f))
        }
        // glass glare
        val wb = android.graphics.RectF(); Gfx.bounds(m.windows[0], wb)
        c.save(); c.clipPath(m.windows[0])
        tmp.reset(); tmp.moveTo(wb.left + wb.width() * 0.3f, wb.bottom); tmp.lineTo(wb.left + wb.width() * 0.6f, wb.top); tmp.lineTo(wb.left + wb.width() * 0.72f, wb.top); tmp.lineTo(wb.left + wb.width() * 0.42f, wb.bottom); tmp.close()
        c.drawPath(tmp, f(0x88FFFFFF.toInt()))
        c.restore()
        if (m.windows.size > 1) {
            Gfx.bounds(m.windows[1], wb)
            c.save(); c.clipPath(m.windows[1])
            tmp.reset(); tmp.moveTo(wb.left + wb.width() * 0.15f, wb.bottom); tmp.lineTo(wb.left + wb.width() * 0.4f, wb.top); tmp.lineTo(wb.left + wb.width() * 0.52f, wb.top); tmp.lineTo(wb.left + wb.width() * 0.27f, wb.bottom); tmp.close()
            c.drawPath(tmp, f(0x77FFFFFF))
            c.restore()
        }
        if (m.doorX > 0f) c.drawLine(m.doorX, 92f, m.doorX, 144f, l(Col.alpha(d, 0.6f), 3.5f))
        val hx = if (m.doorX > 0f) m.doorX + 12f else 220f
        c.drawRoundRect(hx, 100f, hx + 22f, 106f, 3f, 3f, f(d))
        // highlight streak
        val y0 = if (m.type == CarType.SPORTS || m.type == CarType.ROCKET) 108f else 102f
        tmp.reset(); tmp.moveTo(64f, y0); tmp.quadTo(210f, y0 - 12f, 352f, y0 - 2f); tmp.lineTo(352f, y0 + 6f); tmp.quadTo(210f, y0 - 4f, 64f, y0 + 8f); tmp.close()
        c.drawPath(tmp, f(Col.alpha(Color.WHITE, 0.42f + 0.4f * shine)))
        // lights
        c.drawOval(m.headlight[0] - 11f, m.headlight[1] - 8f, m.headlight[0] + 11f, m.headlight[1] + 8f, f(0xFFFFF4A3.toInt()))
        c.drawOval(m.headlight[0] - 11f, m.headlight[1] - 8f, m.headlight[0] + 11f, m.headlight[1] + 8f, l(d, 3f))
        c.drawOval(m.taillight[0] - 7f, m.taillight[1] - 9f, m.taillight[0] + 7f, m.taillight[1] + 9f, f(0xFFFF4D5E.toInt()))
        c.drawOval(m.taillight[0] - 7f, m.taillight[1] - 9f, m.taillight[0] + 7f, m.taillight[1] + 9f, l(d, 3f))
        if (m.type == CarType.SPORTS) {
            c.drawRect(22f, 88f, 64f, 96f, f(d)); c.drawLine(40f, 96f, 44f, 104f, l(d, 5f))
        }
    }

    fun wheels(c: Canvas, m: CarModel, rot: Float = 0f, tireShine: FloatArray? = null, dirt: Float = 0f, dirtColor: Int = 0xFF6B4220.toInt()) {
        val wy = m.wheelY(); val wr = m.wheelR
        for ((i, wx) in m.wheelX.withIndex()) {
            c.drawCircle(wx, wy, wr, f(0xFF23232E.toInt()))
            if (m.type == CarType.MONSTER) {
                c.save(); c.rotate(rot, wx, wy)
                for (k in 0 until 14) { c.save(); c.rotate(k * 360f / 14, wx, wy); c.drawRoundRect(wx - 6f, wy - wr - 4f, wx + 6f, wy - wr + 10f, 3f, 3f, f(0xFF23232E.toInt())); c.restore() }
                c.restore()
            }
            c.drawCircle(wx, wy, wr, l(0xFF0E0E16.toInt(), 4f))
            val rr = wr * 0.56f
            c.save(); c.translate(wx, wy); c.scale(rr, rr)
            fill.shader = rimShader; fill.color = Color.WHITE; c.drawCircle(0f, 0f, 1f, fill)
            c.restore()
            c.drawCircle(wx, wy, rr, l(0xFF7D8796.toInt(), 2f))
            c.save(); c.rotate(rot, wx, wy)
            for (k in 0 until 5) { c.save(); c.rotate(k * 72f, wx, wy); c.drawRoundRect(wx - 3f, wy - rr * 0.92f, wx + 3f, wy - rr * 0.22f, 3f, 3f, f(0xFF8A96A8.toInt())); c.restore() }
            c.restore()
            c.drawCircle(wx, wy, wr * 0.14f, f(m.color)); c.drawCircle(wx, wy, wr * 0.14f, l(Color.WHITE, 2f))
            val ts = tireShine?.getOrNull(i) ?: 0f
            if (ts > 0f) {
                val o = android.graphics.RectF(wx - wr * 0.82f, wy - wr * 0.82f, wx + wr * 0.82f, wy + wr * 0.82f)
                c.drawArc(o, 190f, 80f * ts + 10f, false, l(Col.alpha(Color.WHITE, 0.9f * ts), 5f))
                Gfx.sparkle(c, wx - wr * 0.5f, wy - wr * 0.7f, 10f * ts)
            }
            if (dirt > 0f) {
                c.drawCircle(wx - 6f, wy + 8f, wr * 0.55f, f(Col.alpha(dirtColor, 0.85f * dirt)))
                c.drawCircle(wx + 10f, wy - 6f, wr * 0.3f, f(Col.alpha(dirtColor, 0.7f * dirt)))
            }
        }
    }

    /** Complete car (shadow, body, wheels, sparkles) for menus and rewards. */
    fun draw(c: Canvas, m: CarModel, shine: Float = 0f, time: Float = 0f, rot: Float = 0f, tires: Float = 0f) {
        shadow(c, m)
        c.save(); c.translate(0f, -m.lift); body(c, m, shine, time); c.restore()
        wheels(c, m, rot, if (tires > 0f) floatArrayOf(tires, tires) else null)
        if (shine > 0f) {
            val tw = { o: Float -> 0.5f + 0.5f * sin(time * 4f + o) }
            Gfx.sparkle(c, 330f, 70f - m.lift, 18f * shine * tw(0f))
            Gfx.sparkle(c, 92f, 96f - m.lift, 13f * shine * tw(1.3f))
            Gfx.sparkle(c, 250f, 40f - m.lift, 11f * shine * tw(2.6f))
            Gfx.sparkle(c, 180f, 120f - m.lift, 9f * shine * tw(3.9f), 0xFFFFFBD0.toInt())
        }
    }

    /** Dark silhouette for not-yet-washed cars in the garage. */
    fun silhouette(c: Canvas, m: CarModel) {
        c.save(); c.translate(0f, -m.lift); c.drawPath(m.body, f(0xFF2A3263.toInt())); c.restore()
        for (wx in m.wheelX) c.drawCircle(wx, m.wheelY(), m.wheelR, f(0xFF1E2550.toInt()))
    }
}
