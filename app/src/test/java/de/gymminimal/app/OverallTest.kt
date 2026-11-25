package de.gymminimal.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverallTest {
    private fun e(name: String, kg: Double, reps: Int, group: String = "Brust", id: String = name) =
        LoggedExercise(originalId = id, performedId = id, name = name, status = "completed",
            weight = kg, reps = reps, sets = 3, muscleGroup = group)

    private fun data(): AppData {
        val a = Phase(id = "a", name = "Hypertrophie", weeks = 8, unitsPerWeek = 1, finishedAt = 10)
        val b = Phase(id = "b", name = "Muskelkraft", weeks = 6, unitsPerWeek = 1)
        // Neue Phase = neue Kachel-ID, gleiche Übung über den Namen.
        val history = listOf(
            Workout(phaseId = "a", planDay = 0, timestamp = 1, entries = listOf(e("Bankdrücken", 60.0, 10, id = "x1"))),
            Workout(phaseId = "a", planDay = 0, timestamp = 2, entries = listOf(e("Bankdrücken", 65.0, 10, id = "x1"))),
            Workout(phaseId = "b", planDay = 0, timestamp = 3, entries = listOf(e("bankdrücken ", 80.0, 5, id = "x2"))),
            Workout(phaseId = "b", planDay = 0, timestamp = 4, entries = listOf(e("Bankdrücken", 85.0, 5, id = "x2"),
                e("Kniebeugen", 100.0, 5, "Beine")))
        )
        return AppData(phase = b, plans = emptyList(), history = history, pastPhases = listOf(a))
    }

    @Test fun epleyKeepsSingles() {
        assertEquals(100.0, epley(100.0, 1), 1e-9)
        assertEquals(120.0, epley(100.0, 6), 1e-9)
    }

    @Test fun indexComparesOneRepMaxAcrossPhases() {
        val s = overallStats(data())
        assertEquals(4, s.workouts)
        assertEquals(2, s.phases)
        // 60×10 → 1RM 80; 85×5 → 1RM 99,17. Kniebeugen beginnt erst bei 100 %.
        val bench = 85.0 * (1 + 5 / 30.0) / 80.0 * 100
        assertEquals((bench + 100.0) / 2 - 100.0, s.gain!!, 1e-6)
    }

    @Test fun phaseTypesAndBest() {
        val s = overallStats(data())
        assertEquals(setOf("Hypertrophie", "Muskelkraft"), s.types.map { it.name }.toSet())
        assertTrue(s.types.all { it.phases == 1 && it.workouts == 2 })
        assertEquals("Kniebeugen", s.best.first().name)
        assertEquals(85.0, s.best.first { it.name == "Bankdrücken" }.weight, 1e-9)
    }

    @Test fun muscleGainNeedsTwoEntries() {
        val s = overallStats(data())
        assertNull(s.muscles.first { it.group == "Beine" }.gain)
        assertEquals(1, s.muscles.first { it.group == "Brust" }.exercises)
    }

    @Test fun thinOutKeepsEnds() {
        val t = thinOut((1..1000).toList(), 50)
        assertEquals(50, t.size); assertEquals(1, t.first()); assertEquals(1000, t.last())
    }
}
