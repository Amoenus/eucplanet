package com.eried.eucplanet.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eried.eucplanet.R
import com.eried.eucplanet.ui.theme.appColors
import com.eried.eucplanet.util.Units
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
internal fun SpeedLimitReadbackRow(viewModel: SettingsViewModel, connected: Boolean, speedUnit: String) {
    val readback by viewModel.speedLimitReadback.collectAsStateWithLifecycle()
    // Require a new sample after opening this view or changing connection state.
    val observingSince = remember(connected) { System.currentTimeMillis() }
    val clock by produceState(System.currentTimeMillis(), connected) {
        while (connected) {
            value = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    if (!readback.hasReportedValues) return
    val (tiltback, alarm) = readback.visibleValues(connected, maxOf(clock, System.currentTimeMillis()), observingSince)
    val unit = Units.speedUnit(LocalContext.current, speedUnit)
    val unavailable = stringResource(R.string.speed_readback_unavailable)
    val colors = MaterialTheme.appColors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.speed_readback_title),
            style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.speed_tiltback to tiltback, R.string.speed_alarm to alarm).forEach { (label, value) ->
                Column(Modifier.weight(1f)) {
                    Text(stringResource(label), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    Text(value?.let { String.format(Locale.getDefault(), "%.1f %s", Units.speed(it, speedUnit), unit) }
                        ?: unavailable, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                }
            }
        }
    }
}
