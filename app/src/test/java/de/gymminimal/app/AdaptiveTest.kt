package de.gymminimal.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AdaptiveTest {
    private val end = LocalDate.of(2026, 11, 22) // Sonntag

    private fun days(n: Int) = (0 until n).map { end.minusDays((n - 1 - it).toLong()) }

    @Test fun notReadyWithoutEnoughData() {
        val w = days(5).map { it to 80.0 }
        val i = days(5).associateWith { 2500.0 }
        val a = estimateMaintenance(2300, w, i, end)
        assertFalse(a.ready)
        assertEquals(2300, a.estimatedKcal)
        assertNull(a.dataKcal)
        assertTrue(a.daysMissing > 0)
    }

    @Test fun stableWeightMeansIntakeIsMaintenance() {
        val w = days(28).map { it to 80.0 }
        val i = days(28).associateWith { 2500.0 }
        val a = estimateMaintenance(2300, w, i, end)
        assertTrue(a.ready)
        assertEquals(2500, a.dataKcal)
        assertEquals(2500, a.estimatedKcal)
        assertEquals(0.0, a.kgPerWeek!!, 0.001)
    }

    @Test fun losingWeightRaisesEstimate() {
        // 0,5 kg pro Woche weniger bei 2000 kcal: tatsächlich ca. 2550 kcal Verbrauch.
        val all = days(56)
        val w = all.mapIndexed { idx, d -> d to 80 - 0.5 / 7 * idx }
        val i = all.associateWith { 2000.0 }
        val a = estimateMaintenance(2400, w, i, end)
        assertEquals(2550.0, a.dataKcal!!.toDouble(), 30.0)
        assertEquals(-0.5, a.kgPerWeek!!, 0.05)
    }

    @Test fun unloggedDaysDoNotCountAndShiftIsLimited() {
        val w = days(28).map { it to 80.0 }
        // Nur jeder zweite Tag eingetragen, unrealistisch hoch: Korrektur bleibt in ±25 %.
        val i = days(28).filterIndexed { idx, _ -> idx % 2 == 0 }.associateWith { 6000.0 }
        val a = estimateMaintenance(2000, w, i, end)
        assertEquals(14, a.loggedDays)
        assertTrue(a.ready)
        assertEquals(2500, a.estimatedKcal)
    }

    @Test fun lessDataMeansLessCorrection() {
        val w = days(28).map { it to 80.0 }
        val i = days(28).takeLast(14).associateWith { 2600.0 } // 14 von 21 Tagen
        val a = estimateMaintenance(2300, w, i, end)
        assertEquals(2500, a.estimatedKcal) // 2300 + 14/21 × 300
    }

    @Test fun trendSmoothsWaterDays() {
        val t = weightTrend(listOf(end.minusDays(2) to 80.0, end.minusDays(1) to 82.0, end to 80.0))
        assertEquals(80.0, t[0].trend, 0.0)
        assertTrue(t[1].trend < 80.3)
        assertEquals(LocalDate.of(2026, 11, 16), weekStart(end))
        assertEquals(LocalDate.of(2026, 11, 23), weekStart(LocalDate.of(2026, 11, 23)))
    }

    @Test fun targetChangesOnlyWeekly() {
        // Profil komplett, 4 Wochen stabil bei 3000 kcal → angepasstes Ziel gilt ab dieser Woche.
        val today = LocalDate.of(2026, 11, 25) // Mittwoch
        val weights = (0 until 40).map { WeightEntry(java.time.ZonedDateTime.of(today.minusDays(it.toLong()).atTime(8, 0),
            java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), 80.0) }
        val profile = Profile(name = "T", birthDate = "1996-01-01", sex = "m", heightCm = 182, activity = "mittel",
            goal = "halten", weights = weights)
        var log = FoodLog()
        (0 until 40).forEach { d ->
            log = addEntry(log, quickEntry("x", Nutrients(kcal = 3000.0), 0, dayKey(today.minusDays(d.toLong())), "e$d")!!)
        }
        val target = nutritionTarget(profile, log, today)!!
        assertTrue(target.adaptive)
        assertEquals(3000, target.result.maintenance)
        assertEquals((3000 * nutritionGoal(profile.goal).factor).toInt(), target.result.kcal)
    }
}
