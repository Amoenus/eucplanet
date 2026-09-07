package com.eried.eucplanet.ble

/**
 * Model-specific control mapping inside the shared Veteran wire family.
 *
 * Transport, telemetry framing and parsing remain in [VeteranAdapter]. A
 * profile explicitly chooses command semantics for its model. Keeping this
 * behind [WheelAdapter] lets the rest
 * of the app retain its current API while avoiding model conditionals in each
 * adapter method.
 */
internal interface VeteranControlProfile {
    fun horn(): ByteArray
    fun hornFollowup(): ByteArray?
    fun setLight(on: Boolean): ByteArray
    fun setLightFollowup(on: Boolean): ByteArray?
    fun setTiltbackSpeed(kmh: Int): ByteArray? = null
    fun setAlarmSpeed(kmh: Int): ByteArray? = null
    fun setLock(locked: Boolean): ByteArray? = null
    fun resetTrip(): ByteArray? = null
    fun setVolume(percent: Int): ByteArray? = null
    fun setDRL(on: Boolean): ByteArray? = null
}

/** Existing behavior for LeaperKim and not-yet-specialized Veteran models. */
internal object DefaultVeteranControlProfile : VeteranControlProfile {
    override fun horn(): ByteArray = VeteranCommands.horn()
    override fun hornFollowup(): ByteArray = VeteranCommands.hornCompanion()

    override fun setLight(on: Boolean): ByteArray = VeteranCommands.setHighBeam(on)
    override fun setLightFollowup(on: Boolean): ByteArray =
        VeteranCommands.setHighBeamCompanion(on)
    override fun setTiltbackSpeed(kmh: Int): ByteArray = VeteranCommands.setTiltbackSpeed(kmh)
    override fun setAlarmSpeed(kmh: Int): ByteArray = VeteranCommands.setAlarmSpeed(kmh)
    override fun setLock(locked: Boolean): ByteArray = VeteranCommands.setLock(locked)
    override fun resetTrip(): ByteArray = VeteranCommands.resetTrip()
}
