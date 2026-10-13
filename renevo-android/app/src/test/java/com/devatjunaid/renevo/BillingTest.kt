package com.devatjunaid.renevo

import com.devatjunaid.renevo.backup.BackupManager
import com.devatjunaid.renevo.backup.CsvIO
import com.devatjunaid.renevo.backup.WrongPasswordException
import com.devatjunaid.renevo.data.AppSettings
import com.devatjunaid.renevo.data.CycleUnit
import com.devatjunaid.renevo.data.PricePoint
import com.devatjunaid.renevo.data.SubStatus
import com.devatjunaid.renevo.data.Subscription
import com.devatjunaid.renevo.domain.Billing
import com.devatjunaid.renevo.domain.Formats
import com.devatjunaid.renevo.domain.Portfolio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BillingTest {
    private val today = LocalDate.of(2026, 10, 13)
    private val monthly = Subscription(
        name = "X", priceMinor = 1599, currency = "USD", firstPaymentEpochDay = LocalDate.of(2026, 1, 31).toEpochDay(),
    )

    @Test fun monthEndDoesNotDrift() {
        assertEquals(LocalDate.of(2026, 2, 28), Billing.chargeDate(monthly, 1))
        assertEquals(LocalDate.of(2026, 3, 31), Billing.chargeDate(monthly, 2))
    }

    @Test fun nextChargeAndSpent() {
        assertEquals(LocalDate.of(2026, 10, 31), Billing.nextCharge(monthly, today))
        assertEquals(9, Billing.paidCharges(monthly, today).size)
        assertEquals(9 * 1599L, Billing.spentSoFar(monthly, emptyList(), today))
    }

    @Test fun priceHistoryIsUsedForPastCharges() {
        val a = monthly.copy(id = 1, priceMinor = 5999, firstPaymentEpochDay = LocalDate.of(2023, 3, 27).toEpochDay())
        val hist = listOf(
            PricePoint(1, 1, 5499, LocalDate.of(2023, 3, 27).toEpochDay()),
            PricePoint(2, 1, 5999, LocalDate.of(2026, 8, 1).toEpochDay()),
        )
        assertEquals(41 * 5499L + 2 * 5999L, Billing.spentSoFar(a, hist, today))
    }

    @Test fun cancellingStopsChargesAndCountsSavings() {
        val c = monthly.copy(
            priceMinor = 799, firstPaymentEpochDay = LocalDate.of(2023, 3, 27).toEpochDay(),
            status = SubStatus.CANCELLED, statusChangedEpochDay = LocalDate.of(2026, 6, 14).toEpochDay(),
        )
        assertTrue(Billing.occurrences(c, LocalDate.of(2026, 6, 14), today).isEmpty())
        assertEquals(4 * 799L, Billing.savedSoFar(c, today))
    }

    @Test fun trialIsExcludedFromTotalsButFlaggedWhenEnding() {
        val trialEnd = LocalDate.of(2026, 10, 16).toEpochDay()
        val t = Subscription(
            name = "D", priceMinor = 8400, currency = "USD", cycleUnit = CycleUnit.YEAR, status = SubStatus.TRIAL,
            firstPaymentEpochDay = trialEnd, trialEndEpochDay = trialEnd,
        )
        val p = Portfolio(listOf(monthly, t), emptyList(), AppSettings(currency = "USD"), today)
        assertEquals(1599.0, p.monthlyTotal, 0.001)
        assertTrue(p.upcoming(7).any { it.isTrialConversion })
        assertEquals(8400L, Billing.savedSoFar(t.copy(status = SubStatus.CANCELLED, statusChangedEpochDay = today.toEpochDay()), today))
    }

    @Test fun currenciesConvertWithUserRates() {
        val eur = monthly.copy(id = 2, currency = "EUR", priceMinor = 1000)
        val p = Portfolio(listOf(eur), emptyList(), AppSettings(currency = "USD", rates = mapOf("EUR" to 1.1)), today)
        assertEquals(1100.0, p.monthlyTotal, 0.001)
    }

    @Test fun parsesUserAmounts() {
        assertEquals(1299L, Formats.parseMinor("12,99"))
        assertEquals(123456L, Formats.parseMinor("1.234.56"))
        assertNull(Formats.parseMinor("abc"))
    }

    @Test fun csvRoundTripAndFormulaGuard() {
        val csv = CsvIO.export(listOf(monthly.copy(name = "Name, with \"quotes\"", notes = "=cmd")))
        val back = CsvIO.import(csv, "USD").subs.single()
        assertEquals("Name, with \"quotes\"", back.name)
        assertEquals(1599L, back.priceMinor)
        assertEquals("'=cmd", back.notes)
    }

    @Test fun encryptedBackupRoundTrip() {
        val file = BackupManager.toFile(listOf(monthly.copy(id = 7)), emptyList(), AppSettings(currency = "USD"))
        val bytes = BackupManager.encrypt(file, "secret1".toCharArray())
        val back = BackupManager.fromFile(BackupManager.decrypt(bytes, "secret1".toCharArray()))
        assertEquals("X", back.subs.single().name)
        try {
            BackupManager.decrypt(bytes, "wrong-pass".toCharArray())
            throw AssertionError("expected failure")
        } catch (_: WrongPasswordException) {
        }
    }
}
