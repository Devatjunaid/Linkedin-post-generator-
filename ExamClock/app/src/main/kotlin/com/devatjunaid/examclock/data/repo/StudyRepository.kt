package com.devatjunaid.examclock.data.repo

import androidx.room.withTransaction
import com.devatjunaid.examclock.data.catalog.ExamCatalog
import com.devatjunaid.examclock.data.catalog.ExamTemplate
import com.devatjunaid.examclock.data.db.AppDatabase
import com.devatjunaid.examclock.data.db.ExamEntity
import com.devatjunaid.examclock.data.db.SessionEntity
import com.devatjunaid.examclock.data.db.SessionWithExam
import com.devatjunaid.examclock.data.db.StudyLogEntity
import com.devatjunaid.examclock.data.db.TopicEntity
import com.devatjunaid.examclock.data.prefs.PlanPreferences
import com.devatjunaid.examclock.domain.BusyBlock
import com.devatjunaid.examclock.domain.PlanExam
import com.devatjunaid.examclock.domain.PlanGenerator
import com.devatjunaid.examclock.domain.PlanResult
import com.devatjunaid.examclock.domain.PlanSettings
import com.devatjunaid.examclock.domain.PlanTopic
import com.devatjunaid.examclock.domain.TopicStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Single entry point for exams, topics, planned sessions and study time. */
class StudyRepository(private val db: AppDatabase) {

    private val exams = db.examDao()
    private val topics = db.topicDao()
    private val sessions = db.sessionDao()
    private val logs = db.studyLogDao()

    private val zone: ZoneId get() = ZoneId.systemDefault()

    // ---- Exams -------------------------------------------------------------------------------

    fun observeExams(): Flow<List<ExamEntity>> = exams.observeAll()
    fun observeExam(id: Long): Flow<ExamEntity?> = exams.observe(id)
    suspend fun exam(id: Long): ExamEntity? = exams.get(id)
    suspend fun upcomingExams(now: Long = System.currentTimeMillis()) = exams.upcoming(now)
    suspend fun examCount(): Int = exams.count()

    suspend fun saveExam(exam: ExamEntity): Long =
        if (exam.id == 0L) exams.insert(exam.copy(createdAt = System.currentTimeMillis())) else exam.id.also { exams.update(exam) }

    suspend fun deleteExam(exam: ExamEntity) = exams.delete(exam)

    /** Creates one undated exam per selected paper of a catalog template. Returns how many were added. */
    suspend fun addFromTemplate(template: ExamTemplate, papers: List<String>): Int {
        val now = System.currentTimeMillis()
        val existing = exams.count()
        db.withTransaction {
            papers.forEachIndexed { index, paper ->
                exams.insert(
                    ExamEntity(
                        title = paper,
                        groupName = template.name,
                        examAt = null,
                        durationMinutes = template.paperMinutes,
                        location = "",
                        colorIndex = (existing + index) % SUBJECT_COLOR_COUNT,
                        notes = "",
                        templateId = template.id,
                        createdAt = now,
                    ),
                )
            }
        }
        return papers.size
    }

    // ---- Topics ------------------------------------------------------------------------------

    fun observeTopics(examId: Long): Flow<List<TopicEntity>> = topics.observeForExam(examId)
    fun observeAllTopics(): Flow<List<TopicEntity>> = topics.observeAll()

    /** Adds one topic per non-blank line, keeping the order the user typed. */
    suspend fun addTopics(examId: Long, text: String): Int {
        val titles = text.lines().map { it.trim().trimStart('-', '•', '*').trim() }.filter { it.isNotEmpty() }.distinct()
        if (titles.isEmpty()) return 0
        val start = topics.maxPosition(examId) + 1
        val now = System.currentTimeMillis()
        topics.insertAll(
            titles.mapIndexed { i, title ->
                TopicEntity(examId = examId, title = title.take(120), status = TopicStatus.NOT_STARTED.code, confidence = 3, position = start + i, updatedAt = now)
            },
        )
        return titles.size
    }

    suspend fun updateTopic(topic: TopicEntity) = topics.update(topic.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deleteTopic(topic: TopicEntity) = topics.delete(topic)

    // ---- Sessions ----------------------------------------------------------------------------

    fun observeSessions(from: LocalDate, toExclusive: LocalDate): Flow<List<SessionWithExam>> =
        sessions.observeBetween(from.startMillis(), toExclusive.startMillis())

    suspend fun session(id: Long): SessionEntity? = sessions.get(id)

    suspend fun addSession(session: SessionEntity): Long = sessions.insert(session)

    suspend fun deleteSession(session: SessionEntity) = sessions.delete(session)

    /**
     * Ticking a session off counts its planned minutes as study time, minus any focus time already
     * logged for it; unticking removes only that manual entry, never real focus time.
     */
    suspend fun setSessionCompleted(session: SessionEntity, completed: Boolean) {
        db.withTransaction {
            sessions.update(session.copy(completed = completed))
            if (completed) {
                val missing = session.durationMinutes - logs.minutesForSession(session.id)
                if (missing > 0) {
                    logs.insert(StudyLogEntity(examId = session.examId, sessionId = session.id, startedAt = session.startAt, minutes = missing, manual = true))
                }
            } else {
                logs.deleteManualForSession(session.id)
            }
        }
    }

    /**
     * Records focus time. A linked session is marked done once the focus time logged for it covers
     * (almost) its planned length.
     */
    suspend fun logFocus(examId: Long?, sessionId: Long?, startedAt: Long, minutes: Int) {
        if (minutes <= 0) return
        db.withTransaction {
            logs.insert(StudyLogEntity(examId = examId, sessionId = sessionId, startedAt = startedAt, minutes = minutes))
            if (sessionId != null) {
                val session = sessions.get(sessionId)
                if (session != null && !session.completed && logs.minutesForSession(sessionId) >= session.durationMinutes * 9 / 10) {
                    sessions.update(session.copy(completed = true))
                }
            }
        }
    }

    fun observeLogsSince(day: LocalDate): Flow<List<StudyLogEntity>> = logs.observeSince(day.startMillis())

    suspend fun openSessionsOn(day: LocalDate): List<SessionEntity> =
        sessions.openBetween(day.startMillis(), day.plusDays(1).startMillis())

    // ---- Auto plan ---------------------------------------------------------------------------

    /**
     * Rebuilds the automatic plan from now on. Sessions the user added by hand and sessions already
     * completed are kept; the new plan is fitted around the manual ones.
     */
    suspend fun regeneratePlan(plan: PlanPreferences, countryCode: String): PlanResult {
        val now = LocalDateTime.now(zone)
        val today = now.toLocalDate()
        val nowMillis = now.atZone(zone).toInstant().toEpochMilli()

        val allTopics = topics.all().groupBy { it.examId }
        val planExams = exams.upcoming(nowMillis).mapNotNull { exam ->
            val date = exam.examAt?.toLocalDate() ?: return@mapNotNull null
            PlanExam(
                id = exam.id,
                title = exam.title,
                date = date,
                topics = allTopics[exam.id].orEmpty().map { PlanTopic(it.id, it.title, TopicStatus.fromCode(it.status), it.confidence) },
            )
        }

        val busy = sessions.manualFrom(today.startMillis()).groupBy({ it.startAt.toLocalDate() }) {
            val start = it.startAt.toLocalTime()
            BusyBlock(start, start.plusMinutes(it.durationMinutes.toLong()).let { end -> if (end < start) LocalTime.MAX else end })
        }

        val settings = PlanSettings(
            weekdayMinutes = plan.weekdayMinutes,
            weekendMinutes = plan.weekendMinutes,
            weekendDays = ExamCatalog.country(countryCode).weekendDays,
            restDays = plan.restDays,
            sessionMinutes = plan.sessionMinutes,
            breakMinutes = plan.breakMinutes,
            pastPaperDays = plan.pastPaperDays,
            dayStart = LocalTime.of(plan.dayStartMinute / 60, plan.dayStartMinute % 60),
            dayEnd = LocalTime.of((plan.dayEndMinute / 60).coerceAtMost(23), plan.dayEndMinute % 60),
        )
        val result = PlanGenerator().generate(today, now.toLocalTime(), planExams, settings, busy)

        val entities = result.sessions.map {
            SessionEntity(
                examId = it.examId,
                topicId = it.topicId,
                title = it.title,
                type = it.type.code,
                startAt = LocalDateTime.of(it.date, it.start).atZone(zone).toInstant().toEpochMilli(),
                durationMinutes = it.minutes,
                completed = false,
                autoGenerated = true,
            )
        }
        sessions.replaceAutoPlan(nowMillis, entities)
        return result
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) { db.clearAllTables() }

    private fun LocalDate.startMillis(): Long = atStartOfDay(zone).toInstant().toEpochMilli()
    private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
    private fun Long.toLocalTime(): LocalTime = Instant.ofEpochMilli(this).atZone(zone).toLocalTime()

    companion object {
        const val SUBJECT_COLOR_COUNT = 8
    }
}
