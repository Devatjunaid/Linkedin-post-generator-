package com.driveready.testprep.ui.hazard

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.driveready.testprep.data.content.HazardClip
import com.driveready.testprep.data.content.HazardKind
import com.driveready.testprep.data.content.HazardScene
import kotlin.math.max
import kotlin.math.min

private const val SPEED = 3f          // world units per second
private const val NEAR = 1f
private const val FAR = 22f

/**
 * Renders one frame of a hazard clip from the driver's seat. World coordinates: x in road
 * half-widths (road edges at ±1), z = distance ahead. [kerbSide] is +1 for right-hand
 * traffic (kerb on the right) and -1 for left-hand traffic.
 */
class HazardSceneRenderer(private val clip: HazardClip, private val kerbSide: Float) {

    /** Distance travelled by our car: steady, then the "driver" brakes after the hazard. */
    private fun travelled(t: Float): Float {
        val brakeStart = clip.developAt + 1.4f
        val brakeEnd = brakeStart + 2.6f
        return when {
            t <= brakeStart -> SPEED * t
            t <= brakeEnd -> {
                val dt = t - brakeStart
                val decel = SPEED / (brakeEnd - brakeStart)
                SPEED * brakeStart + SPEED * dt - 0.5f * decel * dt * dt
            }
            else -> SPEED * brakeStart + 0.5f * SPEED * (brakeEnd - brakeStart)
        }
    }

    private val hazardWorldZ get() = 6.5f + travelled(clip.developAt)

    fun draw(scope: DrawScope, t: Float) = with(scope) {
        val w = size.width
        val h = size.height
        val horizon = h * 0.42f
        val cx = w / 2f
        val halfBottom = w * 0.62f

        fun sy(z: Float) = horizon + (h - horizon) * (NEAR / z)
        fun sx(x: Float, z: Float) = cx + x * halfBottom * (NEAR / z)
        fun scale(z: Float) = NEAR / z

        // Sky and ground.
        val sky = when (clip.scene) {
            HazardScene.RURAL -> listOf(Color(0xFF2B2F5A), Color(0xFFE59866))
            else -> listOf(Color(0xFF6FA8DC), Color(0xFFD6E9F8))
        }
        drawRect(Brush.verticalGradient(sky, endY = horizon), size = Size(w, horizon))
        drawRect(if (clip.scene == HazardScene.RURAL) Color(0xFF4E7A3A) else Color(0xFF9BA39A), Offset(0f, horizon), Size(w, h - horizon))

        // Road surface and pavements.
        val road = Path().apply {
            moveTo(sx(-1f, FAR), sy(FAR)); lineTo(sx(1f, FAR), sy(FAR))
            lineTo(sx(1f, NEAR * 0.7f), h); lineTo(sx(-1f, NEAR * 0.7f), h); close()
        }
        if (clip.scene != HazardScene.RURAL) {
            val pave = Path().apply {
                moveTo(sx(-1.35f, FAR), sy(FAR)); lineTo(sx(1.35f, FAR), sy(FAR))
                lineTo(sx(1.35f, NEAR * 0.7f), h); lineTo(sx(-1.35f, NEAR * 0.7f), h); close()
            }
            drawPath(pave, Color(0xFFBFC3C7))
        }
        drawPath(road, Color(0xFF4A4D55))

        val d = travelled(t)

        // Centre dashes.
        val spacing = 2.4f
        var z = spacing - (d % spacing)
        while (z < FAR) {
            if (z > NEAR * 0.8f) {
                val z2 = z + 0.9f
                val p = Path().apply {
                    moveTo(sx(-0.02f, z), sy(z)); lineTo(sx(0.02f, z), sy(z))
                    lineTo(sx(0.02f, z2), sy(z2)); lineTo(sx(-0.02f, z2), sy(z2)); close()
                }
                drawPath(p, Color.White.copy(alpha = 0.85f))
            }
            z += spacing
        }

        // Roadside objects (trees or lamp posts) on both sides.
        var oz = 3f - (d % 3f)
        while (oz < FAR) {
            if (oz > NEAR) {
                listOf(-1.7f, 1.7f).forEach { x ->
                    val s = scale(oz)
                    if (clip.scene == HazardScene.RURAL) {
                        drawCircle(Color(0xFF2F5E2A), 150f * s, Offset(sx(x, oz), sy(oz) - 220f * s))
                        drawRect(Color(0xFF5B3A1E), Offset(sx(x, oz) - 14f * s, sy(oz) - 120f * s), Size(28f * s, 120f * s))
                    } else {
                        drawRect(Color(0xFF6B7078), Offset(sx(x, oz) - 6f * s, sy(oz) - 380f * s), Size(12f * s, 380f * s))
                        drawRect(Color(0xFF6B7078), Offset(sx(x, oz) - (if (x < 0) -6f else 60f) * s, sy(oz) - 380f * s), Size(66f * s, 10f * s))
                    }
                }
            }
            oz += 3f
        }

        // Scene furniture that belongs to each clip.
        val objects = mutableListOf<Pair<Float, () -> Unit>>() // (z, drawer) drawn far → near

        if (clip.kind == HazardKind.CHILD_RUNS_OUT || clip.kind == HazardKind.PEDESTRIAN_STEPS_OUT || clip.kind == HazardKind.CYCLIST_SWERVES) {
            // Row of parked cars along the kerb.
            var pz = 4f - (d % 4f)
            while (pz < FAR) {
                val wz = pz
                val worldZ = wz + d
                val nearHazard = kotlin.math.abs(worldZ - hazardWorldZ) < 1.2f && clip.kind == HazardKind.PEDESTRIAN_STEPS_OUT
                if (wz > NEAR && !nearHazard) objects += wz to { car(this, sx(kerbSide * 1.05f, wz), sy(wz), scale(wz), Color(0xFF8E99A8), false) }
                pz += 4f
            }
        }

        when (clip.kind) {
            HazardKind.CHILD_RUNS_OUT -> {
                val hz = hazardWorldZ - d
                // Ball rolls out first (the clue), then the child follows.
                val ballStart = clip.developAt - 1.6f
                if (t > ballStart && hz > NEAR) {
                    val bx = kerbSide * (1.1f - min(1.4f, (t - ballStart) * 0.9f))
                    objects += (hz - 0.01f) to { drawCircle(Color(0xFFE53935), 40f * scale(hz), Offset(sx(bx, hz), sy(hz) - 40f * scale(hz))) }
                }
                if (t > clip.developAt - 0.4f && hz > NEAR) {
                    val px = kerbSide * (1.15f - max(0f, t - clip.developAt) * 0.55f)
                    objects += (hz - 0.02f) to { person(this, sx(px, hz), sy(hz), scale(hz) * 0.75f, Color(0xFF1E88E5)) }
                }
            }
            HazardKind.CAR_EMERGES -> {
                val hz = hazardWorldZ - d
                if (hz > NEAR) {
                    // Side road mouth.
                    objects += (hz + 0.6f) to {
                        val p = Path().apply {
                            moveTo(sx(kerbSide * 1f, hz + 0.6f), sy(hz + 0.6f)); lineTo(sx(kerbSide * 2.4f, hz + 0.6f), sy(hz + 0.6f))
                            lineTo(sx(kerbSide * 2.4f, hz - 0.6f), sy(hz - 0.6f)); lineTo(sx(kerbSide * 1f, hz - 0.6f), sy(hz - 0.6f)); close()
                        }
                        drawPath(p, Color(0xFF4A4D55))
                    }
                    val creep = ((t - (clip.developAt - 3f)) / 3f).coerceIn(0f, 1f)
                    val pull = max(0f, t - clip.developAt) * 0.6f
                    val ex = kerbSide * (1.9f - creep * 0.6f - pull)
                    objects += hz to { car(this, sx(ex, hz), sy(hz), scale(hz), Color(0xFFD32F2F), false, side = true) }
                }
            }
            HazardKind.LEAD_CAR_BRAKES -> {
                val since = max(0f, t - clip.developAt)
                val a = 3.2f
                val leadTravel = if (t < clip.developAt) 0f else min(SPEED * since - 0.5f * a * since * since, SPEED * SPEED / (2 * a))
                val lz = 5f + (if (t < clip.developAt) 0f else (leadTravel - (d - travelled(clip.developAt))))
                if (lz > NEAR) objects += lz to { car(this, sx(kerbSide * 0.5f, lz), sy(lz), scale(lz), Color(0xFF263238), t >= clip.developAt) }
            }
            HazardKind.CYCLIST_SWERVES -> {
                val vz = 7f + travelled(clip.developAt + 1f) - d
                if (vz > NEAR) objects += vz to { van(this, sx(kerbSide * 1.0f, vz), sy(vz), scale(vz)) }
                val cyclistZ = 9f + 1.2f * t - d
                val out = ((t - clip.developAt) / 1.5f).coerceIn(0f, 1f)
                val cxw = kerbSide * (0.85f - out * 0.5f)
                if (cyclistZ > NEAR) objects += cyclistZ to { cyclist(this, sx(cxw, cyclistZ), sy(cyclistZ), scale(cyclistZ)) }
            }
            HazardKind.PEDESTRIAN_STEPS_OUT -> {
                val hz = hazardWorldZ - d
                if (hz > NEAR) {
                    objects += (hz + 0.5f) to {
                        for (i in 0 until 7) {
                            val x0 = -1f + i * 0.3f
                            val p = Path().apply {
                                moveTo(sx(x0, hz + 0.4f), sy(hz + 0.4f)); lineTo(sx(x0 + 0.16f, hz + 0.4f), sy(hz + 0.4f))
                                lineTo(sx(x0 + 0.16f, hz - 0.4f), sy(hz - 0.4f)); lineTo(sx(x0, hz - 0.4f), sy(hz - 0.4f)); close()
                            }
                            drawPath(p, Color.White)
                        }
                    }
                    val px = kerbSide * (1.2f - max(0f, t - clip.developAt) * 0.5f)
                    objects += (hz - 0.1f) to { person(this, sx(px, hz), sy(hz), scale(hz), Color(0xFF6A1B9A)) }
                }
            }
            HazardKind.ANIMAL_CROSSES -> {
                val signZ = hazardWorldZ - 9f - d
                if (signZ > NEAR) objects += signZ to { warningSign(this, sx(kerbSide * 1.4f, signZ), sy(signZ), scale(signZ)) }
                val hz = hazardWorldZ - d
                if (t > clip.developAt - 1f && hz > NEAR) {
                    val ax = -kerbSide * (1.6f - max(0f, t - clip.developAt + 1f) * 0.7f)
                    objects += hz to { deer(this, sx(ax, hz), sy(hz), scale(hz)) }
                }
            }
        }

        objects.sortedByDescending { it.first }.forEach { it.second() }

        // Dashboard edge for a driver's-eye feel.
        drawRoundRect(Color(0xFF15171C), Offset(-w * 0.1f, h * 0.93f), Size(w * 1.2f, h * 0.2f), CornerRadius(w * 0.4f, w * 0.1f))
    }

    private fun car(s: DrawScope, x: Float, y: Float, k: Float, color: Color, braking: Boolean, side: Boolean = false) = with(s) {
        val bw = (if (side) 420f else 300f) * k
        val bh = 150f * k
        drawRoundRect(color, Offset(x - bw / 2, y - bh), Size(bw, bh), CornerRadius(26f * k))
        drawRoundRect(Color(0xFF263238).copy(alpha = 0.85f), Offset(x - bw * 0.36f, y - bh * 1.45f), Size(bw * 0.72f, bh * 0.55f), CornerRadius(22f * k))
        drawRoundRect(color, Offset(x - bw * 0.38f, y - bh * 1.5f), Size(bw * 0.76f, bh * 0.12f), CornerRadius(10f * k))
        val light = if (braking) Color(0xFFFF1744) else Color(0xFF7F1D1D)
        if (!side) {
            drawRoundRect(light, Offset(x - bw * 0.46f, y - bh * 0.72f), Size(bw * 0.16f, bh * 0.2f), CornerRadius(6f * k))
            drawRoundRect(light, Offset(x + bw * 0.30f, y - bh * 0.72f), Size(bw * 0.16f, bh * 0.2f), CornerRadius(6f * k))
            if (braking) drawCircle(Color(0x55FF1744), bw * 0.22f, Offset(x - bw * 0.38f, y - bh * 0.62f))
            if (braking) drawCircle(Color(0x55FF1744), bw * 0.22f, Offset(x + bw * 0.38f, y - bh * 0.62f))
        }
        drawRect(Color(0xFF111111), Offset(x - bw * 0.42f, y - 8f * k), Size(bw * 0.2f, 22f * k))
        drawRect(Color(0xFF111111), Offset(x + bw * 0.22f, y - 8f * k), Size(bw * 0.2f, 22f * k))
    }

    private fun van(s: DrawScope, x: Float, y: Float, k: Float) = with(s) {
        drawRoundRect(Color(0xFFECEFF1), Offset(x - 170f * k, y - 300f * k), Size(340f * k, 300f * k), CornerRadius(20f * k))
        drawRect(Color(0xFFB0BEC5), Offset(x - 4f * k, y - 290f * k), Size(8f * k, 280f * k))
    }

    private fun person(s: DrawScope, x: Float, y: Float, k: Float, color: Color) = with(s) {
        drawCircle(Color(0xFFFFCC80), 34f * k, Offset(x, y - 300f * k))
        drawRoundRect(color, Offset(x - 40f * k, y - 262f * k), Size(80f * k, 140f * k), CornerRadius(20f * k))
        drawRect(Color(0xFF37474F), Offset(x - 34f * k, y - 125f * k), Size(26f * k, 125f * k))
        drawRect(Color(0xFF37474F), Offset(x + 8f * k, y - 125f * k), Size(26f * k, 125f * k))
    }

    private fun cyclist(s: DrawScope, x: Float, y: Float, k: Float) = with(s) {
        drawCircle(Color(0xFF212121), 60f * k, Offset(x, y - 60f * k))
        drawRect(Color(0xFF212121), Offset(x - 6f * k, y - 170f * k), Size(12f * k, 110f * k))
        drawRoundRect(Color(0xFFFFEB3B), Offset(x - 45f * k, y - 300f * k), Size(90f * k, 130f * k), CornerRadius(20f * k))
        drawCircle(Color(0xFF1565C0), 36f * k, Offset(x, y - 330f * k))
    }

    private fun deer(s: DrawScope, x: Float, y: Float, k: Float) = with(s) {
        drawRoundRect(Color(0xFF8D5524), Offset(x - 150f * k, y - 230f * k), Size(300f * k, 110f * k), CornerRadius(50f * k))
        for (lx in listOf(-120f, -80f, 80f, 120f)) drawRect(Color(0xFF6D4C41), Offset(x + lx * k, y - 130f * k), Size(16f * k, 130f * k))
        drawRoundRect(Color(0xFF8D5524), Offset(x + 120f * k, y - 330f * k), Size(60f * k, 130f * k), CornerRadius(26f * k))
        drawLine(Color(0xFF4E342E), Offset(x + 150f * k, y - 330f * k), Offset(x + 120f * k, y - 400f * k), 10f * k)
        drawLine(Color(0xFF4E342E), Offset(x + 160f * k, y - 330f * k), Offset(x + 200f * k, y - 400f * k), 10f * k)
    }

    private fun warningSign(s: DrawScope, x: Float, y: Float, k: Float) = with(s) {
        drawRect(Color(0xFF6B7078), Offset(x - 6f * k, y - 360f * k), Size(12f * k, 360f * k))
        val p = Path().apply {
            moveTo(x, y - 560f * k); lineTo(x + 120f * k, y - 360f * k); lineTo(x - 120f * k, y - 360f * k); close()
        }
        drawPath(p, Color(0xFFD7261E))
        val inner = Path().apply {
            moveTo(x, y - 520f * k); lineTo(x + 88f * k, y - 378f * k); lineTo(x - 88f * k, y - 378f * k); close()
        }
        drawPath(inner, Color.White)
        drawCircle(Color(0xFF15171C), 22f * k, Offset(x, y - 420f * k))
    }
}
