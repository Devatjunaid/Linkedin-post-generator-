package com.driveready.testprep.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Line icons drawn from SVG path data, so the app needs no icon-font dependency.
 * Each is a 24×24 stroke icon; `Icon(tint = …)` recolours it.
 */
object AppIcons {
    private fun stroke(name: String, vararg paths: String, filled: List<String> = emptyList()): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                )
            }
            filled.forEach { d -> addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black)) }
        }.build()

    val Home = stroke("home", "M4 11 L12 4 L20 11 V20 H14 V14 H10 V20 H4 Z")
    val Book = stroke("book", "M4 5 C7 4 10 4 12 6 C14 4 17 4 20 5 V19 C17 18 14 18 12 20 C10 18 7 18 4 19 Z", "M12 6 V20")
    val Mic = stroke("mic", "M9 6 A3 3 0 0 1 15 6 V11 A3 3 0 0 1 9 11 Z", "M5 11 C5 15 8 17.5 12 17.5 C16 17.5 19 15 19 11", "M12 17.5 V21")
    val Wheel = stroke("wheel", "M12 3.5 A8.5 8.5 0 1 1 11.99 3.5 Z", "M12 9.8 A2.2 2.2 0 1 1 11.99 9.8 Z", "M3.8 11 H9.8", "M14.2 11 H20.2", "M12 14.2 V20.5")
    val Chart = stroke("chart", "M5 20 V13", "M12 20 V6", "M19 20 V10")
    val Back = stroke("back", "M15 5 L8 12 L15 19")
    val Close = stroke("close", "M6 6 L18 18", "M18 6 L6 18")
    val Check = stroke("check", "M5 12.5 L10 17 L19 7")
    val ChevronRight = stroke("chevron", "M9 6 L15 12 L9 18")
    val ChevronDown = stroke("chevron_down", "M6 9 L12 15 L18 9")
    val Flag = stroke("flag", "M5 21 V4", "M5 4 H17 L14.5 8.5 L17 13 H5")
    val Play = stroke("play", filled = listOf("M7 4 L19 12 L7 20 Z"))
    val Replay = stroke("replay", "M4 12 A8 8 0 1 0 6.3 6.3", "M4 4 V9 H9")
    val Search = stroke("search", "M11 4 A7 7 0 1 1 10.99 4 Z", "M16.5 16.5 L21 21")
    val Globe = stroke("globe", "M12 3 A9 9 0 1 1 11.99 3 Z", "M12 3 C8 7 8 17 12 21", "M12 3 C16 7 16 17 12 21", "M3.5 9 H20.5", "M3.5 15 H20.5")
    val Star = stroke("star", "M12 3 L14.8 8.8 L21 9.6 L16.5 14 L17.6 20.3 L12 17.3 L6.4 20.3 L7.5 14 L3 9.6 L9.2 8.8 Z")
    val Warning = stroke("warning", "M12 3 L22 20 H2 Z", "M12 10 V14", "M12 17 V17.5")
    val Eye = stroke("eye", "M2 12 C5 6 19 6 22 12 C19 18 5 18 2 12 Z", "M12 9 A3 3 0 1 1 11.99 9 Z")
    val Calendar = stroke("calendar", "M4 6 H20 V20 H4 Z", "M4 10 H20", "M8 3 V7", "M16 3 V7")
    val Speaker = stroke("speaker", "M4 9 H8 L13 5 V19 L8 15 H4 Z", "M16.5 8.5 C18 10 18 14 16.5 15.5", "M19 6 C22 9 22 15 19 18")
    val List = stroke("list", "M9 6 H20", "M9 12 H20", "M9 18 H20", "M4.5 6 H5", "M4.5 12 H5", "M4.5 18 H5")
    val Shield = stroke("shield", "M12 3 L20 6 V12 C20 16.5 16.5 20 12 21 C7.5 20 4 16.5 4 12 V6 Z", "M8.5 12 L11 14.5 L15.5 9.5")
    val Doc = stroke("doc", "M6 3 H14 L19 8 V21 H6 Z", "M14 3 V8 H19", "M9 13 H16", "M9 17 H14")
    val Signs = stroke("signs", "M12 3 L21 19 H3 Z", "M12 9 V13", "M12 16 V16.5")
    val Settings = stroke("settings", "M12 9 A3 3 0 1 1 11.99 9 Z", "M12 2 V5", "M12 19 V22", "M2 12 H5", "M19 12 H22", "M4.9 4.9 L7 7", "M17 17 L19.1 19.1", "M4.9 19.1 L7 17", "M17 7 L19.1 4.9")
    val Timer = stroke("timer", "M12 6 A8 8 0 1 1 11.99 6 Z", "M12 10 V14 L14.5 15.5", "M9 2 H15")
    val Lightbulb = stroke("bulb", "M9 18 H15", "M10 21 H14", "M12 3 C8 3 6 6 6 9 C6 12 8 13 9 15 H15 C16 13 18 12 18 9 C18 6 16 3 12 3 Z")
}
