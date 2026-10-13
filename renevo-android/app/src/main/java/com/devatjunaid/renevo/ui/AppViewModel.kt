package com.devatjunaid.renevo.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devatjunaid.renevo.AppContainer
import com.devatjunaid.renevo.backup.BackupManager
import com.devatjunaid.renevo.backup.CsvIO
import com.devatjunaid.renevo.backup.PdfReport
import com.devatjunaid.renevo.data.AppSettings
import com.devatjunaid.renevo.data.SubStatus
import com.devatjunaid.renevo.data.Subscription
import com.devatjunaid.renevo.data.ThemeMode
import com.devatjunaid.renevo.data.Usage
import com.devatjunaid.renevo.domain.Portfolio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class AppViewModel(private val c: AppContainer) : ViewModel() {
    private val today = MutableStateFlow(LocalDate.now())

    val settings: StateFlow<AppSettings?> = c.settings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val portfolio: StateFlow<Portfolio> = combine(
        c.repository.subscriptions, c.repository.prices, c.settings.settings, today,
    ) { subs, prices, s, d -> Portfolio(subs, prices, s, d) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, Portfolio.empty())

    /** True after the first real data load, so screens don't flash empty states. */
    val loaded: StateFlow<Boolean> = combine(c.repository.subscriptions, c.settings.settings) { _, _ -> true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val ads get() = c.ads

    fun refreshDay() {
        val now = LocalDate.now()
        if (now != today.value) today.value = now
        viewModelScope.launch { c.repository.convertEndedTrials(now) }
    }

    // ---- subscriptions
    suspend fun get(id: Long): Subscription? = c.repository.get(id)

    fun save(sub: Subscription, onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = c.repository.save(sub)
        onSaved(id)
    }

    fun setStatus(id: Long, status: SubStatus) = viewModelScope.launch { c.repository.setStatus(id, status) }
    fun setUsage(id: Long, usage: Usage) = viewModelScope.launch { c.repository.setUsage(id, usage) }
    fun delete(id: Long) = viewModelScope.launch { c.repository.delete(id) }

    // ---- settings
    fun completeOnboarding(currency: String) = viewModelScope.launch {
        c.settings.setCurrency(currency)
        c.settings.setOnboardingDone(true)
    }
    fun setAppLock(v: Boolean) = viewModelScope.launch { c.settings.setAppLock(v) }
    fun setHideAmounts(v: Boolean) = viewModelScope.launch { c.settings.setHideAmounts(v) }
    fun setBlockScreenshots(v: Boolean) = viewModelScope.launch { c.settings.setBlockScreenshots(v) }
    fun setRenewalReminders(v: Boolean) = viewModelScope.launch { c.settings.setRenewalReminders(v) }
    fun setTrialAlerts(v: Boolean) = viewModelScope.launch { c.settings.setTrialAlerts(v) }
    fun setWeeklyDigest(v: Boolean) = viewModelScope.launch { c.settings.setWeeklyDigest(v) }
    fun setDefaultReminderDays(v: Int) = viewModelScope.launch { c.settings.setDefaultReminderDays(v) }
    fun setCurrency(v: String) = viewModelScope.launch { c.settings.setCurrency(v) }
    fun setBudget(minor: Long) = viewModelScope.launch { c.settings.setBudgetMinor(minor) }
    fun setShowYearly(v: Boolean) = viewModelScope.launch { c.settings.setShowYearly(v) }
    fun setTheme(v: ThemeMode) = viewModelScope.launch { c.settings.setTheme(v) }
    fun setRates(v: Map<String, Double>) = viewModelScope.launch { c.settings.setRates(v) }

    fun deleteAllData(onDone: () -> Unit) = viewModelScope.launch {
        c.repository.deleteAll()
        val keepOnboarding = true
        c.settings.clearAll()
        c.settings.setOnboardingDone(keepOnboarding)
        onDone()
    }

    // ---- backup & export (results are user-facing messages)
    suspend fun createBackup(context: Context, uri: Uri, password: CharArray): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val subs = c.repository.all()
            val file = BackupManager.toFile(subs, c.repository.allPrices(), c.settings.current())
            BackupManager.write(context, uri, BackupManager.encrypt(file, password))
            c.settings.setLastBackup(System.currentTimeMillis())
            "Backup saved · ${subs.size} subscriptions"
        }
    }

    suspend fun restoreBackup(context: Context, uri: Uri, password: CharArray, replace: Boolean): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = BackupManager.decrypt(BackupManager.read(context, uri), password)
                val data = BackupManager.fromFile(file)
                c.repository.import(data.subs, data.prices, replace)
                if (replace) data.settings?.let { c.settings.restore(it) }
                "Restored ${data.subs.size} subscriptions"
            }
        }

    suspend fun exportCsv(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val subs = c.repository.all()
            context.contentResolver.openOutputStream(uri, "wt")?.use {
                it.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                it.write(CsvIO.export(subs).toByteArray(Charsets.UTF_8))
            } ?: error("Couldn't open the file")
            "Exported ${subs.size} subscriptions to CSV"
        }
    }

    suspend fun exportPdf(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            PdfReport.write(context, uri, portfolio.value)
            "PDF report saved"
        }
    }

    suspend fun importCsv(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val text = BackupManager.read(context, uri).toString(Charsets.UTF_8)
            val currency = c.settings.settings.map { it.currency }.first()
            val result = CsvIO.import(text, currency)
            if (result.subs.isEmpty()) error("No subscriptions found in that file")
            c.repository.import(result.subs, emptyList(), replace = false)
            "Imported ${result.subs.size}" + if (result.skipped > 0) " · skipped ${result.skipped} rows" else ""
        }
    }
}
