package com.shelfguard.app.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object Format {
    private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    private val shortDateFormat = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

    fun money(amount: Double, currency: String): String {
        val nf = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            val whole = amount % 1.0 == 0.0
            minimumFractionDigits = if (whole) 0 else 2
            maximumFractionDigits = if (whole) 0 else 2
        }
        val prefix = currency.trim()
        return if (prefix.isEmpty()) nf.format(amount) else "$prefix ${nf.format(amount)}"
    }

    fun number(value: Int): String = NumberFormat.getIntegerInstance().format(value)

    fun date(date: LocalDate): String = date.format(dateFormat)

    fun shortDate(date: LocalDate): String = date.format(shortDateFormat)

    fun quantity(qty: Int, unit: String): String = "${number(qty)} $unit"

    /** "Expired 2 days ago", "Expires today", "Expires in 5 days"... */
    fun expiry(date: LocalDate, today: LocalDate = LocalDate.now()): String {
        val days = ChronoUnit.DAYS.between(today, date)
        return when {
            days < -1 -> "Expired ${-days} days ago"
            days == -1L -> "Expired yesterday"
            days == 0L -> "Expires today"
            days == 1L -> "Expires tomorrow"
            days <= 30 -> "Expires in $days days"
            else -> "Exp ${date(date)}"
        }
    }

    /** Short badge text: "Today", "Tomorrow", "4 days". */
    fun daysBadge(days: Long): String = when {
        days < 0 -> "Expired"
        days == 0L -> "Today"
        days == 1L -> "Tomorrow"
        else -> "$days days"
    }

    fun initials(name: String): String =
        name.split(' ', '-', '(')
            .filter { it.isNotBlank() && it.first().isLetterOrDigit() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "?" }
}
