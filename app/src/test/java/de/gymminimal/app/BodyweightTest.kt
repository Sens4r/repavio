package de.gymminimal.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyweightTest {
    private val start = 1_767_571_200_000L // Mo, 05.01.2026
    private val week = 7L * 24 * 60 * 60 * 1000
    private val band = Band("b1", "Rot", 20.0)

    private fun data(draftWeight: String): AppData {
        val plans = (0..6).map { DayPlan(it) }.toMutableList()
        plans[0] = DayPlan(0, "Rücken", listOf(Exercise(id = "t1", name = "Klimmzüge", weight = "0", reps = "8",
            muscleGroup = "Rücken")), enabled = true)
        val d = migrateExerciseIdentities(AppData(phase = Phase(name = "Kraft", weeks = 4, unitsPerWeek = 1),
            plans = plans, bands = listOf(band)))
        return d.copy(drafts = mapOf("t1" to Values(draftWeight, "8")))
    }

    @Test fun calisthenicsAreBodyweightByDefault() {
        assertTrue(isCalisthenics("Klimmzüge"))
        assertTrue(isCalisthenics(" muscle-ups "))
        assertFalse(isCalisthenics("Bankdrücken"))
        assertFalse(bodyweightFor("Klimmzüge", false))
        assertTrue(bodyweightFor("Bankdrücken", true))
    }

    @Test fun parsesBodyLoad() {
        assertEquals(BodyLoad(), parseBodyLoad("0"))
        assertEquals(12.5, parseBodyLoad("12,5")!!.extraKg, 0.0)
        assertEquals("b1", parseBodyLoad("band:b1")!!.bandId)
        assertNull(parseBodyLoad("abc"))
        assertNull(parseBodyLoad("band:"))
        assertEquals("EK − Rot 20 kg", bodyLoadLabel(BodyLoad(bandId = "b1"), listOf(band)))
        assertEquals("EK +10 kg", bodyLoadLabel(BodyLoad(extraKg = 10.0), emptyList()))
    }

    @Test fun bandIsSubtractedFromBodyweight() {
        val result = finishWorkout(data(bandLoad("b1")), 0, start + 3_600_000, bodyKg = 80.0, durationMs = 45 * 60_000L)!!
        val workout = result.history.single()
        val e = workout.entries.single()
        assertTrue(e.bodyweight)
        assertEquals(60.0, e.weight, 0.0)
        assertEquals(20.0, e.bandKg, 0.0)
        assertEquals("Rot", e.bandName)
        assertEquals(45 * 60_000L, workout.durationMs)
        // Nächster Vorschlag übernimmt das Band.
        assertEquals(Values(bandLoad("b1"), "8"), result.valueFor("t1", Values("0", "8")))
    }

    @Test fun extraWeightIsAdded() {
        val e = finishWorkout(data("10"), 0, start + 3_600_000, bodyKg = 80.0)!!.history.single().entries.single()
        assertEquals(90.0, e.weight, 0.0)
        assertEquals("EK +10 kg", loggedLoadLabel(e))
    }

    @Test fun withoutProfileWeightOnlyExtraCounts() {
        val r = finishWorkout(data("0"), 0, start + 3_600_000, bodyKg = null, durationMs = 5_000L)!!
        assertEquals(0.0, r.history.single().entries.single().weight, 0.0)
        assertNull(r.history.single().durationMs) // unter einer Minute wird nicht gespeichert
    }

    @Test fun predictionTrends() {
        assertNull(predictNext(listOf(60.0 to 8), 2.5))
        assertEquals(Prediction(60.0, 11, 1), predictNext(listOf(60.0 to 8, 60.0 to 9, 60.0 to 10), 2.5))
        assertEquals(Prediction(60.0, 10, 0), predictNext(listOf(60.0 to 10, 60.0 to 10), 2.5))
        assertEquals(Prediction(60.0, 6, -1), predictNext(listOf(60.0 to 10, 60.0 to 8), 2.5))
        assertEquals(Prediction(52.5, 11, 1), predictNext(listOf(50.0 to 8, 50.0 to 11), 2.5))
        assertEquals(Prediction(0.0, 14, 1), predictNext(listOf(0.0 to 10, 0.0 to 12), 0.0))
    }

    @Test fun predictionFromHistoryKeepsEkLoad() {
        var d = data(bandLoad("b1"))
        d = finishWorkout(d, 0, start + 3_600_000, bodyKg = 80.0)!!
        d = finishWorkout(d.copy(drafts = mapOf("t1" to Values(bandLoad("b1"), "10"))), 0, start + week + 3_600_000, bodyKg = 80.0)!!
        val id = d.plan(0).exercises.single().exerciseKey
        val p = predictionFor(d, id, bodyweight = true)!!
        assertEquals(0.0, p.weight, 0.0)
        assertEquals(1, p.trend)
        assertTrue(p.reps > 10)
        assertNull(predictionFor(d, id, bodyweight = false))
    }

    @Test fun tabsReorderAndHide() {
        var t = TabConfig()
        t = moveTab(t, TAB_NUTRITION, -1)
        assertEquals(listOf(0, 2, 1, 3), t.order)
        t = toggleTab(t, TAB_TRAINING)
        assertEquals(listOf(2, 1, 3), t.visible)
        assertEquals(t, toggleTab(t, TAB_PROFILE))
        assertEquals(t, decodeTabs(encodeTabs(t)))
        assertEquals(TabConfig(), decodeTabs("kaputt"))
        assertEquals(listOf(1, 0, 2, 3), decodeTabs("1;3").order)
        assertTrue(decodeTabs("1;3").hidden.isEmpty())
        assertEquals(moveTab(t, TAB_PROFILE, 1), t)
    }

    @Test fun durationFormatting() {
        assertEquals("45 min", durationText(45 * 60_000L))
        assertEquals("1 h 5 min", durationText(65 * 60_000L))
    }
}
