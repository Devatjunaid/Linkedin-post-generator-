package com.sunarc.inverterlogger.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunarc.inverterlogger.ui.theme.DisplayFont
import com.sunarc.inverterlogger.ui.theme.MonoFont
import com.sunarc.inverterlogger.ui.theme.Palette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * The Sun Arc: the sun's path from sunrise to sunset with today's clear-sky production curve
 * beneath it. The logged part is solid amber, the rest is a dashed sky-blue forecast, and the sun
 * travels to the current time when the screen opens.
 */
@Composable
fun SunArc(
    curve: List<Float>,
    progress: Float,
    modifier: Modifier = Modifier,
    description: String
) {
    val travel = remember { Animatable(0f) }
    LaunchedEffect(progress) { travel.animateTo(progress, tween(1400, easing = FastOutSlowInEasing)) }
    val loop = rememberInfiniteTransition(label = "arc")
    val pulse by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "sunPulse")

    Canvas(modifier.semantics { contentDescription = description }) {
        val left = size.width * 0.07f
        val right = size.width * 0.93f
        val base = size.height * 0.80f
        val radius = (right - left) / 2
        val center = Offset((left + right) / 2, base)
        val p = travel.value

        drawLine(Palette.Line, Offset(left - 10, base), Offset(right + 10, base))
        drawArc(
            Palette.Line, 180f, 180f, false, Offset(center.x - radius, center.y - radius), Size(radius * 2, radius * 2),
            style = Stroke(1.5f.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 12f)))
        )
        drawArc(
            Palette.Sun, 180f, 180f * p, false, Offset(center.x - radius, center.y - radius), Size(radius * 2, radius * 2),
            style = Stroke(2.5f.dp.toPx(), cap = StrokeCap.Round)
        )

        if (curve.size >= 2) {
            val peak = max(curve.max(), 0.0001f)
            val h = radius * 0.62f
            fun pt(i: Int) = Offset(left + (right - left) * i / (curve.size - 1), base - curve[i] / peak * h)
            val cut = (p * (curve.size - 1)).toInt().coerceIn(0, curve.size - 1)
            val logged = Path().apply {
                moveTo(left, base)
                for (i in 0..cut) lineTo(pt(i).x, pt(i).y)
                lineTo(pt(cut).x, base)
                close()
            }
            drawPath(logged, Brush.verticalGradient(listOf(Palette.Sun.copy(alpha = 0.38f), Palette.Sun.copy(alpha = 0f)), startY = base - h, endY = base))
            val loggedLine = Path().apply { moveTo(pt(0).x, pt(0).y); for (i in 1..cut) lineTo(pt(i).x, pt(i).y) }
            drawPath(loggedLine, Palette.SunSoft, style = Stroke(1.5f.dp.toPx()))
            if (cut < curve.size - 1) {
                val forecast = Path().apply { moveTo(pt(cut).x, pt(cut).y); for (i in cut + 1 until curve.size) lineTo(pt(i).x, pt(i).y) }
                drawPath(forecast, Palette.Sky, style = Stroke(1.5f.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))))
            }
        }

        val a = PI.toFloat() * (1f - p)
        val sun = Offset(center.x + cos(a) * radius, center.y - sin(a) * radius)
        drawLine(Palette.Sun.copy(alpha = 0.4f), sun, Offset(sun.x, base), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
        drawCircle(Palette.Sun.copy(alpha = 0.14f), radius = (20f + 4f * pulse).dp.toPx() / 1.6f, center = sun)
        drawCircle(Palette.Sun, radius = (9f + 1.5f * pulse).dp.toPx() / 1.3f, center = sun)
    }
}

data class Bar(val value: Double, val highlight: Boolean = false, val estimated: Boolean = false)

/** Bars grow from the axis in a cascade; the highlighted (newest) bar drops in with a bounce. */
@Composable
fun BarChart(bars: List<Bar>, modifier: Modifier = Modifier, height: Dp = 130.dp, description: String, key: Any = bars) {
    val grow = remember(key) { Animatable(0f) }
    val drop = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        grow.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        drop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Canvas(modifier.fillMaxWidth().height(height).semantics { contentDescription = description }) {
        if (bars.isEmpty()) return@Canvas
        val peak = max(bars.maxOf { it.value }, 0.0001).toFloat()
        val gap = 4.dp.toPx()
        val w = (size.width - gap * (bars.size - 1)) / bars.size
        drawLine(Palette.Line, Offset(0f, size.height), Offset(size.width, size.height))
        drawLine(Palette.Line, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
        bars.forEachIndexed { i, b ->
            val local = ((grow.value * bars.size - i * 0.5f) / 2f).coerceIn(0f, 1f)
            val fullH = (b.value.toFloat() / peak) * size.height
            val x = i * (w + gap)
            if (b.highlight) {
                val y = size.height - fullH - (1f - drop.value) * size.height * 0.5f
                drawRoundRect(Palette.Sun, Offset(x, y), Size(w, fullH), CornerRadius(w / 3), alpha = drop.value.coerceIn(0f, 1f))
            } else {
                val h = fullH * local
                drawRoundRect(
                    if (b.estimated) Palette.SunDeep.copy(alpha = 0.6f) else Color(0xFF5A4A26),
                    Offset(x, size.height - h), Size(w, h), CornerRadius(w / 3)
                )
            }
        }
    }
}

/** A month as a 7-column calendar, each day tinted by its yield. */
@Composable
fun YieldHeatmap(days: List<Pair<Int, Double?>>, firstDayOffset: Int, today: Int?, maxKwh: Double, modifier: Modifier = Modifier) {
    val cells = List(firstDayOffset) { null as Pair<Int, Double?>? } + days
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 0 until 7) {
                    val cell = week.getOrNull(i)
                    Box(Modifier.weight(1f).aspectRatio(1.15f), contentAlignment = Alignment.Center) {
                        if (cell != null) {
                            val (day, kwh) = cell
                            val share = if (kwh == null || maxKwh <= 0) -1.0 else kwh / maxKwh
                            val bg = when {
                                share < 0 -> Color.Transparent
                                share > 0.8 -> Palette.Sun
                                share > 0.6 -> Color(0xFFB8811A)
                                share > 0.35 -> Color(0xFF7A5A16)
                                else -> Color(0xFF2A2418)
                            }
                            Box(
                                Modifier.matchParentSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .then(if (share < 0) Modifier.border(1.dp, Palette.Line, RoundedCornerShape(8.dp)) else Modifier)
                                    .then(if (day == today) Modifier.border(2.dp, Palette.Ink, RoundedCornerShape(8.dp)) else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$day", fontFamily = MonoFont, fontSize = 10.sp,
                                    color = if (share > 0.6) Palette.OnSun else if (share < 0) Palette.Faint else Palette.InkSoft
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


/** System health ring: share of expected output, sweeping in on open. */
@Composable
fun HealthRing(percent: Int?, modifier: Modifier = Modifier, size: Dp = 116.dp) {
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(percent) { sweep.animateTo((percent ?: 0) / 100f, tween(1300, easing = FastOutSlowInEasing)) }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size).semantics { contentDescription = if (percent == null) "Health not known yet" else "System health $percent percent of expected" }) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2
            drawArc(Palette.Line, 0f, 360f, false, Offset(inset, inset), Size(this.size.width - stroke, this.size.height - stroke), style = Stroke(stroke))
            drawArc(
                Brush.sweepGradient(listOf(Palette.Sky, Palette.Sun, Palette.SunSoft, Palette.Sky)), -90f, 360f * sweep.value.coerceIn(0f, 1f), false,
                Offset(inset, inset), Size(this.size.width - stroke, this.size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (percent == null) "–" else "$percent%", fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Palette.Ink)
            Text("of expected", fontSize = 10.sp, color = Palette.Muted)
        }
    }
}

/** Horizontal payback track with a glowing sun marker. */
@Composable
fun PaybackTrack(fraction: Float, modifier: Modifier = Modifier) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(fraction) { anim.animateTo(fraction.coerceIn(0f, 1f), tween(1200, easing = FastOutSlowInEasing)) }
    Canvas(modifier.fillMaxWidth().height(34.dp).semantics { contentDescription = "Payback ${(fraction * 100).toInt()} percent" }) {
        val y = size.height / 2
        drawLine(Palette.Line, Offset(0f, y), Offset(size.width, y), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
        val x = size.width * anim.value
        drawLine(Palette.Sun, Offset(0f, y), Offset(x, y), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(Palette.Sun.copy(alpha = 0.3f), radius = 15.dp.toPx(), center = Offset(x, y))
        drawCircle(Palette.Sun, radius = 8.dp.toPx(), center = Offset(x, y))
    }
}

/** Stacked bar for self-used vs exported energy. */
@Composable
fun SplitBar(first: Float, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (first > 0f) Box(Modifier.weight(first.coerceAtLeast(0.001f)).height(12.dp).background(Palette.Sun))
        if (first < 1f) Box(Modifier.weight((1f - first).coerceAtLeast(0.001f)).height(12.dp).background(Palette.Sky))
    }
}
