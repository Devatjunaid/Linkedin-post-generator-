package com.devatjunaid.dripdropdraw.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devatjunaid.dripdropdraw.audio.Sound

fun textStyle(size: TextUnit, weight: FontWeight = FontWeight.SemiBold, color: Color = Palette.Ink) =
    TextStyle(fontFamily = Fredoka, fontWeight = weight, fontSize = size, color = color)

/** Graph-paper background used everywhere. */
fun Modifier.paper(paper: Color = Palette.Paper, grid: Color = Palette.Grid, cell: Dp = 24.dp) = drawBehind {
    drawRect(paper)
    val step = cell.toPx()
    var x = 0f
    while (x < size.width) {
        drawLine(grid, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawLine(grid, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        y += step
    }
}

/**
 * A chunky cartoon button with a dark outline and a "3D" bottom edge that squashes when
 * pressed. Plays a tap sound.
 */
@Composable
fun CartoonButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Palette.White,
    height: Dp = 56.dp,
    corner: Dp = 28.dp,
    description: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val sfx = LocalSfx.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth = if (pressed) 1.dp else 5.dp
    Box(
        modifier
            .height(height + 5.dp)
            .semantics { if (description != null) contentDescription = description }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                sfx?.play(Sound.TAP)
                onClick()
            },
    ) {
        // The dark "3D" edge under the button.
        Box(
            Modifier
                .matchParentSize()
                .padding(top = 5.dp)
                .background(Palette.Ink, RoundedCornerShape(corner)),
        )
        Row(
            Modifier
                .offset(y = 5.dp - depth)
                .fillMaxWidth()
                .height(height)
                .background(color, RoundedCornerShape(corner))
                .border(3.dp, Palette.Ink, RoundedCornerShape(corner))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            content = content,
        )
    }
}

@Composable
fun IconCircleButton(
    icon: IconKind,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Palette.White,
    size: Dp = 48.dp,
) {
    CartoonButton(onClick, modifier.size(size, size + 5.dp), color, size, size / 2, description) {
        GameIcon(icon, Modifier.size(size * 0.5f))
    }
}

@Composable
fun ButtonLabel(text: String, size: TextUnit = 22.sp, color: Color = Palette.Ink) {
    Text(text, style = textStyle(size, FontWeight.Bold, color), maxLines = 1)
}

/** A pill showing a value, e.g. the coin counter. */
@Composable
fun Chip(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier
            .height(40.dp)
            .background(Palette.White, RoundedCornerShape(20.dp))
            .border(2.5.dp, Palette.Ink, RoundedCornerShape(20.dp))
            .padding(start = 6.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

// ---------------------------------------------------------------------------------------------
// Icons, drawn with paths so the app ships no image files.

enum class IconKind { HOME, RESTART, BULB, PLAY, NEXT, SOUND_ON, SOUND_OFF, MUSIC_ON, MUSIC_OFF, GRID, STAR, COIN, LOCK, CLOSE, PENCIL, PARENT, GIFT }

@Composable
fun GameIcon(kind: IconKind, modifier: Modifier = Modifier, color: Color = Palette.Ink, fill: Color = Palette.Star) {
    Canvas(modifier) { drawGameIcon(kind, color, fill) }
}

fun DrawScope.drawGameIcon(kind: IconKind, color: Color, fill: Color = Palette.Star) {
    val s = size.minDimension / 24f
    withTransform({
        translate((size.width - 24f * s) / 2f, (size.height - 24f * s) / 2f)
        scale(s, s, Offset.Zero)
    }) {
        val stroke = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun line(vararg pts: Float) {
            val p = Path()
            p.moveTo(pts[0], pts[1])
            var i = 2
            while (i < pts.size) {
                p.lineTo(pts[i], pts[i + 1]); i += 2
            }
            drawPath(p, color, style = stroke)
        }
        when (kind) {
            IconKind.HOME -> {
                line(3f, 11f, 12f, 3f, 21f, 11f)
                line(5f, 10f, 5f, 20f, 10f, 20f, 10f, 14f, 14f, 14f, 14f, 20f, 19f, 20f, 19f, 10f)
            }
            IconKind.RESTART -> {
                drawArc(color, -20f, -290f, false, Offset(4f, 4f), Size(16f, 16f), style = stroke)
                line(20f, 3.5f, 19.6f, 9f, 14.2f, 8f)
            }
            IconKind.BULB -> {
                val p = Path().apply {
                    moveTo(9f, 16f)
                    cubicTo(9f, 14f, 6f, 12.5f, 6f, 9f)
                    cubicTo(6f, 5.5f, 8.7f, 3f, 12f, 3f)
                    cubicTo(15.3f, 3f, 18f, 5.5f, 18f, 9f)
                    cubicTo(18f, 12.5f, 15f, 14f, 15f, 16f)
                    close()
                }
                drawPath(p, fill)
                drawPath(p, color, style = stroke)
                line(9.5f, 19f, 14.5f, 19f)
                line(10.5f, 21.5f, 13.5f, 21.5f)
            }
            IconKind.PLAY -> {
                val p = Path().apply { moveTo(7f, 4f); lineTo(20f, 12f); lineTo(7f, 20f); close() }
                drawPath(p, color)
                drawPath(p, color, style = stroke)
            }
            IconKind.NEXT -> {
                line(4f, 12f, 19f, 12f)
                line(13f, 6f, 19f, 12f, 13f, 18f)
            }
            IconKind.SOUND_ON, IconKind.SOUND_OFF -> {
                val p = Path().apply {
                    moveTo(3f, 9f); lineTo(7f, 9f); lineTo(12f, 4.5f); lineTo(12f, 19.5f); lineTo(7f, 15f); lineTo(3f, 15f); close()
                }
                drawPath(p, color)
                drawPath(p, color, style = stroke)
                if (kind == IconKind.SOUND_ON) {
                    drawArc(color, -45f, 90f, false, Offset(9f, 7f), Size(10f, 10f), style = stroke)
                    drawArc(color, -50f, 100f, false, Offset(7f, 3.5f), Size(17f, 17f), style = stroke)
                } else {
                    line(16f, 9f, 22f, 15f)
                    line(22f, 9f, 16f, 15f)
                }
            }
            IconKind.MUSIC_ON, IconKind.MUSIC_OFF -> {
                line(9f, 18f, 9f, 5f, 19f, 3f, 19f, 16f)
                drawOval(color, Offset(4f, 15.5f), Size(6f, 5f))
                drawOval(color, Offset(14f, 13.5f), Size(6f, 5f))
                if (kind == IconKind.MUSIC_OFF) {
                    drawLine(Palette.White, Offset(3f, 3f), Offset(21f, 21f), 5f, StrokeCap.Round)
                    line(3f, 3f, 21f, 21f)
                }
            }
            IconKind.GRID -> {
                for (r in 0..1) for (c in 0..1) {
                    drawRoundRect(
                        color, Offset(4f + c * 9f, 4f + r * 9f), Size(7f, 7f), CornerRadius(1.5f), style = stroke,
                    )
                }
            }
            IconKind.STAR -> {
                val p = starPath(12f, 12.5f, 10f, 4.3f)
                drawPath(p, fill)
                drawPath(p, color, style = Stroke(1.6f, join = StrokeJoin.Round))
            }
            IconKind.COIN -> {
                drawCircle(Palette.Star, 10f, Offset(12f, 12f))
                drawCircle(color, 10f, Offset(12f, 12f), style = Stroke(1.8f))
                drawArc(color, 45f, 270f, false, Offset(8f, 7.5f), Size(8f, 9f), style = Stroke(1.9f, cap = StrokeCap.Round))
            }
            IconKind.LOCK -> {
                drawRoundRect(color, Offset(5f, 11f), Size(14f, 10f), CornerRadius(2.5f), style = stroke)
                drawArc(color, 180f, 180f, false, Offset(8f, 4f), Size(8f, 10f), style = stroke)
                line(8f, 9f, 8f, 11f)
                line(16f, 9f, 16f, 11f)
            }
            IconKind.CLOSE -> {
                line(6f, 6f, 18f, 18f)
                line(18f, 6f, 6f, 18f)
            }
            IconKind.PENCIL -> {
                line(4f, 20f, 8f, 19f, 19f, 8f, 16f, 5f, 5f, 16f, 4f, 20f)
                line(14f, 7f, 17f, 10f)
            }
            IconKind.PARENT -> {
                drawCircle(color, 3f, Offset(8f, 7f), style = stroke)
                drawCircle(color, 2.3f, Offset(17f, 9.5f), style = stroke)
                line(3f, 20f, 3f, 16f, 5f, 13f, 11f, 13f, 13f, 16f, 13f, 20f)
                line(14f, 20f, 14f, 17f, 15.5f, 14.5f, 18.5f, 14.5f, 20.5f, 17f, 20.5f, 20f)
            }
            IconKind.GIFT -> {
                drawRoundRect(fill, Offset(4f, 10f), Size(16f, 11f), CornerRadius(1.5f))
                drawRoundRect(color, Offset(4f, 10f), Size(16f, 11f), CornerRadius(1.5f), style = stroke)
                drawRoundRect(color, Offset(3f, 7f), Size(18f, 4f), CornerRadius(1.5f), style = stroke)
                line(12f, 7f, 12f, 21f)
                drawArc(color, 180f, 180f, false, Offset(7f, 3f), Size(5f, 8f), style = stroke)
                drawArc(color, 180f, 180f, false, Offset(12f, 3f), Size(5f, 8f), style = stroke)
            }
        }
    }
}

fun starPath(cx: Float, cy: Float, outer: Float, inner: Float): Path {
    val p = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val a = Math.toRadians((-90 + i * 36).toDouble())
        val x = cx + (r * Math.cos(a)).toFloat()
        val y = cy + (r * Math.sin(a)).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    return p
}
