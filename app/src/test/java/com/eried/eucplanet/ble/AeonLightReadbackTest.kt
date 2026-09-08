package com.eried.eucplanet.ble

import com.eried.eucplanet.ble.nosfet.AeonCommands
import com.eried.eucplanet.ble.nosfet.AeonTelemetryDecoder
import com.eried.eucplanet.ble.nosfet.NosfetAeonProtocol

import com.eried.eucplanet.data.model.AeonLightState
import com.eried.eucplanet.data.model.AeonSetting
import com.eried.eucplanet.data.model.AeonSettingChange
import org.junit.Assert.*
import org.junit.Test
import java.util.zip.CRC32

class AeonLightReadbackTest {
    @Test fun `normal wheel preferences preserve the existing APK packets`() {
        val protocol = NosfetAeonProtocol()
        protocol.acceptFrame(settingsFrame())
        val pairs = listOf(
            com.eried.eucplanet.data.model.WheelPreference.DISPLAY_BRIGHTNESS to AeonSetting.DISPLAY_BRIGHTNESS,
            com.eried.eucplanet.data.model.WheelPreference.BUTTON_SOUND to AeonSetting.KEY_TONE,
            com.eried.eucplanet.data.model.WheelPreference.DISPLAY_UNITS to AeonSetting.UNITS,
        )
        pairs.forEach { (preference, setting) ->
            val expected = AeonCommands.setting(setting, 1)!!
            val actual = protocol.buildSettingChange(com.eried.eucplanet.data.model.WheelPreferenceChange(preference, 1))!!
            assertEquals(expected.size, actual.size)
            expected.zip(actual).forEach { (a, b) -> assertArrayEquals(a, b) }
            assertNull(protocol.buildSettingChange(com.eried.eucplanet.data.model.WheelPreferenceChange(preference, 101)))
        }
        val data = protocol.decorateTelemetry(com.eried.eucplanet.data.model.WheelData()).aeonSettings!!.preferences()
        assertEquals(3, data.values.size)
        assertEquals(30, data.values[com.eried.eucplanet.data.model.WheelPreference.DISPLAY_BRIGHTNESS])
        protocol.reset()
        assertNull(protocol.buildSettingChange(com.eried.eucplanet.data.model.WheelPreferenceChange(
            com.eried.eucplanet.data.model.WheelPreference.BUTTON_SOUND, 1)))
    }

    @Test fun `fresh positive SND selects old paired lights and zero restores silent path`() {
        val protocol = NosfetAeonProtocol()
        protocol.acceptFrame(settingsFrame(snd = 10))
        assertArrayEquals(VeteranCommands.setHighBeam(true), protocol.controls.setLight(true))
        assertArrayEquals(VeteranCommands.setHighBeamCompanion(true), protocol.controls.setLightFollowup(true))
        protocol.acceptFrame(settingsFrame(snd = 0))
        assertArrayEquals(VeteranCommands.setLight(false), protocol.controls.setLight(false))
        assertNull(protocol.controls.setLightFollowup(false))
        protocol.reset()
        assertArrayEquals(VeteranCommands.setLight(true), protocol.controls.setLight(true))
    }

    @Test fun `generic light readback preserves reception age and clears on reset`() {
        val protocol = NosfetAeonProtocol()
        val empty = com.eried.eucplanet.data.model.WheelData()
        assertNull(protocol.decorateTelemetry(empty).headlightReadback!!.level)
        protocol.acceptFrame(frame(3))
        val first = protocol.decorateTelemetry(empty).headlightReadback!!
        assertEquals(com.eried.eucplanet.data.model.HeadlightReadback.Level.HIGH, first.level)
        protocol.acceptFrame(settingsFrame())
        assertEquals(first, protocol.decorateTelemetry(empty).headlightReadback)
        protocol.reset()
        assertNull(protocol.decorateTelemetry(empty).headlightReadback!!.level)
        assertNull(com.eried.eucplanet.ble.DefaultVeteranProtocol().decorateTelemetry(empty).headlightReadback)
    }
    @Test fun `model components own isolated readbacks and session identities`() {
        val first = NosfetAeonProtocol()
        val second = NosfetAeonProtocol()
        val request = AeonSettingChange(AeonSetting.KEY_TONE, 2)
        first.acceptFrame(settingsFrame())
        assertNotNull(first.buildSettingChange(request))
        assertNull(second.buildSettingChange(request))
        val old = first.decorateTelemetry(com.eried.eucplanet.data.model.WheelData()).aeonSettings!!
        second.acceptFrame(settingsFrame())
        val other = second.decorateTelemetry(com.eried.eucplanet.data.model.WheelData()).aeonSettings!!
        assertNotEquals(old.sessionId, other.sessionId)
        first.reset()
        assertNull(first.buildSettingChange(request))
        assertNull(first.decorateTelemetry(com.eried.eucplanet.data.model.WheelData()).aeonSettings)
        first.acceptFrame(settingsFrame())
        assertNotEquals(old.sessionId, first.decorateTelemetry(com.eried.eucplanet.data.model.WheelData()).aeonSettings!!.sessionId)
    }

    @Test fun `name and telemetry detection preserve the same identified session`() {
        val adapter = VeteranAdapter()
        adapter.notifyConnectingTo("NOSFET Aeon")
        val first = adapter.onRawNotification(settingsFrame()).filterIsInstance<DecodeResult.Telemetry>().single().data.aeonSettings!!
        adapter.notifyConnectingTo("NOSFET Aeon")
        val next = adapter.onRawNotification(frame(1)).filterIsInstance<DecodeResult.Telemetry>().single().data.aeonSettings!!
        assertEquals(first, next)
    }

    @Test fun `model switching invalidates Aeon settings and partial lock commands`() {
        val adapter = VeteranAdapter()
        val old = adapter.onRawNotification(settingsFrame()).filterIsInstance<DecodeResult.Telemetry>().single().data.aeonSettings!!
        assertNull(adapter.setLock(true))
        assertFalse(adapter.capabilities.hasLock)
        val other = adapter.onRawNotification(frame(0, version = 9000)).filterIsInstance<DecodeResult.Telemetry>().single().data
        assertNull(other.aeonSettings)
        assertNull(other.aeonLightState)
        assertNull(adapter.setLockFollowup(true))
        assertNull(adapter.buildSettingChange(AeonSettingChange(AeonSetting.KEY_TONE, 2)))
        val back = adapter.onRawNotification(frame(0)).filterIsInstance<DecodeResult.Telemetry>().single().data
        assertNull(back.aeonSettings)
        val fresh = adapter.onRawNotification(settingsFrame()).filterIsInstance<DecodeResult.Telemetry>().single().data.aeonSettings!!
        assertNotEquals(old.sessionId, fresh.sessionId)
    }

    @Test fun `disconnect invalidates pending lock followup`() {
        val adapter = VeteranAdapter()
        adapter.setLock(true)
        adapter.onDisconnect()
        assertNull(adapter.setLockFollowup(true))
    }

    @Test fun `setting registry has unique offsets and exactly the intended first pass writes`() {
        assertEquals(13, AeonSetting.entries.size)
        assertEquals(13, AeonSetting.entries.map { it.offset }.toSet().size)
        assertTrue(AeonSetting.entries.all { it.offset in 47..70 && !it.range.isEmpty() })
        assertEquals(setOf(AeonSetting.DISPLAY_BRIGHTNESS, AeonSetting.KEY_TONE, AeonSetting.UNITS),
            AeonSetting.entries.filter { it.editable }.toSet())
    }

    @Test fun `corrupt settings never replace a valid snapshot`() {
        val adapter = VeteranAdapter()
        adapter.onRawNotification(settingsFrame())
        val damaged = settingsFrame().apply { this[55] = 20 }
        assertTrue(adapter.onRawNotification(damaged).isEmpty())
        val snapshot = adapter.onRawNotification(frame(0)).filterIsInstance<DecodeResult.Telemetry>().single().data.aeonSettings!!
        assertEquals(30, snapshot.value(AeonSetting.DISPLAY_BRIGHTNESS))
    }
    private fun settingsFrame(version: Int = 44250, snd: Int = 0): ByteArray {
        val bytes = ByteArray(75)
        bytes[0] = 0xdc.toByte(); bytes[1] = 0x5a; bytes[2] = 0x5c; bytes[3] = 71
        bytes[28] = (version shr 8).toByte(); bytes[29] = version.toByte(); bytes[46] = 8
        com.eried.eucplanet.data.model.AeonSetting.entries.forEach { bytes[it.offset] = 128.toByte() }
        bytes[55] = 30; bytes[63] = snd.toByte(); bytes[58] = 0; bytes[59] = (-15).toByte(); bytes[65] = 145.toByte()
        val crc = CRC32().apply { update(bytes, 0, 71) }.value
        for (i in 0..3) bytes[71 + i] = (crc shr (24 - 8 * i)).toByte()
        return bytes
    }

    @Test fun `settings preserve sentinels signed values and out of range raw bytes`() {
        val data = AeonTelemetryDecoder.settings(settingsFrame(), VeteranModel.NOSFET_AEON)!!
        assertEquals(30, data.value(AeonSetting.DISPLAY_BRIGHTNESS))
        assertEquals(-15, data.value(AeonSetting.VOLTAGE_ADJUSTMENT))
        assertNull(data.value(AeonSetting.PEDAL_HARDNESS))
        assertNull(data.value(AeonSetting.BRAKE_PRESSURE))
        assertEquals(145, data.rawValues[AeonSetting.BRAKE_PRESSURE])
        assertFalse(data.isFresh(data.receivedAtNanos + 8_000_000_001L))
        assertFalse(data.isFresh(data.receivedAtNanos - 1))
    }

    @Test fun `settings retain reception age between pages and reject other wheels`() {
        val adapter = VeteranAdapter()
        val first = adapter.onRawNotification(settingsFrame()).filterIsInstance<DecodeResult.Telemetry>().single().data.aeonSettings!!
        val next = adapter.onRawNotification(frame(0)).filterIsInstance<DecodeResult.Telemetry>().single().data.aeonSettings!!
        assertEquals(first, next)
        assertNotNull(adapter.buildSettingChange(AeonSettingChange(AeonSetting.KEY_TONE, 2)))
        adapter.onDisconnect()
        assertNull(adapter.buildSettingChange(AeonSettingChange(AeonSetting.KEY_TONE, 2)))
        assertNull(adapter.onRawNotification(frame(0)).filterIsInstance<DecodeResult.Telemetry>().single().data.aeonSettings)
        assertNull(AeonTelemetryDecoder.settings(settingsFrame(), VeteranModel.LYNX))
        assertNull(AeonTelemetryDecoder.settings(settingsFrame().copyOf(74), VeteranModel.NOSFET_AEON))
        assertNull(VeteranAdapter().apply { onRawNotification(settingsFrame(9000)) }.buildSettingChange(AeonSettingChange(AeonSetting.KEY_TONE, 2)))
    }

    @Test fun `new writes match APK layouts and split at 20 bytes without losing CRC`() {
        for ((key, size) in listOf(AeonSetting.DISPLAY_BRIGHTNESS to 20, AeonSetting.KEY_TONE to 28, AeonSetting.UNITS to 23)) {
            for (value in listOf(key.range.first, key.range.last)) {
                val chunks = AeonCommands.setting(key, value)!!
                assertTrue(chunks.all { it.size <= 20 })
                val packet = chunks.flatMap { it.toList() }.toByteArray()
                assertEquals(size, packet.size)
                assertArrayEquals(byteArrayOf(0x4c,0x64,0x41,0x70,size.toByte(),1,2),packet.copyOfRange(0,7))
                assertTrue(packet.copyOfRange(7,size-5).all { it == 0x80.toByte() })
                assertEquals(value, packet[size-5].toInt() and 255)
                val crc = CRC32().apply { update(packet,0,size-4) }.value
                for (i in 0..3) assertEquals((crc ushr (24-8*i)).toByte(),packet[size-4+i])
            }
        }
        assertNull(AeonCommands.setting(AeonSetting.KEY_TONE, -1))
        assertNull(AeonCommands.setting(AeonSetting.KEY_TONE, 101))
        assertNull(AeonCommands.setting(AeonSetting.UNITS, 2))
        assertNull(AeonCommands.setting(AeonSetting.GYRO, 1))
        assertNull(AeonCommands.setting(AeonSetting.BRAKE_PRESSURE, 100))
    }
    // Synthetic CRC-valid frames testing observed offsets, not raw captures.
    private fun frame(value: Int, page: Int = 1, version: Int = 44250): ByteArray {
        val bytes = ByteArray(87)
        bytes[0] = 0xdc.toByte(); bytes[1] = 0x5a; bytes[2] = 0x5c
        bytes[3] = 83
        bytes[28] = (version shr 8).toByte(); bytes[29] = version.toByte()
        bytes[46] = page.toByte(); bytes[49] = value.toByte()
        val crc = CRC32().apply { update(bytes, 0, 83) }.value
        for (i in 0..3) bytes[83 + i] = (crc shr (24 - 8 * i)).toByte()
        return bytes
    }

    @Test fun `physical cycle preserves all four levels through fragmented notifications`() {
        val adapter = VeteranAdapter()
        for (value in listOf(0, 1, 2, 3, 0)) {
            val results = frame(value).toList().chunked(20).flatMap {
                adapter.onRawNotification(it.toByteArray())
            }
            val data = results.filterIsInstance<DecodeResult.Telemetry>().single().data
            assertEquals(AeonLightState(value), data.aeonLightState)
            assertEquals(value != 0, data.lightOn)
        }
    }

    @Test fun `unknown values are not interpreted as a known level`() {
        val state = AeonTelemetryDecoder.lightState(frame(128), VeteranModel.NOSFET_AEON)!!
        assertEquals(128, state.rawValue)
        assertNull(state.level)
        assertNull(state.isOn)
    }

    @Test fun `unrelated pages models and truncated frames have no Aeon interpretation`() {
        assertNull(AeonTelemetryDecoder.lightState(frame(2, page = 5), VeteranModel.NOSFET_AEON))
        assertNull(AeonTelemetryDecoder.lightState(frame(2), VeteranModel.LYNX))
        assertNull(AeonTelemetryDecoder.lightState(frame(2).copyOf(50), VeteranModel.NOSFET_AEON))
    }

    @Test fun `readback persists between pages and clears on disconnect`() {
        val adapter = VeteranAdapter()
        adapter.onRawNotification(frame(3))
        val otherPage = adapter.onRawNotification(frame(0, page = 2))
            .filterIsInstance<DecodeResult.Telemetry>().single().data
        assertEquals(AeonLightState(3), otherPage.aeonLightState)
        adapter.onDisconnect()
        val reconnect = adapter.onRawNotification(frame(0, page = 2))
            .filterIsInstance<DecodeResult.Telemetry>().single().data
        assertNull(reconnect.aeonLightState)
        assertFalse(reconnect.lightOn)
    }

    @Test fun `non Aeon adapter does not adopt the light field`() {
        val data = VeteranAdapter().onRawNotification(frame(3, version = 5000))
            .filterIsInstance<DecodeResult.Telemetry>().single().data
        assertNull(data.aeonLightState)
        assertFalse(data.lightOn)
    }

    @Test fun `corrupted telemetry cannot update light state`() {
        val bytes = frame(3)
        bytes[49] = 2 // Deliberately leave the original CRC in place.
        assertTrue(VeteranAdapter().onRawNotification(bytes).isEmpty())
    }
}
