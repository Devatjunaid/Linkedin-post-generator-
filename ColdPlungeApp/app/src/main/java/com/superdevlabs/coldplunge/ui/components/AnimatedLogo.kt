package com.superdevlabs.coldplunge.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.superdevlabs.coldplunge.ui.theme.K
import com.superdevlabs.coldplunge.ui.theme.Unbounded
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The app mark, drawn live:
 *  1. a rotating thermal ring (ice → violet → ember sweep) that draws itself in,
 *  2. a counter-rotating tick dial,
 *  3. an ice / ember split core (a thermal "yin-yang"),
 *  4. a snowflake on the cold half and a flickering flame on the hot half,
 *  5. orbiting cold and hot particles,
 *  6. steam wisps rising on the hot side and water ripples on the cold side,
 *  7. twinkling sparkles.
 * With [intro] the parts assemble in sequence; afterwards everything keeps moving subtly.
 */
@Composable
fun AnimatedLogo(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    intro: Boolean = true,
    introDurationMs: Int = 1600,
) {
    val introProgress = remember { Animatable(if (intro) 0f else 1f) }
    LaunchedEffect(intro) {
        if (intro) introProgress.animateTo(1f, tween(introDurationMs, easing = LinearEasing))
    }
    val loop = rememberInfiniteTransition(label = "logo")
    val spin by loop.animateFloat(0f, 360f, infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "spin")
    val orbit by loop.animateFloat(0f, 360f, infiniteRepeatable(tween(6_500, easing = LinearEasing)), label = "orbit")
    val flicker by loop.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flicker")
    val drift by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2_800, easing = LinearEasing)), label = "drift")
    val wave by loop.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2_200, easing = LinearEasing)), label = "wave")
    val twinkle by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(1_600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "twinkle")

    Canvas(
        modifier
            .size(size)
            .semantics { contentDescription = "Cold Plunge & Sauna Timer logo" },
    ) {
        drawLogoMark(
            t = introProgress.value,
            spin = spin,
            orbit = orbit,
            flicker = flicker,
            drift = drift,
            wave = wave,
            twinkle = twinkle,
        )
    }
}

/** Static-looking small mark for headers (still subtly alive). */
@Composable
fun LogoMark(modifier: Modifier = Modifier, size: Dp = 40.dp) = AnimatedLogo(modifier, size = size, intro = false)

/** Wordmark with a thermal gradient; letters rise in one by one when [progress] goes 0 → 1. */
@Composable
fun Wordmark(
    progress: Float,
    modifier: Modifier = Modifier,
    text: String = "COLD PLUNGE",
    fontSize: TextUnit = 28.sp,
) {
    val letters = text
    val brush = Brush.horizontalGradient(listOf(K.IceLight, K.Ice, K.Text, K.Ember, K.Gold))
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        letters.forEachIndexed { i, ch ->
            val step = 0.5f / letters.length.coerceAtLeast(1)
            val local = segment(progress, i * step, i * step + 0.5f)
            Text(
                ch.toString(),
                style = TextStyle(
                    brush = brush,
                    fontFamily = Unbounded,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = fontSize,
                    letterSpacing = 2.sp,
                ),
                modifier = Modifier.graphicsLayer {
                    alpha = local
                    translationY = (1f - local) * 40f
                    rotationX = (1f - local) * 70f
                },
            )
        }
    }
}

internal fun segment(t: Float, start: Float, end: Float): Float {
    val x = ((t - start) / (end - start)).coerceIn(0f, 1f)
    return FastOutSlowInEasing.transform(x)
}

private fun backOut(x: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val p = x - 1f
    return 1f + c3 * p * p * p + c1 * p * p
}

/** Path of the cold half of the split core (left side plus the upper bulge). */
internal fun iceHalfPath(c: Offset, r: Float): Path = Path().apply {
    moveTo(c.x, c.y - r)
    arcTo(Rect(c, r), -90f, -180f, false) // left side, top → bottom
    arcTo(Rect(Offset(c.x, c.y + r / 2f), r / 2f), 90f, 180f, false) // lower small circle, via its left side
    arcTo(Rect(Offset(c.x, c.y - r / 2f), r / 2f), 90f, -180f, false) // upper small circle, via its right side
    close()
}

internal fun flamePath(cx: Float, cy: Float, h: Float): Path = Path().apply {
    moveTo(cx, cy - h * 0.55f)
    cubicTo(cx + h * 0.18f, cy - h * 0.2f, cx + h * 0.42f, cy - h * 0.05f, cx + h * 0.36f, cy + h * 0.2f)
    cubicTo(cx + h * 0.3f, cy + h * 0.45f, cx + h * 0.12f, cy + h * 0.5f, cx, cy + h * 0.5f)
    cubicTo(cx - h * 0.12f, cy + h * 0.5f, cx - h * 0.3f, cy + h * 0.45f, cx - h * 0.36f, cy + h * 0.2f)
    cubicTo(cx - h * 0.42f, cy - h * 0.05f, cx - h * 0.12f, cy - h * 0.15f, cx, cy - h * 0.55f)
    close()
}

internal fun DrawScope.drawSnowflake(center: Offset, radius: Float, color: Color, strokeWidth: Float) {
    repeat(6) { i ->
        val a = (i * 60f - 90f) * (PI / 180f).toFloat()
        val dir = Offset(cos(a), sin(a))
        val tip = center + dir * radius
        drawLine(color, center, tip, strokeWidth, StrokeCap.Round)
        val branchBase = center + dir * (radius * 0.6f)
        val left = Offset(cos(a - 0.75f), sin(a - 0.75f)) * (radius * 0.32f)
        val right = Offset(cos(a + 0.75f), sin(a + 0.75f)) * (radius * 0.32f)
        drawLine(color, branchBase, branchBase + left, strokeWidth * 0.85f, StrokeCap.Round)
        drawLine(color, branchBase, branchBase + right, strokeWidth * 0.85f, StrokeCap.Round)
    }
}

internal fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    val p = Path().apply {
        moveTo(center.x, center.y - radius)
        cubicTo(center.x + radius * 0.12f, center.y - radius * 0.12f, center.x + radius * 0.12f, center.y - radius * 0.12f, center.x + radius, center.y)
        cubicTo(center.x + radius * 0.12f, center.y + radius * 0.12f, center.x + radius * 0.12f, center.y + radius * 0.12f, center.x, center.y + radius)
        cubicTo(center.x - radius * 0.12f, center.y + radius * 0.12f, center.x - radius * 0.12f, center.y + radius * 0.12f, center.x - radius, center.y)
        cubicTo(center.x - radius * 0.12f, center.y - radius * 0.12f, center.x - radius * 0.12f, center.y - radius * 0.12f, center.x, center.y - radius)
        close()
    }
    drawPath(p, color)
}

private fun DrawScope.drawLogoMark(
    t: Float,
    spin: Float,
    orbit: Float,
    flicker: Float,
    drift: Float,
    wave: Float,
    twinkle: Float,
) {
    val s = size.minDimension
    val c = center
    val ringR = s * 0.34f
    val ringW = s * 0.034f
    val coreR = s * 0.255f

    val pRing = segment(t, 0f, 0.45f)
    val pCore = backOut(((t - 0.18f) / 0.42f).coerceIn(0f, 1f)).coerceAtLeast(0f)
    val pSymbols = segment(t, 0.45f, 0.8f)
    val pExtras = segment(t, 0.6f, 1f)

    // Split glow: cold light on the left, heat on the right.
    drawCircle(
        Brush.radialGradient(listOf(K.Ice.copy(alpha = 0.32f * pRing), Color.Transparent), center = c + Offset(-s * 0.12f, -s * 0.05f), radius = s * 0.5f),
        radius = s * 0.5f,
        center = c,
    )
    drawCircle(
        Brush.radialGradient(listOf(K.Ember.copy(alpha = 0.30f * pRing), Color.Transparent), center = c + Offset(s * 0.12f, s * 0.06f), radius = s * 0.5f),
        radius = s * 0.5f,
        center = c,
    )

    // 1. Thermal ring.
    rotate(spin, c) {
        drawArc(
            brush = Brush.sweepGradient(K.thermalSweep, c),
            startAngle = -90f,
            sweepAngle = 360f * pRing,
            useCenter = false,
            topLeft = Offset(c.x - ringR, c.y - ringR),
            size = Size(ringR * 2, ringR * 2),
            style = Stroke(width = ringW, cap = StrokeCap.Round),
        )
    }

    // 2. Tick dial.
    rotate(-spin * 0.5f, c) {
        val outer = ringR - ringW * 1.4f
        repeat(48) { i ->
            val a = (i * 7.5f) * (PI / 180f).toFloat()
            val len = if (i % 4 == 0) s * 0.03f else s * 0.014f
            val dir = Offset(cos(a), sin(a))
            drawLine(
                Color.White.copy(alpha = (if (i % 4 == 0) 0.45f else 0.22f) * pRing),
                c + dir * outer,
                c + dir * (outer - len),
                strokeWidth = s * 0.006f,
                cap = StrokeCap.Round,
            )
        }
    }

    // 3. Split core.
    if (pCore > 0f) {
        scale(pCore, pCore, c) {
            drawCircle(Brush.linearGradient(listOf(K.Gold, K.Ember, K.EmberDeep), start = c + Offset(0f, -coreR), end = c + Offset(0f, coreR)), coreR, c)
            drawPath(iceHalfPath(c, coreR), Brush.linearGradient(listOf(K.IceLight, K.Ice, K.IceDeep), start = c + Offset(-coreR, -coreR), end = c + Offset(coreR * 0.3f, coreR)))
            // soft top highlight
            drawCircle(
                Brush.radialGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent), center = c + Offset(-coreR * 0.35f, -coreR * 0.45f), radius = coreR * 0.8f),
                radius = coreR,
                center = c,
            )
            drawCircle(K.Ink.copy(alpha = 0.55f), coreR, c, style = Stroke(width = s * 0.008f))
        }
    }

    // 4. Snowflake (cold half) and flame (hot half).
    if (pSymbols > 0f) {
        val snowC = Offset(c.x, c.y - coreR / 2f)
        rotate((1f - pSymbols) * -120f + spin * 0.25f, snowC) {
            drawSnowflake(snowC, coreR * 0.3f * pSymbols, K.InkDeep.copy(alpha = 0.8f * pSymbols), s * 0.011f)
        }
        val flameC = Offset(c.x, c.y + coreR / 2f)
        scale(pSymbols, pSymbols * flicker, Offset(flameC.x, flameC.y + coreR * 0.18f)) {
            drawPath(flamePath(flameC.x, flameC.y, coreR * 0.62f), Color(0xFFFFF1D6).copy(alpha = 0.95f))
            drawPath(flamePath(flameC.x, flameC.y + coreR * 0.1f, coreR * 0.34f), K.Ember.copy(alpha = 0.9f))
        }
    }

    // 5. Orbiting particles.
    if (pExtras > 0f) {
        val orbitR = ringR + s * 0.07f
        val aIce = (orbit - 90f) * (PI / 180f).toFloat()
        val aHeat = (orbit + 90f) * (PI / 180f).toFloat()
        val iceDot = c + Offset(cos(aIce), sin(aIce)) * orbitR
        val heatDot = c + Offset(cos(aHeat), sin(aHeat)) * (orbitR - s * 0.02f)
        drawCircle(Brush.radialGradient(listOf(K.Ice.copy(alpha = 0.6f * pExtras), Color.Transparent), iceDot, s * 0.05f), s * 0.05f, iceDot)
        drawCircle(K.IceLight.copy(alpha = pExtras), s * 0.018f, iceDot)
        drawCircle(Brush.radialGradient(listOf(K.Ember.copy(alpha = 0.6f * pExtras), Color.Transparent), heatDot, s * 0.05f), s * 0.05f, heatDot)
        drawCircle(K.Gold.copy(alpha = pExtras), s * 0.015f, heatDot)

        // 6a. Steam wisps rising on the hot (right) side.
        repeat(3) { i ->
            val phase = (drift + i / 3f) % 1f
            val x = c.x + ringR * 0.62f + i * s * 0.045f
            val y0 = c.y - ringR * 0.78f
            val y = y0 - phase * s * 0.14f
            val alpha = sin(phase * PI.toFloat()) * 0.7f * pExtras
            val h = s * 0.06f
            val p = Path().apply {
                moveTo(x, y + h)
                cubicTo(x - h * 0.4f, y + h * 0.66f, x + h * 0.4f, y + h * 0.33f, x, y)
            }
            drawPath(p, Color(0xFFFFD9C4).copy(alpha = alpha), style = Stroke(width = s * 0.012f, cap = StrokeCap.Round))
        }

        // 6b. Water ripples on the cold (left) side.
        repeat(2) { row ->
            val baseY = c.y + ringR * 0.98f + row * s * 0.04f
            val startX = c.x - ringR * 1.15f + row * s * 0.03f
            val endX = c.x - ringR * 0.25f
            val p = Path()
            var x = startX
            var first = true
            while (x <= endX) {
                val y = baseY + sin((x - startX) / (s * 0.05f) + wave + row) * s * 0.012f
                if (first) {
                    p.moveTo(x, y)
                    first = false
                } else {
                    p.lineTo(x, y)
                }
                x += s * 0.01f
            }
            drawPath(p, K.Ice.copy(alpha = (0.75f - row * 0.3f) * pExtras), style = Stroke(width = s * 0.011f, cap = StrokeCap.Round))
        }

        // 7. Sparkles.
        drawSparkle(c + Offset(-ringR * 0.95f, -ringR * 0.8f), s * (0.025f + 0.015f * twinkle) * pExtras, K.IceLight.copy(alpha = 0.9f))
        drawSparkle(c + Offset(ringR * 1.02f, ringR * 0.72f), s * (0.022f + 0.014f * (1f - twinkle)) * pExtras, K.Gold.copy(alpha = 0.9f))
    }
}
