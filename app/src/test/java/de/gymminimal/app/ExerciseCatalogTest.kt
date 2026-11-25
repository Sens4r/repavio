package de.gymminimal.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseCatalogTest {
    @Test fun everySuggestionHasInfo() {
        val missing = EXERCISE_SUGGESTIONS.filter { exerciseInfo(it) == null }
        assertTrue("Ohne Übungsinfo: $missing", missing.isEmpty())
    }

    @Test fun entriesAreComplete() {
        EXERCISE_INFOS.values.forEach { info ->
            assertTrue(info.name, info.primary.isNotEmpty())
            assertTrue(info.name, info.steps.size >= 3 && info.steps.all { it.isNotBlank() })
            assertTrue(info.name, info.errors.size >= 2 && info.errors.all { it.isNotBlank() })
            (info.primary + info.secondary).forEach { assertNotNull("${info.name}: $it", MUSCLE_REGIONS[it]) }
        }
    }

    @Test fun lookupIgnoresCaseAndSpaces() {
        assertEquals("Bankdrücken", exerciseInfo("  bankdrücken ")?.name)
        assertNull(exerciseInfo("Meine Spezialübung"))
    }

    @Test fun aliasKeepsOwnName() {
        val info = exerciseInfo("Preacher Curls")!!
        assertEquals("Preacher Curls", info.name)
        assertEquals(exerciseInfo("Scottcurls")!!.steps, info.steps)
    }

    @Test fun primaryWinsOverSecondary() {
        val levels = regionLevels(exerciseInfo("Bankdrücken")!!)
        assertEquals(2, levels["Brust"])
        assertEquals(1, levels["Trizeps"])
        assertEquals(1, levels["Schulter"])
    }
}
