package com.snaptensio.bplog.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snaptensio.bplog.appContainer
import com.snaptensio.bplog.data.AppSettings
import com.snaptensio.bplog.data.DoseLog
import com.snaptensio.bplog.data.Guideline
import com.snaptensio.bplog.data.Medication
import com.snaptensio.bplog.data.Profile
import com.snaptensio.bplog.data.Reading
import com.snaptensio.bplog.data.Reminder
import com.snaptensio.bplog.data.ThemeMode
import com.snaptensio.bplog.domain.DAY_MS
import com.snaptensio.bplog.domain.dayKey
import com.snaptensio.bplog.export.Exporters
import com.snaptensio.bplog.export.ReportOptions
import com.snaptensio.bplog.reminders.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** One activity-scoped ViewModel: the app is small enough that screens share the same live data. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val container = app.appContainer
    private val repo = container.repository
    private val store = container.settings
    private val ctx get() = getApplication<Application>()

    /** null until DataStore has been read once, so the UI can wait instead of flashing onboarding. */
    val settings: StateFlow<AppSettings?> = store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val profiles: StateFlow<List<Profile>> = repo.observeProfiles().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeProfile: StateFlow<Profile?> = combine(profiles, store.settings) { list, s ->
        list.firstOrNull { it.id == s.activeProfileId } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val activeId = activeProfile.map { it?.id }.distinctUntilChanged()

    val readings: StateFlow<List<Reading>> = activeId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repo.observeReadings(id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val medications: StateFlow<List<Medication>> = activeId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repo.observeMedications(id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val doseLogs: StateFlow<List<DoseLog>> = activeId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repo.observeDoseLogs(id, dayKey(System.currentTimeMillis() - 8 * DAY_MS))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val reminders: StateFlow<List<Reminder>> = repo.observeReminders().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            if (repo.allProfiles().isEmpty()) {
                val id = repo.addProfile(Profile(name = "Me"))
                store.setActiveProfile(id)
            }
            if (repo.allReminders().isEmpty()) {
                repo.saveReminder(Reminder(hour = 7, minute = 30, label = "Morning", enabled = false))
                repo.saveReminder(Reminder(hour = 20, minute = 0, label = "Evening", enabled = false))
            }
        }
    }

    // ---------------------------------------------------------------- settings / profiles

    fun completeOnboarding(name: String, targetSys: Int, targetDia: Int) = viewModelScope.launch {
        activeProfile.value?.let { repo.updateProfile(it.copy(name = name.ifBlank { "Me" }, targetSystolic = targetSys, targetDiastolic = targetDia)) }
        store.setOnboarded()
    }

    fun setGuideline(g: Guideline) = viewModelScope.launch { store.setGuideline(g) }
    fun setTheme(t: ThemeMode) = viewModelScope.launch { store.setTheme(t) }
    fun setCrisisAlerts(on: Boolean) = viewModelScope.launch { store.setCrisisAlerts(on) }
    fun setEmergencyNumber(n: String) = viewModelScope.launch { store.setEmergencyNumber(n) }
    fun setDoctorNumber(n: String) = viewModelScope.launch { store.setDoctorNumber(n) }
    fun switchProfile(id: Long) = viewModelScope.launch { store.setActiveProfile(id) }

    fun saveProfile(p: Profile) = viewModelScope.launch {
        if (p.id == 0L) {
            val id = repo.addProfile(p.copy(colorIndex = profiles.value.size % 6))
            store.setActiveProfile(id)
        } else {
            repo.updateProfile(p)
        }
    }

    fun deleteProfile(p: Profile) = viewModelScope.launch {
        if (profiles.value.size <= 1) return@launch
        repo.deleteProfile(p)
        val next = repo.allProfiles().firstOrNull() ?: return@launch
        store.setActiveProfile(next.id)
    }

    // ---------------------------------------------------------------- readings

    suspend fun loadReading(id: Long): Reading? = repo.reading(id)

    fun saveReading(r: Reading, onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        val profileId = if (r.profileId != 0L) r.profileId else activeProfile.value?.id ?: return@launch
        val id = repo.saveReading(r.copy(profileId = profileId))
        onSaved(id)
    }

    fun deleteReading(id: Long) = viewModelScope.launch { repo.deleteReading(id) }

    // ---------------------------------------------------------------- meds & reminders

    fun saveMedication(m: Medication) = viewModelScope.launch {
        val profileId = if (m.profileId != 0L) m.profileId else activeProfile.value?.id ?: return@launch
        val saved = m.copy(profileId = profileId)
        val id = repo.saveMedication(saved)
        ReminderScheduler.schedule(ctx, saved.copy(id = id))
    }

    fun deleteMedication(m: Medication) = viewModelScope.launch {
        ReminderScheduler.cancel(ctx, m)
        repo.deleteMedication(m)
    }

    fun setDoseTaken(m: Medication, dateKey: Int, time: String, taken: Boolean) = viewModelScope.launch {
        if (taken) {
            val inserted = repo.markTaken(DoseLog(medicationId = m.id, dateKey = dateKey, time = time, takenAt = System.currentTimeMillis()))
            if (inserted != -1L && m.pillsLeft != null) repo.saveMedication(m.copy(pillsLeft = (m.pillsLeft - 1).coerceAtLeast(0)))
        } else {
            repo.unmarkTaken(m.id, dateKey, time)
            if (m.pillsLeft != null) repo.saveMedication(m.copy(pillsLeft = m.pillsLeft + 1))
        }
    }

    fun saveReminder(r: Reminder) = viewModelScope.launch {
        val id = repo.saveReminder(r)
        ReminderScheduler.schedule(ctx, r.copy(id = id))
    }

    fun deleteReminder(r: Reminder) = viewModelScope.launch {
        ReminderScheduler.cancel(ctx, r)
        repo.deleteReminder(r)
    }

    // ---------------------------------------------------------------- export / import

    suspend fun buildPdf(days: Int?, options: ReportOptions): File? = withContext(Dispatchers.IO) {
        val p = activeProfile.value ?: return@withContext null
        val s = settings.value ?: AppSettings()
        val from = if (days == null) 0L else System.currentTimeMillis() - days * DAY_MS
        val list = readings.value.filter { it.timestamp >= from }
        Exporters.writePdf(ctx, p, list, medications.value, s.guideline, if (days == null) "All time" else "Last $days days", options)
    }

    suspend fun buildCsv(): File? = withContext(Dispatchers.IO) {
        val p = activeProfile.value ?: return@withContext null
        Exporters.writeCsv(ctx, p, readings.value)
    }

    /** Returns a human-readable result message. */
    suspend fun importCsv(uri: Uri): String = withContext(Dispatchers.IO) {
        val p = activeProfile.value ?: return@withContext "No profile selected"
        runCatching {
            val (list, skipped) = ctx.contentResolver.openInputStream(uri)!!.use { Exporters.parseCsv(it, p.id) }
            val existing = readings.value.map { it.timestamp to it.systolic }.toSet()
            val fresh = list.filter { (it.timestamp to it.systolic) !in existing }
            repo.insertReadings(fresh)
            buildString {
                append("Imported ${fresh.size} reading${if (fresh.size == 1) "" else "s"} into ${p.name}")
                val dup = list.size - fresh.size
                if (dup > 0) append(", $dup already existed")
                if (skipped > 0) append(", $skipped rows skipped")
            }
        }.getOrElse { "Couldn't read that file. Use a CSV with date, systolic and diastolic columns." }
    }
}
