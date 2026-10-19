package com.devatjunaid.kelvin.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Custom stroke icon set (24 × 24 grid). Tint is applied by Icon(). */
object KIcons {
    private fun icon(name: String, vararg paths: String, filled: Boolean = false): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            paths.forEach { d ->
                addPath(
                    pathData = PathParser().parsePathString(d).toNodes(),
                    fill = if (filled) SolidColor(Color.White) else null,
                    stroke = if (filled) null else SolidColor(Color.White),
                    strokeLineWidth = 1.8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Snowflake = icon("snow", "M12 2v20M4.9 7l14.2 10M19.1 7 4.9 17M9 4l3 2 3-2M9 20l3-2 3 2")
    val Flame = icon("flame", "M12 22c4 0 7-3 7-7 0-4-3-6-4-10-2 2-3 4-3 6-1-1-2-2-2-4-3 2-5 5-5 8 0 4 3 7 7 7z")
    val Home = icon("home", "M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z")
    val Sliders = icon("sliders", "M4 7h10M18 7h2M4 17h4M12 17h8", "M14 7a2 2 0 1 0 4 0a2 2 0 1 0-4 0", "M8 17a2 2 0 1 0 4 0a2 2 0 1 0-4 0")
    val Chart = icon("chart", "M5 20V11M11 20V5M17 20v-7M3 20h18")
    val Settings = icon("settings", "M9 12a3 3 0 1 0 6 0a3 3 0 1 0-6 0", "M12 2v3M12 19v3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M2 12h3M19 12h3M4.9 19.1 7 17M17 7l2.1-2.1")
    val Play = icon("play", "M7 4v16l13-8z", filled = true)
    val Pause = icon("pause", "M7 4h3.5v16H7zM13.5 4H17v16h-3.5z", filled = true)
    val Skip = icon("skip", "M5 4v16l11-8zM19 4v16")
    val Plus = icon("plus", "M12 5v14M5 12h14")
    val Minus = icon("minus", "M5 12h14")
    val Close = icon("close", "M6 6l12 12M18 6 6 18")
    val Back = icon("back", "m15 18-6-6 6-6")
    val ChevronLeft = icon("chev_l", "m14 17-5-5 5-5")
    val ChevronRight = icon("chev_r", "m10 17 5-5-5-5")
    val Heart = icon("heart", "M12 21s-8-5.2-8-11a4.5 4.5 0 0 1 8-2.8A4.5 4.5 0 0 1 20 10c0 5.8-8 11-8 11z")
    val Thermometer = icon("thermo", "M14 14.8V4a2 2 0 0 0-4 0v10.8a4 4 0 1 0 4 0z")
    val Clock = icon("clock", "M4 13a8 8 0 1 0 16 0a8 8 0 1 0-16 0", "M12 9v4l2 2M9 2h6")
    val Trash = icon("trash", "M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3")
    val Edit = icon("edit", "M4 20h4L19 9l-4-4L4 16z")
    val Export = icon("export", "M12 3v12M7 8l5-5 5 5M5 14v6h14v-6")
    val Star = icon("star", "m12 2 3 6.5 7 .8-5.2 4.8 1.5 7L12 17.6 5.7 21l1.5-7L2 9.3l7-.8z", filled = true)
    val Thermal = icon("thermal", "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0", "M12 3a4.5 4.5 0 0 1 0 9 4.5 4.5 0 0 0 0 9")
    val Check = icon("check", "M5 12.5l4.5 4.5L19 7.5")
    val Shield = icon("shield", "M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z", "M9 12l2 2 4-4")
    val Bell = icon("bell", "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a1.94 1.94 0 0 0 3.4 0")
    val Calendar = icon("calendar", "M4 6h16v14H4zM4 10h16M8 3v4M16 3v4")
    val Breath = icon("breath", "M3 8h10a3 3 0 1 0-3-3M3 16h14a3 3 0 1 1-3 3M3 12h17")
    val Info = icon("info", "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0", "M12 11v6M12 7.5v.5")
    val Volume = icon("volume", "M4 9h4l5-4v14l-5-4H4z", "M16.5 8.5a5 5 0 0 1 0 7M19 6a8.5 8.5 0 0 1 0 12")
    val Vibrate = icon("vibrate", "M8 4h8v16H8z", "M4 8v8M20 8v8")
    val Privacy = icon("privacy", "M5 11h14v10H5z", "M8 11V7a4 4 0 0 1 8 0v4")
}
