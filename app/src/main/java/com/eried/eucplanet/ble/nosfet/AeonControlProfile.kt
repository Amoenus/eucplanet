package com.eried.eucplanet.ble.nosfet

import com.eried.eucplanet.ble.VeteranCommands
import com.eried.eucplanet.ble.VeteranControlProfile

/** Aeon command policy, independent of the default Veteran policy.
 * Horn single-frame encoding is APK/capture-confirmed. ASCII light toggles were
 * tested on Aeon; earlier app beep results remain qualified in the evidence ledger.
 * Shared builders below preserve existing app behavior, not proof of wheel support. */
internal object AeonControlProfile : VeteranControlProfile {
    override fun horn(): ByteArray = VeteranCommands.horn()
    override fun hornFollowup(): ByteArray? = null
    override fun setLight(on: Boolean): ByteArray = VeteranCommands.setLight(on)
    override fun setLightFollowup(on: Boolean): ByteArray? = null

    // Existing compatibility paths, explicitly selected rather than inherited.
    // Alarm behavior is unresolved. Lock deliberately uses the unsupported default.
    override fun setTiltbackSpeed(kmh: Int): ByteArray = VeteranCommands.setTiltbackSpeed(kmh)
    override fun setAlarmSpeed(kmh: Int): ByteArray = VeteranCommands.setAlarmSpeed(kmh)
    override fun resetTrip(): ByteArray = VeteranCommands.resetTrip()
}
