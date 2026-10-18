package app.layerline.toolkit3d.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.layerline.toolkit3d.ui.theme.LL

/** Spool seen from the side: flange, track, coloured filament arc for what is left, hub. */
@Composable
fun SpoolRing(fraction: Float, color: Color, modifier: Modifier = Modifier, size: Dp = 64.dp, label: String = "") {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(fraction) { anim.animateTo(fraction.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing)) }
    Canvas(
        modifier
            .size(size)
            .semantics { if (label.isNotEmpty()) contentDescription = label },
    ) {
        val w = this.size.minDimension
        val c = Offset(w / 2, w / 2)
        drawCircle(LL.Graphite, radius = w * 0.476f, center = c)
        drawCircle(LL.LineStrong, radius = w * 0.476f, center = c, style = Stroke(1.dp.toPx()))
        val ringR = w * 0.333f
        val ringW = w * 0.19f
        drawCircle(Color(0xFF262B2F), radius = ringR, center = c, style = Stroke(ringW))
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * anim.value,
            useCenter = false,
            topLeft = Offset(c.x - ringR, c.y - ringR),
            size = Size(ringR * 2, ringR * 2),
            style = Stroke(ringW),
        )
        drawCircle(LL.Panel, radius = w * 0.143f, center = c)
        drawCircle(LL.LineStrong, radius = w * 0.143f, center = c, style = Stroke(1.dp.toPx()))
        drawCircle(LL.LineStrong, radius = w * 0.048f, center = c)
    }
}

/** Stack of printed layers filling up from the bottom; the newest layer pulses while printing. */
@Composable
fun LayerStack(progress: Float, modifier: Modifier = Modifier, layers: Int = 12, color: Color = LL.Lime, live: Boolean = true) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(800), label = "layers")
    val pulse = rememberInfiniteTransition(label = "pulse")
    val glow by pulse.animateFloat(0.35f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "glow")
    Canvas(modifier.semantics { contentDescription = "Print progress ${(progress * 100).toInt()} percent" }) {
        val gap = 3.dp.toPx()
        val h = (size.height - gap * (layers - 1)) / layers
        val filled = p * layers
        for (i in 0 until layers) {
            val inset = (if (i % 3 == 0) 0f else (i % 3) * 4.dp.toPx())
            val y = size.height - (i + 1) * h - i * gap
            val amount = (filled - i).coerceIn(0f, 1f)
            drawRect(LL.Line, Offset(inset, y), Size(size.width - inset * 2, h))
            if (amount > 0f) {
                val a = if (live && amount < 1f) glow else 1f
                drawRect(color.copy(alpha = a), Offset(inset, y), Size((size.width - inset * 2) * amount, h))
            }
        }
    }
}

/** Ten-segment counter bar used for maintenance intervals. */
@Composable
fun SegmentBar(fraction: Float, modifier: Modifier = Modifier, segments: Int = 10, color: Color = LL.Lime) {
    val filled = (fraction.coerceIn(0f, 1f) * segments).let { kotlin.math.round(it).toInt() }
    Row(modifier.fillMaxWidth().height(8.dp)) {
        for (i in 0 until segments) {
            Box(
                Modifier
                    .weight(1f)
                    .height(8.dp)
                    .drawBehind { drawRect(if (i < filled) color else LL.Line) },
            )
            if (i < segments - 1) Box(Modifier.size(3.dp))
        }
    }
}

/** Dashed track with a sweeping arc and the percentage in the middle. */
@Composable
fun SuccessRing(fraction: Float, modifier: Modifier = Modifier, size: Dp = 96.dp, centerText: String, caption: String, textStyle: TextStyle, captionStyle: TextStyle) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(fraction) { anim.animateTo(fraction.coerceIn(0f, 1f), tween(1100, easing = FastOutSlowInEasing)) }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 10.dp.toPx()
            val r = this.size.minDimension / 2 - stroke / 2
            val tl = Offset(this.size.width / 2 - r, this.size.height / 2 - r)
            drawArc(
                LL.Line, 0f, 360f, false, tl, Size(r * 2, r * 2),
                style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx()))),
            )
            drawArc(LL.Lime, -90f, 360f * anim.value, false, tl, Size(r * 2, r * 2), style = Stroke(stroke))
        }
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerText, style = textStyle)
            Text(caption, style = captionStyle)
        }
    }
}

/** Vernier-style ruler with OLD and NEW markers for calibration results. */
@Composable
fun CalibrationRuler(oldPos: Float, newPos: Float, modifier: Modifier = Modifier) {
    val o by animateFloatAsState(oldPos, tween(600), label = "old")
    val n by animateFloatAsState(newPos, tween(900, easing = FastOutSlowInEasing), label = "new")
    Canvas(modifier.fillMaxWidth().height(34.dp)) {
        val minor = 8.dp.toPx()
        drawLine(Color(0xFF4A5056), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
        var x = 0f
        var i = 0
        while (x <= size.width) {
            val tall = i % 5 == 0
            drawLine(if (tall) Color(0xFF7A8086) else Color(0xFF4A5056), Offset(x, 0f), Offset(x, if (tall) 18.dp.toPx() else 10.dp.toPx()), 1.dp.toPx())
            x += minor
            i++
        }
        drawLine(LL.Muted, Offset(size.width * o, -4.dp.toPx()), Offset(size.width * o, 26.dp.toPx()), 2.dp.toPx())
        drawLine(LL.Lime, Offset(size.width * n, -8.dp.toPx()), Offset(size.width * n, 30.dp.toPx()), 3.dp.toPx(), cap = StrokeCap.Square)
    }
}

/** Receipt perforation along the top edge. */
fun Modifier.perforatedTop(holeColor: Color = LL.Graphite): Modifier = drawBehind {
    val r = 5.dp.toPx()
    val step = 14.dp.toPx()
    var x = step / 2
    while (x < size.width) {
        drawCircle(holeColor, radius = r, center = Offset(x, 0f))
        x += step
    }
}

/** A thin row of "printed" lines used as a loading / idle accent. */
@Composable
fun ExtrusionLine(modifier: Modifier = Modifier, color: Color = LL.Lime) {
    val t = rememberInfiniteTransition(label = "extrude")
    val head by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), label = "head")
    Canvas(modifier.fillMaxWidth().height(6.dp)) {
        drawRoundRect(LL.Line, size = Size(size.width, 3.dp.toPx()), cornerRadius = CornerRadius(2f))
        drawRoundRect(color, size = Size(size.width * head, 3.dp.toPx()), cornerRadius = CornerRadius(2f))
        drawCircle(LL.Hot, radius = 3.dp.toPx(), center = Offset(size.width * head, 1.5.dp.toPx()))
    }
}
