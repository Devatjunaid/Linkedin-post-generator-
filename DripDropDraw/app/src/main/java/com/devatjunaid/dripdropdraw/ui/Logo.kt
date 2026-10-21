package com.devatjunaid.dripdropdraw.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.sin

enum class Mood { THIRSTY, HAPPY, LAUGH, SAD }

/** The cup character, drawn in a 200 x 200 box. [fill] is how full it is (0..1). */
fun DrawScope.drawMascot(mood: Mood, fill: Float, time: Float, lookX: Float = 0f) {
    val s = size.minDimension / 200f
    withTransform({
        translate((size.width - 200f * s) / 2f, (size.height - 200f * s) / 2f)
        scale(s, s, Offset.Zero)
    }) {
        val bounce = if (mood == Mood.LAUGH) abs(sin(time * 9f)) * -8f else 0f
        withTransform({ translate(0f, bounce) }) {
            val top = 30f
            val bottom = 185f
            val tw = 78f
            val bw = 56f
            val cx = 100f
            // Glass background.
            val inside = Path().apply {
                moveTo(cx - tw, top); lineTo(cx - bw, bottom); lineTo(cx + bw, bottom); lineTo(cx + tw, top); close()
            }
            drawPath(inside, Color.White.copy(alpha = 0.7f))
            // Water with a wavy top.
            val level = bottom - (bottom - top - 12f) * fill.coerceIn(0f, 1f)
            val t = (bottom - level) / (bottom - top)
            val halfAtLevel = bw + (tw - bw) * t - 6f
            val water = Path().apply {
                moveTo(cx - halfAtLevel, level)
                val waves = 4
                val step = halfAtLevel * 2f / waves
                for (i in 0 until waves) {
                    val x0 = cx - halfAtLevel + i * step
                    val dy = if (i % 2 == 0) -6f else 6f
                    quadraticTo(x0 + step / 2f, level + dy * sin(time * 3f + i), x0 + step, level)
                }
                lineTo(cx + bw - 6f, bottom - 6f)
                lineTo(cx - bw + 6f, bottom - 6f)
                close()
            }
            if (fill > 0.01f) drawPath(water, Palette.Water)
            val glass = Path().apply {
                moveTo(cx - tw, top); lineTo(cx - bw, bottom); lineTo(cx + bw, bottom); lineTo(cx + tw, top)
            }
            drawPath(glass, Palette.Ink, style = Stroke(20f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(glass, Palette.Glass, style = Stroke(13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawLine(Color.White.copy(alpha = 0.7f), Offset(cx - tw + 12f, top + 18f), Offset(cx - tw + 20f, top + 70f), 5f, StrokeCap.Round)

            val ey = 100f
            val ex = 26f
            val my = 136f
            drawOval(Palette.Pink.copy(alpha = 0.85f), Offset(cx - ex - 30f, my - 18f), Size(22f, 12f))
            drawOval(Palette.Pink.copy(alpha = 0.85f), Offset(cx + ex + 8f, my - 18f), Size(22f, 12f))
            val blink = (time % 3.4f) < 0.12f
            when (mood) {
                Mood.LAUGH -> {
                    for (sx in listOf(-ex, ex)) {
                        drawArc(Palette.Ink, 200f, 140f, false, Offset(cx + sx - 12f, ey - 8f), Size(24f, 22f), style = Stroke(6f, cap = StrokeCap.Round))
                    }
                    val mouth = Path().apply { moveTo(cx - 24f, my - 6f); quadraticTo(cx, my + 44f, cx + 24f, my - 6f); close() }
                    drawPath(mouth, Palette.Ink)
                    drawOval(Palette.Pink, Offset(cx - 11f, my + 10f), Size(22f, 12f))
                }
                Mood.SAD -> {
                    for (sx in listOf(-ex, ex)) {
                        drawCircle(Color.White, 12f, Offset(cx + sx, ey))
                        drawCircle(Palette.Ink, 12f, Offset(cx + sx, ey), style = Stroke(4f))
                        drawCircle(Palette.Ink, 5.5f, Offset(cx + sx, ey + 4f))
                    }
                    drawLine(Palette.Ink, Offset(cx - ex - 14f, ey - 12f), Offset(cx - ex + 12f, ey - 20f), 5f, StrokeCap.Round)
                    drawLine(Palette.Ink, Offset(cx + ex - 12f, ey - 20f), Offset(cx + ex + 14f, ey - 12f), 5f, StrokeCap.Round)
                    drawArc(Palette.Ink, 200f, 140f, false, Offset(cx - 16f, my), Size(32f, 22f), style = Stroke(5f, cap = StrokeCap.Round))
                    val tearY = ey + 18f + (time * 30f) % 30f
                    drawCircle(Palette.WaterLight, 6f, Offset(cx + ex + 8f, tearY))
                    drawCircle(Palette.WaterDark, 6f, Offset(cx + ex + 8f, tearY), style = Stroke(2.5f))
                }
                else -> {
                    for (sx in listOf(-ex, ex)) {
                        if (blink) {
                            drawLine(Palette.Ink, Offset(cx + sx - 10f, ey), Offset(cx + sx + 10f, ey), 5f, StrokeCap.Round)
                        } else {
                            drawCircle(Color.White, 13f, Offset(cx + sx, ey))
                            drawCircle(Palette.Ink, 13f, Offset(cx + sx, ey), style = Stroke(4f))
                            drawCircle(Palette.Ink, 6f, Offset(cx + sx + lookX * 5f, ey - 2f))
                        }
                    }
                    if (mood == Mood.THIRSTY) {
                        drawOval(Palette.Ink, Offset(cx - 10f, my - 8f), Size(20f, 20f))
                        drawOval(Palette.Pink, Offset(cx - 6f, my + 3f), Size(12f, 11f))
                    } else {
                        val mouth = Path().apply { moveTo(cx - 20f, my - 6f); quadraticTo(cx, my + 30f, cx + 20f, my - 6f); close() }
                        drawPath(mouth, Palette.Ink)
                    }
                }
            }
        }
    }
}

@Composable
fun Mascot(mood: Mood, modifier: Modifier = Modifier, fill: Float = 0.6f) {
    val anim = rememberInfiniteTransition(label = "mascot")
    val time by anim.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1_000_000, easing = LinearEasing), RepeatMode.Restart),
        label = "time",
    )
    Canvas(modifier) { drawMascot(mood, fill, time) }
}

/** Text with a thick dark outline, like a cartoon title. */
@Composable
fun OutlinedTitle(text: String, color: Color, size: TextUnit, modifier: Modifier = Modifier, outline: Dp = 4.dp) {
    val outlinePx = with(LocalDensity.current) { outline.toPx() }
    Box(modifier) {
        Text(
            text,
            style = TextStyle(
                fontFamily = Fredoka,
                fontWeight = FontWeight.Bold,
                fontSize = size,
                color = Palette.Ink,
                drawStyle = Stroke(width = outlinePx * 2f, join = StrokeJoin.Round),
            ),
        )
        Text(text, style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = size, color = color))
    }
}

/** The game's logo: the title with the cup character catching a drop. */
@Composable
fun GameLogo(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            OutlinedTitle("Drip Drop", Palette.Water, 50.sp)
        }
        Box(contentAlignment = Alignment.Center) {
            OutlinedTitle("Draw", Palette.Orange, 62.sp, Modifier.offset(y = (-14).dp))
            Canvas(Modifier.size(220.dp, 30.dp).offset(y = 30.dp)) {
                // A pencil squiggle underline.
                val p = Path().apply {
                    moveTo(10f, size.height * 0.5f)
                    var x = 10f
                    var up = true
                    while (x < size.width - 10f) {
                        val nx = x + 26f
                        quadraticTo(x + 13f, size.height * (if (up) 0.1f else 0.9f), nx, size.height * 0.5f)
                        x = nx
                        up = !up
                    }
                }
                drawPath(p, Palette.Ink, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
