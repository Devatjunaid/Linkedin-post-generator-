package com.devatjunaid.examclock.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class PlanTopic(
    val id: Long,
    val title: String,
    val status: TopicStatus,
    /** 1 (very weak) .. 5 (very confident). */
    val confidence: Int = 3,
)

data class PlanExam(
    val id: Long,
    val title: String,
    val date: LocalDate,
    val topics: List<PlanTopic>,
)

data class PlanSettings(
    val weekdayMinutes: Int,
    val weekendMinutes: Int,
    val weekendDays: Set<DayOfWeek>,
    val restDays: Set<DayOfWeek>,
    val sessionMinutes: Int,
    val breakMinutes: Int,
    /** The last N days before each exam are reserved for past papers and quick revision. */
    val pastPaperDays: Int,
    val dayStart: LocalTime,
    val dayEnd: LocalTime = LocalTime.of(22, 0),
)

data class PlannedSession(
    val date: LocalDate,
    val start: LocalTime,
    val minutes: Int,
    val examId: Long,
    val topicId: Long?,
    val type: SessionType,
    /** Topic title for topic sessions, exam title otherwise. */
    val title: String,
)

data class PlanResult(
    val sessions: List<PlannedSession>,
    /** Exam id -> number of topics that could not be scheduled before the exam. */
    val uncoveredTopics: Map<Long, Int>,
) {
    val totalMinutes: Int get() = sessions.sumOf { it.minutes }
}

/** A time range already taken on a given day (for example a session the user added by hand). */
data class BusyBlock(val start: LocalTime, val end: LocalTime)

/**
 * Builds a revision timetable from today until the last exam.
 *
 * - Each study day is split into fixed-length sessions with breaks, within the day's minute budget.
 * - Every session goes to the exam with the highest urgency (open work / days left); exams already
 *   picked today are discounted so one day mixes subjects instead of cramming one.
 * - Unstarted topics are learned first (in-progress ones, then weakest confidence), and each
 *   learned topic comes back for spaced revision after 1, 3, 7, 14 and 30 days while time allows.
 * - The final [PlanSettings.pastPaperDays] days before an exam alternate past papers and revision.
 * - Nothing is ever scheduled on or after an exam's own date for that exam.
 */
class PlanGenerator {

    private class Review(val topic: PlanTopic, val due: LocalDate, val stage: Int)

    private class ExamState(val exam: PlanExam) {
        val newTopics = ArrayDeque(
            exam.topics
                .filter { it.status == TopicStatus.LEARNING || it.status == TopicStatus.NOT_STARTED }
                .sortedWith(compareBy<PlanTopic>({ if (it.status == TopicStatus.LEARNING) 0 else 1 }, { it.confidence })),
        )
        val reviews = mutableListOf<Review>()
        var paperWindowSlots = 0
        var genericCount = 0

        fun work(day: LocalDate): Double {
            if (exam.topics.isEmpty()) return 1.0
            val dueSoon = reviews.count { !it.due.isAfter(day.plusDays(3)) }
            return newTopics.size + dueSoon * 0.4
        }
    }

    fun generate(
        today: LocalDate,
        now: LocalTime?,
        exams: List<PlanExam>,
        settings: PlanSettings,
        busy: Map<LocalDate, List<BusyBlock>> = emptyMap(),
    ): PlanResult {
        require(settings.sessionMinutes > 0) { "sessionMinutes must be positive" }
        val upcoming = exams.filter { it.date.isAfter(today) }.sortedBy { it.date }
        if (upcoming.isEmpty()) return PlanResult(emptyList(), emptyMap())

        val states = upcoming.map { exam ->
            ExamState(exam).also { state ->
                exam.topics
                    .filter { it.status == TopicStatus.REVISING || it.status == TopicStatus.DONE }
                    .sortedBy { it.confidence }
                    .forEachIndexed { index, topic ->
                        // Revising topics are due now; finished topics are spread over the first days.
                        val offset = if (topic.status == TopicStatus.REVISING) 0L else (index % 3).toLong()
                        state.reviews += Review(topic, today.plusDays(offset), stage = 1)
                    }
            }
        }

        val sessions = mutableListOf<PlannedSession>()
        val lastExamDate = upcoming.last().date
        val dayEndMinute = settings.dayEnd.toSecondOfDay() / 60
        var day = today
        while (day.isBefore(lastExamDate)) {
            if (day.dayOfWeek !in settings.restDays) {
                planDay(day, today, now, states, settings, busy[day].orEmpty(), dayEndMinute, sessions)
            }
            day = day.plusDays(1)
        }

        val uncovered = states.filter { it.newTopics.isNotEmpty() }.associate { it.exam.id to it.newTopics.size }
        return PlanResult(sessions, uncovered)
    }

    private fun planDay(
        day: LocalDate,
        today: LocalDate,
        now: LocalTime?,
        states: List<ExamState>,
        settings: PlanSettings,
        busy: List<BusyBlock>,
        dayEndMinute: Int,
        out: MutableList<PlannedSession>,
    ) {
        val active = states.filter { it.exam.date.isAfter(day) }
        if (active.isEmpty()) return

        val budget = if (day.dayOfWeek in settings.weekendDays) settings.weekendMinutes else settings.weekdayMinutes
        var cursor = settings.dayStart.toSecondOfDay() / 60
        if (day == today && now != null) {
            val nowMinute = now.toSecondOfDay() / 60
            cursor = maxOf(cursor, roundUp(nowMinute, 15))
        }
        val busyRanges = busy
            .map { (it.start.toSecondOfDay() / 60) until (it.end.toSecondOfDay() / 60) }
            .filterNot { it.isEmpty() }
        val length = settings.sessionMinutes
        val pickedToday = HashMap<Long, Int>()
        val reviewsToday = HashMap<Long, Int>()
        var used = 0

        while (used + length <= budget) {
            cursor = firstFreeStart(cursor, length, busyRanges)
            if (cursor + length > dayEndMinute) break

            val state = active.maxWith(
                compareBy<ExamState> { urgency(it, day, settings) / (1 + (pickedToday[it.exam.id] ?: 0)) }
                    .thenBy { -it.exam.date.toEpochDay() }, // tie: earlier exam wins
            )
            val session = nextSession(state, day, settings, reviewsToday)
                .copy(date = day, start = LocalTime.of(cursor / 60, cursor % 60), minutes = length)
            out += session
            pickedToday[state.exam.id] = (pickedToday[state.exam.id] ?: 0) + 1
            used += length
            cursor += length + settings.breakMinutes
        }
    }

    private fun urgency(state: ExamState, day: LocalDate, settings: PlanSettings): Double {
        val daysLeft = ChronoUnit.DAYS.between(day, state.exam.date).coerceAtLeast(1)
        val paperBoost = if (daysLeft <= settings.pastPaperDays) 2.0 else 0.0
        return (state.work(day) + paperBoost) / daysLeft
    }

    private fun nextSession(
        state: ExamState,
        day: LocalDate,
        settings: PlanSettings,
        reviewsToday: HashMap<Long, Int>,
    ): PlannedSession {
        val exam = state.exam
        val daysLeft = ChronoUnit.DAYS.between(day, exam.date)
        val inPaperWindow = daysLeft <= settings.pastPaperDays

        if (inPaperWindow) {
            // Alternate: past paper, quick revision of the weakest topic, past paper, ...
            val slot = state.paperWindowSlots++
            if (slot % 2 == 1) takeReview(state, day, dueOnly = false)?.let { return it }
            return template(exam, null, SessionType.PAST_PAPER, exam.title)
        }

        val reviewsDone = reviewsToday[exam.id] ?: 0
        val preferReview = state.newTopics.isEmpty() || reviewsDone < 2
        if (preferReview) {
            takeReview(state, day, dueOnly = true)?.let {
                reviewsToday[exam.id] = reviewsDone + 1
                return it
            }
        }

        state.newTopics.removeFirstOrNull()?.let { topic ->
            scheduleReview(state, topic, day, stage = 0)
            return template(exam, topic.id, SessionType.STUDY, topic.title)
        }

        takeReview(state, day, dueOnly = false)?.let { return it }

        // No topics recorded for this exam: alternate general study and practice papers.
        state.genericCount++
        return if (state.genericCount % 3 == 0) {
            template(exam, null, SessionType.PAST_PAPER, exam.title)
        } else {
            template(exam, null, SessionType.STUDY, exam.title)
        }
    }

    private fun takeReview(state: ExamState, day: LocalDate, dueOnly: Boolean): PlannedSession? {
        val candidates = if (dueOnly) state.reviews.filter { !it.due.isAfter(day) } else state.reviews
        val review = candidates.minWithOrNull(compareBy<Review>({ it.due }, { it.topic.confidence })) ?: return null
        state.reviews.remove(review)
        scheduleReview(state, review.topic, day, review.stage)
        return template(state.exam, review.topic.id, SessionType.REVISION, review.topic.title)
    }

    private fun scheduleReview(state: ExamState, topic: PlanTopic, day: LocalDate, stage: Int) {
        val interval = REVIEW_INTERVALS.getOrNull(stage) ?: return
        val due = day.plusDays(interval)
        if (due.isBefore(state.exam.date)) state.reviews += Review(topic, due, stage + 1)
    }

    private fun template(exam: PlanExam, topicId: Long?, type: SessionType, title: String) = PlannedSession(
        date = LocalDate.MIN,
        start = LocalTime.MIDNIGHT,
        minutes = 0,
        examId = exam.id,
        topicId = topicId,
        type = type,
        title = title,
    )

    private fun firstFreeStart(start: Int, length: Int, busy: List<IntRange>): Int {
        var cursor = start
        var moved = true
        while (moved) {
            moved = false
            for (range in busy) {
                if (cursor <= range.last && cursor + length > range.first) {
                    cursor = range.last + 1
                    moved = true
                }
            }
        }
        return cursor
    }

    private fun roundUp(value: Int, step: Int): Int = ((value + step - 1) / step) * step

    companion object {
        private val REVIEW_INTERVALS = listOf(1L, 3L, 7L, 14L, 30L)
    }
}
