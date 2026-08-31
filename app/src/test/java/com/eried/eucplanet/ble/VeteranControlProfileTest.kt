package com.eried.eucplanet.ble

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VeteranControlProfileTest {

    @Test
    fun `only Aeon selects the Aeon control profile`() {
        assertSame(AeonControlProfile, VeteranControlProfiles.forModel(VeteranModel.NOSFET_AEON))
        assertSame(DefaultVeteranControlProfile, VeteranControlProfiles.forModel(VeteranModel.LYNX_S))
        assertSame(DefaultVeteranControlProfile, VeteranControlProfiles.forModel(VeteranModel.NOSFET_APEX))
        assertSame(DefaultVeteranControlProfile, VeteranControlProfiles.forModel(null))
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
