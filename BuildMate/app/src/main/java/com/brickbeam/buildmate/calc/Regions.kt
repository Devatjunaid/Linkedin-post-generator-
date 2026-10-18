package com.brickbeam.buildmate.calc

enum class RegionGroup(val label: String) { US("United States"), EUROPE("Europe") }

/**
 * A place the user builds in. Tax rates are the statewide / national STANDARD rates
 * (US: state-level sales tax only — the user adds the local rate per project; Europe: standard VAT,
 * which is already included in shelf prices). All rates are editable per project.
 */
data class Region(
    val code: String,
    val name: String,
    val group: RegionGroup,
    val localeTag: String,
    val zone: PriceZone,
    val taxRatePct: Double,
    val taxLabel: String,
    val market: Market,
    val defaultBrickId: String,
    val localTerms: Map<String, String> = emptyMap(),
) {
    val currency: String get() = zone.currency
    /** EU/UK consumer prices include VAT; US prices exclude sales tax. */
    val taxIncludedInPrices: Boolean get() = group == RegionGroup.EUROPE
    val defaultUnitSystem: UnitSystem get() = if (market == Market.US) UnitSystem.IMPERIAL else UnitSystem.METRIC
    val shortCode: String get() = code.substringAfter('-')
}

object Regions {
    private fun us(code: String, name: String, rate: Double) = Region(
        code = "US-$code", name = name, group = RegionGroup.US, localeTag = "en-US",
        zone = PriceZone.USD, taxRatePct = rate, taxLabel = "State sales tax",
        market = Market.US, defaultBrickId = MasonryUnits.US_MODULAR.id,
    )

    private fun eu(
        code: String, name: String, locale: String, zone: PriceZone, vat: Double,
        brick: String = MasonryUnits.DE_NF.id, terms: Map<String, String> = emptyMap(),
        market: Market = Market.EU,
    ) = Region(
        code = "EU-$code", name = name, group = RegionGroup.EUROPE, localeTag = locale, zone = zone,
        taxRatePct = vat, taxLabel = "VAT", market = market, defaultBrickId = brick, localTerms = terms,
    )

    val US_STATES: List<Region> = listOf(
        us("AL", "Alabama", 4.0), us("AK", "Alaska", 0.0), us("AZ", "Arizona", 5.6),
        us("AR", "Arkansas", 6.5), us("CA", "California", 7.25), us("CO", "Colorado", 2.9),
        us("CT", "Connecticut", 6.35), us("DE", "Delaware", 0.0), us("DC", "District of Columbia", 6.5),
        us("FL", "Florida", 6.0), us("GA", "Georgia", 4.0), us("HI", "Hawaii", 4.0),
        us("ID", "Idaho", 6.0), us("IL", "Illinois", 6.25), us("IN", "Indiana", 7.0),
        us("IA", "Iowa", 6.0), us("KS", "Kansas", 6.5), us("KY", "Kentucky", 6.0),
        us("LA", "Louisiana", 5.0), us("ME", "Maine", 5.5), us("MD", "Maryland", 6.0),
        us("MA", "Massachusetts", 6.25), us("MI", "Michigan", 6.0), us("MN", "Minnesota", 6.875),
        us("MS", "Mississippi", 7.0), us("MO", "Missouri", 4.225), us("MT", "Montana", 0.0),
        us("NE", "Nebraska", 5.5), us("NV", "Nevada", 6.85), us("NH", "New Hampshire", 0.0),
        us("NJ", "New Jersey", 6.625), us("NM", "New Mexico", 4.875), us("NY", "New York", 4.0),
        us("NC", "North Carolina", 4.75), us("ND", "North Dakota", 5.0), us("OH", "Ohio", 5.75),
        us("OK", "Oklahoma", 4.5), us("OR", "Oregon", 0.0), us("PA", "Pennsylvania", 6.0),
        us("RI", "Rhode Island", 7.0), us("SC", "South Carolina", 6.0), us("SD", "South Dakota", 4.2),
        us("TN", "Tennessee", 7.0), us("TX", "Texas", 6.25), us("UT", "Utah", 6.1),
        us("VT", "Vermont", 6.0), us("VA", "Virginia", 5.3), us("WA", "Washington", 6.5),
        us("WV", "West Virginia", 6.0), us("WI", "Wisconsin", 5.0), us("WY", "Wyoming", 4.0),
    )

    val EUROPE: List<Region> = listOf(
        eu("AT", "Austria", "de-AT", PriceZone.EUR, 20.0, terms = mapOf("slab" to "Bodenplatte", "brick" to "Ziegel")),
        eu("BE", "Belgium", "nl-BE", PriceZone.EUR, 21.0, MasonryUnits.BE_MODULE.id),
        eu("BG", "Bulgaria", "bg-BG", PriceZone.EUR, 20.0),
        eu("HR", "Croatia", "hr-HR", PriceZone.EUR, 25.0),
        eu("CY", "Cyprus", "el-CY", PriceZone.EUR, 19.0, MasonryUnits.EU_BLOCK_20.id),
        eu("CZ", "Czechia", "cs-CZ", PriceZone.CZK, 21.0),
        eu("DK", "Denmark", "da-DK", PriceZone.DKK, 25.0, MasonryUnits.DK_NORMAL.id),
        eu("EE", "Estonia", "et-EE", PriceZone.EUR, 24.0),
        eu("FI", "Finland", "fi-FI", PriceZone.EUR, 25.5),
        eu("FR", "France", "fr-FR", PriceZone.EUR, 20.0, MasonryUnits.EU_BLOCK_20.id, mapOf("slab" to "Dalle", "block" to "Parpaing")),
        eu("DE", "Germany", "de-DE", PriceZone.EUR, 19.0, terms = mapOf("slab" to "Bodenplatte", "brick" to "Ziegel", "readymix" to "Transportbeton")),
        eu("GR", "Greece", "el-GR", PriceZone.EUR, 24.0, MasonryUnits.EU_BLOCK_20.id),
        eu("HU", "Hungary", "hu-HU", PriceZone.HUF, 27.0),
        eu("IS", "Iceland", "is-IS", PriceZone.ISK, 24.0, MasonryUnits.EU_BLOCK_20.id),
        eu("IE", "Ireland", "en-IE", PriceZone.EUR, 23.0, MasonryUnits.UK_STANDARD.id),
        eu("IT", "Italy", "it-IT", PriceZone.EUR, 22.0, MasonryUnits.IT_UNI.id, mapOf("slab" to "Platea", "brick" to "Mattone")),
        eu("LV", "Latvia", "lv-LV", PriceZone.EUR, 21.0),
        eu("LT", "Lithuania", "lt-LT", PriceZone.EUR, 21.0),
        eu("LU", "Luxembourg", "fr-LU", PriceZone.EUR, 17.0),
        eu("MT", "Malta", "en-MT", PriceZone.EUR, 18.0, MasonryUnits.EU_BLOCK_20.id),
        eu("NL", "Netherlands", "nl-NL", PriceZone.EUR, 21.0, MasonryUnits.NL_WAAL.id, mapOf("brick" to "Baksteen")),
        eu("NO", "Norway", "nb-NO", PriceZone.NOK, 25.0),
        eu("PL", "Poland", "pl-PL", PriceZone.PLN, 23.0),
        eu("PT", "Portugal", "pt-PT", PriceZone.EUR, 23.0, MasonryUnits.EU_BLOCK_20.id),
        eu("RO", "Romania", "ro-RO", PriceZone.RON, 21.0),
        eu("SK", "Slovakia", "sk-SK", PriceZone.EUR, 23.0),
        eu("SI", "Slovenia", "sl-SI", PriceZone.EUR, 22.0),
        eu("ES", "Spain", "es-ES", PriceZone.EUR, 21.0, MasonryUnits.ES_LADRILLO.id, mapOf("slab" to "Losa", "brick" to "Ladrillo")),
        eu("SE", "Sweden", "sv-SE", PriceZone.SEK, 25.0),
        eu("CH", "Switzerland", "de-CH", PriceZone.CHF, 8.1),
        eu("GB", "United Kingdom", "en-GB", PriceZone.GBP, 20.0, MasonryUnits.UK_STANDARD.id, market = Market.UK),
    )

    val ALL: List<Region> = US_STATES + EUROPE

    val DEFAULT: Region = US_STATES.first { it.code == "US-TX" }

    fun byCode(code: String?): Region? = ALL.firstOrNull { it.code == code }

    /** Best guess from the device country, used only to pre-select the picker. */
    fun guessFromCountry(countryIso: String?): Region? = when (val c = countryIso?.uppercase()) {
        null, "" -> null
        "US" -> null // the user must pick a state
        "UK" -> byCode("EU-GB")
        else -> byCode("EU-$c")
    }
}
