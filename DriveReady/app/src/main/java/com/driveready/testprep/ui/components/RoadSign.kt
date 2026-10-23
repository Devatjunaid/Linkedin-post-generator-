package com.driveready.testprep.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveready.testprep.data.model.SignStyle
import com.driveready.testprep.data.model.SignType
import com.driveready.testprep.ui.theme.Brand
import kotlin.math.cos
import kotlin.math.sin

private val Black = Color(0xFF15171C)
private val White = Color.White

/**
 * Draws a road sign natively, in the design family the selected country uses:
 * Vienna-style red triangles, or MUTCD-style yellow diamonds (US, Canada, Australia, NZ, Ireland).
 */
@Composable
fun RoadSign(
    type: SignType,
    style: SignStyle,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    value: String? = null,
    rectangularSpeed: Boolean = false,
    giveWayText: String? = null,
    description: String = type.name.lowercase().replace('_', ' ')
) {
    val measurer = rememberTextMeasurer()
    Canvas(
        modifier
            .size(size)
            .semantics { contentDescription = "Road sign: $description" }
    ) {
        drawSign(type, style, value, rectangularSpeed, giveWayText, measurer)
    }
}

private fun DrawScope.drawSign(
    type: SignType,
    style: SignStyle,
    value: String?,
    rectangularSpeed: Boolean,
    giveWayText: String?,
    measurer: TextMeasurer
) {
    val s = size.minDimension
    val c = Offset(size.width / 2, size.height / 2)
    when (type) {
        SignType.STOP -> {
            octagon(c, s * 0.48f, Brand.SignRed)
            octagonStroke(c, s * 0.44f, White, s * 0.03f)
            centeredText(measurer, "STOP", c, s * 0.22f, White)
        }
        SignType.GIVE_WAY -> {
            val p = Path().apply {
                moveTo(s * 0.04f, s * 0.12f); lineTo(s * 0.96f, s * 0.12f); lineTo(s * 0.5f, s * 0.92f); close()
            }
            drawPath(p, White)
            drawPath(p, Brand.SignRed, style = Stroke(s * 0.1f, join = StrokeJoin.Round))
            giveWayText?.let { centeredText(measurer, it, Offset(c.x, s * 0.36f), s * 0.11f, Black) }
        }
        SignType.NO_ENTRY -> {
            drawCircle(Brand.SignRed, s * 0.48f, c)
            drawRect(White, Offset(s * 0.18f, s * 0.42f), Size(s * 0.64f, s * 0.16f))
        }
        SignType.SPEED_LIMIT -> if (rectangularSpeed) {
            drawRoundRect(White, Offset(s * 0.16f, s * 0.04f), Size(s * 0.68f, s * 0.92f), CornerRadius(s * 0.06f))
            drawRoundRect(Black, Offset(s * 0.19f, s * 0.07f), Size(s * 0.62f, s * 0.86f), CornerRadius(s * 0.05f), style = Stroke(s * 0.025f))
            centeredText(measurer, "SPEED", Offset(c.x, s * 0.2f), s * 0.11f, Black)
            centeredText(measurer, "LIMIT", Offset(c.x, s * 0.33f), s * 0.11f, Black)
            centeredText(measurer, value ?: "", Offset(c.x, s * 0.63f), s * 0.34f, Black)
        } else {
            prohibitoryRing(c, s)
            centeredText(measurer, value ?: "", c, s * 0.36f, Black)
        }
        SignType.NATIONAL_SPEED_LIMIT -> {
            drawCircle(White, s * 0.48f, c)
            drawCircle(Black, s * 0.48f, c, style = Stroke(s * 0.02f))
            drawLine(Black, Offset(s * 0.2f, s * 0.8f), Offset(s * 0.8f, s * 0.2f), s * 0.14f)
        }
        SignType.NO_U_TURN -> {
            prohibitoryRing(c, s)
            val u = Path().apply {
                moveTo(s * 0.38f, s * 0.74f); lineTo(s * 0.38f, s * 0.42f)
                cubicTo(s * 0.38f, s * 0.24f, s * 0.62f, s * 0.24f, s * 0.62f, s * 0.42f)
                lineTo(s * 0.62f, s * 0.56f)
            }
            drawPath(u, Black, style = Stroke(s * 0.07f))
            arrowHead(Offset(s * 0.62f, s * 0.66f), 90f, s * 0.12f, Black)
            slash(c, s)
        }
        SignType.NO_LEFT_TURN -> {
            prohibitoryRing(c, s)
            val p = Path().apply {
                moveTo(s * 0.56f, s * 0.76f); lineTo(s * 0.56f, s * 0.5f)
                cubicTo(s * 0.56f, s * 0.42f, s * 0.5f, s * 0.4f, s * 0.42f, s * 0.4f)
            }
            drawPath(p, Black, style = Stroke(s * 0.07f))
            arrowHead(Offset(s * 0.32f, s * 0.4f), 180f, s * 0.12f, Black)
            slash(c, s)
        }
        SignType.NO_OVERTAKING -> {
            prohibitoryRing(c, s)
            car(Offset(s * 0.38f, s * 0.5f), s * 0.16f, Brand.SignRed)
            car(Offset(s * 0.62f, s * 0.5f), s * 0.16f, Black)
        }
        SignType.NO_PARKING -> {
            drawCircle(Brand.SignBlue, s * 0.46f, c)
            drawCircle(Brand.SignRed, s * 0.43f, c, style = Stroke(s * 0.09f))
            drawLine(Brand.SignRed, Offset(s * 0.22f, s * 0.22f), Offset(s * 0.78f, s * 0.78f), s * 0.09f)
        }
        SignType.KEEP_LEFT, SignType.KEEP_RIGHT -> {
            drawCircle(Brand.SignBlue, s * 0.48f, c)
            val left = type == SignType.KEEP_LEFT
            val start = Offset(if (left) s * 0.68f else s * 0.32f, s * 0.3f)
            val end = Offset(if (left) s * 0.36f else s * 0.64f, s * 0.64f)
            drawLine(White, start, end, s * 0.09f, cap = StrokeCap.Round)
            arrowHead(end, if (left) 135f else 45f, s * 0.2f, White)
        }
        SignType.MINI_ROUNDABOUT -> {
            drawCircle(Brand.SignBlue, s * 0.48f, c)
            for (i in 0 until 3) {
                val a0 = i * 120f
                drawArc(White, a0 + 10f, 80f, false, Offset(s * 0.24f, s * 0.24f), Size(s * 0.52f, s * 0.52f), style = Stroke(s * 0.07f))
                val end = Math.toRadians((a0 + 90f).toDouble())
                arrowHead(Offset(c.x + s * 0.26f * cos(end).toFloat(), c.y + s * 0.26f * sin(end).toFloat()), a0 + 180f, s * 0.13f, White)
            }
        }
        SignType.ONE_WAY -> {
            drawRoundRect(Brand.SignBlue, Offset(s * 0.12f, s * 0.04f), Size(s * 0.76f, s * 0.92f), CornerRadius(s * 0.06f))
            drawLine(White, Offset(c.x, s * 0.82f), Offset(c.x, s * 0.34f), s * 0.1f)
            arrowHead(Offset(c.x, s * 0.22f), -90f, s * 0.26f, White)
        }
        SignType.HOSPITAL, SignType.PARKING -> {
            drawRoundRect(Brand.SignBlue, Offset(s * 0.06f, s * 0.06f), Size(s * 0.88f, s * 0.88f), CornerRadius(s * 0.1f))
            centeredText(measurer, if (type == SignType.HOSPITAL) "H" else "P", c, s * 0.56f, White)
        }
        else -> {
            // Warning signs: frame depends on the country's sign family.
            val ink = warningFrame(style, s)
            val center = if (style == SignStyle.VIENNA) Offset(c.x, s * 0.6f) else c
            warningPictogram(type, center, s * 0.36f, ink)
        }
    }
}

private fun DrawScope.warningFrame(style: SignStyle, s: Float): Color = when (style) {
    SignStyle.VIENNA -> {
        val p = Path().apply { moveTo(s * 0.5f, s * 0.06f); lineTo(s * 0.96f, s * 0.88f); lineTo(s * 0.04f, s * 0.88f); close() }
        drawPath(p, White)
        drawPath(p, Brand.SignRed, style = Stroke(s * 0.09f, join = StrokeJoin.Round))
        Black
    }
    SignStyle.DIAMOND -> {
        val p = Path().apply { moveTo(s * 0.5f, s * 0.02f); lineTo(s * 0.98f, s * 0.5f); lineTo(s * 0.5f, s * 0.98f); lineTo(s * 0.02f, s * 0.5f); close() }
        drawPath(p, Brand.SignYellow)
        drawPath(p, Black, style = Stroke(s * 0.03f, join = StrokeJoin.Round))
        Black
    }
}

private fun DrawScope.warningPictogram(type: SignType, c: Offset, u: Float, ink: Color) {
    val w = u * 0.16f
    when (type) {
        SignType.PEDESTRIAN_CROSSING -> person(c, u, ink)
        SignType.CHILDREN -> {
            person(Offset(c.x - u * 0.28f, c.y + u * 0.08f), u * 0.82f, ink)
            person(Offset(c.x + u * 0.3f, c.y), u, ink)
        }
        SignType.SLIPPERY_ROAD -> {
            car(Offset(c.x, c.y - u * 0.25f), u * 0.5f, ink)
            val p1 = Path().apply { moveTo(c.x - u * 0.45f, c.y + u * 0.55f); cubicTo(c.x - u * 0.15f, c.y + u * 0.05f, c.x - u * 0.45f, c.y + u * 0.15f, c.x - u * 0.15f, c.y - u * 0.05f) }
            val p2 = Path().apply { moveTo(c.x + u * 0.45f, c.y + u * 0.55f); cubicTo(c.x + u * 0.15f, c.y + u * 0.05f, c.x + u * 0.45f, c.y + u * 0.15f, c.x + u * 0.15f, c.y - u * 0.05f) }
            drawPath(p1, ink, style = Stroke(w * 0.8f, cap = StrokeCap.Round))
            drawPath(p2, ink, style = Stroke(w * 0.8f, cap = StrokeCap.Round))
        }
        SignType.TRAFFIC_SIGNALS_AHEAD -> {
            drawRoundRect(ink, Offset(c.x - u * 0.22f, c.y - u * 0.62f), Size(u * 0.44f, u * 1.2f), CornerRadius(u * 0.1f))
            drawCircle(Color(0xFFFF4D4D), u * 0.13f, Offset(c.x, c.y - u * 0.36f))
            drawCircle(Brand.Amber, u * 0.13f, Offset(c.x, c.y))
            drawCircle(Color(0xFF2ED47A), u * 0.13f, Offset(c.x, c.y + u * 0.36f))
        }
        SignType.RAILWAY_CROSSING -> {
            rotate(35f, c) { drawRect(ink, Offset(c.x - u * 0.65f, c.y - w / 2), Size(u * 1.3f, w)) }
            rotate(-35f, c) { drawRect(ink, Offset(c.x - u * 0.65f, c.y - w / 2), Size(u * 1.3f, w)) }
        }
        SignType.TWO_WAY_TRAFFIC -> {
            drawLine(ink, Offset(c.x - u * 0.22f, c.y + u * 0.6f), Offset(c.x - u * 0.22f, c.y - u * 0.3f), w)
            arrowHead(Offset(c.x - u * 0.22f, c.y - u * 0.5f), -90f, u * 0.4f, ink)
            drawLine(ink, Offset(c.x + u * 0.22f, c.y - u * 0.6f), Offset(c.x + u * 0.22f, c.y + u * 0.3f), w)
            arrowHead(Offset(c.x + u * 0.22f, c.y + u * 0.5f), 90f, u * 0.4f, ink)
        }
        SignType.ROAD_WORKS -> {
            person(Offset(c.x - u * 0.15f, c.y - u * 0.05f), u, ink)
            drawLine(ink, Offset(c.x + u * 0.05f, c.y), Offset(c.x + u * 0.5f, c.y + u * 0.55f), w * 0.8f, cap = StrokeCap.Round)
            drawArc(ink, 180f, 180f, true, Offset(c.x + u * 0.15f, c.y + u * 0.45f), Size(u * 0.7f, u * 0.4f))
        }
        SignType.SHARP_BEND -> {
            val p = Path().apply { moveTo(c.x - u * 0.2f, c.y + u * 0.65f); lineTo(c.x - u * 0.2f, c.y - u * 0.1f); quadraticTo(c.x - u * 0.2f, c.y - u * 0.35f, c.x + u * 0.15f, c.y - u * 0.35f) }
            drawPath(p, ink, style = Stroke(w * 1.2f, join = StrokeJoin.Round))
            arrowHead(Offset(c.x + u * 0.4f, c.y - u * 0.35f), 0f, u * 0.42f, ink)
        }
        SignType.ROUNDABOUT_AHEAD -> {
            for (i in 0 until 3) {
                val a0 = i * 120f
                drawArc(ink, a0 + 15f, 75f, false, Offset(c.x - u * 0.45f, c.y - u * 0.45f), Size(u * 0.9f, u * 0.9f), style = Stroke(w))
                val end = Math.toRadians((a0 + 90f).toDouble())
                arrowHead(Offset(c.x + u * 0.45f * cos(end).toFloat(), c.y + u * 0.45f * sin(end).toFloat()), a0 + 180f, u * 0.3f, ink)
            }
        }
        else -> {
            drawLine(ink, Offset(c.x, c.y - u * 0.55f), Offset(c.x, c.y + u * 0.15f), w * 1.2f, cap = StrokeCap.Round)
            drawCircle(ink, w * 0.75f, Offset(c.x, c.y + u * 0.5f))
        }
    }
}

private fun DrawScope.prohibitoryRing(c: Offset, s: Float) {
    drawCircle(White, s * 0.48f, c)
    drawCircle(Brand.SignRed, s * 0.43f, c, style = Stroke(s * 0.1f))
}

private fun DrawScope.slash(c: Offset, s: Float) {
    drawLine(Brand.SignRed, Offset(s * 0.2f, s * 0.2f), Offset(s * 0.8f, s * 0.8f), s * 0.09f)
}

private fun DrawScope.octagon(c: Offset, r: Float, color: Color) {
    drawPath(octagonPath(c, r), color)
}

private fun DrawScope.octagonStroke(c: Offset, r: Float, color: Color, width: Float) {
    drawPath(octagonPath(c, r), color, style = Stroke(width))
}

private fun octagonPath(c: Offset, r: Float) = Path().apply {
    for (i in 0 until 8) {
        val a = Math.toRadians(22.5 + i * 45.0)
        val x = c.x + r * cos(a).toFloat()
        val y = c.y + r * sin(a).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** Filled arrowhead with its tip at [tip], pointing along [angleDeg] (0 = east, 90 = south). */
private fun DrawScope.arrowHead(tip: Offset, angleDeg: Float, length: Float, color: Color) {
    val a = Math.toRadians(angleDeg.toDouble())
    val back = Offset(tip.x - length * cos(a).toFloat(), tip.y - length * sin(a).toFloat())
    val nx = -sin(a).toFloat() * length * 0.55f
    val ny = cos(a).toFloat() * length * 0.55f
    val p = Path().apply {
        moveTo(tip.x, tip.y); lineTo(back.x + nx, back.y + ny); lineTo(back.x - nx, back.y - ny); close()
    }
    drawPath(p, color)
}

private fun DrawScope.person(c: Offset, u: Float, ink: Color) {
    val w = u * 0.14f
    drawCircle(ink, u * 0.13f, Offset(c.x, c.y - u * 0.5f))
    drawLine(ink, Offset(c.x, c.y - u * 0.33f), Offset(c.x - u * 0.05f, c.y + u * 0.1f), w, cap = StrokeCap.Round)
    drawLine(ink, Offset(c.x - u * 0.05f, c.y + u * 0.1f), Offset(c.x - u * 0.28f, c.y + u * 0.55f), w, cap = StrokeCap.Round)
    drawLine(ink, Offset(c.x - u * 0.05f, c.y + u * 0.1f), Offset(c.x + u * 0.22f, c.y + u * 0.55f), w, cap = StrokeCap.Round)
    drawLine(ink, Offset(c.x - u * 0.02f, c.y - u * 0.22f), Offset(c.x - u * 0.28f, c.y - u * 0.02f), w * 0.85f, cap = StrokeCap.Round)
    drawLine(ink, Offset(c.x - u * 0.02f, c.y - u * 0.22f), Offset(c.x + u * 0.26f, c.y - u * 0.05f), w * 0.85f, cap = StrokeCap.Round)
}

/** A rear-view car silhouette. */
private fun DrawScope.car(c: Offset, u: Float, color: Color) {
    drawRoundRect(color, Offset(c.x - u * 0.55f, c.y - u * 0.1f), Size(u * 1.1f, u * 0.55f), CornerRadius(u * 0.12f))
    drawRoundRect(color, Offset(c.x - u * 0.38f, c.y - u * 0.45f), Size(u * 0.76f, u * 0.42f), CornerRadius(u * 0.12f))
    drawRect(color, Offset(c.x - u * 0.5f, c.y + u * 0.4f), Size(u * 0.22f, u * 0.2f))
    drawRect(color, Offset(c.x + u * 0.28f, c.y + u * 0.4f), Size(u * 0.22f, u * 0.2f))
}

private fun DrawScope.centeredText(measurer: TextMeasurer, text: String, center: Offset, heightPx: Float, color: Color) {
    if (text.isEmpty()) return
    val fontSize = (heightPx / (density * fontScale)).sp
    val layout = measurer.measure(text, TextStyle(color = color, fontSize = fontSize, fontWeight = FontWeight.ExtraBold))
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
}
