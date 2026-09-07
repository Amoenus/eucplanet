package com.eried.eucplanet.data.model

import org.junit.Assert.*
import org.junit.Test

class LegalModeSpeedMemoryTest {
    private val normal = LegalModeSpeedMemory.Limits(50f, 40f)
    private val legal = LegalModeSpeedMemory.Limits(20f, 18f)

    @Test fun `temporary readbacks cannot replace previous normal values`() {
        val memory = LegalModeSpeedMemory()
        memory.enter("aeon", normal)
        memory.observe("aeon", 20f, 18f, 100)
        assertTrue(memory.protects("aeon"))
        assertEquals(normal, memory.restore("aeon", legal, 200))
    }

    @Test fun `repeated enable preserves the original pair`() {
        val memory = LegalModeSpeedMemory()
        memory.enter("aeon", normal)
        memory.enter("aeon", legal)
        assertEquals(normal, memory.restore("aeon", legal, 200))
    }

    @Test fun `missing or partial restoration readback retains previous values`() {
        val memory = LegalModeSpeedMemory()
        memory.enter("aeon", normal)
        memory.restore("aeon", legal, 200)
        memory.observe("aeon", 50f, 18f, 201)
        assertTrue(memory.protects("aeon"))
        memory.observe("aeon", -1f, 40f, 202)
        assertTrue(memory.protects("aeon"))
        memory.observe("aeon", Float.NaN, 40f, 203)
        assertTrue(memory.protects("aeon"))
        // No success callback or readback is required to retain and retry the restore point.
        assertEquals(normal, memory.restore("aeon", legal, 300))
    }

    @Test fun `late matching readback releases snapshot but an old frame does not`() {
        val memory = LegalModeSpeedMemory()
        memory.enter("aeon", normal)
        memory.restore("aeon", legal, 200)
        memory.observe("aeon", 50f, 40f, 199)
        assertTrue(memory.protects("aeon"))
        memory.observe("aeon", 50f, 40f, 201)
        assertFalse(memory.protects("aeon"))
        val updated = LegalModeSpeedMemory.Limits(60f, 45f)
        memory.enter("aeon", updated)
        assertEquals(updated, memory.restore("aeon", legal, 300))
    }

    @Test fun `reconnect to same wheel preserves snapshot and different wheels are isolated`() {
        val memory = LegalModeSpeedMemory()
        memory.enter("aeon", normal)
        assertFalse(memory.protects(null))
        assertFalse(memory.protects("other"))
        memory.enter("other", LegalModeSpeedMemory.Limits(70f, 60f))
        assertEquals(normal, memory.restore("aeon", legal, 200))
        assertEquals(LegalModeSpeedMemory.Limits(70f, 60f), memory.restore("other", legal, 200))
    }

    @Test fun `re-enable after failed restoration still retains the first snapshot`() {
        val memory = LegalModeSpeedMemory()
        memory.enter("aeon", normal)
        assertEquals(true, memory.requestedLegal("aeon"))
        memory.restore("aeon", legal, 200)
        assertEquals(false, memory.requestedLegal("aeon"))
        memory.enter("aeon", legal)
        assertEquals(true, memory.requestedLegal("aeon"))
        assertEquals(normal, memory.restore("aeon", legal, 300))
    }

    @Test fun `without a snapshot restoration falls back to saved normal settings`() {
        val memory = LegalModeSpeedMemory()
        assertNull(memory.requestedLegal("aeon"))
        assertEquals(normal, memory.restore("aeon", normal, 200))
    }
}
