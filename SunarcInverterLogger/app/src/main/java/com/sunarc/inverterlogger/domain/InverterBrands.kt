package com.sunarc.inverterlogger.domain

/** Values the scanner can pull from an inverter display or a monitoring-app screenshot. */
enum class Field(val label: String, val unitKind: UnitKind) {
    ENERGY_TODAY("Energy today", UnitKind.ENERGY),
    ENERGY_TOTAL("Lifetime total", UnitKind.ENERGY),
    ENERGY_MONTH("This month", UnitKind.ENERGY),
    ENERGY_YEAR("This year", UnitKind.ENERGY),
    POWER_NOW("Live power", UnitKind.POWER),
    PV_VOLTAGE("PV voltage", UnitKind.VOLTAGE),
    GRID_VOLTAGE("Grid voltage", UnitKind.VOLTAGE),
    FREQUENCY("Grid frequency", UnitKind.FREQUENCY),
    TEMPERATURE("Temperature", UnitKind.TEMPERATURE),
    FAULT_CODE("Fault code", UnitKind.NONE)
}

enum class UnitKind { ENERGY, POWER, VOLTAGE, FREQUENCY, TEMPERATURE, NONE }

/** What the user usually photographs for this brand. */
enum class ScanTarget(val label: String) {
    LCD_DISPLAY("Inverter display"),
    APP_SCREENSHOT("Monitoring app screenshot"),
    BOTH("Display or app screenshot")
}

data class InverterBrand(
    val id: String,
    val name: String,
    val regions: Set<Region>,
    val models: List<String>,
    val scanTarget: ScanTarget,
    /** True when the inverter commonly exposes SunSpec Modbus TCP on the home network. */
    val sunSpec: Boolean,
    /** Default Modbus unit id used by this brand, when known. */
    val modbusUnitId: Int = 1,
    /** Extra brand-specific labels, added on top of [FieldLabels.common]. */
    val extraLabels: Map<Field, List<String>> = emptyMap(),
    val tip: String
) {
    val initials: String
        get() {
            val words = name.split(' ', '-').filter { it.isNotBlank() }
            return if (words.size >= 2) "${words[0][0]}${words[1][0]}".uppercase()
            else name.take(2).uppercase()
        }
}

object InverterBrands {
    private val NA = setOf(Region.NORTH_AMERICA)
    private val EU = setOf(Region.EUROPE)
    private val BOTH = setOf(Region.NORTH_AMERICA, Region.EUROPE)

    val all: List<InverterBrand> = listOf(
        InverterBrand("enphase", "Enphase", BOTH, listOf("IQ8 microinverters", "IQ7 microinverters", "IQ Gateway"), ScanTarget.APP_SCREENSHOT, false,
            tip = "Microinverters have no screen. Take a screenshot of the Enlighten app and scan it from your gallery."),
        InverterBrand("solaredge", "SolarEdge", BOTH, listOf("Home Wave", "HD-Wave", "Energy Hub", "SE3000–SE11400"), ScanTarget.BOTH, true,
            extraLabels = mapOf(Field.ENERGY_TOTAL to listOf("e-tot", "energy produced")),
            tip = "Older models show values on a small LCD after pressing the toggle. Newer ones are read from the mySolarEdge app."),
        InverterBrand("tesla", "Tesla", NA, listOf("Tesla Solar Inverter", "Powerwall 3"), ScanTarget.APP_SCREENSHOT, false,
            tip = "Scan a screenshot of the Energy view in the Tesla app."),
        InverterBrand("sma", "SMA", BOTH, listOf("Sunny Boy", "Sunny Tripower", "Sunny Boy Storage", "Sunny Island"), ScanTarget.BOTH, true, modbusUnitId = 3,
            extraLabels = mapOf(Field.ENERGY_TODAY to listOf("e-today", "e-heute", "day yield"), Field.ENERGY_TOTAL to listOf("e-total", "e-ges"), Field.POWER_NOW to listOf("pac")),
            tip = "Tap the display once to wake it, then hold the phone straight in front of it."),
        InverterBrand("fronius", "Fronius", BOTH, listOf("Primo", "Symo", "Primo GEN24", "Symo GEN24", "Galvo"), ScanTarget.BOTH, true,
            extraLabels = mapOf(Field.ENERGY_TODAY to listOf("day energy"), Field.ENERGY_TOTAL to listOf("total energy"), Field.FAULT_CODE to listOf("state")),
            tip = "On Primo/Symo press the up/down keys to show NOW, DAY and TOTAL. GEN24 values come from the Solar.web app."),
        InverterBrand("generac", "Generac PWRcell", NA, listOf("PWRcell inverter"), ScanTarget.BOTH, false,
            tip = "Scan the inverter display or a screenshot of the PWRview app."),
        InverterBrand("solark", "Sol-Ark", NA, listOf("Sol-Ark 12K", "Sol-Ark 15K", "Sol-Ark 5K"), ScanTarget.LCD_DISPLAY, false,
            extraLabels = mapOf(Field.ENERGY_TODAY to listOf("solar day", "pv today")),
            tip = "Open the main touchscreen page that shows the daily solar total."),
        InverterBrand("apsystems", "APsystems", BOTH, listOf("DS3", "QS1", "ECU-R"), ScanTarget.APP_SCREENSHOT, false,
            tip = "Scan a screenshot of the EMA app."),
        InverterBrand("hoymiles", "Hoymiles", BOTH, listOf("HMS microinverters", "DTU"), ScanTarget.APP_SCREENSHOT, false,
            tip = "Scan a screenshot of the S-Miles app."),
        InverterBrand("schneider", "Schneider Electric", NA, listOf("Conext XW Pro", "Conext SW"), ScanTarget.LCD_DISPLAY, false,
            tip = "Use the Conext display's energy page."),
        InverterBrand("huawei", "Huawei", EU, listOf("SUN2000 L1", "SUN2000 M1", "LUNA2000"), ScanTarget.APP_SCREENSHOT, false,
            tip = "SUN2000 has only status LEDs. Scan a screenshot of the FusionSolar app."),
        InverterBrand("growatt", "Growatt", BOTH, listOf("MIN TL-X", "MOD TL3-X", "SPH", "MIC"), ScanTarget.BOTH, false,
            extraLabels = mapOf(Field.ENERGY_TODAY to listOf("e_today", "today"), Field.ENERGY_TOTAL to listOf("e_total")),
            tip = "Knock lightly on the display (or press the button) to cycle to E-Today."),
        InverterBrand("goodwe", "GoodWe", EU, listOf("DNS", "SDT", "ET hybrid", "EH hybrid"), ScanTarget.BOTH, false,
            extraLabels = mapOf(Field.ENERGY_TODAY to listOf("e-day")),
            tip = "Press the button to scroll the LCD to E-Day and E-Total."),
        InverterBrand("sungrow", "Sungrow", EU, listOf("SG RS", "SH RS hybrid", "SH RT hybrid"), ScanTarget.BOTH, true,
            tip = "Many models have LEDs only. Scan the iSolarCloud app if there is no display."),
        InverterBrand("kostal", "Kostal", EU, listOf("PIKO IQ", "PLENTICORE plus", "PIKO MP plus"), ScanTarget.BOTH, true, modbusUnitId = 71,
            extraLabels = mapOf(Field.ENERGY_TODAY to listOf("tagesertrag"), Field.ENERGY_TOTAL to listOf("gesamtertrag")),
            tip = "Use the yield page of the display."),
        InverterBrand("victron", "Victron Energy", EU, listOf("MultiPlus-II", "Quattro", "SmartSolar MPPT", "Cerbo GX"), ScanTarget.BOTH, false,
            extraLabels = mapOf(Field.ENERGY_TODAY to listOf("yield today"), Field.POWER_NOW to listOf("pv power", "pv charger")),
            tip = "Scan the GX touch screen or a VictronConnect / VRM screenshot."),
        InverterBrand("solax", "SolaX", EU, listOf("X1 Boost", "X1 Hybrid", "X3 Hybrid"), ScanTarget.BOTH, false,
            tip = "Scroll the LCD to the energy page."),
        InverterBrand("solis", "Solis", BOTH, listOf("S6 mini", "S6 single phase", "RHI hybrid"), ScanTarget.LCD_DISPLAY, false,
            extraLabels = mapOf(Field.ENERGY_TODAY to listOf("today energy"), Field.ENERGY_TOTAL to listOf("total energy")),
            tip = "Press ESC then scroll to Today Energy."),
        InverterBrand("deye", "Deye", EU, listOf("SUN-SG hybrid", "SUN-G string", "Microinverter"), ScanTarget.LCD_DISPLAY, false,
            extraLabels = mapOf(Field.ENERGY_TODAY to listOf("day"), Field.ENERGY_TOTAL to listOf("total")),
            tip = "Open the system flow page on the touchscreen."),
        InverterBrand("sofar", "Sofar Solar", EU, listOf("KTLX-G3", "HYD-ES"), ScanTarget.LCD_DISPLAY, false,
            tip = "Scroll the LCD to Today."),
        InverterBrand("fimer", "FIMER / ABB", BOTH, listOf("UNO-DM-PLUS", "PVI-3.0/3.6/4.2", "PVS"), ScanTarget.LCD_DISPLAY, true,
            extraLabels = mapOf(Field.ENERGY_TOTAL to listOf("e-tot", "lifetime")),
            tip = "Use the display's statistics page."),
        InverterBrand("delta", "Delta", BOTH, listOf("M-series", "H-series"), ScanTarget.LCD_DISPLAY, false,
            tip = "Use the display's energy page."),
        InverterBrand("other", "Other / generic", BOTH, listOf("Any inverter with a display"), ScanTarget.BOTH, false,
            tip = "Generic reading works for most displays in English, German, Dutch, French, Spanish, Italian and Polish.")
    )

    fun byId(id: String?): InverterBrand = all.firstOrNull { it.id == id } ?: all.last()

    fun forRegion(region: Region): List<InverterBrand> = all.filter { region in it.regions }

    /** Brand names to look for in OCR text so the scanner can auto-detect the brand. */
    val detectionKeywords: Map<String, List<String>> = mapOf(
        "sma" to listOf("sunny boy", "sunny tripower", "sma"),
        "fronius" to listOf("fronius", "primo", "symo"),
        "solaredge" to listOf("solaredge", "solar edge"),
        "enphase" to listOf("enphase", "enlighten"),
        "tesla" to listOf("tesla", "powerwall"),
        "huawei" to listOf("huawei", "fusionsolar", "sun2000"),
        "growatt" to listOf("growatt", "shinephone"),
        "goodwe" to listOf("goodwe", "sems"),
        "sungrow" to listOf("sungrow", "isolarcloud"),
        "kostal" to listOf("kostal", "piko", "plenticore"),
        "victron" to listOf("victron", "multiplus", "smartsolar", "vrm"),
        "solax" to listOf("solax"),
        "solis" to listOf("solis", "ginlong"),
        "deye" to listOf("deye"),
        "sofar" to listOf("sofar"),
        "fimer" to listOf("fimer", "abb", "power-one", "aurora"),
        "generac" to listOf("generac", "pwrcell", "pwrview"),
        "solark" to listOf("sol-ark", "solark"),
        "apsystems" to listOf("apsystems", "ema app"),
        "hoymiles" to listOf("hoymiles", "s-miles"),
        "schneider" to listOf("schneider", "conext"),
        "delta" to listOf("delta electronics")
    )

    fun detect(text: String): InverterBrand? {
        val lower = text.lowercase()
        return detectionKeywords.entries
            .firstOrNull { (_, words) -> words.any { w -> Regex("(^|[^a-z])" + Regex.escape(w) + "([^a-z]|$)").containsMatchIn(lower) } }
            ?.let { byId(it.key) }
    }
}

/** Display labels in the languages used across North America and Europe. Matching is case-insensitive. */
object FieldLabels {
    val common: Map<Field, List<String>> = mapOf(
        Field.ENERGY_TODAY to listOf(
            "e-today", "etoday", "e today", "e_today", "e-day", "eday", "e-daily", "today energy", "energy today", "today's energy",
            "todays energy", "today yield", "yield today", "today's yield", "daily yield", "daily energy", "day energy", "daily production",
            "produced today", "production today", "solar today", "pv today", "today", "day",
            "tagesertrag", "ertrag heute", "energie heute", "heute", "e-heute",
            "opbrengst vandaag", "dagopbrengst", "vandaag",
            "e-jour", "énergie du jour", "energie du jour", "production du jour", "aujourd'hui", "aujourd hui",
            "energía hoy", "energia hoy", "energía diaria", "energia diaria", "producción hoy", "hoy",
            "energia oggi", "produzione oggi", "produzione giornaliera", "oggi",
            "energia dzisiaj", "produkcja dzisiaj", "dzisiaj", "dzienna",
            "energia hoje", "hoje", "dagens", "i dag", "idag", "tänään"
        ),
        Field.ENERGY_TOTAL to listOf(
            "e-total", "etotal", "e total", "e_total", "e-tot", "etot", "total energy", "energy total", "total yield", "lifetime energy",
            "lifetime production", "lifetime", "total production", "cumulative", "since installation", "all time", "total",
            "gesamtertrag", "ertrag gesamt", "gesamt", "e-ges",
            "totale opbrengst", "totaal",
            "énergie totale", "energie totale", "e-totale", "production totale",
            "energía total", "energia total", "producción total",
            "energia totale", "produzione totale", "totale",
            "energia całkowita", "energia calkowita", "całkowita", "calkowita",
            "energia total", "totalt", "yhteensä"
        ),
        Field.ENERGY_MONTH to listOf(
            "e-month", "this month", "month energy", "monthly", "month", "monat", "monatsertrag", "maand", "mois", "mes", "mese", "miesiąc", "miesiac"
        ),
        Field.ENERGY_YEAR to listOf(
            "e-year", "this year", "year energy", "yearly", "year", "jahr", "jahresertrag", "jaar", "année", "annee", "año", "ano", "anno", "rok"
        ),
        Field.POWER_NOW to listOf(
            "pac", "p-ac", "p ac", "p_ac", "ac power", "output power", "current power", "power now", "live power", "real-time power",
            "realtime power", "now", "pv power", "solar power", "production", "power", "pout", "p-out",
            "leistung", "aktuelle leistung", "momentanleistung", "vermogen", "huidig vermogen",
            "puissance", "puissance actuelle", "potencia", "potencia actual", "potenza", "potenza attuale", "moc", "moc chwilowa", "effekt"
        ),
        Field.PV_VOLTAGE to listOf("vpv", "upv", "pv voltage", "dc voltage", "vdc", "udc", "pv1 voltage", "u-pv", "v-pv", "dc-spannung"),
        Field.GRID_VOLTAGE to listOf("vac", "uac", "u-ac", "v-ac", "grid voltage", "ac voltage", "vgrid", "netzspannung", "tension réseau", "tensión red"),
        Field.FREQUENCY to listOf("fac", "f-ac", "frequency", "grid frequency", "freq", "netzfrequenz", "fréquence", "frecuencia", "frequenza"),
        Field.TEMPERATURE to listOf("temperature", "temp", "temperatur", "température", "temperatura", "heatsink"),
        Field.FAULT_CODE to listOf("error", "fault", "event", "alarm", "warning", "fehler", "störung", "storing", "erreur", "défaut", "fallo", "errore", "błąd", "blad", "err")
    )

    fun forBrand(brand: InverterBrand?): Map<Field, List<String>> {
        if (brand == null) return common
        return Field.entries.associateWith { f ->
            ((brand.extraLabels[f] ?: emptyList()) + (common[f] ?: emptyList())).distinct()
        }
    }
}
