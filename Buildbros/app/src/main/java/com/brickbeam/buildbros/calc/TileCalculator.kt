package com.brickbeam.buildbros.calc

import kotlin.math.max
import kotlin.math.min

enum class TilePattern(val label: String, val cutWastePct: Double) {
    STRAIGHT("Straight", 7.0),
    OFFSET("Brick offset", 10.0),
    DIAGONAL("Diagonal", 15.0),
    HERRINGBONE("Herringbone", 17.0),
}

/** Adhesive consumption for a notched trowel, kg/m² (typical cementitious C1/C2 adhesives). */
enum class TrowelNotch(val label: String, val notchMm: Double, val kgPerM2: Double) {
    N6("6 mm (¼\")", 6.0, 2.5),
    N8("8 mm (5/16\")", 8.0, 3.2),
    N10("10 mm (3/8\")", 10.0, 4.0),
    N12("12 mm (½\")", 12.0, 5.0),
}

data class TileInput(
    val areaLengthM: Double,
    val areaWidthM: Double,
    val deductM2: Double,
    val tileLengthMm: Double,
    val tileWidthMm: Double,
    val tileThicknessMm: Double,
    val jointMm: Double,
    val pattern: TilePattern,
    val sparePct: Double,
    val tilesPerBox: Int,
    val notch: TrowelNotch,
    val adhesiveBagKg: Double,
    val groutBagKg: Double,
)

data class TileResult(
    val grossAreaM2: Double,
    val netAreaM2: Double,
    val tilesForLayout: Int,
    val tilesToBuy: Int,
    val boxes: Int,
    val boughtAreaM2: Double,
    val method: String,
    val adhesiveKg: Double,
    val adhesiveBags: Int,
    val groutKg: Double,
    val groutBags: Int,
    val backButter: Boolean,
)

object TileCalculator {
    const val GROUT_DENSITY = 1.6        // g/cm³ (kg/dm³), cementitious grout
    const val CONSUMABLE_WASTE = 0.10    // adhesive & grout overage

    /** n tiles with (n-1) joints must reach [spanMm]: n = ceil((span + j) / (tile + j)). */
    fun tilesAcross(spanMm: Double, tileMm: Double, jointMm: Double): Int =
        if (spanMm <= 0 || tileMm <= 0) 0 else ceilSafe((spanMm + jointMm) / (tileMm + jointMm))

    fun gridCount(lMm: Double, wMm: Double, tLmm: Double, tWmm: Double, jMm: Double): Int {
        val a = tilesAcross(lMm, tLmm, jMm) * tilesAcross(wMm, tWmm, jMm)
        val b = tilesAcross(lMm, tWmm, jMm) * tilesAcross(wMm, tLmm, jMm)
        return min(a, b)
    }

    /** kg/m² = (A + B) / (A × B) × joint width × joint depth × density (all mm, density g/cm³). */
    fun groutKgPerM2(tLmm: Double, tWmm: Double, jointMm: Double, depthMm: Double): Double =
        if (tLmm <= 0 || tWmm <= 0) 0.0 else (tLmm + tWmm) / (tLmm * tWmm) * jointMm * depthMm * GROUT_DENSITY

    fun calculate(input: TileInput): TileResult {
        val gross = max(0.0, input.areaLengthM) * max(0.0, input.areaWidthM)
        val net = max(0.0, gross - input.deductM2)
        val effTileM2 = (input.tileLengthMm + input.jointMm) * (input.tileWidthMm + input.jointMm) / 1e6

        val layout: Int
        val method: String
        if (input.pattern == TilePattern.STRAIGHT && input.deductM2 <= 0.0) {
            layout = gridCount(
                input.areaLengthM * 1000, input.areaWidthM * 1000,
                input.tileLengthMm, input.tileWidthMm, input.jointMm,
            )
            method = "Row × column layout (every cut tile counted whole)"
        } else {
            layout = if (effTileM2 <= 0) 0 else ceilSafe(net / effTileM2 * (1 + input.pattern.cutWastePct / 100.0))
            method = "Area ÷ tile + ${input.pattern.cutWastePct.toInt()}% cuts for ${input.pattern.label.lowercase()}"
        }
        val toBuy = ceilSafe(layout * (1 + input.sparePct / 100.0))
        val perBox = max(1, input.tilesPerBox)
        val boxes = ceilSafe(toBuy.toDouble() / perBox)
        val tileM2 = input.tileLengthMm * input.tileWidthMm / 1e6

        val backButter = max(input.tileLengthMm, input.tileWidthMm) >= 600.0
        val adhesiveKgM2 = input.notch.kgPerM2 + if (backButter) 1.0 else 0.0
        val adhesive = net * adhesiveKgM2 * (1 + CONSUMABLE_WASTE)
        val grout = net * groutKgPerM2(input.tileLengthMm, input.tileWidthMm, input.jointMm, input.tileThicknessMm) *
            (1 + CONSUMABLE_WASTE)

        return TileResult(
            grossAreaM2 = gross,
            netAreaM2 = net,
            tilesForLayout = layout,
            tilesToBuy = toBuy,
            boxes = boxes,
            boughtAreaM2 = boxes * perBox * tileM2,
            method = method,
            adhesiveKg = adhesive,
            adhesiveBags = ceilSafe(adhesive / input.adhesiveBagKg),
            groutKg = grout,
            groutBags = ceilSafe(grout / input.groutBagKg),
            backButter = backButter,
        )
    }

    /** Typical box ≈ 1.44 m² (15.5 ft²); used only to pre-fill "tiles per box". */
    fun suggestedTilesPerBox(tLmm: Double, tWmm: Double): Int {
        val a = tLmm * tWmm / 1e6
        return if (a <= 0) 1 else max(1, Math.round(1.44 / a).toInt())
    }
}
