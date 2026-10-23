package com.driveready.testprep

import com.driveready.testprep.data.content.Countries
import com.driveready.testprep.data.content.Maneuvers
import com.driveready.testprep.data.content.QuestionBank
import com.driveready.testprep.data.content.UsStates
import com.driveready.testprep.data.content.questionsFor
import com.driveready.testprep.data.content.signsFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentIntegrityTest {
    @Test
    fun allFiftyStatesPlusDc() {
        assertEquals(51, UsStates.size)
        assertEquals(51, UsStates.map { it.id }.toSet().size)
        UsStates.forEach { assertTrue("${it.name} pass mark", it.passMark in 1..it.questions) }
    }

    @Test
    fun everyQuestionIsWellFormed() {
        assertEquals(QuestionBank.size, QuestionBank.map { it.id }.toSet().size)
        QuestionBank.forEach { q ->
            assertTrue(q.text, q.options.size >= 2)
            assertTrue(q.text, q.correctIndex in q.options.indices)
            assertEquals(q.text, q.options.size, q.options.toSet().size)
        }
    }

    @Test
    fun everyCountryHasCompleteContent() {
        val ids = Countries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        Countries.forEach { c ->
            assertTrue(c.name, c.oral.questions.isNotEmpty())
            assertTrue(c.name, c.practical.steps.isNotEmpty())
            assertTrue(c.name, c.syllabus.size >= 8)
            c.practical.maneuverIds.forEach { assertTrue("${c.name}: $it", it in Maneuvers) }
            val pool = questionsFor(c.id, c.drivingSide, c.speedUnit)
            assertTrue("${c.name} pool ${pool.size}", pool.size >= 50)
            assertTrue(c.name, signsFor(c).size >= 20)
            if (c.theory.questions > 0) assertTrue(c.name, c.theory.passMark <= c.theory.questions)
        }
    }

    @Test
    fun countrySpecificQuestionsReferenceKnownCountries() {
        val ids = Countries.map { it.id }.toSet()
        QuestionBank.flatMap { it.countries }.forEach { assertTrue(it, it in ids) }
    }

    @Test
    fun maneuversHaveStepsAndPoses() {
        Maneuvers.values.forEach { m ->
            assertTrue(m.id, m.steps.isNotEmpty())
            assertTrue(m.id, m.poses.size >= 2)
        }
    }
}
