package com.devatjunaid.examclock.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.devatjunaid.examclock.ui.theme.Brand

/**
 * The Exam Clock mark: a stopwatch ring that fills up as the countdown runs, with a tick in the
 * middle for "ready on exam day". Drawn on a 108-unit grid, the same as the launcher icon.
 */
@Composable
fun LogoMark(
    modifier: Modifier = Modifier,
    ringProgress: Float = 0.75f,
    checkProgress: Float = 1f,
    crownAlpha: Float = 1f,
    trackColor: Color = Brand.InkLine,
    ringColor: Color = Brand.BlueLight,
    checkColor: Color = Color.White,
    accentColor: Color = Brand.OrangeLight,
) {
    Canvas(modifier) {
        val u = size.minDimension / 108f
        val center = Offset(54f * u, 58f * u)
        val radius = 22f * u
        val stroke = 6f * u

        // Crown and stem of the stopwatch.
        drawRoundRect(
            color = ringColor.copy(alpha = crownAlpha),
            topLeft = Offset(47f * u, 23f * u),
            size = Size(14f * u, 6f * u),
            cornerRadius = CornerRadius(2f * u),
        )
        drawRect(color = ringColor.copy(alpha = crownAlpha), topLeft = Offset(52f * u, 28f * u), size = Size(4f * u, 6f * u))

        val arcTopLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2, radius * 2)
        drawArc(trackColor, 0f, 360f, false, arcTopLeft, arcSize, style = Stroke(stroke))
        val sweep = 270f * ringProgress.coerceIn(0f, 1f) / 0.75f
        drawArc(ringColor, -90f, sweep.coerceAtMost(360f), false, arcTopLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        if (ringProgress >= 0.74f) {
            drawCircle(accentColor, radius = 3.6f * u, center = Offset(32f * u, 58f * u))
        }

        if (checkProgress > 0f) {
            val check = Path().apply {
                moveTo(44f * u, 58.5f * u)
                lineTo(51f * u, 65.5f * u)
                lineTo(65f * u, 50.5f * u)
            }
            val measure = PathMeasure().apply { setPath(check, false) }
            val partial = Path()
            measure.getSegment(0f, measure.length * checkProgress.coerceIn(0f, 1f), partial, true)
            drawPath(partial, checkColor, style = Stroke(5.5f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
