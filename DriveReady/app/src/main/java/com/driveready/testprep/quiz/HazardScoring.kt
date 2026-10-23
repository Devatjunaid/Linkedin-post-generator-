package com.driveready.testprep.quiz

import com.driveready.testprep.data.content.HAZARD_MAX_TAPS
import com.driveready.testprep.data.content.HAZARD_WINDOW_SEC

/** Hazard perception scoring, modelled on the UK DVSA approach. */
object HazardScoring {
    const val MAX_PER_CLIP = 5

    /** Practice pass ratio, matching the UK pass mark of 44/75 (≈ 59%). */
    const val PASS_RATIO = 44f / 75f

    /**
     * 5 points for a tap in the first fifth of the window after the hazard starts developing,
     * down to 1 point in the last fifth. Taps outside the window score nothing; more than
     * [HAZARD_MAX_TAPS] taps in a clip is treated as pattern-clicking and scores zero.
     */
    fun score(taps: List<Float>, developAt: Float): Int {
        if (taps.size > HAZARD_MAX_TAPS) return 0
        val first = taps.filter { it >= developAt && it < developAt + HAZARD_WINDOW_SEC }.minOrNull() ?: return 0
        val band = ((first - developAt) / (HAZARD_WINDOW_SEC / MAX_PER_CLIP)).toInt().coerceIn(0, MAX_PER_CLIP - 1)
        return MAX_PER_CLIP - band
    }

    fun passMark(clips: Int): Int = kotlin.math.ceil(clips * MAX_PER_CLIP * PASS_RATIO).toInt()
}
