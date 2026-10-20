package com.dailycash.tracker.ui.brand

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Ink = Color(0xFF0B0F0E)
private val Mint = Color(0xFF6EF0B9)
private val MintLight = Color(0xFFD6FFEE)
private val MintDeep = Color(0xFF2E9E70)
private val Guilloche = Color(0xFF2F7A62)
private val SunTop = Color(0xFFFFD98A)
private val SunBottom = Color(0xFFF29A2E)
private val SunInk = Color(0xFF8A5212)
private val Amber = Color(0xFFFFB547)
private val Sky = Color(0xFF9DBDF0)

/**
 * The Daily Cash mark, drawn live in a 512-unit space:
 * a banknote folded into a paper plane flies a dashed travel route over a coin-sun
 * rising from a guilloche horizon. On entry the sun rises and the plane glides in;
 * after that the plane floats, the sun's rays turn and the route keeps moving.
 */
@Composable
fun DailyCashLogo(modifier: Modifier = Modifier, size: Dp = 168.dp, playEntrance: Boolean = true) {
    val rise = remember { Animatable(if (playEntrance) 1f else 0f) }
    val glide = remember { Animatable(if (playEntrance) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (playEntrance) {
            launch { rise.animateTo(0f, tween(1100, easing = FastOutSlowInEasing)) }
            delay(350)
            glide.animateTo(0f, spring(dampingRatio = 0.62f, stiffness = 140f))
        }
    }
    val loop = rememberInfiniteTransition(label = "logo")
    val t by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(3_200, easing = LinearEasing)), label = "t")

    Canvas(modifier.size(size).semantics { contentDescription = "Daily Cash logo" }) {
        scale(this.size.minDimension / 512f, Offset.Zero) {
            drawMark(t, rise.value, glide.value)
        }
    }
}

private fun DrawScope.drawMark(t: Float, rise: Float, glide: Float) {
    val wave = sin(2 * PI * t).toFloat()
    val bob = wave * 7f
    val rays = t * 30f

    drawRoundRect(
        Brush.radialGradient(listOf(Color(0xFF16302A), Color(0xFF0D1513), Color(0xFF080B0A)), Offset(256f, 318f), 384f),
        size = Size(512f, 512f),
        cornerRadius = CornerRadius(112f),
    )

    // Coin-sun rising behind the horizon.
    clipRect(0f, 0f, 512f, 352f) {
        translate(0f, rise * 150f) {
            val c = Offset(256f, 352f)
            for (i in 0 until 12) {
                val a = Math.toRadians((i * 30 + rays).toDouble())
                val d = Offset(cos(a).toFloat(), sin(a).toFloat())
                drawLine(Amber.copy(alpha = 0.55f), c + d * 128f, c + d * 150f, strokeWidth = 9f, cap = StrokeCap.Round)
            }
            drawCircle(Brush.verticalGradient(listOf(SunTop, SunBottom), 240f, 464f), 112f, c)
            for (i in 0 until 36) {
                rotate(i * 10f - 180f + rays, c) {
                    drawLine(SunInk.copy(alpha = 0.45f), Offset(256f, 248f), Offset(256f, 258f), strokeWidth = 3f)
                }
            }
            drawCircle(SunInk.copy(alpha = 0.5f), 84f, c, style = Stroke(3f))
        }
    }

    drawRoundRect(Mint.copy(alpha = 0.9f), Offset(40f, 350f), Size(432f, 5f), CornerRadius(2.5f))
    for ((y, phase) in listOf(392f to 0, 422f to 1, 452f to 0)) {
        val p = Path().apply {
            moveTo(40f, y)
            var x = 40f
            var k = phase
            while (x < 472f) {
                quadraticTo(x + 18f, y + if (k % 2 == 1) -12f else 12f, x + 36f, y)
                x += 36f
                k++
            }
        }
        drawPath(p, Guilloche.copy(alpha = 0.9f), style = Stroke(3f))
    }

    // Travel route with country stops.
    val routeAlpha = 1f - glide
    translate(0f, bob * 0.4f) {
        val route = Path().apply {
            moveTo(58f, 300f)
            cubicTo(96f, 236f, 132f, 300f, 176f, 232f)
        }
        drawPath(
            route,
            Color(0xFFEEF3EF).copy(alpha = 0.7f * routeAlpha),
            style = Stroke(6f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 16f), t * 44f)),
        )
    }
    val pulse = { k: Int -> 0.75f + 0.25f * sin(2 * PI * (t + k / 3f)).toFloat() }
    drawCircle(Sky.copy(alpha = routeAlpha), 11f * pulse(0), Offset(62f, 294f))
    drawCircle(Amber.copy(alpha = routeAlpha), 11f * pulse(1), Offset(114f, 262f))

    // Banknote paper plane.
    withTransform({
        translate(-glide * 330f, bob + glide * 90f)
        rotate(wave * 1.6f - glide * 18f, Offset(280f, 220f))
        scale(1.16f, 1.16f, Offset(290f, 215f))
    }) {
        val nose = Offset(410f, 140f)
        val tip = Offset(148f, 206f)
        val fold = Offset(252f, 232f)
        val keel = Offset(270f, 266f)
        val tail = Offset(230f, 300f)
        drawPath(tri(nose, fold, keel), Brush.linearGradient(listOf(Mint, Color(0xFF3FC48D)), fold, nose))
        drawPath(tri(fold, tail, keel), MintDeep)
        val wing = tri(nose, tip, fold)
        drawPath(wing, Brush.linearGradient(listOf(MintLight, Color(0xFF8FF5C9)), tip, nose))
        clipPath(wing) {
            drawLine(
                Guilloche,
                Offset(178f, 214f),
                Offset(392f, 152f),
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
            drawCircle(Guilloche, 17f, Offset(232f, 208f), style = Stroke(3f))
            drawCircle(Guilloche, 7f, Offset(232f, 208f))
            val squiggle = Path().apply {
                moveTo(268f, 196f)
                relativeQuadraticTo(12f, -12f, 24f, -6f)
                relativeQuadraticTo(12f, 6f, 24f, -6f)
                relativeQuadraticTo(12f, -12f, 24f, -6f)
            }
            drawPath(squiggle, Guilloche, style = Stroke(3f))
        }
        drawLine(Ink.copy(alpha = 0.25f), nose, fold, strokeWidth = 3f)
    }
}

private fun tri(a: Offset, b: Offset, c: Offset) = Path().apply {
    moveTo(a.x, a.y)
    lineTo(b.x, b.y)
    lineTo(c.x, c.y)
    close()
}
