package com.eried.eucplanet.ble

import com.eried.eucplanet.ble.nosfet.AeonAcknowledgementPolicy
import com.eried.eucplanet.ble.nosfet.AeonAcknowledgedControlProfile
import org.junit.Assert.*
import org.junit.Test

class AeonAcknowledgementPolicyTest {
    @Test fun `unknown stale and unsupported sound readback never requests an audible path`() {
        val key = com.eried.eucplanet.data.model.AeonSetting.KEY_TONE
        val settings = com.eried.eucplanet.data.model.AeonSettings(mapOf(key to 10), 100L)
        assertEquals(10, AeonAcknowledgementPolicy.soundLevel(settings, 100L))
        assertNull(AeonAcknowledgementPolicy.soundLevel(settings, 8_000_000_101L))
        assertNull(AeonAcknowledgementPolicy.soundLevel(settings, 99L))
        assertNull(AeonAcknowledgementPolicy.soundLevel(settings.copy(rawValues = mapOf(key to 128)), 100L))
        assertNull(AeonAcknowledgementPolicy.soundLevel(null, 100L))
    }
    @Test fun `SND selects a known variant but never invents missing silent commands`() {
        val audible = listOf(byteArrayOf(1))
        val silent = listOf(byteArrayOf(2))
        assertSame(audible, AeonAcknowledgementPolicy.select(10, audible, silent))
        listOf(null, 0, -1).forEach { assertSame(silent, AeonAcknowledgementPolicy.select(it, audible, silent)) }
        assertSame(audible, AeonAcknowledgementPolicy.select(0, audible, null))
    }

    @Test fun `light followup retains the decision taken for the first frame`() {
        var snd: Int? = 10
        val profile = AeonAcknowledgedControlProfile { snd }
        assertArrayEquals(VeteranCommands.setHighBeam(true), profile.setLight(true))
        snd = 0
        assertArrayEquals(VeteranCommands.setHighBeamCompanion(true), profile.setLightFollowup(true))
        assertArrayEquals(VeteranCommands.setLight(false), profile.setLight(false))
        snd = 10
        assertNull(profile.setLightFollowup(false))
    }

    @Test fun `reset drops pending light companion and horn is never silenced`() {
        val profile = AeonAcknowledgedControlProfile { 10 }
        profile.setLight(true)
        profile.reset()
        assertNull(profile.setLightFollowup(true))
        assertArrayEquals(VeteranCommands.horn(), profile.horn())
        assertNull(profile.hornFollowup())
    }
}
