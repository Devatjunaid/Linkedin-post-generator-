package com.sunarc.inverterlogger.domain

enum class Region(val label: String) {
    NORTH_AMERICA("North America"),
    EUROPE("Europe")
}

/** How exported solar energy is paid for. Users can change it; this is only the default. */
enum class TariffModel(val label: String, val description: String) {
    NET_METERING(
        "Net metering",
        "Exported kWh offset kWh you buy, so every kWh is worth your buy price."
    ),
    NET_BILLING(
        "Net billing",
        "Exported kWh are credited at a lower export rate than your buy price."
    ),
    FEED_IN(
        "Feed-in / export tariff",
        "Self-used kWh save your buy price; exported kWh earn the feed-in rate."
    )
}

enum class DecimalStyle { POINT, COMMA }

data class Country(
    val code: String,
    val name: String,
    val region: Region,
    val currencyCode: String,
    val currencySymbol: String,
    val tariffModel: TariffModel,
    /** Approximate grid carbon intensity in g CO2 per kWh. Shown as editable in settings. */
    val gridCo2GramsPerKwh: Int,
    /** A central point used for the sun model until the user sets their own location. */
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
    val decimalStyle: DecimalStyle
)

object Countries {
    val all: List<Country> = listOf(
        Country("US", "United States", Region.NORTH_AMERICA, "USD", "$", TariffModel.NET_METERING, 370, 39.8, -98.6, "America/Chicago", DecimalStyle.POINT),
        Country("CA", "Canada", Region.NORTH_AMERICA, "CAD", "$", TariffModel.NET_METERING, 110, 45.4, -75.7, "America/Toronto", DecimalStyle.POINT),
        Country("MX", "Mexico", Region.NORTH_AMERICA, "MXN", "$", TariffModel.NET_METERING, 420, 19.4, -99.1, "America/Mexico_City", DecimalStyle.POINT),
        Country("DE", "Germany", Region.EUROPE, "EUR", "€", TariffModel.FEED_IN, 380, 51.2, 10.4, "Europe/Berlin", DecimalStyle.COMMA),
        Country("NL", "Netherlands", Region.EUROPE, "EUR", "€", TariffModel.NET_METERING, 270, 52.1, 5.3, "Europe/Amsterdam", DecimalStyle.COMMA),
        Country("BE", "Belgium", Region.EUROPE, "EUR", "€", TariffModel.NET_BILLING, 140, 50.8, 4.4, "Europe/Brussels", DecimalStyle.COMMA),
        Country("FR", "France", Region.EUROPE, "EUR", "€", TariffModel.FEED_IN, 55, 46.6, 2.4, "Europe/Paris", DecimalStyle.COMMA),
        Country("ES", "Spain", Region.EUROPE, "EUR", "€", TariffModel.NET_BILLING, 140, 40.4, -3.7, "Europe/Madrid", DecimalStyle.COMMA),
        Country("PT", "Portugal", Region.EUROPE, "EUR", "€", TariffModel.FEED_IN, 140, 39.6, -8.0, "Europe/Lisbon", DecimalStyle.COMMA),
        Country("IT", "Italy", Region.EUROPE, "EUR", "€", TariffModel.NET_BILLING, 280, 42.8, 12.5, "Europe/Rome", DecimalStyle.COMMA),
        Country("GB", "United Kingdom", Region.EUROPE, "GBP", "£", TariffModel.FEED_IN, 210, 52.5, -1.5, "Europe/London", DecimalStyle.POINT),
        Country("IE", "Ireland", Region.EUROPE, "EUR", "€", TariffModel.FEED_IN, 280, 53.3, -7.7, "Europe/Dublin", DecimalStyle.POINT),
        Country("AT", "Austria", Region.EUROPE, "EUR", "€", TariffModel.FEED_IN, 110, 47.6, 14.1, "Europe/Vienna", DecimalStyle.COMMA),
        Country("CH", "Switzerland", Region.EUROPE, "CHF", "CHF ", TariffModel.FEED_IN, 40, 46.8, 8.2, "Europe/Zurich", DecimalStyle.POINT),
        Country("PL", "Poland", Region.EUROPE, "PLN", "zł ", TariffModel.NET_BILLING, 660, 52.1, 19.4, "Europe/Warsaw", DecimalStyle.COMMA),
        Country("CZ", "Czechia", Region.EUROPE, "CZK", "Kč ", TariffModel.FEED_IN, 420, 49.8, 15.5, "Europe/Prague", DecimalStyle.COMMA),
        Country("SE", "Sweden", Region.EUROPE, "SEK", "kr ", TariffModel.FEED_IN, 30, 59.3, 18.1, "Europe/Stockholm", DecimalStyle.COMMA),
        Country("DK", "Denmark", Region.EUROPE, "DKK", "kr ", TariffModel.FEED_IN, 140, 56.0, 10.0, "Europe/Copenhagen", DecimalStyle.COMMA),
        Country("NO", "Norway", Region.EUROPE, "NOK", "kr ", TariffModel.FEED_IN, 20, 60.5, 8.5, "Europe/Oslo", DecimalStyle.COMMA),
        Country("FI", "Finland", Region.EUROPE, "EUR", "€", TariffModel.FEED_IN, 80, 61.9, 25.7, "Europe/Helsinki", DecimalStyle.COMMA),
        Country("GR", "Greece", Region.EUROPE, "EUR", "€", TariffModel.NET_BILLING, 330, 39.1, 22.0, "Europe/Athens", DecimalStyle.COMMA),
        Country("HU", "Hungary", Region.EUROPE, "HUF", "Ft ", TariffModel.NET_BILLING, 200, 47.2, 19.5, "Europe/Budapest", DecimalStyle.COMMA),
        Country("RO", "Romania", Region.EUROPE, "RON", "lei ", TariffModel.NET_BILLING, 250, 45.9, 25.0, "Europe/Bucharest", DecimalStyle.COMMA)
    )

    fun byCode(code: String?): Country = all.firstOrNull { it.code == code } ?: all.first { it.code == "DE" }

    fun inRegion(region: Region): List<Country> = all.filter { it.region == region }
}
