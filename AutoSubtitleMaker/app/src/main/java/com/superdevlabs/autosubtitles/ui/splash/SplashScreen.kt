package com.superdevlabs.autosubtitles.ui.splash

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.superdevlabs.autosubtitles.ui.components.MarkPhase
import com.superdevlabs.autosubtitles.ui.components.BrandWordmark
import com.superdevlabs.autosubtitles.ui.components.drawBrandMark
import com.superdevlabs.autosubtitles.ui.components.easeOutBack
import com.superdevlabs.autosubtitles.ui.components.easeOutCubic
import com.superdevlabs.autosubtitles.ui.components.phase
import com.superdevlabs.autosubtitles.ui.theme.AppColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TOTAL_MS = 2_700f

/**
 * Animated splash, choreographed on one 2.7 s timeline:
 *  0.00 s  shock-wave rings expand from the centre
 *  0.05 s  waveform ring ticks sweep in clockwise
 *  0.25 s  caption bubble twists and pops in
 *  0.70 s  play prism and spark appear
 *  0.90 s  two subtitle bars type out with a caret
 *  1.20 s  "subtitles" letters rise one by one
 *  1.75 s  tagline fades in while its tracking tightens
 *  ~2.3 s  floating caption chips drift up in the background the whole time
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val timeline = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        timeline.animateTo(TOTAL_MS, tween(TOTAL_MS.toInt(), easing = LinearEasing))
        onFinished()
    }
    val idle = rememberInfiniteTransition(label = "idle")
    val time by idle.animateFloat(0f, 20f, infiniteRepeatable(tween(20_000, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val t = timeline.value

    Box(Modifier.fillMaxSize().background(AppColors.Ink), contentAlignment = Alignment.Center) {
        // Background: expanding rings + drifting caption chips.
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            for (k in 0 until 3) {
                val p = phase(t, k * 180f, 1_400f + k * 180f)
                if (p > 0f && p < 1f) {
                    drawCircle(
                        color = AppColors.Lime.copy(alpha = (1f - p) * 0.35f),
                        radius = size.minDimension * (0.15f + 0.6f * easeOutCubic(p)),
                        center = c,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
            val chips = 9
            for (i in 0 until chips) {
                val seed = i * 37.7f
                val x = (sin(seed) * 0.5f + 0.5f) * size.width
                val speed = 0.04f + (i % 3) * 0.015f
                val y = size.height * (1.1f - ((time * speed + i / chips.toFloat()) % 1.2f))
                val w = (60 + (i * 23) % 70).dp.toPx()
                val h = 10.dp.toPx()
                val alpha = 0.06f + (i % 4) * 0.025f
                drawRoundRect(
                    color = if (i % 3 == 0) AppColors.Coral.copy(alpha = alpha) else AppColors.Text.copy(alpha = alpha),
                    topLeft = Offset(x - w / 2f, y),
                    size = androidx.compose.ui.geometry.Size(w, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f),
                )
            }
            // Orbiting particles around the logo.
            val orbit = size.minDimension * 0.36f
            for (i in 0 until 14) {
                val a = (i * 360f / 14f + time * 18f) * PI.toFloat() / 180f
                val r = orbit * (0.92f + 0.08f * sin(time * 2f + i))
                val appear = phase(t, 600f + i * 40f, 1_100f + i * 40f)
                drawCircle(
                    AppColors.Lime.copy(alpha = 0.25f * appear),
                    radius = 2.dp.toPx(),
                    center = Offset(c.x + cos(a) * r, c.y - 40.dp.toPx() + sin(a) * r),
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val markPhase = MarkPhase(
                ring = easeOutCubic(phase(t, 50f, 950f)),
                bubble = easeOutBack(phase(t, 250f, 800f)),
                play = phase(t, 700f, 1_000f),
                bar1 = phase(t, 900f, 1_300f),
                bar2 = phase(t, 1_150f, 1_500f),
                spark = phase(t, 700f, 1_050f),
                time = time,
            )
            Canvas(Modifier.size(210.dp)) { drawBrandMark(markPhase) }
            Spacer(Modifier.height(8.dp))
            BrandWordmark(progress = phase(t, 1_200f, 1_900f), fontSize = 52.sp)
            Spacer(Modifier.height(10.dp))
            val tag = phase(t, 1_750f, 2_300f)
            Text(
                "AUTO SUBTITLES · 100% OFFLINE",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.Muted,
                letterSpacing = (1.4f + 5f * (1f - easeOutCubic(tag))).sp,
                modifier = Modifier.graphicsLayer { alpha = tag },
            )
        }

        Text(
            "Speech recognition runs on your phone",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Muted.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .graphicsLayer { alpha = phase(t, 1_900f, 2_400f) },
        )
    }
}
