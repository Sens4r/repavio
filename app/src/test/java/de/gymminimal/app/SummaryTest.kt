package de.gymminimal.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryTest {
    private fun logged(name: String, kg: Double, reps: Int, id: String = "k-$name", ek: Boolean = false) =
        LoggedExercise(originalId = id, performedId = id, name = name, status = "completed",
            weight = kg, reps = reps, sets = 3, muscleGroup = "Brust", bodyweight = ek)

    private fun data(): AppData {
        val phase = Phase(id = "p", name = "Muskelausdauer", weeks = 6, unitsPerWeek = 1)
        val bench = Exercise(id = "x", name = "Bankdrücken", weight = "70", reps = "18", exerciseKey = "k-Bankdrücken")
        val dips = Exercise(id = "d", name = "Dips", weight = "0", reps = "20", exerciseKey = "k-Dips", bodyweight = true)
        val plans = (0..6).map { DayPlan(it, enabled = it == 0, exercises = if (it == 0) listOf(bench, dips) else emptyList()) }
        val history = listOf(
            Workout(phaseId = "p", planDay = 0, timestamp = 1, entries = listOf(logged("Bankdrücken", 65.0, 18), logged("Dips", 80.0, 18, "k-Dips", true)),
                durationMs = 3_600_000),
            Workout(phaseId = "p", planDay = 0, timestamp = 2, entries = listOf(logged("Bankdrücken", 70.0, 18), logged("Dips", 80.0, 22, "k-Dips", true)),
                durationMs = 3_300_000)
        )
        return AppData(phase = phase, plans = plans, history = history)
    }

    @Test fun startWeightsFromOneRepMax() {
        val s = carryOverSuggestions(data(), "Hypertrophie", setOf(0))
        val bench = s.first { it.exercise.name == "Bankdrücken" }
        // 70 × 18 → 1RM 112; ÷ (1 + 10/30) = 84 → abgerundet 82,5
        assertEquals("82.5", bench.weight)
        assertEquals("10", bench.reps)
        val dips = s.first { it.exercise.name == "Dips" }
        assertTrue(dips.bodyweight)
        assertEquals("22", dips.reps)
        assertEquals("0", dips.weight)
    }

    @Test fun roundingRules() {
        assertEquals(17.0, roundStartWeight(17.9), 1e-9)
        assertEquals(82.5, roundStartWeight(84.9), 1e-9)
        assertEquals(20.0, roundStartWeight(21.0), 1e-9)
    }

    @Test fun newPhaseKeepsChosenExercises() {
        val d = data()
        val carry = carryOverSuggestions(d, "Muskelkraft", setOf(0)).filter { it.exercise.name == "Bankdrücken" }
        val next = beginNewPhase(d, Phase(name = "Muskelkraft", weeks = 6, unitsPerWeek = 1), setOf(0), 10, carry)
        val ex = next.plan(0).exercises.single()
        assertEquals("Bankdrücken", ex.name)
        assertEquals("5", ex.reps)
        assertEquals("k-Bankdrücken", ex.exerciseKey)
        assertTrue(beginNewPhase(d, Phase(name = "Muskelkraft", weeks = 6, unitsPerWeek = 1), setOf(0), 10).plan(0).exercises.isEmpty())
    }

    @Test fun notesByName() {
        val d = setNote(data(), "  Bankdrücken ", "Sitz   4 ")
        assertEquals("Sitz 4", noteFor(d, "bankdrücken"))
        assertNull(noteFor(setNote(d, "Bankdrücken", "  "), "Bankdrücken"))
        assertEquals(NOTE_MAX, setNote(d, "X", "a".repeat(500)).notes.getValue("x").length)
    }

    @Test fun summaryComparesWithLastSession() {
        val d = data()
        val s = workoutSummary(d, d.history.last())
        assertEquals(2, s.unit)
        assertEquals(6, s.target)
        assertEquals(-300_000L, s.durationDeltaMs)
        val bench = s.changes.first { it.name == "Bankdrücken" }
        assertEquals("+5 kg", bench.delta); assertTrue(bench.up)
        assertEquals("+4 Wdh.", s.changes.first { it.name == "Dips" }.delta)
        assertEquals("Dips", s.best?.name)  // 80 × 22 übertrifft den bisherigen Bestwert relativ am stärksten
    }

    @Test fun historyPerExercise() {
        val h = exerciseHistory(data(), "BANKDRÜCKEN")
        assertEquals(2, h.points.size)
        assertEquals(70.0, h.best!!.entry.weight, 1e-9)
    }

    @Test fun copyDayAddsEntriesAndWater() {
        val e = DiaryEntry("a", "2026-11-24", 0, "Haferflocken", 80.0, false, Nutrients(kcal = 300.0))
        val e2 = DiaryEntry("b", "2026-11-24", 2, "Reis", 100.0, false, Nutrients(kcal = 130.0))
        val log = FoodLog(entries = listOf(e, e2), water = mapOf("2026-11-24" to 2500))
        var n = 0
        val next = copyDay(log, "2026-11-24", "2026-11-25", setOf(0), water = true) { "n${n++}" }
        assertEquals(listOf("Haferflocken"), dayEntries(next, "2026-11-25").map { it.name })
        assertEquals(2500, next.water["2026-11-25"])
        assertEquals(log, copyDay(log, "2026-11-24", "2026-11-24", setOf(0), true) { "x" })
    }
}
