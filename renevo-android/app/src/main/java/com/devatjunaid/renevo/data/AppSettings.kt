package com.devatjunaid.renevo.data

import java.util.Currency
import java.util.Locale

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val onboardingDone: Boolean = false,
    val appLock: Boolean = false,
    val hideAmounts: Boolean = false,
    val blockScreenshots: Boolean = false,
    val renewalReminders: Boolean = true,
    val trialAlerts: Boolean = true,
    val weeklyDigest: Boolean = false,
    val defaultReminderDays: Int = 3,
    val currency: String = defaultCurrency(),
    val budgetMinor: Long = 0,
    val showYearly: Boolean = false,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** 1 unit of key currency = value units of the main currency. */
    val rates: Map<String, Double> = emptyMap(),
    val lastBackupMillis: Long = 0,
    val lastReminderRunDay: Long = -1,
) {
    companion object {
        fun defaultCurrency(): String = runCatching {
            Currency.getInstance(Locale.getDefault()).currencyCode
        }.getOrDefault("USD")
    }
}
