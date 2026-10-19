package com.devatjunaid.kelvin.domain

enum class PhaseType { HEAT, COLD, REST }

enum class SessionKind { COLD, HEAT, CONTRAST }

/** One timed block of a session. [round] is 1-based. */
data class Phase(
    val type: PhaseType,
    val durationSec: Int,
    val tempC: Float? = null,
    val round: Int = 1,
)

/** A repeatable contrast-therapy recipe: rounds of heat -> cold, separated by rest. */
data class Protocol(
    val id: Long = 0L,
    val name: String,
    val heatSec: Int,
    val coldSec: Int,
    val restSec: Int,
    val rounds: Int,
    val endOnCold: Boolean = true,
    val heatTempC: Float = 85f,
    val coldTempC: Float = 4f,
    val builtIn: Boolean = false,
)

/** Everything the timer needs to run one session. */
data class SessionPlan(
    val title: String,
    val kind: SessionKind,
    val phases: List<Phase>,
) {
    val totalSec: Int get() = phases.sumOf { it.durationSec }
    val rounds: Int get() = phases.maxOfOrNull { it.round } ?: 1
    val coldTempC: Float? get() = phases.firstOrNull { it.type == PhaseType.COLD }?.tempC
    val heatTempC: Float? get() = phases.firstOrNull { it.type == PhaseType.HEAT }?.tempC
}

/** A finished, logged session. Times are what was actually done, not what was planned. */
data class SessionRecord(
    val id: Long = 0L,
    val startedAt: Long,
    val kind: SessionKind,
    val title: String,
    val totalSec: Int,
    val coldSec: Int,
    val heatSec: Int,
    val rounds: Int,
    val coldTempC: Float?,
    val heatTempC: Float?,
    val mood: Int?,
    val note: String,
)

object Moods {
    val labels = listOf("Drained", "Okay", "Good", "Energized", "Euphoric")
    fun label(mood: Int?): String? = mood?.let { labels.getOrNull(it - 1) }
}
