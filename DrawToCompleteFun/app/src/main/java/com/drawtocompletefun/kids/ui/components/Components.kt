package com.drawtocompletefun.kids.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape as UiShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtocompletefun.kids.game.Picture
import com.drawtocompletefun.kids.game.Pt
import com.drawtocompletefun.kids.game.Shape
import com.drawtocompletefun.kids.ui.theme.Palette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ------------------------------------------------------------------ Text

@Composable
fun KText(
    text: String,
    size: TextUnit = 16.sp,
    color: Color = Palette.Ink,
    weight: FontWeight = FontWeight.ExtraBold,
    align: TextAlign = TextAlign.Start,
    modifier: Modifier = Modifier,
    lineHeight: TextUnit = TextUnit.Unspecified,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = size,
            fontWeight = weight,
            fontFamily = FontFamily.SansSerif,
            textAlign = align,
            lineHeight = lineHeight,
        ),
    )
}

// ------------------------------------------------------------------ Buttons

/** Chunky 3D button: a coloured face sitting on a darker "shadow" that it presses into. */
@Composable
fun ChunkyButton(
    onClick: () -> Unit,
    color: Color,
    shadow: Color,
    modifier: Modifier = Modifier,
    shape: UiShape = RoundedCornerShape(22.dp),
    depth: Dp = 6.dp,
    label: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(80), label = "press")
    Box(
        modifier
            .semantics { if (label != null) contentDescription = label }
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
        propagateMinConstraints = true,
    ) {
        Box(
            Modifier
                .matchParentSize()
                .offset(y = depth)
                .clip(shape)
                .background(shadow),
        )
        Box(
            Modifier
                .offset(y = depth * press)
                .clip(shape)
                .background(color),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

@Composable
fun IconTile(
    ico: Ico,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Palette.Paper,
    shadow: Color = Palette.SkyShadow,
    tint: Color = Palette.Ink,
    size: Dp = 52.dp,
) {
    ChunkyButton(
        onClick = onClick,
        color = color,
        shadow = shadow,
        modifier = modifier.size(size),
        shape = RoundedCornerShape(16.dp),
        depth = 4.dp,
        label = label,
    ) { GameIcon(ico, tint, size * 0.5f) }
}

@Composable
fun StarPill(count: Int, modifier: Modifier = Modifier, background: Color = Palette.Paper) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp)
            .semantics { contentDescription = "$count stars" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StarIcon(filled = true, size = 26.dp)
        KText("$count", size = 20.sp, weight = FontWeight.Black)
    }
}

@Composable
fun StarIcon(filled: Boolean, size: Dp, modifier: Modifier = Modifier, emptyColor: Color = Palette.Paper) {
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        drawStarShape(
            center = Offset(r, r * 1.04f),
            outer = r * 0.94f,
            fill = if (filled) Palette.Yellow else emptyColor,
            outline = if (filled) Palette.YellowShadow else Palette.Locked,
        )
    }
}

@Composable
fun Row3Stars(count: Int, size: Dp, modifier: Modifier = Modifier, emptyColor: Color = Palette.Paper) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { StarIcon(filled = it < count, size = size, emptyColor = emptyColor) }
    }
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    color: Color = Palette.Paper,
    shadow: Color = Palette.SkyShadow,
    radius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier, propagateMinConstraints = true) {
        Box(Modifier.matchParentSize().offset(y = 5.dp).clip(RoundedCornerShape(radius)).background(shadow))
        Box(Modifier.clip(RoundedCornerShape(radius)).background(color), content = content)
    }
}

@Composable
fun ButtonRowLabel(ico: Ico, text: String, tint: Color, size: TextUnit = 22.sp) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GameIcon(ico, tint, 26.dp)
        KText(text, size = size, color = tint, weight = FontWeight.Black)
    }
}

@Composable
fun RowScope.Gap(w: Dp) = Spacer(Modifier.width(w))

@Composable
fun VGap(h: Dp) = Spacer(Modifier.height(h))

// ------------------------------------------------------------------ Pictures

/** Converts a board point (0..100) to canvas pixels. */
fun DrawScope.board(p: Pt): Offset {
    val s = size.minDimension / 100f
    val ox = (size.width - size.minDimension) / 2f
    val oy = (size.height - size.minDimension) / 2f
    return Offset(ox + p.x * s, oy + p.y * s)
}

fun DrawScope.shapePath(shape: Shape, progress: Float = 1f): Path {
    val pts = shape.points
    val path = Path()
    if (pts.isEmpty()) return path
    if (progress >= 1f) {
        pts.forEachIndexed { i, p -> board(p).let { if (i == 0) path.moveTo(it.x, it.y) else path.lineTo(it.x, it.y) } }
        if (shape.filled) path.close()
        return path
    }
    // Partial path, used for the "drawing itself" animation.
    var total = 0f
    for (i in 1 until pts.size) total += com.drawtocompletefun.kids.game.PictureBuilder.dist(pts[i - 1], pts[i])
    var left = total * progress
    board(pts[0]).let { path.moveTo(it.x, it.y) }
    for (i in 1 until pts.size) {
        val seg = com.drawtocompletefun.kids.game.PictureBuilder.dist(pts[i - 1], pts[i])
        if (left >= seg) {
            board(pts[i]).let { path.lineTo(it.x, it.y) }
            left -= seg
        } else {
            val f = if (seg == 0f) 0f else left / seg
            val p = Pt(pts[i - 1].x + (pts[i].x - pts[i - 1].x) * f, pts[i - 1].y + (pts[i].y - pts[i - 1].y) * f)
            board(p).let { path.lineTo(it.x, it.y) }
            break
        }
    }
    return path
}

fun DrawScope.drawShape(shape: Shape, color: Color, width: Float, progress: Float = 1f) {
    val path = shapePath(shape, progress)
    if (shape.filled) {
        drawPath(path, color)
    } else {
        drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

fun DrawScope.drawDashedShape(shape: Shape, color: Color, width: Float, phase: Float = 0f) {
    drawPath(
        shapePath(shape),
        color,
        style = Stroke(
            width,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(width * 0.4f, width * 2.2f), phase),
        ),
    )
}

/**
 * Draws a picture. [hidden] lines are skipped, [colored] lines use their own colour
 * (lines the player has drawn), everything else uses [ink].
 */
fun DrawScope.drawPicture(
    picture: Picture,
    ink: Color = Palette.Ink,
    hidden: Set<String> = emptySet(),
    colored: Map<String, Color> = emptyMap(),
    progress: Map<String, Float> = emptyMap(),
    widthFactor: Float = 1f,
) {
    val w = size.minDimension * 0.022f * widthFactor
    picture.shapes.forEach { s ->
        if (s.tag in hidden) return@forEach
        val color = colored[s.tag] ?: ink
        val width = if (s.tag in colored) w * 1.25f else w
        drawShape(s, color, width, progress[s.tag] ?: 1f)
    }
}

@Composable
fun PictureView(
    picture: Picture,
    modifier: Modifier = Modifier,
    ink: Color = Palette.Ink,
    hidden: Set<String> = emptySet(),
    colored: Map<String, Color> = emptyMap(),
    widthFactor: Float = 1f,
) {
    Canvas(modifier) { drawPicture(picture, ink, hidden, colored, widthFactor = widthFactor) }
}

// ------------------------------------------------------------------ Mascot "Pip" the pencil

@Composable
fun Pip(modifier: Modifier = Modifier, happy: Boolean = true, bob: Boolean = true) {
    val t = rememberInfiniteTransition(label = "pip")
    val y by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )
    Canvas(
        modifier.graphicsLayer {
            if (bob) {
                translationY = -10f * y * density
                rotationZ = -12f + 6f * y
            } else {
                rotationZ = -10f
            }
        },
    ) { drawPip(happy) }
}

fun DrawScope.drawPip(happy: Boolean) {
    // Drawn on a 64 x 150 grid, scaled to fit.
    val s = minOf(size.width / 64f, size.height / 150f)
    drawPipGrid(happy, (size.width - 64f * s) / 2f, (size.height - 150f * s) / 2f, s)
}

/** Draws Pip so that the pencil tip touches [tip]. */
fun DrawScope.drawPipAt(tip: Offset, height: Float, happy: Boolean) {
    val s = height / 150f
    drawPipGrid(happy, tip.x - 32f * s, tip.y - 144f * s, s)
}

private fun DrawScope.drawPipGrid(happy: Boolean, ox: Float, oy: Float, s: Float) {
    fun o(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
    val ink = Palette.Ink
    val stroke = Stroke(4f * s, join = StrokeJoin.Round, cap = StrokeCap.Round)
    fun rect(x: Float, y: Float, w: Float, h: Float, fill: Color) {
        drawRect(fill, o(x, y), androidx.compose.ui.geometry.Size(w * s, h * s))
        drawRect(ink, o(x, y), androidx.compose.ui.geometry.Size(w * s, h * s), style = stroke)
    }
    rect(10f, 6f, 44f, 20f, Palette.Pink)
    rect(10f, 26f, 44f, 10f, Color(0xFFC9CEDB))
    rect(10f, 36f, 44f, 74f, Palette.Yellow)
    val tip = Path().apply {
        o(10f, 110f).let { moveTo(it.x, it.y) }
        o(54f, 110f).let { lineTo(it.x, it.y) }
        o(32f, 144f).let { lineTo(it.x, it.y) }
        close()
    }
    drawPath(tip, Color(0xFFF5CBA7))
    drawPath(tip, ink, style = stroke)
    val lead = Path().apply {
        o(26f, 134.7f).let { moveTo(it.x, it.y) }
        o(38f, 134.7f).let { lineTo(it.x, it.y) }
        o(32f, 144f).let { lineTo(it.x, it.y) }
        close()
    }
    drawPath(lead, ink)
    drawCircle(ink, 5f * s, o(24f, 62f))
    drawCircle(ink, 5f * s, o(40f, 62f))
    drawCircle(Color.White, 1.7f * s, o(25.5f, 60.5f))
    drawCircle(Color.White, 1.7f * s, o(41.5f, 60.5f))
    drawCircle(Palette.Pink.copy(alpha = 0.7f), 4f * s, o(17f, 72f))
    drawCircle(Palette.Pink.copy(alpha = 0.7f), 4f * s, o(47f, 72f))
    val mouth = Path().apply {
        if (happy) {
            o(22f, 75f).let { moveTo(it.x, it.y) }
            o(32f, 86f).let { c -> o(42f, 75f).let { e -> cubicTo(c.x, c.y, c.x, c.y, e.x, e.y) } }
        } else {
            o(24f, 82f).let { moveTo(it.x, it.y) }
            o(32f, 75f).let { c -> o(40f, 82f).let { e -> cubicTo(c.x, c.y, c.x, c.y, e.x, e.y) } }
        }
    }
    drawPath(mouth, ink, style = stroke)
}

// ------------------------------------------------------------------ Confetti

private class Bit(val x: Float, val speed: Float, val delay: Float, val spin: Float, val color: Color, val w: Float)

@Composable
fun Confetti(modifier: Modifier = Modifier, pieces: Int = 40) {
    val bits = remember {
        val r = Random(7)
        List(pieces) {
            Bit(r.nextFloat(), 0.6f + r.nextFloat() * 0.8f, r.nextFloat(), r.nextFloat() * 720f, Palette.Confetti[it % Palette.Confetti.size], 0.6f + r.nextFloat() * 0.6f)
        }
    }
    val t = rememberInfiniteTransition(label = "confetti")
    val time by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3400, easing = LinearEasing)), label = "time")
    Canvas(modifier.fillMaxSize()) {
        bits.forEach { b ->
            val p = ((time * b.speed + b.delay) % 1f)
            val y = -30f + p * (size.height + 60f)
            val x = b.x * size.width + sin((p * 6f + b.delay * 10f).toDouble()).toFloat() * 18f
            val angle = (p * b.spin) * PI.toFloat() / 180f
            val w = 10f * b.w * density / 2f
            val h = 16f * b.w * density / 2f
            val dx = cos(angle) * w
            val path = Path().apply {
                moveTo(x - dx, y - h / 2)
                lineTo(x + dx, y - h / 2)
                lineTo(x + dx, y + h / 2)
                lineTo(x - dx, y + h / 2)
                close()
            }
            drawPath(path, b.color)
        }
    }
}

/** Little sparkle burst used when a line snaps into place. */
fun DrawScope.drawSparkles(center: Offset, progress: Float, color: Color) {
    if (progress <= 0f || progress >= 1f) return
    val r = size.minDimension * 0.08f * progress
    val alpha = 1f - progress
    for (i in 0 until 8) {
        val a = i * PI.toFloat() / 4f
        val p = Offset(center.x + r * cos(a), center.y + r * sin(a))
        drawStarShape(p, size.minDimension * 0.018f * (1f - progress * 0.5f), color.copy(alpha = alpha), null)
    }
}

/** Soft white clouds that drift slowly. */
@Composable
fun Clouds(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "clouds")
    val x by t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "x")
    Canvas(modifier.fillMaxSize()) {
        fun cloud(cx: Float, cy: Float, s: Float) {
            drawOval(Color.White, Offset(cx - 36 * s, cy - 18 * s), androidx.compose.ui.geometry.Size(72 * s, 36 * s))
            drawOval(Color.White, Offset(cx - 5 * s, cy - 26 * s), androidx.compose.ui.geometry.Size(60 * s, 40 * s))
        }
        val d = density
        cloud(size.width * 0.08f + x * 18f * d, size.height * 0.16f, d)
        cloud(size.width * 0.82f - x * 18f * d, size.height * 0.40f, d * 0.9f)
        cloud(size.width * 0.15f + x * 10f * d, size.height * 0.62f, d * 0.7f)
    }
}
