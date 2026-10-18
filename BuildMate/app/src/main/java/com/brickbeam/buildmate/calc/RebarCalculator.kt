package com.brickbeam.buildmate.calc

import kotlin.math.floor
import kotlin.math.max

data class RebarInput(
    val lengthM: Double,
    val widthM: Double,
    val coverM: Double,
    val spacingLongM: Double,  // spacing of bars running along the length
    val spacingShortM: Double, // spacing of bars running along the width
    val bar: RebarSize,
    val stockLengthM: Double,
    /** Lap splice length as a multiple of bar diameter (40d is a common default). */
    val lapDiameters: Double,
    /** 1 = single mat, 2 = top and bottom mats. */
    val layers: Int,
    val chairSpacingM: Double,
)

data class CutPlan(
    val stockBars: Int,
    val usedLengthM: Double,
    val offcutM: Double,
) {
    val boughtLengthM: Double get() = usedLengthM + offcutM
    val offcutPct: Double get() = if (boughtLengthM <= 0) 0.0 else offcutM / boughtLengthM * 100
}

data class RebarResult(
    val barsAlongLength: Int,
    val barsAlongWidth: Int,
    val runLengthLongM: Double,
    val runLengthShortM: Double,
    val lapM: Double,
    val splicesPerLongBar: Int,
    val splicesPerShortBar: Int,
    val totalCutLengthM: Double,
    val netWeightKg: Double,
    val boughtWeightKg: Double,
    val plan: CutPlan,
    val tiePoints: Int,
    val tieWireKg: Double,
    val chairs: Int,
)

object RebarCalculator {
    /** ~30 cm of 1.4 mm annealed wire per saddle tie. */
    const val TIE_WIRE_KG_PER_TIE = 0.004

    fun barCount(clearM: Double, spacingM: Double): Int {
        if (clearM <= 0 || spacingM <= 0) return 0
        return ceilSafe(clearM / spacingM) + 1
    }

    /** Pieces to cut for one continuous run, splicing with laps when longer than stock. */
    fun piecesForRun(runM: Double, stockM: Double, lapM: Double): List<Double> {
        if (runM <= 0) return emptyList()
        if (runM <= stockM + 1e-9) return listOf(runM)
        require(stockM > lapM) { "Stock length must be longer than the lap length" }
        val extra = ceilSafe((runM - stockM) / (stockM - lapM))
        val pieces = MutableList(extra) { stockM }
        val last = runM + extra * lapM - extra * stockM
        pieces.add(last)
        return pieces
    }

    /** First-fit decreasing bin packing of cut pieces into stock bars. */
    fun optimize(pieces: List<Double>, stockM: Double): CutPlan {
        // Full-length pieces use a whole bar each; only the shorter pieces need packing.
        val full = pieces.count { it >= stockM - 1e-9 }
        val sorted = pieces.filter { it < stockM - 1e-9 }.sortedDescending()
        val remaining = ArrayList<Double>()
        for (p in sorted) {
            val idx = remaining.indexOfFirst { it + 1e-9 >= p }
            if (idx >= 0) remaining[idx] = remaining[idx] - p else remaining.add(stockM - p)
        }
        val used = pieces.sum()
        return CutPlan(stockBars = full + remaining.size, usedLengthM = used, offcutM = remaining.sum())
    }

    fun calculate(input: RebarInput): RebarResult {
        val layers = input.layers.coerceIn(1, 2)
        val clearL = max(0.0, input.lengthM - 2 * input.coverM)
        val clearW = max(0.0, input.widthM - 2 * input.coverM)
        // Bars running along the length are distributed across the width, and vice versa.
        val nLong = barCount(clearW, input.spacingLongM)
        val nShort = barCount(clearL, input.spacingShortM)
        val lap = input.lapDiameters * input.bar.diameterMm / 1000.0

        val longPieces = piecesForRun(clearL, input.stockLengthM, lap)
        val shortPieces = piecesForRun(clearW, input.stockLengthM, lap)
        val all = ArrayList<Double>()
        repeat(nLong * layers) { all.addAll(longPieces) }
        repeat(nShort * layers) { all.addAll(shortPieces) }
        val plan = optimize(all, input.stockLengthM)

        val cut = all.sum()
        val ties = nLong * nShort * layers
        val chairsPerLayer = if (input.chairSpacingM > 0 && clearL > 0 && clearW > 0)
            (floor(clearL / input.chairSpacingM).toInt() + 1) * (floor(clearW / input.chairSpacingM).toInt() + 1)
        else 0

        return RebarResult(
            barsAlongLength = nLong * layers,
            barsAlongWidth = nShort * layers,
            runLengthLongM = clearL,
            runLengthShortM = clearW,
            lapM = lap,
            splicesPerLongBar = max(0, longPieces.size - 1),
            splicesPerShortBar = max(0, shortPieces.size - 1),
            totalCutLengthM = cut,
            netWeightKg = cut * input.bar.kgPerM,
            boughtWeightKg = plan.boughtLengthM * input.bar.kgPerM,
            plan = plan,
            tiePoints = ties,
            tieWireKg = ties * TIE_WIRE_KG_PER_TIE,
            chairs = chairsPerLayer * layers,
        )
    }
}
