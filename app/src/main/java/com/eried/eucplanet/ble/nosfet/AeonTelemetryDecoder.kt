package com.eried.eucplanet.ble.nosfet

import com.eried.eucplanet.ble.VeteranModel
import com.eried.eucplanet.ble.VeteranParser

import com.eried.eucplanet.data.model.AeonLightState

/** Model-specific interpretation of already CRC-validated Veteran frames. */
internal object AeonTelemetryDecoder {
    fun settings(frame: ByteArray, model: VeteranModel?): com.eried.eucplanet.data.model.AeonSettings? {
        if (model != VeteranModel.NOSFET_AEON || frame.size != 75 || VeteranParser.pageId(frame) != 8) return null
        return com.eried.eucplanet.data.model.AeonSettings(
            com.eried.eucplanet.data.model.AeonSetting.entries.associateWith { frame[it.offset].toInt() and 0xff },
            System.nanoTime(),
        )
    }

    fun lightState(frame: ByteArray, model: VeteranModel?): AeonLightState? {
        if (model != VeteranModel.NOSFET_AEON) return null
        // Physical-panel capture 2026-09-07: page 1 offset 49 follows
        // off/low/medium/high/off as 0/1/2/3/0 on firmware 44250.
        // Page 8 offset 47 corroborates it, but is not merged because its
        // independently timed updates could overwrite a newer page-1 value.
        if (frame.size != 87 || VeteranParser.pageId(frame) != 1) return null
        return AeonLightState(frame[49].toInt() and 0xff)
    }
}
