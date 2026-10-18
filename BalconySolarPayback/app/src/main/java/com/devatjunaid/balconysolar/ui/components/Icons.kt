package com.devatjunaid.balconysolar.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** The app's own stroke icon set (24-unit grid, 2-unit strokes). */
object AppIcons {
    private fun icon(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            for (p in paths) {
                addPath(
                    pathData = addPathNodes(p),
                    fill = null,
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Pin = icon("pin", "M12 21s-7-6.5-7-12a7 7 0 0 1 14 0c0 5.5-7 12-7 12z", "M12 6.5a2.5 2.5 0 1 1 0 5a2.5 2.5 0 1 1 0-5z")
    val Sliders = icon("sliders", "M4 7h10M18 7h2M4 17h4M12 17h8", "M16 5a2 2 0 1 1 0 4a2 2 0 1 1 0-4z", "M10 15a2 2 0 1 1 0 4a2 2 0 1 1 0-4z")
    val SunArc = icon("sun", "M3 18h18", "M5 18a7 7 0 0 1 14 0", "M15 7a2 2 0 1 1 0 4a2 2 0 1 1 0-4z")
    val Bars = icon("bars", "M5 20V12M10 20V6M15 20V9M20 20V14")
    val Coin = icon("coin", "M12 4a8 8 0 1 1 0 16a8 8 0 1 1 0-16z", "M12 8v8", "M14.5 10.2c0-1-1-1.6-2.5-1.6s-2.5.6-2.5 1.6 1 1.4 2.5 1.6 2.5.6 2.5 1.6-1 1.6-2.5 1.6-2.5-.6-2.5-1.6")
    val Back = icon("back", "M15 6l-6 6 6 6")
    val ChevronRight = icon("chevron", "M9 6l6 6-6 6")
    val Plus = icon("plus", "M12 5v14M5 12h14")
    val Minus = icon("minus", "M5 12h14")
    val Search = icon("search", "M11 4a7 7 0 1 1 0 14a7 7 0 1 1 0-14z", "M20 20l-3.5-3.5")
    val Locate = icon("locate", "M12 8a4 4 0 1 1 0 8a4 4 0 1 1 0-8z", "M12 2v3M12 19v3M2 12h3M19 12h3")
    val Compass = icon("compass", "M12 3a9 9 0 1 1 0 18a9 9 0 1 1 0-18z", "M15.5 8.5l-2 5-5 2 2-5z")
    val PhoneTilt = icon("phone", "M8 3h8a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z", "M11 18h2")
    val Save = icon("save", "M5 4h11l3 3v13H5z", "M8 4v5h7V4", "M8 20v-6h8v6")
    val Share = icon("share", "M12 3v12M7 8l5-5 5 5", "M5 14v5a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-5")
    val Folder = icon("folder", "M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z")
    val Info = icon("info", "M12 3a9 9 0 1 1 0 18a9 9 0 1 1 0-18z", "M12 11v5M12 8h.01")
    val Close = icon("close", "M6 6l12 12M18 6L6 18")
    val Check = icon("check", "M5 12.5l4.5 4.5L19 7")
    val Edit = icon("edit", "M4 20h4L19 9l-4-4L4 16z", "M13.5 6.5l4 4")
    val Satellite = icon("satellite", "M13 7l4-4 4 4-4 4z", "M7 13l-4 4 4 4 4-4z", "M8.5 8.5l7 7", "M10.5 5.5l8 8")
    val Battery = icon("battery", "M3 8h15a1 1 0 0 1 1 1v6a1 1 0 0 1-1 1H3z", "M21 10.5v3", "M6 11v2M9 11v2")
    val Home = icon("home", "M4 11l8-7 8 7", "M6 10v10h12V10")
    val Bolt = icon("bolt", "M13 2L4 14h7l-1 8 9-12h-7z")
    val Leaf = icon("leaf", "M5 19c0-9 6-14 15-15-1 9-6 15-15 15z", "M5 19l7-7")
    val Trash = icon("trash", "M4 7h16", "M9 7V4h6v3", "M6 7l1 13h10l1-13")
    val Calendar = icon("calendar", "M4 6h16v14H4z", "M4 10h16", "M8 3v5M16 3v5")
    val Shield = icon("shield", "M12 3l8 3v6c0 4.5-3.4 8.1-8 9-4.6-.9-8-4.5-8-9V6z")
    val Spark = icon("spark", "M12 3v4M12 17v4M3 12h4M17 12h4M6 6l2.5 2.5M15.5 15.5L18 18M6 18l2.5-2.5M15.5 8.5L18 6")
}
