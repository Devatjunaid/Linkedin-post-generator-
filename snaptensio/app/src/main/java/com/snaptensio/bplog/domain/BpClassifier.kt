package com.snaptensio.bplog.domain

import androidx.compose.ui.graphics.Color
import com.snaptensio.bplog.data.Guideline

/**
 * Severity buckets shared by both guidelines so colours, charts and insights stay consistent.
 * [level] orders them from lowest to highest risk.
 */
enum class Severity(val level: Int, val color: Color, val onColor: Color, val tint: Color) {
    LOW(0, Color(0xFF2B7BB9), Color(0xFF0E3C5E), Color(0xFFE1EEF8)),
    NORMAL(1, Color(0xFF1E8E5A), Color(0xFF14633F), Color(0xFFE7F4EC)),
    ELEVATED(2, Color(0xFFE0A526), Color(0xFF7A4D06), Color(0xFFFDF0D5)),
    STAGE1(3, Color(0xFFE07A2B), Color(0xFF8A3D0D), Color(0xFFFCE6D6)),
    STAGE2(4, Color(0xFFC2272D), Color(0xFF8F1B20), Color(0xFFF9DEDF)),
    CRISIS(5, Color(0xFF7A1020), Color(0xFF5C0B18), Color(0xFFF4D4D8)),
}

data class BpCategory(val label: String, val severity: Severity, val description: String)

object BpClassifier {

    /** Above this, the app shows the "very high reading" safety screen. Same for both guidelines. */
    fun isCrisis(sys: Int, dia: Int): Boolean = sys > 180 || dia > 120

    fun isLow(sys: Int, dia: Int): Boolean = sys < 90 || dia < 60

    fun classify(sys: Int, dia: Int, guideline: Guideline): BpCategory {
        if (isCrisis(sys, dia)) {
            return BpCategory("Very high", Severity.CRISIS, "Over 180 systolic and/or over 120 diastolic")
        }
        if (isLow(sys, dia)) {
            return BpCategory("Low", Severity.LOW, "Under 90 systolic or under 60 diastolic")
        }
        return when (guideline) {
            Guideline.AHA -> when {
                sys >= 140 || dia >= 90 -> BpCategory("Stage 2", Severity.STAGE2, "140+ systolic or 90+ diastolic")
                sys >= 130 || dia >= 80 -> BpCategory("Stage 1", Severity.STAGE1, "130–139 systolic or 80–89 diastolic")
                sys >= 120 -> BpCategory("Elevated", Severity.ELEVATED, "120–129 systolic and under 80 diastolic")
                else -> BpCategory("Normal", Severity.NORMAL, "Under 120 systolic and under 80 diastolic")
            }
            Guideline.ESC -> when {
                sys >= 180 || dia >= 110 -> BpCategory("Grade 3", Severity.STAGE2, "180+ systolic or 110+ diastolic")
                sys >= 160 || dia >= 100 -> BpCategory("Grade 2", Severity.STAGE2, "160–179 systolic or 100–109 diastolic")
                sys >= 140 || dia >= 90 -> BpCategory("Grade 1", Severity.STAGE1, "140–159 systolic or 90–99 diastolic")
                sys >= 130 || dia >= 85 -> BpCategory("High normal", Severity.ELEVATED, "130–139 systolic or 85–89 diastolic")
                sys >= 120 || dia >= 80 -> BpCategory("Normal", Severity.NORMAL, "120–129 systolic or 80–84 diastolic")
                else -> BpCategory("Optimal", Severity.NORMAL, "Under 120 systolic and under 80 diastolic")
            }
        }
    }

    /** Rows for the "What the ranges mean" table. */
    fun legend(guideline: Guideline): List<Triple<String, Severity, String>> = when (guideline) {
        Guideline.AHA -> listOf(
            Triple("Normal", Severity.NORMAL, "<120 and <80"),
            Triple("Elevated", Severity.ELEVATED, "120–129 and <80"),
            Triple("Stage 1", Severity.STAGE1, "130–139 or 80–89"),
            Triple("Stage 2", Severity.STAGE2, "≥140 or ≥90"),
            Triple("Very high", Severity.CRISIS, ">180 and/or >120"),
        )
        Guideline.ESC -> listOf(
            Triple("Optimal", Severity.NORMAL, "<120 and <80"),
            Triple("Normal", Severity.NORMAL, "120–129 and/or 80–84"),
            Triple("High normal", Severity.ELEVATED, "130–139 and/or 85–89"),
            Triple("Grade 1", Severity.STAGE1, "140–159 and/or 90–99"),
            Triple("Grade 2", Severity.STAGE2, "160–179 and/or 100–109"),
            Triple("Grade 3", Severity.STAGE2, "≥180 and/or ≥110"),
        )
    }

    /** Plausible ranges used to validate both typed and scanned values. */
    val SYS_RANGE = 60..260
    val DIA_RANGE = 30..160
    val PULSE_RANGE = 30..220

    fun validate(sys: Int?, dia: Int?, pulse: Int?): String? = when {
        sys == null || dia == null -> "Enter both systolic and diastolic"
        sys !in SYS_RANGE -> "Systolic should be between ${SYS_RANGE.first} and ${SYS_RANGE.last}"
        dia !in DIA_RANGE -> "Diastolic should be between ${DIA_RANGE.first} and ${DIA_RANGE.last}"
        dia >= sys -> "Systolic (top number) must be higher than diastolic"
        pulse != null && pulse !in PULSE_RANGE -> "Pulse should be between ${PULSE_RANGE.first} and ${PULSE_RANGE.last}"
        else -> null
    }
}
