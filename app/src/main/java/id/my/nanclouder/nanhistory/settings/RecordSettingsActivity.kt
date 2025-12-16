package id.my.nanclouder.nanhistory.settings

import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.service.RecordService
import id.my.nanclouder.nanhistory.utils.RecordStatus

class RecordSettingsActivity : SubSettingsActivity("Event Recording") {
    @Composable
    override fun ColumnScope.Content() {
        val scrollState = rememberScrollState()
        val settingsChanged = rememberSaveable { mutableStateOf(false) }
        val isRestarting = rememberSaveable { mutableStateOf(false) }

        // Track all settings states
        val serviceMaxDurationState = Config.serviceMaxDuration.getState()
        val serviceRestartEnabled = Config.serviceRestartEnabled.getState()
        val locationAccuracyThresholdState = Config.locationAccuracyThreshold.getState()
        val locationMinimumDistanceState = Config.locationMinimumDistance.getState()
        val locationUpdateIntervalState = Config.locationUpdateInterval.getState()
        val recordVibrateEnabledState = Config.recordVibrateEnabled.getState()
        val recordVibrateEveryUpdateState = Config.recordVibrateEveryUpdate.getState()
        val recordVibrateIntervalState = Config.recordVibrateInterval.getState()
        val recordVibrateDurationState = Config.recordVibrateDuration.getState()
        val recordShakeEnableState = Config.recordShakeEnabled.getState()
        val recordShakeToStartState = Config.recordShakeToStart.getState()
        val recordShakeSensitivityState = Config.recordShakeSensitivity.getState()
        val audioRecordMaxDurationState = Config.audioRecordMaxDuration.getState()
        val includeAudioRecordState = Config.includeAudioRecord.getState()
        val audioStereoChannelState = Config.audioStereoChannel.getState()
        val audioEncodingBitrateState = Config.audioEncodingBitrate.getState()
        val audioSamplingRateState = Config.audioSamplingRate.getState()
        val recordAudioExperimental = Config.experimentalAudioRecord.get(LocalContext.current)

        val serviceState = RecordService.RecordState.status.collectAsState()

        // Detect config changes
        LaunchedEffect(
            serviceMaxDurationState.value, locationAccuracyThresholdState.value, locationMinimumDistanceState.value,
            locationUpdateIntervalState.value, recordVibrateEnabledState.value, recordVibrateEveryUpdateState.value,
            recordVibrateIntervalState.value, recordVibrateDurationState.value, recordShakeEnableState.value,
            recordShakeToStartState.value, recordShakeSensitivityState.value, audioRecordMaxDurationState.value,
            includeAudioRecordState.value, audioStereoChannelState.value, audioEncodingBitrateState.value,
            audioSamplingRateState.value, serviceRestartEnabled.value
        ) {
            Log.d("NanHistoryDebug", "Config changed!")
            settingsChanged.value = RecordService.RecordState.isRunning.value
        }

        // Detect service state
        LaunchedEffect(serviceState.value) {
            if (serviceState.value == RecordStatus.RESTARTING) {
                isRestarting.value = true
            }

            if (
                (serviceState.value != RecordStatus.RESTARTING && isRestarting.value) ||
                serviceState.value == RecordStatus.READY
            ) {
                settingsChanged.value = false
                isRestarting.value = false
            }
        }

        Column {
            AnimatedVisibility(
                visible = settingsChanged.value
            ) {
                SettingsChangedBanner(
                    isProcessing = serviceState.value == RecordStatus.RESTARTING,
                    onActionClick = {
                        settingsChanged.value = false

                        val intent = Intent(this@RecordSettingsActivity, RecordService::class.java)
                        intent.action = RecordService.ACTION_SERVICE_RESTART

                        startService(intent)
                    },
                )
            }

            Column(Modifier.verticalScroll(scrollState)) {
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
                SettingsSwitch(
                    title = "Enable Restart",
                    description = "Enable service restart. If disabled, Service Max Duration will have no effect.",
                    configValue = Config.serviceRestartEnabled,
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
                CategoryHeader(
                    icon = painterResource(R.drawable.ic_mobile_vibrate_filled),
                    iconDescription = "Vibration",
                    title = "Vibration"
                )
                SettingsSwitch(
                    configValue = Config.recordVibrateEnabled,
                    title = "Enable Vibration",
                    description = "Vibrate device during location recording."
                )
                SettingsSwitch(
                    configValue = Config.recordVibrateEveryUpdate,
                    title = "Vibrate on Every Update",
                    description = "Vibrate on each location update, or only at intervals."
                )
                SettingsNumInput(
                    title = "Vibration Interval",
                    description = "Time between vibrations when not vibrating on every update.",
                    configValue = Config.recordVibrateInterval,
                    valueUnit = "s",
                    valueRange = 1..300
                )
                SettingsNumInput(
                    title = "Vibration Duration",
                    description = "Duration of each vibration pulse.",
                    configValue = Config.recordVibrateDuration,
                    valueUnit = "ms",
                    valueRange = 0..1000
                )
                CategoryHeader(
                    icon = painterResource(R.drawable.ic_hand_gesture_filled),
                    iconDescription = "Shake Detection",
                    title = "Shake Detection"
                )
                SettingsSwitch(
                    configValue = Config.recordShakeEnabled,
                    title = "Enable Shake Detection",
                    description = "Enable shake gesture detection for recording control."
                )
                SettingsSwitch(
                    configValue = Config.recordShakeToStart,
                    title = "Shake to Start",
                    description = "Wait for first shake to start recording. Disable this to only stop recording on shake."
                )
                SettingsNumInput(
                    title = "Shake Sensitivity",
                    description = "Sensitivity level for shake detection. Higher value = more sensitive.",
                    configValue = Config.recordShakeSensitivity,
                    valueUnit = "level",
                    valueRange = 1..20
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

    @Composable
    private fun SettingsChangedBanner(
        onActionClick: () -> Unit,
        isProcessing: Boolean,
        modifier: Modifier = Modifier
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            color =
                if (!isProcessing) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = "Recording service is still running, you have to restart the service to apply new changes.",
                    modifier = Modifier.padding(end = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        if (!isProcessing) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onActionClick, enabled = !isProcessing) {
                    if (!isProcessing) Text("Restart Service")
                    else Text("Restarting")
                }
            }
        }
    }
}