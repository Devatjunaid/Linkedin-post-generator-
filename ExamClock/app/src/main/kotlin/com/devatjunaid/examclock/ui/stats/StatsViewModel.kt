package com.devatjunaid.examclock.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devatjunaid.examclock.AppContainer
import com.devatjunaid.examclock.domain.Readiness
import com.devatjunaid.examclock.domain.Streaks
import com.devatjunaid.examclock.domain.TopicStatus
import com.devatjunaid.examclock.ui.components.Format
import com.devatjunaid.examclock.ui.home.ExamSummary
import com.devatjunaid.examclock.ui.home.summarize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

enum class StatsRange(val days: Int) { WEEK(7), MONTH(30) }

data class DayTotal(val date: LocalDate, val minutes: Int)

data class PaceWarning(val examTitle: String, val topicsPerDay: Float, val daysLeft: Long)

data class StatsState(
    val loading: Boolean = true,
    val range: StatsRange = StatsRange.WEEK,
    val days: List<DayTotal> = emptyList(),
    val totalMinutes: Int = 0,
    val goalMinutes: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val sessionsDone: Int = 0,
    val readiness: List<ExamSummary> = emptyList(),
    val warnings: List<PaceWarning> = emptyList(),
    val perExamMinutes: List<Pair<String, Int>> = emptyList(),
)

class StatsViewModel(container: AppContainer) : ViewModel() {
    private val repo = container.repository
    private val range = MutableStateFlow(StatsRange.WEEK)
    private val today = LocalDate.now()

    val state: StateFlow<StatsState> = combine(
        range,
        repo.observeLogsSince(today.minusDays(400)),
        repo.observeExams(),
        repo.observeAllTopics(),
        container.preferences.preferences,
    ) { r, logs, exams, topics, prefs ->
        val start = when (r) {
            StatsRange.WEEK -> today.with(TemporalAdjusters.previousOrSame(WeekFields.of(Locale.getDefault()).firstDayOfWeek))
            StatsRange.MONTH -> today.minusDays(29)
        }
        val byDay = logs.groupBy { Format.toLocalDate(it.startedAt) }.mapValues { (_, l) -> l.sumOf { it.minutes } }
        val days = (0 until r.days).map { start.plusDays(it.toLong()) }.map { DayTotal(it, byDay[it] ?: 0) }
        val inRange = logs.filter { !Format.toLocalDate(it.startedAt).isBefore(start) }
        val studyDays = byDay.filterValues { it > 0 }.keys
        val now = System.currentTimeMillis()
        val byExamTopics = topics.groupBy { it.examId }
        val upcoming = exams.filter { (it.examAt ?: 0L) > now }
        val titles = exams.associate { it.id to it.title }
        StatsState(
            loading = false,
            range = r,
            days = days,
            totalMinutes = inRange.sumOf { it.minutes },
            goalMinutes = prefs.dailyGoalMinutes * r.days,
            currentStreak = Streaks.current(studyDays, today),
            bestStreak = Streaks.best(studyDays),
            sessionsDone = inRange.mapNotNull { it.sessionId }.distinct().size,
            readiness = upcoming.map { summarize(it, byExamTopics[it.id].orEmpty()) },
            warnings = upcoming.mapNotNull { exam ->
                val statuses = byExamTopics[exam.id].orEmpty().map { TopicStatus.fromCode(it.status) }
                val date = Format.toLocalDate(exam.examAt!!)
                val perDay = Readiness.topicsPerDayNeeded(statuses, today, date) ?: return@mapNotNull null
                if (perDay > 1.5f) PaceWarning(exam.title, perDay, java.time.temporal.ChronoUnit.DAYS.between(today, date)) else null
            },
            perExamMinutes = inRange.groupBy { it.examId }
                .mapNotNull { (id, l) -> id?.let { titles[it] }?.let { it to l.sumOf { log -> log.minutes } } }
                .sortedByDescending { it.second }
                .take(6),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsState())

    fun setRange(r: StatsRange) {
        range.value = r
    }
}
