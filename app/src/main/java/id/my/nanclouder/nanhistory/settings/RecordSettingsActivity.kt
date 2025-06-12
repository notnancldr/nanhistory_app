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

class RecordSettingsActivity : SubSettingsActivity("Event Recording") {
    @Composable
    override fun ColumnScope.Content() {
        val scrollState = rememberScrollState()
        Column(Modifier.verticalScroll(scrollState)) {
            val recordAudioExperimental = Config.experimentalAudioRecord.get(LocalContext.current)
            CategoryHeader(
                icon = painterResource(R.drawable.ic_settings_filled),
                iconDescription = "Service",
                title = "Service"
            )
            SettingsNumInput(
                title = "Service Max Duration",
                description = "Service will be restarted everytime the maximum duration is reached.",
                configValue = Config.serviceMaxDuration,
                valueUnit = "s",
                valueRange = 5..100000
            )
            CategoryHeader(
                icon = painterResource(R.drawable.ic_location),
                iconDescription = "Location",
                title = "Location"
            )
            SettingsNumInput(
                title = "Accuracy Threshold",
                description = "Minimum accuracy of a location update.",
                configValue = Config.locationAccuracyThreshold,
                valueUnit = "m",
                valueRange = 5..100000
            )
            SettingsNumInput(
                title = "Minimum Distance",
                description = "The closest distance to update the location.",
                configValue = Config.locationMinimumDistance,
                valueUnit = "m",
                valueRange = 0..200
            )
            SettingsNumInput(
                title = "Update Interval",
                description = "Shortest interval between location updates.",
                configValue = Config.locationUpdateInterval,
                valueUnit = "s",
                valueRange = 1..60
            )
            if (recordAudioExperimental) {
                CategoryHeader(
                    icon = painterResource(R.drawable.ic_play_arrow_filled),
                    iconDescription = "Audio Record",
                    title = "Audio Record"
                )
                SettingsSwitch(
                    configValue = Config.includeAudioRecord,
                    title = "Include Audio Record",
                    description = "Record audio at the beginning of recording."
                )
                SettingsNumInput(
                    title = "Audio Max Duration",
                    description = "Set max duration for audio recording. Set 0 for no limit.",
                    configValue = Config.audioRecordMaxDuration,
                    valueUnit = "s",
                    valueRange = 0..Int.MAX_VALUE
                )
                SettingsSwitch(
                    configValue = Config.audioStereoChannel,
                    title = "Use Stereo Channel",
                    description = "Record audio using two microphones (if supported)."
                )
                SettingsNumInput(
                    title = "Encoding Bitrate",
                    description = "Bigger the bitrate, higher the audio quality, but using more storage.",
                    configValue = Config.audioEncodingBitrate,
                    valueUnit = "kbps",
                    valueRange = 1..1024
                )
                SettingsNumInput(
                    title = "Sampling Rate",
                    description = "Audio sampling rate.",
                    configValue = Config.audioSamplingRate,
                    valueUnit = "kHz",
                    valueRange = 1..128
                )
            }
        }
    }
}
