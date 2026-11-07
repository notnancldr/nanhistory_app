package id.my.nanclouder.nanhistory.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import id.my.nanclouder.nanhistory.feature.settings.R
import id.my.nanclouder.nanhistory.config.Config

class AppearanceSettingsActivity : SubSettingsActivity("Appearance") {
    @Composable
    override fun ColumnScope.Content() {
        val scrollState = rememberScrollState()
        Column(Modifier.verticalScroll(scrollState)) {
            CategoryHeader(
                icon = painterResource(R.drawable.ic_settings_filled),
                iconDescription = "Old UI",
                title = "Old UI"
            )
            SettingsSwitch(
                title = "Use Old UI",
                description = "WARNING: old UI is no longer receiving updates, some features may not work or break.",
                configValue = Config.appearanceOldUi,
                onUpdated = { _ ->
                    recreate()
                }
            )
        }
    }
}
