package com.sunarc.inverterlogger.ui.brand

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunarc.inverterlogger.ui.theme.Palette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Sunarc mark, drawn live from nine layered parts: halo, dashed orbit, sun trail, sun with
 * highlight, turning rays, a solar panel whose cells catch a moving glint, output bars and an
 * energy bolt.
 *
 * [intro] runs 0→1 once to assemble the mark (orbit draws, panel rises, sun climbs the arc, rays
 * fan out, bars grow, bolt pops). [sunset] runs 0→1 to send the sun down behind the panel; it is
 * used by the exit dialog. [live] keeps the rays turning and the glint moving afterwards.
 */
@Composable
fun SunarcLogo(
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    intro: Float = 1f,
    sunset: Float = 0f,
    live: Boolean = true
) {
    val loop = rememberInfiniteTransition(label = "logo")
    val rayTurn by loop.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "rays")
    val pulse by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "pulse")
    val glint by loop.animateFloat(-0.4f, 1.4f, infiniteRepeatable(tween(3200, delayMillis = 600, easing = LinearEasing)), label = "glint")

    Canvas(modifier.size(size).semantics { contentDescription = "Sunarc logo" }) {
        drawMark(
            intro = intro.coerceIn(0f, 1f),
            sunset = sunset.coerceIn(0f, 1f),
            rayTurn = if (live) rayTurn else 0f,
            pulse = if (live) pulse else 0.5f,
            glint = if (live) glint else -1f
        )
    }
}

private fun seg(p: Float, from: Float, to: Float) = ((p - from) / (to - from)).coerceIn(0f, 1f)
private fun easeOut(t: Float) = 1f - (1f - t) * (1f - t) * (1f - t)
private fun easeInOut(t: Float) = if (t < 0.5f) 4 * t * t * t else 1 - (-2 * t + 2).let { it * it * it } / 2
/** Overshoots a little before settling, for the bolt "pop". */
private fun backOut(t: Float): Float {
    val c1 = 1.9f
    val c3 = c1 + 1
    val x = t - 1
    return 1 + c3 * x * x * x + c1 * x * x
}

private fun DrawScope.drawMark(intro: Float, sunset: Float, rayTurn: Float, pulse: Float, glint: Float) {
    val u = size.minDimension / 100f
    fun o(x: Float, y: Float) = Offset(x * u, y * u)

    val orbitC = o(50f, 62f)
    val orbitR = 34f * u

    val pOrbit = easeOut(seg(intro, 0f, 0.35f))
    val pPanel = easeOut(seg(intro, 0.1f, 0.45f))
    val pSun = easeInOut(seg(intro, 0.2f, 0.7f))
    val pRays = easeOut(seg(intro, 0.55f, 0.85f))
    val pBars = seg(intro, 0.5f, 0.82f)
    val pBolt = backOut(seg(intro, 0.7f, 1f)).coerceAtLeast(0f)

    // Sun angle along the orbit (screen degrees: 180 = left horizon, 270 = top, 360 = right horizon).
    val climbAngle = 180f + 120f * pSun
    val angle = climbAngle + (380f - climbAngle) * easeInOut(sunset)
    val rad = angle * PI.toFloat() / 180f
    val sunPos = Offset(orbitC.x + cos(rad) * orbitR, orbitC.y + sin(rad) * orbitR)
    val dusk = sunset
    val sunColor = lerp(Palette.Sun, Color(0xFFFF7A3D), dusk)
    val sunAlpha = (1f - seg(sunset, 0.75f, 1f)) * seg(intro, 0.18f, 0.3f)

    // 1. Halo.
    if (sunAlpha > 0f) {
        val haloR = (26f + 4f * pulse) * u
        drawCircle(
            Brush.radialGradient(listOf(sunColor.copy(alpha = 0.30f * sunAlpha), Color.Transparent), center = sunPos, radius = haloR),
            radius = haloR, center = sunPos
        )
    }

    // 2. Orbit, dashed, drawn from the left horizon.
    drawArc(
        color = Palette.Line,
        startAngle = 180f, sweepAngle = 180f * pOrbit, useCenter = false,
        topLeft = Offset(orbitC.x - orbitR, orbitC.y - orbitR), size = Size(orbitR * 2, orbitR * 2),
        style = Stroke(width = 1.6f * u, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.5f * u, 4f * u)))
    )

    // 3. Trail behind the sun.
    if (sunAlpha > 0f) {
        drawArc(
            Brush.sweepGradient(listOf(sunColor.copy(alpha = 0f), sunColor, sunColor), center = orbitC),
            startAngle = 180f, sweepAngle = (angle - 180f - 9f).coerceIn(0f, 180f), useCenter = false,
            topLeft = Offset(orbitC.x - orbitR, orbitC.y - orbitR), size = Size(orbitR * 2, orbitR * 2),
            style = Stroke(width = 2.6f * u, cap = StrokeCap.Round), alpha = sunAlpha
        )
    }

    // 4. Rays.
    if (pRays > 0f && sunAlpha > 0f) {
        rotate(rayTurn, pivot = sunPos) {
            for (i in 0 until 12) {
                val a = (i * 30f) * PI.toFloat() / 180f
                val long = i % 2 == 0
                val inner = 11.5f * u
                val outer = inner + (if (long) 5f + 1.5f * pulse else 3f) * u * pRays
                drawLine(
                    color = (if (long) Palette.SunSoft else Palette.Sun).copy(alpha = 0.9f * sunAlpha * pRays),
                    start = sunPos + Offset(cos(a) * inner, sin(a) * inner),
                    end = sunPos + Offset(cos(a) * outer, sin(a) * outer),
                    strokeWidth = 2f * u, cap = StrokeCap.Round
                )
            }
        }
    }

    // 5. Sun disc with a soft highlight.
    if (sunAlpha > 0f) {
        val r = 8.5f * u * (0.6f + 0.4f * seg(intro, 0.18f, 0.4f))
        drawCircle(Brush.radialGradient(listOf(Palette.SunPale, sunColor), center = sunPos + Offset(-2.5f * u, -2.5f * u), radius = r * 1.4f), r, sunPos, alpha = sunAlpha)
    }

    // 6. Output bars rising under the arc, just above the panel.
    val barBase = 62f
    listOf(6f, 10f, 15f).forEachIndexed { i, h ->
        val t = easeOut(seg(pBars, i * 0.2f, 0.6f + i * 0.2f))
        if (t > 0f) {
            val x = 23f + i * 5f
            drawRoundRect(
                color = Palette.Sky.copy(alpha = 0.55f + 0.15f * i),
                topLeft = o(x, barBase - h * t), size = Size(3.4f * u, h * t * u),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.2f * u)
            )
        }
    }

    // 7. Solar panel, rising into place, in front of the orbit's base.
    if (pPanel > 0f) {
        translate(top = (1f - pPanel) * 14f * u) {
            val tl = o(26f, 66f); val tr = o(74f, 66f); val br = o(84f, 86f); val bl = o(16f, 86f)
            val panel = Path().apply { moveTo(tl.x, tl.y); lineTo(tr.x, tr.y); lineTo(br.x, br.y); lineTo(bl.x, bl.y); close() }
            drawPath(panel, Palette.PanelBlue, alpha = pPanel)
            clipPath(panel) {
                // Glint: a bright diagonal band sweeping across the cells.
                if (glint > -0.5f) {
                    val gx = (16f + glint * 68f) * u
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Palette.Sky.copy(alpha = 0.55f), Palette.SunPale.copy(alpha = 0.35f), Color.Transparent),
                            start = Offset(gx - 12f * u, 66f * u), end = Offset(gx + 4f * u, 86f * u)
                        ),
                        topLeft = o(0f, 60f), size = Size(size.width, 30f * u), alpha = pPanel
                    )
                }
                // Sunset glow on the panel as the sun goes down behind it.
                if (dusk > 0f) drawRect(Color(0xFFFF7A3D).copy(alpha = 0.25f * dusk), topLeft = o(0f, 60f), size = Size(size.width, 30f * u))
            }
            val grid = Palette.Sky.copy(alpha = 0.8f * pPanel)
            for (k in 1..3) {
                val t = k / 4f
                drawLine(grid, lerp(tl, tr, t), lerp(bl, br, t), strokeWidth = 1.1f * u)
            }
            drawLine(grid, lerp(tl, bl, 0.5f), lerp(tr, br, 0.5f), strokeWidth = 1.1f * u)
            drawPath(panel, Palette.Sky.copy(alpha = pPanel), style = Stroke(width = 1.6f * u, join = StrokeJoin.Round))
            // Stand.
            drawLine(Palette.Line.copy(alpha = pPanel), o(50f, 86f), o(50f, 92f), strokeWidth = 1.8f * u, cap = StrokeCap.Round)
            drawLine(Palette.Line.copy(alpha = pPanel), o(41f, 92f), o(59f, 92f), strokeWidth = 1.8f * u, cap = StrokeCap.Round)
        }
    }

    // 8. Energy bolt popping out of the panel.
    if (pBolt > 0f) {
        val c = o(50f, 76f)
        val s = pBolt * u
        val bolt = Path().apply {
            moveTo(c.x + 1.5f * s, c.y - 10f * s)
            lineTo(c.x - 6f * s, c.y + 1.5f * s)
            lineTo(c.x - 0.5f * s, c.y + 1.5f * s)
            lineTo(c.x - 3f * s, c.y + 10f * s)
            lineTo(c.x + 6.5f * s, c.y - 2.5f * s)
            lineTo(c.x + 0.8f * s, c.y - 2.5f * s)
            lineTo(c.x + 4f * s, c.y - 10f * s)
            close()
        }
        drawPath(bolt, lerp(Palette.Sun, Color(0xFFFF7A3D), dusk))
        drawPath(bolt, Palette.Night, style = Stroke(width = 1.2f * u, join = StrokeJoin.Round))
    }
}

private fun lerp(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

private fun lerp(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t
)
