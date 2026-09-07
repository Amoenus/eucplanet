package com.eried.eucplanet.diagnostics

import com.eried.eucplanet.ble.nosfet.AeonControlProfile
import com.eried.eucplanet.ble.nosfet.NosfetAeonProtocol
import com.eried.eucplanet.ble.VeteranModel
import com.eried.eucplanet.ble.VeteranModelProtocols
import com.eried.eucplanet.ble.VeteranAdapter
import com.eried.eucplanet.ui.common.isAlphaWheel
import org.junit.Assert.*
import org.junit.Test

class AeonDiagnosticsTest {
    @Test fun `shared adapter exposes distinct stable catalogues without connection or state mutation`() {
        val adapter = VeteranAdapter()
        val before = adapter.diagnosticCatalogs { it.toString() }
        assertEquals(listOf("LeaperKim", "NOSFET"), before.map { it.displayName })
        assertEquals(before.size, before.map { it.displayName }.distinct().size)
        adapter.notifyConnectingTo("NOSFET Aeon")
        assertEquals(before, adapter.diagnosticCatalogs { it.toString() })
        assertNull(adapter.hornFollowup())
        adapter.onDisconnect()
        assertEquals(before, adapter.diagnosticCatalogs { it.toString() })
    }

    @Test fun `catalogue contains only profile light and horn commands with unique labels`() {
        val commands = AeonDiagnostics.commands { it.toString() }
        assertEquals("NOSFET", AeonDiagnostics.displayName)
        assertEquals(listOf("SetLightON", "SetLightOFF", "Horn_LkAp"), commands.map { it.label })
        assertEquals(commands.size, commands.map { it.label }.distinct().size)
        assertArrayEquals(AeonControlProfile.setLight(true), commands[0].bytes)
        assertArrayEquals(AeonControlProfile.setLight(false), commands[1].bytes)
        assertArrayEquals(AeonControlProfile.horn(), commands[2].bytes)
        assertNull(AeonControlProfile.hornFollowup())
        assertEquals(listOf(NosfetAeonProtocol.TRACE_PREFIX), AeonDiagnostics.inspectPrefixes)
        assertEquals("Aeon", shortInspectLabel(AeonDiagnostics.inspectPrefixes.single(), AeonDiagnostics.displayName))
    }

    @Test fun `model trace routing distinguishes Aeon from other Veteran wheels`() {
        for (model in VeteranModel.entries) {
            val prefix = VeteranModelProtocols.create(model).telemetryTracePrefix
            assertEquals(if (model == VeteranModel.NOSFET_AEON) "NOSFET Aeon" else "Veteran realtime", prefix)
        }
    }

    @Test fun `inspector accepts metadata between stream name and length without crossing models`() {
        val text = "NOSFET Aeon spd=0.0 bat=50 pg=8 len=3 body=dc 5a 5c"
        assertEquals(listOf(220, 90, 92), inspectTraceBytes(text, "NOSFET Aeon"))
        assertNull(inspectTraceBytes(text, "Veteran realtime"))
        assertNull(inspectTraceBytes(text, "NOSFET Ae"))
        assertEquals(listOf(220, 90, 92), inspectTraceBytes("Veteran realtime len=3 body=dc 5a 5c", "Veteran realtime"))
        assertNull(inspectTraceBytes("NOSFET Aeon len=3 body=dc xx 5c", "NOSFET Aeon"))
        assertNull(inspectTraceBytes("NOSFET Aeon len=1 body=100", "NOSFET Aeon"))
    }

    @Test fun `alpha label is limited to identified Aeon`() {
        assertTrue(isAlphaWheel("NOSFET Aeon"))
        assertTrue(isAlphaWheel(" nosfet aeon "))
        for (name in listOf(null, "", "NF7445", "NOSFET Apex", "Veteran Lynx", "InMotion V14")) {
            assertFalse(isAlphaWheel(name))
        }
    }
}
