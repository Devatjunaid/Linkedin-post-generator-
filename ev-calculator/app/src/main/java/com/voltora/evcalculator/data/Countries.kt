package com.voltora.evcalculator.data

import java.util.Locale

/**
 * Typical 2025 energy prices, in local currency.
 *
 * home     – residential electricity, all-in price per kWh (energy + network + taxes)
 * publicAc – typical pay-as-you-go price per kWh at public AC (7–22 kW) chargers
 * dcFast   – typical pay-as-you-go price per kWh at DC fast / ultra-fast chargers
 * petrol   – regular unleaded (95 RON / US regular) per litre
 *
 * These are national averages. Real tariffs vary by supplier, region, time of day and
 * charging network, so every value can be overridden by the user in Settings.
 */
data class Country(
    val code: String,
    val name: String,
    val flag: String,
    val currencyCode: String,
    val symbol: String,
    val moneyDecimals: Int,
    val home: Double,
    val publicAc: Double,
    val dcFast: Double,
    val petrol: Double,
    val useMiles: Boolean = false,
)

object Countries {
    val all: List<Country> = listOf(
        // Europe
        Country("DE", "Germany", "🇩🇪", "EUR", "€", 2, 0.40, 0.52, 0.66, 1.75),
        Country("FR", "France", "🇫🇷", "EUR", "€", 2, 0.21, 0.40, 0.55, 1.80),
        Country("GB", "United Kingdom", "🇬🇧", "GBP", "£", 2, 0.265, 0.52, 0.79, 1.37, useMiles = true),
        Country("IT", "Italy", "🇮🇹", "EUR", "€", 2, 0.30, 0.60, 0.75, 1.80),
        Country("ES", "Spain", "🇪🇸", "EUR", "€", 2, 0.19, 0.40, 0.55, 1.55),
        Country("NL", "Netherlands", "🇳🇱", "EUR", "€", 2, 0.30, 0.45, 0.62, 2.00),
        Country("BE", "Belgium", "🇧🇪", "EUR", "€", 2, 0.33, 0.50, 0.69, 1.70),
        Country("AT", "Austria", "🇦🇹", "EUR", "€", 2, 0.27, 0.55, 0.69, 1.60),
        Country("CH", "Switzerland", "🇨🇭", "CHF", "CHF", 2, 0.29, 0.55, 0.70, 1.80),
        Country("NO", "Norway", "🇳🇴", "NOK", "kr", 2, 1.50, 3.50, 5.00, 21.50),
        Country("SE", "Sweden", "🇸🇪", "SEK", "kr", 2, 2.00, 4.00, 6.00, 17.50),
        Country("DK", "Denmark", "🇩🇰", "DKK", "kr", 2, 2.50, 4.00, 5.50, 13.50),
        Country("PL", "Poland", "🇵🇱", "PLN", "zł", 2, 1.10, 2.00, 2.90, 6.00),
        Country("PT", "Portugal", "🇵🇹", "EUR", "€", 2, 0.22, 0.35, 0.50, 1.75),
        Country("IE", "Ireland", "🇮🇪", "EUR", "€", 2, 0.36, 0.45, 0.65, 1.75),
        Country("TR", "Türkiye", "🇹🇷", "TRY", "₺", 2, 3.20, 9.50, 12.50, 46.00),
        // Americas
        Country("US", "United States", "🇺🇸", "USD", "$", 2, 0.17, 0.35, 0.48, 0.84, useMiles = true),
        Country("CA", "Canada", "🇨🇦", "CAD", "C$", 2, 0.19, 0.35, 0.55, 1.55),
        Country("MX", "Mexico", "🇲🇽", "MXN", "MX$", 2, 2.50, 6.00, 8.50, 24.00),
        Country("BR", "Brazil", "🇧🇷", "BRL", "R$", 2, 0.85, 1.80, 2.50, 6.30),
        Country("CL", "Chile", "🇨🇱", "CLP", "CLP$", 0, 200.0, 350.0, 450.0, 1250.0),
        // Asia
        Country("CN", "China", "🇨🇳", "CNY", "¥", 2, 0.55, 1.20, 1.60, 7.60),
        Country("JP", "Japan", "🇯🇵", "JPY", "¥", 0, 31.0, 55.0, 85.0, 175.0),
        Country("KR", "South Korea", "🇰🇷", "KRW", "₩", 0, 150.0, 300.0, 347.0, 1650.0),
        Country("IN", "India", "🇮🇳", "INR", "₹", 2, 7.5, 15.0, 22.0, 104.0),
        Country("PK", "Pakistan", "🇵🇰", "PKR", "Rs", 0, 60.0, 70.0, 90.0, 265.0),
        Country("ID", "Indonesia", "🇮🇩", "IDR", "Rp", 0, 1700.0, 2467.0, 3700.0, 10000.0),
        Country("TH", "Thailand", "🇹🇭", "THB", "฿", 2, 4.20, 6.50, 7.50, 36.0),
        Country("VN", "Vietnam", "🇻🇳", "VND", "₫", 0, 3000.0, 3858.0, 3858.0, 21000.0),
        Country("MY", "Malaysia", "🇲🇾", "MYR", "RM", 2, 0.45, 1.00, 1.40, 2.05),
        Country("SG", "Singapore", "🇸🇬", "SGD", "S$", 2, 0.30, 0.55, 0.75, 2.80),
        Country("PH", "Philippines", "🇵🇭", "PHP", "₱", 2, 12.0, 20.0, 25.0, 60.0),
        Country("AE", "United Arab Emirates", "🇦🇪", "AED", "AED", 2, 0.38, 0.70, 1.20, 2.70),
        Country("SA", "Saudi Arabia", "🇸🇦", "SAR", "SAR", 2, 0.18, 0.90, 1.40, 2.33),
        Country("IL", "Israel", "🇮🇱", "ILS", "₪", 2, 0.64, 1.60, 2.20, 7.00),
        // Oceania & Africa
        Country("AU", "Australia", "🇦🇺", "AUD", "A$", 2, 0.32, 0.45, 0.60, 1.90),
        Country("NZ", "New Zealand", "🇳🇿", "NZD", "NZ$", 2, 0.33, 0.50, 0.75, 2.60),
        Country("ZA", "South Africa", "🇿🇦", "ZAR", "R", 2, 3.50, 5.50, 7.50, 22.00),
    ).sortedBy { it.name }

    fun byCode(code: String?): Country? = all.firstOrNull { it.code == code }

    /** Best guess from the phone's region, falling back to the United States. */
    fun default(): Country = byCode(Locale.getDefault().country) ?: byCode("US")!!
}
