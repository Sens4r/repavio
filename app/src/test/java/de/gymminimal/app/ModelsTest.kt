package de.gymminimal.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {
    private val week = 7L * 24 * 60 * 60 * 1000
    private val start = 1_767_571_200_000L // Mo, 05.01.2026 00:00 UTC

    private fun dataWith(phase: Phase): AppData {
        val plans = (0..6).map { DayPlan(it) }.toMutableList()
        plans[0] = DayPlan(0, "Brust", listOf(Exercise(name = "Bankdrücken", weight = "60", reps = "10",
            muscleGroup = "Brust")), enabled = true)
        return migrateExerciseIdentities(AppData(phase = phase, plans = plans))
    }

    @Test fun ilbTargetIsWeeksTimesDays() {
        val phase = Phase(name = "Hypertrophie", weeks = 6, unitsPerWeek = 3)
        assertEquals(18, phase.target)
        assertTrue(phase.isConfigured)
    }

    @Test fun openEndedPhaseHasNoTargetAndNeverCompletes() {
        val phase = Phase(name = NO_PHASE_NAME, weeks = 0, unitsPerWeek = 1, openEnded = true)
        assertTrue(phase.isConfigured)
        var data = dataWith(phase)
        repeat(5) { i -> data = finishWorkout(data, 0, start + i * week + 3_600_000)!! }
        assertEquals(5, data.completedInPhase)
        assertEquals(data, completePhase(data, start + 6 * week))
        assertNull(data.phase.finishedAt)
    }

    @Test fun openEndedPhaseKeepsOnePlanPerCalendarWeek() {
        val data = dataWith(Phase(name = NO_PHASE_NAME, weeks = 0, unitsPerWeek = 1, openEnded = true))
        val first = finishWorkout(data, 0, start + 3_600_000)!!
        assertNull(finishWorkout(first, 0, start + 2 * 3_600_000))
    }

    @Test fun ilbPhaseStopsAtTarget() {
        var data = dataWith(Phase(name = "Muskelkraft", weeks = 2, unitsPerWeek = 1))
        data = finishWorkout(data, 0, start + 3_600_000)!!
        data = finishWorkout(data, 0, start + week + 3_600_000)!!
        assertEquals(0, data.remainingInPhase)
        assertNull(finishWorkout(data, 0, start + 2 * week + 3_600_000))
        assertNotNull(completePhase(data, start + 2 * week).phase.finishedAt)
    }

    @Test fun switchingFromNoPhaseArchivesItAndClearsTiles() {
        val open = finishWorkout(dataWith(Phase(name = NO_PHASE_NAME, weeks = 0, unitsPerWeek = 1,
            openEnded = true)), 0, start + 3_600_000)!!
        val next = beginNewPhase(open, Phase(name = "Hypertrophie", weeks = 8, unitsPerWeek = 1), setOf(0))
        assertEquals(1, next.pastPhases.size)
        assertTrue(next.pastPhases.single().openEnded)
        assertTrue(next.plan(0).exercises.isEmpty())
        assertEquals(1, next.history.size)
        assertFalse(next.phase.openEnded)
    }

    @Test fun existingMuscleInferenceIsUnchanged() {
        assertEquals("Brust", inferredMuscleGroup("Bankdrücken"))
        assertEquals("Brust", inferredMuscleGroup("Liegestütze"))
        assertEquals("Rücken", inferredMuscleGroup("Klimmzüge"))
        assertEquals("Bizeps", inferredMuscleGroup("Hammer Curls"))
        assertEquals("Trizeps", inferredMuscleGroup("Ring Dips"))
        assertEquals("Beine", inferredMuscleGroup("Pistol Squats"))
    }

    @Test fun calisthenicsSuggestionsAndInference() {
        assertTrue("Australian Pull-ups" in EXERCISE_SUGGESTIONS)
        assertEquals("Rücken", inferredMuscleGroup("Australian Pull-ups"))
        assertEquals("Rücken", inferredMuscleGroup("Front Lever"))
        assertEquals("Schulter", inferredMuscleGroup("Pike Push-ups"))
        assertEquals("Brust", inferredMuscleGroup("Archer Push-ups"))
        assertEquals("Bauch", inferredMuscleGroup("L-Sit"))
    }
}
