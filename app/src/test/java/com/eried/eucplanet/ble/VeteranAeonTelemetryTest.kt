package com.eried.eucplanet.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import kotlin.math.abs

/** Regression fixture introduced by WheelLog's NOSFET Aeon support change. */
class VeteranAeonTelemetryTest {

    @Test
    fun `WheelLog Aeon fixture decodes through shared Veteran telemetry parser`() {
        val chunks = aeonFrameChunks()
        val frame = chunks.fold(ByteArray(0)) { frame, chunk -> frame + chunk }
        val reassembled = chunks.flatMap(VeteranParser()::feed)
        assertEquals(1, reassembled.size)
        assertEquals(true, frame.contentEquals(reassembled.single().bytes))
        val data = VeteranParser.parseTelemetry(frame, model = null)!!

        assertEquals(0f, abs(data.speed), 0.01f)
        assertEquals(143.20f, data.voltage, 0.01f)
        assertEquals(-0.80f, data.current, 0.01f)
        // WheelLog rounds this field to an integer (26); EUC Planet preserves
        // the frame's hundredths-of-a-degree precision.
        assertEquals(26.62f, data.maxTemperature, 0.01f)
        assertEquals(9.026f, data.tripDistance, 0.001f)
        // WheelLog stores this fixture as 9493 metres; WheelData uses km.
        assertEquals(9.493f, data.totalDistance, 0.001f)
        assertEquals(83, data.batteryPercent)
        assertEquals(-0.24f, data.pitchAngle, 0.01f)
        assertSame(VeteranModel.NOSFET_AEON, VeteranModel.fromMVer(VeteranParser.mVerOf(frame)))
        assertEquals(36, VeteranModel.NOSFET_AEON.seriesCells)
    }

    @Test
    fun `Aeon fixture selects Aeon profile through adapter telemetry path`() {
        val adapter = VeteranAdapter()
        val results = mutableListOf<DecodeResult>()
        aeonFrameChunks().forEach { chunk ->
            results += adapter.onRawNotification(chunk)
        }

        assertEquals(151, adapter.nominalPackVoltage)
        assertSame(
            VeteranModel.NOSFET_AEON,
            results.filterIsInstance<DecodeResult.ModelName>().single().model,
        )
        assertEquals("SetLightON", adapter.setLight(true).toString(Charsets.US_ASCII))
    }
}
