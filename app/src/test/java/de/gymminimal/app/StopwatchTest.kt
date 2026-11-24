package de.gymminimal.app

import org.junit.Assert.assertEquals
import org.junit.Test

class StopwatchTest {
    @Test fun defaultBeeps() {
        val c = StopwatchConfig()
        assertEquals(0, stopwatchBeeps(0, c))
        assertEquals(1, stopwatchBeeps(1, c))
        assertEquals(1, stopwatchBeeps(4, c))
        assertEquals(2, stopwatchBeeps(5, c))   // double wins when both fall together
        assertEquals(1, stopwatchBeeps(6, c))
        assertEquals(2, stopwatchBeeps(10, c))
    }

    @Test fun configurableAndOff() {
        val c = StopwatchConfig(singleEvery = 2, doubleEvery = 0)
        assertEquals(0, stopwatchBeeps(1, c))
        assertEquals(1, stopwatchBeeps(2, c))
        assertEquals(0, stopwatchBeeps(5, c))
        assertEquals(0, stopwatchBeeps(5, StopwatchConfig(sound = false)))
        assertEquals(0, stopwatchBeeps(3, StopwatchConfig(singleEvery = 0, doubleEvery = 0)))
    }

    @Test fun format() {
        assertEquals("00:00:00", formatStopwatch(0))
        assertEquals("00:12:47", formatStopwatch(767_000))
        assertEquals("01:00:01", formatStopwatch(3_601_999))
        assertEquals("00:00:00", formatStopwatch(-5))
    }

    @Test fun summary() {
        assertEquals("Piep alle 1 min · doppelt alle 5 min", stopwatchSummary(StopwatchConfig()))
        assertEquals("Ton aus", stopwatchSummary(StopwatchConfig(sound = false)))
        assertEquals("Keine Pieptöne", stopwatchSummary(StopwatchConfig(0, 0)))
    }
}
