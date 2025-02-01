package id.my.nanclouder.nanhistory.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.config.Config

class RecordSettingsActivity : SubSettingsActivity("Event Recording") {
    @Composable
    override fun ColumnScope.Content() {
        CategoryHeader(
            icon = painterResource(R.drawable.ic_location),
            iconDescription = "Location",
            title = "Location"
        )
        SettingsSlider(
            title = "Accuracy Threshold",
            description = "Minimum accuracy of a location update.",
            configValue = Config.locationAccuracyThreshold,
            valueUnit = "m",
            valueRange = 5f..200f,
            steps = 0
        )
        SettingsSlider(
            title = "Minimum Distance",
            description = "The closest distance to update the location.",
            configValue = Config.locationMinimumDistance,
            valueUnit = "m",
            valueRange = 0f..200f,
            steps = 0
        )
        SettingsSlider(
            title = "Update Interval",
            description = "Shortest interval between location updates.",
            configValue = Config.locationUpdateInterval,
            valueUnit = "s",
            valueRange = 1f..60f,
            steps = 0
        )
    }
}
