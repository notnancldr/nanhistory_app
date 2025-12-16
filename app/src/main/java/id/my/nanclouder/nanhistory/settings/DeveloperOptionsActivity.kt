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

            // SettingsSwitch(
            //     title = "Always ask before auto-delete",
            //     description = "Always ask user before auto deletion happens.",
            //     configValue = Config.autoDeleteAlwaysAsk
            // )
        }
    }
}
