package com.dailycash.tracker.ui.brand

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dailycash.tracker.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Daily Cash mark, drawn live:
 * a turning guilloche rosette (the security print on banknotes), two fanned notes,
 * an orbit ring with a travelling coin (money across countries), a front coin that drops
 * in with a bounce, and a location pulse.
 */
@Composable
fun DailyCashLogo(modifier: Modifier = Modifier, size: Dp = 168.dp, playEntrance: Boolean = true) {
    val enter = remember { Animatable(if (playEntrance) 0f else 1f) }
    val coinDrop = remember { Animatable(if (playEntrance) -1f else 0f) }
    LaunchedEffect(Unit) {
        if (playEntrance) {
            launch { enter.animateTo(1f, tween(1300, easing = FastOutSlowInEasing)) }
            delay(450)
            coinDrop.animateTo(0f, spring(dampingRatio = 0.38f, stiffness = Spring.StiffnessLow))
        }
    }
    val loop = rememberInfiniteTransition(label = "logo")
    val spin by loop.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "spin")
    val orbit by loop.animateFloat(0f, 360f, infiniteRepeatable(tween(4_200, easing = LinearEasing)), label = "orbit")
    val pulse by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(1_700, easing = LinearOutSlowInEasing)), label = "pulse")
    val sway by loop.animateFloat(
        -1f, 1f,
        infiniteRepeatable(tween(2_600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway",
    )
    val measurer = rememberTextMeasurer()

    Canvas(modifier.size(size).semantics { contentDescription = "Daily Cash logo" }) {
        val c = center
        val r = this.size.minDimension / 2f
        val p = enter.value

        drawCircle(Palette.Surface2, r * 0.98f * (0.6f + 0.4f * p), c)
        drawCircle(Palette.Line.copy(alpha = p), r * 0.98f, c, style = Stroke(1.dp.toPx()))

        // Guilloche rosette, each petal traced in turn.
        rotate(spin, c) {
            val petals = 9
            for (i in 0 until petals) {
                val sweep = (p * petals - i).coerceIn(0f, 1f)
                if (sweep <= 0f) continue
                rotate(i * (180f / petals), c) {
                    drawArc(
                        color = Palette.Mint.copy(alpha = 0.22f),
                        startAngle = 0f,
                        sweepAngle = 360f * sweep,
                        useCenter = false,
                        topLeft = Offset(c.x - r * 0.86f, c.y - r * 0.28f),
                        size = Size(r * 1.72f, r * 0.56f),
                        style = Stroke(1.1.dp.toPx()),
                    )
                }
            }
        }

        drawNote(-24f * p + sway * 1.5f, -r * 0.10f, -r * 0.20f, Palette.notes[2], r, c, p)
        drawNote(12f * p - sway * 1.5f, r * 0.06f, -r * 0.10f, Palette.notes[1], r, c, p)

        // Orbit ring with a travelling coin.
        rotate(-18f, c) {
            val oc = Offset(c.x, c.y + r * 0.08f)
            drawOval(
                color = Palette.Text.copy(alpha = 0.35f * p),
                topLeft = Offset(oc.x - r * 0.92f, oc.y - r * 0.30f),
                size = Size(r * 1.84f, r * 0.60f),
                style = Stroke(1.4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))),
            )
            val a = Math.toRadians(orbit.toDouble())
            val pos = Offset(oc.x + r * 0.92f * cos(a).toFloat(), oc.y + r * 0.30f * sin(a).toFloat())
            drawCircle(Palette.Amber, r * 0.075f * p, pos)
            drawCircle(Palette.Ink.copy(alpha = 0.45f), r * 0.045f * p, pos, style = Stroke(1.dp.toPx()))
        }

        // Front coin.
        val cr = r * 0.36f
        val coin = Offset(c.x, c.y + r * 0.14f + coinDrop.value * r * 1.1f)
        drawCircle(Color.Black.copy(alpha = 0.35f), cr, coin + Offset(0f, cr * 0.12f))
        drawCircle(Palette.Mint, cr, coin)
        for (i in 0 until 36) {
            val ang = Math.toRadians(i * 10.0 + spin * 0.5)
            val dir = Offset(cos(ang).toFloat(), sin(ang).toFloat())
            drawLine(
                Palette.Ink.copy(alpha = 0.35f),
                coin + dir * (cr * 0.86f),
                coin + dir * (cr * 0.96f),
                strokeWidth = 1.dp.toPx(),
            )
        }
        drawCircle(Palette.Ink, cr * 0.76f, coin, style = Stroke(1.4.dp.toPx()))
        val glyph = measurer.measure(
            "D",
            TextStyle(color = Palette.Ink, fontSize = (cr * 0.95f).toDp().toSp(), fontWeight = FontWeight.Black),
        )
        drawText(glyph, topLeft = coin - Offset(glyph.size.width / 2f, glyph.size.height / 2f))
        drawLine(
            Palette.Ink,
            coin + Offset(-cr * 0.46f, 0f),
            coin + Offset(-cr * 0.16f, 0f),
            strokeWidth = cr * 0.11f,
            cap = StrokeCap.Round,
        )

        // Location pulse.
        val pc = Offset(c.x + r * 0.62f, c.y - r * 0.62f)
        drawCircle(
            Palette.Mint.copy(alpha = (1f - pulse) * 0.6f * p),
            r * 0.06f + r * 0.14f * pulse,
            pc,
            style = Stroke(1.5.dp.toPx()),
        )
        drawCircle(Palette.Mint.copy(alpha = p), r * 0.055f, pc)
    }
}

private fun DrawScope.drawNote(
    angle: Float,
    dx: Float,
    dy: Float,
    colors: Palette.NoteColors,
    r: Float,
    c: Offset,
    alpha: Float,
) {
    val w = r * 1.12f
    val h = r * 0.62f
    rotate(angle, c) {
        translate(dx, dy) {
            val tl = Offset(c.x - w / 2f, c.y - h / 2f)
            drawRoundRect(colors.bg.copy(alpha = alpha), tl, Size(w, h), CornerRadius(r * 0.07f))
            drawRoundRect(
                colors.line.copy(alpha = alpha),
                tl + Offset(r * 0.05f, r * 0.05f),
                Size(w - r * 0.1f, h - r * 0.1f),
                CornerRadius(r * 0.04f),
                style = Stroke(1.dp.toPx()),
            )
            val wave = Path().apply {
                var x = tl.x + r * 0.1f
                val step = r * 0.12f
                var up = true
                moveTo(x, c.y)
                while (x < tl.x + w * 0.62f) {
                    quadraticTo(x + step / 2f, c.y + if (up) -h * 0.22f else h * 0.22f, x + step, c.y)
                    x += step
                    up = !up
                }
            }
            drawPath(wave, colors.ink.copy(alpha = 0.7f * alpha), style = Stroke(1.dp.toPx()))
            val seal = Offset(tl.x + w * 0.78f, c.y)
            drawCircle(colors.line.copy(alpha = alpha), h * 0.26f, seal, style = Stroke(1.dp.toPx()))
            drawCircle(colors.ink.copy(alpha = 0.5f * alpha), h * 0.12f, seal)
        }
    }
}
