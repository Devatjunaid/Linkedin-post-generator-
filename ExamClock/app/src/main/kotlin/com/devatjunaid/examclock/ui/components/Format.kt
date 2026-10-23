package com.devatjunaid.examclock.ui.components

import android.content.Context
import android.text.format.DateFormat
import com.devatjunaid.examclock.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale

/** Locale-aware formatting so dates and times read naturally in every country. */
object Format {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun date(millis: Long): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
            .format(Instant.ofEpochMilli(millis).atZone(zone))

    fun date(date: LocalDate): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()).format(date)

    fun longDate(date: LocalDate): String =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM")).format(date)

    fun weekdayShort(date: LocalDate): String =
        DateTimeFormatter.ofPattern("EEE", Locale.getDefault()).format(date)

    fun monthYear(date: LocalDate): String =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMMyyyy")).format(date)

    fun time(context: Context, millis: Long): String = DateFormat.getTimeFormat(context).format(Date(millis))

    fun time(context: Context, time: LocalTime): String =
        time(context, LocalDate.now().atTime(time).atZone(zone).toInstant().toEpochMilli())

    fun minutes(context: Context, minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> context.getString(R.string.duration_m, m)
            m == 0 -> context.getString(R.string.duration_h, h)
            else -> context.getString(R.string.duration_hm, h, m)
        }
    }

    fun toLocalDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    fun toLocalTime(millis: Long): LocalTime = Instant.ofEpochMilli(millis).atZone(zone).toLocalTime()
    fun millis(date: LocalDate, time: LocalTime): Long = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
}
