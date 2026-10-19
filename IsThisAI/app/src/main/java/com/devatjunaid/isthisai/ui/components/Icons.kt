package com.devatjunaid.isthisai.ui.components

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** A small stroke icon set (24×24 grid), drawn in the app's line style. */
object AppIcons {
    private fun icon(name: String, vararg paths: String, stroke: Float = 1.9f): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        for (d in paths) {
            b.addPath(
                pathData = addPathNodes(d),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = stroke,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    val History = icon("history", "M3 12a9 9 0 1 0 3-6.7L3 8", "M3 3v5h5", "M12 7v5l3 2")
    val Info = icon("info", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z", "M12 11v5", "M12 7.6v0.4")
    val Image = icon("image", "M6 3h12a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3z", "M9 7a2 2 0 1 0 0 4a2 2 0 1 0 0-4z", "M21 15l-5-5L5 21")
    val Paste = icon("paste", "M9 2h6a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1z", "M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2")
    val Folder = icon("folder", "M4 5h5l2 3h9v11a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1z")
    val Lock = icon("lock", "M7 11h10a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z", "M8 11V7a4 4 0 0 1 8 0v4")
    val Close = icon("close", "M18 6L6 18", "M6 6l12 12")
    val Back = icon("back", "M15 18l-6-6l6-6")
    val Chevron = icon("chevron", "M9 6l6 6l-6 6")
    val Arrow = icon("arrow", "M5 12h14", "M13 6l6 6l-6 6")
    val Check = icon("check", "M5 12l5 5l9-10", stroke = 2.4f)
    val Share = icon("share", "M12 3v12", "M7 8l5-5l5 5", "M5 13v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6")
    val Copy = icon("copy", "M11 9h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-8a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2z", "M5 15V5a2 2 0 0 1 2-2h8")
    val Download = icon("download", "M12 3v12", "M7 10l5 5l5-5", "M5 21h14")
    val Trash = icon("trash", "M4 7h16", "M10 11v6", "M14 11v6", "M6 7l1 13h10l1-13", "M9 7V4h6v3")
    val Warning = icon("warning", "M12 3l10 18H2z", "M12 10v5", "M12 18v0.4")
    val Shield = icon("shield", "M12 3l8 3v6c0 5-3.5 8-8 9c-4.5-1-8-4-8-9V6z", "M8.5 12l2.5 2.5l4.5-5")
    val Eye = icon("eye", "M2 12s3.5-7 10-7s10 7 10 7s-3.5 7-10 7s-10-7-10-7z", "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6z")
    val Spark = icon("spark", "M12 3c0.6 4.8 3.4 7.6 8.2 8.2c-4.8 0.6-7.6 3.4-8.2 8.2c-0.6-4.8-3.4-7.6-8.2-8.2c4.8-0.6 7.6-3.4 8.2-8.2z")
    val Camera = icon("camera", "M4 8h3l2-3h6l2 3h3a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z", "M12 10a3.5 3.5 0 1 0 0 7a3.5 3.5 0 1 0 0-7z")
    val Settings = icon("settings", "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6z", "M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3a1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5a1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8a1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1a1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5a1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z", stroke = 1.6f)
}
