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
            // SettingsSwitch(
            //     title = "Always ask before auto-delete",
            //     description = "Always ask user before auto deletion happens.",
            //     configValue = Config.autoDeleteAlwaysAsk
            // )
        }
    }
}
