package com.sunarc.inverterlogger.domain

enum class Severity(val label: String) {
    INFO("Info"),
    CHECK("Check"),
    INSTALLER("Call installer")
}

enum class FaultCategory(val label: String) {
    GRID("Grid"),
    DC("Panels / DC"),
    INSULATION("Insulation"),
    TEMPERATURE("Cooling"),
    COMMUNICATION("Communication"),
    BATTERY("Battery"),
    HARDWARE("Inverter hardware"),
    OTHER("Other")
}

data class FaultInfo(
    val code: String,
    val title: String,
    val category: FaultCategory,
    val severity: Severity,
    val meaning: String,
    val action: String,
    /** Brand id the code is specific to, or null for a general description. */
    val brandId: String? = null
)

/**
 * Offline fault help. Brand-specific entries cover well-known codes; everything else is decoded
 * from the words on the screen ("isolation", "grid", "temperature"…), which works across brands.
 * Every result tells the user to confirm against their inverter manual.
 */
object FaultLibrary {
    private val general = listOf(
        FaultInfo("GRID-V", "Grid voltage out of range", FaultCategory.GRID, Severity.CHECK,
            "The voltage from the grid went above or below the limit, so the inverter paused to protect itself. It is common around midday when many homes export at once.",
            "Note when it happens. If it repeats on most sunny days, ask your installer or grid operator to check the voltage at your meter."),
        FaultInfo("GRID-F", "Grid frequency out of range", FaultCategory.GRID, Severity.INFO,
            "The grid frequency moved outside the allowed band. The inverter reconnects on its own once it is normal.",
            "No action needed if it clears within minutes. Repeated events: tell your installer."),
        FaultInfo("GRID-LOSS", "No grid / grid loss", FaultCategory.GRID, Severity.INFO,
            "The inverter cannot see the grid, usually during a power cut or when the AC breaker is off.",
            "Check that the AC breaker or isolator for the inverter is on. It restarts by itself when the grid is back."),
        FaultInfo("ISO", "Insulation resistance low", FaultCategory.INSULATION, Severity.INSTALLER,
            "There may be moisture or a damaged cable on the panel (DC) side. It often shows on wet mornings and clears as things dry.",
            "Do not touch the DC cables or connectors. If it is still there by midday or comes back often, call your installer."),
        FaultInfo("GFDI", "Earth / residual current fault", FaultCategory.INSULATION, Severity.INSTALLER,
            "The inverter detected current leaking to earth.",
            "Do not touch the system. Switch it off only as the manual describes and call your installer."),
        FaultInfo("ARC", "Arc fault detected", FaultCategory.DC, Severity.INSTALLER,
            "An electrical arc was detected on the DC side, which can be a fire risk.",
            "Leave the inverter off, keep away from the DC wiring and call your installer before restarting."),
        FaultInfo("PV-OV", "PV voltage too high", FaultCategory.DC, Severity.INSTALLER,
            "The panel voltage is above what the inverter accepts, typically on cold, very bright days with a string that is too long.",
            "Tell your installer the time and temperature when it happened."),
        FaultInfo("PV-LOW", "Low or no PV input", FaultCategory.DC, Severity.INFO,
            "Not enough power from the panels: night, snow, heavy shade or a tripped DC isolator.",
            "Check the DC isolator. If it shows on a sunny day, a string may be disconnected."),
        FaultInfo("TEMP", "Over-temperature / derating", FaultCategory.TEMPERATURE, Severity.CHECK,
            "The inverter is hot and is reducing power to protect itself.",
            "Clear dust from the vents and keep about 30 cm of free space around it. Avoid direct afternoon sun on the unit if you can."),
        FaultInfo("FAN", "Fan fault", FaultCategory.TEMPERATURE, Severity.CHECK,
            "A cooling fan is blocked or not working.",
            "Check the fan opening for dust or insects. If the warning stays, contact your installer."),
        FaultInfo("COMM", "Communication / monitoring lost", FaultCategory.COMMUNICATION, Severity.INFO,
            "The inverter cannot reach its Wi-Fi dongle, meter or the cloud. Production is usually not affected.",
            "Restart your router, check the Wi-Fi stick or meter cable. Your photo log in this app keeps working."),
        FaultInfo("BATT", "Battery fault", FaultCategory.BATTERY, Severity.CHECK,
            "The battery reports a problem: temperature, communication or a protection stop.",
            "Check that the battery is on and not too cold or hot. If it does not clear, contact your installer."),
        FaultInfo("RELAY", "Relay / self-test failed", FaultCategory.HARDWARE, Severity.INSTALLER,
            "An internal check before connecting to the grid failed.",
            "Wait for one retry. If it repeats, the inverter may need service."),
        FaultInfo("UNKNOWN", "Unknown code", FaultCategory.OTHER, Severity.CHECK,
            "This code is not in the offline library yet.",
            "Look it up in your inverter manual, and save the photo so you can show your installer.")
    )

    private val brandCodes = listOf(
        FaultInfo("102", "AC voltage too high", FaultCategory.GRID, Severity.CHECK, general[0].meaning, general[0].action, "fronius"),
        FaultInfo("103", "AC voltage too low", FaultCategory.GRID, Severity.CHECK, general[0].meaning, general[0].action, "fronius"),
        FaultInfo("105", "AC frequency too high", FaultCategory.GRID, Severity.INFO, general[1].meaning, general[1].action, "fronius"),
        FaultInfo("106", "AC frequency too low", FaultCategory.GRID, Severity.INFO, general[1].meaning, general[1].action, "fronius"),
        FaultInfo("107", "No AC grid detected", FaultCategory.GRID, Severity.INFO, general[2].meaning, general[2].action, "fronius"),
        FaultInfo("2032", "Grid loss", FaultCategory.GRID, Severity.INFO, general[2].meaning, general[2].action, "huawei"),
        FaultInfo("2033", "Grid undervoltage", FaultCategory.GRID, Severity.CHECK, general[0].meaning, general[0].action, "huawei"),
        FaultInfo("2034", "Grid overvoltage", FaultCategory.GRID, Severity.CHECK, general[0].meaning, general[0].action, "huawei"),
        FaultInfo("2062", "Low insulation resistance", FaultCategory.INSULATION, Severity.INSTALLER, general[3].meaning, general[3].action, "huawei"),
        FaultInfo("3501", "Insulation failure", FaultCategory.INSULATION, Severity.INSTALLER, general[3].meaning, general[3].action, "sma")
    )

    private val keywordRules: List<Pair<Regex, String>> = listOf(
        Regex("(?i)arc") to "ARC",
        Regex("(?i)(iso|insulat|isolat|riso|pv\\s*isolation)") to "ISO",
        Regex("(?i)(gfci|gfdi|residual|leak|rcd|earth|ground fault)") to "GFDI",
        Regex("(?i)(grid\\s*(loss|lost|fail|absent)|no\\s*(grid|utility|ac)|netzausfall|utility loss)") to "GRID-LOSS",
        Regex("(?i)(freq|fac|hz)") to "GRID-F",
        Regex("(?i)(grid|ac|vac|netz|réseau|red)\\s*(volt|over|under|high|low|spannung|tension)|(over|under)\\s*voltage") to "GRID-V",
        Regex("(?i)(pv|dc|string)\\s*(over|high)|over\\s*voltage\\s*pv") to "PV-OV",
        Regex("(?i)(pv|dc|string)\\s*(low|under|no input|missing)|no\\s*pv|low\\s*sun") to "PV-LOW",
        Regex("(?i)fan") to "FAN",
        Regex("(?i)(temp|heat|derat|übertemperatur)") to "TEMP",
        Regex("(?i)(comm|wifi|wi-fi|lan|network|meter|dongle|offline|rs485|can bus)") to "COMM",
        Regex("(?i)(batt|bms|soc)") to "BATT",
        Regex("(?i)(relay|self[- ]?test|selftest)") to "RELAY"
    )

    val allGeneral: List<FaultInfo> get() = general.filter { it.code != "UNKNOWN" }

    fun brandEntries(brandId: String?): List<FaultInfo> = brandCodes.filter { brandId == null || it.brandId == brandId }

    fun search(query: String, brandId: String?): List<FaultInfo> {
        val q = query.trim()
        if (q.isEmpty()) return brandEntries(brandId) + allGeneral
        val lower = q.lowercase()
        val byText = (brandCodes + allGeneral).filter {
            it.code.lowercase().contains(lower) || it.title.lowercase().contains(lower) ||
                it.category.label.lowercase().contains(lower)
        }
        val decoded = decode(q, q, brandId)
        return (listOfNotNull(decoded.takeIf { it.code != "UNKNOWN" }) + byText).distinctBy { it.code + it.brandId }
    }

    /** Best explanation for a code read from a screen plus the surrounding text. */
    fun decode(code: String?, screenText: String, brandId: String?): FaultInfo {
        val digits = code?.filter { it.isDigit() }.orEmpty()
        if (digits.isNotEmpty()) {
            brandCodes.firstOrNull { it.brandId == brandId && it.code == digits }?.let { return it }
        }
        for ((rule, id) in keywordRules) {
            if (rule.containsMatchIn(screenText)) {
                return general.first { it.code == id }.copy(code = code ?: id)
            }
        }
        return general.first { it.code == "UNKNOWN" }.copy(code = code ?: "?")
    }

    const val DISCLAIMER = "General guidance only. Always check your inverter's manual, and leave electrical work to a qualified installer."
}
