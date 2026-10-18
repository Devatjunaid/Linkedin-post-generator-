package com.unpin.photoscrub.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import com.unpin.photoscrub.ui.theme.Clean
import com.unpin.photoscrub.ui.theme.Hill
import com.unpin.photoscrub.ui.theme.Ink
import com.unpin.photoscrub.ui.theme.Paper
import com.unpin.photoscrub.ui.theme.Signal
import com.unpin.photoscrub.ui.theme.Sky
import com.unpin.photoscrub.ui.theme.Sun
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * The Unpin mark, drawn in a 100-unit box. [progress] runs the build-up
 * animation (1 = final mark, identical to the launcher icon); [pulse] drives
 * the idle radar ping (0..1, looping) and may be left at 0 for a static mark.
 */
@Composable
fun LogoMark(
    modifier: Modifier = Modifier,
    progress: Float = 1f,
    pulse: Float = 0f,
    drawTile: Boolean = true,
) {
    Canvas(modifier) {
        val s = size.minDimension / 100f
        translate((size.width - 100f * s) / 2f, (size.height - 100f * s) / 2f) {
            scale(s, s, pivot = Offset.Zero) {
                drawMark(progress.coerceIn(0f, 1f), pulse, drawTile)
            }
        }
    }
}

/** Mark with its own idle pulse, for headers. */
@Composable
fun LiveLogoMark(modifier: Modifier = Modifier, drawTile: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "logo")
    val pulse by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "pulse"
    )
    LogoMark(modifier, 1f, pulse, drawTile)
}

private fun seg(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
private fun easeOutCubic(x: Float) = 1f - (1f - x).pow(3)
private fun easeInOutCubic(x: Float) = if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f).pow(3) / 2f
private fun easeOutBack(x: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    return 1f + c3 * (x - 1f).pow(3) + c1 * (x - 1f).pow(2)
}
private fun bounceOut(x: Float): Float {
    val n = 7.5625f
    val d = 2.75f
    return when {
        x < 1f / d -> n * x * x
        x < 2f / d -> { val y = x - 1.5f / d; n * y * y + 0.75f }
        x < 2.5f / d -> { val y = x - 2.25f / d; n * y * y + 0.9375f }
        else -> { val y = x - 2.625f / d; n * y * y + 0.984375f }
    }
}

private val pinPath = Path().apply {
    moveTo(50f, 17f)
    cubicTo(42.8f, 17f, 37f, 22.6f, 37f, 29.8f)
    cubicTo(37f, 39.5f, 50f, 56f, 50f, 56f)
    cubicTo(50f, 56f, 63f, 39.5f, 63f, 29.8f)
    cubicTo(63f, 22.6f, 57.2f, 17f, 50f, 17f)
    close()
}

private val hillPath = Path().apply {
    moveTo(28f, 64f); lineTo(28f, 56f); lineTo(40f, 46f); lineTo(49f, 54f)
    lineTo(57f, 48f); lineTo(72f, 60f); lineTo(72f, 64f); close()
}

private const val BAND_ANGLE = -28f
private val BAND_PIVOT = Offset(50f, 44f)

/** Pixels the scrubber knocks off the pin: start (on the pin) -> rest (as in the icon). */
private class Bit(val from: Offset, val to: Offset, val size: Float, val color: Color, val stays: Boolean)

private val bits = listOf(
    Bit(Offset(47f, 47f), Offset(55f, 49f), 3.4f, Signal, true),
    Bit(Offset(50f, 50f), Offset(66f, 47f), 3f, Signal, true),
    Bit(Offset(46f, 44f), Offset(63f, 58f), 2.6f, Signal, true),
    Bit(Offset(52f, 46f), Offset(60f, 53f), 3f, Paper.copy(alpha = 0.8f), true),
    Bit(Offset(49f, 52f), Offset(71f, 54f), 2.6f, Paper.copy(alpha = 0.8f), true),
    Bit(Offset(51f, 49f), Offset(76f, 49f), 2.4f, Signal.copy(alpha = 0.47f), true),
    Bit(Offset(48f, 53f), Offset(69f, 62f), 2f, Signal.copy(alpha = 0.47f), true),
    Bit(Offset(47f, 49f), Offset(92f, 40f), 2.4f, Signal, false),
    Bit(Offset(50f, 47f), Offset(88f, 66f), 2f, Paper, false),
    Bit(Offset(52f, 51f), Offset(96f, 54f), 2.2f, Signal, false),
)

private fun DrawScope.drawMark(t: Float, pulse: Float, drawTile: Boolean) {
    // 1. Tile
    if (drawTile) {
        val tp = easeOutBack(seg(t, 0f, 0.16f)).coerceAtLeast(0f)
        scale(0.6f + 0.4f * tp, 0.6f + 0.4f * tp, pivot = Offset(50f, 50f)) {
            drawRoundRect(Ink, Offset.Zero, Size(100f, 100f), CornerRadius(26f, 26f), alpha = seg(t, 0f, 0.08f))
        }
    }

    // 2. Radar rings and idle ping
    val ringAlpha = seg(t, 0.08f, 0.3f)
    drawCircle(Paper, 40f, Offset(50f, 48f), alpha = 0.18f * ringAlpha, style = Stroke(1.2f))
    drawCircle(Paper, 28f, Offset(50f, 48f), alpha = 0.25f * ringAlpha, style = Stroke(1.2f))
    if (pulse > 0f && t >= 1f) {
        drawCircle(Signal, 14f + 34f * pulse, Offset(50f, 46f), alpha = 0.35f * (1f - pulse), style = Stroke(1.4f))
    }

    // 3. Viewfinder brackets slide in from the corners
    val bp = easeOutCubic(seg(t, 0.1f, 0.34f))
    if (bp > 0f) {
        val o = (1f - bp) * 10f
        val stroke = Stroke(3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val brackets = Path().apply {
            moveTo(14f - o, 26f - o); lineTo(14f - o, 18f - o); quadraticTo(14f - o, 14f - o, 18f - o, 14f - o); lineTo(26f - o, 14f - o)
            moveTo(74f + o, 14f - o); lineTo(82f + o, 14f - o); quadraticTo(86f + o, 14f - o, 86f + o, 18f - o); lineTo(86f + o, 26f - o)
            moveTo(14f - o, 74f + o); lineTo(14f - o, 82f + o); quadraticTo(14f - o, 86f + o, 18f - o, 86f + o); lineTo(26f - o, 86f + o)
            moveTo(86f + o, 64f + o); lineTo(86f + o, 70f + o)
        }
        drawPath(brackets, Paper, alpha = bp, style = stroke)
    }

    // 4. Photo card rises into place
    val cp = easeOutCubic(seg(t, 0.18f, 0.42f))
    if (cp > 0f) {
        withTransform({
            translate(0f, (1f - cp) * 14f)
            rotate(-8f - (1f - cp) * 10f, pivot = Offset(50f, 54f))
        }) {
            drawRoundRect(Paper, Offset(24f, 32f), Size(52f, 44f), CornerRadius(4f, 4f), alpha = cp)
            drawRect(Sky, Offset(28f, 36f), Size(44f, 28f), alpha = cp)
            drawCircle(Sun, 4f, Offset(64f, 43f), alpha = cp)
            drawPath(hillPath, Hill, alpha = cp)
        }
    }

    // 5. Pin drops with a bounce; 6. the scrubber slices its lower half away
    val dp = seg(t, 0.34f, 0.58f)
    if (dp > 0f) {
        val dropY = -46f * (1f - bounceOut(dp))
        val pinAlpha = seg(t, 0.34f, 0.4f)
        val erase = easeInOutCubic(seg(t, 0.58f, 0.76f))
        val bandOffset = -70f * (1f - erase)
        val front = 84f + bandOffset

        fun drawPin() {
            translate(0f, dropY) {
                drawPath(pinPath, Signal, alpha = pinAlpha)
                drawCircle(Ink, 4.6f, Offset(50f, 29.5f), alpha = pinAlpha)
            }
        }
        // Part above the band always survives
        rotate(BAND_ANGLE, BAND_PIVOT) {
            clipRect(-100f, -100f, 200f, 40f) { rotate(-BAND_ANGLE, BAND_PIVOT) { drawPin() } }
            // Part below the band survives only until the scrubber has passed
            clipRect(front, 40f, 200f, 200f) { rotate(-BAND_ANGLE, BAND_PIVOT) { drawPin() } }
        }

        // Scrubber band
        val bandAlpha = seg(t, 0.56f, 0.6f)
        if (bandAlpha > 0f) {
            rotate(BAND_ANGLE, BAND_PIVOT) {
                drawRoundRect(Paper, Offset(14.5f + bandOffset, 40.5f), Size(71f, 7f), CornerRadius(1.5f, 1.5f), alpha = bandAlpha)
                for (k in 0 until 7) {
                    val x = 24f + k * 8f + bandOffset
                    drawLine(Ink.copy(alpha = 0.33f), Offset(x, 46f), Offset(x + 3f, 42f), 1f, alpha = bandAlpha)
                }
            }
        }

        // 7. Data pixels fly off
        val fp = easeOutCubic(seg(t, 0.64f, 0.92f))
        if (fp > 0f) {
            bits.forEach { b ->
                val p = Offset(b.from.x + (b.to.x - b.from.x) * fp, b.from.y + (b.to.y - b.from.y) * fp)
                val a = if (b.stays) 1f else (1f - fp)
                drawRect(b.color, p, Size(b.size, b.size), alpha = a * seg(t, 0.64f, 0.68f))
            }
        }
    }

    // 8. Verified badge pops in
    val vp = seg(t, 0.8f, 0.96f)
    if (vp > 0f) {
        val k = easeOutBack(vp)
        scale(k, k, pivot = Offset(77f, 75f)) {
            drawCircle(Ink, 10f, Offset(77f, 75f))
            drawCircle(Clean, 9f, Offset(77f, 75f))
            val tick = Path().apply { moveTo(72.6f, 75.2f); lineTo(75.8f, 78.4f); lineTo(81.6f, 72.2f) }
            drawPath(tick, Paper, style = Stroke(2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        // brief sparkle
        val sp = seg(t, 0.9f, 1f)
        if (sp > 0f && sp < 1f) {
            val r = 3f + 4f * sp
            val c = Offset(88f, 63f)
            for (i in 0 until 4) {
                val ang = (i * 90f + 45f) * PI.toFloat() / 180f
                drawLine(
                    Sun, c + Offset(cos(ang) * r * 0.4f, sin(ang) * r * 0.4f), c + Offset(cos(ang) * r, sin(ang) * r),
                    1.4f, cap = StrokeCap.Round, alpha = 1f - sp
                )
            }
        }
    }
}

/** "unpin" with letters rising in one by one as [progress] goes 0 -> 1. */
@Composable
fun AnimatedWordmark(progress: Float, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val word = "unpin"
    Row(modifier) {
        word.forEachIndexed { i, ch ->
            val p = FastOutSlowInEasing.transform(seg(progress, i * 0.12f, i * 0.12f + 0.5f))
            Text(
                text = ch.toString(),
                style = style,
                color = if (i == 2) Signal else color,
                modifier = Modifier.graphicsLayer {
                    alpha = p
                    translationY = (1f - p) * 24f * density
                },
            )
        }
    }
}
