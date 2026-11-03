package id.my.nanclouder.nanhistory.activity.eventDetail.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.utils.graph.DefaultGraphPresets
import id.my.nanclouder.nanhistory.utils.graph.GraphConfig

/**
 * Dialog letting the user choose between preset graph templates or creating a graph from scratch.
 */
@Composable
fun PresetChooserDialog(
    onDismissRequest: () -> Unit,
    onSelectPreset: (GraphConfig) -> Unit,
    onCreateFromScratch: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.graph_add_from_preset)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    R.string.graph_preset_speed_title to { DefaultGraphPresets.createSpeedGraph() },
                    R.string.graph_preset_altitude_title to { DefaultGraphPresets.createAltitudeGraph() },
                    R.string.graph_preset_speed_accel_title to { DefaultGraphPresets.createSpeedAndAccelGraph() },
                    R.string.graph_preset_telemetry_title to { DefaultGraphPresets.createFullTelemetryGraph() }
                )

                presets.forEach { (titleRes, generator) ->
                    Surface(
                        onClick = { onSelectPreset(generator()) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(titleRes),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.color_cancel))
            }
        },
        confirmButton = {
            Button(onClick = onCreateFromScratch) {
                Text(stringResource(R.string.graph_create_from_scratch))
            }
        }
    )
}
