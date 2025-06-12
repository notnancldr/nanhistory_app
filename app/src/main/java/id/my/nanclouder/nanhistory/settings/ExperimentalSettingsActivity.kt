package id.my.nanclouder.nanhistory.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.config.Config

class ExperimentalSettingsActivity : SubSettingsActivity("Experimental") {
    @Composable
    override fun ColumnScope.Content() {
        CategoryHeader(
            icon = painterResource(R.drawable.ic_circle_filled),
            iconDescription = "Audio Record",
            title = "Audio Record"
        )
        SettingsSwitch(
            title = "Audio Record",
            description = "Activate audio record feature.",
            configValue = Config.experimentalAudioRecord
        )
    }
}
