package com.devatjunaid.dripdropdraw.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import com.devatjunaid.dripdropdraw.engine.Capsule
import com.devatjunaid.dripdropdraw.engine.GameSession
import com.devatjunaid.dripdropdraw.engine.LevelDef
import com.devatjunaid.dripdropdraw.engine.Pt
import com.devatjunaid.dripdropdraw.engine.WallStyle
import com.devatjunaid.dripdropdraw.engine.World
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Maps the fixed-size world onto the screen, centred and as large as possible. */
data class ViewFit(val scale: Float, val ox: Float, val oy: Float) {
    fun toWorldX(x: Float) = (x - ox) / scale
    fun toWorldY(y: Float) = (y - oy) / scale
    fun toScreen(x: Float, y: Float) = Offset(ox + x * scale, oy + y * scale)

    companion object {
        fun of(width: Float, height: Float): ViewFit {
            val s = min(width / World.W, height / World.H)
            return ViewFit(s, (width - World.W * s) / 2f, (height - World.H * s) / 2f)
        }
    }
}

/** Draws a [GameSession]. All art is made from shapes, nothing is loaded from files. */
class GameRenderer(private val textMeasurer: TextMeasurer) {

    private val focus = FloatArray(2)
    private val lookX = FloatArray(4)
    private val lookY = FloatArray(4)
    private val dash = PathEffect.dashPathEffect(floatArrayOf(4f, 22f))
    private val targetDash = PathEffect.dashPathEffect(floatArrayOf(10f, 9f))

    fun draw(scope: DrawScope, s: GameSession, time: Float, showHint: Boolean, theme: WorldTheme) = with(scope) {
        val fit = ViewFit.of(size.width, size.height)
        drawBackground(fit, theme, time)
        val lineColor = if (theme.dark) Color(0xFFF4F1FF) else Palette.Ink
        withTransform({
            translate(fit.ox, fit.oy)
            scale(fit.scale, fit.scale, Offset.Zero)
        }) {
            val w = s.world
            val level = s.level
            for (f in level.fans) drawFanWind(f.x, f.y, f.w, f.h, f.fx, time)
            for (f in level.fires) drawFire(f.x, f.y, f.r, time)
            for (i in w.walls.indices) drawWall(w.walls[i], level.walls[i].style, theme)
            for (st in w.seesaws) {
                val d = st.def
                val tri = Path().apply {
                    moveTo(d.x, d.y + 6f); lineTo(d.x - 34f, d.y + 76f); lineTo(d.x + 34f, d.y + 76f); close()
                }
                drawPath(tri, theme.stone)
                drawPath(tri, Palette.Ink, style = Stroke(5f, join = StrokeJoin.Round))
                drawCapsule(st.plank, theme.wood)
                drawCircle(Palette.Ink, 7f, Offset(d.x, d.y))
            }
            for (m in w.movers) {
                drawCapsule(m.body, Palette.Orange)
                val cx = (m.body.ax + m.body.bx) / 2f
                val cy = (m.body.ay + m.body.by) / 2f
                drawArrowHint(cx, cy, m.def.dx, m.def.dy)
            }
            for (sp in w.spinners) {
                drawCapsule(sp.bar, Palette.Pink)
                drawCircle(Palette.Ink, 13f, Offset(sp.def.x, sp.def.y))
                drawCircle(Palette.Star, 7f, Offset(sp.def.x, sp.def.y))
            }
            for (f in level.fans) drawFanBody(f.x, f.y, f.w, f.h, f.fx, time)
            for (f in level.faucets) drawFaucet(f.x, f.y, theme)

            if (showHint) {
                for (stroke in level.hint) drawStroke(stroke, Palette.Orange.copy(alpha = 0.85f), 12f, dash)
            }

            // Glass behind the water.
            for (c in w.cups) drawCupBack(c.x, c.def.bottom, c.def.topHalf, c.def.bottomHalf, c.def.height)

            drawWater(w)

            for ((k, c) in w.cups.withIndex()) {
                drawCupFront(c.x, c.def.bottom, c.def.topHalf, c.def.bottomHalf, c.def.height, c.def.targetLineHeight(), c.fill >= c.def.target)
                drawCupFace(s, k, c.x, c.def.bottom, c.def.height, time)
            }

            for (b in w.balls) {
                drawCircle(Palette.Ink, b.r + 3f, Offset(b.x, b.y))
                drawCircle(Palette.Ball, b.r, Offset(b.x, b.y))
                rotate(Math.toDegrees(b.rot.toDouble()).toFloat(), Offset(b.x, b.y)) {
                    drawLine(Color.White.copy(alpha = 0.8f), Offset(b.x - b.r * 0.7f, b.y), Offset(b.x + b.r * 0.7f, b.y), 5f, StrokeCap.Round)
                }
                drawCircle(Color.White.copy(alpha = 0.45f), b.r * 0.28f, Offset(b.x - b.r * 0.38f, b.y - b.r * 0.38f))
            }

            for (stroke in s.strokes) drawStroke(stroke, lineColor, World.LINE_R * 2f, null)

            for (e in w.effects) {
                val a = (1f - e.age / 1.2f).coerceIn(0f, 1f)
                drawCircle(Color(0xFFDDE2EA).copy(alpha = 0.8f * a), 10f + e.age * 26f, Offset(e.x, e.y))
            }

            if (s.isTouching && s.canDraw) drawPencil(s.touchX, s.touchY)
        }
        drawBubbles(s, fit)
    }

    // ---- background ---------------------------------------------------------------------

    private fun DrawScope.drawBackground(fit: ViewFit, theme: WorldTheme, time: Float) {
        drawRect(theme.paper)
        val step = 40f * fit.scale
        var x = fit.ox % step
        while (x < size.width) {
            drawLine(theme.grid, Offset(x, 0f), Offset(x, size.height), 1.5f)
            x += step
        }
        var y = fit.oy % step
        while (y < size.height) {
            drawLine(theme.grid, Offset(0f, y), Offset(size.width, y), 1.5f)
            y += step
        }
        if (theme.dark) {
            // Twinkling stars for the space world.
            for (i in 0 until 40) {
                val sx = ((i * 7919) % 1000) / 1000f * size.width
                val sy = ((i * 104729) % 1000) / 1000f * size.height
                val a = 0.35f + 0.35f * sin(time * 2f + i)
                drawCircle(Color.White.copy(alpha = a), 2.2f, Offset(sx, sy))
            }
        }
    }

    // ---- solids -------------------------------------------------------------------------

    private fun DrawScope.drawCapsule(c: Capsule, color: Color) {
        drawLine(Palette.Ink, Offset(c.ax, c.ay), Offset(c.bx, c.by), c.r * 2f + 6f, StrokeCap.Round)
        drawLine(color, Offset(c.ax, c.ay), Offset(c.bx, c.by), c.r * 2f, StrokeCap.Round)
    }

    private fun DrawScope.drawWall(c: Capsule, style: WallStyle, theme: WorldTheme) {
        val color = if (style == WallStyle.WOOD) theme.wood else theme.stone
        drawCapsule(c, color)
        // A light streak for a little depth.
        val dx = c.bx - c.ax
        val dy = c.by - c.ay
        val len = sqrt(dx * dx + dy * dy)
        if (len > 30f) {
            val nx = -dy / len * c.r * 0.4f
            val ny = dx / len * c.r * 0.4f
            drawLine(
                Color.White.copy(alpha = 0.35f),
                Offset(c.ax + dx * 0.08f - nx, c.ay + dy * 0.08f - ny),
                Offset(c.ax + dx * 0.6f - nx, c.ay + dy * 0.6f - ny),
                c.r * 0.35f,
                StrokeCap.Round,
            )
        }
    }

    private fun DrawScope.drawArrowHint(cx: Float, cy: Float, dx: Float, dy: Float) {
        val len = sqrt(dx * dx + dy * dy)
        if (len < 1f) return
        val ux = dx / len
        val uy = dy / len
        for (sgn in listOf(-1f, 1f)) {
            val tipX = cx + ux * 40f * sgn
            val tipY = cy + uy * 40f * sgn
            val p = Path().apply {
                moveTo(tipX + ux * 10f * sgn, tipY + uy * 10f * sgn)
                lineTo(tipX - uy * 8f, tipY + ux * 8f)
                lineTo(tipX + uy * 8f, tipY - ux * 8f)
                close()
            }
            drawPath(p, Palette.Ink)
        }
    }

    private fun DrawScope.drawFaucet(x: Float, y: Float, theme: WorldTheme) {
        val outline = Stroke(5f, join = StrokeJoin.Round)
        // Pipe from the top of the screen.
        drawRect(theme.stone, Offset(x - 13f, -20f), Size(26f, y - 50f))
        drawRect(Palette.Ink, Offset(x - 13f, -20f), Size(26f, y - 50f), style = outline)
        drawRoundRect(theme.stone, Offset(x - 31f, y - 76f), Size(62f, 48f), CornerRadius(14f))
        drawRoundRect(Palette.Ink, Offset(x - 31f, y - 76f), Size(62f, 48f), CornerRadius(14f), style = outline)
        drawRoundRect(theme.stone, Offset(x - 14f, y - 34f), Size(28f, 34f), CornerRadius(7f))
        drawRoundRect(Palette.Ink, Offset(x - 14f, y - 34f), Size(28f, 34f), CornerRadius(7f), style = outline)
        // Red handle.
        drawRoundRect(Palette.Orange, Offset(x + 22f, y - 70f), Size(26f, 18f), CornerRadius(6f))
        drawRoundRect(Palette.Ink, Offset(x + 22f, y - 70f), Size(26f, 18f), CornerRadius(6f), style = Stroke(4f))
        drawLine(Color.White.copy(alpha = 0.5f), Offset(x - 20f, y - 66f), Offset(x - 20f, y - 40f), 5f, StrokeCap.Round)
    }

    private fun DrawScope.drawFire(x: Float, y: Float, r: Float, t: Float) {
        // Logs.
        drawLine(Palette.Ink, Offset(x - r, y + r * 0.75f), Offset(x + r, y + r * 0.55f), 22f, StrokeCap.Round)
        drawLine(Color(0xFFA0673A), Offset(x - r, y + r * 0.75f), Offset(x + r, y + r * 0.55f), 16f, StrokeCap.Round)
        drawLine(Palette.Ink, Offset(x - r, y + r * 0.55f), Offset(x + r, y + r * 0.75f), 22f, StrokeCap.Round)
        drawLine(Color(0xFFB87A48), Offset(x - r, y + r * 0.55f), Offset(x + r, y + r * 0.75f), 16f, StrokeCap.Round)
        // Three flickering flames.
        for (i in -1..1) {
            val fx = x + i * r * 0.45f
            val hgt = r * (1.3f - abs(i) * 0.35f) * (1f + 0.12f * sin(t * 13f + i * 2f))
            val base = y + r * 0.45f
            val wid = r * 0.42f
            val outer = flame(fx, base, wid, hgt, sin(t * 7f + i) * 6f)
            drawPath(outer, Palette.Fire)
            drawPath(outer, Palette.Ink, style = Stroke(4f, join = StrokeJoin.Round))
            drawPath(flame(fx, base, wid * 0.5f, hgt * 0.55f, 0f), Palette.FireCore)
        }
    }

    private fun flame(x: Float, base: Float, w: Float, h: Float, sway: Float) = Path().apply {
        moveTo(x + sway, base - h)
        cubicTo(x - w * 0.4f + sway, base - h * 0.6f, x - w * 1.2f, base - h * 0.3f, x - w * 0.6f, base)
        lineTo(x + w * 0.6f, base)
        cubicTo(x + w * 1.2f, base - h * 0.3f, x + w * 0.4f + sway, base - h * 0.6f, x + sway, base - h)
        close()
    }

    private fun DrawScope.drawFanWind(x: Float, y: Float, w: Float, h: Float, fx: Float, t: Float) {
        val dir = if (fx < 0f) -1f else 1f
        val streak = Color(0xFF7FB8E6).copy(alpha = 0.55f)
        val rows = (h / 70f).toInt().coerceAtLeast(2)
        for (r in 0 until rows) {
            val yy = y + (r + 0.5f) * h / rows
            val phase = ((t * 0.9f + r * 0.37f) % 1f)
            val start = if (dir < 0f) x + w - phase * w else x + phase * w
            val end = start + dir * 70f
            if (end > x - 10f && end < x + w + 10f && start > x - 10f && start < x + w + 10f) {
                drawLine(streak, Offset(start, yy), Offset(end, yy + 6f * sin(t * 6f + r)), 6f, StrokeCap.Round)
            }
        }
    }

    private fun DrawScope.drawFanBody(x: Float, y: Float, w: Float, h: Float, fx: Float, t: Float) {
        // The fan sits on the side the wind comes from.
        val bx = if (fx < 0f) x + w + 24f else x - 24f
        val by = y + h / 2f
        drawRoundRect(Palette.Stone, Offset(bx - 22f, by - 50f), Size(44f, 100f), CornerRadius(12f))
        drawRoundRect(Palette.Ink, Offset(bx - 22f, by - 50f), Size(44f, 100f), CornerRadius(12f), style = Stroke(5f))
        drawCircle(Color.White, 36f, Offset(bx, by))
        drawCircle(Palette.Ink, 36f, Offset(bx, by), style = Stroke(5f))
        rotate(t * 720f, Offset(bx, by)) {
            for (k in 0 until 3) {
                rotate(k * 120f, Offset(bx, by)) {
                    drawOval(Palette.Orange, Offset(bx - 8f, by - 32f), Size(16f, 30f))
                    drawOval(Palette.Ink, Offset(bx - 8f, by - 32f), Size(16f, 30f), style = Stroke(3f))
                }
            }
        }
        drawCircle(Palette.Ink, 6f, Offset(bx, by))
    }

    // ---- water --------------------------------------------------------------------------

    private fun DrawScope.drawWater(w: World) {
        val n = w.count
        val px = w.px
        val py = w.py
        val r = World.PR
        for (i in 0 until n) drawCircle(Palette.WaterDark, r * 1.55f, Offset(px[i], py[i]))
        for (i in 0 until n) drawCircle(Palette.Water, r * 1.3f, Offset(px[i], py[i]))
        for (i in 0 until n step 3) drawCircle(Palette.WaterLight, r * 0.35f, Offset(px[i] - 3f, py[i] - 4f))
    }

    // ---- cups ---------------------------------------------------------------------------

    private fun cupPath(x: Float, bottom: Float, top: Float, bot: Float, h: Float) = Path().apply {
        moveTo(x - top, bottom - h)
        lineTo(x - bot, bottom)
        lineTo(x + bot, bottom)
        lineTo(x + top, bottom - h)
    }

    private fun DrawScope.drawCupBack(x: Float, bottom: Float, top: Float, bot: Float, h: Float) {
        val p = cupPath(x, bottom, top, bot, h).apply { close() }
        drawPath(p, Color.White.copy(alpha = 0.55f))
    }

    private fun DrawScope.drawCupFront(x: Float, bottom: Float, top: Float, bot: Float, h: Float, lineH: Float, full: Boolean) {
        val p = cupPath(x, bottom, top, bot, h)
        drawPath(p, Palette.Ink, style = Stroke(World.CUP_WALL_R * 2f + 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(p, Palette.Glass, style = Stroke(World.CUP_WALL_R * 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(
            Color.White.copy(alpha = 0.6f),
            Offset(x - top + 6f, bottom - h + 14f),
            Offset(x - top + 6f + (top - bot) * 0.5f, bottom - h * 0.5f),
            4f,
            StrokeCap.Round,
        )
        // The "fill to here" line.
        val ly = bottom - lineH
        val t = lineH / h
        val half = bot + (top - bot) * t - World.CUP_WALL_R - 2f
        drawLine(
            if (full) Palette.Green else Palette.Ink,
            Offset(x - half, ly),
            Offset(x + half, ly),
            4f,
            pathEffect = targetDash,
        )
    }

    private fun DrawScope.drawCupFace(s: GameSession, k: Int, x: Float, bottom: Float, h: Float, time: Float) {
        val w = s.world
        val cup = w.cups[k]
        val won = s.state == GameSession.State.WON
        val sad = s.state == GameSession.State.FAILED && cup.fill < cup.def.target
        val bounce = if (won) abs(sin(time * 9f)) * -8f else 0f
        val ey = bottom - h * 0.55f + bounce
        val ex = 24f
        val my = ey + 34f

        // Where the eyes look: the faucet before the water starts, then the water.
        val target = if (s.state == GameSession.State.READY || w.count == 0) {
            val f = s.level.faucets.first()
            f.x to f.y
        } else if (w.waterFocus(focus)) {
            focus[0] to focus[1]
        } else {
            x to ey - 100f
        }
        val dx = target.first - x
        val dy = target.second - ey
        val d = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val tx = dx / d * 4.5f
        val ty = dy / d * 4.5f
        lookX[k % 4] += (tx - lookX[k % 4]) * 0.2f
        lookY[k % 4] += (ty - lookY[k % 4]) * 0.2f

        val blink = ((time + k * 1.3f) % 3.7f) < 0.12f
        val faceInk = Palette.Ink
        drawOval(Palette.Pink.copy(alpha = 0.8f), Offset(x - ex - 26f, my - 14f), Size(20f, 11f))
        drawOval(Palette.Pink.copy(alpha = 0.8f), Offset(x + ex + 6f, my - 14f), Size(20f, 11f))

        when {
            won -> {
                for (sx in listOf(-ex, ex)) {
                    drawArc(faceInk, 200f, 140f, false, Offset(x + sx - 11f, ey - 8f), Size(22f, 20f), style = Stroke(5f, cap = StrokeCap.Round))
                }
                val mouth = Path().apply {
                    moveTo(x - 20f, my - 4f)
                    quadraticTo(x, my + 40f, x + 20f, my - 4f)
                    close()
                }
                drawPath(mouth, faceInk)
                drawOval(Palette.Pink, Offset(x - 9f, my + 9f), Size(18f, 10f))
            }
            sad -> {
                for (sx in listOf(-ex, ex)) {
                    drawCircle(Color.White, 10f, Offset(x + sx, ey))
                    drawCircle(faceInk, 10f, Offset(x + sx, ey), style = Stroke(3.5f))
                    drawCircle(faceInk, 4.5f, Offset(x + sx, ey + 3f))
                    drawLine(faceInk, Offset(x + sx - 12f, ey - 14f + if (sx < 0) 5f else 0f), Offset(x + sx + 12f, ey - 14f + if (sx < 0) 0f else 5f), 4f, StrokeCap.Round)
                }
                drawArc(faceInk, 200f, 140f, false, Offset(x - 14f, my), Size(28f, 20f), style = Stroke(4.5f, cap = StrokeCap.Round))
                val tearY = ey + 16f + (time * 30f) % 26f
                drawCircle(Palette.WaterLight, 5f, Offset(x + ex + 6f, tearY))
                drawCircle(Palette.WaterDark, 5f, Offset(x + ex + 6f, tearY), style = Stroke(2f))
            }
            else -> {
                val excited = s.state == GameSession.State.RUNNING && s.lastSplashAge[k] < 0.35f
                val r = if (excited) 12f else 10f
                for (sx in listOf(-ex, ex)) {
                    if (blink) {
                        drawLine(faceInk, Offset(x + sx - 9f, ey), Offset(x + sx + 9f, ey), 4f, StrokeCap.Round)
                    } else {
                        drawCircle(Color.White, r, Offset(x + sx, ey))
                        drawCircle(faceInk, r, Offset(x + sx, ey), style = Stroke(3.5f))
                        drawCircle(faceInk, r * 0.48f, Offset(x + sx + lookX[k % 4], ey + lookY[k % 4]))
                    }
                }
                when {
                    excited -> drawOval(faceInk, Offset(x - 9f, my - 8f), Size(18f, 22f))
                    cup.fill >= cup.def.target -> {
                        val mouth = Path().apply {
                            moveTo(x - 16f, my - 4f)
                            quadraticTo(x, my + 26f, x + 16f, my - 4f)
                            close()
                        }
                        drawPath(mouth, faceInk)
                    }
                    s.state == GameSession.State.READY -> {
                        // Thirsty: little open mouth with the tongue out.
                        drawOval(faceInk, Offset(x - 8f, my - 6f), Size(16f, 16f))
                        drawOval(Palette.Pink, Offset(x - 5f, my + 3f), Size(10f, 9f))
                    }
                    else -> drawArc(faceInk, 20f, 140f, false, Offset(x - 13f, my - 12f), Size(26f, 18f), style = Stroke(4.5f, cap = StrokeCap.Round))
                }
            }
        }
    }

    // ---- lines --------------------------------------------------------------------------

    private fun DrawScope.drawStroke(points: List<Pt>, color: Color, width: Float, effect: PathEffect?) {
        if (points.size < 2) return
        val p = Path()
        p.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) p.lineTo(points[i].x, points[i].y)
        drawPath(p, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = effect))
    }

    private fun DrawScope.drawPencil(x: Float, y: Float) {
        withTransform({
            translate(x, y)
            rotate(-50f, Offset.Zero)
        }) {
            val outline = Stroke(4f, join = StrokeJoin.Round)
            val tip = Path().apply { moveTo(0f, 0f); lineTo(26f, -11f); lineTo(26f, 11f); close() }
            drawPath(tip, Color(0xFFF6D7B0))
            drawPath(tip, Palette.Ink, style = outline)
            val lead = Path().apply { moveTo(0f, 0f); lineTo(9f, -4f); lineTo(9f, 4f); close() }
            drawPath(lead, Palette.Ink)
            drawRect(Color(0xFFFFB627), Offset(26f, -11f), Size(96f, 22f))
            drawRect(Palette.Ink, Offset(26f, -11f), Size(96f, 22f), style = outline)
            drawRect(Color(0xFFC9CED6), Offset(122f, -11f), Size(12f, 22f))
            drawRect(Palette.Ink, Offset(122f, -11f), Size(12f, 22f), style = outline)
            drawRoundRect(Palette.Pink, Offset(134f, -11f), Size(22f, 22f), CornerRadius(6f))
            drawRoundRect(Palette.Ink, Offset(134f, -11f), Size(22f, 22f), CornerRadius(6f), style = outline)
        }
    }

    // ---- speech bubbles -----------------------------------------------------------------

    private val bubbleStyle = TextStyle(fontFamily = HandFont, fontSize = 22.sp, color = Palette.Ink)

    private fun DrawScope.drawBubbles(s: GameSession, fit: ViewFit) {
        for (b in s.bubbles) {
            val pop = min(1f, b.age / 0.15f)
            val fade = (1f - max(0f, b.age - 1.3f) / 0.3f).coerceIn(0f, 1f)
            val layout = textMeasurer.measure(b.text, bubbleStyle)
            val center = fit.toScreen(b.x, b.y - b.age * 12f)
            val w = layout.size.width + 28f
            val h = layout.size.height + 14f
            val left = (center.x - w / 2f).coerceIn(4f, max(4f, size.width - w - 4f))
            val top = (center.y - h / 2f).coerceAtLeast(4f)
            val rect = Rect(left, top, left + w, top + h)
            withTransform({ scale(pop, pop, rect.center) }) {
                drawRoundRect(Color.White.copy(alpha = fade), rect.topLeft, rect.size, CornerRadius(h / 2f))
                drawRoundRect(Palette.Ink.copy(alpha = fade), rect.topLeft, rect.size, CornerRadius(h / 2f), style = Stroke(5f))
                drawText(layout, topLeft = Offset(rect.left + 14f, rect.top + 7f), alpha = fade)
            }
        }
    }

    companion object {
        /** Ink colour for drawn lines in a level (light on the dark space paper). */
        fun worldTheme(level: LevelDef) = Worlds[level.world.coerceIn(0, Worlds.size - 1)]
    }
}
