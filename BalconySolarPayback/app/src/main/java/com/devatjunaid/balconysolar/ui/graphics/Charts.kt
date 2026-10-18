package com.devatjunaid.balconysolar.ui.graphics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.devatjunaid.balconysolar.ui.theme.AppFonts
import com.devatjunaid.balconysolar.ui.theme.Palette
import kotlin.math.max

private val axisStyle = TextStyle(fontFamily = AppFonts.mono, fontSize = 10.sp, color = Palette.Muted)

private fun DrawScope.label(tm: TextMeasurer, text: String, at: Offset, style: TextStyle = axisStyle, center: Boolean = false, alignEnd: Boolean = false) {
    val layout = tm.measure(text, style)
    val x = when {
        center -> at.x - layout.size.width / 2f
        alignEnd -> at.x - layout.size.width
        else -> at.x
    }
    drawText(layout, topLeft = Offset(x, at.y - layout.size.height / 2f))
}

@Composable
private fun rememberGrow(key: Any?): Float {
    val a = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { a.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    return a.value
}

data class SkyPoint(val azimuth: Double, val elevation: Double)

data class SunPathSeries(val points: List<SkyPoint>, val color: Color, val width: Float)

/**
 * Sun elevation against compass direction. [facingAzimuth] and [facingElevation] mark where the
 * panel's surface normal points; [now] marks the sun at the current moment.
 */
@Composable
fun SunPathChart(
    series: List<SunPathSeries>,
    facingAzimuth: Double,
    facingElevation: Double,
    now: SkyPoint?,
    southern: Boolean,
    modifier: Modifier = Modifier,
    description: String,
) {
    val tm = rememberTextMeasurer()
    val grow = rememberGrow(series.firstOrNull()?.points?.size to series.size)
    Canvas(modifier.semantics { contentDescription = description }) {
        val left = 30f
        val right = size.width - 12f
        val top = 26f
        val bottom = size.height - 30f
        // In the north the sky is centred on south (180°); in the south on north (0/360°).
        val centre = if (southern) 0.0 else 180.0
        fun x(az: Double): Float {
            var d = az - centre
            while (d > 180) d -= 360
            while (d < -180) d += 360
            return (left + (d + 150) / 300 * (right - left)).toFloat()
        }
        val maxEl = max(70.0, (series.flatMap { it.points }.maxOfOrNull { it.elevation } ?: 60.0) + 8)
        fun y(el: Double) = (bottom - el / maxEl * (bottom - top)).toFloat()

        drawRect(Brush.verticalGradient(listOf(Color(0xFF16304F), Palette.Panel), 0f, bottom), size = Size(size.width, bottom))
        for (el in listOf(15, 30, 45, 60, 75)) {
            if (el > maxEl) continue
            drawLine(Palette.Line, Offset(left, y(el.toDouble())), Offset(right, y(el.toDouble())), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 6f)))
            label(tm, "$el°", Offset(2f, y(el.toDouble())))
        }
        for (s in series) {
            val pts = s.points.filter { it.elevation > -1 && kotlin.math.abs(normalise(it.azimuth - centre)) <= 150 }
            if (pts.size < 2) continue
            val count = (pts.size * grow).toInt().coerceAtLeast(2)
            val path = Path()
            pts.take(count).forEachIndexed { i, p ->
                val px = x(p.azimuth); val py = y(p.elevation.coerceAtLeast(0.0))
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            drawPath(path, s.color, style = Stroke(s.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
            // Two-hour dots along the arc are added by the caller as every 12th point.
            pts.take(count).forEachIndexed { i, p -> if (i % 12 == 0 && i > 0) drawCircle(s.color, s.width * 0.8f, Offset(x(p.azimuth), y(p.elevation.coerceAtLeast(0.0)))) }
        }
        // Panel normal.
        val fx = x(facingAzimuth)
        val fy = y(facingElevation.coerceIn(0.0, maxEl))
        drawLine(Palette.Ink, Offset(fx, bottom), Offset(fx, fy + 7f), 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f)))
        drawCircle(Palette.Ink, 7f, Offset(fx, fy), style = Stroke(2f))
        drawCircle(Palette.Ink, 2f, Offset(fx, fy))
        label(tm, "panel faces", Offset(fx + 11f, fy + 12f), axisStyle.copy(color = Palette.Ink))
        if (now != null && now.elevation > 0) {
            val n = Offset(x(now.azimuth), y(now.elevation))
            drawCircle(Palette.Amber.copy(alpha = 0.2f), 20f, n)
            drawCircle(Palette.Amber, 10f, n)
            label(tm, "now", Offset(n.x, n.y - 22f), axisStyle.copy(color = Palette.Amber), center = true)
        }
        drawRect(Color(0xFF0D1A2B), Offset(0f, bottom), Size(size.width, size.height - bottom))
        drawLine(Color(0xFF4A6283), Offset(0f, bottom), Offset(size.width, bottom), 1.5f)
        val marks = if (southern) listOf(225.0 to "SW", 270.0 to "W", 315.0 to "NW", 0.0 to "N", 45.0 to "NE", 90.0 to "E", 135.0 to "SE")
        else listOf(30.0 to "NE", 90.0 to "E", 135.0 to "SE", 180.0 to "S", 225.0 to "SW", 270.0 to "W", 330.0 to "NW")
        for ((az, name) in marks) label(tm, name, Offset(x(az), bottom + 15f), center = true)
    }
}

private fun normalise(d: Double): Double { var v = d; while (v > 180) v -= 360; while (v < -180) v += 360; return v }

/** Monthly bars; tapping a bar selects it. */
@Composable
fun MonthlyBars(values: List<Double>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, description: String) {
    val tm = rememberTextMeasurer()
    val grow = rememberGrow(values.sum().toInt())
    val months = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
    Canvas(
        modifier
            .semantics { contentDescription = description }
            .pointerInput(values.size) {
                detectTapGestures { p ->
                    val slot = size.width / 12f
                    onSelect((p.x / slot).toInt().coerceIn(0, 11))
                }
            },
    ) {
        val max = (values.maxOrNull() ?: 1.0).coerceAtLeast(1e-6)
        val slot = size.width / 12f
        val barW = slot * 0.66f
        val base = size.height - 18f
        val top = 6f
        for (i in values.indices) {
            val h = ((values[i] / max) * (base - top) * grow).toFloat()
            val x = i * slot + (slot - barW) / 2
            drawRoundRect(
                if (i == selected) Palette.Amber else Palette.LineStrong,
                Offset(x, base - h), Size(barW, h), CornerRadius(5f, 5f),
            )
            label(tm, months[i], Offset(i * slot + slot / 2, base + 10f), axisStyle.copy(color = if (i == selected) Palette.Amber else Palette.Muted), center = true)
        }
        drawLine(Color(0xFF4A6283), Offset(0f, base), Offset(size.width, base), 1f)
    }
}

/** Typical-day power: PV as a filled area and household load as a line, hours 0–24. */
@Composable
fun DayCurve(pvW: DoubleArray, loadW: DoubleArray?, modifier: Modifier = Modifier, description: String) {
    val tm = rememberTextMeasurer()
    val grow = rememberGrow(pvW.sum().toInt())
    Canvas(modifier.semantics { contentDescription = description }) {
        val left = 0f
        val right = size.width
        val top = 18f
        val base = size.height - 18f
        val n = pvW.size
        val peak = max(pvW.maxOrNull() ?: 0.0, loadW?.maxOrNull() ?: 0.0).coerceAtLeast(50.0) * 1.1
        fun x(i: Int) = left + (i + 0.5f) / n * (right - left)
        fun y(v: Double) = (base - v / peak * (base - top) * grow).toFloat()
        val area = Path().apply {
            moveTo(x(0), base)
            for (i in 0 until n) lineTo(x(i), y(pvW[i]))
            lineTo(x(n - 1), base); close()
        }
        drawPath(area, Brush.verticalGradient(listOf(Palette.Amber.copy(alpha = 0.55f), Palette.Amber.copy(alpha = 0.03f)), top, base))
        val line = Path().apply { for (i in 0 until n) if (i == 0) moveTo(x(i), y(pvW[i])) else lineTo(x(i), y(pvW[i])) }
        drawPath(line, Palette.Amber, style = Stroke(2.5f, join = StrokeJoin.Round))
        if (loadW != null) {
            val l = Path().apply { for (i in 0 until n) if (i == 0) moveTo(x(i), y(loadW[i])) else lineTo(x(i), y(loadW[i])) }
            drawPath(l, Palette.Mint, style = Stroke(2f, join = StrokeJoin.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))))
        }
        val iPeak = pvW.indices.maxByOrNull { pvW[it] } ?: 0
        if (pvW[iPeak] > 0) {
            val p = Offset(x(iPeak), y(pvW[iPeak]))
            drawCircle(Palette.Night, 5f, p); drawCircle(Palette.Amber, 5f, p, style = Stroke(2f))
            label(tm, "${pvW[iPeak].toInt()} W", Offset(p.x + 8f, p.y - 8f), axisStyle.copy(color = Palette.Amber))
        }
        drawLine(Color(0xFF4A6283), Offset(left, base), Offset(right, base), 1f)
        for (h in 0..24 step 6) label(tm, "%02d".format(h), Offset(left + h / 24f * (right - left), base + 10f), center = h in 1..23, alignEnd = h == 24)
    }
}

/** Cumulative net savings by year with the break-even point marked. */
@Composable
fun CumulativeChart(cumulative: List<Double>, paybackYears: Double?, currencyLabel: (Double) -> String, modifier: Modifier = Modifier, description: String) {
    val tm = rememberTextMeasurer()
    val grow = rememberGrow(cumulative.lastOrNull()?.toInt())
    Canvas(modifier.semantics { contentDescription = description }) {
        val left = 44f
        val right = size.width - 6f
        val top = 10f
        val base = size.height - 20f
        val years = cumulative.size - 1
        val minV = cumulative.minOrNull() ?: 0.0
        val maxV = (cumulative.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
        fun x(yr: Double) = (left + yr / years * (right - left)).toFloat()
        fun y(v: Double) = (base - (v - minV) / (maxV - minV) * (base - top)).toFloat()
        for (k in 0..3) {
            val v = minV + (maxV - minV) * k / 3
            drawLine(Palette.Line, Offset(left, y(v)), Offset(right, y(v)), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 5f)))
            label(tm, currencyLabel(v), Offset(left - 6f, y(v)), alignEnd = true)
        }
        // Zero line = costs recovered.
        if (minV < 0 && maxV > 0) {
            drawLine(Palette.Amber, Offset(left, y(0.0)), Offset(right, y(0.0)), 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            label(tm, "break-even", Offset(right, y(0.0) - 9f), axisStyle.copy(color = Palette.Amber), alignEnd = true)
        }
        val shown = (years * grow).coerceAtLeast(0.001f)
        val path = Path()
        val area = Path()
        area.moveTo(x(0.0), base)
        for (i in 0..years) {
            if (i > shown + 1) break
            val px = x(minOf(i.toDouble(), shown.toDouble()))
            val v = if (i <= shown) cumulative[i] else cumulative[i - 1] + (cumulative[i] - cumulative[i - 1]) * (shown - (i - 1))
            if (i == 0) path.moveTo(px, y(v)) else path.lineTo(px, y(v))
            area.lineTo(px, y(v))
        }
        area.lineTo(x(shown.toDouble()), base); area.close()
        drawPath(area, Brush.verticalGradient(listOf(Palette.Mint.copy(alpha = 0.30f), Color.Transparent), top, base))
        drawPath(path, Palette.Mint, style = Stroke(2.5f, join = StrokeJoin.Round))
        if (paybackYears != null && paybackYears <= years && grow > 0.95f) {
            val p = Offset(x(paybackYears), y(0.0))
            drawLine(Palette.Ink, p, Offset(p.x, base), 1f)
            drawCircle(Palette.Night, 6f, p); drawCircle(Palette.Ink, 6f, p, style = Stroke(2f))
        }
        drawLine(Color(0xFF4A6283), Offset(left, base), Offset(right, base), 1f)
        val step = if (years > 20) 10 else 5
        for (yr in 0..years step step) label(tm, if (yr == years) "$yr yr" else "$yr", Offset(x(yr.toDouble()), base + 11f), center = yr != years, alignEnd = yr == years)
    }
}

@Composable
fun RingGauge(fraction: Double, modifier: Modifier = Modifier, color: Color = Palette.Mint) {
    val grow = rememberGrow(fraction)
    Canvas(modifier) {
        val stroke = size.minDimension * 0.13f
        val d = size.minDimension - stroke
        val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
        drawArc(Palette.Line, 0f, 360f, false, tl, Size(d, d), style = Stroke(stroke))
        drawArc(color, -90f, (360f * fraction.coerceIn(0.0, 1.0) * grow).toFloat(), false, tl, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round))
    }
}
