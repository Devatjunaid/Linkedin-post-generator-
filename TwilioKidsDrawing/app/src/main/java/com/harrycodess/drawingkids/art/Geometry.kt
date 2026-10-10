package com.harrycodess.drawingkids.art

import android.graphics.Matrix
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.core.graphics.PathParser
import kotlin.math.min
import android.graphics.Path as APath

/* Small helpers shared by every hand-built drawing in the app (animals, icons, logo). */

private val Number.f get() = toFloat()

fun circle(cx: Number, cy: Number, r: Number): APath =
    APath().apply { addCircle(cx.f, cy.f, r.f, APath.Direction.CW) }

fun ellipse(cx: Number, cy: Number, rx: Number, ry: Number, rotate: Number = 0): APath {
    val p = APath()
    p.addOval(RectF(cx.f - rx.f, cy.f - ry.f, cx.f + rx.f, cy.f + ry.f), APath.Direction.CW)
    if (rotate.f != 0f) p.transform(Matrix().apply { setRotate(rotate.f, cx.f, cy.f) })
    return p
}

fun rect(x: Number, y: Number, w: Number, h: Number, r: Number = 0): APath =
    APath().apply { addRoundRect(RectF(x.f, y.f, x.f + w.f, y.f + h.f), r.f, r.f, APath.Direction.CW) }

/** Parses SVG path data ("M10 10 L20 20 Q ... Z"). */
fun path(d: String): APath = PathParser.createPathFromPathData(d)

/** SVG path data straight to a Compose path (used by icons). */
fun svg(d: String): Path = path(d).asComposePath()

fun inkStroke(width: Float) = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)

/**
 * Maps a logical drawing space (e.g. the 200x200 animal box) into a canvas, keeping aspect ratio.
 */
data class Viewport(val scale: Float, val offset: Offset, val origin: Offset, val logical: Size) {
    fun toLogical(p: Offset) = Offset((p.x - offset.x) / scale + origin.x, (p.y - offset.y) / scale + origin.y)

    companion object {
        fun fit(canvas: Size, origin: Offset, logical: Size, padding: Float = 0f): Viewport {
            val s = min((canvas.width - 2 * padding) / logical.width, (canvas.height - 2 * padding) / logical.height)
                .coerceAtLeast(0.0001f)
            val ox = (canvas.width - logical.width * s) / 2f
            val oy = (canvas.height - logical.height * s) / 2f
            return Viewport(s, Offset(ox, oy), origin, logical)
        }
    }
}

inline fun DrawScope.inViewport(v: Viewport, block: DrawScope.() -> Unit) {
    withTransform({
        translate(v.offset.x, v.offset.y)
        scale(v.scale, v.scale, pivot = Offset.Zero)
        translate(-v.origin.x, -v.origin.y)
    }, block)
}

/** Draws [block] authored in a square [box]-sized coordinate system, centered in the current size. */
inline fun DrawScope.inBox(box: Float, block: DrawScope.() -> Unit) {
    inViewport(Viewport.fit(size, Offset.Zero, Size(box, box)), block)
}
