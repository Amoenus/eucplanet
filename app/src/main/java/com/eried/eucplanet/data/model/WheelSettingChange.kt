package com.eried.eucplanet.data.model

/** Optional typed extensions to the generic Boolean-era controls.
 * Future wheel families can retain their own value types and semantics. */
sealed interface WheelSettingChange

data class AeonSettingChange(val setting: AeonSetting, val value: Int) : WheelSettingChange
