package com.eried.eucplanet.ui.settings

import androidx.compose.foundation.clickable
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
fun VoiceVocabularyDialog(
    onDismiss: () -> Unit,
    /** Speaks the answer for a name, so the list previews itself. */
    onPreview: (String) -> Unit = {},
) {
    val metricNames = MetricCatalog.all.associate { it.key to stringResource(it.labelRes) }
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
    val terms = VoiceVocabulary.build(
        metricNames = metricNames,
        reportNames = reportNames,
        splitName = stringResource(R.string.voice_split_term),
    )
    // Metric and report names overlap by design (Speed is both), so the list a
    // rider reads is the set of distinct things they can say.
    val names = terms.map { it.name }.distinct().sorted()

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
                        // Tap to hear it. The answer comes from the same path a
                        // spoken question takes, so what a rider previews here
                        // is what they will actually hear on the road.
                        Text(
                            name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPreview(name) }
                                .padding(vertical = 6.dp),
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
