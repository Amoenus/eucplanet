package com.eried.eucplanet.data.model

internal fun WheelPreference.aeonSetting(): AeonSetting = when (this) {
    WheelPreference.DISPLAY_BRIGHTNESS -> AeonSetting.DISPLAY_BRIGHTNESS
    WheelPreference.BUTTON_SOUND -> AeonSetting.KEY_TONE
    WheelPreference.DISPLAY_UNITS -> AeonSetting.UNITS
}

/** APK-confirmed page-8 fields. Names are not claims of tested Aeon writes.
 * See docs/protocols/aeon/capabilities.json for evidence and exclusions. */
enum class AeonSetting(val offset: Int, val label: String, val range: IntRange) {
    PEDAL_HARDNESS(50, "MD · pedal hardness (%)", 0..100),
    TILTBACK_SPEED(52, "TLT · tiltback (km/h)", 10..120),
    PWM_TILTBACK(53, "PWT · PWM tiltback (%)", 30..100),
    DISPLAY_BRIGHTNESS(55, "BRT · display brightness (%)", 0..100),
    GYRO(56, "CAL · calibration state", 0..2),
    TRANSPORT(57, "TRM · transport mode", 0..1),
    UNITS(58, "UNT · units (0=km/h, 1=mph)", 0..1),
    VOLTAGE_ADJUSTMENT(59, "VtA · adjustment (0.1%)", -15..15),
    LOW_VOLTAGE_MODE(60, "APK low-voltage mode", 0..1),
    HIGH_SPEED_MODE(61, "APK high-speed mode", 0..1),
    KEY_TONE(63, "SND · menu-key sound (%)", 0..100),
    MAX_CHARGE_VOLTAGE(64, "MxV · charge limit (145 + raw/10 V)", 0..70),
    BRAKE_PRESSURE(65, "APK brake-pressure field (unresolved)", 90..125);

    /** A deliberately narrow first-pass allowlist, not a firmware capability flag. */
    val editable: Boolean get() = this == DISPLAY_BRIGHTNESS || this == KEY_TONE || this == UNITS
}

/** Keep unsupported (0x80), out-of-range and unknown values intact. Never zero-fill. */
data class AeonSettings(
    val rawValues: Map<AeonSetting, Int>,
    val receivedAtNanos: Long,
    val sessionId: Long = 0,
) {
    fun preferences(): WheelPreferences = WheelPreferences(
        WheelPreference.entries.mapNotNull { preference ->
            value(preference.aeonSetting())?.let { preference to it }
        }.toMap(), sessionId,
    )
    fun value(setting: AeonSetting): Int? {
        val raw = rawValues[setting] ?: return null
        if (raw == 0x80) return null
        val decoded = if (setting == AeonSetting.VOLTAGE_ADJUSTMENT && raw >= 128) raw - 256 else raw
        return decoded.takeIf { it in setting.range }
    }

    fun isFresh(nowNanos: Long = System.nanoTime()): Boolean =
        nowNanos - receivedAtNanos in 0..8_000_000_000L
}
