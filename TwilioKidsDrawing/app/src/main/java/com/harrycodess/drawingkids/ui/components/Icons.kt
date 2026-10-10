package com.harrycodess.drawingkids.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.harrycodess.drawingkids.art.inBox
import com.harrycodess.drawingkids.art.inkStroke
import com.harrycodess.drawingkids.art.svg
import com.harrycodess.drawingkids.drawing.Tool
import com.harrycodess.drawingkids.ui.theme.Kid

/** All icons are hand-built vector doodles; nothing comes from an icon font. */
object KidIcons {
    private val home = svg("M5 15 L16 5 L27 15 V27 H19 V19 H13 V27 H5 Z")
    private val undo = svg("M10 9 L4 15 L10 21 M4 15 H20 Q28 15 28 22 Q28 27 22 27")
    private val redo = svg("M22 9 L28 15 L22 21 M28 15 H12 Q4 15 4 22 Q4 27 10 27")
    private val trash = svg("M7 9 H25 M12 9 V6 H20 V9 M9 9 L10 27 H22 L23 9")
    private val trashBody = svg("M9 9 L10 27 H22 L23 9 Z")
    private val check = svg("M11 16 L15 20 L22 12")
    private val back = svg("M20 6 L10 16 L20 26")
    private val speaker = svg("M6 12 H12 L20 6 V26 L12 20 H6 Z")
    private val waves = svg("M24 11 Q28 16 24 21 M27 8 Q33 16 27 24")
    private val mute = svg("M24 12 L30 20 M30 12 L24 20")
    private val close = svg("M9 9 L23 23 M23 9 L9 23")

    fun DrawScope.homeIcon() {
        drawPath(home, Kid.Orange)
        drawPath(home, Kid.Ink, style = inkStroke(2.5f))
    }

    fun DrawScope.undoIcon(enabled: Boolean) = drawPath(undo, if (enabled) Kid.Ink else Kid.Line, style = inkStroke(3f))
    fun DrawScope.redoIcon(enabled: Boolean) = drawPath(redo, if (enabled) Kid.Ink else Kid.Line, style = inkStroke(3f))

    fun DrawScope.trashIcon() {
        drawPath(trashBody, Kid.Blush)
        drawPath(trash, Kid.Ink, style = inkStroke(2.5f))
    }

    fun DrawScope.saveIcon() {
        drawRoundRect(Kid.Green, topLeft = Offset(5f, 5f), size = androidx.compose.ui.geometry.Size(22f, 22f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f))
        drawRoundRect(Kid.Ink, topLeft = Offset(5f, 5f), size = androidx.compose.ui.geometry.Size(22f, 22f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f), style = inkStroke(2.5f))
        drawPath(check, Kid.Ink, style = inkStroke(3f))
    }

    fun DrawScope.backIcon() = drawPath(back, Kid.Ink, style = inkStroke(3.5f))
    fun DrawScope.closeIcon() = drawPath(close, Kid.Ink, style = inkStroke(3.5f))

    fun DrawScope.soundIcon(on: Boolean) {
        drawPath(speaker, Kid.Yellow)
        drawPath(speaker, Kid.Ink, style = inkStroke(2.5f))
        drawPath(if (on) waves else mute, Kid.Ink, style = inkStroke(2.5f))
    }
}

/** A 32x32 icon, scaled into whatever size it is given. */
@Composable
fun Icon32(modifier: Modifier, draw: DrawScope.() -> Unit) {
    Canvas(modifier) { inBox(32f) { draw() } }
}

// ---------------------------------------------------------------- tool icons (64x64)

private val pencilBody = svg("M10 54 L14 40 L44 10 L54 20 L24 50 Z")
private val pencilWood = svg("M10 54 L14 40 L24 50 Z")
private val pencilLead = svg("M10 54 L12 47 L17 52 Z")
private val pencilBand = svg("M38 16 L48 26")
private val markerBody = svg("M14 50 L20 34 L44 10 L54 20 L30 44 Z")
private val markerCap = svg("M38 16 L48 26 L54 20 L44 10 Z")
private val markerTip = svg("M14 50 L20 34 L30 44 Z")
private val markerInk = svg("M8 56 L14 50")
private val crayonBody = svg("M12 52 L16 38 L42 12 L52 22 L26 48 Z")
private val crayonBands = svg("M22 32 L32 42 M30 24 L40 34")
private val crayonTip = svg("M12 52 L16 38 L26 48 Z")
private val eraserBody = svg("M8 40 L32 16 L52 36 L36 52 L20 52 Z")
private val eraserEnd = svg("M8 40 L20 52 L36 52 L24 40 Z")
private val bucketBody = svg("M14 26 L34 10 L52 30 L32 50 Z")
private val bucketPaint = svg("M14 26 L52 30 L32 50 Z")
private val bucketDrip = svg("M52 34 Q58 46 54 50 Q48 50 52 34")
private val starShape = svg("M32 8 L38 24 L56 24 L42 35 L47 53 L32 42 L17 53 L22 35 L8 24 L26 24 Z")

fun DrawScope.drawToolIcon(tool: Tool, color: Color) {
    val o = inkStroke(3f)
    fun piece(p: androidx.compose.ui.graphics.Path, fill: Color) {
        drawPath(p, fill)
        drawPath(p, Kid.Ink, style = o)
    }
    when (tool) {
        Tool.PENCIL -> {
            piece(pencilBody, color); piece(pencilWood, Kid.Sand)
            drawPath(pencilLead, Kid.Ink); drawPath(pencilBand, Kid.Ink, style = o)
        }
        Tool.MARKER -> {
            piece(markerBody, Color.White); piece(markerCap, color); piece(markerTip, color)
            drawPath(markerInk, color, style = inkStroke(6f))
        }
        Tool.CRAYON -> {
            piece(crayonBody, color); drawPath(crayonBands, Kid.Ink, style = inkStroke(2.5f)); piece(crayonTip, color)
        }
        Tool.ERASER -> {
            piece(eraserBody, Kid.Blush); piece(eraserEnd, Color.White)
        }
        Tool.FILL -> {
            piece(bucketBody, Color.White); piece(bucketPaint, color)
            drawPath(bucketDrip, color); drawPath(bucketDrip, Kid.Ink, style = inkStroke(2.5f))
        }
        Tool.STAMP -> piece(starShape, color)
    }
}

@Composable
fun ToolIcon(tool: Tool, color: Color, modifier: Modifier) {
    Canvas(modifier) { inBox(64f) { drawToolIcon(tool, color) } }
}
