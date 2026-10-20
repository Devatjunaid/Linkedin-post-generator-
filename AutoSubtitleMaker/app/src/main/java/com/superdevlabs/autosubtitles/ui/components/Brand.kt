package com.superdevlabs.autosubtitles.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.superdevlabs.autosubtitles.ui.theme.Display
import com.superdevlabs.autosubtitles.ui.theme.AppColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Animation state of the brand mark. Every part animates on its own value:
 * waveform ring, caption bubble, play prism, the two subtitle bars and the orbiting spark.
 * [time] (seconds) drives the idle motion: ring "listening" pulse and spark orbit.
 */
data class MarkPhase(
    val ring: Float = 1f,
    val bubble: Float = 1f,
    val play: Float = 1f,
    val bar1: Float = 1f,
    val bar2: Float = 1f,
    val spark: Float = 1f,
    val time: Float = 0f,
)

fun easeOutCubic(t: Float): Float = 1f - (1f - t.coerceIn(0f, 1f)).pow(3)

fun easeOutBack(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    val c1 = 1.70158f
    val c3 = c1 + 1f
    return 1f + c3 * (x - 1f).pow(3) + c1 * (x - 1f).pow(2)
}

/** Progress of a phase that runs from [startMs] to [endMs] on a timeline at [nowMs]. */
fun phase(nowMs: Float, startMs: Float, endMs: Float): Float = ((nowMs - startMs) / (endMs - startMs)).coerceIn(0f, 1f)

fun DrawScope.drawBrandMark(p: MarkPhase) {
    val s = size.minDimension
    val c = Offset(size.width / 2f, size.height / 2f)
    val ringRadius = s * 0.36f

    // 1. Waveform ring: 48 ticks that grow in clockwise and then pulse like live audio.
    val ticks = 48
    for (i in 0 until ticks) {
        val appear = (p.ring * ticks - i).coerceIn(0f, 1f)
        if (appear <= 0f) continue
        val angle = ((i * 360f / ticks - 90f + p.time * 10f) * PI / 180f).toFloat()
        val amp = abs(sin(p.time * 3.2f + i * 0.7f) * cos(i * 0.23f)) * 0.75f + 0.25f
        val len = s * (0.025f + 0.085f * amp) * appear
        val dir = Offset(cos(angle), sin(angle))
        drawLine(
            color = AppColors.Lime.copy(alpha = (0.3f + 0.7f * amp) * appear),
            start = c + dir * ringRadius,
            end = c + dir * (ringRadius + len),
            strokeWidth = s * 0.016f,
            cap = StrokeCap.Round,
        )
    }

    // 2. Caption bubble with tail, popping in with a twist.
    val bw = s * 0.46f
    val bh = s * 0.35f
    val left = c.x - bw / 2f
    val top = c.y - bh / 2f - s * 0.02f
    if (p.bubble > 0f) {
        withTransform({
            rotate(-16f * (1f - p.bubble), c)
            scale(p.bubble, p.bubble, c)
        }) {
            val bubble = Path().apply {
                addRoundRect(RoundRect(left, top, left + bw, top + bh, CornerRadius(s * 0.075f)))
                moveTo(left + bw * 0.16f, top + bh - 1f)
                lineTo(left + bw * 0.06f, top + bh + s * 0.085f)
                lineTo(left + bw * 0.4f, top + bh - 1f)
                close()
            }
            drawPath(bubble, AppColors.Lime)

            // 3. Play prism.
            val pc = Offset(left + bw * 0.24f, top + bh * 0.3f)
            val ps = s * 0.075f * easeOutBack(p.play)
            if (ps > 0f) {
                val prism = Path().apply {
                    moveTo(pc.x - ps * 0.45f, pc.y - ps * 0.55f)
                    lineTo(pc.x + ps * 0.6f, pc.y)
                    lineTo(pc.x - ps * 0.45f, pc.y + ps * 0.55f)
                    close()
                }
                drawPath(prism, AppColors.Coral)
            }

            // 4. Subtitle bars typing out, with a blinking caret.
            val barH = s * 0.042f
            val barX = left + bw * 0.14f
            val bar1Y = top + bh * 0.58f
            val bar2Y = bar1Y + barH * 1.7f
            val bar1W = bw * 0.68f * easeOutCubic(p.bar1)
            val bar2W = bw * 0.4f * easeOutCubic(p.bar2)
            if (bar1W > 0f) {
                drawRoundRect(AppColors.Ink, Offset(barX, bar1Y), Size(bar1W, barH), CornerRadius(barH / 2f))
            }
            if (bar2W > 0f) {
                drawRoundRect(AppColors.Ink.copy(alpha = 0.55f), Offset(barX, bar2Y), Size(bar2W, barH), CornerRadius(barH / 2f))
            }
            val typing = when {
                p.bar1 in 0.01f..0.99f -> Offset(barX + bar1W + barH * 0.4f, bar1Y)
                p.bar2 in 0.01f..0.99f -> Offset(barX + bar2W + barH * 0.4f, bar2Y)
                else -> null
            }
            if (typing != null && sin(p.time * 18f) > -0.2f) {
                drawRect(AppColors.Coral, typing, Size(barH * 0.35f, barH))
            }
        }
    }

    // 5. Orbiting spark and a counter-orbiting dot.
    if (p.spark > 0f) {
        val sparkAngle = ((-50f + p.time * 40f) * PI / 180f).toFloat()
        val sp = c + Offset(cos(sparkAngle), sin(sparkAngle)) * (ringRadius + s * 0.13f)
        val r = s * 0.045f * easeOutBack(p.spark)
        val star = Path().apply {
            moveTo(sp.x, sp.y - r)
            quadraticTo(sp.x, sp.y, sp.x + r, sp.y)
            quadraticTo(sp.x, sp.y, sp.x, sp.y + r)
            quadraticTo(sp.x, sp.y, sp.x - r, sp.y)
            quadraticTo(sp.x, sp.y, sp.x, sp.y - r)
            close()
        }
        rotate(p.time * 60f, sp) { drawPath(star, AppColors.Coral) }
        val dotAngle = ((130f - p.time * 25f) * PI / 180f).toFloat()
        val dotPos = c + Offset(cos(dotAngle), sin(dotAngle)) * (ringRadius + s * 0.1f)
        drawCircle(AppColors.Text.copy(alpha = p.spark), radius = s * 0.016f, center = dotPos)
    }
}

/** The mark with idle motion running (ring pulse, spark orbit). */
@Composable
fun LiveBrandMark(size: Dp, modifier: Modifier = Modifier, phase: MarkPhase = MarkPhase()) {
    val transition = rememberInfiniteTransition(label = "mark")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 36f,
        animationSpec = infiniteRepeatable(tween(36_000, easing = LinearEasing), RepeatMode.Restart),
        label = "time",
    )
    Canvas(modifier.size(size).semantics { contentDescription = "Auto Subtitles logo" }) {
        drawBrandMark(phase.copy(time = time))
    }
}

/** Static mark for small places (headers). */
@Composable
fun BrandMark(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { drawBrandMark(MarkPhase(time = 2f)) }
}

/**
 * "subtitles" wordmark. Letters rise in one after another as [progress] goes 0 → 1;
 * the final "a" is lime and lands with a small overshoot.
 */
@Composable
fun BrandWordmark(progress: Float, fontSize: TextUnit = 44.sp, modifier: Modifier = Modifier) {
    val letters = "subtitles"
    Row(modifier) {
        letters.forEachIndexed { i, ch ->
            val lp = ((progress * (letters.length + 1.5f)) - i).coerceIn(0f, 1f)
            val eased = if (i == letters.lastIndex) easeOutBack(lp) else easeOutCubic(lp)
            Text(
                text = ch.toString(),
                fontFamily = Display,
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSize,
                letterSpacing = (-1).sp,
                color = if (i == letters.lastIndex) AppColors.Lime else AppColors.Text,
                modifier = Modifier.graphicsLayer {
                    alpha = lp
                    translationY = (1f - eased) * 28.dp.toPx()
                    rotationZ = (1f - eased) * if (i % 2 == 0) -10f else 10f
                },
            )
        }
    }
}
