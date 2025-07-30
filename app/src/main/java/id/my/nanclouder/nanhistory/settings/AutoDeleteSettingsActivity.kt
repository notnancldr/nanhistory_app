package id.my.nanclouder.nanhistory.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.config.Config

class AutoDeleteSettingsActivity : SubSettingsActivity("Auto-delete") {
    @Composable
    override fun ColumnScope.Content() {
        val scrollState = rememberScrollState()
        Column(Modifier.verticalScroll(scrollState)) {
            CategoryHeader(
                icon = painterResource(R.drawable.ic_delete_filled),
                iconDescription = "Auto deletion",
                title = "Auto Deletion"
            )
            SettingsSwitch(
                title = "Notify when auto-delete",
                description = "Notify everytime auto deletion happens.",
                configValue = Config.autoDeleteNotifyUser
            )
            // SettingsSwitch(
            //     title = "Always ask before auto-delete",
            //     description = "Always ask user before auto deletion happens.",
            //     configValue = Config.autoDeleteAlwaysAsk
            // )
        }
    }
}
