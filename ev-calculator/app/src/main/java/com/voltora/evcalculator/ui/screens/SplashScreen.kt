package com.voltora.evcalculator.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltora.evcalculator.ui.components.AnimatedLogo
import com.voltora.evcalculator.ui.theme.Body
import com.voltora.evcalculator.ui.theme.Display
import com.voltora.evcalculator.ui.theme.Ev
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Branded splash: speed-lines sweep across, the logo builds itself, the wordmark is revealed
 * letter by letter and a segmented battery bar charges up before the app opens.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    val lines = remember { Animatable(0f) }
    val word = remember { Animatable(0f) }
    val tag = remember { Animatable(0f) }
    val battery = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { lines.animateTo(1f, tween(1800, easing = LinearEasing)) }
        delay(1100)
        launch { word.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        launch { battery.animateTo(1f, tween(1500, easing = FastOutSlowInEasing)) }
        delay(600)
        tag.animateTo(1f, tween(400))
        delay(700)
        finish()
    }

    Box(Modifier.fillMaxSize().background(Ev.Ink)) {
        SpeedLines(lines.value)

        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedLogo(size = 170.dp, intro = true)
            Spacer(Modifier.height(18.dp))
            RevealWord("EV", word.value, 64, Ev.Volt)
            RevealWord("CALCULATOR", word.value, 34, Ev.Paper, letterSpacing = 6f)
            Spacer(Modifier.height(10.dp))
            Text(
                "Charge smarter. Drive further.",
                color = Ev.MutedOnDark,
                fontFamily = Body,
                fontSize = 15.sp,
                modifier = Modifier.graphicsLayer { alpha = tag.value; translationY = (1f - tag.value) * 20f },
            )
        }

        BatteryBar(
            progress = battery.value,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 48.dp),
        )
    }
}

@Composable
private fun RevealWord(text: String, progress: Float, sizeSp: Int, color: Color, letterSpacing: Float = 0f) {
    Row {
        text.forEachIndexed { i, ch ->
            val p = (progress * (text.length + 2) - i).coerceIn(0f, 1f)
            Text(
                ch.toString(),
                color = color,
                fontFamily = Display,
                fontWeight = FontWeight.ExtraBold,
                fontSize = sizeSp.sp,
                lineHeight = sizeSp.sp,
                letterSpacing = letterSpacing.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = p
                    translationY = (1f - p) * 30f
                },
            )
        }
    }
}

/** Diagonal light streaks moving across the screen, like a car at speed. */
@Composable
private fun SpeedLines(progress: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val streaks = listOf(0.18f to 0.9f, 0.32f to 0.6f, 0.71f to 1.0f, 0.84f to 0.75f, 0.55f to 0.5f)
        streaks.forEachIndexed { i, (yFrac, speed) ->
            val len = size.width * (0.25f + 0.1f * (i % 3))
            val travel = size.width + len
            val x = -len + travel * ((progress * speed * 1.6f + i * 0.17f) % 1f)
            val y = size.height * yFrac
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, Ev.Volt.copy(alpha = 0.22f)),
                    startX = x, endX = x + len,
                ),
                start = Offset(x, y),
                end = Offset(x + len, y),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
}

@Composable
private fun BatteryBar(progress: Float, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.fillMaxWidth(0.42f).height(22.dp)) {
            val capW = 5.dp.toPx()
            val bodyW = size.width - capW
            val stroke = 2.dp.toPx()
            drawRoundRect(
                color = Ev.Line, topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(bodyW - stroke, size.height - stroke),
                cornerRadius = CornerRadius(6.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
            )
            drawRoundRect(
                color = Ev.Line, topLeft = Offset(bodyW + 1.dp.toPx(), size.height * 0.3f),
                size = Size(capW - 1.dp.toPx(), size.height * 0.4f), cornerRadius = CornerRadius(2.dp.toPx()),
            )
            val segments = 10
            val gap = 3.dp.toPx()
            val inner = 4.dp.toPx()
            val segW = (bodyW - inner * 2 - gap * (segments - 1)) / segments
            val filled = (progress * segments).toInt()
            for (i in 0 until segments) {
                val on = i < filled
                drawRoundRect(
                    color = if (on) Ev.Volt else Ev.Ink3,
                    topLeft = Offset(inner + i * (segW + gap), inner),
                    size = Size(segW, size.height - inner * 2),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "${(progress * 100).toInt()}%",
            color = Ev.MutedOnDark, fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 13.sp,
        )
    }
}
