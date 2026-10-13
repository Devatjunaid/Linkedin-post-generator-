package com.snaptensio.bplog.domain

import com.snaptensio.bplog.data.Arm
import com.snaptensio.bplog.data.Guideline
import com.snaptensio.bplog.data.Medication
import com.snaptensio.bplog.data.Profile
import com.snaptensio.bplog.data.Reading
import com.snaptensio.bplog.data.tagList
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

const val DAY_MS = 24L * 60 * 60 * 1000

data class BpAverage(val sys: Int, val dia: Int, val pulse: Int?, val count: Int) {
    override fun toString() = "$sys/$dia"
}

fun List<Reading>.average(): BpAverage? {
    if (isEmpty()) return null
    val pulses = mapNotNull { it.pulse }
    return BpAverage(
        sys = map { it.systolic }.average().roundToInt(),
        dia = map { it.diastolic }.average().roundToInt(),
        pulse = if (pulses.isEmpty()) null else pulses.average().roundToInt(),
        count = size,
    )
}

fun List<Reading>.since(fromMillis: Long) = filter { it.timestamp >= fromMillis }

fun hourOf(ts: Long): Int = Calendar.getInstance().apply { timeInMillis = ts }.get(Calendar.HOUR_OF_DAY)

fun Reading.isMorning() = hourOf(timestamp) in 4..11
fun Reading.isEvening() = hourOf(timestamp) in 17..23

fun Reading.inTarget(profile: Profile) = systolic < profile.targetSystolic && diastolic < profile.targetDiastolic

data class PeriodStats(
    val average: BpAverage?,
    val min: Reading?,
    val max: Reading?,
    val inTargetPercent: Int,
    val distribution: List<Pair<Severity, Int>>,
    val morning: BpAverage?,
    val evening: BpAverage?,
    val changeVsPrevious: Int?,
)

fun computeStats(all: List<Reading>, profile: Profile, guideline: Guideline, days: Int, now: Long = System.currentTimeMillis()): PeriodStats {
    val from = now - days * DAY_MS
    val period = all.filter { it.timestamp in from..now }
    val previous = all.filter { it.timestamp in (from - days * DAY_MS) until from }
    val counts = period.groupingBy { BpClassifier.classify(it.systolic, it.diastolic, guideline).severity }.eachCount()
    val total = period.size.coerceAtLeast(1)
    val distribution = Severity.entries.mapNotNull { s -> counts[s]?.let { s to (it * 100f / total).roundToInt() } }
    val cur = period.average()
    val prev = previous.average()
    return PeriodStats(
        average = cur,
        min = period.minByOrNull { it.systolic },
        max = period.maxByOrNull { it.systolic },
        inTargetPercent = if (period.isEmpty()) 0 else (period.count { it.inTarget(profile) } * 100f / period.size).roundToInt(),
        distribution = distribution,
        morning = period.filter { it.isMorning() }.average(),
        evening = period.filter { it.isEvening() }.average(),
        changeVsPrevious = if (cur != null && prev != null && previous.size >= 3 && period.size >= 3) cur.sys - prev.sys else null,
    )
}

enum class InsightTone { GOOD, INFO, WATCH }

data class Insight(val badge: String, val title: String, val body: String, val tone: InsightTone)

private val dateFmt get() = SimpleDateFormat("d MMM", Locale.getDefault())

/**
 * Rule-based insights. Everything is plain arithmetic on the user's own readings, done on the device.
 * Thresholds are intentionally conservative and phrased as observations, never as diagnoses.
 */
fun computeInsights(all: List<Reading>, meds: List<Medication>, now: Long = System.currentTimeMillis()): List<Insight> {
    val out = mutableListOf<Insight>()
    val last30 = all.since(now - 30 * DAY_MS)
    if (last30.size < 3) return out

    // 1. Trend: last 14 days vs the 14 days before.
    val recent = all.filter { it.timestamp >= now - 14 * DAY_MS }.average()
    val before = all.filter { it.timestamp in (now - 28 * DAY_MS) until (now - 14 * DAY_MS) }.average()
    if (recent != null && before != null && recent.count >= 3 && before.count >= 3) {
        val d = recent.sys - before.sys
        if (abs(d) >= 5) {
            out += if (d < 0) {
                Insight("−${abs(d)}", "Systolic down ${abs(d)} mmHg in the last 2 weeks", "Average $recent now vs $before in the 2 weeks before.", InsightTone.GOOD)
            } else {
                Insight("+$d", "Systolic up $d mmHg in the last 2 weeks", "Average $recent now vs $before before. Worth mentioning to your doctor if it continues.", InsightTone.WATCH)
            }
        }
    }

    // 2. Medication start effect.
    for (med in meds) {
        val pre = all.filter { it.timestamp in (med.startDate - 14 * DAY_MS) until med.startDate }.average()
        val post = all.filter { it.timestamp >= med.startDate && it.timestamp <= med.startDate + 30 * DAY_MS }.average()
        if (pre != null && post != null && pre.count >= 3 && post.count >= 3) {
            val d = post.sys - pre.sys
            if (abs(d) >= 5) {
                val dir = if (d < 0) "down" else "up"
                out += Insight(
                    if (d < 0) "−${abs(d)}" else "+$d",
                    "Systolic $dir ${abs(d)} mmHg since starting ${med.name}",
                    "Average ${pre} before ${dateFmt.format(Date(med.startDate))}, $post after. Share this with your doctor.",
                    if (d < 0) InsightTone.GOOD else InsightTone.WATCH,
                )
            }
        }
    }

    // 3. Morning vs evening.
    val morning = last30.filter { it.isMorning() }.average()
    val evening = last30.filter { it.isEvening() }.average()
    if (morning != null && evening != null && morning.count >= 3 && evening.count >= 3) {
        val d = evening.sys - morning.sys
        if (abs(d) >= 5) {
            val which = if (d > 0) "Evenings" else "Mornings"
            out += Insight(
                "${abs(d)}",
                "$which run ${abs(d)} mmHg higher",
                "Morning average $morning, evening average $evening over the last 30 days.",
                InsightTone.INFO,
            )
        }
    }

    // 4. Tag effects.
    val tags = last30.flatMap { it.tagList() }.groupingBy { it }.eachCount().filter { it.value >= 3 }.keys
    for (tag in tags) {
        val with = last30.filter { tag in it.tagList() }.average() ?: continue
        val without = last30.filter { tag !in it.tagList() }.average() ?: continue
        if (without.count < 3) continue
        val d = with.sys - without.sys
        if (abs(d) >= 5) {
            out += Insight(
                if (d > 0) "+$d" else "−${abs(d)}",
                "Readings tagged “$tag” are ${abs(d)} ${if (d > 0) "higher" else "lower"}",
                "Average $with with this tag vs $without without it.",
                if (d > 0) InsightTone.WATCH else InsightTone.INFO,
            )
        }
    }

    // 5. Arm difference.
    val left = last30.filter { it.arm == Arm.LEFT.name }.average()
    val right = last30.filter { it.arm == Arm.RIGHT.name }.average()
    if (left != null && right != null && left.count >= 2 && right.count >= 2) {
        val d = abs(left.sys - right.sys)
        if (d > 10) {
            out += Insight(
                "$d",
                "Your arms differ by $d mmHg",
                "Left $left, right $right. A gap over 10 mmHg is worth mentioning at your next visit. Measure on the higher arm.",
                InsightTone.WATCH,
            )
        }
    }

    // 6. Pulse pressure and pulse.
    val avg = last30.average()!!
    val pp = avg.sys - avg.dia
    if (pp > 60) {
        out += Insight("$pp", "Wide gap between your numbers", "Your average pulse pressure (top minus bottom) is $pp mmHg. Ask your doctor whether this matters for you.", InsightTone.WATCH)
    }
    avg.pulse?.let { p ->
        if (p > 100) out += Insight("$p", "Resting pulse averages $p bpm", "That is above the usual resting range of 60–100. Make sure you rest 5 minutes before measuring.", InsightTone.WATCH)
        if (p < 50) out += Insight("$p", "Resting pulse averages $p bpm", "That is below 50. This can be normal for fit people; mention it if you feel dizzy or tired.", InsightTone.INFO)
    }

    // 7. Consistency.
    val days7 = all.since(now - 7 * DAY_MS).map { dayKey(it.timestamp) }.toSet().size
    out += if (days7 >= 6) {
        Insight("$days7", "Measured on $days7 of the last 7 days", "Great consistency. Regular readings make your averages much more reliable.", InsightTone.GOOD)
    } else {
        Insight("$days7", "Measured on $days7 of the last 7 days", "Doctors usually ask for readings twice a day for 7 days before a visit. Set reminders in Meds.", InsightTone.INFO)
    }
    return out
}

fun dayKey(ts: Long): Int {
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
}

fun startOfDay(ts: Long): Long = Calendar.getInstance().apply {
    timeInMillis = ts
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis
