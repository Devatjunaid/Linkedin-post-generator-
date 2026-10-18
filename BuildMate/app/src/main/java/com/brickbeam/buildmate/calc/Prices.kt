package com.brickbeam.buildmate.calc

import kotlin.math.round

/** Category of a material, used for grouping, colours and the cost breakdown bar. */
enum class Category(val label: String) {
    BRICK("Bricks & blocks"),
    CEMENT("Cement & concrete"),
    STEEL("Steel"),
    TILE("Tiles"),
    PAINT("Paint"),
    OTHER("Other"),
}

/**
 * Every price the calculators can cost. [usUnit]/[metricUnit] are the purchase units of the
 * market (US vs UK/EU), shown next to the price field so it is clear what the number means.
 */
enum class PriceKey(
    val label: String,
    val usUnit: String,
    val metricUnit: String,
    val category: Category,
) {
    BRICK("Brick", "each", "each", Category.BRICK),
    BLOCK("Concrete / AAC block", "each", "each", Category.BRICK),
    WALL_TIE("Wall tie", "each", "each", Category.BRICK),
    MASONRY_CEMENT_BAG("Mortar cement bag", "70 lb bag", "25 kg bag", Category.CEMENT),
    CEMENT_BAG("Cement bag (concrete)", "94 lb bag", "25 kg bag", Category.CEMENT),
    SAND("Sand", "ton", "tonne", Category.CEMENT),
    GRAVEL("Gravel / aggregate", "ton", "tonne", Category.CEMENT),
    PREMIX_BAG("Pre-mixed concrete bag", "80 lb bag", "25 kg bag", Category.CEMENT),
    READYMIX("Ready-mix concrete", "yd³", "m³", Category.CEMENT),
    REBAR("Rebar", "lb", "kg", Category.STEEL),
    TIE_WIRE("Tie wire", "lb", "kg", Category.STEEL),
    REBAR_CHAIR("Rebar chair / spacer", "each", "each", Category.STEEL),
    TILE("Tile", "sq ft", "m²", Category.TILE),
    ADHESIVE_BAG("Tile adhesive bag", "50 lb bag", "bag", Category.TILE),
    GROUT_BAG("Grout bag", "25 lb bag", "bag", Category.TILE),
    PAINT_S("Paint, small can", "quart", "1 L", Category.PAINT),
    PAINT_M("Paint, medium can", "gallon", "2.5 L", Category.PAINT),
    PAINT_L("Paint, large can", "5 gal", "5 L", Category.PAINT),
    PAINT_XL("Paint, 10 L can", "—", "10 L", Category.PAINT);

    fun unit(market: Market) = if (market == Market.US) usUnit else metricUnit

    /** The US market has no 10 L can. */
    fun appliesTo(market: Market) = !(this == PAINT_XL && market == Market.US)
}

/** Currency + price level zone. Prices are SAMPLE defaults that the user edits in the app. */
enum class PriceZone(val currency: String, val factorFromEur: Double, val decimals: Int) {
    USD("USD", 0.0, 2),
    GBP("GBP", 0.0, 2),
    EUR("EUR", 1.0, 2),
    CHF("CHF", 1.30, 2),
    SEK("SEK", 11.5, 0),
    NOK("NOK", 12.5, 0),
    DKK("DKK", 7.6, 0),
    PLN("PLN", 3.4, 2),
    CZK("CZK", 21.0, 0),
    HUF("HUF", 300.0, 0),
    RON("RON", 3.6, 2),
    ISK("ISK", 200.0, 0),
}

object DefaultPrices {
    private val usd = mapOf(
        PriceKey.BRICK to 0.65, PriceKey.BLOCK to 2.10, PriceKey.WALL_TIE to 0.30,
        PriceKey.MASONRY_CEMENT_BAG to 16.00, PriceKey.CEMENT_BAG to 15.80,
        PriceKey.SAND to 50.0, PriceKey.GRAVEL to 45.0, PriceKey.PREMIX_BAG to 6.50,
        PriceKey.READYMIX to 165.0, PriceKey.REBAR to 0.85, PriceKey.TIE_WIRE to 2.50,
        PriceKey.REBAR_CHAIR to 0.35, PriceKey.TILE to 2.60, PriceKey.ADHESIVE_BAG to 22.0,
        PriceKey.GROUT_BAG to 25.0, PriceKey.PAINT_S to 18.0, PriceKey.PAINT_M to 45.0,
        PriceKey.PAINT_L to 190.0, PriceKey.PAINT_XL to 0.0,
    )
    private val gbp = mapOf(
        PriceKey.BRICK to 0.55, PriceKey.BLOCK to 1.80, PriceKey.WALL_TIE to 0.25,
        PriceKey.MASONRY_CEMENT_BAG to 7.20, PriceKey.CEMENT_BAG to 7.20,
        PriceKey.SAND to 45.0, PriceKey.GRAVEL to 40.0, PriceKey.PREMIX_BAG to 6.00,
        PriceKey.READYMIX to 125.0, PriceKey.REBAR to 1.40, PriceKey.TIE_WIRE to 4.00,
        PriceKey.REBAR_CHAIR to 0.25, PriceKey.TILE to 22.0, PriceKey.ADHESIVE_BAG to 18.0,
        PriceKey.GROUT_BAG to 10.0, PriceKey.PAINT_S to 9.0, PriceKey.PAINT_M to 19.0,
        PriceKey.PAINT_L to 32.0, PriceKey.PAINT_XL to 55.0,
    )
    private val eur = mapOf(
        PriceKey.BRICK to 0.42, PriceKey.BLOCK to 1.90, PriceKey.WALL_TIE to 0.25,
        PriceKey.MASONRY_CEMENT_BAG to 6.90, PriceKey.CEMENT_BAG to 6.90,
        PriceKey.SAND to 38.0, PriceKey.GRAVEL to 32.0, PriceKey.PREMIX_BAG to 5.20,
        PriceKey.READYMIX to 145.0, PriceKey.REBAR to 1.60, PriceKey.TIE_WIRE to 4.00,
        PriceKey.REBAR_CHAIR to 0.25, PriceKey.TILE to 24.0, PriceKey.ADHESIVE_BAG to 16.0,
        PriceKey.GROUT_BAG to 12.0, PriceKey.PAINT_S to 9.0, PriceKey.PAINT_M to 19.0,
        PriceKey.PAINT_L to 32.0, PriceKey.PAINT_XL to 55.0,
    )

    fun forZone(zone: PriceZone): Map<PriceKey, Double> = when (zone) {
        PriceZone.USD -> usd
        PriceZone.GBP -> gbp
        PriceZone.EUR -> eur
        else -> eur.mapValues { (_, v) -> roundTo(v * zone.factorFromEur, zone.decimals) }
    }

    private fun roundTo(v: Double, decimals: Int): Double {
        var f = 1.0
        repeat(decimals) { f *= 10 }
        return round(v * f) / f
    }
}
