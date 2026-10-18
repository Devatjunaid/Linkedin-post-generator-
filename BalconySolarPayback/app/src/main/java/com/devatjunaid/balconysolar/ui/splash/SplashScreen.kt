package com.devatjunaid.balconysolar.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devatjunaid.balconysolar.ui.graphics.AnimatedLogo
import com.devatjunaid.balconysolar.ui.graphics.drawSparkle
import com.devatjunaid.balconysolar.ui.theme.Palette
import com.devatjunaid.balconysolar.ui.theme.Type
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SPLASH_MS = 2600

/** Animated brand intro: dawn breaks over a city of balconies while the logo assembles. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    val progress = remember { Animatable(0f) }
    val logoIn = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { logoIn.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        progress.animateTo(1f, tween(SPLASH_MS, easing = LinearEasing))
        delay(120)
        finish()
    }
    val t = rememberInfiniteTransition(label = "splash")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "drift")
    val p = progress.value
    val skyTop = lerp(Palette.NightDeep, Color(0xFF1A2550), p)
    val skyBottom = lerp(Palette.Night, Color(0xFF8A4A52), p)

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(skyTop, Palette.Dusk, skyBottom)))) {
        Canvas(Modifier.fillMaxSize()) {
            drawFloatingLights(drift, p)
            drawSkyline(p)
        }
        Column(
            Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(40.dp))
            Box(Modifier.graphicsLayer {
                val s = 0.6f + 0.4f * logoIn.value
                scaleX = s; scaleY = s; alpha = logoIn.value
                rotationZ = (1f - logoIn.value) * -12f
            }) { AnimatedLogo(168.dp) }
            Spacer(Modifier.height(28.dp))
            StaggeredWord("Balcony Solar", progress = ((p - 0.18f) / 0.35f).coerceIn(0f, 1f))
            Spacer(Modifier.height(6.dp))
            Text(
                "Payback calculator",
                style = Type.bodyMedium.copy(letterSpacing = 3.sp),
                color = Palette.AmberSoft,
                modifier = Modifier.graphicsLayer { alpha = ((p - 0.45f) / 0.2f).coerceIn(0f, 1f) },
            )
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 40.dp, start = 48.dp, end = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.12f))) {
                Box(Modifier.fillMaxWidth(p).height(4.dp).background(Brush.horizontalGradient(listOf(Palette.Mint, Palette.Amber))))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Sun-position maths · built-in climate tables",
                style = Type.mono, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StaggeredWord(text: String, progress: Float) {
    Row {
        text.forEachIndexed { i, ch ->
            val local = ((progress * (text.length + 4) - i) / 4f).coerceIn(0f, 1f)
            Text(
                ch.toString(),
                style = Type.display.copy(fontSize = 38.sp, lineHeight = 42.sp),
                color = if (i < 7) Palette.Ink else Palette.Amber,
                modifier = Modifier.graphicsLayer { alpha = local; translationY = (1f - local) * 24f },
            )
            if (ch == ' ') Spacer(Modifier.width(2.dp))
        }
    }
}

private fun DrawScope.drawFloatingLights(drift: Float, p: Float) {
    val w = size.width
    val h = size.height
    for (i in 0 until 18) {
        val seed = i * 37.13f
        val x = (seed * 13f % w + drift * w * (0.05f + (i % 5) * 0.02f)) % w
        val y = h * (0.08f + ((seed * 7f) % 55f) / 100f) + sin((drift * 6.28f) + i) * 6f
        val a = (0.25f + (i % 4) * 0.12f) * (1f - p * 0.7f)
        if (i % 6 == 0) drawSparkle(Offset(x, y), 6f, Color.White.copy(alpha = a))
        else drawCircle(Color.White.copy(alpha = a), 1.4f + (i % 3) * 0.6f, Offset(x, y))
    }
}

/** City silhouette; balconies light up and panels catch the sunrise as [p] goes 0→1. */
private fun DrawScope.drawSkyline(p: Float) {
    val w = size.width
    val h = size.height
    val base = h
    val buildings = listOf(
        0.00f to 0.22f, 0.10f to 0.30f, 0.21f to 0.18f, 0.30f to 0.34f, 0.43f to 0.24f,
        0.52f to 0.28f, 0.64f to 0.20f, 0.73f to 0.33f, 0.86f to 0.25f, 0.95f to 0.21f,
    )
    val widths = listOf(0.11f, 0.12f, 0.10f, 0.14f, 0.10f, 0.13f, 0.10f, 0.14f, 0.10f, 0.08f)
    val buildingColor = lerp(Color(0xFF060C14), Color(0xFF151C33), p)
    for ((i, b) in buildings.withIndex()) {
        val left = b.first * w
        val bw = widths[i] * w
        val top = base - b.second * h
        drawRect(buildingColor, Offset(left, top), Size(bw, base - top))
        // Balcony slabs every floor; some carry a small panel that glints.
        var y = top + 26f
        var floor = 0
        while (y < base - 16f) {
            drawRect(Color.White.copy(alpha = 0.06f), Offset(left + 4f, y), Size(bw - 8f, 3f))
            val lit = ((i * 7 + floor * 3) % 10) / 10f < p
            if (lit) drawRect(Palette.AmberSoft.copy(alpha = 0.55f), Offset(left + bw * 0.2f, y - 12f), Size(bw * 0.18f, 9f))
            if ((i + floor) % 3 == 0) {
                val glint = (p * 1.4f - (floor * 0.08f)).coerceIn(0f, 1f)
                drawRect(lerp(Palette.Cell, Color(0xFF6FA8F0), glint), Offset(left + bw * 0.55f, y - 9f), Size(bw * 0.3f, 6f))
            }
            y += 30f
            floor++
        }
    }
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Palette.NightDeep.copy(alpha = 0.6f)), base - h * 0.12f, base), Offset(0f, base - h * 0.12f), Size(w, h * 0.12f))
}
