package com.eried.eucplanet.data.model

/** Optional typed extensions to the generic Boolean-era controls.
 * Future wheel families can retain their own value types and semantics. */
sealed interface WheelSettingChange

data class AeonSettingChange(val setting: AeonSetting, val value: Int) : WheelSettingChange

/** Wheel-local preferences, separate from phone brightness and warning volume. */
enum class WheelPreference { DISPLAY_BRIGHTNESS, BUTTON_SOUND, DISPLAY_UNITS }

data class WheelPreferenceChange(val preference: WheelPreference, val value: Int) : WheelSettingChange

data class WheelPreferences(val values: Map<WheelPreference, Int> = emptyMap(), val sessionId: Long = 0)
