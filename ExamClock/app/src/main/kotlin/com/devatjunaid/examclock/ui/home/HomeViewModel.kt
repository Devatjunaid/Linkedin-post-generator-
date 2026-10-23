package com.devatjunaid.examclock.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devatjunaid.examclock.AppContainer
import com.devatjunaid.examclock.data.db.ExamEntity
import com.devatjunaid.examclock.data.db.SessionWithExam
import com.devatjunaid.examclock.data.db.TopicEntity
import com.devatjunaid.examclock.domain.Readiness
import com.devatjunaid.examclock.domain.Streaks
import com.devatjunaid.examclock.domain.TopicStatus
import com.devatjunaid.examclock.ui.components.Format
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ExamSummary(
    val exam: ExamEntity,
    val readiness: Float,
    val topicCount: Int,
    val doneCount: Int,
)

data class HomeState(
    val loading: Boolean = true,
    val name: String = "",
    val goalMinutes: Int = 120,
    val todayMinutes: Int = 0,
    val streak: Int = 0,
    val next: ExamSummary? = null,
    val upcoming: List<ExamSummary> = emptyList(),
    val undated: List<ExamEntity> = emptyList(),
    val today: List<SessionWithExam> = emptyList(),
    val hasAnyExam: Boolean = false,
)

fun summarize(exam: ExamEntity, topics: List<TopicEntity>): ExamSummary {
    val statuses = topics.map { TopicStatus.fromCode(it.status) }
    return ExamSummary(exam, Readiness.score(statuses), statuses.size, statuses.count { it == TopicStatus.DONE })
}

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.repository
    private val today = LocalDate.now()

    val state: StateFlow<HomeState> = combine(
        repo.observeExams(),
        repo.observeAllTopics(),
        repo.observeSessions(today, today.plusDays(1)),
        repo.observeLogsSince(today.minusDays(400)),
        container.preferences.preferences,
    ) { exams, topics, sessions, logs, prefs ->
        val now = System.currentTimeMillis()
        val byExam = topics.groupBy { it.examId }
        val dated = exams.filter { it.examAt != null && it.examAt > now }.map { summarize(it, byExam[it.id].orEmpty()) }
        val studyDays = logs.map { Format.toLocalDate(it.startedAt) }.toSet()
        HomeState(
            loading = false,
            name = prefs.userName,
            goalMinutes = prefs.dailyGoalMinutes,
            todayMinutes = logs.filter { Format.toLocalDate(it.startedAt) == today }.sumOf { it.minutes },
            streak = Streaks.current(studyDays, today),
            next = dated.firstOrNull(),
            upcoming = dated.drop(1),
            undated = exams.filter { it.examAt == null },
            today = sessions,
            hasAnyExam = exams.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun setCompleted(item: SessionWithExam, completed: Boolean) {
        viewModelScope.launch { repo.setSessionCompleted(item.session, completed) }
    }
}
