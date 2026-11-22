package de.gymminimal.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProfileTest {
    private val today = LocalDate.of(2026, 11, 22)
    private fun ms(date: LocalDate, hour: Int = 9): Long =
        date.atTime(hour, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val james = Profile(name = "James", birthDate = "1996-05-10", sex = "m", heightCm = 182,
        activity = "mittel", goal = "aufbau", weights = listOf(WeightEntry(ms(today), 80.0)))

    @Test fun ageIsDerivedFromBirthDate() {
        assertEquals(30, ageOn("1996-05-10", today))
        assertEquals(29, ageOn("1996-11-23", today))
        assertEquals(30, ageOn("1996-11-22", today))
        assertNull(ageOn("", today))
        assertNull(ageOn("2030-01-01", today))
    }

    @Test fun birthDateParsing() {
        assertEquals("1996-05-10", parseBirthDate("10.05.1996", today))
        assertNull(parseBirthDate("31.02.1996", today))
        assertNull(parseBirthDate("01.01.2020", today)) // younger than 10
        assertEquals("10.05.1996", formatBirthDate("1996-05-10"))
    }

    @Test fun mifflinStJeorMale() {
        val n = nutritionFor(james, today)!!
        assertEquals(1793, n.bmr)          // 800 + 1137,5 − 150 + 5 = 1792,5
        assertEquals(2778, n.maintenance)  // × 1,55
        assertEquals(3056, n.kcal)         // + 10 %
        assertEquals(160, n.proteinG)
        assertEquals(72, n.fatG)
        assertEquals(442, n.carbsG)        // (3056 − 640 − 648) / 4
    }

    @Test fun mifflinStJeorFemaleAndGoals() {
        val p = Profile(birthDate = "1996-05-10", sex = "w", heightCm = 165, activity = "wenig", goal = "halten",
            weights = listOf(WeightEntry(ms(today), 60.0)))
        val n = nutritionFor(p, today)!!
        assertEquals(1320, n.bmr)          // 600 + 1031,25 − 150 − 161 = 1320,25
        assertEquals(1584, n.kcal)         // × 1,2
        val cut = nutritionFor(p.copy(goal = "definition"), today)!!
        assertEquals(1267, cut.kcal)       // 1584,3 × 0,8 = 1267,4
    }

    @Test fun missingFieldsBlockCalculation() {
        val empty = Profile()
        assertNull(nutritionFor(empty, today))
        assertEquals(listOf("Geburtsdatum", "Geschlecht", "Größe", "Gewicht"), missingForNutrition(empty, today))
    }

    @Test fun weightLogReplacesSameDayAndUsesLatest() {
        var p = Profile()
        p = addWeight(p, 81.24, ms(today.minusDays(7)))!!
        p = addWeight(p, 80.6, ms(today, 8))!!
        p = addWeight(p, 80.2, ms(today, 20))!!
        assertEquals(2, p.weights.size)
        assertEquals(80.2, p.currentWeight!!, 0.0001)
        assertEquals(81.2, p.weights.first().kg, 0.0001)
        assertNull(addWeight(p, 10.0, ms(today)))
        p = deleteWeight(p, p.weights.last().timestamp)
        assertEquals(81.2, p.currentWeight!!, 0.0001)
    }

    @Test fun nutritionUsesNewestWeight() {
        val heavier = addWeight(james, 90.0, ms(today.plusDays(1)))!!
        assertTrue(nutritionFor(heavier, today.plusDays(1))!!.proteinG == 180)
    }
}
