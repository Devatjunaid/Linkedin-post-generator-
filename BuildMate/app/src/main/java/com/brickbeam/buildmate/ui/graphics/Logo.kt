package com.brickbeam.buildmate.ui.graphics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import com.brickbeam.buildmate.ui.theme.MaterialColors as M

/**
 * The BuildMate mark: a roofline drawn in one stroke, a 2×2 tile window, a steel I-beam,
 * three brick courses in running bond, a falling paint drop and a trowel that swings in.
 * Same geometry as the launcher icon (108-unit grid), but every part animates in on its own
 * beat. [p] is 0..1 build progress; 1 is the finished static logo.
 */
fun DrawScope.drawLogoMark(p: Float, badge: Color?, roof: Color) {
    val s = size.minDimension / 108f
    val ox = (size.width - 108f * s) / 2
    val oy = (size.height - 108f * s) / 2
    fun o(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
    fun sz(w: Float, h: Float) = Size(w * s, h * s)

    if (badge != null) {
        val bp = easeOutBack(phase(p, 0f, 0.18f))
        scale(bp, pivot = o(54f, 54f)) {
            drawRoundRect(
                Brush.linearGradient(listOf(badge, badge.copy(alpha = 0.82f)), start = o(18f, 14f), end = o(90f, 94f)),
                topLeft = o(14f, 14f), size = sz(80f, 80f), cornerRadius = CornerRadius(22f * s),
            )
        }
    }

    // 1. Roofline draws itself.
    val roofPath = Path().apply { moveTo(ox + 30f * s, oy + 50f * s); lineTo(ox + 54f * s, oy + 28f * s); lineTo(ox + 78f * s, oy + 50f * s) }
    val rp = easeInOut(phase(p, 0.08f, 0.22f))
    if (rp > 0f) {
        val measure = PathMeasure().apply { setPath(roofPath, false) }
        val seg = Path()
        measure.getSegment(0f, measure.length * rp, seg, true)
        drawPath(seg, roof, style = Stroke(5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    // 2. Brick courses drop in, bottom row first.
    val courses = listOf(
        listOf(34f to 12f, 47f to 13f, 61f to 13f) to 70f,
        listOf(34f to 6f, 41f to 13f, 55f to 12f, 68f to 6f) to 63.5f,
        listOf(34f to 12f, 47f to 13f, 61f to 13f) to 57f,
    )
    val brickColors = listOf(M.BrickLight, M.Brick, M.BrickDark, M.Brick)
    var n = 0
    courses.forEachIndexed { row, (bricks, y) ->
        bricks.forEachIndexed { i, (x, w) ->
            val d = phase(p, 0.18f + n * 0.025f, 0.14f)
            val dy = (1f - easeOutBack(d)) * -22f
            if (d > 0f) drawRect(brickColors[(row + i) % 4], o(x, y + dy), sz(w, 5.5f), alpha = d)
            n++
        }
    }

    // 3. Steel beam slides in from the left.
    val bp = easeOutCubic(phase(p, 0.45f, 0.18f))
    if (bp > 0f) {
        val dx = (1f - bp) * -40f
        drawRect(Color(0xFF8EA3B6), o(32f + dx, 48.5f), sz(44f, 2f), alpha = bp)
        drawRect(M.Steel, o(34f + dx, 50.5f), sz(40f, 2.5f), alpha = bp)
        drawRect(M.SteelDark, o(32f + dx, 53f), sz(44f, 2f), alpha = bp)
    }

    // 4. Tile window flips open, one tile at a time.
    val tiles = listOf(Triple(49f, 37f, M.Tile), Triple(54.5f, 37f, M.TileLight), Triple(49f, 42.5f, M.TileLight), Triple(54.5f, 42.5f, M.Tile))
    tiles.forEachIndexed { i, (x, y, c) ->
        val tp = easeOutCubic(phase(p, 0.55f + i * 0.04f, 0.12f))
        if (tp > 0f) scale(tp, 1f, pivot = o(x + 2.25f, y + 2.25f)) { drawRect(c, o(x, y), sz(4.5f, 4.5f)) }
    }

    // 5. Paint drop falls into place and leaves a ripple.
    val dp = phase(p, 0.62f, 0.16f)
    if (dp > 0f) {
        val fall = (1f - easeOutCubic(dp)) * -24f
        val drop = Path().apply {
            moveTo(ox + 72f * s, oy + (25f + fall) * s)
            cubicTo(ox + 72f * s, oy + (25f + fall) * s, ox + 66f * s, oy + (32.5f + fall) * s, ox + 66f * s, oy + (36.5f + fall) * s)
            cubicTo(ox + 66f * s, oy + (44.5f + fall) * s, ox + 78f * s, oy + (44.5f + fall) * s, ox + 78f * s, oy + (36.5f + fall) * s)
            cubicTo(ox + 78f * s, oy + (32.5f + fall) * s, ox + 72f * s, oy + (25f + fall) * s, ox + 72f * s, oy + (25f + fall) * s)
            close()
        }
        drawPath(drop, M.Paint, alpha = dp)
        drawCircle(M.PaintPink, 1.4f * s, o(70f, 37f + fall), alpha = dp)
        val ripple = phase(p, 0.76f, 0.2f)
        if (ripple in 0.01f..0.99f) {
            drawCircle(M.PaintPink, (6f + 10f * ripple) * s, o(72f, 37f), alpha = 1f - ripple, style = Stroke(1.2f * s))
        }
    }

    // 6. Trowel swings in from the right.
    val tp = easeOutBack(phase(p, 0.74f, 0.2f))
    if (tp > 0f) {
        rotate((1f - tp) * 50f, pivot = o(83f, 60.5f)) {
            val blade = Path().apply {
                moveTo(ox + 60f * s, oy + 75f * s); lineTo(ox + 77f * s, oy + 66.5f * s); lineTo(ox + 74.5f * s, oy + 81f * s); close()
            }
            drawPath(blade, M.SteelLight, alpha = tp.coerceIn(0f, 1f))
            drawLine(M.Wood, o(76f, 67.5f), o(83f, 60.5f), strokeWidth = 3.4f * s, cap = StrokeCap.Round, alpha = tp.coerceIn(0f, 1f))
        }
    }
}

/** Plays the build-up once (or loops when [loop] is true). */
@Composable
fun AnimatedLogo(
    modifier: Modifier,
    badge: Color?,
    roof: Color,
    durationMs: Int = 1800,
    loop: Boolean = false,
    animate: Boolean = true,
) {
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(animate, loop) {
        if (!animate) { progress.snapTo(1f); return@LaunchedEffect }
        do {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMs, easing = LinearEasing))
            if (loop) kotlinx.coroutines.delay(1400)
        } while (loop)
    }
    Canvas(modifier) { drawLogoMark(progress.value, badge, roof) }
}
