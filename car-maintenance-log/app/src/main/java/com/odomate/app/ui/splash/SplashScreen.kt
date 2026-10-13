package com.odomate.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odomate.app.BuildConfig
import com.odomate.app.ui.theme.Archivo
import com.odomate.app.ui.theme.Mono
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.cos
import kotlin.math.sin

private val Ink = Color(0xFF0E1113)
private val Amber = Color(0xFFF5A524)
private val Teal = Color(0xFF46C7A8)
private val Coral = Color(0xFFFF6B57)
private val Paper = Color(0xFFEEF1F3)
private val Muted = Color(0xFF9AA5AE)
private val Lcd = Color(0xFF1B2A22)
private val LcdText = Color(0xFF9BE3B9)

/**
 * Animated instrument-cluster intro: scan brackets lock on, the tri-colour gauge sweeps,
 * the wrench needle swings into place and the odometer rolls. Waits for [ready] (consent,
 * database) but never longer than ~6 s.
 */
@Composable
fun SplashScreen(ready: Boolean, onFinished: () -> Unit) {
    val brackets = remember { Animatable(0f) }
    val sweep = remember { Animatable(0f) }
    val needle = remember { Animatable(0f) }
    val counter = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val tagline = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }
    val readyState by rememberUpdatedState(ready)
    val finish by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { progress.animateTo(0.85f, tween(2200, easing = LinearEasing)) }
            listOf(
                async { brackets.animateTo(1f, tween(550, easing = FastOutSlowInEasing)) },
                async { delay(250); sweep.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) },
                async { delay(600); needle.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) },
                async { delay(450); counter.animateTo(1f, tween(1500, easing = FastOutSlowInEasing)) },
                async { delay(900); title.animateTo(1f, tween(500)) },
                async { delay(1250); tagline.animateTo(1f, tween(500)) },
            ).awaitAll()
            withTimeoutOrNull(4_000) { snapshotFlow { readyState }.first { it } }
            progress.animateTo(1f, tween(250))
            delay(120)
            finish()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .background(Brush.radialGradient(listOf(Amber.copy(alpha = 0.10f), Color.Transparent), radius = 900f)),
    ) {
        Column(
            Modifier.align(Alignment.Center).padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Canvas(Modifier.size(230.dp)) {
                drawBrackets(brackets.value)
                drawGauge(sweep.value, needle.value)
            }
            Spacer(Modifier.height(4.dp))
            RollingOdometer(counter.value)
            Spacer(Modifier.height(28.dp))
            Text(
                "OdoMate",
                fontFamily = Archivo, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, color = Paper, letterSpacing = (-0.5).sp,
                modifier = Modifier.graphicsLayer { alpha = title.value; translationY = (1f - title.value) * 24f },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Snap the odometer. Never miss a service.",
                fontFamily = Archivo, fontSize = 15.sp, color = Muted,
                modifier = Modifier.alpha(tagline.value),
            )
        }

        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 36.dp).width(180.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Color(0xFF242B30))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(progress.value).clip(CircleShape).background(Amber))
            }
            Text(
                if (ready) "Starting engine…" else "Preparing your garage…",
                fontFamily = Archivo, fontSize = 12.sp, color = Muted,
            )
            Text("v${BuildConfig.VERSION_NAME}", fontFamily = Mono, fontSize = 10.sp, color = Color(0xFF5C6770))
        }
    }
}

@Composable
private fun RollingOdometer(t: Float) {
    val target = 128_640
    val value = (target * t).toInt()
    val digits = value.toString().padStart(6, '0')
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(Lcd).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text("ODO", fontFamily = Mono, fontSize = 10.sp, color = LcdText.copy(alpha = 0.7f))
        Spacer(Modifier.width(6.dp))
        digits.forEach { ch ->
            Text(ch.toString(), fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = LcdText)
        }
    }
}

private fun DrawScope.drawBrackets(t: Float) {
    val s = size.minDimension
    val inset = s * 0.08f + (1f - t) * s * 0.08f
    val len = s * 0.14f
    val w = s * 0.028f
    val color = Amber.copy(alpha = t)
    val l = inset
    val r = s - inset
    fun corner(x: Float, y: Float, dx: Float, dy: Float) {
        drawLine(color, Offset(x, y), Offset(x + dx * len, y), w, StrokeCap.Round)
        drawLine(color, Offset(x, y), Offset(x, y + dy * len), w, StrokeCap.Round)
    }
    corner(l, l, 1f, 1f)
    corner(r, l, -1f, 1f)
    corner(l, r, 1f, -1f)
    corner(r, r, -1f, -1f)
    // Scan line
    drawLine(Amber.copy(alpha = 0.35f * t), Offset(s * 0.2f, s * 0.53f), Offset(s * 0.8f, s * 0.53f), s * 0.008f, StrokeCap.Round)
}

private fun DrawScope.drawGauge(sweep: Float, needle: Float) {
    val s = size.minDimension
    val center = Offset(s / 2f, s * 0.53f)
    val radius = s * 0.29f
    val stroke = s * 0.045f
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = Size(radius * 2, radius * 2)
    val colors = listOf(Teal, Amber, Coral)
    val total = 270f * sweep
    colors.forEachIndexed { i, color ->
        val segStart = i * 90f
        val visible = (total - segStart).coerceIn(0f, 86f)
        if (visible > 0f) {
            drawArc(color, 135f + segStart + 2f, visible, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }

    // Wrench-shaped needle swinging from the start of the scale to the amber zone.
    val angle = 135f + 165f * needle
    if (needle > 0.01f) {
        rotate(angle, pivot = center) {
            val shaftEnd = Offset(center.x + radius * 0.5f, center.y)
            drawLine(Paper, center, shaftEnd, s * 0.026f, StrokeCap.Round)
            val headCenter = Offset(center.x + radius * 0.68f, center.y)
            val headR = radius * 0.2f
            drawArc(
                Paper, 45f, 270f, false,
                Offset(headCenter.x - headR, headCenter.y - headR), Size(headR * 2, headR * 2),
                style = Stroke(s * 0.024f, cap = StrokeCap.Round),
            )
        }
    }
    drawCircle(Paper, s * 0.03f, center)
    drawCircle(Ink, s * 0.012f, center)

    // Tick marks
    for (i in 0..9) {
        val a = Math.toRadians((135.0 + i * 30.0))
        val inner = radius - stroke * 1.6f
        val outer = radius - stroke * 1.0f
        drawLine(
            Muted.copy(alpha = 0.5f * sweep),
            Offset(center.x + (inner * cos(a)).toFloat(), center.y + (inner * sin(a)).toFloat()),
            Offset(center.x + (outer * cos(a)).toFloat(), center.y + (outer * sin(a)).toFloat()),
            s * 0.006f,
        )
    }
}
