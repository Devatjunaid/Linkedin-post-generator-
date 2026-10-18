package app.layerline.toolkit3d.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0"
private fun rect(x: Float, y: Float, w: Float, h: Float) = "M$x,${y}h${w}v${h}h${-w}z"

private fun strokeIcon(name: String, vararg paths: String, width: Float = 1.8f): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = width,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }
        .build()

/** Hand-drawn stroke icon set that matches the mockups (no Material icon font needed). */
object LIcons {
    val Bench = strokeIcon("bench", rect(3f, 3f, 7f, 7f), rect(14f, 3f, 7f, 7f), rect(3f, 14f, 7f, 7f), rect(14f, 14f, 7f, 7f))
    val Cost = strokeIcon("cost", "M6 3h12v18l-3-2-3 2-3-2-3 2z", "M9 8h6M9 12h6M9 16h3")
    val Spool = strokeIcon("spool", circle(12f, 12f, 9f), circle(12f, 12f, 3f), "M12 3v6M12 15v6")
    val Calib = strokeIcon("calib", "M3 17L17 3l4 4L7 21z", "M7 13l2 2M10 10l2 2M13 7l2 2")
    val Log = strokeIcon("log", "M12 3l9 5-9 5-9-5z", "M3 13l9 5 9-5", "M3 17l9 5 9-5")
    val Settings = strokeIcon("settings", circle(12f, 12f, 3f), "M12 2v3M12 19v3M4.2 4.2l2.1 2.1M17.7 17.7l2.1 2.1M2 12h3M19 12h3M4.2 19.8l2.1-2.1M17.7 6.3l2.1-2.1")
    val Back = strokeIcon("back", "M15 5l-7 7 7 7", width = 2f)
    val Next = strokeIcon("next", "M9 5l7 7-7 7", width = 2f)
    val Plus = strokeIcon("plus", "M12 5v14M5 12h14", width = 2.4f)
    val Close = strokeIcon("close", "M6 6l12 12M18 6L6 18", width = 2f)
    val Check = strokeIcon("check", "M5 12.5l4.5 4.5L19 7", width = 2.2f)
    val Edit = strokeIcon("edit", "M4 20h4L19 9l-4-4L4 16z", "M13 7l4 4")
    val Trash = strokeIcon("trash", "M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13", "M10 11v6M14 11v6")
    val Share = strokeIcon("share", circle(6f, 12f, 2.5f), circle(18f, 6f, 2.5f), circle(18f, 18f, 2.5f), "M8.3 10.8l7.4-3.6M8.3 13.2l7.4 3.6")
    val Copy = strokeIcon("copy", rect(8f, 8f, 12f, 12f), "M16 8V4H4v12h4")
    val Printer = strokeIcon("printer", rect(3f, 3f, 18f, 18f), "M3 8h18M10 8v5h4V8M9 17h6")
    val Bolt = strokeIcon("bolt", "M13 2L4 14h7l-1 8 9-12h-7z")
    val Clock = strokeIcon("clock", circle(12f, 12f, 9f), "M12 7v5l3 3")
    val Scale = strokeIcon("scale", "M5 20h14l-2-11H7z", circle(12f, 6f, 2.5f), "M10 14h4")
    val Wrench = strokeIcon("wrench", "M14.5 4a5 5 0 0 0-4.8 6.4L3 17.1 6.9 21l6.7-6.7A5 5 0 0 0 20 9.5l-3 3-3.5-1-1-3.5 3-3a5 5 0 0 0-1-1")
    val Globe = strokeIcon("globe", circle(12f, 12f, 9f), "M3 12h18M12 3c3 3.5 3 14.5 0 18M12 3c-3 3.5-3 14.5 0 18")
    val Shield = strokeIcon("shield", "M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z", "M9 12l2 2 4-4")
    val Info = strokeIcon("info", circle(12f, 12f, 9f), "M12 11v6M12 7.5v.5")
    val Flame = strokeIcon("flame", "M12 21c-4 0-6-2.7-6-6 0-4 4-6 4-10 3 2 8 6 8 10.5 0 3.2-2.3 5.5-6 5.5z", "M12 21c-1.7 0-2.6-1.2-2.6-2.6 0-1.8 2.6-3.4 2.6-3.4s2.6 1.6 2.6 3.4c0 1.4-.9 2.6-2.6 2.6z")
    val Receipt = Cost
    val Exit = strokeIcon("exit", "M14 4h5v16h-5", "M10 8l-4 4 4 4M6 12h10")
}

@Composable
fun LIcon(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp, contentDescription: String? = null) {
    Icon(imageVector = icon, contentDescription = contentDescription, tint = tint, modifier = modifier.size(size))
}
