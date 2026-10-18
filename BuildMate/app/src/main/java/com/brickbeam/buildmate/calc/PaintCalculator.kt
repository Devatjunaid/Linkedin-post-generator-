package com.brickbeam.buildmate.calc

import kotlin.math.max
import kotlin.math.roundToInt

enum class Surface(val label: String, val factor: Double) {
    SMOOTH("Smooth / previously painted", 1.0),
    LIGHT_TEXTURE("Light texture", 1.15),
    HEAVY_TEXTURE("Heavy texture / masonry", 1.30),
    NEW_PLASTER("New plaster or drywall", 1.20),
}

data class PaintInput(
    /** Room mode: length, width and height. Ignored when [directWallAreaM2] is set. */
    val roomLengthM: Double,
    val roomWidthM: Double,
    val wallHeightM: Double,
    val directWallAreaM2: Double?,
    val doors: Int,
    val doorAreaM2: Double,
    val windows: Int,
    val windowAreaM2: Double,
    val wallCoats: Int,
    val includeCeiling: Boolean,
    val ceilingCoats: Int,
    val coverageM2PerL: Double,
    val surface: Surface,
    val primer: Boolean,
    val primerCoverageM2PerL: Double,
)

data class CanCount(val can: PaintCan, val count: Int)

data class CanPlan(val cans: List<CanCount>, val totalLitres: Double, val cost: Double) {
    val description: String
        get() = cans.filter { it.count > 0 }.joinToString(" + ") { "${it.count} × ${it.can.label}" }.ifEmpty { "—" }
}

data class PaintResult(
    val wallAreaM2: Double,
    val openingsM2: Double,
    val paintableWallM2: Double,
    val ceilingM2: Double,
    val wallLitres: Double,
    val ceilingLitres: Double,
    val primerLitres: Double,
)

object PaintCalculator {
    fun calculate(input: PaintInput): PaintResult {
        val gross = input.directWallAreaM2
            ?: (2 * (max(0.0, input.roomLengthM) + max(0.0, input.roomWidthM)) * max(0.0, input.wallHeightM))
        val openings = (input.doors * input.doorAreaM2 + input.windows * input.windowAreaM2).coerceIn(0.0, gross)
        val walls = gross - openings
        val ceiling = if (input.includeCeiling && input.directWallAreaM2 == null)
            max(0.0, input.roomLengthM) * max(0.0, input.roomWidthM) else 0.0
        val cov = if (input.coverageM2PerL > 0) input.coverageM2PerL else 1.0
        val wallL = walls * input.wallCoats * input.surface.factor / cov
        val ceilL = ceiling * input.ceilingCoats / cov
        val primerL = if (input.primer && input.primerCoverageM2PerL > 0)
            (walls * input.surface.factor + ceiling) / input.primerCoverageM2PerL else 0.0
        return PaintResult(gross, openings, walls, ceiling, wallL, ceilL, primerL)
    }

    /**
     * Cheapest combination of cans holding at least [litres] (unbounded knapsack over millilitres).
     * Ties are broken by the least leftover paint. Zero-priced cans are treated as unavailable.
     */
    fun planCans(litres: Double, cans: List<PaintCan>, prices: Map<PriceKey, Double>): CanPlan {
        val usable = cans.filter { (prices[it.priceKey] ?: 0.0) > 0.0 }.ifEmpty { cans }
        if (litres <= 0 || usable.isEmpty()) return CanPlan(cans.map { CanCount(it, 0) }, 0.0, 0.0)
        // Work in millilitres; coarsen the grid for very large jobs to keep memory small.
        val step = if (litres > 200) 10.0 else 1.0
        val need = kotlin.math.ceil(litres * 1000 / step).toInt()
        val sizes = usable.map { max(1, (it.litres * 1000 / step).roundToInt()) }
        val costs = usable.map { prices[it.priceKey]?.takeIf { p -> p > 0 } ?: it.litres } // fallback: by volume
        val limit = need + sizes.max()
        val dp = DoubleArray(limit + 1) { Double.POSITIVE_INFINITY }
        val choice = IntArray(limit + 1) { -1 }
        dp[0] = 0.0
        for (c in 1..limit) {
            for (i in sizes.indices) {
                val prev = c - sizes[i]
                if (prev >= 0 && dp[prev] + costs[i] < dp[c] - 1e-9) {
                    dp[c] = dp[prev] + costs[i]
                    choice[c] = i
                }
            }
        }
        var best = -1
        for (c in need..limit) if (dp[c].isFinite() && (best < 0 || dp[c] < dp[best] - 1e-9)) best = c
        val counts = IntArray(usable.size)
        var c = best
        while (c > 0) {
            val i = choice[c]
            counts[i]++
            c -= sizes[i]
        }
        val result = cans.map { can ->
            val idx = usable.indexOf(can)
            CanCount(can, if (idx >= 0) counts[idx] else 0)
        }
        val total = result.sumOf { it.count * it.can.litres }
        val cost = result.sumOf { it.count * (prices[it.can.priceKey] ?: 0.0) }
        return CanPlan(result, total, cost)
    }
}
