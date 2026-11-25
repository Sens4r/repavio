package de.gymminimal.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WeighingTest {
    private fun at(day: LocalDate) = WeightEntry(day.atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 80.0)
    private val monday = LocalDate.of(2026, 11, 23)
    private val weekly = WeighConfig("weekly", 0)

    @Test fun noWeightsNoPrompt() {
        assertFalse(weighDue(weekly, emptyList(), monday, null))
    }

    @Test fun weeklyAsksOnChosenDayAndAfter() {
        val w = listOf(at(monday.minusDays(7)))
        assertTrue(weighDue(weekly, w, monday, null))
        assertTrue(weighDue(weekly, w, monday.plusDays(2), null))      // verpasst: fragt weiter
        assertFalse(weighDue(weekly, w, monday.minusDays(1), null))    // Sonntag davor: noch nicht fällig
    }

    @Test fun alreadyWeighedOrSnoozed() {
        assertFalse(weighDue(weekly, listOf(at(monday)), monday, null))
        assertFalse(weighDue(weekly, listOf(at(monday.minusDays(7))), monday, monday))
        assertTrue(weighDue(weekly, listOf(at(monday.minusDays(7))), monday.plusDays(1), monday))
    }

    @Test fun otherFrequencies() {
        val w = listOf(at(monday.minusDays(2)))
        assertTrue(weighDue(WeighConfig("daily"), w, monday, null))
        assertFalse(weighDue(WeighConfig("3d"), w, monday, null))
        assertTrue(weighDue(WeighConfig("3d"), w, monday.plusDays(1), null))
        assertFalse(weighDue(WeighConfig("off"), w, monday, null))
        assertFalse(weighDue(WeighConfig("2w", 0), listOf(at(monday.minusDays(7))), monday, null))
        assertTrue(weighDue(WeighConfig("2w", 0), listOf(at(monday.minusDays(14))), monday, null))
    }

    @Test fun configRoundTrip() {
        val c = WeighConfig("2w", 4)
        assertEquals(c, decodeWeighConfig(encodeWeighConfig(c)))
        assertEquals(WeighConfig(), decodeWeighConfig("quatsch:99"))
    }
}
