package com.feedthewrongthing.game.models

import com.feedthewrongthing.game.engine.DrawCtx
import com.feedthewrongthing.game.engine.Meshes
import com.feedthewrongthing.game.engine.RenderFrame
import com.feedthewrongthing.game.game.CreatureType
import com.feedthewrongthing.game.game.Hat
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Visual state of a creature that the model reads every frame. */
class Pose {
    var sx = 1f
    var sy = 1f
    var sz = 1f
    var mouth = 0f
    var blink = 0f
    var sleepy = 0f
    var lookX = 0f
    var lookY = 0f
    var cheek = 0f
    var tint = 0
    var tintAmt = 0f
    var spinEyes = false
    var spinAngle = 0f
    var heartEyes = false
    var beak = 0f
    var bunny = 0f
    var blocky = 0f
    var hula = 0f
    var hulaAngle = 0f
    var flap = 0f
    var wiggle = 0f
    var tiltZ = 0f
    var yaw = 0f
    var brow = 0f
    var hat = Hat.NONE
    var time = 0f
    var rainbow = false

    fun reset() {
        sx = 1f; sy = 1f; sz = 1f; mouth = 0f; blink = 0f; sleepy = 0f; lookX = 0f; lookY = 0f
        cheek = 0f; tint = 0; tintAmt = 0f; spinEyes = false; spinAngle = 0f; heartEyes = false
        beak = 0f; bunny = 0f; blocky = 0f; hula = 0f; hulaAngle = 0f; tiltZ = 0f; yaw = 0f; brow = 0f
        rainbow = false
    }
}

/** Procedural 3D creatures built from primitive shapes. Origin is between the feet. */
object CreatureModels {
    private const val INK = 0x1A1428
    private const val MOUTH = 0x5A1530
    private const val TONGUE = 0xFF6F93
    private const val WHITE = 0xFFFFFF
    private const val BLUSH = 0xFF8FAB

    fun draw(ctx: DrawCtx, type: CreatureType, p: Pose) {
        ctx.push()
        ctx.rotateY(p.yaw)
        ctx.rotateZ(p.tiltZ)
        ctx.scale(p.sx, p.sy, p.sz)
        val keepTint = ctx.tintAmount
        val keepCol = ctx.tintColor
        if (p.tintAmt > 0f && keepTint < 0.9f) {
            ctx.tintColor = if (p.rainbow) rainbow(p.time) else p.tint
            ctx.tintAmount = max(keepTint, p.tintAmt)
        }
        if (p.blocky > 0.01f) {
            blockBody(ctx, type, p)
        } else when (type) {
            CreatureType.MUNCHY, CreatureType.BIG_MAMA -> munchy(ctx, type, p)
            CreatureType.GULPO -> gulpo(ctx, type, p)
            CreatureType.SLURPO -> slurpo(ctx, type, p)
            CreatureType.BLORP -> blorp(ctx, type, p)
            CreatureType.CHOMPETTE -> chompette(ctx, type, p)
            CreatureType.GRUMBO -> grumbo(ctx, type, p)
            CreatureType.FIZZLE -> fizzle(ctx, type, p)
            CreatureType.SNOOZLE -> snoozle(ctx, type, p)
            CreatureType.TWIRLZ -> twirlz(ctx, type, p)
        }
        extras(ctx, type, p)
        val hat = when {
            p.hat != Hat.NONE -> p.hat
            type == CreatureType.BIG_MAMA -> Hat.CROWN
            else -> Hat.NONE
        }
        if (hat != Hat.NONE) {
            ctx.push()
            ctx.translate(0f, type.hatY + (if (p.bunny > 0.2f) 0.05f else 0f), 0f)
            ctx.scale(type.hatScale)
            HatModels.draw(ctx, hat, p.time)
            ctx.pop()
        } else if (type == CreatureType.SNOOZLE) {
            ctx.push(); ctx.translate(0f, type.hatY, 0f); nightcap(ctx); ctx.pop()
        } else if (type == CreatureType.TWIRLZ) {
            ctx.push(); ctx.translate(0f, type.hatY, 0f); ctx.scale(type.hatScale); HatModels.draw(ctx, Hat.TOP_HAT, p.time); ctx.pop()
        }
        ctx.tintAmount = keepTint
        ctx.tintColor = keepCol
        ctx.pop()
    }

    private fun rainbow(t: Float): Int {
        val h = (t * 0.6f) % 1f
        val r = (0.5f + 0.5f * cos(6.2832f * (h + 0f))).coerceIn(0f, 1f)
        val g = (0.5f + 0.5f * cos(6.2832f * (h + 0.33f))).coerceIn(0f, 1f)
        val b = (0.5f + 0.5f * cos(6.2832f * (h + 0.67f))).coerceIn(0f, 1f)
        return ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
    }

    // ---------- shared face parts ----------

    fun eye(ctx: DrawCtx, x: Float, y: Float, z: Float, r: Float, p: Pose, pupil: Float = 0.5f) {
        ctx.push()
        ctx.translate(x, y, z)
        val close = max(p.blink, p.sleepy)
        ctx.push()
        ctx.scale(r, r * max(0.1f, 1f - close), r * 0.85f)
        ctx.draw(Meshes.sphere, WHITE, gloss = 0.75f, rim = 0.15f)
        ctx.pop()
        if (close < 0.8f) {
            val open = 1f - close
            if (p.heartEyes) {
                val beat = 1f + 0.15f * sin(p.time * 14f)
                ctx.part(Meshes.heart, 0xFF2E63, 0f, 0f, r * 0.72f, r * 0.55f * beat, r * 0.55f * beat * open, r * 0.3f,
                    gloss = 0.8f, emissive = 0.25f)
            } else {
                val ox: Float
                val oy: Float
                if (p.spinEyes) {
                    ox = cos(p.spinAngle) * r * 0.38f; oy = sin(p.spinAngle) * r * 0.38f
                } else {
                    ox = p.lookX * r * 0.32f; oy = p.lookY * r * 0.3f
                }
                ctx.part(Meshes.sphere, INK, ox, oy * open, r * 0.62f, r * pupil, r * pupil * open, r * pupil * 0.6f, gloss = 0.9f, rim = 0.1f)
                ctx.part(Meshes.sphereLo, WHITE, ox - r * 0.16f, (oy + r * 0.18f) * open, r * 0.92f, r * 0.15f, r * 0.15f * open, r * 0.08f,
                    mode = RenderFrame.UNLIT)
            }
        }
        ctx.pop()
    }

    fun mouth(ctx: DrawCtx, y: Float, z: Float, w: Float, p: Pose, teeth: Int = 2, fangs: Boolean = false) {
        val open = p.mouth.coerceIn(0f, 1.2f)
        val h = 0.06f + open * 0.34f
        ctx.part(Meshes.sphere, MOUTH, 0f, y - open * 0.07f, z - 0.06f, w, h, 0.16f, gloss = 0.5f, rim = 0f)
        if (open > 0.12f) {
            ctx.part(Meshes.sphere, TONGUE, 0f, y - h * 0.62f - open * 0.05f, z - 0.02f, w * 0.62f, h * 0.36f, 0.12f, gloss = 0.6f)
            if (teeth > 0) {
                val tw = w * 0.16f
                for (k in 0 until teeth) {
                    val tx = (k - (teeth - 1) / 2f) * tw * 2.3f
                    if (fangs) ctx.part(Meshes.cone, WHITE, tx, y + h * 0.55f - 0.05f, z + 0.03f, tw * 0.7f, -tw * 0.9f, tw * 0.7f, gloss = 0.7f)
                    else ctx.part(Meshes.roundCube, WHITE, tx, y + h * 0.62f - 0.05f, z + 0.04f, tw, tw * 0.9f, 0.04f, gloss = 0.7f)
                }
            }
        }
    }

    private fun blush(ctx: DrawCtx, x: Float, y: Float, z: Float) {
        ctx.part(Meshes.sphereLo, BLUSH, x, y, z, 0.13f, 0.07f, 0.05f, gloss = 0.1f, rim = 0f)
        ctx.part(Meshes.sphereLo, BLUSH, -x, y, z, 0.13f, 0.07f, 0.05f, gloss = 0.1f, rim = 0f)
    }

    private fun cheeks(ctx: DrawCtx, color: Int, x: Float, y: Float, z: Float, p: Pose) {
        if (p.cheek <= 0.02f) return
        val r = 0.2f + 0.42f * p.cheek
        for (s in intArrayOf(-1, 1)) {
            ctx.part(Meshes.sphere, color, s * (x + p.cheek * 0.18f), y, z, r, r * 0.92f, r * 0.85f, gloss = 0.6f, rim = 0.5f)
            ctx.part(Meshes.sphereLo, BLUSH, s * (x + p.cheek * 0.18f + 0.05f), y - 0.02f, z + r * 0.7f, r * 0.5f, r * 0.3f, r * 0.15f, gloss = 0.1f)
        }
    }

    private fun feet(ctx: DrawCtx, color: Int, x: Float, z: Float, s: Float = 1f) {
        ctx.part(Meshes.sphere, color, x, 0.08f, z, 0.3f * s, 0.14f * s, 0.36f * s, gloss = 0.5f)
        ctx.part(Meshes.sphere, color, -x, 0.08f, z, 0.3f * s, 0.14f * s, 0.36f * s, gloss = 0.5f)
    }

    // ---------- creatures ----------

    private fun munchy(ctx: DrawCtx, t: CreatureType, p: Pose) {
        val body = t.color
        feet(ctx, t.dark, 0.42f, 0.28f)
        ctx.part(Meshes.sphere, t.dark, 0.98f, 0.8f + p.wiggle * 0.1f, 0.12f, 0.16f, 0.24f, 0.16f, rz = -20f - p.wiggle * 30f)
        ctx.part(Meshes.sphere, t.dark, -0.98f, 0.8f + p.wiggle * 0.1f, 0.12f, 0.16f, 0.24f, 0.16f, rz = 20f + p.wiggle * 30f)
        ctx.part(Meshes.teardrop, body, 0f, 1.88f, 0f, 0.14f, 0.24f, 0.14f, rz = 8f * sin(p.time * 3f), gloss = 0.6f)
        ctx.part(Meshes.egg, body, 0f, 0.95f, 0f, 1.0f, 0.95f, 0.92f, gloss = 0.62f, rim = 0.45f)
        ctx.part(Meshes.sphere, t.light, 0f, 0.6f, 0.42f, 0.62f, 0.48f, 0.5f, gloss = 0.4f)
        cheeks(ctx, body, 0.62f, 0.78f, 0.45f, p)
        eye(ctx, 0.34f, 1.28f, 0.7f, 0.26f, p)
        eye(ctx, -0.34f, 1.28f, 0.7f, 0.26f, p)
        brows(ctx, 0.34f, 1.6f, 0.78f, p, t.dark)
        blush(ctx, 0.6f, 0.95f, 0.68f)
        mouth(ctx, t.mouthY, t.mouthZ, 0.42f, p)
    }

    private fun gulpo(ctx: DrawCtx, t: CreatureType, p: Pose) {
        val body = t.color
        feet(ctx, t.dark, 0.38f, 0.25f, 0.9f)
        ctx.part(Meshes.cylinder, t.dark, -0.12f, 2.5f, 0f, 0.035f, 0.22f, 0.035f, rz = 25f + 10f * sin(p.time * 4f))
        ctx.part(Meshes.sphere, 0xFFD23F, -0.24f + 0.05f * sin(p.time * 4f), 2.72f, 0f, 0.11f, gloss = 0.8f, emissive = 0.3f)
        ctx.part(Meshes.sphere, t.dark, 0.88f, 0.95f, 0.1f, 0.14f, 0.24f, 0.14f, rz = -25f - p.wiggle * 30f)
        ctx.part(Meshes.sphere, t.dark, -0.88f, 0.95f, 0.1f, 0.14f, 0.24f, 0.14f, rz = 25f + p.wiggle * 30f)
        ctx.part(Meshes.pear, body, 0f, 1.18f, 0f, 0.92f, 1.18f, 0.88f, gloss = 0.6f, rim = 0.45f)
        ctx.part(Meshes.sphere, t.light, 0f, 0.6f, 0.4f, 0.55f, 0.45f, 0.48f, gloss = 0.3f)
        cheeks(ctx, body, 0.55f, 0.95f, 0.42f, p)
        eye(ctx, 0f, 1.66f, 0.5f, 0.36f, p, 0.48f)
        brows(ctx, 0f, 2.08f, 0.6f, p, t.dark, single = true)
        blush(ctx, 0.5f, 1.15f, 0.7f)
        mouth(ctx, t.mouthY, t.mouthZ, 0.48f, p, teeth = 2)
        if (p.mouth < 0.12f) {
            ctx.part(Meshes.roundCube, WHITE, 0.07f, t.mouthY - 0.07f, t.mouthZ + 0.02f, 0.06f, 0.07f, 0.03f)
            ctx.part(Meshes.roundCube, WHITE, -0.07f, t.mouthY - 0.07f, t.mouthZ + 0.02f, 0.06f, 0.07f, 0.03f)
        }
    }

    private fun slurpo(ctx: DrawCtx, t: CreatureType, p: Pose) {
        val body = t.color
        for (k in 0 until 5) {
            val a = (k / 5f) * 6.2832f + 0.6f
            val sway = 18f * sin(p.time * 3f + k)
            ctx.push()
            ctx.translate(cos(a) * 0.55f, 0.42f, sin(a) * 0.55f)
            ctx.rotateY(-a * 57.3f)
            ctx.rotateZ(180f + sway)
            ctx.translate(0f, 0.18f, 0f)
            ctx.scale(0.17f, 0.42f, 0.17f)
            ctx.draw(Meshes.teardrop, t.dark, gloss = 0.85f, rim = 0.5f)
            ctx.pop()
        }
        ctx.part(Meshes.sphere, body, 0f, 1.05f, 0f, 1.0f, 0.84f, 0.98f, gloss = 0.92f, rim = 0.7f)
        for ((x, y, z) in listOf(Triple(0.35f, 1.75f, 0.2f), Triple(-0.5f, 1.6f, 0.0f), Triple(0.62f, 1.4f, -0.2f))) {
            ctx.part(Meshes.sphereLo, t.light, x, y, z, 0.13f, 0.06f, 0.13f, gloss = 0.6f)
        }
        cheeks(ctx, body, 0.6f, 0.86f, 0.5f, p)
        eye(ctx, 0.32f, 1.25f, 0.76f, 0.24f, p)
        eye(ctx, -0.32f, 1.25f, 0.76f, 0.24f, p)
        brows(ctx, 0.32f, 1.54f, 0.82f, p, t.dark)
        blush(ctx, 0.58f, 1.0f, 0.74f)
        mouth(ctx, t.mouthY, t.mouthZ, 0.26f, p, teeth = 0)
    }

    private fun blorp(ctx: DrawCtx, t: CreatureType, p: Pose) {
        val body = t.color
        feet(ctx, t.dark, 0.36f, 0.25f, 0.8f)
        ctx.part(Meshes.fuzzy, body, 0f, 0.98f, 0f, 0.96f, 0.94f, 0.94f, gloss = 0.25f, rim = 0.6f)
        ctx.part(Meshes.teardrop, body, 0.2f, 1.9f, 0f, 0.1f, 0.18f, 0.1f, rz = -25f)
        ctx.part(Meshes.teardrop, body, -0.18f, 1.88f, 0f, 0.09f, 0.16f, 0.09f, rz = 30f)
        cheeks(ctx, body, 0.58f, 0.75f, 0.45f, p)
        eye(ctx, -0.4f, 1.2f, 0.74f, 0.2f, p)
        eye(ctx, 0f, 1.42f, 0.78f, 0.24f, p)
        eye(ctx, 0.4f, 1.2f, 0.74f, 0.2f, p)
        blush(ctx, 0.58f, 0.92f, 0.72f)
        mouth(ctx, t.mouthY, t.mouthZ, 0.44f, p, teeth = 3)
    }

    private fun chompette(ctx: DrawCtx, t: CreatureType, p: Pose) {
        val body = t.color
        val flap = sin(p.flap) * 25f
        feet(ctx, t.dark, 0.34f, 0.2f, 0.75f)
        ctx.part(Meshes.cone, body, 0f, 0.4f, -0.82f, 0.17f, 0.42f, 0.17f, rx = -72f, gloss = 0.6f)
        ctx.part(Meshes.cone, 0xFFD23F, 0f, 0.62f, -1.18f, 0.1f, 0.1f, 0.04f, rx = -72f)
        ctx.part(Meshes.teardrop, 0xFFD23F, 0.82f, 1.2f, -0.3f, 0.3f, 0.42f, 0.06f, rz = -55f - flap, ry = -20f, gloss = 0.6f)
        ctx.part(Meshes.teardrop, 0xFFD23F, -0.82f, 1.2f, -0.3f, 0.3f, 0.42f, 0.06f, rz = 55f + flap, ry = 20f, gloss = 0.6f)
        ctx.part(Meshes.cone, WHITE, 0.3f, 1.78f, 0.05f, 0.09f, 0.18f, 0.09f, rz = -15f)
        ctx.part(Meshes.cone, WHITE, -0.3f, 1.78f, 0.05f, 0.09f, 0.18f, 0.09f, rz = 15f)
        ctx.part(Meshes.egg, body, 0f, 0.95f, 0f, 0.86f, 0.9f, 0.8f, gloss = 0.62f, rim = 0.45f)
        ctx.part(Meshes.sphere, t.light, 0f, 0.62f, 0.38f, 0.52f, 0.45f, 0.45f, gloss = 0.4f)
        ctx.part(Meshes.sphereLo, t.dark, 0.09f, 1.03f, 0.78f, 0.035f)
        ctx.part(Meshes.sphereLo, t.dark, -0.09f, 1.03f, 0.78f, 0.035f)
        cheeks(ctx, body, 0.52f, 0.78f, 0.42f, p)
        eye(ctx, 0.3f, 1.26f, 0.64f, 0.22f, p)
        eye(ctx, -0.3f, 1.26f, 0.64f, 0.22f, p)
        brows(ctx, 0.3f, 1.52f, 0.72f, p, t.dark)
        blush(ctx, 0.52f, 0.96f, 0.64f)
        mouth(ctx, t.mouthY, t.mouthZ, 0.4f, p, teeth = 4, fangs = true)
    }

    private fun grumbo(ctx: DrawCtx, t: CreatureType, p: Pose) {
        val body = t.color
        feet(ctx, t.dark, 0.46f, 0.25f, 1.1f)
        ctx.part(Meshes.sphere, t.dark, 1.05f, 0.82f, 0.1f, 0.27f, 0.36f, 0.27f, rz = -15f - p.wiggle * 30f)
        ctx.part(Meshes.sphere, t.dark, -1.05f, 0.82f, 0.1f, 0.27f, 0.36f, 0.27f, rz = 15f + p.wiggle * 30f)
        ctx.part(Meshes.roundCube, body, 0f, 1.0f, 0f, 0.95f, 0.95f, 0.78f, gloss = 0.3f, rim = 0.35f)
        for ((x, y) in listOf(0.6f to 0.4f, -0.55f to 1.55f, 0.7f to 1.4f, -0.68f to 0.5f)) {
            ctx.part(Meshes.sphereLo, t.dark, x, y, 0.74f, 0.1f, 0.1f, 0.06f)
        }
        ctx.part(Meshes.fuzzySoft, 0x6CC35A, 0f, 1.93f, 0f, 0.78f, 0.2f, 0.62f, gloss = 0.2f)
        ctx.part(Meshes.fuzzy, 0x5AB54A, 0.3f, 2.02f, 0.1f, 0.22f, gloss = 0.2f)
        ctx.part(Meshes.fuzzy, 0x5AB54A, -0.35f, 2.0f, -0.1f, 0.2f, gloss = 0.2f)
        ctx.part(Meshes.roundCube, 0x3A3A52, 0f, 1.5f + p.brow * 0.05f, 0.76f, 0.5f, 0.07f, 0.06f, rz = p.brow * 8f)
        cheeks(ctx, body, 0.62f, 0.78f, 0.45f, p)
        eye(ctx, 0.3f, 1.28f, 0.73f, 0.17f, p, 0.55f)
        eye(ctx, -0.3f, 1.28f, 0.73f, 0.17f, p, 0.55f)
        mouth(ctx, t.mouthY, t.mouthZ, 0.62f, p, teeth = 0)
        ctx.part(Meshes.cone, 0xFFF6E5, 0.34f, t.mouthY + 0.02f, t.mouthZ + 0.03f, 0.07f, 0.13f, 0.07f)
        ctx.part(Meshes.cone, 0xFFF6E5, -0.34f, t.mouthY + 0.02f, t.mouthZ + 0.03f, 0.07f, 0.13f, 0.07f)
    }

    private fun fizzle(ctx: DrawCtx, t: CreatureType, p: Pose) {
        val body = t.color
        feet(ctx, t.dark, 0.32f, 0.2f, 0.8f)
        ctx.part(Meshes.bottle, body, 0f, 0.8f, 0f, 0.75f, 0.8f, 0.75f, gloss = 0.85f, rim = 0.55f)
        ctx.part(Meshes.cylinder, t.light, 0f, 0.55f, 0f, 0.77f, 0.1f, 0.77f, gloss = 0.5f)
        ctx.part(Meshes.cylinder, 0xFF5A5A, 0f, 1.98f, 0f, 0.38f, 0.07f, 0.38f, gloss = 0.7f)
        for (k in 0 until 3) {
            val ph = (p.time * 0.8f + k * 0.33f) % 1f
            ctx.part(Meshes.sphereLo, WHITE, (k - 1) * 0.15f + 0.05f * sin(p.time * 5f + k), 2.05f + ph * 0.6f, 0f,
                0.05f + 0.04f * k, alpha = 1f - ph, gloss = 0.9f)
        }
        ctx.part(Meshes.sphere, t.dark, 0.78f, 0.9f, 0.1f, 0.13f, 0.22f, 0.13f, rz = -25f - p.wiggle * 30f)
        ctx.part(Meshes.sphere, t.dark, -0.78f, 0.9f, 0.1f, 0.13f, 0.22f, 0.13f, rz = 25f + p.wiggle * 30f)
        cheeks(ctx, body, 0.5f, 0.72f, 0.38f, p)
        eye(ctx, 0.3f, 1.15f, 0.62f, 0.22f, p)
        eye(ctx, -0.3f, 1.15f, 0.62f, 0.22f, p)
        brows(ctx, 0.3f, 1.42f, 0.66f, p, t.dark)
        blush(ctx, 0.5f, 0.9f, 0.64f)
        mouth(ctx, t.mouthY, t.mouthZ, 0.38f, p)
    }

    private fun snoozle(ctx: DrawCtx, t: CreatureType, p: Pose) {
        val body = t.color
        ctx.part(Meshes.sphere, body, 0f, 0.98f, 0f, 0.95f, 0.8f, 0.85f, gloss = 0.3f, rim = 0.6f)
        for ((x, y, r) in listOf(Triple(0.75f, 0.78f, 0.55f), Triple(-0.75f, 0.78f, 0.55f), Triple(0f, 1.52f, 0.55f),
            Triple(0.48f, 1.4f, 0.45f), Triple(-0.48f, 1.4f, 0.45f), Triple(0f, 0.42f, 0.5f))) {
            ctx.part(Meshes.sphere, body, x, y, 0f, r, r * 0.9f, r * 0.9f, gloss = 0.3f, rim = 0.6f)
        }
        cheeks(ctx, body, 0.6f, 0.82f, 0.48f, p)
        val keep = p.sleepy
        p.sleepy = max(p.sleepy, 0.42f)
        eye(ctx, 0.32f, 1.2f, 0.74f, 0.22f, p)
        eye(ctx, -0.32f, 1.2f, 0.74f, 0.22f, p)
        p.sleepy = keep
        blush(ctx, 0.58f, 0.95f, 0.72f)
        mouth(ctx, t.mouthY, t.mouthZ, 0.3f, p, teeth = 0)
    }

    private fun twirlz(ctx: DrawCtx, t: CreatureType, p: Pose) {
        feet(ctx, 0xFF6B6B, 0.25f, 0.18f, 0.7f)
        val sway = sin(p.time * 4f) * 30f + p.wiggle * 40f
        ctx.part(Meshes.capsule, 0xFF6B6B, 0.72f, 1.05f, 0f, 0.07f, 0.16f, 0.07f, rz = -60f - sway)
        ctx.part(Meshes.capsule, 0xFF6B6B, -0.72f, 1.05f, 0f, 0.07f, 0.16f, 0.07f, rz = 60f + sway)
        ctx.part(Meshes.capsule, WHITE, 0f, 1.0f, 0f, 0.62f, 0.48f, 0.62f, gloss = 0.75f, rim = 0.4f,
            pattern = 3, color2 = 0xFF4D5E, patternScale = 2.2f)
        cheeks(ctx, WHITE, 0.4f, 1.0f, 0.4f, p)
        eye(ctx, 0.24f, 1.42f, 0.53f, 0.19f, p)
        eye(ctx, -0.24f, 1.42f, 0.53f, 0.19f, p)
        blush(ctx, 0.4f, 1.2f, 0.54f)
        mouth(ctx, t.mouthY, t.mouthZ, 0.3f, p)
    }

    private fun blockBody(ctx: DrawCtx, t: CreatureType, p: Pose) {
        val k = p.blocky.coerceIn(0f, 1f)
        feet(ctx, t.dark, 0.42f, 0.28f)
        ctx.part(Meshes.roundCube, t.color, 0f, 0.95f, 0f, 0.9f, 0.9f * (0.7f + 0.3f * k), 0.85f, gloss = 0.5f, rim = 0.4f)
        ctx.part(Meshes.roundCube, 0xFFD23F, 0f, 0.95f, 0.86f, 0.5f, 0.5f, 0.03f)
        eye(ctx, 0.3f, 1.25f, 0.88f, 0.22f, p)
        eye(ctx, -0.3f, 1.25f, 0.88f, 0.22f, p)
        mouth(ctx, 0.72f, 0.92f, 0.36f, p)
    }

    private fun brows(ctx: DrawCtx, x: Float, y: Float, z: Float, p: Pose, color: Int, single: Boolean = false) {
        if (p.brow == 0f) return
        val ang = p.brow * 18f
        if (single) {
            ctx.part(Meshes.capsule, color, 0f, y, z, 0.045f, 0.07f, 0.045f, rz = 90f)
            return
        }
        ctx.part(Meshes.capsule, color, x, y, z, 0.04f, 0.05f, 0.04f, rz = 90f + ang)
        ctx.part(Meshes.capsule, color, -x, y, z, 0.04f, 0.05f, 0.04f, rz = 90f - ang)
    }

    private fun extras(ctx: DrawCtx, t: CreatureType, p: Pose) {
        if (p.beak > 0.01f) {
            val b = p.beak
            ctx.part(Meshes.sphere, 0xFF9A3C, 0f, t.mouthY + 0.05f, t.mouthZ + 0.12f * b, 0.36f * b, 0.12f * b, 0.28f * b, gloss = 0.7f)
            ctx.part(Meshes.sphere, 0xE07A1C, 0f, t.mouthY - 0.07f, t.mouthZ + 0.08f * b, 0.3f * b, 0.08f * b, 0.22f * b, gloss = 0.6f)
            ctx.part(Meshes.teardrop, 0xFFD23F, 0f, t.hatY + 0.05f, 0f, 0.1f * b, 0.2f * b, 0.1f * b)
        }
        if (p.bunny > 0.01f) {
            val b = p.bunny
            for (s in intArrayOf(-1, 1)) {
                ctx.push()
                ctx.translate(s * 0.3f, t.hatY - 0.1f, 0f)
                ctx.rotateZ(-s * (12f + 8f * sin(p.time * 6f)))
                ctx.translate(0f, 0.45f * b, 0f)
                ctx.scale(0.16f * b, 0.5f * b, 0.1f * b)
                ctx.draw(Meshes.sphere, WHITE, gloss = 0.3f)
                ctx.translate(0f, 0f, 0.6f)
                ctx.scale(0.6f, 0.8f, 0.5f)
                ctx.draw(Meshes.sphere, 0xFFB3C6, gloss = 0.2f)
                ctx.pop()
            }
        }
        if (p.hula > 0.01f) {
            ctx.push()
            ctx.translate(cos(p.hulaAngle) * 0.18f, 0.85f + 0.05f * sin(p.hulaAngle * 2f), sin(p.hulaAngle) * 0.18f)
            ctx.rotateX(8f * sin(p.hulaAngle)); ctx.rotateZ(8f * cos(p.hulaAngle))
            ctx.scale(1.15f * p.hula, 0.9f, 1.15f * p.hula)
            ctx.draw(Meshes.torus, 0xE0A866, gloss = 0.5f)
            ctx.translate(0f, 0.05f, 0f)
            ctx.scale(1.01f, 0.7f, 1.01f)
            ctx.draw(Meshes.torus, 0xFF8FC8, gloss = 0.6f)
            ctx.pop()
        }
    }

    private fun nightcap(ctx: DrawCtx) {
        ctx.part(Meshes.cylinder, WHITE, 0f, 0.02f, 0f, 0.42f, 0.06f, 0.42f, gloss = 0.2f)
        ctx.part(Meshes.cone, 0x5B8CFF, 0.1f, 0.38f, 0f, 0.4f, 0.4f, 0.4f, rz = -28f, gloss = 0.3f, pattern = 2, color2 = 0x8FB1FF, patternScale = 2.5f)
        ctx.part(Meshes.fuzzy, WHITE, 0.52f, 0.62f, 0f, 0.13f, gloss = 0.2f)
    }
}

/** Equippable hats, drawn at the creature's head anchor. */
object HatModels {
    private const val WHITE = 0xFFFFFF

    fun draw(ctx: DrawCtx, hat: Hat, time: Float) {
        when (hat) {
            Hat.NONE -> {}
            Hat.PARTY -> {
                ctx.part(Meshes.cone, 0xFF6B8B, 0f, 0.3f, 0f, 0.3f, 0.32f, 0.3f, gloss = 0.6f, pattern = 2, color2 = 0xFFD23F, patternScale = 2.4f)
                ctx.part(Meshes.fuzzy, WHITE, 0f, 0.66f, 0f, 0.11f, gloss = 0.2f)
            }
            Hat.CROWN -> {
                ctx.part(Meshes.cylinderOpen, 0xFFC21F, 0f, 0.12f, 0f, 0.36f, 0.13f, 0.36f, gloss = 0.95f, rim = 0.5f)
                for (k in 0 until 6) {
                    val a = k / 6f * 6.2832f
                    ctx.part(Meshes.cone, 0xFFC21F, cos(a) * 0.36f, 0.33f, sin(a) * 0.36f, 0.08f, 0.1f, 0.08f, gloss = 0.95f)
                    ctx.part(Meshes.sphereLo, 0xFFE68A, cos(a) * 0.36f, 0.44f, sin(a) * 0.36f, 0.04f, gloss = 0.9f, emissive = 0.3f)
                }
                ctx.part(Meshes.sphere, 0xFF2E63, 0f, 0.13f, 0.36f, 0.07f, 0.07f, 0.04f, gloss = 0.95f, emissive = 0.2f)
                ctx.part(Meshes.sphere, 0x3E8BFF, 0.3f, 0.13f, 0.2f, 0.05f, 0.05f, 0.03f, gloss = 0.95f)
                ctx.part(Meshes.sphere, 0x3FC48A, -0.3f, 0.13f, 0.2f, 0.05f, 0.05f, 0.03f, gloss = 0.95f)
            }
            Hat.CHEF -> {
                ctx.part(Meshes.cylinder, WHITE, 0f, 0.16f, 0f, 0.3f, 0.17f, 0.3f, gloss = 0.2f)
                ctx.part(Meshes.fuzzySoft, WHITE, 0f, 0.42f, 0f, 0.42f, 0.24f, 0.42f, gloss = 0.2f)
                ctx.part(Meshes.sphere, WHITE, 0.2f, 0.5f, 0.05f, 0.2f, gloss = 0.2f)
                ctx.part(Meshes.sphere, WHITE, -0.2f, 0.5f, -0.05f, 0.2f, gloss = 0.2f)
            }
            Hat.TOP_HAT -> {
                ctx.part(Meshes.cylinder, 0x2B2340, 0f, 0.02f, 0f, 0.42f, 0.025f, 0.42f, gloss = 0.5f)
                ctx.part(Meshes.cylinder, 0x2B2340, 0f, 0.28f, 0f, 0.26f, 0.26f, 0.26f, gloss = 0.5f)
                ctx.part(Meshes.cylinder, 0xFFD23F, 0f, 0.1f, 0f, 0.265f, 0.06f, 0.265f, gloss = 0.6f)
            }
            Hat.FLOWER -> {
                ctx.push(); ctx.translate(0.22f, 0.08f, 0.1f); ctx.rotateZ(-20f)
                for (k in 0 until 6) {
                    val a = k / 6f * 6.2832f
                    ctx.part(Meshes.sphere, 0xFF8FC8, cos(a) * 0.15f, 0.04f, sin(a) * 0.15f, 0.11f, 0.05f, 0.11f, gloss = 0.5f)
                }
                ctx.part(Meshes.sphere, 0xFFD23F, 0f, 0.07f, 0f, 0.09f, gloss = 0.5f)
                ctx.pop()
            }
            Hat.BOW -> {
                ctx.push(); ctx.translate(0.18f, 0.02f, 0.15f); ctx.rotateZ(-15f)
                ctx.part(Meshes.cone, 0xFF4D7A, 0.18f, 0.08f, 0f, 0.14f, 0.16f, 0.07f, rz = 90f, gloss = 0.6f)
                ctx.part(Meshes.cone, 0xFF4D7A, -0.18f, 0.08f, 0f, 0.14f, 0.16f, 0.07f, rz = -90f, gloss = 0.6f)
                ctx.part(Meshes.sphere, 0xE0335F, 0f, 0.08f, 0f, 0.08f, gloss = 0.6f)
                ctx.pop()
            }
            Hat.PROPELLER -> {
                ctx.part(Meshes.halfSphere, 0x5B8CFF, 0f, -0.05f, 0f, 0.4f, 0.3f, 0.4f, gloss = 0.5f, pattern = 3, color2 = 0xFFD23F, patternScale = 2f)
                ctx.part(Meshes.cylinder, 0x9AA6BF, 0f, 0.3f, 0f, 0.02f, 0.06f, 0.02f)
                ctx.push(); ctx.translate(0f, 0.37f, 0f); ctx.rotateY(time * 900f)
                ctx.part(Meshes.roundCube, 0xFF5A5A, 0.22f, 0f, 0f, 0.22f, 0.012f, 0.05f, gloss = 0.6f)
                ctx.part(Meshes.roundCube, 0x3FC48A, -0.22f, 0f, 0f, 0.22f, 0.012f, 0.05f, gloss = 0.6f)
                ctx.pop()
            }
            Hat.VIKING -> {
                ctx.part(Meshes.halfSphere, 0x9AA6BF, 0f, -0.05f, 0f, 0.42f, 0.36f, 0.42f, gloss = 0.9f, rim = 0.5f)
                ctx.part(Meshes.torusThin, 0xC9A060, 0f, -0.04f, 0f, 0.42f, 0.42f, 0.42f, gloss = 0.7f)
                ctx.part(Meshes.teardrop, 0xFFF6E5, 0.45f, 0.22f, 0f, 0.09f, 0.2f, 0.09f, rz = -40f, gloss = 0.6f)
                ctx.part(Meshes.teardrop, 0xFFF6E5, -0.45f, 0.22f, 0f, 0.09f, 0.2f, 0.09f, rz = 40f, gloss = 0.6f)
            }
            Hat.SUNHAT -> {
                ctx.part(Meshes.cylinder, 0xF3D58A, 0f, 0.02f, 0f, 0.7f, 0.02f, 0.7f, gloss = 0.2f)
                ctx.part(Meshes.halfSphere, 0xF3D58A, 0f, 0f, 0f, 0.36f, 0.3f, 0.36f, gloss = 0.2f)
                ctx.part(Meshes.cylinder, 0xFF8FC8, 0f, 0.08f, 0f, 0.345f, 0.05f, 0.345f, gloss = 0.4f)
                ctx.part(Meshes.sphere, 0xFFD23F, 0.3f, 0.12f, 0.18f, 0.08f, gloss = 0.4f)
            }
            Hat.BEANIE -> {
                ctx.part(Meshes.halfSphere, 0x3FC48A, 0f, -0.04f, 0f, 0.42f, 0.38f, 0.42f, gloss = 0.15f, pattern = 2, color2 = 0x2AA570, patternScale = 7f)
                ctx.part(Meshes.torus, 0x2AA570, 0f, -0.02f, 0f, 0.38f, 0.25f, 0.38f, gloss = 0.15f)
                ctx.part(Meshes.fuzzy, 0xFFD23F, 0f, 0.4f, 0f, 0.13f, gloss = 0.2f)
            }
            Hat.WIZARD -> {
                ctx.part(Meshes.cylinder, 0x5D3FC0, 0f, 0.02f, 0f, 0.6f, 0.025f, 0.6f, gloss = 0.4f)
                ctx.part(Meshes.cone, 0x5D3FC0, 0.05f, 0.5f, 0f, 0.34f, 0.5f, 0.34f, rz = -10f, gloss = 0.4f, pattern = 5, color2 = 0xFFD23F, patternScale = 3f)
                ctx.part(Meshes.star, 0xFFD23F, 0.2f, 0.98f, 0f, 0.1f, gloss = 0.8f, emissive = 0.4f)
            }
            Hat.HEADPHONES -> {
                ctx.push(); ctx.translate(0f, -0.25f, 0f); ctx.rotateX(90f)
                ctx.part(Meshes.torusThin, 0x2B2340, 0f, 0f, 0f, 0.82f, 0.82f, 1.6f, rx = 0f, gloss = 0.6f)
                ctx.pop()
                ctx.part(Meshes.cylinder, 0xFF5A5A, 0.82f, -0.35f, 0f, 0.2f, 0.08f, 0.2f, rz = 90f, gloss = 0.6f)
                ctx.part(Meshes.cylinder, 0xFF5A5A, -0.82f, -0.35f, 0f, 0.2f, 0.08f, 0.2f, rz = 90f, gloss = 0.6f)
            }
            Hat.CHICK -> {
                val bob = 0.03f * sin(time * 8f)
                ctx.part(Meshes.sphere, 0xFFE14D, 0f, 0.14f + bob, 0f, 0.18f, 0.15f, 0.18f, gloss = 0.3f)
                ctx.part(Meshes.sphere, 0xFFE14D, 0f, 0.33f + bob, 0.05f, 0.12f, gloss = 0.3f)
                ctx.part(Meshes.cone, 0xFF9A3C, 0f, 0.31f + bob, 0.18f, 0.04f, 0.05f, 0.04f, rx = 90f)
                ctx.part(Meshes.sphereLo, 0x1A1428, 0.05f, 0.36f + bob, 0.14f, 0.02f)
                ctx.part(Meshes.sphereLo, 0x1A1428, -0.05f, 0.36f + bob, 0.14f, 0.02f)
                ctx.part(Meshes.teardrop, 0xF2C800, 0.17f, 0.15f + bob, 0f, 0.05f, 0.1f, 0.03f, rz = -60f)
                ctx.part(Meshes.teardrop, 0xF2C800, -0.17f, 0.15f + bob, 0f, 0.05f, 0.1f, 0.03f, rz = 60f)
            }
            Hat.HALO -> {
                ctx.part(Meshes.torusThin, 0xFFE15C, 0f, 0.35f + 0.05f * sin(time * 3f), 0f, 0.38f, 0.38f, 0.38f, gloss = 0.9f, emissive = 0.8f)
            }
        }
    }
}
