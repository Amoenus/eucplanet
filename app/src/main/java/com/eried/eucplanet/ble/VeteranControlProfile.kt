package com.eried.eucplanet.ble

/**
 * Model-specific control mapping inside the shared Veteran wire family.
 *
 * Transport, telemetry framing and parsing remain in [VeteranAdapter]. A
 * profile only chooses the exact command sequence for controls whose firmware
 * semantics differ by model. Keeping this behind [WheelAdapter] lets the rest
 * of the app retain its current API while avoiding model conditionals in each
 * adapter method.
 */
internal interface VeteranControlProfile {
    fun horn(): ByteArray
    fun hornFollowup(): ByteArray?
    fun setLight(on: Boolean): ByteArray
    fun setLightFollowup(on: Boolean): ByteArray?
}

/** Existing behavior for LeaperKim and not-yet-specialized Veteran models. */
internal object DefaultVeteranControlProfile : VeteranControlProfile {
    override fun horn(): ByteArray = VeteranCommands.horn()
    override fun hornFollowup(): ByteArray = VeteranCommands.hornCompanion()

    override fun setLight(on: Boolean): ByteArray = VeteranCommands.setHighBeam(on)
    override fun setLightFollowup(on: Boolean): ByteArray =
        VeteranCommands.setHighBeamCompanion(on)
}

/**
 * NOSFET Aeon (mVer 44) control differences with separate evidence levels.
 *
 * Horn: a single LkAp frame is confirmed from official NOSFET traffic.
 * Light: verified on Aeon — ASCII SetLightON/OFF toggles the lamp silently;
 * the generic binary LkAp + LdAp pair toggles it with an acknowledgement beep.
 */
internal object AeonControlProfile : VeteranControlProfile {
    override fun horn(): ByteArray = VeteranCommands.horn()
    override fun hornFollowup(): ByteArray? = null

    override fun setLight(on: Boolean): ByteArray = VeteranCommands.setLight(on)
    override fun setLightFollowup(on: Boolean): ByteArray? = null
}

internal object VeteranControlProfiles {
    fun forModel(model: VeteranModel?): VeteranControlProfile = when (model) {
        VeteranModel.NOSFET_AEON -> AeonControlProfile
        else -> DefaultVeteranControlProfile
    }
}
