package com.brickbeam.buildbros.ui.graphics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.brickbeam.buildbros.calc.TilePattern
import com.brickbeam.buildbros.ui.theme.LocalMotion
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import com.brickbeam.buildbros.ui.theme.MaterialColors as M

/** Replays a 0→1 build animation whenever [key] changes. */
@Composable
private fun rememberBuild(key: Any, durationMs: Int): Animatable<Float, *> {
    val motion = LocalMotion.current
    val a = remember { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(key, motion) {
        if (!motion) { a.snapTo(1f); return@LaunchedEffect }
        a.snapTo(0f)
        a.animateTo(1f, tween(durationMs, easing = FastOutSlowInEasing))
    }
    return a
}

data class WallOpeningSpec(val widthM: Double, val heightM: Double)

/**
 * A to-scale elevation of the wall: running-bond courses, doors and windows cut out.
 * Very small units (e.g. a 30 m wall) are drawn proportionally larger so they stay visible.
 */
@Composable
fun WallPreview(
    lengthM: Double,
    heightM: Double,
    openings: List<WallOpeningSpec>,
    unitLengthMm: Double,
    unitHeightMm: Double,
    jointMm: Double,
    background: Color,
    surface: Color,
    accent: Color,
    modifier: Modifier,
) {
    val build = rememberBuild(listOf(lengthM, heightM, openings, unitLengthMm, unitHeightMm, jointMm), 1400)
    Canvas(modifier) {
        if (lengthM <= 0 || heightM <= 0) return@Canvas
        val aspect = (lengthM / heightM).coerceIn(0.6, 6.0).toFloat()
        val w0 = size.width
        val h0 = size.height
        val (ww, wh) = if (w0 / h0 > aspect) (h0 * aspect) to h0 else w0 to (w0 / aspect)
        val left = (w0 - ww) / 2
        val top = (h0 - wh) / 2
        val sx = ww / lengthM.toFloat()
        val sy = wh / heightM.toFloat()
        drawRoundRect(background, Offset(left, top), Size(ww, wh), CornerRadius(6f))

        // Openings spread evenly; tall ones (doors) stand on the floor, others are windows.
        val rects = ArrayList<Rect>()
        val totalW = openings.sumOf { it.widthM }.toFloat()
        val gap = ((lengthM.toFloat() - totalW) / (openings.size + 1)).coerceAtLeast(0.05f)
        var cursor = gap
        openings.forEach { o ->
            val ow = o.widthM.toFloat().coerceAtMost(lengthM.toFloat())
            val oh = o.heightM.toFloat().coerceAtMost(heightM.toFloat())
            val isDoor = oh >= heightM * 0.7
            val bottom = if (isDoor) heightM.toFloat() else (heightM.toFloat() - (heightM.toFloat() - oh) * 0.45f)
            rects += Rect(left + cursor * sx, top + (bottom - oh) * sy, left + (cursor + ow) * sx, top + bottom * sy)
            cursor += ow + gap
        }

        val courseM = ((unitHeightMm + jointMm) / 1000).toFloat()
        val unitM = ((unitLengthMm + jointMm) / 1000).toFloat()
        val k = max(1f, ceil(9f / (courseM * sy)))   // visual upscaling for tiny units
        val ch = courseM * sy * k
        val bw = unitM * sx * k
        val jointPx = max(1f, (jointMm / 1000).toFloat() * sy * k)
        val rows = ceil(wh / ch).toInt()
        val colors = listOf(M.Brick, M.BrickDark, M.BrickLight, M.Brick, Color(0xFFB9502E))
        clipRect(left, top, left + ww, top + wh) {
            for (r in 0 until rows) {
                val d = phase(build.value, r.toFloat() / (rows + 2), 2.5f / (rows + 2))
                if (d <= 0f) continue
                val y = top + wh - (r + 1) * ch
                val dy = (1f - easeOutBack(d)) * -ch * 1.5f
                val offset = if (r % 2 == 1) -bw / 2 else 0f
                var x = left + offset
                var c = 0
                while (x < left + ww) {
                    val brick = Rect(x + jointPx / 2, y + jointPx / 2, x + bw - jointPx / 2, y + ch - jointPx / 2)
                    if (rects.none { it.overlaps(brick) }) {
                        drawRect(colors[(r * 3 + c) % colors.size], brick.topLeft.copy(y = brick.top + dy), brick.size, alpha = d)
                    }
                    x += bw
                    c++
                }
            }
        }
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
        rects.forEach { r ->
            drawRect(surface, r.topLeft, r.size)
            drawRect(accent, r.topLeft, r.size, style = Stroke(2.5f, pathEffect = dash))
        }
    }
}

/** Isometric slab: rebar mat drops in, concrete pours across, then a curing sheen. */
@Composable
fun SlabPreview(
    lengthM: Double,
    widthM: Double,
    thicknessM: Double,
    rebarSpacingM: Double,
    showRebar: Boolean,
    ink: Color,
    modifier: Modifier,
) {
    val t = rememberLoop(7000, stillAt = 0.95f)
    Canvas(modifier) {
        val l = lengthM.coerceAtLeast(0.1).toFloat()
        val w = widthM.coerceAtLeast(0.1).toFloat()
        val c30 = cos(Math.toRadians(30.0)).toFloat()
        val s30 = sin(Math.toRadians(30.0)).toFloat()
        val thickPx = 14f
        val k = min(size.width * 0.86f / ((l + w) * c30), (size.height * 0.8f - thickPx) / ((l + w) * s30))
        val cx = size.width / 2 + (w - l) * c30 * k / 2
        val cy = (size.height - (l + w) * s30 * k - thickPx) / 2
        fun iso(x: Float, y: Float) = Offset(cx + (x - y) * c30 * k, cy + (x + y) * s30 * k)
        val tv = t.value

        val top = Path().apply {
            val a = iso(0f, 0f); val b = iso(l, 0f); val c = iso(l, w); val d = iso(0f, w)
            moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close()
        }
        // Formwork (timber) box.
        val formSide = Path().apply {
            val b = iso(l, 0f); val c = iso(l, w); val d = iso(0f, w)
            moveTo(d.x, d.y); lineTo(c.x, c.y); lineTo(b.x, b.y)
            lineTo(b.x, b.y + thickPx); lineTo(c.x, c.y + thickPx); lineTo(d.x, d.y + thickPx); close()
        }
        drawPath(formSide, Color(0xFF8E6234))
        drawPath(top, Color(0xFF6F4A25).copy(alpha = 0.35f))
        drawPath(top, Color(0xFFB7834A), style = Stroke(5f))

        // Rebar mat.
        if (showRebar && rebarSpacingM > 0) {
            val drop = easeOutCubic(phase(tv, 0f, 0.16f))
            val lift = (1f - drop) * -40f
            val nx = min(28, floor(l / rebarSpacingM).toInt()).coerceAtLeast(2)
            val ny = min(28, floor(w / rebarSpacingM).toInt()).coerceAtLeast(2)
            translate(0f, lift) {
                for (i in 1 until nx) {
                    val x = l * i / nx
                    drawLine(M.Steel, iso(x, 0.03f * w), iso(x, 0.97f * w), strokeWidth = 2f, alpha = drop)
                }
                for (j in 1 until ny) {
                    val y = w * j / ny
                    drawLine(M.SteelDark, iso(0.03f * l, y), iso(0.97f * l, y), strokeWidth = 2f, alpha = drop)
                }
            }
        }

        // Concrete pour sweeps along the length.
        val pour = easeInOut(phase(tv, 0.22f, 0.38f))
        val fadeOut = 1f - phase(tv, 0.94f, 0.06f)
        if (pour > 0f) {
            val px = l * pour
            val poured = Path().apply {
                val a = iso(0f, 0f); val b = iso(px, 0f); val c = iso(px, w); val d = iso(0f, w)
                moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close()
            }
            drawPath(poured, M.Cement, alpha = fadeOut)
            val sheen = phase(tv, 0.66f, 0.26f)
            if (sheen > 0f && sheen < 1f) {
                clipPath(poured) {
                    val sxp = iso(l * sheen, 0f)
                    drawLine(
                        Color.White.copy(alpha = 0.35f * fadeOut), Offset(sxp.x - 40f, sxp.y - 60f), Offset(sxp.x + 40f, sxp.y + 140f),
                        strokeWidth = 26f, cap = StrokeCap.Round,
                    )
                }
            }
        }
        // Thickness tick.
        val e = iso(l, w)
        drawLine(ink.copy(alpha = 0.6f), Offset(e.x + 8f, e.y), Offset(e.x + 8f, e.y + thickPx), strokeWidth = 1.5f)
    }
}

/** Plan view of the floor with the tile pattern drawn to scale (radial reveal on change). */
@Composable
fun TilePlanPreview(
    lengthM: Double,
    widthM: Double,
    tileLengthMm: Double,
    tileWidthMm: Double,
    jointMm: Double,
    pattern: TilePattern,
    groutColor: Color,
    modifier: Modifier,
) {
    val build = rememberBuild(listOf(lengthM, widthM, tileLengthMm, tileWidthMm, jointMm, pattern), 1300)
    Canvas(modifier) {
        if (lengthM <= 0 || widthM <= 0 || tileLengthMm <= 0 || tileWidthMm <= 0) return@Canvas
        val aspect = (lengthM / widthM).coerceIn(0.5, 3.0).toFloat()
        val (rw, rh) = if (size.width / size.height > aspect) (size.height * aspect) to size.height else size.width to (size.width / aspect)
        val left = (size.width - rw) / 2
        val top = (size.height - rh) / 2
        val ppm = min(rw / lengthM.toFloat(), rh / widthM.toFloat())
        val k = max(1f, ceil(10f / ((tileWidthMm / 1000).toFloat() * ppm)))
        val tl = (tileLengthMm / 1000).toFloat() * ppm * k
        val tw = (tileWidthMm / 1000).toFloat() * ppm * k
        val j = max(1f, (jointMm / 1000).toFloat() * ppm * k)
        val origin = Offset(left, top)
        val maxD = hypot(rw, rh)
        val prog = build.value
        val c1 = Color(0xFFE4DED3)
        val c2 = Color(0xFFD3CBBE)
        drawRect(groutColor, origin, Size(rw, rh))

        fun tile(x: Float, y: Float, w: Float, h: Float, c: Color) {
            val dist = hypot(x - left, y - top)
            if (dist > prog * maxD * 1.05f) return
            drawRect(c, Offset(x + j / 2, y + j / 2), Size(max(0f, w - j), max(0f, h - j)))
        }

        clipRect(left, top, left + rw, top + rh) {
            when (pattern) {
                TilePattern.STRAIGHT, TilePattern.OFFSET -> {
                    var row = 0
                    var y = top
                    while (y < top + rh) {
                        var x = left - if (pattern == TilePattern.OFFSET && row % 2 == 1) tl / 2 else 0f
                        var col = 0
                        while (x < left + rw) { tile(x, y, tl, tw, if ((row + col) % 2 == 0) c1 else c2); x += tl; col++ }
                        y += tw; row++
                    }
                }
                TilePattern.DIAGONAL -> {
                    val cx = left + rw / 2
                    val cy = top + rh / 2
                    val span = hypot(rw, rh)
                    rotate(45f, pivot = Offset(cx, cy)) {
                        var row = 0
                        var y = cy - span
                        while (y < cy + span) {
                            var x = cx - span
                            var col = 0
                            while (x < cx + span) {
                                drawRect(
                                    if ((row + col) % 2 == 0) c1 else c2, Offset(x + j / 2, y + j / 2),
                                    Size(tl - j, tw - j), alpha = phase(prog, 0f, 1f),
                                )
                                x += tl; col++
                            }
                            y += tw; row++
                        }
                    }
                }
                TilePattern.HERRINGBONE -> {
                    // Lattice u=(1,1), v=(2,-2) in short-side units; H tile 2×1, V tile 1×2 at (+2,-1).
                    val unit = tw
                    val nMax = ceil((rw + rh) / unit).toInt() + 4
                    for (jj in -nMax / 2..nMax / 2) for (kk in -4..nMax) {
                        val ox = left + (kk + 2 * jj) * unit
                        val oy = top + (kk - 2 * jj) * unit
                        if (ox > left + rw + 3 * unit || oy > top + rh + 3 * unit || ox < left - 4 * unit || oy < top - 4 * unit) continue
                        tile(ox, oy, 2 * unit, unit, c1)
                        tile(ox + 2 * unit, oy - unit, unit, 2 * unit, c2)
                    }
                }
            }
        }
        drawRect(groutColor.copy(alpha = 0.9f), origin, Size(rw, rh), style = Stroke(3f))
    }
}

/** Isometric room; walls repaint (bottom-up wash) when the colour changes, roller keeps rolling. */
@Composable
fun PaintRoomPreview(wallColor: Color, floorColor: Color, ink: Color, modifier: Modifier) {
    val animated by animateColorAsState(wallColor, tween(600), label = "wall")
    val wash = rememberBuild(wallColor, 1200)
    val t = rememberLoop(2400, stillAt = 0.25f)
    Canvas(modifier) {
        val s = min(size.width / 350f, size.height / 222f)
        val ox = (size.width - 350f * s) / 2
        val oy = (size.height - 222f * s) / 2
        fun o(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
        fun poly(pts: List<Offset>) = Path().apply {
            moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) }; close()
        }
        val leftWall = poly(listOf(o(40f, 150f), o(175f, 85f), o(175f, 8f), o(40f, 73f)))
        val rightWall = poly(listOf(o(175f, 85f), o(310f, 150f), o(310f, 73f), o(175f, 8f)))
        drawPath(leftWall, Color(0xFFEDE8DF))
        drawPath(rightWall, Color(0xFFE2DCD1))
        val washTop = oy + (150f - 150f * wash.value) * s
        clipRect(0f, washTop, size.width, size.height) {
            drawPath(leftWall, animated)
            drawPath(rightWall, animated)
            drawPath(rightWall, Color.Black.copy(alpha = 0.12f))
        }
        drawPath(poly(listOf(o(70f, 136f), o(100f, 121f), o(100f, 64f), o(70f, 79f))), Color(0xFF7A5A3C))
        drawCircle(Color(0xFFE9D8B8), 1.6f * s, o(95f, 98f))
        drawPath(poly(listOf(o(220f, 92f), o(270f, 116f), o(270f, 84f), o(220f, 60f))), Color(0xFFBFE3F2))
        drawPath(poly(listOf(o(220f, 92f), o(270f, 116f), o(270f, 84f), o(220f, 60f))), Color.White, style = Stroke(2f * s))
        drawPath(poly(listOf(o(40f, 150f), o(175f, 85f), o(310f, 150f), o(175f, 215f))), floorColor)
        // Floor boards.
        for (i in 1..6) {
            val f = i / 7f
            drawLine(ink.copy(alpha = 0.08f), o(40f + 135f * f, 150f - 65f * f), o(175f + 135f * f, 215f - 65f * f), strokeWidth = 1f)
        }
        // Roller.
        val dy = -52f * (0.5f - 0.5f * cos(t.value * 2 * Math.PI.toFloat()))
        rotate(-26f, pivot = o(128f, 96f + dy)) {
            drawRoundRect(animated, o(114f, 96f + dy), Size(28f * s, 9f * s), CornerRadius(4f * s))
            drawRoundRect(Color(0xFF2A2A2A), o(114f, 96f + dy), Size(28f * s, 9f * s), CornerRadius(4f * s), style = Stroke(1.2f * s))
            val handle = Path().apply {
                val a = o(142f, 100f + dy); val b = o(148f, 100f + dy); val c = o(148f, 118f + dy); val d = o(140f, 118f + dy); val e = o(140f, 136f + dy)
                moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); lineTo(e.x, e.y)
            }
            drawPath(handle, Color(0xFF2A2A2A), style = Stroke(2f * s))
            drawRoundRect(Color(0xFFD8A24A), o(137f, 134f + dy), Size(6f * s, 16f * s), CornerRadius(2f * s))
        }
    }
}

/** sqrt helper kept for readability of iso maths elsewhere. */
internal fun isoScale(a: Float, b: Float) = sqrt(a * a + b * b)
