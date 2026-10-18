package app.layerline.toolkit3d.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.layerline.toolkit3d.ui.theme.LL
import kotlin.math.sin

private fun seg(t: Float, from: Float, to: Float) = ((t - from) / (to - from)).coerceIn(0f, 1f)
private fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f
private fun ease(f: Float) = 1f - (1f - f) * (1f - f) * (1f - f)

/**
 * The Layerline mark, drawn in the same 108-unit grid as the launcher icon:
 * a spool ring, the filament strand feeding a hotend, and an "L" built from printed layers.
 *
 * [progress] runs the build animation: 0 = empty bed, 1 = the finished logo. In between, the
 * nozzle travels in, extrudes the foot of the L, stacks four layers for the upright, then parks.
 */
@Composable
fun LayerlineMark(progress: Float, modifier: Modifier = Modifier, glow: Float = 1f, showDots: Boolean = true) {
    Canvas(modifier.semantics { contentDescription = "Layerline logo" }) {
        drawMark(progress.coerceIn(0f, 1f), glow, showDots)
    }
}

private fun DrawScope.drawMark(t: Float, glow: Float, showDots: Boolean) {
    val s = size.minDimension / 108f
    fun o(x: Float, y: Float) = Offset(x * s, y * s)
    fun rect(color: Color, x: Float, y: Float, w: Float, h: Float) {
        if (w > 0f && h > 0f) drawRect(color, o(x, y), Size(w * s, h * s))
    }

    // 1. Print bed dots.
    if (showDots) {
        val dotAlpha = seg(t, 0f, 0.12f)
        for (gx in 1..11) for (gy in 1..11) {
            val d = (gx + gy) / 22f
            val a = (dotAlpha * 1.4f - d * 0.4f).coerceIn(0f, 1f)
            drawCircle(LL.Dot.copy(alpha = a), radius = 0.7f * s, center = o(gx * 9f, gy * 9f))
        }
    }

    // 2. Spool: track, then the remaining-filament arc sweeping in.
    val ringT = ease(seg(t, 0.05f, 0.45f))
    val r = 27f
    val tl = o(54f - r, 54f - r)
    val sz = Size(2 * r * s, 2 * r * s)
    drawArc(LL.Line, 0f, 360f * ringT, false, tl, sz, style = Stroke(7f * s))
    drawArc(LL.Lime, 120f, 210f * ringT, false, tl, sz, style = Stroke(7f * s, cap = StrokeCap.Round))

    // 3. Nozzle path and the layers it lays down.
    val footT = seg(t, 0.25f, 0.50f)
    rect(LL.Ink, 39f, 61f, 28f * footT, 4f)

    val layerTops = listOf(56f, 51f, 46f, 41f)
    var tipX: Float
    var tipY: Float
    when {
        t < 0.12f -> { tipX = 67f; tipY = -10f }
        t < 0.25f -> {
            val f = ease(seg(t, 0.12f, 0.25f)); tipX = lerp(67f, 39f, f); tipY = lerp(-10f, 58f, f)
        }
        t < 0.50f -> { tipX = 39f + 28f * footT; tipY = 58f }
        t < 0.82f -> {
            val local = seg(t, 0.50f, 0.82f) * 4f
            val idx = local.toInt().coerceAtMost(3)
            val f = local - idx
            val top = layerTops[idx]
            if (f < 0.25f) {
                val fromX = if (idx == 0) 67f else 47f
                val fromY = if (idx == 0) 58f else layerTops[idx - 1] - 3f
                val tf = ease(f / 0.25f); tipX = lerp(fromX, 39f, tf); tipY = lerp(fromY, top - 3f, tf)
            } else {
                tipX = 39f + 8f * ((f - 0.25f) / 0.75f); tipY = top - 3f
            }
        }
        else -> {
            val f = ease(seg(t, 0.82f, 0.92f)); tipX = lerp(47f, 67f, f); tipY = lerp(38f, 58f, f)
        }
    }
    val columnT = seg(t, 0.50f, 0.82f) * 4f
    layerTops.forEachIndexed { i, top ->
        val f = ((columnT - i - 0.25f) / 0.75f).coerceIn(0f, 1f)
        if (columnT > i) rect(LL.Lime, 39f, top, 8f * f, 3f)
    }

    // 4. Filament strand from the spool into the hotend, once it has parked.
    val strandT = seg(t, 0.88f, 1f)
    if (strandT > 0f && ringT > 0.99f) {
        val p = Path().apply {
            moveTo(77.38f * s, 40.5f * s)
            quadraticTo(71.5f * s, 41.5f * s, lerp(77.38f, 67f, strandT) * s, lerp(40.5f, 46f, strandT) * s)
        }
        drawPath(p, LL.Lime, style = Stroke(2f * s, cap = StrokeCap.Round))
    }

    // 5. Hotend: heater block, nozzle cone and the glowing tip.
    if (t >= 0.12f || tipY > -10f) {
        rect(LL.Ink, tipX - 6f, tipY - 12f, 12f, 7f)
        val cone = Path().apply {
            moveTo((tipX - 3f) * s, (tipY - 5f) * s)
            lineTo((tipX + 3f) * s, (tipY - 5f) * s)
            lineTo((tipX + 1f) * s, tipY * s)
            lineTo((tipX - 1f) * s, tipY * s)
            close()
        }
        drawPath(cone, LL.Hot)
        drawCircle(LL.Hot.copy(alpha = 0.25f * glow), radius = 3.2f * s, center = o(tipX, tipY + 1.6f))
        drawCircle(LL.Hot, radius = 1.2f * s, center = o(tipX, tipY + 1.6f))
    }
}

/** Exit dialog art: a parked hotend whose heat shimmer fades as it cools. [heat] goes 1 → 0. */
@Composable
fun CoolingHotend(heat: Float, phase: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension / 100f
        val cx = size.width / 2
        // Heat shimmer.
        for (i in 0..2) {
            val x0 = cx + (i - 1) * 14f * s
            val path = Path()
            var y = 46f * s
            path.moveTo(x0, y)
            while (y > 6f * s) {
                y -= 2f * s
                path.lineTo(x0 + sin((y / s + phase * 40f + i * 20f) / 6f) * 3f * s, y)
            }
            drawPath(path, LL.Hot.copy(alpha = 0.7f * heat), style = Stroke(2f * s, cap = StrokeCap.Round))
        }
        // Heater block and cone.
        val block = lerpColor(LL.Muted, LL.Ink, heat)
        drawRect(block, Offset(cx - 16f * s, 50f * s), Size(32f * s, 18f * s))
        val cone = Path().apply {
            moveTo(cx - 8f * s, 68f * s); lineTo(cx + 8f * s, 68f * s)
            lineTo(cx + 3f * s, 82f * s); lineTo(cx - 3f * s, 82f * s); close()
        }
        drawPath(cone, lerpColor(LL.LineStrong, LL.Hot, heat))
        drawCircle(LL.Hot.copy(alpha = heat), radius = 3f * s, center = Offset(cx, 86f * s))
        // Bed line.
        drawLine(LL.LineStrong, Offset(cx - 40f * s, 96f * s), Offset(cx + 40f * s, 96f * s), 2f * s)
    }
}

private fun lerpColor(a: Color, b: Color, f: Float) = Color(
    red = lerp(a.red, b.red, f),
    green = lerp(a.green, b.green, f),
    blue = lerp(a.blue, b.blue, f),
    alpha = 1f,
)
