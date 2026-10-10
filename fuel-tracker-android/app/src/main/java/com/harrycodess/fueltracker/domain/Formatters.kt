package com.harrycodess.fueltracker.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object Fmt {
    private val dayFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
    private val fullDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val shortMonthFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

    fun money(amount: Double, currency: String): String =
        currency + String.format(Locale.getDefault(), "%,.2f", amount)

    fun number(value: Double, decimals: Int = 1): String =
        String.format(Locale.getDefault(), "%,.${decimals}f", value)

    /** Value suitable for pre-filling a text field: max 2 decimals, "." separator, no grouping. */
    fun input(value: Double): String =
        BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    fun day(date: LocalDate): String = date.format(dayFormatter)
    fun fullDate(date: LocalDate): String = date.format(fullDateFormatter)
    fun month(month: YearMonth): String = month.format(monthFormatter)
    fun shortMonth(month: YearMonth): String = month.format(shortMonthFormatter)
}
