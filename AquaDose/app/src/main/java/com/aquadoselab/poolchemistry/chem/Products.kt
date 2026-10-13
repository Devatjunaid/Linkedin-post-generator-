package com.aquadoselab.poolchemistry.chem

enum class ProductForm { LIQUID, SOLID }

/** A chlorine source. [strength] is % available chlorine (liquids: "trade %", g per 100 mL). */
data class ChlorineProduct(
    val id: String,
    val name: String,
    val form: ProductForm,
    val strength: Double,
    /** ppm of CYA added for every 1 ppm FC (stabilized chlorine). */
    val cyaPerPpm: Double = 0.0,
    /** ppm of calcium hardness added for every 1 ppm FC (cal-hypo). */
    val chPerPpm: Double = 0.0,
    val tip: String
)

data class AcidProduct(
    val id: String,
    val name: String,
    val form: ProductForm,
    /** Equivalents of acid per mL (liquids) or per gram (solids). */
    val eqPerUnit: Double,
    val tip: String
)

data class SimpleProduct(val id: String, val name: String, val tip: String)

object Products {

    val chlorine = listOf(
        ChlorineProduct(
            "liquid_10", "Liquid chlorine 10%", ProductForm.LIQUID, 10.0,
            tip = "Pour slowly in front of a return jet with the pump running."
        ),
        ChlorineProduct(
            "liquid_12_5", "Liquid chlorine 12.5%", ProductForm.LIQUID, 12.5,
            tip = "Pour slowly in front of a return jet with the pump running."
        ),
        ChlorineProduct(
            "bleach_6", "Plain bleach 6%", ProductForm.LIQUID, 6.0,
            tip = "Use plain, unscented bleach only — no \"splash-less\" or scented types."
        ),
        ChlorineProduct(
            "bleach_8_25", "Plain bleach 8.25%", ProductForm.LIQUID, 8.25,
            tip = "Use plain, unscented bleach only — no \"splash-less\" or scented types."
        ),
        ChlorineProduct(
            "calhypo_65", "Cal-hypo 65% (shock)", ProductForm.SOLID, 65.0, chPerPpm = 0.7,
            tip = "Pre-dissolve in a bucket of pool water, let it settle, pour the clear liquid in. Raises calcium hardness."
        ),
        ChlorineProduct(
            "calhypo_73", "Cal-hypo 73% (shock)", ProductForm.SOLID, 73.0, chPerPpm = 0.7,
            tip = "Pre-dissolve in a bucket of pool water, let it settle, pour the clear liquid in. Raises calcium hardness."
        ),
        ChlorineProduct(
            "dichlor_56", "Dichlor 56% granules", ProductForm.SOLID, 56.0, cyaPerPpm = 0.9,
            tip = "Broadcast over the water or pre-dissolve. Adds stabilizer (CYA) every time you use it."
        ),
        ChlorineProduct(
            "trichlor_90", "Trichlor 90% tablets", ProductForm.SOLID, 90.0, cyaPerPpm = 0.6,
            tip = "Slow-dissolving: use a floater or feeder, never the skimmer. Adds CYA and lowers pH over time."
        )
    )

    // 31.45% HCl: density 1.16 g/mL -> 0.3145 * 1.16 / 36.46 eq/mL
    val acids = listOf(
        AcidProduct(
            "muriatic_31", "Muriatic acid 31.45%", ProductForm.LIQUID, 0.3145 * 1.16 / 36.46,
            tip = "Pump running. Pour slowly in front of a return jet, low over the water. Always add acid to water, never water to acid."
        ),
        AcidProduct(
            "muriatic_14", "Muriatic acid 14.5% (low-fume)", ProductForm.LIQUID, 0.145 * 1.07 / 36.46,
            tip = "Pump running. Pour slowly in front of a return jet, low over the water."
        ),
        AcidProduct(
            "dry_acid", "Dry acid (sodium bisulfate 93%)", ProductForm.SOLID, 0.932 / 120.06,
            tip = "Pre-dissolve in a bucket of pool water, then pour in front of a return jet. Raises TDS / sulfates over time."
        )
    )

    val phUp = listOf(
        SimpleProduct(
            "soda_ash", "Soda ash (sodium carbonate)",
            "Pre-dissolve in a bucket and add slowly near a return. Water may cloud briefly. Also raises alkalinity."
        )
    )

    val calcium = listOf(
        SimpleProduct(
            "cacl2_anhydrous", "Calcium chloride 94% (anhydrous)",
            "Dissolve in a bucket of pool water first — it gets very hot. Pour slowly around the edges."
        ),
        SimpleProduct(
            "cacl2_dihydrate", "Calcium chloride 77% (flake)",
            "Dissolve in a bucket of pool water first — it gets hot. Pour slowly around the edges."
        )
    )

    fun chlorine(id: String) = chlorine.firstOrNull { it.id == id } ?: chlorine.first()
    fun acid(id: String) = acids.firstOrNull { it.id == id } ?: acids.first()
    fun calcium(id: String) = calcium.firstOrNull { it.id == id } ?: calcium.first()

    /** Fraction of CaCl2 in the calcium product. */
    fun calciumPurity(id: String) = if (id == "cacl2_dihydrate") 0.77 else 0.94
}
