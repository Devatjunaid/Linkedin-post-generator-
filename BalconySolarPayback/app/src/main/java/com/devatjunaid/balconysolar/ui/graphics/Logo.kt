package com.devatjunaid.balconysolar.ui.graphics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devatjunaid.balconysolar.ui.theme.Palette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Balcony Solar mark: a night-to-dawn badge with a sun riding its daily arc, rotating rays,
 * two shimmering solar panels on a balcony railing, and a payback coin whose ring fills up.
 *
 * @param intro 0→1 build-in progress (sun rises, panels slide in, railing draws, coin fills).
 */
@Composable
fun AnimatedLogo(size: Dp, modifier: Modifier = Modifier, playIntro: Boolean = true, loop: Boolean = true) {
    val intro = remember { Animatable(if (playIntro) 0f else 1f) }
    LaunchedEffect(playIntro) {
        if (playIntro) intro.animateTo(1f, tween(1800, easing = FastOutSlowInEasing))
    }
    val transition = rememberInfiniteTransition(label = "logo")
    val spin by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "spin")
    val shimmer by transition.animateFloat(-0.4f, 1.4f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "shimmer")
    val pulse by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    Canvas(modifier.size(size).semantics { contentDescription = "Balcony Solar logo" }) {
        drawLogo(
            intro = intro.value,
            spin = if (loop) spin else 0f,
            shimmer = if (loop) shimmer else -1f,
            pulse = if (loop) pulse else 0.5f,
        )
    }
}

fun DrawScope.drawLogo(intro: Float, spin: Float, shimmer: Float, pulse: Float) {
    val s = size.minDimension
    val u = s / 100f
    fun stage(start: Float, end: Float) = ((intro - start) / (end - start)).coerceIn(0f, 1f)
    fun ease(x: Float) = 1 - (1 - x) * (1 - x) * (1 - x)

    val badge = Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(0f, 0f, s, s), CornerRadius(26 * u)))
    }
    clipPath(badge) {
        // 1. Sky: night at the top, dawn glow near the horizon.
        drawRect(Brush.verticalGradient(listOf(Palette.NightDeep, Palette.Dusk.copy(alpha = 0.9f), Color(0xFF7A3E4A)), startY = 0f, endY = s))
        val glowCenter = Offset(50 * u, 66 * u)
        drawCircle(
            Brush.radialGradient(listOf(Palette.Amber.copy(alpha = 0.55f * stage(0f, 0.6f)), Color.Transparent), glowCenter, 52 * u),
            radius = 52 * u, center = glowCenter,
        )
        // Stars fade out as the sun rises.
        val starAlpha = (1f - stage(0.2f, 0.8f)) * 0.9f + 0.1f
        val stars = listOf(14f to 14f, 30f to 9f, 82f to 12f, 90f to 30f, 8f to 34f, 64f to 8f)
        for ((i, p) in stars.withIndex()) {
            val tw = 0.6f + 0.4f * sin((pulse * 2 * PI + i).toFloat())
            drawCircle(Color.White.copy(alpha = starAlpha * tw), radius = (0.9f + (i % 2) * 0.5f) * u, center = Offset(p.first * u, p.second * u))
        }

        // 2. The sun's daily path: dashed arc from horizon to horizon.
        val arcRect = Rect(12 * u, 22 * u, 88 * u, 98 * u)
        drawArc(
            color = Palette.AmberSoft.copy(alpha = 0.55f),
            startAngle = 180f, sweepAngle = 180f * stage(0f, 0.45f), useCenter = false,
            topLeft = arcRect.topLeft, size = arcRect.size,
            style = Stroke(1.6f * u, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3 * u, 3.2f * u))),
        )
        // 3. Sun rises along the arc to the upper right, with rotating rays.
        val angle = PI * (1.0 - 0.72 * ease(stage(0.05f, 0.7f)))
        val sun = Offset(
            (arcRect.center.x + arcRect.width / 2 * cos(angle)).toFloat(),
            (arcRect.center.y - arcRect.height / 2 * sin(angle)).toFloat(),
        )
        val sunR = 9.5f * u
        drawCircle(Palette.Amber.copy(alpha = 0.22f + 0.12f * pulse), radius = sunR * 2.0f, center = sun)
        rotate(spin, pivot = sun) {
            for (i in 0 until 12) {
                val a = i * 30.0 * PI / 180
                val r0 = sunR * 1.35f
                val r1 = sunR * (if (i % 2 == 0) 1.95f else 1.65f)
                drawLine(
                    Palette.AmberSoft, Offset(sun.x + r0 * cos(a).toFloat(), sun.y + r0 * sin(a).toFloat()),
                    Offset(sun.x + r1 * cos(a).toFloat(), sun.y + r1 * sin(a).toFloat()),
                    strokeWidth = 2.2f * u, cap = StrokeCap.Round,
                )
            }
        }
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE2A6), Palette.Amber), sun, sunR), radius = sunR, center = sun)

        // 4. Balcony railing drawn left to right.
        val railY = 76 * u
        val railP = ease(stage(0.35f, 0.85f))
        val railEnd = 10 * u + 80 * u * railP
        drawLine(Palette.Mint, Offset(10 * u, railY), Offset(railEnd, railY), strokeWidth = 3 * u, cap = StrokeCap.Round)
        var x = 16 * u
        while (x < railEnd - 2 * u) {
            drawLine(Palette.Mint.copy(alpha = 0.75f), Offset(x, railY), Offset(x, 94 * u), strokeWidth = 1.8f * u, cap = StrokeCap.Round)
            x += 9 * u
        }
        drawRect(Palette.NightDeep.copy(alpha = 0.55f), topLeft = Offset(0f, 94 * u), size = Size(s, 6 * u))

        // 5. Two tilted panels slide up onto the railing, then shimmer.
        val panelLift = (1f - ease(stage(0.45f, 0.95f))) * 30 * u
        for (k in 0 until 2) {
            val left = (18 + k * 33) * u
            drawPanel(left, railY - 2 * u + panelLift, 29 * u, 22 * u, u, shimmer - k * 0.25f)
        }

        // 6. Payback coin with a ring that fills.
        val coinC = Offset(80 * u, 82 * u)
        val coinR = 8.5f * u
        val coinIn = ease(stage(0.6f, 1f))
        if (coinIn > 0f) {
            drawCircle(Palette.NightDeep, radius = (coinR + 2.6f * u) * coinIn, center = coinC)
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD98F), Palette.Amber), coinC, coinR), radius = coinR * coinIn, center = coinC)
            drawArc(
                Palette.Mint, -90f, 360f * coinIn, false,
                topLeft = Offset(coinC.x - coinR - 1.4f * u, coinC.y - coinR - 1.4f * u),
                size = Size((coinR + 1.4f * u) * 2, (coinR + 1.4f * u) * 2),
                style = Stroke(1.8f * u, cap = StrokeCap.Round),
            )
            // Upward arrow: the investment coming back.
            val a = Path().apply {
                moveTo(coinC.x, coinC.y + 4.2f * u); lineTo(coinC.x, coinC.y - 4.2f * u)
                moveTo(coinC.x - 3.4f * u, coinC.y - 1f * u); lineTo(coinC.x, coinC.y - 4.4f * u); lineTo(coinC.x + 3.4f * u, coinC.y - 1f * u)
            }
            drawPath(a, Palette.OnAmber, alpha = coinIn, style = Stroke(1.9f * u, cap = StrokeCap.Round))
        }

        // 7. Sparkle on the leading panel corner.
        val sp = stage(0.8f, 1f) * (0.5f + 0.5f * pulse)
        if (sp > 0f) drawSparkle(Offset(48 * u, 46 * u), 4.5f * u * sp, Color.White.copy(alpha = 0.9f * sp))
    }
    drawRoundRect(Color.White.copy(alpha = 0.08f), cornerRadius = CornerRadius(26 * u), style = Stroke(1.2f * u))
}

private fun DrawScope.drawPanel(left: Float, bottom: Float, w: Float, h: Float, u: Float, shimmer: Float) {
    // A panel leaning back, drawn as a parallelogram in light perspective.
    val skew = h * 0.42f
    val p = Path().apply {
        moveTo(left, bottom); lineTo(left + w, bottom)
        lineTo(left + w + skew * 0.35f, bottom - h); lineTo(left + skew * 0.35f, bottom - h); close()
    }
    drawPath(p, Brush.verticalGradient(listOf(Color(0xFF2F6FC4), Palette.Cell, Color(0xFF14325E)), startY = bottom - h, endY = bottom))
    clipPath(p) {
        val cells = Color(0xFF8BB8F5).copy(alpha = 0.45f)
        for (i in 1 until 4) {
            val t = i / 4f
            drawLine(cells, Offset(left + w * t, bottom), Offset(left + w * t + skew * 0.35f, bottom - h), strokeWidth = 0.7f * u)
        }
        for (j in 1 until 3) {
            val t = j / 3f
            drawLine(cells, Offset(left + skew * 0.35f * t, bottom - h * t), Offset(left + w + skew * 0.35f * t, bottom - h * t), strokeWidth = 0.7f * u)
        }
        if (shimmer in -0.3f..1.3f) {
            val cx = left + (w + skew) * shimmer
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent),
                    start = Offset(cx - 8 * u, bottom), end = Offset(cx + 8 * u, bottom - h),
                ),
                topLeft = Offset(left - skew, bottom - h), size = Size(w + 3 * skew, h),
            )
        }
    }
    drawPath(p, Color(0xFFA9CBFF).copy(alpha = 0.8f), style = Stroke(0.9f * u))
}

fun DrawScope.drawSparkle(c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x, c.y, c.x + r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y + r)
        quadraticTo(c.x, c.y, c.x - r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    drawPath(p, color)
}
