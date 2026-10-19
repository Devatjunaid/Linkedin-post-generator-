package com.dailycash.tracker.ui.brand

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailycash.tracker.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val symbols = listOf("$", "€", "£", "₹", "¥", "₺", "₩", "₦", "฿", "₱", "₽", "₪")

/** Branded launch animation (~2.6 s), shown once after the system splash. No ads here. */
@Composable
fun AnimatedSplash(onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    val progress = remember { Animatable(0f) }
    val tagline = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch {
            delay(1_250)
            tagline.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        }
        progress.animateTo(1f, tween(2_600, easing = LinearEasing))
        finish()
    }
    val loop = rememberInfiniteTransition(label = "splash")
    val ripple by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2_400, easing = LinearEasing)), label = "ripple")
    val drift by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(9_000, easing = LinearEasing)), label = "drift")

    Box(Modifier.fillMaxSize().background(Palette.Ink), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val maxR = size.maxDimension * 0.75f
            for (k in 0 until 3) {
                val t = (ripple + k / 3f) % 1f
                drawCircle(Palette.Mint.copy(alpha = (1f - t) * 0.14f), maxR * t, center, style = Stroke(1.dp.toPx()))
            }
            val step = 28.dp.toPx()
            var y = step / 2f
            while (y < size.height) {
                var x = step / 2f
                while (x < size.width) {
                    drawCircle(Palette.Line.copy(alpha = 0.5f), 1.dp.toPx(), Offset(x, y))
                    x += step
                }
                y += step
            }
        }

        FloatingSymbols(drift)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DailyCashLogo(size = 184.dp)
            Spacer(Modifier.height(28.dp))
            Wordmark()
            Spacer(Modifier.height(10.dp))
            Text(
                "Every coin. Every country.",
                color = Palette.Muted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.graphicsLayer {
                    alpha = tagline.value
                    translationY = (1f - tagline.value) * 24f
                },
            )
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 48.dp)
                .width(140.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Palette.Line),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.value)
                    .height(4.dp)
                    .background(Palette.Mint),
            )
        }
    }
}

@Composable
private fun Wordmark() {
    val word = "Daily Cash"
    val anims = remember { word.map { Animatable(0f) } }
    LaunchedEffect(Unit) {
        delay(650)
        anims.forEachIndexed { i, a ->
            launch {
                delay(i * 55L)
                a.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
            }
        }
    }
    Row(horizontalArrangement = Arrangement.Center) {
        word.forEachIndexed { i, ch ->
            val v = anims[i].value
            Text(
                ch.toString(),
                color = if (i >= 6) Palette.Mint else Palette.Text,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                modifier = Modifier.graphicsLayer {
                    alpha = v.coerceIn(0f, 1f)
                    translationY = (1f - v) * 60f
                    rotationZ = (1f - v) * -18f
                },
            )
        }
    }
}

@Composable
private fun FloatingSymbols(drift: Float) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        symbols.forEachIndexed { i, s ->
            val lane = (i + 0.5f) / symbols.size
            val speed = 0.6f + (i % 3) * 0.25f
            val t = (drift * speed + i * 0.137f) % 1f
            Text(
                s,
                color = if (i % 2 == 0) Palette.Mint else Palette.Amber,
                fontSize = (18 + (i % 4) * 6).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    translationX = lane * wPx - 12f
                    translationY = hPx * (1.05f - t * 1.15f)
                    alpha = 0.16f * (1f - kotlin.math.abs(t - 0.5f) * 2f).coerceIn(0f, 1f)
                    rotationZ = t * 90f - 45f
                },
            )
        }
    }
}
