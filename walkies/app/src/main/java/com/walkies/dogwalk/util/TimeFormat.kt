package com.walkies.dogwalk.util

import android.content.Context
import android.text.format.DateFormat
import com.walkies.dogwalk.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun LocalDate.startMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()

fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

/** Clock time in the user's 12/24-hour preference, e.g. "07:10" or "7:10 AM". */
fun formatClock(context: Context, millis: Long): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
}

fun formatClock(context: Context, hour: Int, minute: Int): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        .format(java.time.LocalTime.of(hour, minute))
}

fun formatDay(date: LocalDate): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()).format(date)

fun formatWeekdayDate(date: LocalDate): String =
    DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()).format(date)

/** "2h 10m", "42 min" or "less than a minute". */
fun formatDuration(context: Context, millis: Long): String {
    val totalMinutes = (millis / 60_000L).coerceAtLeast(0L)
    val hours = (totalMinutes / 60).toInt()
    val minutes = (totalMinutes % 60).toInt()
    return when {
        hours > 0 -> context.getString(R.string.duration_h_m, hours, minutes)
        minutes > 0 -> context.getString(R.string.duration_m, minutes)
        else -> context.getString(R.string.duration_less_min)
    }
}

/** Stopwatch style, "18:42" or "1:05:09". */
fun formatStopwatch(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}
