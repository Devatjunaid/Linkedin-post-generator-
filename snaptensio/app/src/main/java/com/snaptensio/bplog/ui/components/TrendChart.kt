package com.snaptensio.bplog.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snaptensio.bplog.data.Reading
import com.snaptensio.bplog.domain.DAY_MS
import com.snaptensio.bplog.ui.theme.Brand
import com.snaptensio.bplog.ui.theme.Manrope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChartMarker(val timestamp: Long, val label: String)

/**
 * Systolic/diastolic line chart with dashed target lines. Lines draw in from left to right whenever
 * the data changes. Readings arrive newest-first from the database; they are sorted here.
 */
@Composable
fun TrendChart(
    readings: List<Reading>,
    targetSys: Int,
    targetDia: Int,
    days: Int,
    modifier: Modifier = Modifier,
    height: Dp = 190.dp,
    markers: List<ChartMarker> = emptyList(),
    showAxis: Boolean = true,
    now: Long = System.currentTimeMillis(),
) {
    val data = remember(readings) { readings.sortedBy { it.timestamp } }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(data, days) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    val measurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val targetColor = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    val labelStyle = TextStyle(fontFamily = Manrope, fontSize = 10.sp, color = axisColor)
    val fmt = remember(days) { SimpleDateFormat(if (days > 120) "MMM" else "d MMM", Locale.getDefault()) }
    val desc = if (data.isEmpty()) "No readings in this period" else
        "Chart of ${data.size} readings. Systolic from ${data.minOf { it.systolic }} to ${data.maxOf { it.systolic }}, diastolic from ${data.minOf { it.diastolic }} to ${data.maxOf { it.diastolic }}."

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            Modifier.fillMaxWidth().height(height).clipToBounds().semantics { contentDescription = desc },
        ) {
            val left = if (showAxis) 30.dp.toPx() else 0f
            val bottomPad = if (showAxis) 18.dp.toPx() else 0f
            val plot = Rect(left, 6.dp.toPx(), size.width - 4.dp.toPx(), size.height - bottomPad)
            val t1 = now
            val t0 = now - days * DAY_MS
            val values = data.flatMap { listOf(it.systolic, it.diastolic) } + listOf(targetSys, targetDia)
            val minV = ((values.minOrNull() ?: 60) - 10).coerceAtLeast(30) / 10 * 10
            val maxV = ((values.maxOrNull() ?: 160) + 10 + 9) / 10 * 10
            fun x(ts: Long) = plot.left + (ts - t0).toFloat() / (t1 - t0) * plot.width
            fun y(v: Int) = plot.bottom - (v - minV).toFloat() / (maxV - minV) * plot.height

            // Grid + y labels
            val step = if (maxV - minV > 100) 40 else 20
            var v = (minV / step + 1) * step
            while (v < maxV) {
                drawLine(gridColor, Offset(plot.left, y(v)), Offset(plot.right, y(v)), 1f)
                if (showAxis) drawText(measurer, v.toString(), Offset(0f, y(v) - 7.dp.toPx()), labelStyle)
                v += step
            }
            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            drawLine(targetColor, Offset(plot.left, y(targetSys)), Offset(plot.right, y(targetSys)), 2f, pathEffect = dash)
            drawLine(targetColor, Offset(plot.left, y(targetDia)), Offset(plot.right, y(targetDia)), 2f, pathEffect = dash)

            // X labels
            if (showAxis) {
                listOf(t0, (t0 + t1) / 2, t1).forEachIndexed { i, t ->
                    val text = fmt.format(Date(t))
                    val w = measurer.measure(text, labelStyle).size.width
                    val tx = when (i) { 0 -> plot.left; 1 -> x(t) - w / 2; else -> plot.right - w }
                    drawText(measurer, text, Offset(tx, plot.bottom + 4.dp.toPx()), labelStyle)
                }
            }

            // Markers (e.g. medication started)
            markers.filter { it.timestamp in t0..t1 }.forEach { m ->
                val mx = x(m.timestamp)
                drawLine(onSurface.copy(alpha = 0.6f), Offset(mx, plot.top), Offset(mx, plot.bottom), 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
                drawText(measurer, m.label, Offset((mx + 4.dp.toPx()).coerceAtMost(plot.right - 80.dp.toPx()), plot.top), labelStyle.copy(color = onSurface))
            }

            if (data.isEmpty()) return@Canvas
            val reveal = plot.left + plot.width * progress.value
            clipRect(right = reveal) {
                fun series(color: Color, value: (Reading) -> Int) {
                    val path = Path()
                    data.forEachIndexed { i, r ->
                        val p = Offset(x(r.timestamp), y(value(r)))
                        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                    }
                    drawPath(path, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    if (data.size <= 60) data.forEach { r -> drawCircle(color, 3.dp.toPx(), Offset(x(r.timestamp), y(value(r)))) }
                }
                series(Brand.Systolic) { it.systolic }
                series(Brand.Diastolic) { it.diastolic }
            }
        }
        if (showAxis) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendItem(Brand.Systolic, "Systolic")
                LegendItem(Brand.Diastolic, "Diastolic")
                LegendItem(targetColor, "Target $targetSys/$targetDia", dashed = true)
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, dashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.width(14.dp).height(4.dp)) {
            drawLine(
                color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), size.height,
                cap = StrokeCap.Round,
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(6f, 5f)) else null,
            )
        }
        Box(Modifier.size(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
