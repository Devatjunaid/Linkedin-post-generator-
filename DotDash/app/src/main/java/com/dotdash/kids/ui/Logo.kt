package com.dotdash.kids.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class LogoMotion { Intro, Bob }

private class Letter(val ch: String, val color: Color, val tilt: Float)

private val ROW1 = listOf(Letter("D", C.Sun, -6f), null, Letter("T", C.Coral, 5f))
private val ROW2 = listOf(
    Letter("D", C.Sky, -4f), Letter("A", C.Sun, 4f), Letter("S", C.Coral, -5f), Letter("H", C.Mint, 3f), Letter("!", C.Grape, 8f),
)

/**
 * DOT DASH! in chunky sticker letters: each letter has a thick ink outline and a
 * drop shadow, the "O" is a glowing dot, and a dotted trail marches underneath.
 */
@Composable
fun Logo(size: Dp, motion: LogoMotion, modifier: Modifier = Modifier, introDelayMs: Int = 1750) {
    Column(
        modifier.semantics { contentDescription = "Dot Dash!" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ROW1.forEachIndexed { i, l ->
                Moving(i, motion, size, introDelayMs) {
                    if (l == null) GlowDot(size * 0.74f) else StickerLetter(l, size)
                }
            }
        }
        Row(Modifier.offset(y = -size * 0.14f), verticalAlignment = Alignment.CenterVertically) {
            ROW2.forEachIndexed { i, l -> Moving(i + 3, motion, size, introDelayMs) { StickerLetter(l, size) } }
        }
        MarchingTrail(size)
    }
}

@Composable
private fun Moving(i: Int, motion: LogoMotion, size: Dp, introDelayMs: Int, content: @Composable () -> Unit) {
    when (motion) {
        LogoMotion.Intro -> {
            val y = remember { Animatable(-1600f) }
            LaunchedEffect(Unit) {
                delay(introDelayMs + i * 90L)
                y.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 380f))
            }
            Box(Modifier.graphicsLayer { translationY = y.value }) { content() }
        }
        LogoMotion.Bob -> {
            val t = rememberInfiniteTransition(label = "letter")
            val p by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse, StartOffset(i * 140)), label = "p")
            Box(Modifier.graphicsLayer { translationY = -2.5f * density * (p + 1); rotationZ = p * 2f }) { content() }
        }
    }
}

@Composable
private fun StickerLetter(l: Letter, size: Dp) {
    val fs = (size.value).sp
    val stroke = size.value * 0.16f
    Box(Modifier.padding(horizontal = (size.value * 0.015f).dp).graphicsLayer { rotationZ = l.tilt }) {
        // drop shadow
        BasicText(
            l.ch, Modifier.offset(y = size * 0.1f),
            style = display(fs, C.InkDark).copy(drawStyle = Stroke(stroke, join = StrokeJoin.Round)),
        )
        // ink outline, then the colour on top
        BasicText(l.ch, style = display(fs, C.Ink).copy(drawStyle = Stroke(stroke, join = StrokeJoin.Round)))
        BasicText(l.ch, style = display(fs, l.color))
    }
}

@Composable
private fun GlowDot(d: Dp) {
    val t = rememberInfiniteTransition(label = "dot")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "ring")
    Canvas(Modifier.padding(horizontal = d * 0.06f).size(d)) {
        val r = size.minDimension * 0.38f
        val c = center
        drawCircle(C.Mint.copy(alpha = 0.9f * (1 - p)), r * (1 + 0.6f * p), c, style = Stroke(size.minDimension * 0.08f))
        drawCircle(C.InkDark, r + size.minDimension * 0.045f, c + Offset(0f, size.minDimension * 0.07f))
        drawCircle(C.Ink, r + size.minDimension * 0.045f, c)
        drawCircle(C.Mint, r - size.minDimension * 0.01f, c)
        rotate(-35f, c + Offset(-r * 0.37f, -r * 0.42f)) {
            drawOval(
                Color.White.copy(alpha = 0.85f),
                c + Offset(-r * 0.37f - r * 0.29f, -r * 0.42f - r * 0.18f),
                Size(r * 0.58f, r * 0.36f),
            )
        }
        drawCircle(Color.White.copy(alpha = 0.5f), r * 0.13f, c + Offset(r * 0.26f, r * 0.32f))
    }
}

@Composable
private fun MarchingTrail(size: Dp) {
    val t = rememberInfiniteTransition(label = "trail")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "phase")
    Canvas(Modifier.offset(y = -size * 0.12f).width(size * 3.6f).height(size * 0.36f)) {
        inBox(0f, 0f, 300f, 40f) {
            val p = Paths.svg("M8 26 C 70 44, 130 4, 190 22 S 260 30, 292 12")
            drawPath(p, C.Sun.copy(alpha = 0.25f), style = Stroke(10f, cap = StrokeCap.Round))
            drawPath(
                p, C.SunLight,
                style = Stroke(4f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(1f, 12f), -13f * phase)),
            )
        }
    }
}

