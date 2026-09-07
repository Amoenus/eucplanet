package com.eried.eucplanet.ble.nosfet

import com.eried.eucplanet.data.model.AeonSetting
import java.util.zip.CRC32

/** Exact NOSFET 1.1.3 setting frames; this builds bytes, never transmits them.
 * Calibration, riding/safety parameters and unresolved commands are excluded. */
internal object AeonCommands {
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
        val crc = CRC32().apply { update(frame, 0, length - 4) }.value
        for (i in 0..3) frame[length - 4 + i] = (crc ushr (24 - 8 * i)).toByte()
        return frame.toList().chunked(20).map { it.toByteArray() }
    }
}
