package com.eried.eucplanet.ble.nosfet

import com.eried.eucplanet.data.model.AeonSetting
import java.util.zip.CRC32
import java.util.Calendar

/** Exact NOSFET 1.1.3 setting frames; this builds bytes, never transmits them.
 * Calibration, riding/safety parameters and unresolved commands are excluded. */
internal object AeonCommands {
    /** NOSFET 1.1.3 Util.getTimeBytes + BtManager.sendBytesData CRC.
     * Calendar supplies local wall time (including DST); the zone byte deliberately
     * uses raw standard offset, truncated toward zero, just like the official app.
     * An unrepresentable phone year is skipped rather than wrapped on the wire. */
    fun synchronizeClock(calendar: Calendar = Calendar.getInstance()): ByteArray? {
        val year = calendar.get(Calendar.YEAR)
        if (calendar.get(Calendar.ERA) != java.util.GregorianCalendar.AD || year !in 2000..2255) return null
        val frame = byteArrayOf(
            0x4c, 0x64, 0x41, 0x70, 18, 0, 5,
            (year - 2000).toByte(), (calendar.get(Calendar.MONTH) + 1).toByte(),
            calendar.get(Calendar.DAY_OF_MONTH).toByte(), calendar.get(Calendar.HOUR_OF_DAY).toByte(),
            calendar.get(Calendar.MINUTE).toByte(), calendar.get(Calendar.SECOND).toByte(),
            (calendar.timeZone.rawOffset / 3_600_000).toByte(),
            0, 0, 0, 0,
        )
        appendCrc(frame)
        return frame
    }

    fun setting(setting: AeonSetting, value: Int): List<ByteArray>? {
        if (!setting.editable || value !in setting.range) return null
        val length = when (setting) {
            AeonSetting.DISPLAY_BRIGHTNESS -> 20
            AeonSetting.KEY_TONE -> 28
            AeonSetting.UNITS -> 23
            else -> return null
        }
        val frame = ByteArray(length) { 0x80.toByte() }
        byteArrayOf(0x4c, 0x64, 0x41, 0x70, length.toByte(), 1, 2).copyInto(frame)
        frame[length - 5] = value.toByte()
        appendCrc(frame)
        return frame.toList().chunked(20).map { it.toByteArray() }
    }

    private fun appendCrc(frame: ByteArray) {
        val crc = CRC32().apply { update(frame, 0, frame.size - 4) }.value
        for (i in 0..3) frame[frame.size - 4 + i] = (crc ushr (24 - 8 * i)).toByte()
    }
}
