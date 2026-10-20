package com.superdevlabs.coldplunge.domain

data class PhaseTotals(val heatSec: Int, val coldSec: Int, val restSec: Int) {
    val totalSec: Int get() = heatSec + coldSec + restSec
}

object ProtocolPlanner {

    /** Built-in protocols. Negative ids so they never collide with database rows. */
    val presets: List<Protocol> = listOf(
        Protocol(-1, "Nordic Classic", heatSec = 15 * 60, coldSec = 3 * 60, restSec = 5 * 60, rounds = 3, heatTempC = 85f, coldTempC = 4f, builtIn = true),
        Protocol(-2, "Quick Reset", heatSec = 10 * 60, coldSec = 2 * 60, restSec = 2 * 60, rounds = 2, heatTempC = 80f, coldTempC = 6f, builtIn = true),
        Protocol(-3, "Deep Recovery", heatSec = 20 * 60, coldSec = 4 * 60, restSec = 8 * 60, rounds = 3, heatTempC = 90f, coldTempC = 5f, builtIn = true),
        Protocol(-4, "First Timer", heatSec = 10 * 60, coldSec = 60, restSec = 5 * 60, rounds = 2, heatTempC = 75f, coldTempC = 12f, builtIn = true),
        Protocol(-5, "Warm Finish", heatSec = 12 * 60, coldSec = 2 * 60, restSec = 3 * 60, rounds = 2, endOnCold = false, heatTempC = 85f, coldTempC = 6f, builtIn = true),
    )

    /**
     * Expands a protocol into phases: every round is heat then cold, with rest between rounds.
     * When [Protocol.endOnCold] is false a rest and a final heat block are appended.
     */
    fun phases(p: Protocol): List<Phase> {
        val rounds = p.rounds.coerceAtLeast(1)
        val out = mutableListOf<Phase>()
        for (r in 1..rounds) {
            out += Phase(PhaseType.HEAT, p.heatSec, p.heatTempC, r)
            out += Phase(PhaseType.COLD, p.coldSec, p.coldTempC, r)
            if (r < rounds) out += Phase(PhaseType.REST, p.restSec, null, r)
        }
        if (!p.endOnCold) {
            out += Phase(PhaseType.REST, p.restSec, null, rounds)
            out += Phase(PhaseType.HEAT, p.heatSec, p.heatTempC, rounds)
        }
        return out.filter { it.durationSec > 0 }
    }

    fun plan(p: Protocol): SessionPlan = SessionPlan(p.name, SessionKind.CONTRAST, phases(p))

    fun single(type: PhaseType, durationSec: Int, tempC: Float): SessionPlan {
        require(type != PhaseType.REST) { "A single session must be heat or cold" }
        val cold = type == PhaseType.COLD
        return SessionPlan(
            title = if (cold) "Cold Plunge" else "Sauna",
            kind = if (cold) SessionKind.COLD else SessionKind.HEAT,
            phases = listOf(Phase(type, durationSec, tempC, 1)),
        )
    }

    fun totals(phases: List<Phase>): PhaseTotals = PhaseTotals(
        heatSec = phases.filter { it.type == PhaseType.HEAT }.sumOf { it.durationSec },
        coldSec = phases.filter { it.type == PhaseType.COLD }.sumOf { it.durationSec },
        restSec = phases.filter { it.type == PhaseType.REST }.sumOf { it.durationSec },
    )
}
