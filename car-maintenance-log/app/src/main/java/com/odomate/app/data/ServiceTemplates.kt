package com.odomate.app.data

import com.odomate.app.data.db.ServiceType

object Category {
    const val ENGINE = "Engine"
    const val TIRES_BRAKES = "Tires & brakes"
    const val FLUIDS = "Fluids"
    const val ELECTRICAL = "Electrical & body"
    const val DOCUMENTS = "Documents & checks"
    const val CUSTOM = "Custom"
}

/** Default maintenance schedule seeded for every new vehicle. Users can edit every number. */
object ServiceTemplates {
    private data class Template(
        val name: String,
        val icon: String,
        val category: String,
        val km: Int?,
        val mi: Int?,
        val months: Int?,
        val enabled: Boolean = true,
    )

    private val templates = listOf(
        Template("Engine oil & filter", "oil", Category.ENGINE, 5_000, 3_000, 6),
        Template("Engine air filter", "air_filter", Category.ENGINE, 15_000, 10_000, 24),
        Template("Spark plugs", "spark", Category.ENGINE, 100_000, 60_000, null),
        Template("Tire rotation", "tire", Category.TIRES_BRAKES, 10_000, 6_000, 12),
        Template("Wheel alignment", "alignment", Category.TIRES_BRAKES, 20_000, 12_000, 12),
        Template("Brake pads inspection", "brakes", Category.TIRES_BRAKES, 20_000, 12_000, 12),
        Template("Brake fluid", "drop", Category.FLUIDS, null, null, 24),
        Template("Coolant flush", "coolant", Category.FLUIDS, 60_000, 40_000, 36),
        Template("Transmission fluid", "transmission", Category.FLUIDS, 60_000, 40_000, null),
        Template("Cabin air filter", "cabin_filter", Category.ELECTRICAL, 15_000, 10_000, 12),
        Template("Battery check", "battery", Category.ELECTRICAL, null, null, 12),
        Template("Wiper blades", "wiper", Category.ELECTRICAL, null, null, 12),
        Template("Insurance renewal", "insurance", Category.DOCUMENTS, null, null, 12),
        Template("Registration / road tax", "document", Category.DOCUMENTS, null, null, 12),
        Template("Annual inspection", "inspection", Category.DOCUMENTS, null, null, 12, enabled = false),
    )

    fun defaultReminderDistance(miles: Boolean): Int = if (miles) 300 else 500

    fun forVehicle(vehicleId: Long, miles: Boolean): List<ServiceType> =
        templates.mapIndexed { index, t ->
            ServiceType(
                vehicleId = vehicleId,
                name = t.name,
                iconKey = t.icon,
                category = t.category,
                intervalDistance = if (miles) t.mi else t.km,
                intervalMonths = t.months,
                remindDistanceBefore = defaultReminderDistance(miles),
                remindDaysBefore = if (t.category == Category.DOCUMENTS) 14 else 7,
                enabled = t.enabled,
                sortOrder = index,
            )
        }

    /** Icons a user can choose for a custom service. */
    val customIconChoices = listOf(
        "wrench", "oil", "tire", "brakes", "battery", "drop", "coolant", "air_filter",
        "spark", "wiper", "document", "insurance", "inspection", "wash", "transmission", "alignment",
    )
}
