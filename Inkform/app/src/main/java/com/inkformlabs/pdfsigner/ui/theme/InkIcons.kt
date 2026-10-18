package com.inkformlabs.pdfsigner.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Inkform's own line-icon set (24 x 24, 2 px round strokes), tinted by Icon(). */
object InkIcons {
    private fun stroke(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                paths.forEach { d ->
                    addPath(
                        pathData = addPathNodes(d),
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    )
                }
            }.build()

    private fun filled(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply { paths.forEach { d -> addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black)) } }
            .build()

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0"

    val Back = stroke("back", "M15,6 L9,12 L15,18")
    val Forward = stroke("forward", "M9,6 L15,12 L9,18")
    val ArrowRight = stroke("arrowRight", "M5,12 H19", "M13,6 L19,12 L13,18")
    val Close = stroke("close", "M6,6 L18,18", "M18,6 L6,18")
    val Check = stroke("check", "M5,12.5 L10,17 L19,7.5")
    val Plus = stroke("plus", "M12,5 V19", "M5,12 H19")
    val Minus = stroke("minus", "M5,12 H19")
    val Search = stroke("search", circle(11f, 11f, 7f), "M20,20 L16,16")
    val More = filled("more", circle(5f, 12f, 2f), circle(12f, 12f, 2f), circle(19f, 12f, 2f))
    val Home = stroke("home", "M3,11 L12,4 L21,11 V20 A1,1 0 0,1 20,21 H15 V15 H9 V21 H4 A1,1 0 0,1 3,20 Z")
    val Files = stroke("files", "M3,6 A2,2 0 0,1 5,4 H9 L11,6 H19 A2,2 0 0,1 21,8 V18 A2,2 0 0,1 19,20 H5 A2,2 0 0,1 3,18 Z")
    val Pen = stroke("pen", "M14.5,4.5 L19.5,9.5 L9,20 H4 V15 Z", "M4,20 H20")
    val User = stroke("user", circle(12f, 8f, 4f), "M4,21 C5,17 8,15 12,15 C16,15 19,17 20,21")
    val Settings = stroke(
        "settings", circle(12f, 12f, 3f),
        "M12,2.5 V5 M12,19 V21.5 M2.5,12 H5 M19,12 H21.5 M5.3,5.3 L7.1,7.1 M16.9,16.9 L18.7,18.7 M5.3,18.7 L7.1,16.9 M16.9,7.1 L18.7,5.3",
    )
    val Shield = stroke("shield", "M12,3 L20,6 V12 C20,17 16.5,20 12,21 C7.5,20 4,17 4,12 V6 Z", "M9,12 L11,14 L15,10")
    val Scan = stroke("scan", "M4,8 V5 A1,1 0 0,1 5,4 H8", "M16,4 H19 A1,1 0 0,1 20,5 V8", "M20,16 V19 A1,1 0 0,1 19,20 H16", "M8,20 H5 A1,1 0 0,1 4,19 V16", "M4,12 H20")
    val Camera = stroke("camera", "M4,8 H7 L9,5 H15 L17,8 H20 A1,1 0 0,1 21,9 V19 A1,1 0 0,1 20,20 H4 A1,1 0 0,1 3,19 V9 A1,1 0 0,1 4,8 Z", circle(12f, 13.5f, 3.5f))
    val Image = stroke("image", "M5,4 H19 A2,2 0 0,1 21,6 V18 A2,2 0 0,1 19,20 H5 A2,2 0 0,1 3,18 V6 A2,2 0 0,1 5,4 Z", circle(9f, 10f, 2f), "M21,16 L16,11 L7,20")
    val Import = stroke("import", "M12,3 V15", "M7,10 L12,15 L17,10", "M4,15 V19 A1,1 0 0,0 5,20 H19 A1,1 0 0,0 20,19 V15")
    val Download = stroke("download", "M12,4 V15", "M7,10 L12,15 L17,10", "M5,20 H19")
    val Share = stroke("share", circle(18f, 5f, 3f), circle(6f, 12f, 3f), circle(18f, 19f, 3f), "M8.6,13.5 L15.4,17.5", "M15.4,6.5 L8.6,10.5")
    val Print = stroke("print", "M6,9 V3 H18 V9", "M6,17 H4 A1,1 0 0,1 3,16 V10 A1,1 0 0,1 4,9 H20 A1,1 0 0,1 21,10 V16 A1,1 0 0,1 20,17 H18", "M6,14 H18 V21 H6 Z")
    val Trash = stroke("trash", "M4,7 H20", "M10,11 V17", "M14,11 V17", "M6,7 L7,20 H17 L18,7", "M9,7 V4 H15 V7")
    val Edit = stroke("edit", "M14.5,4.5 L19.5,9.5 L9,20 H4 V15 Z")
    val Text = stroke("text", "M5,6 V4 H19 V6", "M12,4 V20", "M9,20 H15")
    val Calendar = stroke("calendar", "M5,5 H19 A1,1 0 0,1 20,6 V19 A1,1 0 0,1 19,20 H5 A1,1 0 0,1 4,19 V6 A1,1 0 0,1 5,5 Z", "M4,10 H20", "M8,3 V7", "M16,3 V7")
    val Tick = stroke("tick", "M4,12.5 L9.5,18 L20,6.5")
    val Cross = stroke("cross", "M6,6 L18,18", "M18,6 L6,18")
    val Signature = stroke("signature", "M3,17 C6,11 8,11 9,15 C10,18 12,17 14,13 C15.5,10 17,11 18,15", "M3,21 H21")
    val Fields = stroke("fields", "M4,5 H20 V10 H4 Z", "M4,14 H20 V19 H4 Z", "M7,7.5 H11", "M7,16.5 H13")
    val Sparkle = stroke("sparkle", "M12,3 L13.8,8.2 L19,10 L13.8,11.8 L12,17 L10.2,11.8 L5,10 L10.2,8.2 Z", "M19,17 L19.8,19.2 L22,20 L19.8,20.8 L19,23")
    val Lock = stroke("lock", "M6,11 H18 A1,1 0 0,1 19,12 V20 A1,1 0 0,1 18,21 H6 A1,1 0 0,1 5,20 V12 A1,1 0 0,1 6,11 Z", "M8,11 V7 A4,4 0 0,1 16,7 V11")
    val Undo = stroke("undo", "M9,14 L4,9 L9,4", "M4,9 H14 A6,6 0 0,1 14,21 H11")
    val Replay = stroke("replay", "M3,12 A9,9 0 1,0 6,5.3 L3,8", "M3,3 V8 H8")
    val ZoomIn = stroke("zoomIn", circle(11f, 11f, 7f), "M20,20 L16,16", "M11,8 V14", "M8,11 H14")
    val ZoomOut = stroke("zoomOut", circle(11f, 11f, 7f), "M20,20 L16,16", "M8,11 H14")
    val Revert = stroke("revert", "M4,4 V9 H9", "M4.5,15 A8,8 0 1,0 6,7 L4,9")
    val Info = stroke("info", circle(12f, 12f, 9f), "M12,11 V16", "M12,8 V8.01")
    val Star = stroke("star", "M12,3 L14.8,8.8 L21,9.6 L16.5,13.9 L17.6,20 L12,17 L6.4,20 L7.5,13.9 L3,9.6 L9.2,8.8 Z")
    val Palette = stroke("palette", "M12,3 A9,9 0 1,0 12,21 C13.5,21 14,20 14,19 C14,17.5 13,17 13,16 C13,15 14,14 15.5,14 H17 A4,4 0 0,0 21,10 C21,6 17,3 12,3 Z", circle(7.5f, 11f, 1f), circle(10f, 7f, 1f), circle(15f, 7.5f, 1f))
    val Exit = stroke("exit", "M14,4 H19 A1,1 0 0,1 20,5 V19 A1,1 0 0,1 19,20 H14", "M10,8 L6,12 L10,16", "M6,12 H16")
}
