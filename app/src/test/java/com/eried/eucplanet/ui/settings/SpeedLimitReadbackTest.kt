package com.eried.eucplanet.ui.settings

import org.junit.Assert.*
import org.junit.Test

class SpeedLimitReadbackTest {
    @Test fun `fresh reported thresholds are returned unchanged`() {
        val state = SpeedLimitReadback(40f, 35f, 10_000L)
        assertTrue(state.hasReportedValues)
        assertEquals(40f to 35f, state.visibleValues(true, 11_000L, 9_000L))
    }

    @Test fun `missing and invalid values remain independently unknown`() {
        assertFalse(SpeedLimitReadback().hasReportedValues)
        assertEquals(40f to null, SpeedLimitReadback(40f, -1f, 100L).visibleValues(true, 100L, 0L))
        assertEquals(null to 35f, SpeedLimitReadback(Float.NaN, 35f, 100L).visibleValues(true, 100L, 0L))
        assertFalse(SpeedLimitReadback(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).hasReportedValues)
    }

    @Test fun `reported zero is not replaced by a preset or marked disabled`() {
        assertEquals(0f to 0f, SpeedLimitReadback(0f, 0f, 100L).visibleValues(true, 100L, 0L))
    }

    @Test fun `disconnected stale future and previous connection samples are hidden`() {
        val state = SpeedLimitReadback(40f, 35f, 10_000L)
        val unknown = Pair<Float?, Float?>(null, null)
        assertEquals(unknown, state.visibleValues(false, 10_000L, 9_000L))
        assertEquals(unknown, state.visibleValues(true, 13_001L, 9_000L))
        assertEquals(unknown, state.visibleValues(true, 9_999L, 9_000L))
        assertEquals(unknown, state.visibleValues(true, 10_001L, 10_001L))
        assertEquals(40f to 35f, state.visibleValues(true, 13_000L, 9_000L))
    }

    @Test fun `legal and restored telemetry replaces displayed pair without storing presets`() {
        val legal = SpeedLimitReadback(25f, 20f, 100L)
        val restored = SpeedLimitReadback(40f, 35f, 200L)
        assertEquals(25f to 20f, legal.visibleValues(true, 100L, 0L))
        assertEquals(40f to 35f, restored.visibleValues(true, 200L, 0L))
    }
}
