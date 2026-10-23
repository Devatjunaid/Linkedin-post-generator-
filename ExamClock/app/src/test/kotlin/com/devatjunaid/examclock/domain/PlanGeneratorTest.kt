package com.devatjunaid.examclock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class PlanGeneratorTest {

    private val monday = LocalDate.of(2026, 10, 19)
    private val settings = PlanSettings(
        weekdayMinutes = 90,
        weekendMinutes = 180,
        weekendDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
        restDays = emptySet(),
        sessionMinutes = 45,
        breakMinutes = 10,
        pastPaperDays = 2,
        dayStart = LocalTime.of(16, 0),
    )

    private fun topics(examId: Long, count: Int, status: TopicStatus = TopicStatus.NOT_STARTED) =
        (1..count).map { PlanTopic(examId * 100 + it, "Topic $it", status) }

    @Test
    fun noUpcomingExams_givesEmptyPlan() {
        val past = PlanExam(1, "Physics", monday.minusDays(1), topics(1, 3))
        val result = PlanGenerator().generate(monday, null, listOf(past), settings)
        assertTrue(result.sessions.isEmpty())
    }

    @Test
    fun weekdayBudget_isSplitIntoSessionsWithBreaks() {
        val exam = PlanExam(1, "Physics", monday.plusDays(3), topics(1, 10))
        val result = PlanGenerator().generate(monday, null, listOf(exam), settings)
        val mondaySessions = result.sessions.filter { it.date == monday }
        assertEquals(2, mondaySessions.size)
        assertEquals(LocalTime.of(16, 0), mondaySessions[0].start)
        assertEquals(LocalTime.of(16, 55), mondaySessions[1].start)
        assertTrue(result.sessions.all { it.minutes == 45 })
    }

    @Test
    fun weekendDays_useWeekendBudget() {
        val exam = PlanExam(1, "Physics", monday.plusDays(14), topics(1, 30))
        val result = PlanGenerator().generate(monday, null, listOf(exam), settings)
        assertEquals(4, result.sessions.count { it.date == monday.plusDays(5) }) // Saturday: 180 / 45
    }

    @Test
    fun restDays_areSkipped() {
        val exam = PlanExam(1, "Physics", monday.plusDays(10), topics(1, 10))
        val rest = settings.copy(restDays = setOf(DayOfWeek.WEDNESDAY))
        val result = PlanGenerator().generate(monday, null, listOf(exam), rest)
        assertTrue(result.sessions.none { it.date.dayOfWeek == DayOfWeek.WEDNESDAY })
    }

    @Test
    fun nothingIsScheduledOnOrAfterTheExamDate() {
        val physics = PlanExam(1, "Physics", monday.plusDays(4), topics(1, 6))
        val chemistry = PlanExam(2, "Chemistry", monday.plusDays(9), topics(2, 6))
        val result = PlanGenerator().generate(monday, null, listOf(physics, chemistry), settings)
        assertTrue(result.sessions.filter { it.examId == 1L }.all { it.date.isBefore(physics.date) })
        assertTrue(result.sessions.filter { it.examId == 2L }.all { it.date.isBefore(chemistry.date) })
        assertTrue(result.sessions.any { it.examId == 1L } && result.sessions.any { it.examId == 2L })
    }

    @Test
    fun lastDaysBeforeExam_containPastPapers() {
        val exam = PlanExam(1, "Physics", monday.plusDays(10), topics(1, 4))
        val result = PlanGenerator().generate(monday, null, listOf(exam), settings)
        val window = result.sessions.filter { it.date >= exam.date.minusDays(2) }
        assertTrue(window.isNotEmpty())
        assertEquals(SessionType.PAST_PAPER, window.first().type)
        assertTrue(window.all { it.type == SessionType.PAST_PAPER || it.type == SessionType.REVISION })
    }

    @Test
    fun everyTopicIsLearnedAndRevised_whenThereIsEnoughTime() {
        val exam = PlanExam(1, "Physics", monday.plusDays(20), topics(1, 8))
        val result = PlanGenerator().generate(monday, null, listOf(exam), settings)
        assertTrue(result.uncoveredTopics.isEmpty())
        val learned = result.sessions.filter { it.type == SessionType.STUDY }.mapNotNull { it.topicId }.toSet()
        assertEquals(8, learned.size)
        exam.topics.forEach { topic ->
            val firstStudy = result.sessions.first { it.topicId == topic.id && it.type == SessionType.STUDY }
            assertTrue(result.sessions.any { it.topicId == topic.id && it.type == SessionType.REVISION && it.date > firstStudy.date })
        }
    }

    @Test
    fun notEnoughTime_reportsUncoveredTopics() {
        val exam = PlanExam(7, "Biology", monday.plusDays(2), topics(7, 20))
        val result = PlanGenerator().generate(monday, null, listOf(exam), settings)
        assertTrue((result.uncoveredTopics[7L] ?: 0) > 0)
    }

    @Test
    fun busyBlocks_areNotOverlapped() {
        val exam = PlanExam(1, "Physics", monday.plusDays(3), topics(1, 10))
        val busy = mapOf(monday to listOf(BusyBlock(LocalTime.of(16, 30), LocalTime.of(17, 30))))
        val result = PlanGenerator().generate(monday, null, listOf(exam), settings, busy)
        val starts = result.sessions.filter { it.date == monday }.map { it.start }
        assertEquals(listOf(LocalTime.of(17, 30), LocalTime.of(18, 25)), starts)
    }

    @Test
    fun today_startsAfterTheCurrentTime() {
        val exam = PlanExam(1, "Physics", monday.plusDays(3), topics(1, 10))
        val result = PlanGenerator().generate(monday, LocalTime.of(18, 7), listOf(exam), settings)
        assertEquals(LocalTime.of(18, 15), result.sessions.first { it.date == monday }.start)
    }

    @Test
    fun dayEnd_limitsLateSessions() {
        val exam = PlanExam(1, "Physics", monday.plusDays(3), topics(1, 10))
        val result = PlanGenerator().generate(monday, LocalTime.of(21, 30), listOf(exam), settings)
        assertTrue(result.sessions.none { it.date == monday })
    }

    @Test
    fun examWithoutTopics_stillGetsGeneralSessions() {
        val exam = PlanExam(3, "English", monday.plusDays(6), emptyList())
        val result = PlanGenerator().generate(monday, null, listOf(exam), settings)
        assertTrue(result.sessions.isNotEmpty())
        assertTrue(result.sessions.all { it.topicId == null })
    }
}
