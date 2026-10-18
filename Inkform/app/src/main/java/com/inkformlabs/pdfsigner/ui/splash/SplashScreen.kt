package com.inkformlabs.pdfsigner.ui.splash

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkformlabs.pdfsigner.ui.components.InkformLogo
import com.inkformlabs.pdfsigner.ui.components.InkformWordmark
import com.inkformlabs.pdfsigner.ui.theme.LocalInk
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private data class Flake(val x: Float, val speed: Float, val phase: Float, val size: Float, val spin: Float)

/**
 * Animated brand splash (about 2.8 s): drifting ink blobs, rising paper flakes, rotating guide rings,
 * the logo building itself, the wordmark dropping in letter by letter and an ink progress line.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val p = LocalInk.current
    val finish by rememberUpdatedState(onFinished)
    val tagline = remember { Animatable(0f) }
    val line = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { delay(1350); tagline.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
        launch { line.animateTo(1f, tween(2500, easing = FastOutSlowInEasing)) }
        delay(2600)
        exit.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        finish()
    }

    val infinite = rememberInfiniteTransition(label = "splash")
    val time by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "time")
    val flakes = remember {
        List(14) { i ->
            Flake(
                x = ((i * 37) % 100) / 100f,
                speed = 0.6f + (i % 4) * 0.18f,
                phase = (i * 0.137f) % 1f,
                size = 7f + (i % 3) * 4f,
                spin = if (i % 2 == 0) 1f else -1f,
            )
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .graphicsLayer { alpha = 1f - exit.value; scaleX = 1f + exit.value * 0.06f; scaleY = 1f + exit.value * 0.06f },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val t = time * 2f * PI.toFloat()
            // Soft ink blobs drifting around the logo
            drawCircle(p.accent2.copy(alpha = 0.16f), radius = w * 0.42f, center = Offset(w * (0.92f + 0.03f * sin(t)), h * (0.12f + 0.02f * sin(t * 2))))
            drawCircle(p.accent.copy(alpha = 0.18f), radius = w * 0.36f, center = Offset(w * (0.05f + 0.03f * sin(t + 1.3f)), h * (0.86f + 0.02f * sin(t))))
            drawCircle(p.hi.copy(alpha = 0.08f), radius = w * 0.22f, center = Offset(w * 0.18f, h * (0.2f + 0.02f * sin(t * 3))))
            // Rotating guide rings
            val center = Offset(w / 2f, h * 0.42f)
            rotate(time * 360f, pivot = center) {
                drawCircle(
                    p.ink.copy(alpha = 0.08f), radius = w * 0.40f, center = center,
                    style = Stroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 18f))),
                )
            }
            rotate(-time * 540f, pivot = center) {
                drawCircle(
                    p.ink.copy(alpha = 0.06f), radius = w * 0.31f, center = center,
                    style = Stroke(1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 10f))),
                )
            }
            // Paper flakes rising
            flakes.forEach { f ->
                val progress = (time * f.speed * 3f + f.phase) % 1f
                val y = h * (1.05f - progress * 1.15f)
                val x = w * f.x + 18f * sin(progress * 6f + f.phase * 10f)
                val s = f.size * density / 2.2f
                rotate(progress * 300f * f.spin, pivot = Offset(x, y)) {
                    drawRoundRect(
                        color = p.ink.copy(alpha = 0.10f * sin(progress * PI.toFloat())),
                        topLeft = Offset(x - s / 2, y - s * 0.65f), size = Size(s, s * 1.3f),
                        cornerRadius = CornerRadius(s * 0.18f),
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            InkformLogo(Modifier.size(210.dp), playIntro = true, idle = true, introMillis = 1900)
            Spacer(Modifier.height(10.dp))
            InkformWordmark(fontSize = 46.sp, delayMillis = 650)
            Spacer(Modifier.height(8.dp))
            Text(
                "Fill & sign any PDF — offline",
                style = MaterialTheme.typography.titleMedium,
                color = p.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = tagline.value
                    translationY = (1f - tagline.value) * 14f * density
                },
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Canvas(Modifier.width(120.dp).height(6.dp)) {
                val y = size.height / 2
                drawLine(p.line, Offset(0f, y), Offset(size.width, y), size.height, StrokeCap.Round)
                drawLine(p.hi, Offset(0f, y), Offset(size.width * line.value, y), size.height, StrokeCap.Round)
            }
            Text(
                "Private by design",
                style = MaterialTheme.typography.labelMedium,
                color = p.muted.copy(alpha = tagline.value),
            )
        }
    }
}
