package com.eried.eucplanet.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eried.eucplanet.R
import com.eried.eucplanet.data.model.MetricCatalog
import com.eried.eucplanet.service.VoiceReportPlan
import com.eried.eucplanet.ui.theme.appColors
import com.eried.eucplanet.voice.VoiceVocabulary

/**
 * What can I say.
 *
 * Generated from the same catalogs the matcher listens against, never a
 * hand-written list, so it cannot promise a rider a phrase that does not work
 * or quietly omit one that does. Rule 10 asks exactly this of any surface that
 * previews what the app will do.
 *
 * The names are the catalog's own localised labels, which is also what a rider
 * reads on a tile, so the list doubles as the answer to "what do I call this".
 */
@Composable
fun VoiceVocabularyDialog(onDismiss: () -> Unit) {
    val metricNames = MetricCatalog.all.associate { it.key to stringResource(it.spokenLabelRes ?: it.labelRes) }
    // The report keys are English identifiers; their names have been
    // translated all along under report_*. Using the keys put "Battery" and
    // "Distance" into a German rider's list beside Akku and Energie.
    val reportNames = mapOf(
        "Speed" to stringResource(R.string.report_speed),
        "Battery" to stringResource(R.string.report_battery),
        "PhoneBattery" to stringResource(R.string.report_phone_battery),
        "Temp" to stringResource(R.string.report_temp),
        "PWM" to stringResource(R.string.report_pwm),
        "Current" to stringResource(R.string.report_current),
        "Power" to stringResource(R.string.report_power),
        "Distance" to stringResource(R.string.report_distance),
        "Recording" to stringResource(R.string.report_recording),
        "Time" to stringResource(R.string.report_time),
        "Navigation" to stringResource(R.string.report_navigation),
    )
    // The same phrase lists the matcher listens against, so the list cannot
    // promise something that will not work or omit something that will. It
    // used to build only metrics, reports and the split, which left every
    // special and action off the one page that explains them.
    val terms = VoiceVocabulary.build(
        metricNames = metricNames,
        reportNames = reportNames,
        splitName = stringResource(R.string.voice_split_term),
        helpPhrases = stringResource(R.string.voice_help_terms),
        specialPhrases = mapOf(
            VoiceVocabulary.Special.WEATHER to stringResource(R.string.voice_sp_weather_terms),
            VoiceVocabulary.Special.DAYLIGHT to stringResource(R.string.voice_sp_daylight_terms),
            VoiceVocabulary.Special.CONNECTED to stringResource(R.string.voice_sp_connected_terms),
            VoiceVocabulary.Special.UPTIME to stringResource(R.string.voice_sp_uptime_terms),
            VoiceVocabulary.Special.NAV_NEXT to stringResource(R.string.voice_sp_nav_terms),
            VoiceVocabulary.Special.LAST_TRIP to stringResource(R.string.voice_sp_last_trip_terms),
            VoiceVocabulary.Special.REPORT to stringResource(R.string.voice_sp_report_terms),
        ),
        actionPhrases = mapOf(
            "V_LIGHT_ON" to stringResource(R.string.voice_act_light_on_terms),
            "V_LIGHT_OFF" to stringResource(R.string.voice_act_light_off_terms),
            "V_LOCK" to stringResource(R.string.voice_act_lock_terms),
            "V_UNLOCK" to stringResource(R.string.voice_act_unlock_terms),
            "HORN" to stringResource(R.string.voice_act_horn_terms),
            "RECORD_START" to stringResource(R.string.voice_act_record_start_terms),
            "RECORD_STOP" to stringResource(R.string.voice_act_record_stop_terms),
            "RESET_TRIP" to stringResource(R.string.voice_act_reset_trip_terms),
        ),
    )
    // Metric and report names overlap by design (Speed is both), so the list a
    // rider reads is the set of distinct things they can say. Specials and
    // actions carry several phrasings per key, and listing every one of them
    // would bury the metrics: the first is the canonical one, and the matcher
    // accepts the rest whether or not they are written down.
    val names = terms
        .groupBy { if (it.kind == VoiceVocabulary.Kind.METRIC ||
                it.kind == VoiceVocabulary.Kind.REPORT ||
                it.kind == VoiceVocabulary.Kind.SPLIT
            ) it.name else it.key
        }
        .map { (_, group) -> group.first().name }
        .distinct()
        .sorted()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.voice_command_vocabulary)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.voice_command_vocabulary_tap),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.appColors.textSecondary,
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(names) { name ->
                        // A reference, not a control. Tapping used to speak the
                        // answer, which made a list of words look like a list of
                        // buttons and invited a rider to press one instead of
                        // reading it.
                        Text(
                            name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
        },
    )
}
