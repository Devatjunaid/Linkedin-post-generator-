package com.devatjunaid.dripdropdraw.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Plays every level twice: once with its hint line (must win, even when the line is drawn a
 * little off) and once without drawing anything (must not win, otherwise the level is trivial).
 */
class LevelSolvabilityTest {

    private fun play(level: LevelDef, strokes: List<List<Pt>>): GameSession.State {
        val s = GameSession(level)
        for (stroke in strokes) s.drawStroke(stroke)
        if (strokes.isEmpty()) s.startWater()
        var t = 0f
        while (t < 40f && (s.state == GameSession.State.READY || s.state == GameSession.State.RUNNING)) {
            s.step()
            t += World.STEP
        }
        return s.state
    }

    private fun shifted(h: List<List<Pt>>, dx: Float, dy: Float) = h.map { s -> s.map { Pt(it.x + dx, it.y + dy) } }

    @Test
    fun everyLevelIsSolvableWithItsHint() {
        for (n in 1..Levels.COUNT) {
            val level = Levels.get(n)
            assertEquals("level $n hint crosses a solid object", 0, LevelGenerator.blockedHintPoints(level))
            assertEquals("level $n hint", GameSession.State.WON, play(level, level.hint))
            assertEquals("level $n shifted hint", GameSession.State.WON, play(level, shifted(level.hint, 6f, -8f)))
            assertEquals("level $n shifted hint", GameSession.State.WON, play(level, shifted(level.hint, -6f, 8f)))
        }
    }

    @Test
    fun noLevelIsSolvedWithoutDrawing() {
        for (n in 1..Levels.COUNT) {
            assertNotEquals("level $n needs no drawing", GameSession.State.WON, play(Levels.get(n), emptyList()))
        }
    }

    @Test
    fun hintFitsInTheInk() {
        for (n in 1..Levels.COUNT) {
            val level = Levels.get(n)
            assert(level.hintLength < level.threeStarInk) { "level $n" }
        }
    }
}
