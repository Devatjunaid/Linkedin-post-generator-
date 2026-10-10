package com.harrycodess.drawingkids.drawing

import android.graphics.Bitmap
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.harrycodess.drawingkids.art.inkStroke
import com.harrycodess.drawingkids.art.svg
import com.harrycodess.drawingkids.ui.theme.Kid
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class Tool { PENCIL, MARKER, CRAYON, ERASER, STAMP, FILL }

/** Brush widths in dp for the four size buttons. */
val BrushSizesDp = listOf(4f, 9f, 16f, 26f)

fun Tool.widthMultiplier() = when (this) {
    Tool.PENCIL -> 0.6f
    Tool.MARKER -> 1.4f
    Tool.CRAYON -> 1.0f
    Tool.ERASER -> 1.7f
    Tool.STAMP -> 1.5f
    Tool.FILL -> 1f
}

/** One finished (or in-progress) stroke or stamp, in logical canvas units. */
class Mark(
    val tool: Tool,
    val color: Color,
    val width: Float,
    val seed: Int = Random.nextInt(),
) {
    val points: SnapshotStateList<Offset> = mutableStateListOf()
}

object Brushes {

    private val pencilGrain: ImageBitmap by lazy { noise(48, seed = 7, block = 1, holes = 0.22f, minAlpha = 120) }
    private val crayonWax: ImageBitmap by lazy { noise(64, seed = 11, block = 2, holes = 0.34f, minAlpha = 200) }

    private val pencilBrush by lazy { ShaderBrush(ImageShader(pencilGrain, TileMode.Repeated, TileMode.Repeated)) }
    private val crayonBrush by lazy { ShaderBrush(ImageShader(crayonWax, TileMode.Repeated, TileMode.Repeated)) }

    /** Grainy alpha texture used to give pencils and crayons their paper-y look. */
    private fun noise(size: Int, seed: Int, block: Int, holes: Float, minAlpha: Int): ImageBitmap {
        val rnd = Random(seed)
        val px = IntArray(size * size)
        val cells = size / block
        for (cy in 0 until cells) for (cx in 0 until cells) {
            val a = if (rnd.nextFloat() < holes) rnd.nextInt(0, 70) else rnd.nextInt(minAlpha, 256)
            for (dy in 0 until block) for (dx in 0 until block) {
                px[(cy * block + dy) * size + cx * block + dx] = a shl 24
            }
        }
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bmp.setPixels(px, 0, size, 0, 0, size, size)
        return bmp.asImageBitmap()
    }

    fun strokePath(points: List<Offset>): Path {
        val p = Path()
        if (points.isEmpty()) return p
        val first = points[0]
        p.moveTo(first.x, first.y)
        if (points.size == 1) {
            p.lineTo(first.x + 0.01f, first.y)
            return p
        }
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            p.quadraticBezierTo(a.x, a.y, (a.x + b.x) / 2f, (a.y + b.y) / 2f)
        }
        val last = points.last()
        p.lineTo(last.x, last.y)
        return p
    }

    fun DrawScope.drawMark(mark: Mark) {
        if (mark.points.isEmpty()) return
        when (mark.tool) {
            Tool.STAMP -> drawStamp(mark)
            Tool.PENCIL -> drawPath(
                strokePath(mark.points), pencilBrush, style = inkStroke(mark.width),
                colorFilter = ColorFilter.tint(mark.color, BlendMode.SrcIn),
            )
            Tool.CRAYON -> drawPath(
                strokePath(mark.points), crayonBrush, style = inkStroke(mark.width),
                colorFilter = ColorFilter.tint(mark.color, BlendMode.SrcIn),
            )
            Tool.MARKER -> drawPath(
                strokePath(mark.points), mark.color.copy(alpha = 0.62f), style = inkStroke(mark.width),
            )
            Tool.ERASER -> drawPath(
                strokePath(mark.points), Color.Black, style = inkStroke(mark.width), blendMode = BlendMode.Clear,
            )
            Tool.FILL -> Unit
        }
    }

    // ------------------------------------------------------------------ stamps

    private val star: Path by lazy {
        val p = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) 1f else 0.46f
            val a = -Math.PI / 2 + i * Math.PI / 5
            val x = (cos(a) * r).toFloat()
            val y = (sin(a) * r).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        p
    }
    private val heart by lazy { svg("M0 0.9 C-1.25 0 -0.75 -1.15 0 -0.45 C0.75 -1.15 1.25 0 0 0.9 Z") }
    private val stampTypes = 4

    private fun DrawScope.drawStamp(mark: Mark) {
        val c = mark.points[0]
        val r = mark.width
        val outline = inkStroke(0.09f)
        withTransform({
            translate(c.x, c.y)
            rotate(((mark.seed % 30) - 15).toFloat(), pivot = Offset.Zero)
            scale(r, r, pivot = Offset.Zero)
        }) {
            when (Math.floorMod(mark.seed, stampTypes)) {
                0 -> {
                    drawPath(star, mark.color)
                    drawPath(star, Kid.Ink, style = outline)
                }
                1 -> {
                    drawPath(heart, mark.color)
                    drawPath(heart, Kid.Ink, style = outline)
                }
                2 -> {
                    for (i in 0 until 5) {
                        val a = -Math.PI / 2 + i * 2 * Math.PI / 5
                        val o = Offset((cos(a) * 0.55).toFloat(), (sin(a) * 0.55).toFloat())
                        drawCircle(mark.color, 0.42f, o)
                        drawCircle(Kid.Ink, 0.42f, o, style = outline)
                    }
                    drawCircle(Kid.Yellow, 0.36f, Offset.Zero)
                    drawCircle(Kid.Ink, 0.36f, Offset.Zero, style = outline)
                }
                else -> {
                    drawCircle(mark.color, 0.9f, Offset.Zero)
                    drawCircle(Kid.Ink, 0.9f, Offset.Zero, style = outline)
                    drawCircle(Kid.Ink, 0.12f, Offset(-0.32f, -0.2f))
                    drawCircle(Kid.Ink, 0.12f, Offset(0.32f, -0.2f))
                    drawPath(svg("M-0.42 0.2 Q0 0.62 0.42 0.2"), Kid.Ink, style = inkStroke(0.1f))
                }
            }
        }
    }
}
