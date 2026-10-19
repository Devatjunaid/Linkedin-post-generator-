package com.devatjunaid.kelvin.ui.splash

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.devatjunaid.kelvin.ui.components.AnimatedLogo
import com.devatjunaid.kelvin.ui.components.Wordmark
import com.devatjunaid.kelvin.ui.components.segment
import com.devatjunaid.kelvin.ui.theme.K
import com.devatjunaid.kelvin.ui.theme.KType
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * Branded launch sequence (~2.6 s): thermal fog rolls in, frost particles fall on the cold side
 * while embers rise on the hot side, the logo assembles, the wordmark flips up letter by letter,
 * then a progress filament fills and the whole scene zooms through to the app.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(2300, easing = LinearEasing))
        delay(120)
        exit.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        onFinished()
    }
    val loop = rememberInfiniteTransition(label = "splash")
    val drift by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(4_000, easing = LinearEasing)), label = "drift")

    val p = progress.value
    val out = exit.value
    Box(
        Modifier
            .fillMaxSize()
            .background(K.InkDeep)
            .graphicsLayer {
                alpha = 1f - out
                scaleX = 1f + out * 0.35f
                scaleY = 1f + out * 0.35f
            },
    ) {
        // Thermal fog + particles.
        Canvas(Modifier.fillMaxSize()) {
            val fog = segment(p, 0f, 0.4f)
            val w = size.width
            val h = size.height
            drawCircle(Brush.radialGradient(listOf(K.IceDeep.copy(alpha = 0.45f * fog), Color.Transparent), Offset(w * 0.1f, h * 0.3f), w), w, Offset(w * 0.1f, h * 0.3f))
            drawCircle(Brush.radialGradient(listOf(K.EmberDeep.copy(alpha = 0.35f * fog), Color.Transparent), Offset(w * 0.95f, h * 0.7f), w), w, Offset(w * 0.95f, h * 0.7f))
            repeat(26) { i ->
                val seed = (i * 0.618f) % 1f
                val local = (drift + seed) % 1f
                if (i % 2 == 0) {
                    // frost falling on the left
                    val x = w * (0.04f + 0.42f * ((i * 0.37f) % 1f)) + sin(local * 6f + i) * 12f
                    val y = h * local
                    drawCircle(K.IceLight, 1.6f + (i % 3), Offset(x, y), alpha = 0.55f * fog * (1f - local * 0.5f))
                } else {
                    // embers rising on the right
                    val x = w * (0.54f + 0.42f * ((i * 0.37f) % 1f)) + sin(local * 7f + i) * 12f
                    val y = h * (1f - local)
                    drawCircle(if (i % 3 == 0) K.Gold else K.Ember, 1.4f + (i % 3), Offset(x, y), alpha = 0.7f * fog * (1f - local))
                }
            }
            // expanding shock-ring when the logo locks in
            val ring = segment(p, 0.55f, 0.95f)
            if (ring in 0.01f..0.99f) {
                drawCircle(
                    Brush.sweepGradient(K.thermalSweep, Offset(w / 2f, h * 0.42f)),
                    radius = w * (0.2f + ring * 0.6f),
                    center = Offset(w / 2f, h * 0.42f),
                    alpha = 0.5f * (1f - ring),
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
        }

        Column(
            Modifier.align(Alignment.Center).padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedLogo(size = 210.dp, intro = true, introDurationMs = 1500)
            Spacer(Modifier.height(18.dp))
            Wordmark(progress = segment(p, 0.35f, 0.9f))
            Spacer(Modifier.height(10.dp))
            val tag = segment(p, 0.65f, 1f)
            Text(
                "COLD  ·  HEAT  ·  CONTRAST",
                style = KType.Label.copy(color = K.Muted, letterSpacing = KType.Label.letterSpacing * 1.6f),
                modifier = Modifier.graphicsLayer { alpha = tag; translationY = (1f - tag) * 20f },
            )
        }

        // Progress filament.
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .width(140.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x1AFFFFFF)),
            ) {
                Box(
                    Modifier
                        .width(140.dp * segment(p, 0.1f, 1f))
                        .height(3.dp)
                        .background(Brush.horizontalGradient(listOf(K.Ice, K.Violet, K.Ember))),
                )
            }
        }
    }
}
