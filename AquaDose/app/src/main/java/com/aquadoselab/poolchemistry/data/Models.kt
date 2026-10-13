package com.aquadoselab.poolchemistry.data

import kotlinx.serialization.Serializable

@Serializable
enum class PoolShape(val label: String) {
    RECTANGLE("Rectangle"),
    ROUND("Round"),
    OVAL("Oval"),
    KIDNEY("Kidney"),
    CUSTOM("Known volume")
}

@Serializable
enum class Sanitizer(val label: String) {
    CHLORINE("Chlorine"),
    SALT("Salt (SWG)")
}

@Serializable
enum class Surface(val label: String) {
    PLASTER("Plaster / Pebble"),
    VINYL("Vinyl"),
    FIBERGLASS("Fiberglass")
}

@Serializable
enum class UnitSystem { US, METRIC }

@Serializable
enum class TempUnit { F, C }

/** Dimensions are always stored in feet; volume always in US gallons. */
@Serializable
data class Pool(
    val id: String,
    val name: String,
    val shape: PoolShape = PoolShape.RECTANGLE,
    val lengthFt: Double = 0.0,
    val widthFt: Double = 0.0,
    /** Second width for kidney pools (the narrow end). */
    val width2Ft: Double = 0.0,
    val shallowFt: Double = 0.0,
    val deepFt: Double = 0.0,
    val volumeGallons: Double = 0.0,
    val sanitizer: Sanitizer = Sanitizer.CHLORINE,
    val surface: Surface = Surface.PLASTER,
    val indoor: Boolean = false,
    val targets: Targets = Targets.recommended(Sanitizer.CHLORINE, Surface.PLASTER, false),
    val products: ProductChoice = ProductChoice()
)

@Serializable
data class Targets(
    val ph: Double = 7.5,
    val ta: Double = 80.0,
    val ch: Double = 350.0,
    val cya: Double = 40.0,
    val salt: Double = 3200.0
) {
    companion object {
        fun recommended(sanitizer: Sanitizer, surface: Surface, indoor: Boolean): Targets {
            val ch = when (surface) {
                Surface.PLASTER -> 350.0
                Surface.VINYL -> 250.0
                Surface.FIBERGLASS -> 250.0
            }
            val cya = when {
                indoor -> 20.0
                sanitizer == Sanitizer.SALT -> 70.0
                else -> 40.0
            }
            val ta = if (sanitizer == Sanitizer.SALT) 70.0 else 80.0
            return Targets(ph = 7.5, ta = ta, ch = ch, cya = cya, salt = 3200.0)
        }
    }
}

@Serializable
data class ProductChoice(
    val chlorine: String = "liquid_10",
    val acid: String = "muriatic_31",
    val phUp: String = "soda_ash",
    val calcium: String = "cacl2_anhydrous"
)

/** A water test. Nullable values were not tested. */
@Serializable
data class WaterTest(
    val id: String,
    val poolId: String,
    val timestamp: Long,
    val fc: Double?,
    val cc: Double? = null,
    val ph: Double?,
    val ta: Double? = null,
    val ch: Double? = null,
    val cya: Double? = null,
    val salt: Double? = null,
    /** Always stored in °F. */
    val tempF: Double? = null,
    val dosesApplied: Boolean = false,
    val note: String = ""
)

@Serializable
data class AppSettings(
    val units: UnitSystem = UnitSystem.US,
    val tempUnit: TempUnit = TempUnit.F,
    val activePoolId: String? = null,
    val onboardingDone: Boolean = false
)

@Serializable
data class AppData(
    val pools: List<Pool> = emptyList(),
    val tests: List<WaterTest> = emptyList(),
    val settings: AppSettings = AppSettings()
) {
    val activePool: Pool?
        get() = pools.firstOrNull { it.id == settings.activePoolId } ?: pools.firstOrNull()

    fun testsFor(poolId: String): List<WaterTest> =
        tests.filter { it.poolId == poolId }.sortedByDescending { it.timestamp }

    /** Most recent known value of a reading for this pool (used when a field is left blank). */
    fun lastKnown(poolId: String, pick: (WaterTest) -> Double?): Double? =
        testsFor(poolId).firstNotNullOfOrNull(pick)
}
