package com.devatjunaid.renevo.backup

import com.devatjunaid.renevo.data.CycleUnit
import com.devatjunaid.renevo.data.SubStatus
import com.devatjunaid.renevo.data.Subscription
import com.devatjunaid.renevo.data.Usage
import com.devatjunaid.renevo.domain.Formats
import java.time.LocalDate

/** Plain spreadsheet export/import. Not encrypted - the UI warns about this. */
object CsvIO {
    private val HEADER = listOf(
        "name", "price", "currency", "cycle_unit", "cycle_count", "first_payment", "category",
        "payment_method", "status", "trial_end", "reminder_days", "usage", "notes",
    )

    fun export(subs: List<Subscription>): String = buildString {
        appendLine(HEADER.joinToString(","))
        subs.forEach { s ->
            appendLine(
                listOf(
                    s.name, Formats.minorToInput(s.priceMinor), s.currency, s.cycleUnit.name, s.cycleCount.toString(),
                    LocalDate.ofEpochDay(s.firstPaymentEpochDay).toString(), s.category, s.paymentMethod, s.status.name,
                    s.trialEndEpochDay?.let { LocalDate.ofEpochDay(it).toString() }.orEmpty(),
                    s.reminderDaysBefore.toString(), s.usage.name, s.notes,
                ).joinToString(",") { escape(it) },
            )
        }
    }

    data class ImportResult(val subs: List<Subscription>, val skipped: Int)

    fun import(text: String, defaultCurrency: String): ImportResult {
        val rows = parse(text)
        if (rows.isEmpty()) return ImportResult(emptyList(), 0)
        val header = rows.first().map { it.trim().lowercase() }
        fun idx(n: String) = header.indexOf(n)
        val iName = idx("name")
        val iPrice = idx("price")
        if (iName < 0 || iPrice < 0) throw IllegalArgumentException("The CSV needs at least 'name' and 'price' columns.")
        val out = ArrayList<Subscription>()
        var skipped = 0
        rows.drop(1).forEach { r ->
            fun col(n: String): String? = idx(n).takeIf { it >= 0 && it < r.size }?.let { r[it].trim() }?.takeIf { it.isNotEmpty() }
            val name = col("name")
            val price = col("price")?.let { Formats.parseMinor(it) }
            if (name == null || price == null) {
                skipped++
                return@forEach
            }
            val status = col("status")?.let { runCatching { SubStatus.valueOf(it.uppercase()) }.getOrNull() } ?: SubStatus.ACTIVE
            val first = col("first_payment")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
            val trialEnd = col("trial_end")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            out += Subscription(
                name = name,
                priceMinor = price,
                currency = col("currency")?.uppercase() ?: defaultCurrency,
                cycleUnit = col("cycle_unit")?.let { runCatching { CycleUnit.valueOf(it.uppercase()) }.getOrNull() } ?: CycleUnit.MONTH,
                cycleCount = col("cycle_count")?.toIntOrNull()?.coerceIn(1, 365) ?: 1,
                firstPaymentEpochDay = first.toEpochDay(),
                category = col("category") ?: "Other",
                paymentMethod = col("payment_method").orEmpty(),
                notes = col("notes").orEmpty(),
                status = status,
                trialEndEpochDay = trialEnd?.toEpochDay(),
                reminderDaysBefore = col("reminder_days")?.toIntOrNull() ?: 3,
                usage = col("usage")?.let { runCatching { Usage.valueOf(it.uppercase()) }.getOrNull() } ?: Usage.UNKNOWN,
                statusChangedEpochDay = if (status == SubStatus.CANCELLED || status == SubStatus.PAUSED) LocalDate.now().toEpochDay() else null,
            )
        }
        return ImportResult(out, skipped)
    }

    private fun escape(v: String): String {
        // Leading =,+,-,@ would run as a formula in spreadsheet apps.
        val safe = if (v.isNotEmpty() && v[0] in "=+-@" && v.toDoubleOrNull() == null) "'$v" else v
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }

    /** RFC 4180 parser: quoted fields, escaped quotes, CRLF. */
    fun parse(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        val t = text.removePrefix("﻿")
        while (i < t.length) {
            val c = t[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < t.length && t[i + 1] == '"') { field.append('"'); i++ } else inQuotes = false
                } else field.append(c)
            } else when (c) {
                '"' -> inQuotes = true
                ',' -> { row.add(field.toString()); field.setLength(0) }
                '\r' -> {}
                '\n' -> { row.add(field.toString()); field.setLength(0); if (row.any { it.isNotBlank() }) rows.add(row); row = ArrayList() }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            if (row.any { it.isNotBlank() }) rows.add(row)
        }
        return rows
    }
}
