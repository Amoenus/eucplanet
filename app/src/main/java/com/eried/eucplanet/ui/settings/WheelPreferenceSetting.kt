package com.eried.eucplanet.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eried.eucplanet.R
import com.eried.eucplanet.data.model.WheelPreference

/** Standard settings widgets; no vendor card, Apply dialog or command-ack workflow. */
@Composable
internal fun WheelPreferenceSetting(viewModel: SettingsViewModel, preference: WheelPreference) {
    val preferences by viewModel.wheelPreferences.collectAsStateWithLifecycle()
    val connected by viewModel.isConnected.collectAsStateWithLifecycle()
    val reported = preferences.values[preference] ?: return
    if (!connected) return
    var value by remember(preference, preferences.sessionId, reported) { mutableIntStateOf(reported) }
    val label = stringResource(when (preference) {
        WheelPreference.DISPLAY_BRIGHTNESS -> R.string.wheel_display_brightness
        WheelPreference.BUTTON_SOUND -> R.string.wheel_button_sounds
        WheelPreference.DISPLAY_UNITS -> R.string.wheel_display_units
    })
    val change: (Int) -> Unit = { next ->
        if (next != value && viewModel.setWheelPreference(preference, next)) value = next
    }
    if (preference == WheelPreference.DISPLAY_UNITS) {
        SimpleDropdown(label, value.toString(), listOf(
            "0" to stringResource(R.string.units_speed_kmh),
            "1" to stringResource(R.string.units_speed_mph),
        ), { change(it.toInt()) })
    } else {
        NumberUpDown(value, change, 0..100, label = label, suffix = "%")
    }
}
