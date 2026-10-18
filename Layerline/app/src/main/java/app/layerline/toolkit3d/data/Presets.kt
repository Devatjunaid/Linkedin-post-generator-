package app.layerline.toolkit3d.data

/**
 * Starting values only. Electricity tariffs, filament prices and exchange rates are rough
 * 2025-26 averages; every one of them can be overridden in Settings or per quote.
 */
data class Region(
    val code: String,
    val name: String,
    val currency: String,
    val symbol: String,
    val decimals: Int,
    val kwhRate: Double,
    val filamentPerKg: Double,
    val labourPerHour: Double,
    val perUsd: Double,
)

data class MachinePreset(
    val brand: String,
    val model: String,
    val avgWatts: Double,
    val priceUsd: Double,
    val lifeHours: Double,
    val kind: MachineKind,
)

enum class MachineKind { CoreXY, BedSlinger, Enclosed }

data class Material(
    val name: String,
    val density: Double,
    val priceFactor: Double,
    val nozzle: String,
    val bed: String,
)

object Presets {

    val regions: List<Region> = listOf(
        Region("PK", "Pakistan", "PKR", "Rs ", 0, 62.0, 4800.0, 800.0, 281.0),
        Region("IN", "India", "INR", "₹", 0, 8.5, 1500.0, 400.0, 84.0),
        Region("BD", "Bangladesh", "BDT", "৳", 0, 9.0, 2200.0, 350.0, 120.0),
        Region("US", "United States", "USD", "$", 2, 0.18, 20.0, 20.0, 1.0),
        Region("CA", "Canada", "CAD", "C$", 2, 0.17, 30.0, 25.0, 1.37),
        Region("GB", "United Kingdom", "GBP", "£", 2, 0.25, 19.0, 15.0, 0.78),
        Region("DE", "Germany", "EUR", "€", 2, 0.39, 22.0, 20.0, 0.92),
        Region("FR", "France", "EUR", "€", 2, 0.25, 22.0, 20.0, 0.92),
        Region("IT", "Italy", "EUR", "€", 2, 0.33, 22.0, 18.0, 0.92),
        Region("ES", "Spain", "EUR", "€", 2, 0.22, 21.0, 16.0, 0.92),
        Region("NL", "Netherlands", "EUR", "€", 2, 0.33, 22.0, 22.0, 0.92),
        Region("AE", "UAE", "AED", "AED ", 2, 0.38, 85.0, 50.0, 3.67),
        Region("SA", "Saudi Arabia", "SAR", "SAR ", 2, 0.18, 80.0, 45.0, 3.75),
        Region("TR", "Türkiye", "TRY", "₺", 2, 2.6, 700.0, 300.0, 41.0),
        Region("AU", "Australia", "AUD", "A$", 2, 0.33, 32.0, 30.0, 1.53),
        Region("JP", "Japan", "JPY", "¥", 0, 31.0, 3000.0, 1500.0, 150.0),
        Region("CN", "China", "CNY", "¥", 2, 0.55, 70.0, 60.0, 7.2),
        Region("BR", "Brazil", "BRL", "R$", 2, 0.80, 120.0, 40.0, 5.6),
        Region("NG", "Nigeria", "NGN", "₦", 0, 210.0, 30000.0, 3000.0, 1550.0),
        Region("ZA", "South Africa", "ZAR", "R ", 2, 3.4, 380.0, 150.0, 18.0),
    )

    val machines: List<MachinePreset> = listOf(
        MachinePreset("Bambu Lab", "A1 mini", 70.0, 249.0, 4000.0, MachineKind.BedSlinger),
        MachinePreset("Bambu Lab", "A1", 95.0, 399.0, 5000.0, MachineKind.BedSlinger),
        MachinePreset("Bambu Lab", "P1S", 110.0, 699.0, 6000.0, MachineKind.Enclosed),
        MachinePreset("Bambu Lab", "X1 Carbon", 130.0, 1199.0, 7000.0, MachineKind.Enclosed),
        MachinePreset("Bambu Lab", "H2D", 200.0, 1899.0, 8000.0, MachineKind.Enclosed),
        MachinePreset("Prusa", "MINI+", 60.0, 459.0, 6000.0, MachineKind.BedSlinger),
        MachinePreset("Prusa", "MK4S", 80.0, 1099.0, 8000.0, MachineKind.BedSlinger),
        MachinePreset("Prusa", "CORE One", 100.0, 1199.0, 8000.0, MachineKind.Enclosed),
        MachinePreset("Creality", "Ender-3 V3 SE", 100.0, 199.0, 4000.0, MachineKind.BedSlinger),
        MachinePreset("Creality", "Ender-3 V3 KE", 110.0, 279.0, 4000.0, MachineKind.BedSlinger),
        MachinePreset("Creality", "K1C", 120.0, 459.0, 5000.0, MachineKind.Enclosed),
        MachinePreset("Creality", "K2 Plus", 200.0, 1499.0, 6000.0, MachineKind.Enclosed),
        MachinePreset("Elegoo", "Neptune 4 Pro", 120.0, 299.0, 4000.0, MachineKind.BedSlinger),
        MachinePreset("Elegoo", "Centauri Carbon", 120.0, 299.0, 5000.0, MachineKind.Enclosed),
        MachinePreset("Anycubic", "Kobra 3", 120.0, 349.0, 4000.0, MachineKind.BedSlinger),
        MachinePreset("Sovol", "SV06", 100.0, 239.0, 4000.0, MachineKind.BedSlinger),
        MachinePreset("Qidi", "Q1 Pro", 150.0, 459.0, 5000.0, MachineKind.Enclosed),
        MachinePreset("Voron", "2.4 (DIY)", 200.0, 1300.0, 8000.0, MachineKind.CoreXY),
        MachinePreset("Custom", "Other printer", 100.0, 300.0, 5000.0, MachineKind.BedSlinger),
    )

    val materials: List<Material> = listOf(
        Material("PLA", 1.24, 1.0, "190-220 °C", "55-65 °C"),
        Material("PLA+", 1.24, 1.1, "200-230 °C", "55-65 °C"),
        Material("PETG", 1.27, 1.1, "230-250 °C", "70-85 °C"),
        Material("ABS", 1.04, 1.05, "230-260 °C", "95-110 °C"),
        Material("ASA", 1.07, 1.25, "240-260 °C", "95-110 °C"),
        Material("TPU", 1.21, 1.6, "210-230 °C", "40-60 °C"),
        Material("PA", 1.14, 2.2, "250-290 °C", "70-90 °C"),
        Material("PC", 1.20, 2.0, "260-300 °C", "100-120 °C"),
        Material("PLA-CF", 1.29, 1.8, "210-240 °C", "55-65 °C"),
        Material("PETG-CF", 1.30, 1.9, "240-260 °C", "70-85 °C"),
    )

    /** Typical empty-spool weights for the weigh tool. */
    val tares: List<Pair<String, Double>> = listOf(
        "Plastic 1 kg" to 230.0,
        "Cardboard" to 145.0,
        "Reusable" to 250.0,
        "Masterspool" to 175.0,
    )

    val failReasons = listOf(
        "Spaghetti", "Bed adhesion", "Warping", "Clog", "Layer shift",
        "Stringing", "Under-extrusion", "Power loss", "Filament ran out", "Other",
    )

    /** Spool colours offered in the editor (ARGB). */
    val spoolColors: List<Long> = listOf(
        0xFFFF7A3D, 0xFFEDEDE6, 0xFF6A7075, 0xFF2B2E31, 0xFF5B7BF0, 0xFF3FB6E8,
        0xFF9AAA4F, 0xFF3DBE7A, 0xFFD9434E, 0xFFE85A9B, 0xFF9B6BE8, 0xFFD9B44A,
        0xFFF5D547, 0xFFC8F03C, 0xFF8D6E52, 0xFFB8C2C9,
    )

    fun region(code: String): Region = regions.firstOrNull { it.code == code } ?: regions.first()

    fun material(name: String): Material = materials.firstOrNull { it.name == name } ?: materials.first()

    /** Maintenance jobs added to a new printer, as (name, interval in print hours). */
    fun defaultMaintenance(kind: MachineKind): List<Pair<String, Double>> = buildList {
        add("Nozzle check / swap" to 500.0)
        add("Clean build plate" to 25.0)
        add("Belt tension check" to 200.0)
        if (kind == MachineKind.BedSlinger) add("Lube Z / linear rods" to 150.0) else add("Clean & lube rods" to 100.0)
        if (kind == MachineKind.Enclosed) add("Replace carbon filter" to 1000.0) else add("Bed level / Z-offset" to 50.0)
    }
}
