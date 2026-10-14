package com.devatjunaid.renewalert.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devatjunaid.renewalert.ui.theme.Brand
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun clamp01(v: Float) = v.coerceIn(0f, 1f)

private fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val x = t - 1f
    return 1f + c3 * x * x * x + c1 * x * x
}

private fun easeOut(t: Float) = 1f - (1f - t) * (1f - t) * (1f - t)

/**
 * The Renew Alert mark, drawn in code so it can animate at any size:
 * gradient tile pops in, the renewal ring draws itself, the coin bounces in,
 * the check writes, the spark twinkles, then a soft shine sweeps across forever.
 */
@Composable
fun RenewAlertLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    animate: Boolean = true,
    idleMotion: Boolean = true,
) {
    val intro = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(animate) {
        if (animate) intro.animateTo(1f, tween(durationMillis = 1300, easing = LinearEasing))
    }
    val loop = rememberInfiniteTransition(label = "logo-loop")
    val twinkle by loop.animateFloat(
        initialValue = 0.75f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "twinkle",
    )
    val shine by loop.animateFloat(
        initialValue = -0.6f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "shine",
    )
    val p = intro.value

    Canvas(
        modifier
            .size(size)
            .semantics { contentDescription = "Renew Alert logo" },
    ) {
        drawLogo(
            progress = p,
            twinkle = if (idleMotion) twinkle else 1f,
            shine = if (idleMotion && p >= 1f) shine else -1f,
        )
    }
}

fun DrawScope.drawLogo(progress: Float, twinkle: Float = 1f, shine: Float = -1f) {
    val w = size.minDimension
    val tileT = easeOutBack(clamp01(progress / 0.35f))
    val ringT = easeOut(clamp01((progress - 0.12f) / 0.45f))
    val coinT = easeOutBack(clamp01((progress - 0.35f) / 0.3f))
    val arrowT = easeOutBack(clamp01((progress - 0.5f) / 0.2f))
    val checkT = easeOut(clamp01((progress - 0.62f) / 0.25f))
    val sparkT = easeOutBack(clamp01((progress - 0.7f) / 0.3f))

    val center = Offset(w / 2f, w / 2f + w * 0.02f)
    val corner = w * 0.28f

    // Tile with soft glow and gloss.
    scale(tileT.coerceAtLeast(0.001f), pivot = Offset(w / 2f, w / 2f)) {
        drawRoundRect(
            brush = Brush.radialGradient(listOf(Brand.Violet.copy(alpha = 0.35f), Color.Transparent), center = Offset(w / 2, w / 2), radius = w * 0.75f),
            topLeft = Offset(-w * 0.15f, -w * 0.15f), size = Size(w * 1.3f, w * 1.3f), cornerRadius = CornerRadius(corner * 1.4f),
        )
        val tile = Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, w, w, CornerRadius(corner)))
        }
        drawPath(tile, Brush.linearGradient(listOf(Brand.Blue, Brand.Indigo, Brand.Violet), start = Offset.Zero, end = Offset(w, w)))
        clipPath(tile) {
            drawCircle(Color.White.copy(alpha = 0.08f), radius = w * 0.45f, center = Offset(w * 0.12f, w * 0.1f))
            if (shine >= -0.5f) {
                val x = shine * w
                drawRect(
                    brush = Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent),
                        start = Offset(x - w * 0.25f, 0f), end = Offset(x + w * 0.25f, w * 0.4f),
                    ),
                    size = Size(w, w),
                )
            }
        }
    }

    // Renewal ring.
    val r = w * 0.25f
    val stroke = w * 0.085f
    val start = -40f
    val sweep = 290f * ringT
    if (ringT > 0f) {
        drawArc(
            color = Color.White,
            startAngle = start,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(center.x - r, center.y - r),
            size = Size(r * 2, r * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }

    // Arrowhead at the end of the ring.
    if (arrowT > 0f) {
        val a = ((start + 290f) * PI / 180.0).toFloat()
        val end = Offset(center.x + r * cos(a), center.y + r * sin(a))
        val t = Offset(-sin(a), cos(a))
        val n = Offset(-t.y, t.x)
        val k = w / 72f
        val tip = end + t * (7.5f * k)
        val base = end - t * (1.5f * k)
        val arrow = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(base.x + n.x * 6.5f * k, base.y + n.y * 6.5f * k)
            lineTo(base.x - n.x * 6.5f * k, base.y - n.y * 6.5f * k)
            close()
        }
        scale(arrowT, pivot = end) {
            drawPath(arrow, Color.White)
            drawPath(arrow, Color.White, style = Stroke(width = 1.6f * k, join = StrokeJoin.Round))
        }
    }

    // Coin with check.
    if (coinT > 0f) {
        scale(coinT, pivot = center) {
            drawCircle(Brand.coin, radius = w * 0.118f, center = center)
        }
        if (checkT > 0f) {
            val k = w / 72f
            val p1 = center + Offset(-3.6f * k, 0.2f * k)
            val p2 = center + Offset(-0.9f * k, 2.9f * k)
            val p3 = center + Offset(4f * k, -2.6f * k)
            val path = Path().apply {
                moveTo(p1.x, p1.y)
                if (checkT < 0.4f) {
                    val f = checkT / 0.4f
                    lineTo(p1.x + (p2.x - p1.x) * f, p1.y + (p2.y - p1.y) * f)
                } else {
                    lineTo(p2.x, p2.y)
                    val f = (checkT - 0.4f) / 0.6f
                    lineTo(p2.x + (p3.x - p2.x) * f, p2.y + (p3.y - p2.y) * f)
                }
            }
            drawPath(path, Color.White, style = Stroke(width = 2.3f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }

    // Spark.
    if (sparkT > 0f) {
        val k = w / 72f
        val c = center + Offset(15f * k, -23f * k)
        val s = 4.6f * k * sparkT * twinkle
        rotate(degrees = (1f - sparkT) * -90f, pivot = c) {
            val q = s * 0.16f
            val star = Path().apply {
                moveTo(c.x, c.y - s)
                quadraticBezierTo(c.x + q, c.y - q, c.x + s, c.y)
                quadraticBezierTo(c.x + q, c.y + q, c.x, c.y + s)
                quadraticBezierTo(c.x - q, c.y + q, c.x - s, c.y)
                quadraticBezierTo(c.x - q, c.y - q, c.x, c.y - s)
                close()
            }
            drawPath(star, Color.White)
        }
        drawCircle(Color.White.copy(alpha = 0.75f * sparkT), radius = 1.5f * k, center = center + Offset(23.5f * k, -13f * k))
    }
}
