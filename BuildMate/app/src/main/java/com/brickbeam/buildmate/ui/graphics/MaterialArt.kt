package com.brickbeam.buildmate.ui.graphics

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.brickbeam.buildmate.ui.theme.MaterialColors as M

/*
 * Animated material illustrations. Every piece is drawn in a 64 × 44 unit box (like the
 * mockup SVGs) and scaled to the Canvas, so the same art works as a small tile icon or a
 * large hero graphic. `t` is a 0..1 loop clock.
 */

private class Units(val scope: DrawScope, w: Float = 64f, h: Float = 44f) {
    val s = minOf(scope.size.width / w, scope.size.height / h)
    val ox = (scope.size.width - w * s) / 2
    val oy = (scope.size.height - h * s) / 2
    fun x(v: Float) = ox + v * s
    fun y(v: Float) = oy + v * s
    fun o(px: Float, py: Float) = Offset(x(px), y(py))
    fun sz(w: Float, h: Float) = Size(w * s, h * s)
    fun r(v: Float) = CornerRadius(v * s, v * s)
}

private fun DrawScope.brick(u: Units, x: Float, y: Float, w: Float, h: Float, c: Color, dy: Float = 0f, alpha: Float = 1f) {
    drawRoundRect(c, u.o(x, y + dy), u.sz(w, h), u.r(1.6f), alpha = alpha)
    drawRect(Color.White.copy(alpha = 0.12f * alpha), u.o(x, y + dy), u.sz(w, h * 0.22f))
}

fun DrawScope.drawBrickStack(t: Float) {
    val u = Units(this)
    val bricks = listOf(
        floatArrayOf(2f, 30f, 19f), floatArrayOf(23f, 30f, 19f), floatArrayOf(44f, 30f, 18f),
        floatArrayOf(12f, 18f, 19f), floatArrayOf(33f, 18f, 19f), floatArrayOf(22f, 6f, 19f),
    )
    val colors = listOf(M.Brick, M.BrickDark, M.Brick, M.BrickLight, M.Brick, M.BrickDark)
    val fade = 1f - phase(t, 0.88f, 0.12f)
    bricks.forEachIndexed { i, b ->
        val d = phase(t, i * 0.07f, 0.16f)
        val dy = (1f - easeOutBack(d)) * -26f
        brick(u, b[0], b[1], b[2], 10f, colors[i], dy, alpha = d * fade)
    }
}

fun DrawScope.drawCementPour(t: Float) {
    val u = Units(this)
    // Heap breathes as the stream lands.
    val heapScale = 0.55f + 0.45f * (0.5f + 0.5f * wave(t))
    scale(1f, heapScale, pivot = u.o(33f, 42f)) {
        val heap = Path().apply {
            moveTo(u.x(14f), u.y(42f))
            quadraticTo(u.x(33f), u.y(22f), u.x(52f), u.y(42f))
            close()
        }
        drawPath(heap, M.CementDark)
    }
    // Dashed stream.
    drawLine(
        M.Cement, u.o(22f, 22f), u.o(28f, 37f), strokeWidth = 3f * u.s, cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * u.s, 4f * u.s), -t * 24f * u.s * 4),
    )
    // Swaying bag.
    rotate(wave(t) * 5f, pivot = u.o(18f, 2f)) {
        val bag = Path().apply {
            moveTo(u.x(6f), u.y(2f)); lineTo(u.x(30f), u.y(2f)); lineTo(u.x(33f), u.y(22f)); lineTo(u.x(3f), u.y(22f)); close()
        }
        drawPath(bag, M.Cement)
        drawRoundRect(M.CementDark, u.o(9f, 8f), u.sz(18f, 6f), u.r(1f))
    }
}

fun DrawScope.drawTileFlip(t: Float) {
    val u = Units(this)
    for (i in 0 until 6) {
        val col = i % 3
        val row = i / 3
        val start = i * 0.06f
        val open = easeOutCubic(phase(t, start, 0.2f)) * (1f - phase(t, 0.85f + i * 0.02f, 0.1f))
        val x = 4f + col * 19f
        val y = 4f + row * 19f
        val c = if ((col + row) % 2 == 0) M.Tile else M.TileLight
        scale(open.coerceAtLeast(0.02f), 1f, pivot = u.o(x + 8.5f, y + 8.5f)) {
            drawRoundRect(c, u.o(x, y), u.sz(17f, 17f), u.r(2f))
            drawRoundRect(Color.White.copy(alpha = 0.18f), u.o(x + 2f, y + 2f), u.sz(5f, 2f), u.r(1f))
        }
    }
}

fun DrawScope.drawPaintBucket(t: Float, mute: Color) {
    val u = Units(this)
    val handle = Path().apply { moveTo(u.x(12f), u.y(10f)); quadraticTo(u.x(29f), u.y(-4f), u.x(46f), u.y(10f)) }
    drawPath(handle, mute, style = Stroke(2f * u.s))
    val can = Path().apply {
        moveTo(u.x(14f), u.y(10f)); lineTo(u.x(44f), u.y(10f)); lineTo(u.x(44f), u.y(36f))
        quadraticTo(u.x(44f), u.y(40f), u.x(40f), u.y(40f)); lineTo(u.x(18f), u.y(40f))
        quadraticTo(u.x(14f), u.y(40f), u.x(14f), u.y(36f)); close()
    }
    drawPath(can, M.Paint)
    val lip = Path().apply {
        moveTo(u.x(14f), u.y(10f)); lineTo(u.x(44f), u.y(10f)); lineTo(u.x(44f), u.y(17f))
        quadraticTo(u.x(40f), u.y(20f), u.x(37f), u.y(17f)); quadraticTo(u.x(33f), u.y(21f), u.x(29f), u.y(17f))
        quadraticTo(u.x(25f), u.y(20f), u.x(22f), u.y(17f)); quadraticTo(u.x(18f), u.y(21f), u.x(14f), u.y(17f)); close()
    }
    drawPath(lip, M.PaintPink)
    listOf(20f to 0f, 34f to 0.37f).forEach { (x, delay) ->
        val d = ((t + delay) % 1f)
        val len = 4f + 8f * easeOutCubic(d)
        drawRoundRect(M.PaintPink, u.o(x, 16f + d * 6f), u.sz(3f, len), u.r(1.5f), alpha = 1f - d)
    }
}

fun DrawScope.drawSteelBars(t: Float) {
    val u = Units(this)
    val colors = listOf(M.Steel, M.SteelDark, M.Steel, M.Rust)
    colors.forEachIndexed { i, c ->
        val d = easeOutCubic(phase(t, i * 0.08f, 0.25f))
        val fade = 1f - phase(t, 0.85f, 0.15f)
        val y = 8f + i * 9f
        val dx = (1f - d) * -26f
        drawRoundRect(c, u.o(2f + dx, y), u.sz(60f, 5f), u.r(2.5f), alpha = d * fade)
        // Ribs.
        for (k in 0 until 10) {
            drawLine(
                Color.Black.copy(alpha = 0.18f * d * fade), u.o(6f + dx + k * 6f, y), u.o(8f + dx + k * 6f, y + 5f),
                strokeWidth = 0.8f * u.s,
            )
        }
    }
    // Moving metal glint.
    val gx = -10f + 80f * phase(t, 0.35f, 0.4f)
    clipRect(u.x(2f), u.y(8f), u.x(62f), u.y(40f)) {
        drawRect(
            Brush.horizontalGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent),
                startX = u.x(gx), endX = u.x(gx + 10f),
            ),
            topLeft = u.o(gx, 8f), size = u.sz(10f, 32f),
        )
    }
}

fun DrawScope.drawRulerArt(t: Float, ink: Color, accent: Color) {
    val u = Units(this)
    drawRoundRect(Color(0xFFE9C46A), u.o(2f, 14f), u.sz(60f, 16f), u.r(2f))
    for (k in 0..14) {
        val x = 4f + k * 4f
        val h = if (k % 2 == 0) 7f else 4f
        drawLine(ink.copy(alpha = 0.7f), u.o(x, 14f), u.o(x, 14f + h), strokeWidth = 0.8f * u.s)
    }
    val mx = 6f + 52f * (0.5f + 0.5f * wave(t))
    drawLine(accent, u.o(mx, 8f), u.o(mx, 36f), strokeWidth = 2f * u.s, cap = StrokeCap.Round)
    drawCircle(accent, 2.6f * u.s, u.o(mx, 8f))
}

fun DrawScope.drawEstimateArt(t: Float, fg: Color) {
    val u = Units(this)
    drawRoundRect(fg, u.o(12f, 2f), u.sz(36f, 40f), u.r(5f), style = Stroke(2.5f * u.s))
    listOf(14f, 26f).forEachIndexed { i, y ->
        val d = phase(t, 0.1f + i * 0.2f, 0.2f)
        val p = Path().apply { moveTo(u.x(19f), u.y(y)); lineTo(u.x(22f), u.y(y + 3f)); lineTo(u.x(28f), u.y(y - 3f)) }
        drawPath(p, fg.copy(alpha = d), style = Stroke(2.5f * u.s, cap = StrokeCap.Round))
        drawLine(fg, u.o(32f, y + 1f), u.o(42f, y + 1f), strokeWidth = 2.5f * u.s, cap = StrokeCap.Round)
    }
    drawLine(fg, u.o(19f, 36f), u.o(19f + 23f * phase(t, 0.5f, 0.3f), 36f), strokeWidth = 2.5f * u.s, cap = StrokeCap.Round)
}

/** The wide hero scene on Home: a wall building itself, a spinning mixer, steel, paint, tiles. */
fun DrawScope.drawHeroScene(t: Float, ground: Color, ink: Color) {
    val u = Units(this, 342f, 92f)
    drawRoundRect(ground, u.o(0f, 84f), u.sz(342f, 8f), u.r(4f))
    val wall = listOf(
        floatArrayOf(14f, 70f), floatArrayOf(46f, 70f), floatArrayOf(78f, 70f), floatArrayOf(110f, 70f),
        floatArrayOf(30f, 55f), floatArrayOf(62f, 55f), floatArrayOf(94f, 55f),
        floatArrayOf(46f, 40f), floatArrayOf(78f, 40f), floatArrayOf(62f, 25f),
    )
    val cs = listOf(M.Brick, M.BrickDark, M.Brick, M.BrickLight)
    val fade = 1f - phase(t, 0.9f, 0.1f)
    wall.forEachIndexed { i, b ->
        val d = phase(t, i * 0.045f, 0.12f)
        val dy = (1f - easeOutBack(d)) * -34f
        drawRoundRect(cs[i % 4], u.o(b[0], b[1] + dy), u.sz(30f, 13f), u.r(2f), alpha = d * fade)
    }
    // Mixer truck.
    translate(u.x(160f) - u.x(0f), 0f) {
        drawRoundRect(M.CementDark, u.o(6f, 70f), u.sz(58f, 10f), u.r(3f))
        drawCircle(ink, 5f * u.s, u.o(14f, 82f))
        drawCircle(ink, 5f * u.s, u.o(56f, 82f))
        rotate(t * 360f * 2, pivot = u.o(36f, 50f)) {
            drawOval(M.Cement, u.o(12f, 30f), u.sz(48f, 40f))
            drawLine(M.CementDark, u.o(16f, 40f), u.o(56f, 60f), strokeWidth = 4f * u.s, cap = StrokeCap.Round)
            drawLine(M.CementDark, u.o(26f, 32f), u.o(58f, 48f), strokeWidth = 4f * u.s, cap = StrokeCap.Round)
        }
        val chute = Path().apply { moveTo(u.x(58f), u.y(56f)); quadraticTo(u.x(72f), u.y(62f), u.x(70f), u.y(76f)) }
        drawPath(
            chute, M.Cement,
            style = Stroke(5f * u.s, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * u.s, 4f * u.s), -t * 96f * u.s)),
        )
    }
    // Steel bars, paint can, tiles.
    translate(u.x(244f) - u.x(0f), 0f) {
        listOf(M.Steel, M.SteelDark, M.Steel).forEachIndexed { i, c ->
            val d = easeOutCubic(phase(t, 0.2f + i * 0.08f, 0.25f))
            drawRoundRect(c, u.o(-26f * (1 - d), 38f + i * 10f), u.sz(88f, 5f), u.r(2.5f), alpha = d * fade)
        }
        drawRoundRect(M.Paint, u.o(26f, 70f), u.sz(26f, 14f), u.r(3f))
        drawRoundRect(M.PaintPink, u.o(26f, 70f), u.sz(26f, 4f), u.r(2f))
        val dd = (t * 2f) % 1f
        drawRoundRect(M.PaintPink, u.o(34f, 74f + dd * 6f), u.sz(3f, 4f + 6f * dd), u.r(1.5f), alpha = 1f - dd)
        for (i in 0 until 4) {
            val open = easeOutCubic(phase(t, 0.3f + i * 0.06f, 0.15f))
            val x = 62f + (i % 2) * 12f
            val y = 68f + (i / 2) * 12f
            scale(open.coerceAtLeast(0.02f), 1f, pivot = u.o(x + 5f, y + 5f)) {
                drawRoundRect(if (i == 0 || i == 3) M.Tile else M.TileLight, u.o(x, y), u.sz(10f, 10f), u.r(1.5f))
            }
        }
    }
}

// ---- Composable wrappers -------------------------------------------------------------

@Composable
fun BrickArt(modifier: Modifier) {
    val t = rememberLoop(4000)
    Canvas(modifier) { drawBrickStack(t.value) }
}

@Composable
fun CementArt(modifier: Modifier) {
    val t = rememberLoop(2600)
    Canvas(modifier) { drawCementPour(t.value) }
}

@Composable
fun TileArt(modifier: Modifier) {
    val t = rememberLoop(3400)
    Canvas(modifier) { drawTileFlip(t.value) }
}

@Composable
fun PaintArt(modifier: Modifier, mute: Color) {
    val t = rememberLoop(1900)
    Canvas(modifier) { drawPaintBucket(t.value, mute) }
}

@Composable
fun SteelArt(modifier: Modifier) {
    val t = rememberLoop(3200)
    Canvas(modifier) { drawSteelBars(t.value) }
}

@Composable
fun RulerArt(modifier: Modifier, ink: Color, accent: Color) {
    val t = rememberLoop(3000)
    Canvas(modifier) { drawRulerArt(t.value, ink, accent) }
}

@Composable
fun EstimateArt(modifier: Modifier, fg: Color) {
    val t = rememberLoop(3000, stillAt = 1f)
    Canvas(modifier) { drawEstimateArt(t.value, fg) }
}

@Composable
fun HeroScene(modifier: Modifier, ground: Color, ink: Color) {
    val t = rememberLoop(5200, stillAt = 0.7f)
    Canvas(modifier) { drawHeroScene(t.value, ground, ink) }
}
