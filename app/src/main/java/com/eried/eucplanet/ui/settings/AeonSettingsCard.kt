package com.eried.eucplanet.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eried.eucplanet.R
import com.eried.eucplanet.data.model.AeonSetting
import com.eried.eucplanet.data.model.AeonSettingResult
import com.eried.eucplanet.ui.theme.appColors

/** Wheel readback is separate from drafts; composition never sends commands. */
@Composable
internal fun AeonSettingsCard(viewModel: SettingsViewModel, connected: Boolean) {
    val data by viewModel.aeonWheelData.collectAsStateWithLifecycle()
    val busy by viewModel.aeonSettingBusy.collectAsStateWithLifecycle()
    val result by viewModel.aeonSettingResult.collectAsStateWithLifecycle()
    val settings = data.aeonSettings
    if (!connected || settings == null) return
    val colors = MaterialTheme.appColors
    val textColors = ButtonDefaults.textButtonColors(contentColor = colors.primary)
    var expanded by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<Triple<AeonSetting, Int, Int>?>(null) }
    val fieldNames = stringArrayResource(R.array.aeon_field_names)
    Card(Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface, contentColor = colors.textPrimary)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.aeon_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.aeon_intro), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.aeon_sound), style = MaterialTheme.typography.bodySmall)
            AeonSetting.entries.filter { it.editable }.forEach { setting ->
                val value = settings.value(setting)
                val label = stringResource(when (setting) {
                    AeonSetting.DISPLAY_BRIGHTNESS -> R.string.aeon_brightness
                    AeonSetting.KEY_TONE -> R.string.aeon_key_tone
                    else -> R.string.units_label
                })
                if (value != null) {
                    var draft by remember(setting, value, settings.sessionId) { mutableIntStateOf(value) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) { Text(label) }
                        Column(Modifier.weight(1.4f)) {
                            if (setting == AeonSetting.UNITS) {
                                Row {
                                    listOf(R.string.units_speed_kmh, R.string.units_speed_mph).forEachIndexed { index, resource ->
                                        TextButton(onClick = { draft = index }, enabled = !busy,
                                            modifier = Modifier.weight(1f), colors = ButtonDefaults.textButtonColors(
                                                contentColor = if (draft == index) colors.primary else colors.textSecondary)) {
                                            Text(stringResource(resource))
                                        }
                                    }
                                }
                            } else NumberUpDown(draft, { draft = it }, setting.range, enabled = !busy, suffix = "%")
                            Row {
                                TextButton(onClick = { pending = Triple(setting, draft, value) },
                                    enabled = !busy && draft != value, colors = textColors) { Text(stringResource(R.string.action_apply)) }
                                TextButton(onClick = { draft = value }, enabled = !busy && draft != value,
                                    colors = textColors) { Text(stringResource(R.string.action_cancel)) }
                            }
                        }
                    }
                }
            }
            TextButton(onClick = { expanded = !expanded }, colors = textColors) { Text(stringResource(R.string.aeon_readback)) }
            if (expanded) {
                AeonSetting.entries.forEach { setting ->
                    val raw = settings.rawValues[setting]
                    val shown = if (raw == null || raw == 128) stringResource(R.string.aeon_readback_unavailable)
                        else "${settings.value(setting) ?: raw}"
                    Text("${fieldNames[setting.ordinal]}: $shown")
                }
                data.aeonLightState?.let { Text("${stringResource(R.string.aeon_headlight_raw)}: ${it.rawValue}") }
            }
            val status = if (busy) R.string.aeon_pending else when (result) {
                AeonSettingResult.NOT_SENT -> R.string.aeon_not_sent
                AeonSettingResult.UNCHANGED, AeonSettingResult.READBACK_MATCH -> R.string.aeon_match
                AeonSettingResult.UNKNOWN -> R.string.aeon_unconfirmed
                null -> null
            }
            status?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
        }
    }
    pending?.let { (setting, value, original) ->
        AlertDialog(onDismissRequest = { pending = null },
            properties = DialogProperties(dismissOnClickOutside = false),
            title = { Text(stringResource(R.string.action_apply)) },
            text = { Text(stringResource(R.string.aeon_confirm, fieldNames[setting.ordinal], value, original)) },
            confirmButton = { TextButton(onClick = { pending = null; viewModel.setAeonSetting(setting, value) }, colors = textColors) { Text(stringResource(R.string.action_apply)) } },
            dismissButton = { TextButton(onClick = { pending = null }, colors = textColors) { Text(stringResource(R.string.action_cancel)) } })
    }
}
