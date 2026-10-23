package com.devatjunaid.examclock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PoliciesTest {

    @Test
    fun bannerRetry_twoImmediateCallsThenFifteenSeconds() {
        val policy = AdRetryPolicy()
        // Call 1 fails -> call 2 goes out immediately.
        assertEquals(0L, policy.onFailure())
        // Call 2 fails -> wait 15 s, and keep waiting 15 s between further calls.
        assertEquals(15_000L, policy.onFailure())
        assertEquals(15_000L, policy.onFailure())
        policy.onSuccess()
        assertEquals(0L, policy.onFailure())
    }

    @Test
    fun interstitialCap_allowsOnePer30Seconds() {
        var now = 1_000L
        val cap = FrequencyCap(30_000L) { now }
        assertTrue(cap.canShow())
        cap.markShown()
        now += 29_999L
        assertFalse(cap.canShow())
        now += 1L
        assertTrue(cap.canShow())
    }

    @Test
    fun countdown_splitsRemainingTime() {
        val c = Countdown.between(0L, ((2 * 86_400L) + (3 * 3_600L) + (4 * 60L) + 5L) * 1000L)
        assertEquals(Countdown(2, 3, 4, 5), c)
        assertTrue(Countdown.between(10L, 0L).isOver)
    }

    @Test
    fun streaks() {
        val today = LocalDate.of(2026, 10, 23)
        val days = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(5), today.minusDays(6), today.minusDays(7))
        assertEquals(2, Streaks.current(days, today))
        assertEquals(3, Streaks.current(days + today, today))
        assertEquals(3, Streaks.best(days))
        assertEquals(0, Streaks.current(setOf(today.minusDays(3)), today))
    }

    @Test
    fun readiness() {
        assertEquals(0f, Readiness.score(emptyList()))
        assertEquals(0.5f, Readiness.score(listOf(TopicStatus.DONE, TopicStatus.NOT_STARTED)), 0.0001f)
        val perDay = Readiness.topicsPerDayNeeded(
            listOf(TopicStatus.NOT_STARTED, TopicStatus.LEARNING, TopicStatus.DONE),
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 5),
        )
        assertEquals(0.5f, perDay!!, 0.0001f)
    }
}
