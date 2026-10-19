package com.devatjunaid.isthisai.ui.logo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devatjunaid.isthisai.ui.components.drawAperture
import com.devatjunaid.isthisai.ui.components.drawCornerBrackets
import com.devatjunaid.isthisai.ui.components.drawSpark
import com.devatjunaid.isthisai.ui.theme.AiOrange
import com.devatjunaid.isthisai.ui.theme.CameraBlue
import com.devatjunaid.isthisai.ui.theme.Display
import com.devatjunaid.isthisai.ui.theme.Ink
import com.devatjunaid.isthisai.ui.theme.Lime
import com.devatjunaid.isthisai.ui.theme.LineStrong
import com.devatjunaid.isthisai.ui.theme.Paper
import com.devatjunaid.isthisai.ui.theme.Surface
import kotlin.math.cos
import kotlin.math.sin

/**
 * The "Lens Mark": seven layered pieces that assemble as [assemble] goes 0→1 and then keep moving.
 *  1. scan brackets that snap in from outside
 *  2. a tick-marked lens barrel that draws itself around
 *  3. an orbiting lime satellite
 *  4. a three-colour verdict ring (AI orange, camera blue, scan lime)
 *  5. six aperture blades that spin open
 *  6. the AI spark in the iris, which pops in and pulses
 *  7. a scanline sweeping across the iris
 */
@Composable
fun LensMark(modifier: Modifier = Modifier, size: Dp = 120.dp, assemble: Float = 1f, close: Float = 0f) {
    val t = rememberInfiniteTransition(label = "lens")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "spin")
    val orbit by t.animateFloat(0f, 360f, infiniteRepeatable(tween(5_000, easing = LinearEasing)), label = "orbit")
    val breathe by t.animateFloat(0f, 10f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "breathe")
    val pulse by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "pulse")
    val sweep by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "sweep")

    Canvas(modifier.size(size).semantics { contentDescription = "Is This AI? logo" }) {
        drawLensMark(assemble.coerceIn(0f, 1f), close.coerceIn(0f, 1f), spin, orbit, breathe, pulse, sweep)
    }
}

private fun stage(p: Float, from: Float, to: Float) = ((p - from) / (to - from)).coerceIn(0f, 1f)

private fun easeOutBack(x: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val y = x - 1f
    return 1f + c3 * y * y * y + c1 * y * y
}

private fun DrawScope.drawLensMark(a: Float, close: Float, spin: Float, orbit: Float, breathe: Float, pulse: Float, sweep: Float) {
    val c = center
    val r = size.minDimension / 2f

    // 1. Brackets
    val br = stage(a, 0f, 0.35f)
    if (br > 0f) {
        drawCornerBrackets(Paper.copy(alpha = br), r * 0.04f, r * 0.22f, r * 0.045f, scale = 1.35f - 0.35f * br)
    }

    // 2. Tick barrel, drawn around progressively
    val ticks = stage(a, 0.1f, 0.6f)
    val count = (60 * ticks).toInt()
    rotate(spin * 0.25f, pivot = c) {
        for (i in 0 until count) {
            val ang = Math.toRadians(i * 6.0 - 90)
            val long = i % 5 == 0
            val r1 = r * 0.80f
            val r2 = r * if (long) 0.87f else 0.84f
            drawLine(
                LineStrong.copy(alpha = if (long) 1f else 0.75f),
                Offset(c.x + r1 * cos(ang).toFloat(), c.y + r1 * sin(ang).toFloat()),
                Offset(c.x + r2 * cos(ang).toFloat(), c.y + r2 * sin(ang).toFloat()),
                strokeWidth = r * if (long) 0.022f else 0.013f, cap = StrokeCap.Round,
            )
        }
    }

    // 3. Orbiting satellite
    if (ticks >= 1f) {
        val oa = Math.toRadians(orbit.toDouble() - 90)
        drawCircle(Lime, r * 0.035f, Offset(c.x + r * 0.835f * cos(oa).toFloat(), c.y + r * 0.835f * sin(oa).toFloat()))
    }

    // 4. Verdict ring: three arcs that grow in
    val ring = stage(a, 0.2f, 0.75f)
    if (ring > 0f) {
        val rr = r * 0.70f
        val rect = Rect(c, rr)
        val sw = 110f * ring
        rotate(-spin * 0.6f, pivot = c) {
            listOf(-85f to AiOrange, 35f to CameraBlue, 155f to Lime).forEach { (start, color) ->
                drawArc(color, start, sw, false, topLeft = rect.topLeft, size = Size(rect.width, rect.height), style = Stroke(r * 0.07f, cap = StrokeCap.Round))
            }
        }
    }

    // 5. Disc and blades spinning open
    val bl = stage(a, 0.3f, 0.9f)
    if (bl > 0f) {
        val discR = r * 0.58f * easeOutBack(bl).coerceAtMost(1.08f)
        drawCircle(Surface, discR, c)
        drawCircle(LineStrong, discR, c, style = Stroke(r * 0.012f))
        val bladeR = r * 0.56f * bl
        // Closing (exit) rotates blades inward and shrinks the iris.
        drawAperture(c, bladeR, 120f * (1f - bl) + breathe + 60f * close, light = Color(0xFF353A2E), dark = Color(0xFF2A2E24))
        val irisR = r * 0.16f * (1f - close)
        if (irisR > 0.5f) {
            drawCircle(Ink, irisR, c)
            // 7. Scanline inside the iris
            if (a >= 1f) {
                val ly = c.y - irisR + 2 * irisR * sweep
                clipPath(Path().apply { addOval(Rect(c, irisR)) }) {
                    drawLine(Lime.copy(alpha = 0.55f), Offset(c.x - irisR, ly), Offset(c.x + irisR, ly), r * 0.012f)
                }
            }
        }
    }

    // 6. Spark
    val sp = stage(a, 0.65f, 1f)
    if (sp > 0f && close < 1f) {
        val s = easeOutBack(sp) * (1f - close)
        drawCircle(Lime.copy(alpha = 0.16f * pulse * sp), r * 0.2f * s, c)
        drawSpark(c, r * (0.11f + 0.015f * pulse) * s, Lime)
    }
}

/**
 * "is this AI?" set in Bricolage Grotesque. [reveal] staggers the letters in from below.
 */
@Composable
fun Wordmark(modifier: Modifier = Modifier, fontSize: TextUnit = 22.sp, reveal: Float = 1f) {
    val parts = listOf("i", "s", " ", "t", "h", "i", "s", " ", "A", "I", "?")
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = "Is This AI?" }, verticalAlignment = Alignment.Bottom) {
        parts.forEachIndexed { i, ch ->
            val local = ((reveal * (parts.size + 4) - i) / 4f).coerceIn(0f, 1f)
            val isAi = i >= 8
            Text(
                ch,
                fontFamily = Display,
                fontWeight = if (isAi) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontSize = fontSize,
                color = if (i == 10) Lime else Paper,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.graphicsLayer {
                    alpha = local
                    translationY = (1f - local) * fontSize.value * 1.4f
                },
            )
        }
    }
}
