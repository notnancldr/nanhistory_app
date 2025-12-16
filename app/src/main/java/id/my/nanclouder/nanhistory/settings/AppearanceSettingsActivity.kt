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

class AppearanceSettingsActivity : SubSettingsActivity("Appearance") {
    @Composable
    override fun ColumnScope.Content() {
        val scrollState = rememberScrollState()
        Column(Modifier.verticalScroll(scrollState)) {
            CategoryHeader(
                icon = painterResource(R.drawable.ic_settings_filled),
                iconDescription = "New UI",
                title = "New UI"
            )
            SettingsSwitch(
                title = "Enable New UI",
                description = "Use new UI for application appearance",
                configValue = Config.appearanceNewUI,
                onUpdated = { _ ->
                    recreate()
                }
            )
            // SettingsSwitch(
            //     title = "Always ask before auto-delete",
            //     description = "Always ask user before auto deletion happens.",
            //     configValue = Config.autoDeleteAlwaysAsk
            // )
        }
    }
}
