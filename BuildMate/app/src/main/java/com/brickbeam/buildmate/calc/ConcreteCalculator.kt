package com.brickbeam.buildmate.calc

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

enum class ConcreteShape(val label: String) {
    SLAB("Slab"),
    FOOTING("Footing"),
    WALL("Wall"),
    COLUMN_ROUND("Round column"),
    COLUMN_SQUARE("Square column"),
    ROUND_PAD("Round pad"),
    STEPS("Steps"),
}

/** All dimensions in metres. Unused fields are ignored for the chosen shape. */
data class ConcreteDims(
    val shape: ConcreteShape,
    val a: Double = 0.0, // length / diameter / step width
    val b: Double = 0.0, // width / height / riser height
    val c: Double = 0.0, // thickness / depth / tread depth
    val count: Int = 1,  // number of columns / number of steps
)

data class MixGrade(
    val id: String,
    val label: String,
    val equivalent: String,
    val cementKgPerM3: Double,
    val waterCementRatio: Double,
)

object MixGrades {
    private const val LB_YD3_TO_KG_M3 = Conv.KG_PER_LB / Conv.M3_PER_YD3 // 0.5933

    /** Typical US residential mixes; cement contents from 5 to 7 sack (94 lb) mixes. */
    val US = listOf(
        MixGrade("psi2500", "2,500 psi", "17 MPa", 470 * LB_YD3_TO_KG_M3, 0.62),
        MixGrade("psi3000", "3,000 psi", "21 MPa", 517 * LB_YD3_TO_KG_M3, 0.58),
        MixGrade("psi3500", "3,500 psi", "24 MPa", 564 * LB_YD3_TO_KG_M3, 0.53),
        MixGrade("psi4000", "4,000 psi", "28 MPa", 611 * LB_YD3_TO_KG_M3, 0.48),
        MixGrade("psi4500", "4,500 psi", "31 MPa", 658 * LB_YD3_TO_KG_M3, 0.44),
    )

    /** EN 206 strength classes (cylinder/cube), nominal design-mix cement contents. */
    val EN = listOf(
        MixGrade("c16", "C16/20", "≈2,300 psi", 260.0, 0.65),
        MixGrade("c20", "C20/25", "≈2,900 psi", 290.0, 0.60),
        MixGrade("c25", "C25/30", "≈3,600 psi", 320.0, 0.55),
        MixGrade("c30", "C30/37", "≈4,350 psi", 360.0, 0.50),
        MixGrade("c35", "C35/45", "≈5,100 psi", 400.0, 0.45),
    )

    fun forMarket(market: Market) = if (market == Market.US) US else EN
    fun default(market: Market) = forMarket(market)[if (market == Market.US) 1 else 2]
}

data class ConcreteResult(
    val netVolumeM3: Double,
    val orderVolumeM3: Double,
    val cementKg: Double,
    val cementBags: Int,
    val waterL: Double,
    val sandKg: Double,
    val gravelKg: Double,
    val sandM3: Double,
    val gravelM3: Double,
    val premixBags: Int,
    val readyMixOrderM3: Double,
    val trucks: Int,
    val shortLoad: Boolean,
    val recommendReadyMix: Boolean,
)

object ConcreteCalculator {
    const val FRESH_DENSITY = 2400.0      // kg/m³ including water
    const val SAND_FRACTION = 0.40        // of total aggregate mass
    const val SAND_BULK = 1600.0          // kg/m³ loose
    const val GRAVEL_BULK = 1500.0        // kg/m³ loose
    /** Below roughly 3 m³ (≈4 yd³) many plants add a short-load fee. */
    const val SHORT_LOAD_M3 = 3.0
    /** Above ~1 m³ hand mixing becomes impractical for one pour. */
    const val READY_MIX_THRESHOLD_M3 = 1.0

    fun volume(d: ConcreteDims): Double {
        val n = max(1, d.count)
        return when (d.shape) {
            ConcreteShape.SLAB, ConcreteShape.FOOTING, ConcreteShape.WALL -> d.a * d.b * d.c * n
            ConcreteShape.COLUMN_ROUND -> PI * (d.a / 2) * (d.a / 2) * d.b * n
            ConcreteShape.COLUMN_SQUARE -> d.a * d.a * d.b * n
            ConcreteShape.ROUND_PAD -> PI * (d.a / 2) * (d.a / 2) * d.c
            // Solid steps on grade: step i (1-based) is i risers tall.
            ConcreteShape.STEPS -> d.a * d.c * d.b * n * (n + 1) / 2.0
        }.coerceAtLeast(0.0)
    }

    /** Sloped length of a stair flight (handy for formwork); not used in volume. */
    fun stairSlopeLength(d: ConcreteDims) = sqrt(d.b * d.b + d.c * d.c) * max(1, d.count)

    fun calculate(
        dims: ConcreteDims,
        grade: MixGrade,
        wastePct: Double,
        spec: MarketSpec,
        premix: PremixBag,
    ): ConcreteResult {
        val net = volume(dims)
        val order = net * (1 + wastePct / 100.0)
        val cement = order * grade.cementKgPerM3
        val water = cement * grade.waterCementRatio
        val aggregate = max(0.0, order * FRESH_DENSITY - cement - water)
        val sand = aggregate * SAND_FRACTION
        val gravel = aggregate - sand
        val inc = spec.readyMixIncrementM3
        val ready = if (order <= 0) 0.0 else ceil(order / inc - 1e-9) * inc
        return ConcreteResult(
            netVolumeM3 = net,
            orderVolumeM3 = order,
            cementKg = cement,
            cementBags = ceilSafe(cement / spec.concreteCement.massKg),
            waterL = water,
            sandKg = sand,
            gravelKg = gravel,
            sandM3 = sand / SAND_BULK,
            gravelM3 = gravel / GRAVEL_BULK,
            premixBags = ceilSafe(order / premix.yieldM3),
            readyMixOrderM3 = ready,
            trucks = ceilSafe(ready / spec.truckCapacityM3),
            shortLoad = ready in 0.0001..SHORT_LOAD_M3,
            recommendReadyMix = order > READY_MIX_THRESHOLD_M3,
        )
    }
}
