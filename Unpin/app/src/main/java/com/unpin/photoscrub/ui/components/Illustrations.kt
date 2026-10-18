package com.unpin.photoscrub.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unpin.photoscrub.ui.theme.Hill
import com.unpin.photoscrub.ui.theme.Ink
import com.unpin.photoscrub.ui.theme.InkLine
import com.unpin.photoscrub.ui.theme.InkSoft
import com.unpin.photoscrub.ui.theme.Mono
import com.unpin.photoscrub.ui.theme.MutedOnInk
import com.unpin.photoscrub.ui.theme.Paper
import com.unpin.photoscrub.ui.theme.Signal
import com.unpin.photoscrub.ui.theme.Sky
import com.unpin.photoscrub.ui.theme.Sun
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** 20-cell meter: [exposedCells] of 20 lit in signal orange. */
@Composable
fun ExposureMeter(exposedCells: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(20) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (i < exposedCells) Signal else InkLine)
            )
        }
    }
}

/**
 * Stylised street map around the photo's coordinates (generated from the
 * coordinates, not real map data) with a pulsing pin at the exact spot.
 */
@Composable
fun MapPreview(latitude: Double, longitude: Double, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "map")
    val ping by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "ping")
    val seed = remember(latitude, longitude) { ((latitude * 1000).toLong() * 31 + (longitude * 1000).toLong()) }
    Canvas(modifier) {
        val rnd = Random(seed)
        drawRect(Color(0xFFE7E2D6))
        // river
        val river = Path().apply {
            val y0 = size.height * (0.55f + rnd.nextFloat() * 0.3f)
            moveTo(-20f, y0)
            cubicTo(size.width * 0.3f, y0 - size.height * 0.3f, size.width * 0.6f, y0 + size.height * 0.25f, size.width + 20f, y0 - size.height * 0.2f)
        }
        drawPath(river, Color(0xFFBCD0D9), style = Stroke(14.dp.toPx(), cap = StrokeCap.Round))
        // parks and blocks
        repeat(5) {
            val w = size.width * (0.08f + rnd.nextFloat() * 0.12f)
            val h = size.height * (0.12f + rnd.nextFloat() * 0.18f)
            drawRoundRect(
                if (it % 2 == 0) Color(0xFFCBD9C4) else Color(0xFFD6D0C1),
                Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height), Size(w, h), CornerRadius(3.dp.toPx())
            )
        }
        // street grid
        val street = Color(0xFFFBF9F4)
        var x = rnd.nextFloat() * 50f
        while (x < size.width) {
            drawLine(street, Offset(x, 0f), Offset(x, size.height), 4.dp.toPx()); x += 60.dp.toPx() + rnd.nextFloat() * 40f
        }
        var y = rnd.nextFloat() * 30f
        while (y < size.height) {
            drawLine(street, Offset(0f, y), Offset(size.width, y), 4.dp.toPx()); y += 34.dp.toPx() + rnd.nextFloat() * 20f
        }
        drawLine(street, Offset(0f, size.height * 0.1f), Offset(size.width, size.height * 0.85f), 7.dp.toPx())
        drawLine(street, Offset(size.width * 0.55f, 0f), Offset(size.width * 0.4f, size.height), 6.dp.toPx())

        // pin
        val c = Offset(size.width / 2f, size.height * 0.42f)
        drawCircle(Signal, 34.dp.toPx() * (0.4f + 0.6f * ping), c, alpha = 0.25f * (1f - ping))
        drawCircle(Signal, 18.dp.toPx(), c, alpha = 0.2f)
        val s = 1.dp.toPx()
        translate(c.x - 12 * s, c.y - 24 * s) {
            val pin = Path().apply {
                moveTo(12 * s, 0f)
                cubicTo(5 * s, 0f, 0f, 5 * s, 0f, 11.5f * s)
                cubicTo(0f, 20f * s, 12 * s, 32 * s, 12 * s, 32 * s)
                cubicTo(12 * s, 32 * s, 24 * s, 20f * s, 24 * s, 11.5f * s)
                cubicTo(24 * s, 5 * s, 19 * s, 0f, 12 * s, 0f)
                close()
            }
            drawPath(pin, Ink)
            drawCircle(Signal, 4 * s, Offset(12 * s, 11.5f * s))
        }
    }
}

/** Report hero: a photo whose metadata is being shredded into falling strips. */
@Composable
fun ShredderHero(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shred")
    val t by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "t")
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontFamily = Mono, fontSize = 11.sp, color = MutedOnInk)
    val tags = listOf("GPS ✕", "ISO 64 ✕", "07:42 ✕", "SERIAL ✕", "f/1.7 ✕", "MODEL ✕")
    Canvas(modifier) {
        drawRect(Ink)
        val grid = InkSoft
        var gx = size.width / 6f
        while (gx < size.width) { drawLine(grid, Offset(gx, 0f), Offset(gx, size.height), 1f); gx += size.width / 6f }
        var gy = size.height / 4f
        while (gy < size.height) { drawLine(grid, Offset(0f, gy), Offset(size.width, gy), 1f); gy += size.height / 4f }

        val u = size.height / 236f
        val cardW = 140 * u
        val left = (size.width - cardW) / 2f
        val top = 22 * u
        // intact top of the photo
        clipRect(left, top, left + cardW, top + 92 * u) {
            drawRoundRect(Sky, Offset(left, top), Size(cardW, 100 * u), CornerRadius(10 * u))
            drawCircle(Sun, 10 * u, Offset(left + 105 * u, top + 24 * u))
            val hills = Path().apply {
                moveTo(left, top + 78 * u); lineTo(left + 34 * u, top + 44 * u); lineTo(left + 60 * u, top + 66 * u)
                lineTo(left + 82 * u, top + 48 * u); lineTo(left + cardW, top + 88 * u); lineTo(left + cardW, top + 92 * u)
                lineTo(left, top + 92 * u); close()
            }
            drawPath(hills, Hill)
        }
        drawRect(Paper, Offset(left, top + 88 * u), Size(cardW, 6 * u))
        drawLine(Signal, Offset(left, top + 94 * u), Offset(left + cardW, top + 94 * u), 2 * u,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4 * u, 4 * u), t * 8 * u))

        // falling strips
        val colors = listOf(Paper, Signal, Paper, InkLine, Paper, Signal, Paper)
        val stripW = 16 * u
        for (i in 0 until 7) {
            val phase = (t + i * 0.143f) % 1f
            val fall = phase * 60 * u
            val len = (36 + (i * 13) % 30) * u
            val cx = left + 2 * u + i * 20 * u + stripW / 2
            val cy = top + 104 * u + fall + len / 2
            rotate(sin((phase + i) * 6f) * 10f, Offset(cx, cy)) {
                drawRoundRect(
                    colors[i], Offset(cx - stripW / 2, cy - len / 2), Size(stripW, len), CornerRadius(2 * u),
                    alpha = (1f - phase).coerceIn(0f, 1f)
                )
            }
        }
        // metadata fragments drifting away
        val spots = listOf(Offset(22f, 140f), Offset(34f, 190f), Offset(268f, 140f), Offset(258f, 196f), Offset(22f, 36f), Offset(270f, 36f))
        tags.forEachIndexed { i, tag ->
            val phase = (t + i * 0.17f) % 1f
            val drift = abs(sin(phase * 3.14f))
            val base = spots[i]
            val px = base.x / 358f * size.width
            val py = base.y * u - drift * 6 * u
            drawText(measurer, tag, Offset(px, py), labelStyle.copy(color = MutedOnInk.copy(alpha = 0.4f + 0.6f * drift)))
        }
    }
}

/** Exit dialog art: the pin lifts off the photo and floats away, looping. */
@Composable
fun FlyAwayGraphic(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "fly")
    val t by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "t")
    Canvas(modifier) { drawFlyAway(t) }
}

private fun DrawScope.drawFlyAway(t: Float) {
    val u = size.height / 140f
    val cx = size.width / 2f
    drawCircle(Signal, 60 * u, Offset(cx, 76 * u), alpha = 0.08f)
    drawCircle(Ink, 44 * u, Offset(cx, 76 * u), alpha = 0.05f)
    // photo card
    rotate(-6f, Offset(cx, 96 * u)) {
        drawRoundRect(Card, Offset(cx - 44 * u, 66 * u), Size(88 * u, 62 * u), CornerRadius(8 * u))
        drawRoundRect(Ink, Offset(cx - 44 * u, 66 * u), Size(88 * u, 62 * u), CornerRadius(8 * u), style = Stroke(1.5f * u))
        drawRect(Sky, Offset(cx - 38 * u, 72 * u), Size(76 * u, 40 * u))
        drawCircle(Sun, 5 * u, Offset(cx + 22 * u, 82 * u))
        val hills = Path().apply {
            moveTo(cx - 38 * u, 112 * u); lineTo(cx - 38 * u, 104 * u); lineTo(cx - 16 * u, 88 * u); lineTo(cx, 100 * u)
            lineTo(cx + 14 * u, 92 * u); lineTo(cx + 38 * u, 106 * u); lineTo(cx + 38 * u, 112 * u); close()
        }
        drawPath(hills, Hill)
    }
    // the spot the pin left
    drawCircle(Signal, 4 * u, Offset(cx, 92 * u), alpha = 0.5f + 0.5f * t)
    // pin floating up with a dashed trail
    val lift = easeOut(t)
    val px = cx + sin(t * 6f) * 10 * u
    val py = 70 * u - lift * 58 * u
    val alpha = if (t < 0.8f) 1f else (1f - (t - 0.8f) / 0.2f)
    val trail = Path().apply {
        moveTo(cx, 90 * u)
        quadraticTo(cx - 14 * u, (90 * u + py) / 2f, px, py + 22 * u)
    }
    drawPath(trail, Ink, alpha = 0.35f * alpha, style = Stroke(1.6f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4 * u, 5 * u))))
    translate(px - 12 * u, py - 8 * u) {
        rotate(sin(t * 8f) * 12f, Offset(12 * u, 16 * u)) {
            val pin = Path().apply {
                moveTo(12 * u, 0f)
                cubicTo(5 * u, 0f, 0f, 5 * u, 0f, 11.5f * u)
                cubicTo(0f, 20 * u, 12 * u, 30 * u, 12 * u, 30 * u)
                cubicTo(12 * u, 30 * u, 24 * u, 20 * u, 24 * u, 11.5f * u)
                cubicTo(24 * u, 5 * u, 19 * u, 0f, 12 * u, 0f)
                close()
            }
            drawPath(pin, Signal, alpha = alpha)
            drawCircle(Ink, 4.2f * u, Offset(12 * u, 11.5f * u), alpha = alpha)
        }
    }
    // little data bits trailing
    for (i in 0 until 5) {
        val p = ((t * 1.3f + i * 0.2f) % 1f)
        drawRect(
            if (i % 2 == 0) Signal else Ink,
            Offset(cx + (i - 2) * 14 * u + sin(p * 5f) * 4 * u, 88 * u - p * 70 * u),
            Size(4 * u, 4 * u), alpha = (1f - p) * 0.8f
        )
    }
}

private val Card = Color(0xFFFFFFFF)
private fun easeOut(x: Float) = 1f - (1f - x) * (1f - x)
