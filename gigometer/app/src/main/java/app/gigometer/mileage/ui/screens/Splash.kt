package app.gigometer.mileage.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.gigometer.mileage.ui.theme.Grotesk
import app.gigometer.mileage.ui.theme.Manrope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private val Mint = Color(0xFF4FD18B)
private val Amber = Color(0xFFF2B33D)

/**
 * Brand splash: the speedometer arc sweeps in, the needle swings up,
 * the road scrolls toward you, then the wordmark rises.
 */
@Composable
fun AnimatedSplash(onDone: () -> Unit) {
    val sweep = remember { Animatable(0f) }
    val needle = remember { Animatable(0f) }
    val word = remember { Animatable(0f) }
    val road by rememberInfiniteTransition(label = "road").animateFloat(
        0f, 1f, infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Restart), label = "dash",
    )
    LaunchedEffect(Unit) {
        launch { sweep.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        launch { delay(250); needle.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
        launch { delay(700); word.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
        delay(2100)
        onDone()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF123D2C), Color(0xFF0B1E16), Color(0xFF07120D)))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(listOf(Color(0x552E8B5F), Color.Transparent), center = Offset(size.width / 2, size.height * 0.42f), radius = size.width * 0.8f),
                radius = size.width * 0.8f, center = Offset(size.width / 2, size.height * 0.42f),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Canvas(Modifier.size(220.dp)) { drawGauge(sweep.value, needle.value, road) }
            Spacer(Modifier.height(8.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(word.value).graphicsLayer { translationY = (1f - word.value) * 40f },
            ) {
                Text("Gigometer", color = Color.White, fontFamily = Grotesk, fontWeight = FontWeight.Bold, fontSize = 44.sp, letterSpacing = (-1.5).sp)
                Text("Every mile. Every dollar.", color = Mint, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
        Text(
            "Free GPS mileage log for gig drivers",
            color = Color(0xFFB9C6BE), fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 32.dp).alpha(word.value),
        )
    }
}

/** Same geometry as the launcher icon (108-unit canvas), scaled to this DrawScope. */
fun DrawScope.drawGauge(sweep: Float, needle: Float, roadPhase: Float) {
    val u = size.minDimension / 108f
    val c = Offset(54f * u, 52f * u)
    val r = 25f * u
    val arcStroke = 7f * u
    val brush = Brush.linearGradient(listOf(Mint, Color(0xFF9BD36A), Amber), start = Offset(29f * u, 70f * u), end = Offset(79f * u, 70f * u))
    // soft glow + arc
    drawArc(Mint.copy(alpha = 0.18f), 135f, 270f * sweep, false, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r), style = Stroke(arcStroke * 2.2f, cap = StrokeCap.Round))
    drawArc(brush, 135f, 270f * sweep, false, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r), style = Stroke(arcStroke, cap = StrokeCap.Round))
    // ticks
    listOf(180f, 225f, 270f, 315f, 360f).forEachIndexed { i, a ->
        val show = ((sweep * 6f) - i).coerceIn(0f, 1f)
        val rad = Math.toRadians(a.toDouble())
        drawLine(
            Color.White.copy(alpha = 0.7f * show),
            Offset(c.x + (16f * u * cos(rad)).toFloat(), c.y + (16f * u * sin(rad)).toFloat()),
            Offset(c.x + (19.5f * u * cos(rad)).toFloat(), c.y + (19.5f * u * sin(rad)).toFloat()),
            strokeWidth = 2.2f * u, cap = StrokeCap.Round,
        )
    }
    // road (vanishes into the hub)
    val road = Path().apply {
        moveTo(52f * u, 60f * u); lineTo(56f * u, 60f * u); lineTo(67f * u, 88f * u); lineTo(41f * u, 88f * u); close()
    }
    drawPath(road, Color(0xFF0A1712))
    drawLine(Color.White.copy(alpha = 0.55f), Offset(52f * u, 60f * u), Offset(41f * u, 88f * u), 1.2f * u)
    drawLine(Color.White.copy(alpha = 0.55f), Offset(56f * u, 60f * u), Offset(67f * u, 88f * u), 1.2f * u)
    for (k in 0..3) {
        val t = ((k + roadPhase) / 4f) // 0 at hub, 1 at bottom
        val y0 = 61f + t * 27f
        val len = 1.5f + t * 4f
        val w = 0.8f + t * 1.6f
        if (y0 + len <= 88f) drawLine(Amber.copy(alpha = 0.4f + 0.6f * t), Offset(54f * u, y0 * u), Offset(54f * u, (y0 + len) * u), w * u, cap = StrokeCap.Butt)
    }
    // needle: swings from 135° (rest) to 310°
    val angle = 135f + 175f * needle
    rotate(angle + 90f, pivot = c) {
        val p = Path().apply {
            moveTo(c.x - 2f * u, c.y); lineTo(c.x + 2f * u, c.y); lineTo(c.x + 0.4f * u, c.y - 19f * u); lineTo(c.x - 0.4f * u, c.y - 19f * u); close()
        }
        drawPath(p, Color.White)
    }
    drawCircle(Color.White, 4.2f * u, c)
    drawCircle(Color(0xFF0E2A20), 1.8f * u, c)
}
