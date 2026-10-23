package com.driveready.testprep.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.driveready.testprep.data.content.Maneuver
import com.driveready.testprep.data.content.Pose
import com.driveready.testprep.data.content.SceneItem
import com.driveready.testprep.ui.theme.Brand
import kotlin.math.floor

private val Asphalt = Color(0xFF3A3D44)
private val Pavement = Color(0xFF8F9296)
private val ParkedCarColor = Color(0xFFC9CBD1)
private val ConeColor = Color(0xFFFF7A1A)
private val IslandColor = Color(0xFF3F8F4F)

/**
 * Animated top-down diagram. [stepIndex] selects how far along the maneuver the car is;
 * [replayKey] restarts the animation from the beginning. Mirrored for left-hand traffic.
 */
@Composable
fun ManeuverDiagram(
    maneuver: Maneuver,
    stepIndex: Int,
    mirror: Boolean,
    replayKey: Int,
    modifier: Modifier = Modifier
) {
    val progress = remember(maneuver.id) { Animatable(0f) }
    val lastPose = (maneuver.poses.size - 1).toFloat()
    val target = if (maneuver.steps.isEmpty()) lastPose
    else ((stepIndex + 1).toFloat() / maneuver.steps.size * lastPose).coerceIn(0f, lastPose)

    LaunchedEffect(maneuver.id, replayKey) {
        progress.snapTo(0f)
        progress.animateTo(target, tween((900 * target).toInt().coerceAtLeast(600), easing = LinearEasing))
    }
    LaunchedEffect(target) {
        progress.animateTo(target, tween(1100))
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(100f / 62f)
            .clip(RoundedCornerShape(22.dp))
            .background(Asphalt)
            .semantics { contentDescription = "Diagram of ${maneuver.name}, step ${stepIndex + 1}" }
    ) {
        val k = size.width / 100f
        withTransform({ if (mirror) scale(-1f, 1f, pivot = center) }) {
            maneuver.scene.forEach { drawItem(it, k) }
            drawTrail(maneuver.poses, progress.value, k)
            val pose = interpolate(maneuver.poses, progress.value)
            drawVehicle(pose, k, Brand.Amber, maneuver.isTwoWheeler, mine = true)
        }
    }
}

private fun interpolate(poses: List<Pose>, p: Float): Pose {
    if (poses.isEmpty()) return Pose(50f, 31f, 0f)
    val i = floor(p).toInt().coerceIn(0, poses.lastIndex)
    val j = (i + 1).coerceAtMost(poses.lastIndex)
    val f = (p - i).coerceIn(0f, 1f)
    val a = poses[i]
    val b = poses[j]
    var da = b.angle - a.angle
    while (da > 180f) da -= 360f
    while (da < -180f) da += 360f
    return Pose(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f, a.angle + da * f)
}

private fun DrawScope.drawTrail(poses: List<Pose>, p: Float, k: Float) {
    if (poses.size < 2) return
    val path = Path()
    val steps = (p * 8).toInt().coerceAtLeast(1)
    for (n in 0..steps) {
        val pose = interpolate(poses, p * n / steps)
        if (n == 0) path.moveTo(pose.x * k, pose.y * k) else path.lineTo(pose.x * k, pose.y * k)
    }
    drawPath(path, Brand.Amber.copy(alpha = 0.7f), style = Stroke(width = 0.6f * k, pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.6f * k, 1.4f * k))))
}

private fun DrawScope.drawItem(item: SceneItem, k: Float) {
    when (item) {
        is SceneItem.Pavement -> drawRect(Pavement, Offset(item.left * k, item.top * k), Size((item.right - item.left) * k, (item.bottom - item.top) * k))
        is SceneItem.Kerb -> drawLine(Color.White, Offset(item.x1 * k, item.y1 * k), Offset(item.x2 * k, item.y2 * k), 0.8f * k)
        is SceneItem.LaneLine -> drawLine(Color.White.copy(alpha = 0.8f), Offset(item.x1 * k, item.y1 * k), Offset(item.x2 * k, item.y2 * k), 0.5f * k,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * k, 3f * k)))
        is SceneItem.BayLine -> drawLine(Color.White, Offset(item.x1 * k, item.y1 * k), Offset(item.x2 * k, item.y2 * k), 0.6f * k)
        is SceneItem.ParkedCar -> drawVehicle(Pose(item.x, item.y, item.angle), k, ParkedCarColor, twoWheeler = false, mine = false)
        is SceneItem.Cone -> {
            drawCircle(ConeColor, 1.3f * k, Offset(item.x * k, item.y * k))
            drawCircle(Color.White, 0.5f * k, Offset(item.x * k, item.y * k))
        }
        is SceneItem.Island -> {
            drawCircle(IslandColor, item.r * k, Offset(item.x * k, item.y * k))
            drawCircle(Color.White, item.r * k, Offset(item.x * k, item.y * k), style = Stroke(0.6f * k))
        }
    }
}

private fun DrawScope.drawVehicle(pose: Pose, k: Float, color: Color, twoWheeler: Boolean, mine: Boolean) {
    val length = if (twoWheeler) 6f else 14f
    val width = if (twoWheeler) 2.4f else 7f
    val c = Offset(pose.x * k, pose.y * k)
    rotate(pose.angle, c) {
        val tl = Offset(c.x - length / 2 * k, c.y - width / 2 * k)
        drawRoundRect(color, tl, Size(length * k, width * k), CornerRadius(1.6f * k))
        if (mine) drawRoundRect(Brand.Ink, tl, Size(length * k, width * k), CornerRadius(1.6f * k), style = Stroke(0.35f * k))
        if (!twoWheeler) {
            val glass = if (mine) Brand.Ink else Color(0xFF8D9097)
            // Windscreen (front = +x) and rear window.
            drawRoundRect(glass, Offset(c.x + 1.2f * k, c.y - 2.6f * k), Size(2.6f * k, 5.2f * k), CornerRadius(0.6f * k))
            drawRoundRect(glass, Offset(c.x - 5.2f * k, c.y - 2.6f * k), Size(1.8f * k, 5.2f * k), CornerRadius(0.6f * k))
        }
        if (mine) {
            val tip = Offset(c.x + (length / 2 + 2.2f) * k, c.y)
            val arrow = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(tip.x - 1.6f * k, tip.y - 1.3f * k)
                lineTo(tip.x - 1.6f * k, tip.y + 1.3f * k)
                close()
            }
            drawPath(arrow, Brand.Amber)
        }
    }
}
