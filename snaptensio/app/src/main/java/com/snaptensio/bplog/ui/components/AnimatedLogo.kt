package com.snaptensio.bplog.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.snaptensio.bplog.ui.theme.Brand

/**
 * The SnapTensio mark: a camera viewfinder framing a heart with an ECG pulse running through it.
 * Drawn on a 108-unit grid (same geometry as the adaptive launcher icon) so it matches pixel for pixel.
 *
 * @param intro play the one-off "focus + draw" intro (splash), otherwise draw fully at once.
 * @param beating keep a gentle heartbeat and travelling pulse glow going.
 * @param tile draw the gradient tile behind the mark (launcher-style) instead of a transparent background.
 */
@Composable
fun AnimatedLogo(
    size: Dp,
    modifier: Modifier = Modifier,
    intro: Boolean = true,
    beating: Boolean = true,
    tile: Boolean = true,
    lineColor: Color = Color.White,
) {
    val corners = remember { Animatable(if (intro) 1.4f else 1f) }
    val heart = remember { Animatable(if (intro) 0f else 1f) }
    val pulse = remember { Animatable(if (intro) 0f else 1f) }
    LaunchedEffect(intro) {
        if (!intro) return@LaunchedEffect
        corners.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(intro) {
        if (!intro) return@LaunchedEffect
        kotlinx.coroutines.delay(180)
        heart.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
    }
    LaunchedEffect(intro) {
        if (!intro) return@LaunchedEffect
        kotlinx.coroutines.delay(320)
        pulse.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
    }
    val loop = rememberInfiniteTransition(label = "beat")
    val beat by loop.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 1200
                1f at 0
                1.12f at 120
                1f at 260
                1.07f at 380
                1f at 560
            },
        ),
        label = "beatScale",
    )
    val glow by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "glow",
    )

    val heartPath = remember { heartPath() }
    val pulsePath = remember { pulsePath() }
    val measure = remember { PathMeasure().apply { setPath(pulsePath, false) } }

    Canvas(modifier.size(size).semantics { contentDescription = "SnapTensio logo" }) {
        val s = this.size.minDimension / 108f
        scale(s, pivot = Offset.Zero) {
            if (tile) {
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(Brand.BlueBright, Brand.BlueDeep), Offset.Zero, Offset(108f, 108f)),
                    size = Size(108f, 108f),
                    cornerRadius = CornerRadius(30f, 30f),
                )
                drawCircle(Color.White.copy(alpha = 0.07f), radius = 44f, center = Offset(54f, 54f), style = Stroke(1.5f))
            }
            scale(corners.value, pivot = Offset(54f, 54f)) { drawCorners(lineColor) }
            val hs = heart.value * (if (beating) beat else 1f)
            scale(hs, pivot = Offset(54f, 55f)) {
                translate(39f, 40f) {
                    scale(1.25f, pivot = Offset.Zero) { drawPath(heartPath, Brand.Coral) }
                }
            }
            val len = measure.length
            val drawn = Path()
            measure.getSegment(0f, len * pulse.value, drawn, true)
            drawPath(drawn, lineColor, style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            if (beating && pulse.value >= 1f) {
                // A bright dot that runs along the ECG line, like a monitor trace.
                val pos = measure.getPosition(len * glow)
                drawCircle(Color.White.copy(alpha = 0.35f), radius = 4.5f, center = pos)
                drawCircle(Color.White, radius = 2f, center = pos)
            }
        }
    }
}

private fun DrawScope.drawCorners(color: Color) {
    val p = Path().apply {
        moveTo(33f, 43f); lineTo(33f, 38f); quadraticTo(33f, 33f, 38f, 33f); lineTo(43f, 33f)
        moveTo(65f, 33f); lineTo(70f, 33f); quadraticTo(75f, 33f, 75f, 38f); lineTo(75f, 43f)
        moveTo(75f, 65f); lineTo(75f, 70f); quadraticTo(75f, 75f, 70f, 75f); lineTo(65f, 75f)
        moveTo(43f, 75f); lineTo(38f, 75f); quadraticTo(33f, 75f, 33f, 70f); lineTo(33f, 65f)
    }
    drawPath(p, color, style = Stroke(4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Material "favorite" heart on a 24-unit grid. */
fun heartPath() = Path().apply {
    moveTo(12f, 21.35f)
    lineTo(10.55f, 20.03f)
    cubicTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
    cubicTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
    cubicTo(9.24f, 3f, 10.91f, 3.81f, 12f, 5.09f)
    cubicTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
    cubicTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
    cubicTo(22f, 12.28f, 18.6f, 15.36f, 13.45f, 20.04f)
    close()
}

private fun pulsePath() = Path().apply {
    moveTo(35f, 55f); lineTo(45f, 55f); lineTo(48f, 49f); lineTo(52f, 62f)
    lineTo(56f, 45f); lineTo(59f, 55f); lineTo(73f, 55f)
}
