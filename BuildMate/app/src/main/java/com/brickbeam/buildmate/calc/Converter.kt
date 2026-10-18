package com.brickbeam.buildmate.calc

/** A unit with a linear factor to the category's base unit. */
data class ConvUnit(val symbol: String, val name: String, val toBase: Double)

enum class ConvCategory(val label: String, val units: List<ConvUnit>) {
    LENGTH(
        "Length",
        listOf(
            ConvUnit("mm", "millimetre", 0.001), ConvUnit("cm", "centimetre", 0.01),
            ConvUnit("m", "metre", 1.0), ConvUnit("in", "inch", Conv.M_PER_IN),
            ConvUnit("ft", "foot", Conv.M_PER_FT), ConvUnit("yd", "yard", 0.9144),
        ),
    ),
    AREA(
        "Area",
        listOf(
            ConvUnit("m²", "square metre", 1.0), ConvUnit("ft²", "square foot", Conv.M2_PER_FT2),
            ConvUnit("yd²", "square yard", 0.83612736), ConvUnit("in²", "square inch", 0.00064516),
            ConvUnit("square", "roofing square (100 ft²)", 100 * Conv.M2_PER_FT2),
        ),
    ),
    VOLUME(
        "Volume",
        listOf(
            ConvUnit("m³", "cubic metre", 1.0), ConvUnit("yd³", "cubic yard", Conv.M3_PER_YD3),
            ConvUnit("ft³", "cubic foot", Conv.M3_PER_FT3), ConvUnit("L", "litre", 0.001),
            ConvUnit("gal", "US gallon", Conv.L_PER_GAL / 1000), ConvUnit("qt", "US quart", Conv.L_PER_QT / 1000),
        ),
    ),
    WEIGHT(
        "Weight",
        listOf(
            ConvUnit("kg", "kilogram", 1.0), ConvUnit("lb", "pound", Conv.KG_PER_LB),
            ConvUnit("t", "tonne", 1000.0), ConvUnit("ton", "US short ton", Conv.KG_PER_SHORT_TON),
            ConvUnit("bag94", "94 lb cement bag", 94 * Conv.KG_PER_LB), ConvUnit("bag25", "25 kg bag", 25.0),
        ),
    ),
    PRESSURE(
        "Strength",
        listOf(
            ConvUnit("MPa", "megapascal (N/mm²)", 1.0), ConvUnit("psi", "pound per sq inch", Conv.MPA_PER_PSI),
            ConvUnit("ksi", "kip per sq inch", Conv.MPA_PER_PSI * 1000), ConvUnit("bar", "bar", 0.1),
        ),
    ),
    COVERAGE(
        "Paint coverage",
        listOf(
            ConvUnit("m²/L", "square metre per litre", 1.0),
            ConvUnit("ft²/gal", "square foot per US gallon", Conv.M2_PER_FT2 / Conv.L_PER_GAL),
        ),
    ),
    BAR_WEIGHT(
        "Bar weight",
        listOf(
            ConvUnit("kg/m", "kilogram per metre", 1.0),
            ConvUnit("lb/ft", "pound per foot", Conv.KG_M_PER_LB_FT),
        ),
    );

    fun convert(value: Double, from: ConvUnit, to: ConvUnit): Double = value * from.toBase / to.toBase
}
