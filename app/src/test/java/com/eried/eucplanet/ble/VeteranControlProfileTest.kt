package com.eried.eucplanet.ble

import com.eried.eucplanet.ble.nosfet.AeonControlProfile

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VeteranControlProfileTest {
    @Test
    fun `model boundary retains existing compatibility commands and capability exposure`() {
        for (model in listOf(null, VeteranModel.LYNX_S, VeteranModel.NOSFET_AEON)) {
            val protocol = VeteranModelProtocols.create(model)
            org.junit.Assert.assertEquals(
                WheelCapabilities.VETERAN.copy(hasLock = model != VeteranModel.NOSFET_AEON),
                protocol.capabilities
            )
            org.junit.Assert.assertArrayEquals(VeteranCommands.setTiltbackSpeed(40), protocol.controls.setTiltbackSpeed(40))
            org.junit.Assert.assertArrayEquals(VeteranCommands.setAlarmSpeed(35), protocol.controls.setAlarmSpeed(35))
            org.junit.Assert.assertArrayEquals(VeteranCommands.resetTrip(), protocol.controls.resetTrip())
            assertNull(protocol.controls.setVolume(20))
            assertNull(protocol.controls.setDRL(true))
        }
    }

    @Test
    fun `Aeon lock is unavailable at capability and command boundaries`() {
        val adapter = VeteranAdapter()
        adapter.setLock(true) // A pending generic command must not survive model identification.
        adapter.notifyConnectingTo("NOSFET Aeon")
        org.junit.Assert.assertFalse(adapter.capabilities.hasLock)
        assertNull(adapter.setLock(true))
        assertNull(adapter.setLock(false))
        assertNull(adapter.setLockFollowup(true))
        assertNull(adapter.setLockFollowup(false))
        adapter.onDisconnect()
        assertTrue(adapter.capabilities.hasLock)
        org.junit.Assert.assertNotNull(adapter.setLock(true))
    }

    @Test
    fun `only Aeon selects the Aeon control profile`() {
        assertSame(AeonControlProfile, VeteranModelProtocols.create(VeteranModel.NOSFET_AEON).controls)
        for (model in VeteranModel.entries.filter { it != VeteranModel.NOSFET_AEON } + null) {
            assertSame(DefaultVeteranControlProfile, VeteranModelProtocols.create(model).controls)
        }
    }

    @Test
    fun `Aeon profile exposes only single-write horn and light commands`() {
        assertTrue(AeonControlProfile.horn().contentEquals(VeteranCommands.horn()))
        assertNull(AeonControlProfile.hornFollowup())
        assertTrue(AeonControlProfile.setLight(true).contentEquals("SetLightON".toByteArray()))
        assertTrue(AeonControlProfile.setLight(false).contentEquals("SetLightOFF".toByteArray()))
        assertNull(AeonControlProfile.setLightFollowup(true))
        assertNull(AeonControlProfile.setLightFollowup(false))
    }

    @Test
    fun `disconnect resets adapter to default profile`() {
        val adapter = VeteranAdapter()
        adapter.notifyConnectingTo("NOSFET Aeon")
        assertTrue(adapter.setLight(true).contentEquals("SetLightON".toByteArray()))
        assertNull(adapter.setLightFollowup(true))

        adapter.onDisconnect()

        assertTrue(adapter.setLight(true).contentEquals(VeteranCommands.setHighBeam(true)))
        assertTrue(
            adapter.setLightFollowup(true)!!
                .contentEquals(VeteranCommands.setHighBeamCompanion(true))
        )
    }
}
