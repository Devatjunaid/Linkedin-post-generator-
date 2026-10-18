package com.unpin.photoscrub.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Hand-drawn 24 px stroke icons, tinted by Icon(). */
object UnpinIcons {
    private fun stroke(name: String, vararg paths: String, width: Float = 1.9f): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = width,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private fun filled(name: String, d: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black))
        }.build()

    val Settings = stroke("settings", "M4 7h10M18 7h2M4 17h4M12 17h8", "M14 7a2 2 0 1 0 4 0a2 2 0 1 0 -4 0", "M8 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0")
    val Photos = stroke("photos", "M6 5h12a3 3 0 0 1 3 3v8a3 3 0 0 1 -3 3H6a3 3 0 0 1 -3 -3V8a3 3 0 0 1 3 -3z", "M3 15l5 -4l4 3l3 -2l6 4", "M14.5 9a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0")
    val Search = stroke("search", "M4.5 11a6.5 6.5 0 1 0 13 0a6.5 6.5 0 1 0 -13 0", "M16 16l4.5 4.5")
    val Home = stroke("home", "M4 11l8 -7l8 7v9H4z")
    val Grid = stroke("grid", "M3 5a2 2 0 0 1 2 -2h3.5a2 2 0 0 1 2 2v3.5a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2 -2z", "M13.5 5a2 2 0 0 1 2 -2H19a2 2 0 0 1 2 2v3.5a2 2 0 0 1 -2 2h-3.5a2 2 0 0 1 -2 -2z", "M3 15.5a2 2 0 0 1 2 -2h3.5a2 2 0 0 1 2 2V19a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2 -2z", "M13.5 15.5a2 2 0 0 1 2 -2H19a2 2 0 0 1 2 2V19a2 2 0 0 1 -2 2h-3.5a2 2 0 0 1 -2 -2z")
    val Clock = stroke("clock", "M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0 -17 0", "M12 7.5V12l3 2")
    val Back = stroke("back", "M15 5l-7 7l7 7", width = 2.1f)
    val Close = stroke("close", "M6 6l12 12M18 6L6 18", width = 2.1f)
    val More = filled("more", "M12 3.2a1.8 1.8 0 1 1 0 3.6a1.8 1.8 0 1 1 0 -3.6zM12 10.2a1.8 1.8 0 1 1 0 3.6a1.8 1.8 0 1 1 0 -3.6zM12 17.2a1.8 1.8 0 1 1 0 3.6a1.8 1.8 0 1 1 0 -3.6z")
    val Check = stroke("check", "M5 12.5l4.5 4.5L19 7.5", width = 3f)
    val Trash = stroke("trash", "M4 7h16M9 7V4h6v3M6 7l1 13h10l1 -13", width = 2.1f)
    val Arrow = stroke("arrow", "M5 12h14M13 6l6 6l-6 6", width = 2.3f)
    val Share = stroke("share", "M12 15V3M7 8l5 -5l5 5M5 13v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2 -2v-6", width = 2.1f)
    val Folder = stroke("folder", "M3 7a2 2 0 0 1 2 -2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2 -2z")
    val Plus = stroke("plus", "M12 5v14M5 12h14", width = 2.2f)
    val Lock = stroke("lock", "M6 11h12v9H6z", "M8.5 11V8a3.5 3.5 0 0 1 7 0v3")
    val Shield = stroke("shield", "M12 3l7 3v5c0 4.5 -3 8.2 -7 10c-4 -1.8 -7 -5.5 -7 -10V6z", "M9 12l2 2l4 -4")
    val Map = stroke("map", "M9 4L3 6v14l6 -2l6 2l6 -2V4l-6 2z", "M9 4v14M15 6v14")
    val Info = stroke("info", "M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0 -17 0", "M12 11v5M12 8h0.01")
    val External = stroke("external", "M14 4h6v6M20 4l-9 9M18 14v5a1 1 0 0 1 -1 1H5a1 1 0 0 1 -1 -1V7a1 1 0 0 1 1 -1h5")
    val Pin = filled("pin", "M12 2C8 2 5 5 5 9c0 5.3 7 13 7 13s7 -7.7 7 -13c0 -4 -3 -7 -7 -7zm0 9.5A2.5 2.5 0 1 1 12 6.5a2.5 2.5 0 0 1 0 5z")
    val Warning = stroke("warning", "M12 4l9 16H3z", "M12 10v4M12 17h0.01")
}
