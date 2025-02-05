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
            icon = painterResource(R.drawable.ic_code),
            iconDescription = "List Loading",
            title = "List Loading"
        )
        SettingsSwitch(
            title = "Stream List",
            description = "Append list item without wait until list loading ends.",
            configValue = Config.experimentalStreamList
        )
    }
}
