package com.devatjunaid.examclock.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object Readiness {
    /** 0..1 score: every topic contributes its [TopicStatus.weight]. No topics means 0. */
    fun score(statuses: List<TopicStatus>): Float =
        if (statuses.isEmpty()) 0f else statuses.sumOf { it.weight.toDouble() }.toFloat() / statuses.size

    /**
     * Topics that still need to be finished per remaining study day so that everything is
     * at least "revising" one day before the exam. Returns null when the exam is today/past.
     */
    fun topicsPerDayNeeded(statuses: List<TopicStatus>, today: LocalDate, examDate: LocalDate): Float? {
        val days = ChronoUnit.DAYS.between(today, examDate)
        if (days <= 0) return null
        val open = statuses.count { it == TopicStatus.NOT_STARTED || it == TopicStatus.LEARNING }
        return open.toFloat() / days
    }
}

object Streaks {
    /** Consecutive study days ending today, or ending yesterday when today has no study yet. */
    fun current(studyDays: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in studyDays) today else today.minusDays(1)
        var count = 0
        while (day in studyDays) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    fun best(studyDays: Set<LocalDate>): Int {
        var best = 0
        for (day in studyDays) {
            if (day.minusDays(1) in studyDays) continue // not the start of a run
            var length = 0
            var d = day
            while (d in studyDays) {
                length++
                d = d.plusDays(1)
            }
            if (length > best) best = length
        }
        return best
    }
}
