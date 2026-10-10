package com.dotdash.kids.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import com.dotdash.kids.game.ArtCircle
import com.dotdash.kids.game.ArtEye
import com.dotdash.kids.game.ArtFill
import com.dotdash.kids.game.ArtLine
import com.dotdash.kids.game.ArtOval
import com.dotdash.kids.game.ArtRect
import com.dotdash.kids.game.ArtStar
import com.dotdash.kids.game.ArtText
import com.dotdash.kids.game.Picture
import com.dotdash.kids.game.Pt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Shared drawing helpers. Pictures use a 100×100 box, Zip a 120×120 box and
// icons a 24×24 box; callers fit those boxes into real pixels.
// ---------------------------------------------------------------------------

object Paths {
    private val cache = HashMap<String, Path>()
    fun svg(d: String): Path = cache.getOrPut(d) { PathParser().parsePathString(d).toPath() }

    fun polygon(pts: List<Pt>): Path = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
        close()
    }

    fun star(cx: Float, cy: Float, r: Float, inner: Float = 0.45f): Path = Path().apply {
        for (i in 0 until 10) {
            val a = PI * i / 5 - PI / 2
            val rr = if (i % 2 == 0) r else r * inner
            val x = cx + rr * cos(a).toFloat()
            val y = cy + rr * sin(a).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

private val polyCache = HashMap<String, Path>()
fun Picture.outline(): Path = polyCache.getOrPut(id) { Paths.polygon(pts) }

/** Fits a design box (x0, y0, w, h) into this DrawScope, centred, and draws in its units. */
inline fun DrawScope.inBox(x0: Float, y0: Float, w: Float, h: Float, block: DrawScope.() -> Unit) {
    val s = min(size.width / w, size.height / h)
    val dx = (size.width - w * s) / 2 - x0 * s
    val dy = (size.height - h * s) / 2 - y0 * s
    withTransform({
        translate(dx, dy)
        scale(s, s, Offset.Zero)
    }) { block() }
}

/** The finished, coloured picture in its 100-unit box. */
fun DrawScope.drawPicture(pic: Picture, fill: Color, textMeasurer: TextMeasurer?, outline: Float = 1.6f) {
    val body = pic.outline()
    drawPath(body, fill)
    drawPath(body, C.Ink, style = Stroke(outline, join = StrokeJoin.Round))
    for (a in pic.art) {
        when (a) {
            is ArtCircle -> drawCircle(hex(a.color), a.r, Offset(a.x, a.y))
            is ArtOval -> drawOval(hex(a.color), Offset(a.x - a.rx, a.y - a.ry), Size(a.rx * 2, a.ry * 2))
            is ArtFill -> drawPath(Paths.svg(a.d), hex(a.color))
            is ArtLine -> drawPath(Paths.svg(a.d), hex(a.color), style = Stroke(a.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
            is ArtRect -> drawRoundRect(hex(a.color), Offset(a.x, a.y), Size(a.w, a.h), CornerRadius(a.rx, a.rx))
            is ArtStar -> drawPath(Paths.star(a.x, a.y, a.r), hex(a.color))
            is ArtEye -> drawEye(a.x, a.y, a.s)
            is ArtText -> if (textMeasurer != null) {
                val style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = a.size.toSp(), color = hex(a.color))
                val layout = textMeasurer.measure(a.text, style)
                drawText(layout, topLeft = Offset(a.x - layout.size.width / 2f, a.y - layout.size.height * 0.78f))
            }
        }
    }
}

fun DrawScope.drawEye(x: Float, y: Float, s: Float) {
    drawCircle(Color.White, 3.3f * s, Offset(x, y))
    drawCircle(C.Ink, 1.9f * s, Offset(x + 0.5f * s, y + 0.4f * s))
    drawCircle(Color.White, 0.7f * s, Offset(x + 1.1f * s, y - 0.4f * s))
}

/** A picture drawn into a square, as used by stickers, map nodes and cards. */
@Composable
fun PictureIcon(pic: Picture, size: Dp, fill: Color = hex(pic.fill), modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()
    Canvas(modifier.size(size)) {
        inBox(-6f, -6f, 112f, 112f) { drawPicture(pic, fill, tm, 2.2f) }
    }
}

/** Solid silhouette, for the "still hiding" peek on the exit screen. */
@Composable
fun Silhouette(pic: Picture, size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        inBox(-6f, -6f, 112f, 112f) {
            val p = pic.outline()
            drawPath(p, color)
            drawPath(p, color, style = Stroke(2f, join = StrokeJoin.Round))
        }
    }
}

// --- Zip the firefly (120-unit box) -----------------------------------------
fun DrawScope.drawZip(gold: Boolean = false, sad: Boolean = false, wing: Float = 1f) {
    val body = if (gold) Color(0xFFFFD34D) else Color(0xFF7B5CFF)
    val thorax = if (gold) Color(0xFFFFB020) else C.Coral
    val tail = if (gold) Color(0xFFFFF3B0) else Color(0xFFFFE14D)
    // soft glow around the tail
    drawCircle(tail.copy(alpha = 0.22f), 34f, Offset(60f, 84f))
    drawCircle(tail.copy(alpha = 0.18f), 26f, Offset(60f, 84f))
    // wings flap by squashing vertically
    val wingH = 22f * wing
    rotate(-30f, Offset(34f, 50f)) { drawOval(Color.White.copy(alpha = 0.8f), Offset(14f, 50f - wingH / 2), Size(40f, wingH)) }
    rotate(30f, Offset(86f, 50f)) { drawOval(Color.White.copy(alpha = 0.8f), Offset(66f, 50f - wingH / 2), Size(40f, wingH)) }
    drawOval(tail, Offset(40f, 65f), Size(40f, 38f))
    drawOval(thorax, Offset(39f, 44f), Size(42f, 32f))
    drawPath(Paths.svg("M50 20 Q44 8 36 8 M70 20 Q76 8 84 8"), Color.White, style = Stroke(3f, cap = StrokeCap.Round))
    drawCircle(C.Sun, 4.5f, Offset(36f, 8f))
    drawCircle(C.Sun, 4.5f, Offset(84f, 8f))
    drawCircle(body, 20f, Offset(60f, 36f))
    drawCircle(Color.White, 7f, Offset(52f, 34f))
    drawCircle(Color.White, 7f, Offset(68f, 34f))
    drawCircle(C.Ink, 3.5f, Offset(53f, 35f))
    drawCircle(C.Ink, 3.5f, Offset(69f, 35f))
    drawCircle(Color.White, 1.2f, Offset(54.5f, 33.5f))
    drawCircle(Color.White, 1.2f, Offset(70.5f, 33.5f))
    val mouth = if (sad) "M53 48 Q60 43 67 48" else "M53 45 Q60 51 67 45"
    drawPath(Paths.svg(mouth), Color.White, style = Stroke(2.5f, cap = StrokeCap.Round))
    if (sad) drawPath(Paths.svg("M46 25 L56 28 M74 25 L64 28"), Color.White, style = Stroke(2f, cap = StrokeCap.Round))
    if (gold) {
        val crown = Paths.svg("M48 18 L52 10 L56 16 L60 8 L64 16 L68 10 L72 18 Z")
        drawPath(crown, C.Sun)
        drawPath(crown, Color(0xFFE0A800), style = Stroke(1.5f, join = StrokeJoin.Round))
    }
}

@Composable
fun ZipView(size: Dp, modifier: Modifier = Modifier, gold: Boolean = false, sad: Boolean = false) {
    val t = rememberInfiniteTransition(label = "wings")
    val wing by t.animateFloat(1f, 0.6f, infiniteRepeatable(tween(250, easing = LinearEasing), RepeatMode.Reverse), label = "flap")
    Canvas(modifier.size(size)) {
        inBox(0f, 0f, 120f, 120f) { drawZip(gold, sad, wing) }
    }
}

// --- Stars --------------------------------------------------------------------
fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    val p = Paths.star(center.x, center.y, radius)
    drawPath(p, color)
    drawPath(p, color, style = Stroke(radius * 0.17f, join = StrokeJoin.Round))
}

@Composable
fun StarIcon(size: Dp, on: Boolean = true, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        drawStar(center, this.size.minDimension * 0.44f, if (on) C.Sun else C.Night3)
    }
}

// --- Line icons (24-unit box) ----------------------------------------------------
enum class Ic(val d: String, val filled: Boolean = false) {
    Back("M15 5l-7 7 7 7"),
    Next("M9 5l7 7-7 7"),
    Play("M7 4.5v15l12.5-7.5z", filled = true),
    Sound("M4 9h4l5-4v14l-5-4H4z M16.5 8.5a5 5 0 0 1 0 7 M19 6a8.5 8.5 0 0 1 0 12"),
    Mute("M4 9h4l5-4v14l-5-4H4z M17 9l5 6 M22 9l-5 6"),
    Music("M9 18V5l11-2v13 M3 18a3 3 0 1 0 6 0a3 3 0 1 0 -6 0 M14 16a3 3 0 1 0 6 0a3 3 0 1 0 -6 0"),
    NoMusic("M9 18V5l11-2v13 M3 18a3 3 0 1 0 6 0a3 3 0 1 0 -6 0 M14 16a3 3 0 1 0 6 0a3 3 0 1 0 -6 0 M3 3l18 18"),
    Hint("M9 18h6 M10 21h4 M12 3a6 6 0 0 0 -3.5 10.9c.6 .5 1 1.2 1 2.1h5c0 -.9 .4 -1.6 1 -2.1A6 6 0 0 0 12 3z"),
    Speak("M4 9h4l5-4v14l-5-4H4z M16.5 8.5a5 5 0 0 1 0 7"),
    Gift("M5 9h14a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-8a2 2 0 0 1 2 -2z M3 13h18 M12 9v12 M12 9C10 5 6 5 6.5 7.5S12 9 12 9z M12 9c2 -4 6 -4 5.5 -1.5S12 9 12 9z"),
    Book("M4 19.5V5a2 2 0 0 1 2 -2h14v16H6.5A2.5 2.5 0 0 0 4 21.5 M12 7.5l1.2 2.4 2.6 .4 -1.9 1.8 .5 2.6 -2.4 -1.3 -2.4 1.3 .5 -2.6 -1.9 -1.8 2.6 -.4z"),
    Lock("M7.5 11h9a2.5 2.5 0 0 1 2.5 2.5v5a2.5 2.5 0 0 1 -2.5 2.5h-9a2.5 2.5 0 0 1 -2.5 -2.5v-5a2.5 2.5 0 0 1 2.5 -2.5z M8 11V8a4 4 0 0 1 8 0v3"),
    Replay("M4 12a8 8 0 1 0 2.5 -5.8 M4 4v5h5"),
    Paint("M18.5 3.5l2 2L10 16l-3 1 1-3z M7 17c-2 0 -3 1.5 -3 3.5 2 0 3.5 -1 3.5 -3"),
    Map("M9 4L3 6v14l6-2 6 2 6-2V4l-6 2z M9 4v14 M15 6v14"),
    Check("M5 12.5l4.5 4.5L19 7.5"),
}

@Composable
fun Icon24(icon: Ic, size: Dp, color: Color, modifier: Modifier = Modifier, stroke: Float = 2.6f) {
    Canvas(modifier.size(size)) {
        inBox(0f, 0f, 24f, 24f) {
            val p = Paths.svg(icon.d)
            if (icon.filled) drawPath(p, color)
            drawPath(p, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
