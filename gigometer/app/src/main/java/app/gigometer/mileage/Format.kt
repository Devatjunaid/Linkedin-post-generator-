package app.gigometer.mileage

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Format {
    private val money = NumberFormat.getCurrencyInstance(Locale.US)
    private val time = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val dayLong = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.US)
    private val dayShort = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    private val dateFull = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
    val zone: ZoneId get() = ZoneId.systemDefault()

    fun money(v: Double): String = money.format(v)
    fun miles(v: Double): String = if (v >= 1000) String.format(Locale.US, "%,.0f", v) else String.format(Locale.US, "%.1f", v)
    fun milesPrecise(v: Double): String = String.format(Locale.US, "%.2f", v)
    fun rate(v: Double): String {
        val tenthsOfCent = Math.round(v * 1000)
        return if (tenthsOfCent % 10 == 0L) String.format(Locale.US, "$%.2f", v) else String.format(Locale.US, "$%.3f", v)
    }
    fun cents(v: Double): String {
        val tenthsOfCent = Math.round(v * 1000)
        return if (tenthsOfCent % 10 == 0L) "${tenthsOfCent / 10}¢" else String.format(Locale.US, "%.1f¢", tenthsOfCent / 10.0)
    }

    fun localDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    fun time(ms: Long): String = time.format(Instant.ofEpochMilli(ms).atZone(zone))
    fun dayLong(d: LocalDate): String = dayLong.format(d)
    fun dayShort(d: LocalDate): String = dayShort.format(d)
    fun date(ms: Long): String = dateFull.format(Instant.ofEpochMilli(ms).atZone(zone))
    fun date(d: LocalDate): String = dateFull.format(d)

    fun duration(ms: Long): String {
        val totalMin = (ms / 60000).coerceAtLeast(0)
        return if (totalMin < 60) "$totalMin min" else "${totalMin / 60} h ${totalMin % 60} min"
    }

    fun clock(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return if (s >= 3600) String.format(Locale.US, "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
        else String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
    }

    fun minuteOfDay(m: Int): String {
        val h = m / 60
        val min = m % 60
        val ampm = if (h < 12) "a" else "p"
        val h12 = when (val x = h % 12) { 0 -> 12; else -> x }
        return if (min == 0) "$h12$ampm" else String.format(Locale.US, "%d:%02d%s", h12, min, ampm)
    }
}
