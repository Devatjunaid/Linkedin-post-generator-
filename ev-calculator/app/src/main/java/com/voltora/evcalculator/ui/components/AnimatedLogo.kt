package com.voltora.evcalculator.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voltora.evcalculator.ui.theme.Ev
import kotlin.math.cos
import kotlin.math.sin

/**
 * The EV Calculator mark: a 300° charge gauge (gap at the bottom, where two plug prongs sit),
 * a lightning bolt, and electrons orbiting the gauge.
 *
 * With [intro] = true the mark builds itself: ticks sweep in, the gauge fills, the bolt is
 * traced then filled, prongs drop in and a charge pulse rings out. Afterwards (and always
 * when [intro] = false) it idles: electrons orbit and the bolt glows softly.
 */
@Composable
fun AnimatedLogo(
    size: Dp,
    modifier: Modifier = Modifier,
    intro: Boolean = false,
    accent: Color = Ev.Volt,
) {
    val ring = remember { Animatable(if (intro) 0f else 1f) }
    val bolt = remember { Animatable(if (intro) 0f else 1f) }
    val fill = remember { Animatable(if (intro) 0f else 1f) }
    val pulse = remember { Animatable(if (intro) 0f else 1f) }

    LaunchedEffect(intro) {
        if (!intro) return@LaunchedEffect
        ring.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        bolt.animateTo(1f, tween(450, easing = LinearEasing))
        fill.animateTo(1f, tween(250))
        pulse.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }

    val idle = rememberInfiniteTransition(label = "logoIdle")
    val orbit by idle.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "orbit",
    )
    val glow by idle.animateFloat(
        0.25f, 0.6f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )

    val boltPath = remember { Path() }
    val boltSegment = remember { Path() }
    val measure = remember { PathMeasure() }

    Canvas(
        modifier
            .size(size)
            .semantics { contentDescription = "EV Calculator logo" }
    ) {
        val w = this.size.minDimension
        val c = Offset(this.size.width / 2, this.size.height / 2)
        val r = w * 0.36f
        val stroke = w * 0.055f
        val ringP = ring.value

        // Tick marks around the inside of the gauge
        val ticks = 30
        for (i in 0..ticks) {
            val t = i / ticks.toFloat()
            if (t > ringP) break
            val a = Math.toRadians((120 + 300 * t).toDouble())
            val inner = r - stroke * 1.6f
            val outer = r - stroke * 1.0f
            drawLine(
                color = accent.copy(alpha = 0.35f),
                start = Offset(c.x + inner * cos(a).toFloat(), c.y + inner * sin(a).toFloat()),
                end = Offset(c.x + outer * cos(a).toFloat(), c.y + outer * sin(a).toFloat()),
                strokeWidth = w * 0.012f,
                cap = StrokeCap.Round,
            )
        }

        // Gauge ring
        drawArc(
            color = accent.copy(alpha = 0.12f),
            startAngle = 120f, sweepAngle = 300f, useCenter = false,
            topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        drawArc(
            brush = Brush.sweepGradient(
                0f to accent.copy(alpha = 0.55f), 0.5f to accent, 1f to accent.copy(alpha = 0.55f),
                center = c,
            ),
            startAngle = 120f, sweepAngle = 300f * ringP, useCenter = false,
            topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )

        // Plug prongs in the gap
        if (fill.value > 0f) {
            val pw = w * 0.035f
            val ph = w * 0.075f * fill.value
            val top = c.y + r * 0.92f
            for (dx in listOf(-w * 0.045f, w * 0.045f)) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(c.x + dx - pw / 2, top),
                    size = Size(pw, ph),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(pw / 3),
                )
            }
        }

        // Bolt: traced, then filled, with a soft glow while idle
        buildBolt(boltPath, c, w)
        if (fill.value >= 1f) {
            drawPath(boltPath, accent.copy(alpha = glow * 0.35f), style = Stroke(w * 0.06f, join = StrokeJoin.Round))
        }
        if (bolt.value > 0f) {
            measure.setPath(boltPath, false)
            boltSegment.reset()
            measure.getSegment(0f, measure.length * bolt.value, boltSegment, true)
            drawPath(boltSegment, accent, style = Stroke(w * 0.02f, join = StrokeJoin.Round, cap = StrokeCap.Round))
        }
        if (fill.value > 0f) drawPath(boltPath, accent.copy(alpha = fill.value))

        // Charge pulse
        val p = pulse.value
        if (p in 0.01f..0.99f) {
            drawCircle(
                color = accent.copy(alpha = (1f - p) * 0.6f),
                radius = r * (1f + p * 0.45f),
                center = c,
                style = Stroke(w * 0.01f + w * 0.02f * (1f - p)),
            )
        }

        // Orbiting electrons (only once the gauge is drawn)
        if (ringP >= 1f) drawElectrons(c, r, w, orbit, accent)
    }
}

private fun buildBolt(path: Path, c: Offset, w: Float) {
    // Bolt in a unit box centred on the gauge
    val pts = listOf(
        0.58f to 0.05f, 0.24f to 0.58f, 0.47f to 0.58f, 0.40f to 0.95f, 0.78f to 0.40f, 0.54f to 0.40f,
    )
    val bw = w * 0.36f
    val ox = c.x - bw * 0.51f
    val oy = c.y - bw * 0.52f
    path.reset()
    pts.forEachIndexed { i, (x, y) ->
        val px = ox + x * bw
        val py = oy + y * bw
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
}

private fun DrawScope.drawElectrons(c: Offset, r: Float, w: Float, orbit: Float, accent: Color) {
    rotate(degrees = orbit * 360f, pivot = c) {
        for (i in 0 until 3) {
            val a = Math.toRadians((i * 120.0) - 90.0)
            val pos = Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
            drawCircle(Color.White, radius = w * 0.022f, center = pos)
            drawCircle(accent.copy(alpha = 0.25f), radius = w * 0.045f, center = pos)
        }
    }
}
