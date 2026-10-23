package com.walkies.dogwalk.ui.splash

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * The Walkies logo (a person walking a dog) drawn with jointed limbs so it can walk.
 * Geometry matches ic_launcher_foreground.xml: a 100-unit box, y pointing down.
 *
 * @param phase walk cycle position, 0..1 (one full stride).
 * @param stride 0 = standing still (the static logo pose), 1 = full walking swing.
 */
fun DrawScope.drawWalkers(
    phase: Float,
    stride: Float,
    figure: Color,
    leash: Color,
    origin: Offset = Offset.Zero,
    unit: Float = size.minDimension / 100f,
) {
    fun p(x: Float, y: Float) = Offset(origin.x + x * unit, origin.y + y * unit)
    fun limb(from: Offset, angleDeg: Float, length: Float): Offset {
        val r = angleDeg * PI.toFloat() / 180f
        return Offset(from.x + sin(r) * length * unit, from.y + cos(r) * length * unit)
    }

    val t = phase * 2f * PI.toFloat()
    val s = sin(t)
    val c = cos(t)
    val bob = abs(c) * 1.4f * stride

    // --- Person ---------------------------------------------------------------------------
    val hip = p(32f, 53f - bob)
    val shoulder = p(35f, 30f - bob)
    val neck = p(35f, 28f - bob)
    val head = p(36f, 15f - bob)
    val legW = 9.5f * unit
    val armW = 8f * unit

    // Back arm swings against the legs; the front hand holds the leash.
    val upperArm = lerp(-28.6f, -8f - 24f * s, stride)
    val backElbow = limb(shoulder, upperArm, 12.5f)
    val backHand = limb(backElbow, upperArm + lerp(4.6f, 10f, stride), 9.8f)
    drawPolyline(listOf(shoulder, backElbow, backHand), figure, armW)

    // Legs swing in opposition; the knee bends more while a leg swings forward.
    // With stride = 0 the joints land exactly on the static logo pose.
    val frontThigh = lerp(25.2f, 24f * s, stride)
    val backThigh = lerp(-19.4f, -24f * s, stride)
    val frontKnee = limb(hip, frontThigh, 18.8f)
    val backKnee = limb(hip, backThigh, 18f)
    val frontFoot = limb(frontKnee, frontThigh - lerp(8.8f, 6f + 16f * max(0f, c), stride), 17.7f)
    val backFoot = limb(backKnee, backThigh - lerp(3f, 6f + 16f * max(0f, -c), stride), 18.4f)
    drawPolyline(listOf(hip, backKnee, backFoot), figure, armW)
    drawLine(figure, backFoot, backFoot + Offset(-5f * unit, 0f), strokeWidth = 5f * unit, cap = StrokeCap.Round)

    drawLine(figure, neck, hip, strokeWidth = legW, cap = StrokeCap.Round)
    drawPolyline(listOf(hip, frontKnee, frontFoot), figure, legW)
    drawLine(figure, frontFoot, frontFoot + Offset(6f * unit, 0f), strokeWidth = 5f * unit, cap = StrokeCap.Round)

    val frontElbow = p(43f, 40f - bob + 1.2f * s * stride)
    val hand = p(51f, 45f - bob + 1.6f * s * stride)
    drawPolyline(listOf(shoulder, frontElbow, hand), figure, legW)
    drawCircle(figure, radius = 7.5f * unit, center = head)

    // --- Dog --------------------------------------------------------------------------------
    val dogBob = abs(sin(t * 2f)) * 0.8f * stride
    val dy = -dogBob
    val legLen = 15.5f
    val dogLegW = 5f * unit
    listOf(
        Triple(66f, 74f, 1f),
        Triple(70f, 75f, -1f),
        Triple(82f, 75f, -1f),
        Triple(86f, 74f, 1f),
    ).forEachIndexed { i, (x, y, dir) ->
        val rest = when (i) {
            0 -> -17.4f
            1 -> 3.8f
            2 -> -11.3f
            else -> 10.6f
        }
        val angle = lerp(rest, dir * 22f * s, stride)
        val top = p(x, y + dy)
        drawLine(figure, top, limb(top, angle, legLen), strokeWidth = dogLegW, cap = StrokeCap.Round)
    }
    // Tail wags faster than the stride.
    val wag = sin(t * 2f) * 4f * stride
    val tail = Path().apply {
        val s = p(62f, 67f + dy)
        moveTo(s.x, s.y)
        val c = p(55f, 63f + dy)
        val e = p(55f + wag, 55f + dy)
        quadraticTo(c.x, c.y, e.x, e.y)
    }
    drawPath(tail, figure, style = Stroke(width = dogLegW, cap = StrokeCap.Round))

    val body = Path().apply {
        addRoundRectCompat(p(60f, 64f + dy), p(90f, 79f + dy), 7.5f * unit)
    }
    drawPath(body, figure)
    val headPath = Path().apply {
        moveTo(p(81f, 66f + dy))
        lineTo(p(84f, 57f + dy))
        cubicTo(p(85f, 53f + dy), p(88f, 51.5f + dy), p(91f, 52f + dy))
        cubicTo(p(93f, 52.5f + dy), p(94.5f, 54f + dy), p(95f, 56f + dy))
        lineTo(p(99f, 58.5f + dy))
        cubicTo(p(100.5f, 59.5f + dy), p(100f, 62f + dy), p(98f, 62.5f + dy))
        lineTo(p(93f, 63f + dy))
        cubicTo(p(92f, 66f + dy), p(91f, 69f + dy), p(89f, 71f + dy))
        close()
        moveTo(p(86f, 55f + dy))
        lineTo(p(87.5f, 46.5f + dy))
        lineTo(p(92.5f, 53f + dy))
        close()
    }
    drawPath(headPath, figure)
    drawLine(leash, p(83.5f, 61.5f + dy), p(90.5f, 64f + dy), strokeWidth = 2.4f * unit, cap = StrokeCap.Round)

    // --- Leash ------------------------------------------------------------------------------
    val collar = p(84f, 63f + dy)
    val sag = 3f + 2f * sin(t + 1f) * stride
    val leashPath = Path().apply {
        moveTo(hand.x, hand.y)
        cubicTo(
            hand.x + 9f * unit, hand.y + (13f + sag) * unit,
            collar.x - 14f * unit, collar.y + sag * unit * 0.2f,
            collar.x, collar.y,
        )
    }
    drawPath(leashPath, leash, style = Stroke(width = 2.6f * unit, cap = StrokeCap.Round))
}

private fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f

private fun DrawScope.drawPolyline(points: List<Offset>, color: Color, width: Float) {
    val path = Path().apply {
        moveTo(points.first())
        points.drop(1).forEach { lineTo(it) }
    }
    drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun Path.moveTo(o: Offset) = moveTo(o.x, o.y)
private fun Path.lineTo(o: Offset) = lineTo(o.x, o.y)
private fun Path.cubicTo(a: Offset, b: Offset, c: Offset) = cubicTo(a.x, a.y, b.x, b.y, c.x, c.y)

private fun Path.addRoundRectCompat(topLeft: Offset, bottomRight: Offset, radius: Float) {
    addRoundRect(
        androidx.compose.ui.geometry.RoundRect(
            left = topLeft.x,
            top = topLeft.y,
            right = bottomRight.x,
            bottom = bottomRight.y,
            radiusX = radius,
            radiusY = radius,
        ),
    )
}
