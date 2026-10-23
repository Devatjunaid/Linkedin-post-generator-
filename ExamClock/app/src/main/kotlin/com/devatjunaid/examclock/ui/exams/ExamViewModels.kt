package com.devatjunaid.examclock.ui.exams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devatjunaid.examclock.AppContainer
import com.devatjunaid.examclock.data.catalog.ExamCatalog
import com.devatjunaid.examclock.data.catalog.ExamTemplate
import com.devatjunaid.examclock.data.db.ExamEntity
import com.devatjunaid.examclock.data.db.TopicEntity
import com.devatjunaid.examclock.data.repo.StudyRepository
import com.devatjunaid.examclock.domain.TopicStatus
import com.devatjunaid.examclock.ui.components.Format
import com.devatjunaid.examclock.ui.home.ExamSummary
import com.devatjunaid.examclock.ui.home.summarize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class ExamListState(
    val loading: Boolean = true,
    val upcoming: List<ExamSummary> = emptyList(),
    val undated: List<ExamSummary> = emptyList(),
    val past: List<ExamSummary> = emptyList(),
)

class ExamListViewModel(container: AppContainer) : ViewModel() {
    val state: StateFlow<ExamListState> = combine(container.repository.observeExams(), container.repository.observeAllTopics()) { exams, topics ->
        val now = System.currentTimeMillis()
        val byExam = topics.groupBy { it.examId }
        val all = exams.map { summarize(it, byExam[it.id].orEmpty()) }
        ExamListState(
            loading = false,
            upcoming = all.filter { (it.exam.examAt ?: 0L) > now },
            undated = all.filter { it.exam.examAt == null },
            past = all.filter { it.exam.examAt != null && it.exam.examAt <= now }.reversed(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExamListState())
}

data class ExamDetailState(
    val loading: Boolean = true,
    val exam: ExamEntity? = null,
    val topics: List<TopicEntity> = emptyList(),
) {
    val statuses: List<TopicStatus> get() = topics.map { TopicStatus.fromCode(it.status) }
}

class ExamDetailViewModel(private val repo: StudyRepository, private val examId: Long) : ViewModel() {
    val state: StateFlow<ExamDetailState> = combine(repo.observeExam(examId), repo.observeTopics(examId)) { exam, topics ->
        ExamDetailState(loading = false, exam = exam, topics = topics)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExamDetailState())

    fun addTopics(text: String) = viewModelScope.launch { repo.addTopics(examId, text) }

    fun cycleStatus(topic: TopicEntity) = viewModelScope.launch {
        repo.updateTopic(topic.copy(status = TopicStatus.fromCode(topic.status).next().code))
    }

    fun setStatus(topic: TopicEntity, status: TopicStatus) = viewModelScope.launch { repo.updateTopic(topic.copy(status = status.code)) }
    fun setConfidence(topic: TopicEntity, value: Int) = viewModelScope.launch { repo.updateTopic(topic.copy(confidence = value.coerceIn(1, 5))) }
    fun rename(topic: TopicEntity, title: String) = viewModelScope.launch {
        if (title.isNotBlank()) repo.updateTopic(topic.copy(title = title.trim().take(120)))
    }
    fun deleteTopic(topic: TopicEntity) = viewModelScope.launch { repo.deleteTopic(topic) }

    /** The screen closes itself once the exam disappears from [state]. */
    fun deleteExam() = viewModelScope.launch {
        state.value.exam?.let { repo.deleteExam(it) }
    }
}

data class EditExamState(
    val loaded: Boolean = false,
    val id: Long = 0,
    val title: String = "",
    val group: String = "",
    val date: LocalDate? = null,
    val time: LocalTime = LocalTime.of(9, 0),
    val durationMinutes: String = "180",
    val location: String = "",
    val colorIndex: Int = 0,
    val notes: String = "",
    val templateId: String? = null,
    val createdAt: Long = 0,
    val saving: Boolean = false,
    val showTitleError: Boolean = false,
) {
    val isNew: Boolean get() = id == 0L
}

class EditExamViewModel(private val repo: StudyRepository, private val examId: Long?) : ViewModel() {
    private val _state = MutableStateFlow(EditExamState())
    val state: StateFlow<EditExamState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val exam = examId?.let { repo.exam(it) }
            _state.value = if (exam == null) {
                EditExamState(loaded = true, colorIndex = repo.examCount() % StudyRepository.SUBJECT_COLOR_COUNT)
            } else {
                EditExamState(
                    loaded = true,
                    id = exam.id,
                    title = exam.title,
                    group = exam.groupName,
                    date = exam.examAt?.let(Format::toLocalDate),
                    time = exam.examAt?.let(Format::toLocalTime) ?: LocalTime.of(9, 0),
                    durationMinutes = exam.durationMinutes.toString(),
                    location = exam.location,
                    colorIndex = exam.colorIndex,
                    notes = exam.notes,
                    templateId = exam.templateId,
                    createdAt = exam.createdAt,
                )
            }
        }
    }

    fun update(transform: (EditExamState) -> EditExamState) = _state.update { transform(it).copy(showTitleError = false) }

    /** Validates and saves; returns false (and shows the error) when the title is missing. */
    fun save(onSaved: (isNew: Boolean) -> Unit) {
        val s = _state.value
        if (s.saving) return
        if (s.title.isBlank()) {
            _state.update { it.copy(showTitleError = true) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            repo.saveExam(
                ExamEntity(
                    id = s.id,
                    title = s.title.trim(),
                    groupName = s.group.trim(),
                    examAt = s.date?.let { Format.millis(it, s.time) },
                    durationMinutes = s.durationMinutes.toIntOrNull()?.coerceIn(5, 720) ?: 180,
                    location = s.location.trim(),
                    colorIndex = s.colorIndex,
                    notes = s.notes.trim(),
                    templateId = s.templateId,
                    createdAt = s.createdAt,
                ),
            )
            onSaved(s.isNew)
        }
    }
}

data class CatalogState(
    val countryCode: String = "",
    val templates: List<ExamTemplate> = emptyList(),
    val selection: Map<String, Set<String>> = emptyMap(),
    val expanded: String? = null,
    val saving: Boolean = false,
) {
    val selectedCount: Int get() = selection.values.sumOf { it.size }
}

class CatalogViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(CatalogState())
    val state: StateFlow<CatalogState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            container.preferences.preferences.map { it.countryCode }.collect { code ->
                _state.update { it.copy(countryCode = code, templates = ExamCatalog.templatesFor(code)) }
            }
        }
    }

    fun toggle(template: ExamTemplate) = _state.update {
        val sel = it.selection.toMutableMap()
        if (sel.remove(template.id) == null) sel[template.id] = template.papers.toSet()
        it.copy(selection = sel, expanded = if (sel.containsKey(template.id)) template.id else it.expanded)
    }

    fun togglePaper(template: ExamTemplate, paper: String) = _state.update {
        val current = it.selection[template.id].orEmpty()
        val next = if (paper in current) current - paper else current + paper
        val sel = it.selection.toMutableMap()
        if (next.isEmpty()) sel.remove(template.id) else sel[template.id] = next
        it.copy(selection = sel)
    }

    fun expand(id: String) = _state.update { it.copy(expanded = if (it.expanded == id) null else id) }

    fun addSelected(onDone: (Int) -> Unit) {
        val s = _state.value
        if (s.saving || s.selectedCount == 0) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            var added = 0
            for ((id, papers) in s.selection) {
                val template = s.templates.firstOrNull { it.id == id } ?: continue
                added += container.repository.addFromTemplate(template, template.papers.filter { it in papers })
            }
            _state.update { it.copy(saving = false, selection = emptyMap()) }
            onDone(added)
        }
    }
}
