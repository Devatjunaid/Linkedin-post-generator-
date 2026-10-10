package com.harrycodess.drawingkids.drawing

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.harrycodess.drawingkids.art.AnimalArt
import com.harrycodess.drawingkids.art.Viewport
import com.harrycodess.drawingkids.art.drawArt
import com.harrycodess.drawingkids.art.inViewport
import com.harrycodess.drawingkids.drawing.Brushes.drawMark
import com.harrycodess.drawingkids.ui.theme.Kid
import kotlin.math.roundToInt

/** Runs [block] against an off-screen [ImageBitmap]. */
fun ImageBitmap.drawInto(block: DrawScope.() -> Unit) {
    CanvasDrawScope().draw(
        Density(1f), LayoutDirection.Ltr, Canvas(this), Size(width.toFloat(), height.toFloat()), block,
    )
}

private class StrokeLayer(val bitmap: ImageBitmap) {
    var drawnCount = 0
    var drawnReset = -1
}

private fun DrawScope.clipToPage(v: Viewport, block: DrawScope.() -> Unit) {
    inViewport(v) { clipRect(v.origin.x, v.origin.y, v.origin.x + v.logical.width, v.origin.y + v.logical.height) { block() } }
}

/**
 * The paper kids draw on. Layers, bottom to top:
 *  1. white page (+ the animal with its bucket fills when coloring),
 *  2. the kid's strokes (cached in a bitmap; the eraser only clears this layer),
 *  3. the animal's outlines, so lines stay crisp over crayon.
 */
@Composable
fun DrawingSurface(
    doc: CanvasDocument,
    tool: Tool,
    color: Color,
    sizeIndex: Int,
    art: AnimalArt?,
    modifier: Modifier = Modifier,
    onFill: () -> Unit = {},
    onStamp: () -> Unit = {},
) {
    val localDensity = LocalDensity.current
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var live by remember { mutableStateOf<Mark?>(null) }

    val viewport: Viewport? = remember(canvasSize, art, doc) {
        if (canvasSize.width == 0 || canvasSize.height == 0) {
            null
        } else {
            val cs = Size(canvasSize.width.toFloat(), canvasSize.height.toFloat())
            if (art != null) {
                Viewport.fit(cs, AnimalArt.ORIGIN, AnimalArt.SIZE, padding = with(localDensity) { 14.dp.toPx() })
            } else {
                val page = doc.pageSize ?: Size(cs.width / localDensity.density, cs.height / localDensity.density)
                    .also { doc.pageSize = it }
                Viewport.fit(cs, Offset.Zero, page)
            }
        }
    }
    val layer = remember(canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            StrokeLayer(ImageBitmap(canvasSize.width, canvasSize.height))
        } else {
            null
        }
    }

    val currentTool by rememberUpdatedState(tool)
    val currentColor by rememberUpdatedState(color)
    val currentSize by rememberUpdatedState(sizeIndex)
    val currentViewport by rememberUpdatedState(viewport)
    val currentOnFill by rememberUpdatedState(onFill)
    val currentOnStamp by rememberUpdatedState(onStamp)

    Box(
        modifier
            .clipToBounds()
            .onSizeChanged { canvasSize = it }
            .pointerInput(doc, art) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val v = currentViewport ?: return@awaitEachGesture
                    val start = v.toLogical(down.position)
                    val dpToLogical = localDensity.density / v.scale
                    when (val t = currentTool) {
                        Tool.FILL -> {
                            val group = art?.groupAt(start)
                            if (group != null) {
                                doc.fill(group, currentColor)
                                currentOnFill()
                            }
                            down.consume()
                        }
                        Tool.STAMP -> {
                            val radius = (12f + BrushSizesDp[currentSize] * t.widthMultiplier()) * dpToLogical
                            doc.add(Mark(t, currentColor, radius).apply { points.add(start) })
                            currentOnStamp()
                            down.consume()
                        }
                        else -> {
                            val width = BrushSizesDp[currentSize] * t.widthMultiplier() * dpToLogical
                            val mark = Mark(t, currentColor, width).apply { points.add(start) }
                            live = mark
                            down.consume()
                            val minStep = 1.5f * dpToLogical
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                val p = v.toLogical(change.position)
                                if ((p - mark.points.last()).getDistance() >= minStep) mark.points.add(p)
                                change.consume()
                            }
                            live = null
                            doc.add(mark)
                        }
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val v = viewport ?: return@Canvas
            if (art == null) {
                drawRect(Kid.Outside)
                inViewport(v) { drawRect(Color.White, topLeft = v.origin, size = v.logical) }
            } else {
                drawRect(Color.White)
                inViewport(v) { drawArt(art, doc.fills) }
            }
        }

        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            val v = viewport ?: return@Canvas
            val l = layer ?: return@Canvas
            // Reading the versions subscribes this layer to document changes.
            val reset = doc.resetVersion
            @Suppress("UNUSED_VARIABLE") val append = doc.appendVersion
            if (l.drawnReset != reset) {
                l.bitmap.drawInto { drawRect(Color.Black, blendMode = BlendMode.Clear) }
                l.drawnCount = 0
                l.drawnReset = reset
            }
            if (l.drawnCount < doc.marks.size) {
                val pending = doc.marks.subList(l.drawnCount, doc.marks.size).toList()
                l.bitmap.drawInto { clipToPage(v) { pending.forEach { drawMark(it) } } }
                l.drawnCount = doc.marks.size
            }
            drawImage(l.bitmap)
            live?.let { m -> clipToPage(v) { drawMark(m) } }
        }

        if (art != null) {
            Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
                val v = viewport ?: return@Canvas
                inViewport(v) { drawArt(art, emptyMap(), overlayOnly = true) }
            }
        }
    }
}

/** Flattens a page into a shareable picture for the gallery. */
object ArtworkRenderer {
    fun render(doc: CanvasDocument, art: AnimalArt?): Bitmap {
        val origin = if (art != null) AnimalArt.ORIGIN else Offset.Zero
        val logical = if (art != null) AnimalArt.SIZE else (doc.pageSize ?: Size(400f, 300f))
        val scale = if (art != null) 1080f / AnimalArt.SIZE.width else 2f
        val w = (logical.width * scale).roundToInt().coerceAtLeast(1)
        val h = (logical.height * scale).roundToInt().coerceAtLeast(1)
        val v = Viewport(scale, Offset.Zero, origin, logical)

        val strokes = ImageBitmap(w, h).apply {
            drawInto { inViewport(v) { doc.marks.forEach { drawMark(it) } } }
        }
        val overlay = art?.let { a ->
            ImageBitmap(w, h).apply { drawInto { inViewport(v) { drawArt(a, emptyMap(), overlayOnly = true) } } }
        }
        val out = ImageBitmap(w, h)
        out.drawInto {
            drawRect(Color.White)
            if (art != null) inViewport(v) { drawArt(art, doc.fills) }
            drawImage(strokes)
            overlay?.let { drawImage(it) }
        }
        return out.asAndroidBitmap()
    }
}
