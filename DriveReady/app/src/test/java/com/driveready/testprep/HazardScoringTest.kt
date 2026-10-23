package com.driveready.testprep

import com.driveready.testprep.quiz.HazardScoring
import org.junit.Assert.assertEquals
import org.junit.Test

class HazardScoringTest {
    private val develop = 6f

    @Test
    fun earlyTapScoresFive() = assertEquals(5, HazardScoring.score(listOf(6.2f), develop))

    @Test
    fun lateTapScoresOne() = assertEquals(1, HazardScoring.score(listOf(10.5f), develop))

    @Test
    fun tapBeforeHazardScoresZero() = assertEquals(0, HazardScoring.score(listOf(3f), develop))

    @Test
    fun bestTapInWindowCounts() = assertEquals(4, HazardScoring.score(listOf(2f, 7.4f, 9f), develop))

    @Test
    fun patternClickingScoresZero() =
        assertEquals(0, HazardScoring.score(List(9) { 6f + it * 0.3f }, develop))

    @Test
    fun passMarkMatchesUkRatio() = assertEquals(18, HazardScoring.passMark(6))
}
