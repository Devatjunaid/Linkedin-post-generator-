package com.glowdash.warningidentifier.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.glowdash.warningidentifier.data.LightsCatalog
import com.glowdash.warningidentifier.ui.components.LightIcon
import com.glowdash.warningidentifier.ui.components.PathIcon
import com.glowdash.warningidentifier.ui.components.ScreenBackground
import com.glowdash.warningidentifier.ui.components.UiIcons
import com.glowdash.warningidentifier.ui.theme.WiColors
import com.glowdash.warningidentifier.ui.theme.WiType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SPLASH_MS = 2400L

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)

    val logoScale = remember { Animatable(0.55f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleShift = remember { Animatable(24f) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { logoScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 180f)) }
        launch {
            delay(350)
            launch { titleAlpha.animateTo(1f, tween(600)) }
            titleShift.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
        }
        launch { progress.animateTo(1f, tween(SPLASH_MS.toInt() - 200, easing = FastOutSlowInEasing)) }
        delay(SPLASH_MS)
        finish()
    }

    val t = rememberInfiniteTransition(label = "splash")
    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "rot")
    val pulse by t.animateFloat(0.94f, 1.06f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "pulse")
    val ripple by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "ripple")
    val blinkIndex by t.animateFloat(0f, 5f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "blink")

    ScreenBackground(topGlow = WiColors.Violet, bottomGlow = WiColors.Pink) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .size(230.dp)
                    .graphicsLayer { scaleX = logoScale.value; scaleY = logoScale.value },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    for (i in 0..1) {
                        val p = (ripple + i * 0.5f) % 1f
                        drawCircle(
                            color = (if (i == 0) WiColors.Cyan else WiColors.Pink).copy(alpha = (1f - p) * 0.5f),
                            radius = r * (0.45f + 0.5f * p),
                            style = Stroke(2.dp.toPx()),
                        )
                    }
                    rotate(rot) {
                        drawCircle(
                            WiColors.StrokeStrong, radius = r - 4.dp.toPx(),
                            style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 14f))),
                        )
                        drawArc(
                            WiColors.Cyan, -90f, 110f, false,
                            topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                            size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
                            style = Stroke(4.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                    rotate(-rot * 1.4f) {
                        val inset = 22.dp.toPx()
                        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                        drawArc(WiColors.Pink, 90f, 70f, false, Offset(inset, inset), arcSize, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                        drawArc(WiColors.Amber, 210f, 45f, false, Offset(inset, inset), arcSize, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
                Box(
                    Modifier
                        .size(124.dp)
                        .graphicsLayer { scaleX = pulse; scaleY = pulse }
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFF3FB7D6), Color(0xFF2F55C9), Color(0xFF4A2A93)))),
                    contentAlignment = Alignment.Center,
                ) {
                    PathIcon(UiIcons.Alert, Color.White, size = 58.dp, strokeWidth = 1.8f)
                }
            }

            Spacer(Modifier.height(28.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha.value
                    translationY = titleShift.value * density
                },
            ) {
                Text(
                    buildAnnotatedString {
                        append("Warning ")
                        withStyle(SpanStyle(color = WiColors.Cyan)) { append("Identifier") }
                    },
                    style = WiType.Display.copy(fontSize = WiType.Display.fontSize * 1.1f),
                )
                Spacer(Modifier.height(6.dp))
                Text("Know every dashboard light in seconds", style = WiType.Body)
            }
        }

        // Dashboard symbols lighting up one after another.
        val row = remember {
            listOf("check_engine", "oil_pressure", "battery", "tpms", "high_beam").mapNotNull { LightsCatalog.find(it) }
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                row.forEachIndexed { i, light ->
                    val lit = blinkIndex.toInt() == i
                    LightIcon(
                        light, size = 26.dp,
                        color = if (lit) light.color.color else WiColors.StrokeStrong,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .width(160.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(WiColors.Stroke),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.value)
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(listOf(WiColors.Cyan, WiColors.Pink))),
                )
            }
        }
    }
}
