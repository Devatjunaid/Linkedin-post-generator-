package com.devatjunaid.examclock.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Countdown(val days: Long, val hours: Int, val minutes: Int, val seconds: Int) {
    val isOver: Boolean get() = days == 0L && hours == 0 && minutes == 0 && seconds == 0

    companion object {
        fun between(nowMillis: Long, targetMillis: Long): Countdown {
            val totalSeconds = ((targetMillis - nowMillis).coerceAtLeast(0L)) / 1000L
            return Countdown(
                days = totalSeconds / 86_400L,
                hours = ((totalSeconds % 86_400L) / 3_600L).toInt(),
                minutes = ((totalSeconds % 3_600L) / 60L).toInt(),
                seconds = (totalSeconds % 60L).toInt(),
            )
        }

        /** Calendar days from [today] to [date]; negative when the date is in the past. */
        fun calendarDays(today: LocalDate, date: LocalDate): Long = ChronoUnit.DAYS.between(today, date)
    }
}
