package com.devatjunaid.examclock.ui.onboarding

import android.content.Context
import android.telephony.TelephonyManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devatjunaid.examclock.AppContainer
import com.devatjunaid.examclock.data.catalog.Country
import com.devatjunaid.examclock.data.catalog.ExamCatalog
import com.devatjunaid.examclock.data.catalog.ExamTemplate
import com.devatjunaid.examclock.notifications.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class SetupState(
    val step: Int = 0,
    val detected: Country? = null,
    val countryCode: String = "",
    val query: String = "",
    /** templateId -> chosen papers */
    val selection: Map<String, Set<String>> = emptyMap(),
    val expanded: String? = null,
    val name: String = "",
    val dailyGoalMinutes: Int = 120,
    val remindersEnabled: Boolean = true,
    val saving: Boolean = false,
) {
    val country: Country get() = ExamCatalog.country(countryCode)
    val selectedPaperCount: Int get() = selection.values.sumOf { it.size }

    val filteredCountries: List<Country>
        get() {
            val q = query.trim()
            val list = ExamCatalog.countries + ExamCatalog.other
            return if (q.isEmpty()) list else list.filter { c ->
                c.name.contains(q, ignoreCase = true) || c.code.equals(q, ignoreCase = true) ||
                    c.templates.any { it.name.contains(q, ignoreCase = true) }
            }
        }
}

class SetupViewModel(private val container: AppContainer, context: Context) : ViewModel() {

    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(SetupState())
    val state: StateFlow<SetupState> = _state.asStateFlow()

    init {
        val detected = detectCountry(appContext)
        _state.update { it.copy(detected = detected, countryCode = detected?.code ?: "") }
    }

    fun setQuery(q: String) = _state.update { it.copy(query = q) }

    fun chooseCountry(code: String) = _state.update {
        if (it.countryCode == code) it else it.copy(countryCode = code, selection = emptyMap(), expanded = null)
    }

    fun toggleTemplate(template: ExamTemplate) = _state.update {
        val selection = it.selection.toMutableMap()
        if (selection.containsKey(template.id)) {
            selection.remove(template.id)
            it.copy(selection = selection, expanded = if (it.expanded == template.id) null else it.expanded)
        } else {
            selection[template.id] = template.papers.toSet()
            it.copy(selection = selection, expanded = template.id)
        }
    }

    fun togglePaper(template: ExamTemplate, paper: String) = _state.update {
        val current = it.selection[template.id].orEmpty()
        val next = if (paper in current) current - paper else current + paper
        val selection = it.selection.toMutableMap()
        if (next.isEmpty()) selection.remove(template.id) else selection[template.id] = next
        it.copy(selection = selection)
    }

    fun expand(id: String?) = _state.update { it.copy(expanded = if (it.expanded == id) null else id) }
    fun setName(name: String) = _state.update { it.copy(name = name.take(40)) }
    fun setGoal(minutes: Int) = _state.update { it.copy(dailyGoalMinutes = minutes) }
    fun setReminders(enabled: Boolean) = _state.update { it.copy(remindersEnabled = enabled) }
    fun goTo(step: Int) = _state.update { it.copy(step = step.coerceIn(0, 2)) }

    fun finish(onDone: () -> Unit) {
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val templates = ExamCatalog.templatesFor(s.countryCode)
            for ((id, papers) in s.selection) {
                val template = templates.firstOrNull { it.id == id } ?: continue
                container.repository.addFromTemplate(template, template.papers.filter { it in papers })
            }
            val prefs = container.preferences
            prefs.completeOnboarding(s.name, s.countryCode.ifEmpty { ExamCatalog.other.code }, s.dailyGoalMinutes)
            prefs.setReminders(s.remindersEnabled)
            ReminderScheduler.schedule(appContext, s.remindersEnabled, prefs.current().reminderMinuteOfDay)
            onDone()
        }
    }

    companion object {
        /** SIM / network country without any permission, falling back to the device locale. */
        fun detectCountry(context: Context): Country? {
            val tm = context.getSystemService(TelephonyManager::class.java)
            val candidates = listOfNotNull(tm?.simCountryIso, tm?.networkCountryIso, Locale.getDefault().country)
            return candidates
                .map { it.uppercase(Locale.ROOT) }
                .firstNotNullOfOrNull { code -> ExamCatalog.countries.firstOrNull { it.code == code } }
        }
    }
}
