package de.gymminimal.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsTest {
    private val all = listOf(
        ReleaseNews("1.5.2", "A", listOf(NewsItem("food", "x", "y"))),
        ReleaseNews("1.6.0", "B", listOf(NewsItem("trend", "x", "y")))
    )

    @Test fun freshInstallShowsNothing() {
        assertTrue(newsToShow("1.5.2", null, updated = false, disabled = false, all = all).isEmpty())
    }

    @Test fun firstUpdateShowsCurrentOnly() {
        assertEquals(listOf("1.5.2"), newsToShow("1.5.2", null, true, false, all).map { it.version })
    }

    @Test fun bugfixVersionShowsNothing() {
        assertTrue(newsToShow("1.5.3", "1.5.2", true, false, all).isEmpty())
    }

    @Test fun skippedVersionsAreCombinedNewestFirst() {
        assertEquals(listOf("1.6.0", "1.5.2"), newsToShow("1.6.1", "1.5.0", true, false, all).map { it.version })
    }

    @Test fun shownOnlyOnceAndNeverWhenDisabled() {
        assertTrue(newsToShow("1.5.2", "1.5.2", true, false, all).isEmpty())
        assertTrue(newsToShow("1.6.0", "1.5.2", true, true, all).isEmpty())
    }

    @Test fun versionComparison() {
        assertTrue(compareVersions("1.10.0", "1.9.9") > 0)
        assertEquals(0, compareVersions("v1.5.2", "1.5.2"))
        assertTrue(RELEASE_NEWS.all { it.items.size in 1..6 })
    }
}
