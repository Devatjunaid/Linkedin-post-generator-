package com.brickbeam.buildbros.calc

/**
 * Packaging and product conventions differ by market. Prices are always quoted in the market's
 * own purchase units (e.g. rebar per lb in the US, per kg in Europe), independent of the unit
 * system the user prefers to *see* measurements in.
 */
enum class Market { US, UK, EU }

data class CementBag(val label: String, val massKg: Double, val bulkDensityKgM3: Double) {
    /** Loose volume of one bag, used for volume-batched mixes (1 : N). */
    val volumeM3: Double get() = massKg / bulkDensityKgM3
}

data class PremixBag(val label: String, val massKg: Double, val yieldM3: Double)

data class PaintCan(val label: String, val litres: Double, val priceKey: PriceKey)

data class RebarSize(val id: String, val label: String, val diameterMm: Double, val kgPerM: Double)

data class MarketSpec(
    val market: Market,
    val concreteCement: CementBag,
    val masonryCement: CementBag,
    val premixBags: List<PremixBag>,
    val adhesiveBagKg: Double,
    val adhesiveBagLabel: String,
    val groutBagKg: Double,
    val groutBagLabel: String,
    val paintCans: List<PaintCan>,
    val rebarSizes: List<RebarSize>,
    val defaultRebarId: String,
    val stockLengthsM: List<Double>,
    val wallTiesPerM2: Double,
    val truckCapacityM3: Double,
    val readyMixIncrementM3: Double,
    val defaultPalletSize: Int,
) {
    /** Mass unit used on price tags for bulk materials (short ton in the US, tonne elsewhere). */
    val bulkTonKg: Double get() = if (market == Market.US) Conv.KG_PER_SHORT_TON else 1000.0
}

object Markets {
    private fun usRebar(id: String, label: String, dIn: Double, lbPerFt: Double) =
        RebarSize(id, label, dIn * 25.4, lbPerFt * Conv.KG_M_PER_LB_FT)

    /** Metric bar mass: d² × π/4 × 7850 kg/m³ = d² / 162.2 (d in mm). */
    private fun metricRebar(d: Int) = RebarSize("d$d", "Ø$d", d.toDouble(), d * d / 162.2)

    val US = MarketSpec(
        market = Market.US,
        concreteCement = CementBag("94 lb portland", 94 * Conv.KG_PER_LB, 1506.0), // 94 lb = 1 ft³ loose
        masonryCement = CementBag("70 lb masonry cement", 70 * Conv.KG_PER_LB, 1121.0), // 70 lb = 1 ft³
        premixBags = listOf(
            PremixBag("80 lb bag", 80 * Conv.KG_PER_LB, 0.60 * Conv.M3_PER_FT3),
            PremixBag("60 lb bag", 60 * Conv.KG_PER_LB, 0.45 * Conv.M3_PER_FT3),
            PremixBag("50 lb bag", 50 * Conv.KG_PER_LB, 0.375 * Conv.M3_PER_FT3),
            PremixBag("40 lb bag", 40 * Conv.KG_PER_LB, 0.30 * Conv.M3_PER_FT3),
        ),
        adhesiveBagKg = 50 * Conv.KG_PER_LB, adhesiveBagLabel = "50 lb thinset",
        groutBagKg = 25 * Conv.KG_PER_LB, groutBagLabel = "25 lb grout",
        paintCans = listOf(
            PaintCan("Quart", Conv.L_PER_QT, PriceKey.PAINT_S),
            PaintCan("Gallon", Conv.L_PER_GAL, PriceKey.PAINT_M),
            PaintCan("5-gal pail", 5 * Conv.L_PER_GAL, PriceKey.PAINT_L),
        ),
        rebarSizes = listOf(
            usRebar("n3", "#3 (3/8\")", 0.375, 0.376),
            usRebar("n4", "#4 (1/2\")", 0.500, 0.668),
            usRebar("n5", "#5 (5/8\")", 0.625, 1.043),
            usRebar("n6", "#6 (3/4\")", 0.750, 1.502),
            usRebar("n7", "#7 (7/8\")", 0.875, 2.044),
            usRebar("n8", "#8 (1\")", 1.000, 2.670),
        ),
        defaultRebarId = "n4",
        stockLengthsM = listOf(20 * Conv.M_PER_FT, 40 * Conv.M_PER_FT),
        wallTiesPerM2 = 1.0 / (2.67 * Conv.M2_PER_FT2), // 1 tie per 2.67 ft² (TMS 402 veneer)
        truckCapacityM3 = 10 * Conv.M3_PER_YD3,
        readyMixIncrementM3 = 0.25 * Conv.M3_PER_YD3,
        defaultPalletSize = 500,
    )

    private val metricRebars = listOf(6, 8, 10, 12, 14, 16, 20, 25).map { metricRebar(it) }

    val UK = MarketSpec(
        market = Market.UK,
        concreteCement = CementBag("25 kg cement", 25.0, 1440.0),
        masonryCement = CementBag("25 kg cement", 25.0, 1440.0),
        premixBags = listOf(PremixBag("25 kg bag", 25.0, 0.012), PremixBag("20 kg bag", 20.0, 0.0096)),
        adhesiveBagKg = 20.0, adhesiveBagLabel = "20 kg adhesive",
        groutBagKg = 5.0, groutBagLabel = "5 kg grout",
        paintCans = listOf(
            PaintCan("1 L", 1.0, PriceKey.PAINT_S),
            PaintCan("2.5 L", 2.5, PriceKey.PAINT_M),
            PaintCan("5 L", 5.0, PriceKey.PAINT_L),
            PaintCan("10 L", 10.0, PriceKey.PAINT_XL),
        ),
        rebarSizes = metricRebars, defaultRebarId = "d10",
        stockLengthsM = listOf(6.0, 12.0),
        wallTiesPerM2 = 2.5, // BS EN 1996: 2.5 ties/m² in cavity walls
        truckCapacityM3 = 6.0,
        readyMixIncrementM3 = 0.25,
        defaultPalletSize = 500,
    )

    val EU = UK.copy(
        market = Market.EU,
        premixBags = listOf(PremixBag("25 kg bag", 25.0, 0.012), PremixBag("40 kg bag", 40.0, 0.019)),
        adhesiveBagKg = 25.0, adhesiveBagLabel = "25 kg adhesive",
        truckCapacityM3 = 8.0,
        defaultPalletSize = 400,
    )

    fun spec(market: Market) = when (market) {
        Market.US -> US
        Market.UK -> UK
        Market.EU -> EU
    }
}
