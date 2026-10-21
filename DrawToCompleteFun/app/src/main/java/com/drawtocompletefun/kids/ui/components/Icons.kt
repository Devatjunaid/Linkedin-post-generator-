package com.drawtocompletefun.kids.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Hand drawn icon set (24x24 grid), so the game ships no third party artwork. */
enum class Ico {
    Play, Back, Close, Map, Sticker, Gift, SoundOn, SoundOff, MusicOn, MusicOff, Settings,
    Bulb, Check, Replay, Next, Lock, Video, Shield, Heart
}

@Composable
fun GameIcon(ico: Ico, tint: Color, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        scale(s, pivot = Offset.Zero) { drawIco(ico, tint) }
    }
}

private fun path(block: Path.() -> Unit): Path = Path().apply(block)

private fun DrawScope.line(color: Color, vararg c: Float, w: Float = 2.6f) {
    val p = path {
        moveTo(c[0], c[1])
        var i = 2
        while (i < c.size) {
            lineTo(c[i], c[i + 1]); i += 2
        }
    }
    drawPath(p, color, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.ring(color: Color, cx: Float, cy: Float, r: Float, w: Float = 2.6f) =
    drawCircle(color, r, Offset(cx, cy), style = Stroke(w))

private fun DrawScope.arcStroke(color: Color, cx: Float, cy: Float, r: Float, start: Float, sweep: Float, w: Float = 2.6f) =
    drawArc(
        color, start, sweep, false, Offset(cx - r, cy - r), Size(2 * r, 2 * r),
        style = Stroke(w, cap = StrokeCap.Round),
    )

fun DrawScope.drawStarShape(center: Offset, outer: Float, fill: Color, outline: Color?, outlineWidth: Float = outer * 0.12f) {
    val inner = outer * 0.48f
    val p = path {
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outer else inner
            val a = (-90f + i * 36f) * PI.toFloat() / 180f
            val x = center.x + r * cos(a)
            val y = center.y + r * sin(a)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(p, fill)
    if (outline != null) {
        drawPath(p, outline, style = Stroke(outlineWidth, join = StrokeJoin.Round))
    }
}

private fun DrawScope.drawIco(ico: Ico, c: Color) {
    when (ico) {
        Ico.Play -> drawPath(
            path { moveTo(8f, 4.5f); lineTo(20f, 12f); lineTo(8f, 19.5f); close() },
            c,
        ).also { line(c, 8f, 4.5f, 20f, 12f, 8f, 19.5f, 8f, 4.5f, w = 2.4f) }

        Ico.Back -> line(c, 15f, 5f, 8f, 12f, 15f, 19f, w = 3f)
        Ico.Next -> {
            line(c, 5f, 12f, 19f, 12f, w = 3.2f)
            line(c, 13f, 6f, 19f, 12f, 13f, 18f, w = 3.2f)
        }

        Ico.Close -> {
            line(c, 6f, 6f, 18f, 18f, w = 3f)
            line(c, 18f, 6f, 6f, 18f, w = 3f)
        }

        Ico.Map -> {
            line(c, 9f, 4f, 3f, 6f, 3f, 20f, 9f, 18f, 15f, 20f, 21f, 18f, 21f, 4f, 15f, 6f, 9f, 4f)
            line(c, 9f, 4f, 9f, 18f)
            line(c, 15f, 6f, 15f, 20f)
        }

        Ico.Sticker -> {
            line(c, 4f, 4f, 16f, 4f, 20f, 8f, 20f, 20f, 4f, 20f, 4f, 4f)
            line(c, 16f, 4f, 16f, 8f, 20f, 8f)
            ring(c, 11f, 13f, 3.5f)
        }

        Ico.Gift -> {
            line(c, 3f, 10f, 21f, 10f, 21f, 20f, 3f, 20f, 3f, 10f)
            line(c, 12f, 10f, 12f, 20f)
            line(c, 12f, 10f, 7f, 6f, 9f, 4f, 12f, 10f)
            line(c, 12f, 10f, 17f, 6f, 15f, 4f, 12f, 10f)
        }

        Ico.SoundOn, Ico.SoundOff -> {
            drawPath(path { moveTo(4f, 9f); lineTo(8f, 9f); lineTo(13f, 5f); lineTo(13f, 19f); lineTo(8f, 15f); lineTo(4f, 15f); close() }, c)
            if (ico == Ico.SoundOn) {
                arcStroke(c, 13f, 12f, 4f, -50f, 100f)
                arcStroke(c, 13f, 12f, 7.5f, -50f, 100f)
            } else {
                line(c, 16f, 9f, 22f, 15f)
                line(c, 22f, 9f, 16f, 15f)
            }
        }

        Ico.MusicOn, Ico.MusicOff -> {
            drawCircle(c, 3f, Offset(7f, 18f))
            drawCircle(c, 3f, Offset(17f, 16f))
            line(c, 10f, 18f, 10f, 5f, 20f, 3f, 20f, 16f)
            if (ico == Ico.MusicOff) line(c, 3f, 3f, 21f, 21f, w = 2.8f)
        }

        Ico.Settings -> {
            ring(c, 12f, 12f, 3.4f)
            for (i in 0 until 8) {
                val a = i * PI.toFloat() / 4f
                line(c, 12f + 6.5f * cos(a), 12f + 6.5f * sin(a), 12f + 9.5f * cos(a), 12f + 9.5f * sin(a), w = 3f)
            }
            ring(c, 12f, 12f, 6.5f, w = 2.4f)
        }

        Ico.Bulb -> {
            line(c, 9f, 18f, 15f, 18f)
            line(c, 10f, 21f, 14f, 21f)
            arcStroke(c, 12f, 9.5f, 6f, 140f, 260f)
            line(c, 7.4f, 13.4f, 9.5f, 16f, 14.5f, 16f, 16.6f, 13.4f)
        }

        Ico.Check -> line(c, 5f, 12.5f, 10f, 17.5f, 20f, 6.5f, w = 3.4f)
        Ico.Replay -> {
            arcStroke(c, 12f, 12f, 8f, -150f, 300f, w = 2.8f)
            line(c, 3.5f, 4f, 4.8f, 8.2f, 9f, 7f, w = 2.8f)
        }

        Ico.Lock -> {
            line(c, 5f, 11f, 19f, 11f, 19f, 21f, 5f, 21f, 5f, 11f)
            arcStroke(c, 12f, 8f, 4f, 180f, 180f)
            line(c, 8f, 8f, 8f, 11f)
            line(c, 16f, 8f, 16f, 11f)
        }

        Ico.Video -> {
            line(c, 3f, 6f, 16f, 6f, 16f, 18f, 3f, 18f, 3f, 6f)
            drawPath(path { moveTo(16f, 10f); lineTo(21f, 7f); lineTo(21f, 17f); lineTo(16f, 14f); close() }, c)
        }

        Ico.Shield -> {
            line(c, 12f, 3f, 20f, 6f, 20f, 12f)
            arcStroke(c, 12f, 12f, 8f, 0f, 180f)
            line(c, 4f, 12f, 4f, 6f, 12f, 3f)
            line(c, 8.5f, 12f, 11f, 14.5f, 15.5f, 9.5f)
        }

        Ico.Heart -> drawPath(
            path {
                moveTo(12f, 20f)
                cubicTo(4f, 14f, 2f, 10f, 4f, 6.5f)
                cubicTo(6f, 3.5f, 10f, 4f, 12f, 7.5f)
                cubicTo(14f, 4f, 18f, 3.5f, 20f, 6.5f)
                cubicTo(22f, 10f, 20f, 14f, 12f, 20f)
                close()
            },
            c,
        )
    }
}
