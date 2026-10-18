package com.devatjunaid.balconysolar.ui.graphics

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.devatjunaid.balconysolar.ui.theme.Palette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Globe with the user's latitude band, a pin at their longitude and sun rays at the June noon angle. */
@Composable
fun GlobeGraphic(latitude: Double, longitude: Double, modifier: Modifier = Modifier) {
    val lat by animateFloatAsState(latitude.toFloat(), spring(stiffness = 120f), label = "lat")
    val lon by animateFloatAsState(longitude.toFloat(), spring(stiffness = 120f), label = "lon")
    val t = rememberInfiniteTransition(label = "globe")
    val spin by t.animateFloat(0f, 1f, infiniteRepeatable(tween(16_000, easing = LinearEasing)), label = "spin")
    Canvas(modifier.semantics { contentDescription = "Globe showing your latitude" }) {
        val w = size.width
        val h = size.height
        drawRect(Brush.radialGradient(listOf(Palette.Amber.copy(alpha = 0.30f), Color.Transparent), Offset(w * 0.86f, h * 0.2f), w * 0.5f))
        val stars = listOf(0.07f to 0.15f, 0.17f to 0.86f, 0.79f to 0.82f, 0.92f to 0.6f, 0.11f to 0.5f, 0.6f to 0.1f)
        for (s in stars) drawCircle(Palette.Faint, 1.2f, Offset(w * s.first, h * s.second))

        val r = h * 0.43f
        val c = Offset(w * 0.42f, h * 0.55f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFF1B3A60), Color(0xFF0F2540)), c - Offset(r * 0.3f, r * 0.3f), r * 1.4f), r, c)
        val globe = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c.x - r, c.y - r, c.x + r, c.y + r)) }
        clipPath(globe) {
            val grid = Palette.LineStrong
            for (la in listOf(-60, -30, 0, 30, 60)) {
                val y = c.y - r * sin(la * PI / 180).toFloat()
                val hw = r * cos(la * PI / 180).toFloat()
                drawOval(grid, Offset(c.x - hw, y - hw * 0.16f), Size(hw * 2, hw * 0.32f), style = Stroke(1f))
            }
            // Meridians drift slowly to suggest rotation.
            for (k in 0 until 6) {
                val phase = ((k / 6f + spin) % 1f)
                val rx = r * abs(cos(phase * PI)).toFloat()
                drawOval(grid, Offset(c.x - rx, c.y - r), Size(rx * 2, r * 2), style = Stroke(1f))
            }
        }
        drawCircle(Palette.LineStrong, r, c, style = Stroke(1.2f))

        // Highlighted latitude band.
        val y = c.y - r * sin(lat * PI / 180).toFloat()
        val hw = r * cos(lat * PI / 180).toFloat()
        drawOval(Palette.Amber, Offset(c.x - hw, y - hw * 0.16f), Size(hw * 2, hw * 0.32f), style = Stroke(2.6f))
        // Pin on the front side of the band; longitude shifts it sideways.
        val px = c.x + hw * sin(((lon % 90) / 90f) * PI / 2).toFloat() * 0.85f
        val py = y + hw * 0.16f
        val pin = Path().apply {
            moveTo(px, py)
            cubicTo(px - 10f, py - 9f, px - 10f, py - 22f, px, py - 24f)
            cubicTo(px + 10f, py - 22f, px + 10f, py - 9f, px, py)
            close()
        }
        drawPath(pin, Palette.Mint)
        drawCircle(Palette.Night, 3.6f, Offset(px, py - 15f))

        // Sun and rays toward the pin.
        val sun = Offset(w * 0.86f, h * 0.22f)
        for (i in 0 until 3) {
            drawLine(
                Palette.Amber.copy(alpha = 0.8f), Offset(sun.x - 14f, sun.y + i * 8f), Offset(px + 12f + i * 8f, py - 18f + i * 16f),
                strokeWidth = 1.3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 7f)),
            )
        }
        drawCircle(Palette.Amber.copy(alpha = 0.3f), 30f, sun, style = Stroke(1.5f))
        drawCircle(Palette.Amber, 20f, sun)
    }
}

/** Side view of a balcony with the panel at the chosen tilt and the sun at its noon elevation. */
@Composable
fun BalconyGraphic(tiltDeg: Double, sunElevationDeg: Double, panelCount: Int, modifier: Modifier = Modifier) {
    val tilt by animateFloatAsState(tiltDeg.toFloat(), spring(stiffness = 140f), label = "tilt")
    val elev by animateFloatAsState(sunElevationDeg.toFloat().coerceIn(5f, 85f), tween(600), label = "elev")
    val t = rememberInfiniteTransition(label = "rays")
    val dash by t.animateFloat(0f, 28f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "dash")
    Canvas(modifier.semantics { contentDescription = "Side view of your panel at ${tiltDeg.toInt()} degrees" }) {
        val w = size.width
        val h = size.height
        val floor = h * 0.80f
        // Building wall with windows.
        drawRect(Color(0xFF18304E), Offset.Zero, Size(w * 0.15f, h))
        drawRoundRect(Palette.Line, Offset(w * 0.035f, h * 0.10f), Size(w * 0.085f, h * 0.24f), CornerRadius(4f))
        drawRoundRect(Color(0xFF2E4C74), Offset(w * 0.045f, h * 0.12f), Size(w * 0.065f, h * 0.20f), CornerRadius(3f))
        drawRoundRect(Palette.Line, Offset(w * 0.035f, h * 0.45f), Size(w * 0.085f, h * 0.24f), CornerRadius(4f))
        // Balcony slab and railing.
        drawRect(Palette.Line, Offset(w * 0.15f, floor), Size(w * 0.70f, h * 0.06f))
        val railX = w * 0.82f
        val railTop = floor - h * 0.32f
        drawLine(Color(0xFF4A6283), Offset(w * 0.15f, railTop), Offset(railX + 4f, railTop), 5f, StrokeCap.Round)
        drawLine(Color(0xFF4A6283), Offset(railX, railTop), Offset(railX, floor), 5f, StrokeCap.Round)
        for (i in 1..3) drawLine(Palette.LineStrong, Offset(w * 0.15f + i * w * 0.17f, railTop), Offset(w * 0.15f + i * w * 0.17f, floor), 2.5f)

        // Sun placed on a circle around the panel at the noon elevation.
        val pivot = Offset(w * 0.64f, floor - 3f)
        val sunDist = h * 0.95f
        val sunPos = Offset(
            (pivot.x + sunDist * cos(elev * PI / 180)).toFloat().coerceAtMost(w - 18f),
            (pivot.y - sunDist * sin(elev * PI / 180)).toFloat().coerceAtLeast(18f),
        )
        val len = h * 0.62f
        val rad = tilt * PI / 180
        val top = Offset(pivot.x - (len * cos(rad)).toFloat(), pivot.y - (len * sin(rad)).toFloat())
        for (i in 0 until 3) {
            val f = 0.3f + i * 0.28f
            val target = Offset(pivot.x + (top.x - pivot.x) * f, pivot.y + (top.y - pivot.y) * f)
            drawLine(
                Palette.Amber.copy(alpha = 0.85f), sunPos, target, 1.6f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), dash),
            )
        }
        drawCircle(Palette.Amber.copy(alpha = 0.25f), 24f, sunPos)
        drawCircle(Palette.Amber, 15f, sunPos)

        // Stand leg and the panel (thickness shows a second panel behind when there are two).
        val mid = Offset((pivot.x + top.x) / 2, (pivot.y + top.y) / 2)
        drawLine(Palette.Muted, mid, Offset(mid.x, floor), 3f)
        if (panelCount > 1) drawLine(Color(0xFF14325E), pivot + Offset(8f, -4f), top + Offset(8f, -4f), 11f, StrokeCap.Round)
        drawLine(Palette.Cell, pivot, top, 11f, StrokeCap.Round)
        val cells = 5
        for (i in 1 until cells) {
            val p = Offset(pivot.x + (top.x - pivot.x) * i / cells, pivot.y + (top.y - pivot.y) * i / cells)
            drawCircle(Color(0xFF8BB8F5), 1.6f, p)
        }
        drawLine(Color(0xFF5BA0FF), pivot, top, 1.4f)

        // Tilt angle arc between the floor and the panel.
        val arcR = h * 0.17f
        drawArc(
            Palette.Mint, 180f, tilt, false,
            topLeft = Offset(pivot.x - arcR, pivot.y - arcR), size = Size(arcR * 2, arcR * 2), style = Stroke(2.5f),
        )
        drawLine(Palette.Muted.copy(alpha = 0.6f), pivot, Offset(pivot.x - arcR * 1.6f, pivot.y), 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 4f)))
    }
}

/** Compass dial showing the direction the panel faces, with a shaded field of view. */
@Composable
fun CompassGraphic(azimuthDeg: Double, modifier: Modifier = Modifier, live: Boolean = false) {
    val az by animateFloatAsState(azimuthDeg.toFloat(), spring(stiffness = if (live) 300f else 120f), label = "az")
    Canvas(modifier.semantics { contentDescription = "Compass: panel faces ${azimuthDeg.toInt()} degrees" }) {
        val c = center
        val r = size.minDimension / 2 - 4f
        drawCircle(Palette.Night, r, c)
        drawCircle(Palette.LineStrong, r, c, style = Stroke(1.5f))
        for (i in 0 until 72) {
            val a = (i * 5 - 90) * PI / 180
            val long = i % 18 == 0
            val r0 = r - if (long) 12f else if (i % 2 == 0) 7f else 4f
            drawLine(
                if (long) Palette.Muted else Palette.LineStrong,
                Offset(c.x + r0 * cos(a).toFloat(), c.y + r0 * sin(a).toFloat()),
                Offset(c.x + (r - 2f) * cos(a).toFloat(), c.y + (r - 2f) * sin(a).toFloat()),
                strokeWidth = if (long) 2f else 1f,
            )
        }
        // North marker.
        val n = Path().apply { moveTo(c.x, c.y - r + 14f); lineTo(c.x - 5f, c.y - r + 24f); lineTo(c.x + 5f, c.y - r + 24f); close() }
        drawPath(n, Palette.Coral)
        rotate(az, c) {
            drawArc(
                Palette.Amber.copy(alpha = 0.16f), 90f - 45f + 180f, 90f, true,
                topLeft = Offset(c.x - r * 0.8f, c.y - r * 0.8f), size = Size(r * 1.6f, r * 1.6f),
            )
            val needle = Path().apply { moveTo(c.x, c.y - r * 0.72f); lineTo(c.x - 6f, c.y); lineTo(c.x + 6f, c.y); close() }
            drawPath(needle, Palette.Amber)
            val tail = Path().apply { moveTo(c.x, c.y + r * 0.45f); lineTo(c.x - 6f, c.y); lineTo(c.x + 6f, c.y); close() }
            drawPath(tail, Palette.LineStrong)
        }
        drawCircle(Palette.Ink, 4f, c)
    }
}

/** Quarter-circle gauge from flat (0°) to vertical (90°). */
@Composable
fun TiltGauge(tiltDeg: Double, modifier: Modifier = Modifier) {
    val tilt by animateFloatAsState(tiltDeg.toFloat(), spring(stiffness = 140f), label = "gauge")
    Canvas(modifier.semantics { contentDescription = "Tilt gauge ${tiltDeg.toInt()} degrees" }) {
        val pivot = Offset(size.width * 0.12f, size.height * 0.9f)
        val r = minOf(size.width * 0.78f, size.height * 0.8f)
        val box = Size(r * 2, r * 2)
        val tl = Offset(pivot.x - r, pivot.y - r)
        drawArc(Palette.Line, 270f, 90f, false, tl, box, style = Stroke(10f, cap = StrokeCap.Round))
        drawArc(Palette.Mint, 360f - tilt, tilt, false, tl, box, style = Stroke(10f, cap = StrokeCap.Round))
        val a = tilt * PI / 180
        drawLine(Palette.Amber, pivot, Offset(pivot.x + (r * cos(a)).toFloat(), pivot.y - (r * sin(a)).toFloat()), 3.5f, StrokeCap.Round)
        drawCircle(Palette.Ink, 5f, pivot)
    }
}
