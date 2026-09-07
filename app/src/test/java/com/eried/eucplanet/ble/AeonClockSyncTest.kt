package com.eried.eucplanet.ble

import com.eried.eucplanet.ble.nosfet.AeonCommands
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import java.util.zip.CRC32

class AeonClockSyncTest {
    private fun calendar(zone: String, year: Int = 2026) = GregorianCalendar(TimeZone.getTimeZone(zone)).apply {
        clear()
        set(year, Calendar.AUGUST, 12, 22, 5, 15)
    }

    @Test fun `official capture matches exact frame including big endian CRC`() {
        val expected = "4c6441701200051a080c16050f02bac042a9".chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        assertArrayEquals(expected, AeonCommands.synchronizeClock(calendar("GMT+02:00")))
    }

    @Test fun `official encoding keeps local DST time but raw standard timezone`() {
        val summer = calendar("Europe/Riga")
        assertTrue(summer.get(Calendar.DST_OFFSET) != 0)
        val packet = AeonCommands.synchronizeClock(summer)!!
        assertEquals(22, packet[10].toInt())
        assertEquals(2, packet[13].toInt())
    }

    @Test fun `fractional and negative timezone offsets truncate toward zero`() {
        assertEquals(5, AeonCommands.synchronizeClock(calendar("GMT+05:45"))!![13].toInt())
        assertEquals(-3, AeonCommands.synchronizeClock(calendar("GMT-03:30"))!![13].toInt())
        assertEquals(0, AeonCommands.synchronizeClock(calendar("UTC"))!![13].toInt())
    }

    @Test fun `unrepresentable years skip instead of wrapping`() {
        assertNull(AeonCommands.synchronizeClock(calendar("UTC", 1999)))
        assertNull(AeonCommands.synchronizeClock(calendar("UTC", 2256)))
        assertEquals(0, AeonCommands.synchronizeClock(calendar("UTC", 2000))!![7].toInt())
        assertEquals(255, AeonCommands.synchronizeClock(calendar("UTC", 2255))!![7].toInt() and 255)
    }

    @Test fun `name match and partial or corrupt packets cannot start initialization`() {
        val adapter = VeteranAdapter()
        adapter.notifyConnectingTo("NOSFET Aeon")
        assertTrue(adapter.initSequence().isEmpty())
        assertTrue(adapter.pollRealtime().isEmpty())
        val corrupt = frame().apply { this[10] = (this[10].toInt() xor 1).toByte() }
        adapter.onRawNotification(corrupt)
        assertTrue(adapter.pollRealtime().isEmpty())
        adapter.onRawNotification(frame().copyOfRange(0, 20))
        assertTrue(adapter.pollRealtime().isEmpty())
    }

    @Test fun `valid Aeon data enables exactly one sync and disconnect rearms only after new data`() {
        val adapter = VeteranAdapter()
        adapter.onRawNotification(frame())
        val command = adapter.pollRealtime()
        assertEquals(18, command.size)
        assertArrayEquals(byteArrayOf(0x4c, 0x64, 0x41, 0x70, 18, 0, 5), command.copyOfRange(0, 7))
        repeat(3) {
            adapter.onRawNotification(frame())
            assertTrue(adapter.pollRealtime().isEmpty())
            assertTrue(adapter.pollSettings().isEmpty())
        }
        adapter.onDisconnect()
        adapter.notifyConnectingTo("NOSFET Aeon")
        assertTrue(adapter.pollRealtime().isEmpty())
        adapter.onRawNotification(frame())
        assertEquals(18, adapter.pollRealtime().size)
    }

    @Test fun `other models and a changed model discard pending Aeon startup`() {
        val adapter = VeteranAdapter()
        adapter.onRawNotification(frame())
        adapter.onRawNotification(frame(9000))
        assertTrue(adapter.pollRealtime().isEmpty())
        adapter.onRawNotification(frame())
        assertEquals(18, adapter.pollRealtime().size)
        assertTrue(adapter.pollRealtime().isEmpty())
    }

    private fun frame(version: Int = 44250): ByteArray {
        val bytes = aeonFrameChunks().flatMap { it.toList() }.toByteArray()
        bytes[28] = (version shr 8).toByte()
        bytes[29] = version.toByte()
        val crc = CRC32().apply { update(bytes, 0, bytes.size - 4) }.value
        for (i in 0..3) bytes[bytes.size - 4 + i] = (crc ushr (24 - i * 8)).toByte()
        return bytes
    }
}
