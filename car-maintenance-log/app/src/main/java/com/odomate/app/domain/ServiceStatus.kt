package com.odomate.app.domain

import com.odomate.app.data.db.OdometerReading
import com.odomate.app.data.db.ServiceType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToLong

enum class Level(val rank: Int) { OVERDUE(0), SOON(1), OK(2), UNKNOWN(3), DISABLED(4) }

data class ServiceStatus(
    val type: ServiceType,
    val level: Level,
    val dueOdometer: Int?,
    val dueDate: Long?,
    val remainingDistance: Int?,
    val remainingDays: Long?,
    val distanceProgress: Float?,
    val timeProgress: Float?,
    /** Earliest of the hard due date and the date projected from average daily driving. */
    val estimatedDueDate: Long?,
) {
    val progress: Float get() = max(distanceProgress ?: 0f, timeProgress ?: 0f).coerceIn(0f, 1f)

    /** True when the distance dimension is the one that will trigger first. */
    val distanceDominant: Boolean
        get() = when {
            remainingDistance == null -> false
            remainingDays == null -> true
            else -> (distanceProgress ?: 0f) >= (timeProgress ?: 0f)
        }
}

object StatusCalculator {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun toLocalDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    fun toMillis(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun compute(
        type: ServiceType,
        currentOdometer: Int?,
        avgPerDay: Double?,
        now: Long = System.currentTimeMillis(),
    ): ServiceStatus {
        if (!type.enabled) return empty(type, Level.DISABLED)

        val today = toLocalDate(now)

        var dueOdo: Int? = null
        var remDist: Int? = null
        var distProgress: Float? = null
        val interval = type.intervalDistance
        val lastOdo = type.lastDoneOdometer
        if (interval != null && interval > 0 && lastOdo != null) {
            dueOdo = lastOdo + interval
            if (currentOdometer != null) {
                remDist = dueOdo - currentOdometer
                distProgress = (currentOdometer - lastOdo).toFloat() / interval
            }
        }

        var dueDate: Long? = null
        var remDays: Long? = null
        var timeProgress: Float? = null
        val months = type.intervalMonths
        val lastDate = type.lastDoneDate
        if (months != null && months > 0 && lastDate != null) {
            val start = toLocalDate(lastDate)
            val due = start.plusMonths(months.toLong())
            dueDate = toMillis(due)
            remDays = ChronoUnit.DAYS.between(today, due)
            val total = ChronoUnit.DAYS.between(start, due).coerceAtLeast(1)
            timeProgress = ChronoUnit.DAYS.between(start, today).toFloat() / total
        }

        if (remDist == null && remDays == null) {
            return empty(type, Level.UNKNOWN).copy(dueOdometer = dueOdo, dueDate = dueDate)
        }

        val level = when {
            (remDist != null && remDist < 0) || (remDays != null && remDays < 0) -> Level.OVERDUE
            (remDist != null && remDist <= type.remindDistanceBefore) ||
                (remDays != null && remDays <= type.remindDaysBefore) -> Level.SOON
            else -> Level.OK
        }

        var estimate: Long? = dueDate
        if (remDist != null && avgPerDay != null && avgPerDay > 0.5) {
            val days = (remDist / avgPerDay).roundToLong()
            val projected = toMillis(today.plusDays(days))
            estimate = if (estimate == null) projected else minOf(estimate, projected)
        }

        return ServiceStatus(type, level, dueOdo, dueDate, remDist, remDays, distProgress, timeProgress, estimate)
    }

    private fun empty(type: ServiceType, level: Level) =
        ServiceStatus(type, level, null, null, null, null, null, null, null)

    /** Average distance per day using readings from the last ~120 days (falls back to all readings). */
    fun averagePerDay(readings: List<OdometerReading>, now: Long = System.currentTimeMillis()): Double? {
        if (readings.size < 2) return null
        val windowStart = now - 120L * 24 * 3600 * 1000
        val recent = readings.filter { it.date >= windowStart }.takeIf { it.size >= 2 } ?: readings
        val first = recent.minBy { it.date }
        val last = recent.maxBy { it.date }
        val days = (last.date - first.date) / (24.0 * 3600 * 1000)
        if (days < 1.0) return null
        val dist = recent.maxOf { it.value } - first.value
        if (dist <= 0) return null
        return dist / days
    }

    fun sort(statuses: List<ServiceStatus>): List<ServiceStatus> =
        statuses.sortedWith(
            compareBy<ServiceStatus> { it.level.rank }
                .thenByDescending { it.progress }
                .thenBy { it.type.sortOrder }
        )
}
