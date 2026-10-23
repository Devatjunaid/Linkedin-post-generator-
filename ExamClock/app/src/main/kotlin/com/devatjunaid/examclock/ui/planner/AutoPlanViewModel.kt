package com.devatjunaid.examclock.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devatjunaid.examclock.AppContainer
import com.devatjunaid.examclock.data.catalog.ExamCatalog
import com.devatjunaid.examclock.data.prefs.PlanPreferences
import com.devatjunaid.examclock.domain.PlanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek

data class AutoPlanState(
    val loaded: Boolean = false,
    val plan: PlanPreferences = PlanPreferences(),
    val weekendDays: Set<DayOfWeek> = emptySet(),
    val datedExamCount: Int = 0,
    val undatedExamCount: Int = 0,
    val generating: Boolean = false,
    val result: PlanResult? = null,
    /** Exam id -> title, for the "won't fit" warning. */
    val examTitles: Map<Long, String> = emptyMap(),
)

class AutoPlanViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(AutoPlanState())
    val state: StateFlow<AutoPlanState> = _state.asStateFlow()
    private var countryCode = ""

    init {
        viewModelScope.launch {
            val prefs = container.preferences.current()
            countryCode = prefs.countryCode
            val exams = container.repository.observeExams().first()
            val now = System.currentTimeMillis()
            _state.value = AutoPlanState(
                loaded = true,
                plan = prefs.plan,
                weekendDays = ExamCatalog.country(prefs.countryCode).weekendDays,
                datedExamCount = exams.count { (it.examAt ?: 0L) > now },
                undatedExamCount = exams.count { it.examAt == null },
                examTitles = exams.associate { it.id to it.title },
            )
        }
    }

    fun update(transform: (PlanPreferences) -> PlanPreferences) = _state.update { it.copy(plan = transform(it.plan)) }

    fun toggleRestDay(day: DayOfWeek) = update { p ->
        p.copy(restDays = if (day in p.restDays) p.restDays - day else (p.restDays + day).takeIf { it.size < 7 } ?: p.restDays)
    }

    fun generate() {
        val s = _state.value
        if (s.generating) return
        _state.update { it.copy(generating = true) }
        viewModelScope.launch {
            container.preferences.setPlan(s.plan)
            val result = container.repository.regeneratePlan(s.plan, countryCode)
            _state.update { it.copy(generating = false, result = result) }
        }
    }

    fun dismissResult() = _state.update { it.copy(result = null) }
}
