package com.feedthewrongthing.game.models

import com.feedthewrongthing.game.engine.Bake
import com.feedthewrongthing.game.engine.DrawCtx
import com.feedthewrongthing.game.engine.Meshes
import com.feedthewrongthing.game.engine.RenderFrame
import com.feedthewrongthing.game.game.Island
import kotlin.math.cos
import kotlin.math.sin

/** Level stages, background decorations and the floating islands used in menus. */
object EnvModels {
    const val MIN_X = -4.8f
    const val MAX_X = 4.8f
    const val MIN_Z = -7.6f
    const val MAX_Z = 2.6f

    private var tree = 0
    private var pine = 0
    private var crystal = 0
    private var mountain = 0
    private var hill = 0
    private var gear = 0
    private var mushroom = 0

    fun build() {
        val m = Meshes
        tree = m.add(Bake()
            .add(m.rawCylinder, 0x8A5233, 0f, 0.6f, 0f, 0.16f, 0.6f, 0.16f)
            .add(m.rawSphere, 0x4FB34A, 0f, 1.6f, 0f, 0.8f)
            .add(m.rawSphere, 0x5FC456, 0.45f, 1.3f, 0.3f, 0.55f)
            .add(m.rawSphere, 0x45A040, -0.45f, 1.35f, -0.2f, 0.55f)
            .build())
        pine = m.add(Bake()
            .add(m.rawCylinder, 0x7A4A2A, 0f, 0.3f, 0f, 0.14f, 0.3f, 0.14f)
            .add(m.rawCone, 0x2E7A4A, 0f, 0.95f, 0f, 0.75f, 0.55f, 0.75f)
            .add(m.rawCone, 0x358A55, 0f, 1.55f, 0f, 0.58f, 0.45f, 0.58f)
            .add(m.rawCone, 0x3E9A5E, 0f, 2.05f, 0f, 0.4f, 0.38f, 0.4f)
            .add(m.rawCone, 0xFFFFFF, 0f, 2.32f, 0f, 0.2f, 0.18f, 0.2f)
            .build())
        crystal = m.add(Bake()
            .add(m.rawCone, 0x7FE0FF, 0f, 0.6f, 0f, 0.25f, 0.6f, 0.25f)
            .add(m.rawCone, 0xC28BFF, 0.3f, 0.35f, 0.1f, 0.16f, 0.35f, 0.16f, rz = -20f)
            .add(m.rawCone, 0xFF8FD8, -0.28f, 0.4f, -0.05f, 0.18f, 0.4f, 0.18f, rz = 22f)
            .build())
        mountain = m.add(Bake()
            .add(m.rawCone, 0x9AB3D6, 0f, 1f, 0f, 1f, 1f, 1f)
            .add(m.rawCone, 0xFFFFFF, 0f, 1.62f, 0f, 0.39f, 0.385f, 0.39f)
            .build())
        hill = m.add(Bake().add(m.rawSphere, 0xFFFFFF, 0f, 0f, 0f, 1f, 0.45f, 1f).build())
        gear = m.add(Bake().also { b ->
            b.add(m.rawCylinder, 0xFFFFFF, 0f, 0f, 0f, 0.8f, 0.15f, 0.8f)
            for (k in 0 until 10) {
                val a = k / 10f * 6.2832f
                b.add(m.rawCube, 0xFFFFFF, cos(a) * 0.9f, 0f, sin(a) * 0.9f, 0.14f, 0.15f, 0.14f, ry = -a * 57.3f)
            }
            b.add(m.rawCylinder, 0x777788, 0f, 0f, 0f, 0.25f, 0.2f, 0.25f)
        }.build())
        mushroom = m.add(Bake()
            .add(m.rawCylinder, 0xFFF1D6, 0f, 0.25f, 0f, 0.12f, 0.25f, 0.12f)
            .add(m.rawHalfSphere, 0xFF5A5A, 0f, 0.45f, 0f, 0.38f, 0.3f, 0.38f)
            .add(m.rawSphereLo, 0xFFFFFF, 0.15f, 0.68f, 0.1f, 0.07f)
            .add(m.rawSphereLo, 0xFFFFFF, -0.18f, 0.62f, 0.12f, 0.06f)
            .build())
    }

    /** Sets sky, light and fog for an island. */
    fun lighting(f: RenderFrame, island: Island) {
        rgb(island.skyTop, f.skyTop)
        rgb(island.skyBottom, f.skyBottom)
        rgb(island.skyBottom, f.fogCol)
        when (island) {
            Island.MOON -> {
                set(f.lightCol, 0.72f, 0.78f, 1.0f); set(f.skyCol, 0.45f, 0.5f, 0.85f); set(f.groundCol, 0.25f, 0.25f, 0.4f)
                set(f.lightDir, -0.3f, 0.9f, 0.4f)
            }
            Island.SOCKS -> {
                set(f.lightCol, 0.95f, 0.88f, 1.0f); set(f.skyCol, 0.7f, 0.6f, 1.0f); set(f.groundCol, 0.4f, 0.3f, 0.55f)
                set(f.lightDir, -0.35f, 0.88f, 0.45f)
            }
            Island.CARNIVAL -> {
                set(f.lightCol, 1.0f, 0.93f, 0.85f); set(f.skyCol, 1.0f, 0.8f, 0.85f); set(f.groundCol, 0.6f, 0.4f, 0.4f)
                set(f.lightDir, -0.45f, 0.85f, 0.45f)
            }
            else -> {
                set(f.lightCol, 1.0f, 0.96f, 0.88f); set(f.skyCol, 0.78f, 0.88f, 1.0f); set(f.groundCol, 0.55f, 0.5f, 0.45f)
                set(f.lightDir, -0.42f, 0.85f, 0.45f)
            }
        }
        f.fog[0] = 26f; f.fog[1] = 90f
    }

    private fun set(a: FloatArray, x: Float, y: Float, z: Float) { a[0] = x; a[1] = y; a[2] = z }

    fun rgb(c: Int, out: FloatArray) {
        out[0] = ((c shr 16) and 255) / 255f
        out[1] = ((c shr 8) and 255) / 255f
        out[2] = (c and 255) / 255f
    }

    /** The playing surface plus decorations for a level. */
    fun drawStage(ctx: DrawCtx, island: Island, t: Float) {
        val cx = (MIN_X + MAX_X) / 2f
        val cz = (MIN_Z + MAX_Z) / 2f
        val hx = (MAX_X - MIN_X) / 2f
        val hz = (MAX_Z - MIN_Z) / 2f
        when (island) {
            Island.PICNIC, Island.FEAST -> {
                ground(ctx, 0x6CC35A, -1.2f)
                ctx.part(Meshes.roundCube, 0x9C6232, cx, -0.32f, cz, hx + 0.15f, 0.3f, hz + 0.15f, gloss = 0.3f)
                ctx.part(Meshes.ground, if (island == Island.FEAST) 0xFFD23F else 0xFF6B6B, cx, 0.004f, cz, hx, 1f, hz,
                    gloss = 0.15f, rim = 0f, pattern = 1, color2 = 0xFFFFFF, patternScale = 4f)
                hills(ctx, 0x7DD14C)
                treeAt(ctx, -9f, -1.2f, -14f, 1.8f); treeAt(ctx, 9.5f, -1.2f, -12f, 2.1f); treeAt(ctx, -12f, -1.2f, -4f, 1.6f)
                treeAt(ctx, 12f, -1.2f, -3f, 1.7f)
                ctx.part(mushroom, 0xFFFFFF, -6.4f, -1.2f, 1f, 1.2f)
                clouds(ctx, t, 0xFFFFFF)
                if (island == Island.FEAST) balloons(ctx, t)
            }
            Island.SOCKS -> {
                ground(ctx, 0x3A2A6E, -1.2f)
                ctx.part(Meshes.roundCube, 0x6B4FA8, cx, -0.4f, cz, hx + 0.2f, 0.4f, hz + 0.2f, gloss = 0.25f)
                ctx.part(Meshes.ground, 0x8E6CF0, cx, 0.004f, cz, hx, 1f, hz, gloss = 0.2f, rim = 0f, pattern = 5, color2 = 0x7A5AD8, patternScale = 3f)
                for ((x, z, s) in listOf(Triple(-8f, -10f, 2.2f), Triple(8.5f, -9f, 2.6f), Triple(-11f, -2f, 1.6f), Triple(11f, 0f, 1.7f))) {
                    ctx.part(crystal, 0xFFFFFF, x, -1.2f, z, s, emissive = 0.35f + 0.1f * sin(t * 2f + x))
                }
                for ((x, z) in listOf(-6f to -16f, 5f to -18f, 0f to -20f)) {
                    ctx.push(); ctx.translate(x, 6.5f, z - 6f); ctx.rotateZ(10f * sin(t + x)); ctx.scale(6f)
                    ItemModels.draw(ctx, com.feedthewrongthing.game.game.ItemType.SOCK)
                    ctx.pop()
                }
                for (k in 0 until 7) {
                    ctx.part(Meshes.cone, 0x4A3A8C, -14f + k * 4.6f, 9f, -18f + (k % 2) * 3f, 0.8f, -1.8f, 0.8f, gloss = 0.2f)
                }
            }
            Island.BATH -> {
                ground(ctx, 0xBFE8FF, -1.4f, pattern = true, patCol = 0xE8F8FF)
                ctx.part(Meshes.roundCube, 0xFFFFFF, cx, -0.6f, cz, hx + 0.9f, 0.7f, hz + 0.9f, gloss = 0.8f, rim = 0.4f)
                ctx.push(); ctx.translate(cx, 0.0f + 0.03f * sin(t * 1.5f), cz); ctx.scale(hx + 0.2f, 1f, hz + 0.2f)
                ctx.draw(Meshes.ground, 0x7FD3FF, alpha = 0.78f, gloss = 0.95f, rim = 0.3f, mode = RenderFrame.GLASS)
                ctx.pop()
                ctx.part(Meshes.ground, 0x4FA9E0, cx, -0.35f, cz, hx, 1f, hz, gloss = 0.3f)
                for ((x, z) in listOf(-3.5f to -6.8f, 3.8f to -6.2f, -4.2f to -1f)) {
                    ctx.push(); ctx.translate(x, 0.15f + 0.05f * sin(t * 2f + x), z); ctx.rotateY(t * 20f + x * 30f); ctx.scale(1.4f)
                    ItemModels.draw(ctx, com.feedthewrongthing.game.game.ItemType.RUBBER_DUCK)
                    ctx.pop()
                }
                for (k in 0 until 10) {
                    val x = -12f + k * 2.7f
                    val y = 1.5f + ((t * 0.3f + k * 0.37f) % 1f) * 8f
                    ctx.part(Meshes.sphere, 0xE8F8FF, x, y, -14f - (k % 3) * 2f, 0.4f + (k % 3) * 0.2f, alpha = 0.45f,
                        gloss = 0.95f, rim = 0.8f, mode = RenderFrame.GLASS)
                }
                ctx.part(Meshes.cylinder, 0xB9C3D6, 7f, 2.5f, -9f, 0.25f, 1.6f, 0.25f, gloss = 0.9f)
                ctx.part(Meshes.cylinder, 0xB9C3D6, 6.2f, 4.0f, -9f, 0.2f, 0.9f, 0.2f, rz = 90f, gloss = 0.9f)
            }
            Island.TOYS -> {
                ground(ctx, 0xE0A866, -0.02f, pattern = true, patCol = 0xC98A4B, pat = 6)
                ctx.part(Meshes.ground, 0x5B8CFF, cx, 0.006f, cz, hx, 1f, hz, gloss = 0.2f, rim = 0f, pattern = 5, color2 = 0x7FA6FF, patternScale = 2f)
                val cols = intArrayOf(0xFF5A5A, 0xFFD23F, 0x3FC48A, 0x8E6CF0, 0x5B8CFF)
                for (k in 0 until 9) {
                    val x = -11f + (k % 3) * 1.3f + (k / 3) * 15f
                    val y = 0.65f + (k / 3 % 2) * 1.3f
                    ctx.part(Meshes.roundCube, cols[k % cols.size], if (k / 3 == 2) 10.5f + (k % 3) * 1.3f else x, y - 0.02f, -9f - (k % 2), 0.65f, gloss = 0.5f)
                }
                ctx.part(Meshes.sphere, 0xFF5A5A, -7f, 1.2f, -12f, 1.2f, gloss = 0.7f, pattern = 2, color2 = 0xFFFFFF, patternScale = 1.5f)
                for (k in 0 until 4) {
                    ctx.part(Meshes.cylinder, cols[k], -2f + k * 1.3f, 0.35f, -14f, 0.3f, 2.5f, 0.3f, rx = 0f, gloss = 0.5f)
                    ctx.part(Meshes.cone, 0xFFE6C2, -2f + k * 1.3f, 3.2f, -14f, 0.3f, 0.35f, 0.3f)
                }
            }
            Island.KITCHEN -> {
                ground(ctx, 0xC98A4B, -1.2f, pattern = true, patCol = 0xB37840, pat = 6)
                ctx.part(Meshes.roundCube, 0xFFF6E5, cx, -0.35f, cz, hx + 0.3f, 0.35f, hz + 0.3f, gloss = 0.5f)
                ctx.part(Meshes.ground, 0xF3E3C8, cx, 0.004f, cz, hx, 1f, hz, gloss = 0.5f, rim = 0f, pattern = 1, color2 = 0xFFFFFF, patternScale = 2.5f)
                ctx.part(Meshes.roundCube, 0xFFFFFF, -9f, 3f, -12f, 2f, 4.2f, 1.6f, gloss = 0.7f)
                ctx.part(Meshes.roundCube, 0xB9C3D6, -7.4f, 3.6f, -10.3f, 0.08f, 1f, 0.12f, gloss = 0.9f)
                ctx.part(Meshes.cylinder, 0xE8453C, 8f, 0.9f, -10f, 1.4f, 1.0f, 1.4f, gloss = 0.7f)
                ctx.part(Meshes.cylinder, 0xC0342A, 8f, 1.95f, -10f, 1.45f, 0.06f, 1.45f, gloss = 0.7f)
                ctx.part(Meshes.cylinder, 0x9AA6BF, 5f, 0.7f, -13f, 1.1f, 0.8f, 1.1f, gloss = 0.9f)
                for (k in 0 until 3) {
                    val y = 1.3f + ((t * 0.5f + k * 0.33f) % 1f) * 3f
                    ctx.part(Meshes.sphereLo, 0xFFFFFF, 8f + sin(t + k) * 0.4f, y + 1f, -10f, 0.5f + k * 0.15f, alpha = 0.35f, gloss = 0f, rim = 0f, mode = RenderFrame.UNLIT)
                }
            }
            Island.SNOW -> {
                ground(ctx, 0xF2F7FF, -1.0f)
                ctx.part(Meshes.roundCube, 0xDDEBFF, cx, -0.4f, cz, hx + 0.2f, 0.4f, hz + 0.2f, gloss = 0.6f)
                ctx.part(Meshes.ground, 0xE6F2FF, cx, 0.004f, cz, hx, 1f, hz, gloss = 0.85f, rim = 0.2f, pattern = 5, color2 = 0xD2E6FF, patternScale = 2f)
                for ((x, z, s) in listOf(Triple(-22f, -48f, 11f), Triple(2f, -56f, 14f), Triple(24f, -46f, 10f))) {
                    ctx.part(mountain, 0xFFFFFF, x, -1f, z, s)
                }
                for ((x, z) in listOf(-8f to -8f, -10f to -3f, 9f to -7f, 11f to -1f, -7.5f to 1f)) ctx.part(pine, 0xFFFFFF, x, -1f, z, 1.3f)
                snowman(ctx, 7f, -1f, 1.5f)
            }
            Island.FACTORY -> {
                ground(ctx, 0x6E7A8E, -1.2f)
                ctx.part(Meshes.roundCube, 0x9AA6BF, cx, -0.35f, cz, hx + 0.3f, 0.35f, hz + 0.3f, gloss = 0.8f)
                ctx.part(Meshes.ground, 0xB9C3D6, cx, 0.004f, cz, hx, 1f, hz, gloss = 0.7f, rim = 0.1f, pattern = 6, color2 = 0x8E9AB0, patternScale = 3f)
                ctx.part(Meshes.ground, 0xFFD23F, cx, 0.006f, MAX_Z - 0.15f, hx, 1f, 0.15f, gloss = 0.3f, pattern = 3, color2 = 0x2B2340, patternScale = 3f)
                for ((x, z) in listOf(-8f to -11f, 8.5f to -12f)) {
                    ctx.part(Meshes.cylinder, 0x5D6A86, x, 3f, z, 0.6f, 4f, 0.6f, gloss = 0.8f)
                    ctx.part(Meshes.torus, 0x3E4A66, x, 5f, z, 0.75f, 0.6f, 0.75f, gloss = 0.8f)
                }
                ctx.part(Meshes.cylinder, 0x5D6A86, 0f, 6.5f, -13f, 0.4f, 9f, 0.4f, rz = 90f, gloss = 0.8f)
                ctx.push(); ctx.translate(-5f, 3.5f, -15f); ctx.rotateX(90f); ctx.rotateY(t * 40f); ctx.scale(2f)
                ctx.draw(gear, 0xFFC21F, gloss = 0.8f); ctx.pop()
                ctx.push(); ctx.translate(-2.2f, 4.6f, -15.2f); ctx.rotateX(90f); ctx.rotateY(-t * 40f + 18f); ctx.scale(1.4f)
                ctx.draw(gear, 0xB9C3D6, gloss = 0.8f); ctx.pop()
                ctx.part(Meshes.bottle, 0xB6F05A, 6f, 2f, -15f, 1.2f, 2f, 1.2f, gloss = 0.9f)
            }
            Island.MOON -> {
                ground(ctx, 0x1F3A3A, -1.2f)
                ctx.part(Meshes.roundCube, 0x3E2E6E, cx, -0.32f, cz, hx + 0.15f, 0.3f, hz + 0.15f, gloss = 0.3f)
                ctx.part(Meshes.ground, 0x5B4FA8, cx, 0.004f, cz, hx, 1f, hz, gloss = 0.2f, rim = 0f, pattern = 5, color2 = 0xFFE68A, patternScale = 2f)
                ctx.part(Meshes.sphere, 0xFFF3B0, 12f, 14f, -40f, 4f, gloss = 0f, rim = 0f, emissive = 1.0f, mode = RenderFrame.UNLIT)
                for (k in 0 until 18) {
                    val x = -30f + (k * 37 % 60)
                    val y = 8f + (k * 13 % 14)
                    val tw = 0.6f + 0.4f * sin(t * 3f + k)
                    ctx.part(Meshes.star, 0xFFF6C2, x, y, -45f, 0.35f * tw, mode = RenderFrame.UNLIT)
                }
                for ((x, z) in listOf(-6.2f to -2f, 6.2f to -4f, -6f to -9f, 6.4f to -10f)) {
                    ctx.part(Meshes.cylinder, 0x5D6A86, x, 0.5f, z, 0.06f, 1.7f, 0.06f)
                    ctx.part(Meshes.sphere, 0xFFD27A, x, 2.3f, z, 0.32f, gloss = 0f, rim = 0f, emissive = 0.9f + 0.1f * sin(t * 4f + x), mode = RenderFrame.UNLIT)
                }
                for ((x, z) in listOf(-9f to -12f, 10f to -13f)) ctx.part(tree, 0x8899CC, x, -1.2f, z, 2f)
            }
            Island.CARNIVAL -> {
                ground(ctx, 0x8E3A55, -1.2f)
                ctx.part(Meshes.roundCube, 0xFFD23F, cx, -0.35f, cz, hx + 0.3f, 0.35f, hz + 0.3f, gloss = 0.5f)
                ctx.part(Meshes.ground, 0xFF6B6B, cx, 0.004f, cz, hx, 1f, hz, gloss = 0.3f, rim = 0f, pattern = 6, color2 = 0xFFFFFF, patternScale = 2f)
                for ((x, z) in listOf(-9f to -12f, 9f to -12f)) {
                    ctx.part(Meshes.cylinder, 0xFFFFFF, x, 1.2f, z, 2.2f, 2.2f, 2.2f, gloss = 0.3f, pattern = 6, color2 = 0xFF4D5E, patternScale = 1.5f)
                    ctx.part(Meshes.cone, 0xFF4D5E, x, 4.4f, z, 2.6f, 1.1f, 2.6f, gloss = 0.4f)
                    ctx.part(Meshes.sphere, 0xFFD23F, x, 5.7f, z, 0.25f, emissive = 0.4f)
                }
                ctx.push(); ctx.translate(0f, 8.5f, -34f); ctx.rotateZ(t * 15f); ctx.scale(6.5f)
                ctx.part(Meshes.torusThin, 0xFFFFFF, 0f, 0f, 0f, 1f, 1f, 1f, rx = 90f, gloss = 0.6f, emissive = 0.2f)
                for (k in 0 until 8) {
                    val a = k / 8f * 6.2832f
                    ctx.part(Meshes.cylinder, 0xFFFFFF, cos(a) * 0.5f, sin(a) * 0.5f, 0f, 0.015f, 0.5f, 0.015f, rz = a * 57.3f - 90f)
                    ctx.part(Meshes.roundCube, intArrayOf(0xFF5A5A, 0xFFD23F, 0x4FD1BA, 0x8E6CF0)[k % 4], cos(a), sin(a), 0f, 0.09f, emissive = 0.2f)
                }
                ctx.pop()
                for (k in 0 until 14) {
                    val x = -12f + k * 1.85f
                    val col = intArrayOf(0xFF5A5A, 0xFFD23F, 0x4FD1BA, 0x8E6CF0, 0x5B8CFF)[k % 5]
                    val on = ((t * 4f).toInt() + k) % 3 != 0
                    ctx.part(Meshes.sphereLo, col, x, 6.2f - 0.6f * sin(k / 13f * 3.14f), -11f, 0.18f, gloss = 0.5f,
                        emissive = if (on) 0.9f else 0.1f, mode = if (on) RenderFrame.UNLIT else RenderFrame.LIT)
                }
            }
        }
    }

    private fun ground(ctx: DrawCtx, color: Int, y: Float, pattern: Boolean = false, patCol: Int = 0, pat: Int = 1) {
        ctx.push()
        ctx.translate(0f, y, -20f)
        ctx.scale(70f, 1f, 50f)
        if (pattern) ctx.draw(Meshes.ground, color, gloss = 0.2f, rim = 0f, pattern = pat, color2 = patCol, patternScale = 30f)
        else ctx.draw(Meshes.ground, color, gloss = 0.1f, rim = 0f)
        ctx.pop()
    }

    private fun hills(ctx: DrawCtx, color: Int) {
        ctx.part(hill, color, -16f, -1.5f, -36f, 16f, 1.6f, 9f, gloss = 0.1f)
        ctx.part(hill, color, 14f, -1.5f, -40f, 18f, 2f, 10f, gloss = 0.1f)
        ctx.part(hill, 0x9BDE6E, 0f, -1.5f, -48f, 22f, 2.2f, 10f, gloss = 0.1f)
    }

    private fun treeAt(ctx: DrawCtx, x: Float, y: Float, z: Float, s: Float) = ctx.part(tree, 0xFFFFFF, x, y, z, s, gloss = 0.25f)

    private fun clouds(ctx: DrawCtx, t: Float, color: Int) {
        for (k in 0 until 4) {
            val x = ((k * 9f + t * 0.6f) % 44f) - 22f
            ctx.part(Meshes.cloud, color, x, 9f + (k % 2) * 2.5f, -30f - k * 3f, 2.2f + (k % 3) * 0.6f, gloss = 0f, rim = 0.2f)
        }
    }

    private fun balloons(ctx: DrawCtx, t: Float) {
        val cols = intArrayOf(0xFF5A6E, 0xFFD23F, 0x4FD1BA, 0x8E6CF0, 0x5B8CFF)
        for (k in 0 until 8) {
            val x = -10f + k * 2.8f
            val y = 3f + ((t * 0.25f + k * 0.29f) % 1f) * 9f
            ctx.part(Meshes.egg, cols[k % 5], x, y, -12f - (k % 3) * 2f, 0.6f, 0.75f, 0.6f, rx = 180f, gloss = 0.9f)
        }
    }

    private fun snowman(ctx: DrawCtx, x: Float, y: Float, z: Float) {
        ctx.part(Meshes.sphere, 0xFFFFFF, x, y + 0.7f, z, 0.8f, gloss = 0.5f)
        ctx.part(Meshes.sphere, 0xFFFFFF, x, y + 1.75f, z, 0.55f, gloss = 0.5f)
        ctx.part(Meshes.cone, 0xFF8A1C, x, y + 1.75f, z + 0.65f, 0.08f, 0.22f, 0.08f, rx = 90f)
        ctx.part(Meshes.sphereLo, 0x1A1428, x + 0.18f, y + 1.9f, z + 0.48f, 0.06f)
        ctx.part(Meshes.sphereLo, 0x1A1428, x - 0.18f, y + 1.9f, z + 0.48f, 0.06f)
        ctx.part(Meshes.torus, 0xFF5A5A, x, y + 1.3f, z, 0.5f, 0.3f, 0.5f)
    }

    /** A floating island used on the home screen, map and results podium. */
    fun drawIsland(ctx: DrawCtx, island: Island, t: Float, locked: Boolean = false) {
        val top = if (locked) 0x8C86A6 else island.ground
        ctx.part(Meshes.cone, 0x8A5233, 0f, -1.35f, 0f, 3.1f, -1.3f, 3.1f, gloss = 0.2f)
        ctx.part(Meshes.cone, 0x6B3A22, 0.4f, -2.9f, 0.3f, 1.2f, -0.6f, 1.2f, gloss = 0.2f)
        ctx.part(Meshes.cylinder, top, 0f, -0.15f, 0f, 3.2f, 0.18f, 3.2f, gloss = 0.25f, rim = 0.3f)
        ctx.part(Meshes.cylinder, 0xFFFFFF, 0f, -0.05f, 0f, 3.0f, 0.1f, 3.0f, alpha = 0.18f, gloss = 0f, rim = 0f, mode = RenderFrame.UNLIT)
        if (locked) return
        when (island) {
            Island.PICNIC, Island.FEAST -> {
                treeAt(ctx, -2f, 0f, -1.2f, 0.9f)
                ctx.part(Meshes.ground, 0xFF6B6B, 1.2f, 0.02f, 0.9f, 0.8f, 1f, 0.6f, ry = 15f, pattern = 1, color2 = 0xFFFFFF, patternScale = 3f, gloss = 0.1f)
                ctx.part(mushroom, 0xFFFFFF, 2.2f, 0f, -1.3f, 0.7f)
            }
            Island.SOCKS -> {
                ctx.part(crystal, 0xFFFFFF, -2f, 0f, -1f, 1.2f, emissive = 0.4f)
                ctx.push(); ctx.translate(2f, 0.6f, -1f); ctx.rotateZ(-15f); ctx.scale(2.2f)
                ItemModels.draw(ctx, com.feedthewrongthing.game.game.ItemType.SOCK); ctx.pop()
            }
            Island.BATH -> {
                ctx.part(Meshes.cylinder, 0xFFFFFF, -1.9f, 0.35f, -0.9f, 1.0f, 0.35f, 0.7f, gloss = 0.8f)
                ctx.part(Meshes.cylinder, 0x7FD3FF, -1.9f, 0.62f, -0.9f, 0.88f, 0.08f, 0.6f, gloss = 0.9f)
                for (k in 0 until 3) ctx.part(Meshes.sphere, 0xE8F8FF, -1.6f + k * 0.4f, 1f + k * 0.25f, -0.9f, 0.18f + k * 0.05f, alpha = 0.5f, mode = RenderFrame.GLASS)
            }
            Island.TOYS -> {
                ctx.part(Meshes.roundCube, 0xFF5A5A, -2f, 0.4f, -1f, 0.4f)
                ctx.part(Meshes.roundCube, 0xFFD23F, -1.2f, 0.4f, -1.2f, 0.4f)
                ctx.part(Meshes.roundCube, 0x5B8CFF, -1.6f, 1.2f, -1.1f, 0.4f)
            }
            Island.KITCHEN -> {
                ctx.part(Meshes.cylinder, 0xE8453C, -1.9f, 0.45f, -1f, 0.7f, 0.45f, 0.7f, gloss = 0.7f)
                ctx.part(Meshes.cylinder, 0xC0342A, -1.9f, 0.92f, -1f, 0.73f, 0.04f, 0.73f, gloss = 0.7f)
            }
            Island.SNOW -> {
                ctx.part(pine, 0xFFFFFF, -2f, 0f, -1f, 0.7f)
                snowman(ctx, 2.1f, 0f, -0.8f)
            }
            Island.FACTORY -> {
                ctx.part(Meshes.cylinder, 0x5D6A86, -2f, 1f, -1f, 0.3f, 1f, 0.3f, gloss = 0.8f)
                ctx.push(); ctx.translate(2f, 1f, -1f); ctx.rotateX(90f); ctx.rotateY(t * 60f); ctx.scale(0.8f)
                ctx.draw(gear, 0xFFC21F, gloss = 0.8f); ctx.pop()
            }
            Island.MOON -> {
                ctx.part(Meshes.cylinder, 0x5D6A86, -2f, 0.8f, -1f, 0.04f, 0.8f, 0.04f)
                ctx.part(Meshes.sphere, 0xFFD27A, -2f, 1.7f, -1f, 0.22f, emissive = 0.9f, mode = RenderFrame.UNLIT)
                ctx.part(Meshes.softCube, 0xDDE6FF, 1.8f, 0.2f, -0.6f, 0.7f, 0.2f, 0.5f, gloss = 0.1f)
            }
            Island.CARNIVAL -> {
                ctx.part(Meshes.cylinder, 0xFFFFFF, -1.9f, 0.6f, -1f, 0.8f, 0.6f, 0.8f, pattern = 6, color2 = 0xFF4D5E, patternScale = 3f)
                ctx.part(Meshes.cone, 0xFF4D5E, -1.9f, 1.6f, -1f, 0.95f, 0.45f, 0.95f)
            }
        }
    }

    /** Small sparkling platform used under creatures on the results / collection screens. */
    fun podium(ctx: DrawCtx, color: Int) {
        ctx.part(Meshes.cylinder, color, 0f, -0.25f, 0f, 1.6f, 0.25f, 1.6f, gloss = 0.7f, rim = 0.5f)
        ctx.part(Meshes.torusThin, 0xFFFFFF, 0f, 0.0f, 0f, 1.45f, 1f, 1.45f, gloss = 0.9f, emissive = 0.5f)
    }
}
