package com.superdevlabs.autosubtitles.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.superdevlabs.autosubtitles.ui.theme.AppColors
import kotlin.math.sin

/**
 * Exit confirmation shown when Back is pressed on Home. The card springs up, the live mark
 * keeps "listening", and a row of subtitle bars waves goodbye. Deliberately ad-free:
 * AdMob does not allow ads on app-exit prompts.
 */
@Composable
fun ExitDialog(onStay: () -> Unit, onExit: () -> Unit) {
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enter.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow))
    }
    val wave = rememberInfiniteTransition(label = "wave")
    val time by wave.animateFloat(0f, 12f, infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Restart), label = "time")

    Dialog(onDismissRequest = onStay, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val e = enter.value
                    alpha = e.coerceIn(0f, 1f)
                    translationY = (1f - e) * 120.dp.toPx()
                    scaleX = 0.85f + 0.15f * e
                    scaleY = 0.85f + 0.15f * e
                    rotationZ = (1f - e) * -4f
                }
                .clip(RoundedCornerShape(30.dp))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Line, RoundedCornerShape(30.dp))
                .padding(24.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                LiveBrandMark(size = 132.dp)
                Spacer(Modifier.height(4.dp))
                // Waving subtitle bars.
                Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                    val bars = 5
                    val gap = 8.dp.toPx()
                    val barH = 8.dp.toPx()
                    val widths = floatArrayOf(46f, 70f, 34f, 58f, 40f).map { it.dp.toPx() }
                    val total = widths.sum() + gap * (bars - 1)
                    var x = (size.width - total) / 2f
                    for (i in 0 until bars) {
                        val y = size.height / 2f - barH / 2f + sin(time * 5f + i * 0.9f) * 5.dp.toPx()
                        drawRoundRect(
                            color = if (i == 1) AppColors.Lime else AppColors.Text.copy(alpha = 0.25f),
                            topLeft = Offset(x, y),
                            size = Size(widths[i], barH),
                            cornerRadius = CornerRadius(barH / 2f),
                        )
                        x += widths[i] + gap
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Leaving already?", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Your projects and styles are saved on this phone. Pick up right where you left off.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.Muted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("Exit", onExit, Modifier.weight(1f), primary = false)
                    PillButton("Stay", onStay, Modifier.weight(1f))
                }
            }
        }
    }
}

