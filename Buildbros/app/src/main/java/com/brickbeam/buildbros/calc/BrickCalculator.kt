package com.brickbeam.buildbros.calc

import kotlin.math.ceil
import kotlin.math.max

/** Actual (not nominal) unit dimensions in millimetres. */
data class MasonryUnit(
    val id: String,
    val name: String,
    val lengthMm: Double,
    val heightMm: Double,
    val depthMm: Double,
    val defaultJointMm: Double,
    val isBlock: Boolean = false,
    val markets: Set<Market> = Market.entries.toSet(),
) {
    val volumeM3: Double get() = lengthMm * heightMm * depthMm / 1e9
}

object MasonryUnits {
    private val US = setOf(Market.US)
    private val EUROPE = setOf(Market.UK, Market.EU)

    // US sizes from ASTM C216 / BIA Technical Note 10 (actual dimensions).
    val US_MODULAR = MasonryUnit("us_modular", "Modular brick 7⅝×2¼×3⅝ in", 193.7, 57.2, 92.1, 9.5, markets = US)
    val US_STANDARD = MasonryUnit("us_standard", "Standard brick 8×2¼×3⅝ in", 203.2, 57.2, 92.1, 9.5, markets = US)
    val US_QUEEN = MasonryUnit("us_queen", "Queen brick 7⅝×2¾×3 in", 193.7, 69.9, 76.2, 9.5, markets = US)
    val US_KING = MasonryUnit("us_king", "King brick 9⅝×2⅝×3 in", 244.5, 66.7, 76.2, 9.5, markets = US)
    val US_UTILITY = MasonryUnit("us_utility", "Utility brick 11⅝×3⅝×3⅝ in", 295.3, 92.1, 92.1, 9.5, markets = US)
    val CMU_4 = MasonryUnit("cmu_4", "CMU block 4×8×16 in", 396.9, 193.7, 92.1, 9.5, true, US)
    val CMU_6 = MasonryUnit("cmu_6", "CMU block 6×8×16 in", 396.9, 193.7, 142.9, 9.5, true, US)
    val CMU_8 = MasonryUnit("cmu_8", "CMU block 8×8×16 in", 396.9, 193.7, 193.7, 9.5, true, US)
    val CMU_12 = MasonryUnit("cmu_12", "CMU block 12×8×16 in", 396.9, 193.7, 295.3, 9.5, true, US)

    val UK_STANDARD = MasonryUnit("uk_standard", "UK brick 215×102.5×65 mm", 215.0, 65.0, 102.5, 10.0, markets = EUROPE)
    val UK_BLOCK_100 = MasonryUnit("uk_block_100", "UK block 440×215×100 mm", 440.0, 215.0, 100.0, 10.0, true, EUROPE)
    val UK_BLOCK_140 = MasonryUnit("uk_block_140", "UK block 440×215×140 mm", 440.0, 215.0, 140.0, 10.0, true, EUROPE)
    val DE_DF = MasonryUnit("de_df", "DF 240×115×52 mm", 240.0, 52.0, 115.0, 12.0, markets = EUROPE)
    val DE_NF = MasonryUnit("de_nf", "NF 240×115×71 mm", 240.0, 71.0, 115.0, 12.0, markets = EUROPE)
    val DE_2DF = MasonryUnit("de_2df", "2DF 240×115×113 mm", 240.0, 113.0, 115.0, 12.0, markets = EUROPE)
    val NL_WAAL = MasonryUnit("nl_waal", "Waalformaat 210×100×50 mm", 210.0, 50.0, 100.0, 12.0, markets = EUROPE)
    val BE_MODULE = MasonryUnit("be_module", "Belgian module 190×90×50 mm", 190.0, 50.0, 90.0, 10.0, markets = EUROPE)
    val DK_NORMAL = MasonryUnit("dk_normal", "Danish normal 228×108×54 mm", 228.0, 54.0, 108.0, 12.0, markets = EUROPE)
    val IT_UNI = MasonryUnit("it_uni", "Italian UNI 250×120×55 mm", 250.0, 55.0, 120.0, 10.0, markets = EUROPE)
    val ES_LADRILLO = MasonryUnit("es_ladrillo", "Ladrillo 240×115×50 mm", 240.0, 50.0, 115.0, 10.0, markets = EUROPE)
    val EU_BLOCK_20 = MasonryUnit("eu_block_20", "Concrete block 500×200×200 mm", 500.0, 200.0, 200.0, 10.0, true, EUROPE)
    val AAC_100 = MasonryUnit("aac_100", "AAC block 625×250×100 (thin bed)", 625.0, 250.0, 100.0, 2.0, true, EUROPE)
    val AAC_200 = MasonryUnit("aac_200", "AAC block 625×250×200 (thin bed)", 625.0, 250.0, 200.0, 2.0, true, EUROPE)

    val ALL = listOf(
        US_MODULAR, US_STANDARD, US_QUEEN, US_KING, US_UTILITY, CMU_4, CMU_6, CMU_8, CMU_12,
        UK_STANDARD, UK_BLOCK_100, UK_BLOCK_140, DE_DF, DE_NF, DE_2DF, NL_WAAL, BE_MODULE,
        DK_NORMAL, IT_UNI, ES_LADRILLO, EU_BLOCK_20, AAC_100, AAC_200,
    )

    fun byId(id: String?) = ALL.firstOrNull { it.id == id }
    fun forMarket(market: Market) = ALL.filter { market in it.markets }
}

data class Opening(val widthM: Double, val heightM: Double, val count: Int = 1) {
    val areaM2: Double get() = widthM * heightM * count
}

data class BrickInput(
    val wallLengthM: Double,
    val wallHeightM: Double,
    val openings: List<Opening>,
    val unit: MasonryUnit,
    val jointMm: Double,
    /** 1 = half-brick / single wythe, 2 = one-brick / double wythe with a collar joint. */
    val leaves: Int,
    val wastePct: Double,
    /** Mortar mix 1 : [sandParts] cement : sand by volume. */
    val sandParts: Double,
    val mortarWastePct: Double,
    val cementBag: CementBag,
    val tiesPerM2: Double,
    val palletSize: Int,
    /** Single-wythe veneer tied back to a frame/block backing (needs wall ties too). */
    val tiedVeneer: Boolean = false,
)

data class BrickResult(
    val grossAreaM2: Double,
    val openingsAreaM2: Double,
    val netAreaM2: Double,
    val unitsPerM2: Double,
    val unitsExact: Double,
    val unitsToBuy: Int,
    val fullPallets: Int,
    val looseUnits: Int,
    val mortarWetM3: Double,
    val mortarDryM3: Double,
    val cementKg: Double,
    val cementBags: Int,
    val sandM3: Double,
    val sandKg: Double,
    val wallTies: Int,
)

object BrickCalculator {
    /** Dry loose materials needed per m³ of wet mortar (bulking + voids). */
    const val MORTAR_DRY_FACTOR = 1.27
    const val SAND_BULK_DENSITY = 1600.0

    fun unitsPerM2(unit: MasonryUnit, jointMm: Double): Double {
        val l = (unit.lengthMm + jointMm) / 1000.0
        val h = (unit.heightMm + jointMm) / 1000.0
        return 1.0 / (l * h)
    }

    fun calculate(input: BrickInput): BrickResult {
        val gross = max(0.0, input.wallLengthM) * max(0.0, input.wallHeightM)
        val openings = input.openings.sumOf { it.areaM2 }.coerceAtMost(gross)
        val net = gross - openings
        val perM2 = unitsPerM2(input.unit, input.jointMm)
        val leaves = input.leaves.coerceIn(1, 2)

        val exactPerLeaf = net * perM2
        val exact = exactPerLeaf * leaves
        val toBuy = ceilSafe(exact * (1 + input.wastePct / 100.0))

        // Mortar = wall volume - unit volume, per leaf, plus a collar joint between two leaves.
        val leafVolume = net * input.unit.depthMm / 1000.0
        val mortarPerLeaf = max(0.0, leafVolume - exactPerLeaf * input.unit.volumeM3)
        val collar = if (leaves == 2) net * input.jointMm / 1000.0 else 0.0
        val wet = (mortarPerLeaf * leaves + collar) * (1 + input.mortarWastePct / 100.0)
        val dry = wet * MORTAR_DRY_FACTOR
        val parts = 1.0 + input.sandParts
        val cementVol = dry / parts
        val sandVol = dry * input.sandParts / parts
        val bags = ceilSafe(cementVol / input.cementBag.volumeM3)

        val pallets = if (input.palletSize > 0) toBuy / input.palletSize else 0
        val loose = if (input.palletSize > 0) toBuy % input.palletSize else toBuy

        return BrickResult(
            grossAreaM2 = gross,
            openingsAreaM2 = openings,
            netAreaM2 = net,
            unitsPerM2 = perM2,
            unitsExact = exact,
            unitsToBuy = toBuy,
            fullPallets = pallets,
            looseUnits = loose,
            mortarWetM3 = wet,
            mortarDryM3 = dry,
            cementKg = cementVol * input.cementBag.bulkDensityKgM3,
            cementBags = bags,
            sandM3 = sandVol,
            sandKg = sandVol * SAND_BULK_DENSITY,
            wallTies = if (leaves == 2 || input.tiedVeneer) ceilSafe(net * input.tiesPerM2) else 0,
        )
    }
}

/** ceil that ignores floating-point noise (e.g. 900.0000000001 -> 900). */
fun ceilSafe(v: Double): Int = if (v <= 0) 0 else ceil(v - 1e-9).toInt()
