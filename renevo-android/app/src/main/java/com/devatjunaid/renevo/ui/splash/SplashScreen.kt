package com.devatjunaid.renevo.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devatjunaid.renevo.ui.components.RenevoLogo
import com.devatjunaid.renevo.ui.theme.Brand
import com.devatjunaid.renevo.ui.theme.DmSans
import com.devatjunaid.renevo.ui.theme.SpaceGrotesk
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * In-app splash that continues the system splash animation while Google's consent
 * check runs. Leaves after the intro (~1.8 s) once [ready] is true, or after a hard
 * timeout so a slow network never blocks the app.
 */
@Composable
fun SplashScreen(ready: Boolean, onFinished: () -> Unit) {
    var introDone by remember { mutableStateOf(false) }
    var timedOut by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1800)
        introDone = true
        delay(4200)
        timedOut = true
    }
    var finished by remember { mutableStateOf(false) }
    LaunchedEffect(introDone, ready, timedOut) {
        if (!finished && introDone && (ready || timedOut)) {
            finished = true
            onFinished()
        }
    }

    val drift = rememberInfiniteTransition(label = "drift")
    val t by drift.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "t")
    val pulse by drift.animateFloat(0.85f, 1.15f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "pulse")

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Brand.Navy, Color(0xFF151A45), Brand.NavyLight))),
    ) {
        // Background glow orbs.
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(listOf(Brand.Violet.copy(alpha = 0.45f), Color.Transparent), center = Offset(size.width * 0.85f, size.height * 0.12f), radius = size.width * 0.7f),
                radius = size.width * 0.7f, center = Offset(size.width * 0.85f, size.height * 0.12f),
            )
            drawCircle(
                Brush.radialGradient(listOf(Brand.Blue.copy(alpha = 0.35f), Color.Transparent), center = Offset(size.width * 0.1f, size.height * 0.9f), radius = size.width * 0.8f),
                radius = size.width * 0.8f, center = Offset(size.width * 0.1f, size.height * 0.9f),
            )
            // Floating "subscription chips".
            val chips = listOf(
                Triple(0.12f, 0.20f, Brand.Orange), Triple(0.78f, 0.30f, Brand.Blue),
                Triple(0.20f, 0.70f, Brand.Violet), Triple(0.80f, 0.74f, Brand.Gold),
                Triple(0.55f, 0.12f, Color(0xFF12938A)),
            )
            chips.forEachIndexed { i, (x, y, color) ->
                val phase = (t + i * 0.2f) * 2f * Math.PI.toFloat()
                val cx = size.width * x + sin(phase) * 10.dp.toPx()
                val cy = size.height * y + sin(phase * 0.5f + i) * 14.dp.toPx()
                val w = 74.dp.toPx()
                val h = 28.dp.toPx()
                drawRoundRect(Color.White.copy(alpha = 0.07f), Offset(cx - w / 2, cy - h / 2), Size(w, h), CornerRadius(h / 2))
                drawCircle(color.copy(alpha = 0.9f), radius = 7.dp.toPx(), center = Offset(cx - w / 2 + h / 2, cy))
                drawRoundRect(Color.White.copy(alpha = 0.18f), Offset(cx - w / 2 + h, cy - 3.dp.toPx()), Size(w - h * 1.5f, 6.dp.toPx()), CornerRadius(3.dp.toPx()))
            }
        }

        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(190.dp)
                        .graphicsLayer { scaleX = pulse; scaleY = pulse; alpha = 0.5f }
                        .clip(RoundedCornerShape(80.dp))
                        .background(Brush.radialGradient(listOf(Brand.Blue.copy(alpha = 0.45f), Color.Transparent))),
                )
                RenevoLogo(size = 120.dp)
            }
            Spacer(Modifier.height(28.dp))
            Wordmark()
            Spacer(Modifier.height(8.dp))
            val tagAlpha = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                delay(900)
                tagAlpha.animateTo(1f, tween(600))
            }
            Text(
                "Subscriptions, under control.",
                modifier = Modifier.alpha(tagAlpha.value),
                color = Color(0xFFC9CCF2),
                fontFamily = DmSans,
                fontSize = 16.sp,
            )
        }

        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LoadingDots()
            Text("Private by design · No bank link", color = Color(0xFF9FA6D9), fontFamily = DmSans, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Wordmark() {
    val letters = "Renevo"
    Row {
        letters.forEachIndexed { i, ch ->
            val anim = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                delay(450L + i * 70L)
                anim.animateTo(1f, tween(420))
            }
            Text(
                ch.toString(),
                modifier = Modifier.graphicsLayer {
                    alpha = anim.value
                    translationY = (1f - anim.value) * 24.dp.toPx()
                },
                color = Color.White,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                letterSpacing = (-1).sp,
            )
        }
    }
}

@Composable
private fun LoadingDots() {
    val tr = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { i ->
            val a by tr.animateFloat(
                0.25f, 1f,
                infiniteRepeatable(tween(500, delayMillis = i * 160), RepeatMode.Reverse),
                label = "dot$i",
            )
            Box(
                Modifier
                    .size(8.dp)
                    .alpha(a)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White),
            )
        }
        Spacer(Modifier.width(0.dp))
    }
}
