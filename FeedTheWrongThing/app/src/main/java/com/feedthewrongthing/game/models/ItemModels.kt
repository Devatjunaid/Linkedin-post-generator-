package com.feedthewrongthing.game.models

import com.feedthewrongthing.game.engine.Bake
import com.feedthewrongthing.game.engine.DrawCtx
import com.feedthewrongthing.game.engine.MeshData
import com.feedthewrongthing.game.engine.Meshes
import com.feedthewrongthing.game.engine.RenderFrame
import com.feedthewrongthing.game.game.ItemType
import kotlin.math.cos
import kotlin.math.sin

/** Builds one baked mesh per item (plus an optional see-through part) and draws them. */
object ItemModels {
    private val opaque = IntArray(ItemType.entries.size) { -1 }
    private val glass = IntArray(ItemType.entries.size) { -1 }
    private val glassColor = IntArray(ItemType.entries.size)
    private val gloss = FloatArray(ItemType.entries.size) { 0.5f }

    private const val WHITE = 0xFFFFFF

    fun build() {
        val m = Meshes
        for (t in ItemType.entries) {
            val b = Bake()
            when (t) {
                ItemType.PIZZA -> {
                    b.add(m.rawWedge, 0xFFC94D, 0f, 0f, 0.04f, 0.44f, 0.55f, 0.44f)
                    b.add(m.rawCapsule, 0xE59A3C, 0f, 0.02f, -0.24f, 0.08f, 0.17f, 0.08f, rz = 90f)
                    b.add(m.rawCylinder, 0xE0412F, -0.09f, 0.075f, 0.02f, 0.075f, 0.014f, 0.075f)
                    b.add(m.rawCylinder, 0xE0412F, 0.1f, 0.075f, -0.07f, 0.07f, 0.014f, 0.07f)
                    b.add(m.rawCylinder, 0xE0412F, 0.01f, 0.075f, 0.2f, 0.06f, 0.014f, 0.06f)
                    b.add(m.rawSphereLo, 0x6CC35A, 0.08f, 0.075f, 0.1f, 0.03f, 0.012f, 0.03f)
                    gloss[t.ordinal] = 0.55f
                }
                ItemType.SOCK -> {
                    b.add(m.rawCapsule, WHITE, 0f, 0.08f, 0f, 0.13f, 0.1f, 0.13f)
                    b.add(m.rawCapsule, WHITE, 0f, -0.13f, 0.11f, 0.13f, 0.09f, 0.13f, rx = 90f)
                    b.add(m.rawCylinder, 0xFF5A5A, 0f, 0.27f, 0f, 0.142f, 0.045f, 0.142f)
                    b.add(m.rawCylinder, 0x5B8CFF, 0f, 0.14f, 0f, 0.136f, 0.028f, 0.136f)
                    b.add(m.rawCylinder, 0x5B8CFF, 0f, 0.03f, 0f, 0.136f, 0.028f, 0.136f)
                    b.add(m.rawSphereLo, 0xFF5A5A, 0f, -0.13f, 0.26f, 0.12f, 0.1f, 0.07f)
                    b.add(m.rawSphereLo, 0xFF5A5A, 0f, -0.14f, -0.05f, 0.12f, 0.1f, 0.07f)
                    gloss[t.ordinal] = 0.12f
                }
                ItemType.RUBBER_DUCK -> {
                    b.add(m.rawSphere, 0xFFD23F, 0f, -0.07f, -0.02f, 0.27f, 0.21f, 0.33f)
                    b.add(m.rawTeardrop, 0xFFD23F, 0f, 0.0f, -0.3f, 0.1f, 0.12f, 0.08f, rx = -55f)
                    b.add(m.rawSphere, 0xFFD23F, 0f, 0.19f, 0.14f, 0.17f)
                    b.add(m.rawSphere, 0xFF9A3C, 0f, 0.15f, 0.31f, 0.1f, 0.045f, 0.09f)
                    b.add(m.rawSphereLo, 0x1A1428, 0.085f, 0.25f, 0.27f, 0.032f)
                    b.add(m.rawSphereLo, 0x1A1428, -0.085f, 0.25f, 0.27f, 0.032f)
                    b.add(m.rawSphereLo, WHITE, 0.09f, 0.262f, 0.295f, 0.011f)
                    b.add(m.rawSphereLo, WHITE, -0.08f, 0.262f, 0.295f, 0.011f)
                    b.add(m.rawSphere, 0xF2B800, 0.25f, -0.04f, -0.03f, 0.05f, 0.11f, 0.17f)
                    b.add(m.rawSphere, 0xF2B800, -0.25f, -0.04f, -0.03f, 0.05f, 0.11f, 0.17f)
                    gloss[t.ordinal] = 0.85f
                }
                ItemType.FISH -> {
                    b.add(m.rawSphere, 0x5B8CFF, 0f, 0f, 0f, 0.36f, 0.2f, 0.13f)
                    b.add(m.rawSphere, 0xBFD6FF, 0.02f, -0.07f, 0f, 0.3f, 0.12f, 0.11f)
                    b.add(m.rawCone, 0x3E6BE0, -0.42f, 0f, 0f, 0.16f, 0.12f, 0.04f, rz = -90f)
                    b.add(m.rawTeardrop, 0x3E6BE0, -0.02f, 0.2f, 0f, 0.12f, 0.08f, 0.03f, rz = 20f)
                    b.add(m.rawSphereLo, WHITE, 0.22f, 0.05f, 0.09f, 0.055f)
                    b.add(m.rawSphereLo, WHITE, 0.22f, 0.05f, -0.09f, 0.055f)
                    b.add(m.rawSphereLo, 0x1A1428, 0.24f, 0.05f, 0.125f, 0.026f)
                    b.add(m.rawSphereLo, 0x1A1428, 0.24f, 0.05f, -0.125f, 0.026f)
                    b.add(m.rawSphereLo, 0xFF7A9A, 0.34f, -0.03f, 0f, 0.03f, 0.02f, 0.05f)
                    gloss[t.ordinal] = 0.8f
                }
                ItemType.CHILI -> {
                    b.add(m.rawCurveUp, 0xE8453C, 0f, -0.04f, 0f, 0.34f, 0.32f, 0.34f, rz = 35f)
                    b.add(m.rawCylinder, 0x2E9A3A, -0.27f, 0.27f, 0f, 0.035f, 0.07f, 0.035f, rz = 40f)
                    b.add(m.rawSphere, 0x3FB548, -0.23f, 0.2f, 0f, 0.09f, 0.045f, 0.09f, rz = 35f)
                    gloss[t.ordinal] = 0.8f
                }
                ItemType.TOOTHBRUSH -> {
                    b.add(m.rawRoundCube, 0x8E6CF0, -0.06f, 0f, 0f, 0.38f, 0.045f, 0.05f)
                    b.add(m.rawRoundCube, WHITE, 0.33f, 0.005f, 0f, 0.09f, 0.04f, 0.055f)
                    b.add(m.rawRoundCube, 0x9EE0FF, 0.34f, 0.075f, 0f, 0.075f, 0.04f, 0.05f)
                    b.add(m.rawCapsule, 0x3FC48A, 0.34f, 0.14f, 0f, 0.035f, 0.02f, 0.035f, rz = 90f)
                    b.add(m.rawCapsule, WHITE, 0.34f, 0.15f, 0.015f, 0.022f, 0.018f, 0.02f, rz = 90f)
                    gloss[t.ordinal] = 0.7f
                }
                ItemType.BANANA -> {
                    b.add(m.rawCurveUp, 0xFFE14D, 0f, -0.05f, 0f, 0.38f, 0.38f, 0.36f)
                    b.add(m.rawSphereLo, 0x7A5A2A, 0.35f, 0.07f, 0f, 0.035f)
                    b.add(m.rawCylinder, 0x8A6A3C, -0.34f, 0.1f, 0f, 0.03f, 0.05f, 0.03f, rz = 30f)
                    gloss[t.ordinal] = 0.5f
                }
                ItemType.CUPCAKE -> {
                    b.add(m.rawWrapper, 0x5B8CFF, 0f, -0.14f, 0f, 0.28f, 0.28f, 0.28f)
                    b.add(m.rawSphere, 0xFF8FC8, 0f, 0.08f, 0f, 0.33f, 0.18f, 0.33f)
                    b.add(m.rawSphere, 0xFF8FC8, 0f, 0.22f, 0f, 0.22f, 0.12f, 0.22f)
                    b.add(m.rawSphere, 0xFFB3DA, 0f, 0.32f, 0f, 0.12f, 0.08f, 0.12f)
                    b.add(m.rawSphere, 0xE0233A, 0f, 0.42f, 0f, 0.075f)
                    b.add(m.rawCylinder, 0x2E9A3A, 0.03f, 0.5f, 0f, 0.01f, 0.05f, 0.01f, rz = -25f)
                    for (k in 0 until 7) {
                        val a = k * 0.9f
                        val col = intArrayOf(0xFFD23F, 0x4FD1BA, 0xFFFFFF, 0x8E6CF0)[k % 4]
                        b.add(m.rawCapsule, col, cos(a) * 0.2f, 0.19f + (k % 2) * 0.05f, sin(a) * 0.2f, 0.014f, 0.012f, 0.014f, rz = 90f, ry = a * 57f)
                    }
                    gloss[t.ordinal] = 0.6f
                }
                ItemType.BROCCOLI -> {
                    b.add(m.rawCylinder, 0x9AD86E, 0f, -0.16f, 0f, 0.085f, 0.15f, 0.085f)
                    b.add(m.rawFuzzy, 0x3FA53A, 0f, 0.12f, 0f, 0.2f)
                    b.add(m.rawFuzzy, 0x37963A, -0.16f, 0.05f, 0.04f, 0.14f)
                    b.add(m.rawFuzzy, 0x37963A, 0.16f, 0.05f, 0.03f, 0.14f)
                    b.add(m.rawFuzzy, 0x45B042, 0f, 0.06f, 0.15f, 0.13f)
                    b.add(m.rawFuzzy, 0x45B042, 0f, 0.06f, -0.14f, 0.13f)
                    gloss[t.ordinal] = 0.25f
                }
                ItemType.CHEESE -> {
                    b.add(m.rawWedge, 0xFFD23F, 0f, 0f, 0.03f, 0.42f, 1.4f, 0.42f)
                    for ((x, z, r) in listOf(Triple(0.05f, 0.05f, 0.05f), Triple(-0.12f, -0.12f, 0.04f), Triple(0.13f, -0.15f, 0.035f), Triple(0f, 0.25f, 0.03f))) {
                        b.add(m.rawCylinder, 0xE0A800, x, 0.17f, z, r, 0.005f, r)
                    }
                    gloss[t.ordinal] = 0.35f
                }
                ItemType.ICE_CUBE -> {
                    b.add(m.rawSphereLo, WHITE, 0.08f, 0.1f, 0.12f, 0.04f)
                    glass[t.ordinal] = m.add(Bake().add(m.rawRoundCube, 0xCFEFFF, 0f, 0f, 0f, 0.27f, 0.27f, 0.27f, ry = 20f, rx = 10f).build())
                    glassColor[t.ordinal] = 0xBFE8FF
                    gloss[t.ordinal] = 0.9f
                }
                ItemType.LOLLIPOP -> {
                    b.add(m.rawCylinder, WHITE, 0f, -0.2f, 0f, 0.025f, 0.2f, 0.025f)
                    b.add(m.rawSphere, 0xFF6BB5, 0f, 0.14f, 0f, 0.26f, 0.26f, 0.09f)
                    b.add(m.rawTorusThin, WHITE, 0f, 0.14f, 0.07f, 0.17f, 0.17f, 0.17f, rx = 90f)
                    b.add(m.rawTorusThin, 0xFFD23F, 0f, 0.14f, 0.078f, 0.1f, 0.1f, 0.1f, rx = 90f)
                    b.add(m.rawSphereLo, WHITE, 0f, 0.14f, 0.085f, 0.03f, 0.03f, 0.01f)
                    gloss[t.ordinal] = 0.9f
                }
                ItemType.SNEAKER -> {
                    b.add(m.rawRoundCube, 0xFF5A5A, 0f, 0.02f, 0.02f, 0.16f, 0.1f, 0.3f)
                    b.add(m.rawRoundCube, 0xFF5A5A, 0f, 0.15f, -0.13f, 0.145f, 0.11f, 0.14f)
                    b.add(m.rawRoundCube, WHITE, 0f, -0.1f, 0.01f, 0.175f, 0.045f, 0.34f)
                    b.add(m.rawSphere, WHITE, 0f, -0.02f, 0.25f, 0.155f, 0.09f, 0.1f)
                    for (k in 0 until 3) b.add(m.rawRoundCube, WHITE, 0f, 0.125f, 0.12f - k * 0.08f, 0.1f, 0.015f, 0.018f)
                    b.add(m.rawSphereLo, 0xFFD23F, 0.16f, 0.03f, -0.05f, 0.015f, 0.05f, 0.08f)
                    gloss[t.ordinal] = 0.4f
                }
                ItemType.TOY_BLOCK -> {
                    b.add(m.rawRoundCube, 0xFF5A5A, 0f, 0f, 0f, 0.25f)
                    b.add(m.rawRoundCube, 0xFFD23F, 0f, 0f, 0.245f, 0.16f, 0.16f, 0.02f)
                    b.add(m.rawRoundCube, 0x5B8CFF, 0f, 0.245f, 0f, 0.16f, 0.02f, 0.16f)
                    b.add(m.rawRoundCube, 0x3FC48A, 0.245f, 0f, 0f, 0.02f, 0.16f, 0.16f)
                    b.add(m.rawRoundCube, 0x8E6CF0, -0.245f, 0f, 0f, 0.02f, 0.16f, 0.16f)
                    gloss[t.ordinal] = 0.5f
                }
                ItemType.ALARM_CLOCK -> {
                    b.add(m.rawCylinder, 0x5B8CFF, 0f, 0f, 0f, 0.26f, 0.09f, 0.26f, rx = 90f)
                    b.add(m.rawCylinder, WHITE, 0f, 0f, 0.085f, 0.21f, 0.012f, 0.21f, rx = 90f)
                    b.add(m.rawCube, 0x1A1428, 0f, 0.05f, 0.1f, 0.012f, 0.065f, 0.006f)
                    b.add(m.rawCube, 0x1A1428, 0.045f, 0f, 0.1f, 0.055f, 0.012f, 0.006f)
                    b.add(m.rawHalfSphere, 0xFFD23F, 0.15f, 0.23f, 0f, 0.1f, rz = -30f)
                    b.add(m.rawHalfSphere, 0xFFD23F, -0.15f, 0.23f, 0f, 0.1f, rz = 30f)
                    b.add(m.rawSphereLo, 0x2346A8, 0.15f, -0.26f, 0f, 0.045f)
                    b.add(m.rawSphereLo, 0x2346A8, -0.15f, -0.26f, 0f, 0.045f)
                    gloss[t.ordinal] = 0.75f
                }
                ItemType.BALLOON -> {
                    b.add(m.rawEgg, 0xFF5A6E, 0f, 0.08f, 0f, 0.29f, 0.35f, 0.29f, rx = 180f)
                    b.add(m.rawCone, 0xE0405A, 0f, -0.29f, 0f, 0.05f, 0.04f, 0.05f)
                    b.add(m.rawCylinder, WHITE, 0f, -0.42f, 0f, 0.008f, 0.12f, 0.008f)
                    b.add(m.rawSphere, 0xFFB3BF, -0.1f, 0.22f, 0.2f, 0.06f, 0.09f, 0.04f)
                    gloss[t.ordinal] = 0.95f
                }
                ItemType.TRUMPET -> {
                    b.add(m.rawBell, 0xFFC94D, 0.2f, 0f, 0f, 0.15f, 0.12f, 0.15f, rz = -90f)
                    b.add(m.rawCylinder, 0xFFC94D, -0.1f, 0f, 0f, 0.035f, 0.2f, 0.035f, rz = 90f)
                    b.add(m.rawCylinder, 0xE0A020, -0.1f, -0.08f, 0f, 0.03f, 0.13f, 0.03f, rz = 90f)
                    for (k in 0 until 3) b.add(m.rawCylinder, 0xE0A020, -0.17f + k * 0.07f, 0.06f, 0f, 0.022f, 0.05f, 0.022f)
                    b.add(m.rawSphereLo, 0xE0A020, -0.32f, 0f, 0f, 0.045f)
                    gloss[t.ordinal] = 0.95f
                }
                ItemType.FEATHER -> {
                    b.add(m.rawTeardrop, 0x8E6CF0, 0f, 0.04f, 0f, 0.13f, 0.3f, 0.025f, rz = 25f)
                    b.add(m.rawTeardrop, 0xC2B0FF, 0.01f, 0.06f, 0.01f, 0.07f, 0.22f, 0.025f, rz = 25f)
                    b.add(m.rawCylinder, WHITE, -0.08f, -0.24f, 0f, 0.012f, 0.13f, 0.012f, rz = 25f)
                    gloss[t.ordinal] = 0.3f
                }
                ItemType.DONUT -> {
                    b.add(m.rawTorus, 0xE0A866, 0f, -0.02f, 0f, 0.24f, 0.26f, 0.24f)
                    b.add(m.rawTorus, 0xFF8FC8, 0f, 0.025f, 0f, 0.245f, 0.18f, 0.245f)
                    for (k in 0 until 9) {
                        val a = k * 0.7f
                        val r = if (k % 2 == 0) 0.2f else 0.29f
                        val col = intArrayOf(0xFFD23F, 0x4FD1BA, 0xFFFFFF, 0x8E6CF0, 0xFF5A5A)[k % 5]
                        b.add(m.rawCapsule, col, cos(a) * r, 0.085f, sin(a) * r, 0.014f, 0.012f, 0.014f, rz = 90f, ry = a * 80f)
                    }
                    gloss[t.ordinal] = 0.6f
                }
                ItemType.CARROT -> {
                    b.add(m.rawCone, 0xFF8A1C, 0f, -0.04f, 0f, 0.13f, 0.28f, 0.13f, rx = 180f)
                    for (k in 0 until 3) b.add(m.rawCylinder, 0xE07010, 0f, 0.1f - k * 0.12f, 0f, 0.115f - k * 0.035f, 0.006f, 0.115f - k * 0.035f)
                    b.add(m.rawTeardrop, 0x3FB548, -0.05f, 0.33f, 0f, 0.045f, 0.11f, 0.03f, rz = 20f)
                    b.add(m.rawTeardrop, 0x3FB548, 0f, 0.35f, 0f, 0.045f, 0.12f, 0.03f)
                    b.add(m.rawTeardrop, 0x3FB548, 0.05f, 0.33f, 0f, 0.045f, 0.11f, 0.03f, rz = -20f)
                    gloss[t.ordinal] = 0.45f
                }
                ItemType.SODA -> {
                    b.add(m.rawCylinder, 0xE8453C, 0f, 0f, 0f, 0.16f, 0.22f, 0.16f)
                    b.add(m.rawCylinder, WHITE, 0f, 0f, 0f, 0.163f, 0.06f, 0.163f)
                    b.add(m.rawCylinder, 0xC9D2E0, 0f, 0.225f, 0f, 0.145f, 0.012f, 0.145f)
                    b.add(m.rawRoundCube, 0x9AA6BF, 0.04f, 0.24f, 0f, 0.05f, 0.008f, 0.025f)
                    b.add(m.rawSphereLo, 0xFFD23F, 0f, 0f, 0.165f, 0.05f, 0.05f, 0.01f)
                    gloss[t.ordinal] = 0.9f
                }
                ItemType.PILLOW -> {
                    b.add(m.rawRoundCube, 0xDDE6FF, 0f, 0f, 0f, 0.34f, 0.12f, 0.26f)
                    b.add(m.rawSphereLo, 0xFF8FC8, 0f, 0.115f, 0f, 0.045f, 0.02f, 0.045f)
                    gloss[t.ordinal] = 0.15f
                }
                ItemType.JELLY -> {
                    b.add(m.rawSphere, WHITE, 0f, 0.16f, 0f, 0.15f, 0.06f, 0.15f)
                    b.add(m.rawSphere, 0xE0233A, 0f, 0.23f, 0f, 0.055f)
                    glass[t.ordinal] = m.add(Bake().add(m.rawWrapper, 0x5BD86E, 0f, -0.04f, 0f, 0.26f, 0.2f, 0.26f, rx = 180f).build())
                    glassColor[t.ordinal] = 0x5BD86E
                    gloss[t.ordinal] = 0.9f
                }
                ItemType.WATERMELON -> {
                    b.add(m.rawWedge, 0xFF4D6A, 0f, 0f, 0.04f, 0.38f, 0.8f, 0.36f)
                    b.add(m.rawCapsule, 0xD8F5C8, 0f, 0f, -0.18f, 0.06f, 0.14f, 0.06f, rz = 90f)
                    b.add(m.rawCapsule, 0x3FA53A, 0f, 0f, -0.215f, 0.065f, 0.15f, 0.065f, rz = 90f)
                    for ((x, z) in listOf(0f to 0.1f, -0.1f to -0.05f, 0.1f to -0.06f, 0f to 0.24f, -0.05f to 0.05f, 0.06f to 0.02f)) {
                        b.add(m.rawEgg, 0x1A1428, x, 0.1f, z, 0.022f, 0.012f, 0.032f)
                    }
                    gloss[t.ordinal] = 0.6f
                }
                ItemType.PEPPER -> {
                    b.add(m.rawCylinder, 0xF2F2F2, 0f, -0.06f, 0f, 0.14f, 0.2f, 0.14f)
                    b.add(m.rawCylinder, 0x333344, 0f, -0.04f, 0f, 0.143f, 0.05f, 0.143f)
                    b.add(m.rawHalfSphere, 0xB9C3D6, 0f, 0.14f, 0f, 0.145f, 0.13f, 0.145f)
                    for (k in 0 until 5) {
                        val a = k * 1.2566f
                        b.add(m.rawSphereLo, 0x333344, cos(a) * 0.05f, 0.265f, sin(a) * 0.05f, 0.015f)
                    }
                    b.add(m.rawSphereLo, 0x333344, 0f, 0.27f, 0f, 0.015f)
                    gloss[t.ordinal] = 0.85f
                }
            }
            opaque[t.ordinal] = m.add(b.build())
        }
    }

    /** Draws an item centred at the current matrix origin. */
    fun draw(ctx: DrawCtx, t: ItemType, alpha: Float = 1f) {
        val i = t.ordinal
        ctx.draw(opaque[i], WHITE, alpha = alpha, gloss = gloss[i], rim = 0.3f)
        if (glass[i] >= 0) {
            ctx.draw(glass[i], glassColor[i], alpha = 0.55f * alpha, gloss = 0.95f, rim = 0.6f, mode = RenderFrame.GLASS)
        }
    }

    @Suppress("unused")
    fun mesh(t: ItemType): MeshData = Meshes.all[opaque[t.ordinal]]
}
