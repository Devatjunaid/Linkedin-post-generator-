package com.glowdash.warningidentifier.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glowdash.warningidentifier.data.WarningLight
import com.glowdash.warningidentifier.ui.theme.Manrope

/**
 * Draws stroke icons from SVG path data on a 24×24 grid — the same format as
 * the design mockups — so every symbol stays crisp at any size.
 */
@Composable
fun PathIcon(
    path: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    strokeWidth: Float = 2f,
    label: String? = null,
    labelSize: Float = 6f,
    contentDescription: String? = null,
) {
    val parsed = remember(path) { PathParser().parsePathString(path).toPath() }
    val measurer = if (label != null) rememberTextMeasurer() else null
    val semantics = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else Modifier

    Canvas(modifier.size(size).then(semantics)) {
        val k = this.size.minDimension / 24f
        val dx = (this.size.width - 24f * k) / 2f
        val dy = (this.size.height - 24f * k) / 2f
        withTransform({
            translate(dx, dy)
            scale(k, k, pivot = Offset.Zero)
        }) {
            drawPath(
                path = parsed,
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        if (label != null && measurer != null) {
            val layout = measurer.measure(
                text = label,
                style = TextStyle(
                    color = color,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (labelSize * k).toSp(),
                ),
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    center.x - layout.size.width / 2f,
                    center.y - layout.size.height / 2f,
                ),
            )
        }
    }
}

/** A dashboard symbol, tinted with the colour it glows on a real car. */
@Composable
fun LightIcon(light: WarningLight, modifier: Modifier = Modifier, size: Dp = 30.dp, color: Color = light.color.color) {
    PathIcon(
        path = light.iconPath,
        color = color,
        modifier = modifier,
        size = size,
        strokeWidth = 1.8f,
        label = light.iconLabel,
        labelSize = light.iconLabelSize,
        contentDescription = light.name,
    )
}

/** UI glyphs on the same 24×24 grid. */
object UiIcons {
    const val Back = "M15 18l-6-6 6-6"
    const val Settings = "M4 6h9M17 6h3M15 4v4M4 12h3M11 12h9M9 10v4M4 18h11M19 18h1M17 16v4"
    const val Gallery = "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2zM7 8.5a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0-3 0M21 15l-5-5L5 21"
    const val Search = "M4 11a7 7 0 1 0 14 0a7 7 0 1 0-14 0M21 21l-4.3-4.3"
    const val Flash = "M13 2L4 14h7l-1 8 9-12h-7z"
    const val Home = "M3 10l9-7 9 7v10a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"
    const val Grid = "M4 3h5.5a1 1 0 0 1 1 1v5.5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1zM14.5 3H20a1 1 0 0 1 1 1v5.5a1 1 0 0 1-1 1h-5.5a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1zM4 13.5h5.5a1 1 0 0 1 1 1V20a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-5.5a1 1 0 0 1 1-1zM14.5 13.5H20a1 1 0 0 1 1 1V20a1 1 0 0 1-1 1h-5.5a1 1 0 0 1-1-1v-5.5a1 1 0 0 1 1-1z"
    const val Scan = "M3 7V5a2 2 0 0 1 2-2h2M17 3h2a2 2 0 0 1 2 2v2M21 17v2a2 2 0 0 1-2 2h-2M7 21H5a2 2 0 0 1-2-2v-2M8.5 12a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0"
    const val History = "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0M12 7v5l3 2"
    const val Book = "M4 19.5A2.5 2.5 0 0 1 6.5 17H20V3H6.5A2.5 2.5 0 0 0 4 5.5zM4 19.5A2.5 2.5 0 0 0 6.5 22H20v-5"
    const val Pin = "M12 21s-7-6.2-7-11a7 7 0 0 1 14 0c0 4.8-7 11-7 11zM9.5 10a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0"
    const val Speaker = "M11 5L6 9H2v6h4l5 4zM15.5 8.5a5 5 0 0 1 0 7M19 5a10 10 0 0 1 0 14"
    const val Info = "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0M12 11v5M12 8h.01"
    const val Check = "M9 11l3 3 8-8M20 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"
    const val Alert = "M10.3 3.9L1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0zM12 9v4M12 17h.01"
    const val Car = "M5 17h14v-4l-2-5H7l-2 5zM7 17v2M17 17v2M7.5 13.5h.01M16.5 13.5h.01"
    const val Trash = "M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14"
    const val Gauge = "M12 14l4-4M3.3 19a9.9 9.9 0 1 1 17.4 0M10.4 14a1.6 1.6 0 1 0 3.2 0a1.6 1.6 0 1 0-3.2 0M6 12h1M12 6v1M17 12h1M7.8 7.8l.7.7"
    const val Shield = "M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z"
    const val Power = "M12 3v8M6.4 6.4a8 8 0 1 0 11.2 0"
    const val ChevronRight = "M9 6l6 6-6 6"
    const val Close = "M6 6l12 12M18 6L6 18"
    const val Cpu = "M7 4h10a3 3 0 0 1 3 3v10a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3V7a3 3 0 0 1 3-3zM9 9h6v6H9zM9 1v3M15 1v3M9 20v3M15 20v3M1 9h3M1 15h3M20 9h3M20 15h3"
}
