package id.my.nanclouder.nanhistory.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.config.Config
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import id.my.nanclouder.nanhistory.config.LocationIterationLogic
import id.my.nanclouder.nanhistory.utils.transportModel.TransportModelTrainingModal
import id.my.nanclouder.nanhistory.utils.transportModel.TransportModelTrainingScreen
import id.my.nanclouder.nanhistory.utils.AccelerometerChange

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import java.io.File
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.requiredHeight

class DeveloperOptionsActivity : SubSettingsActivity("Developer Options") {
    @Composable
    override fun ColumnScope.Content() {
        val scrollState = rememberScrollState()
        var developerModeEnabled by remember { mutableStateOf(Config.developerModeEnabled.get(applicationContext)) }
        Column(Modifier.verticalScroll(scrollState)) {
            SettingsSwitch(
                title = "Enable developer mode",
                configValue = Config.developerModeEnabled,
                onUpdated = {
                    developerModeEnabled = it
                }
            )
            CategoryHeader(
                icon = painterResource(R.drawable.ic_delete_filled),
                iconDescription = "Auto deletion",
                title = "Auto Deletion"
            )
            SettingsSwitch(
                title = "Auto-delete new deleted items in 1 hour",
                description = "New moved items in Trash will be automatically permanently deleted after 1 hour. The app may still warn auto-delete after 30 days despite this setting.",
                configValue = Config.developer1hourAutoDelete,
                enabled = developerModeEnabled
            )

            CategoryHeader(
                icon = painterResource(R.drawable.ic_circle_filled),
                iconDescription = "Record",
                title = "Recording"
            )
            SettingsSwitch(
                title = "Service debug notification",
                description = "Enable debug notification for service.",
                configValue = Config.developerServiceDebug,
                enabled = developerModeEnabled
            )
            SettingsDropdown(
                title = "Location iteration logic",
                description = "Choose specific check logic for location iteration.",
                configValue = Config.locationIterationLogic,
                enumClass = LocationIterationLogic::class.java
            )

            CategoryHeader(
                icon = painterResource(R.drawable.ic_directions_car_filled),
                iconDescription = "Transport Detection",
                title = "Transport Mode Detection"
            )
            SettingsSwitch(
                title = "Show detected transport",
                description = "Show auto-determined transport mode from transport mode determination algorithm.",
                configValue = Config.developerShowDetectedTransport,
                enabled = developerModeEnabled
            )
            TransportModelTrainingModal()

            CategoryHeader(
                icon = painterResource(R.drawable.ic_code),
                iconDescription = "Dev Tools",
                title = "Dev Tools"
            )
            SettingsSwitch(
                title = "Collect accelerometer data",
                description = "Collect and store accelerometer data while recording event.",
                configValue = Config.developerCollectAccelerometer,
                enabled = developerModeEnabled
            )

            // if (developerModeEnabled) {
                // Aggregated Stats
                val context = LocalContext.current
                var aggregatedData by remember { mutableStateOf<Map<String, List<AccelerometerChange>>>(emptyMap()) }
                
                // Load data
                LaunchedEffect(Unit) {
                    val appDir = context.filesDir
                    val accelDir = File(appDir, "accelerometer_data")
                    if (accelDir.exists() && accelDir.isDirectory) {
                        val files = accelDir.listFiles()?.filter { it.name.endsWith(".accel") } ?: emptyList()
                        
                        // Group by transport mode
                        val grouped = files.groupBy { file ->
                            // Read last line for transport mode
                            try {
                                // Basic approach: read last non-empty line. 
                                // Note: This relies on the file ending with the mode string as per earlier plan assumption,
                                // even though we didn't implement the writing of it in RecordService (User said it's elsewhere).
                                // If the file doesn't have it, we might skip it or use "Unknown".
                                // Let's try to read the last few bytes or just read lines.
                                val lines = file.readLines().filter { it.isNotBlank() }
                                if (lines.isNotEmpty()) {
                                    val last = lines.last()
                                    if (!last.contains(",")) last else "Unknown" 
                                } else "Unknown"
                            } catch (e: Exception) {
                                "Error"
                            }
                        }
                        
                        val computed = grouped.mapValues { (_, modeFiles) ->
                            val allSamples = modeFiles.flatMap { 
                                parseAccelerometerFile(it) 
                            }
                            computeAverageSample(allSamples)
                        }.filter { it.key != "Unknown" && it.key != "Error" && it.value.isNotEmpty() }
                        
                        aggregatedData = computed
                    }
                }
                
                if (aggregatedData.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Typical Minute Profile",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    
                    aggregatedData.forEach { (mode, data) ->
                        Card(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = mode,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                AccelerometerGraph(
                                    data = data,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .requiredHeight(300.dp)
                                        .padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // SettingsSwitch(
            //     title = "Always ask before auto-delete",
            //     description = "Always ask user before auto deletion happens.",
            //     configValue = Config.autoDeleteAlwaysAsk
            // )
        // }
    }
}
