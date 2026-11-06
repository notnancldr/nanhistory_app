package id.my.nanclouder.nanhistory.activity.eventLocation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.activity.eventLocation.common.DataMode
import id.my.nanclouder.nanhistory.activity.eventLocation.ui.replay.ReplayBottomSheet
import id.my.nanclouder.nanhistory.activity.eventLocation.ui.replay.ReplayTelemetryHud
import id.my.nanclouder.nanhistory.state.ReplayUiState
import id.my.nanclouder.nanhistory.ui.map.maplibre.EventLocationMap
import id.my.nanclouder.nanhistory.utils.Coordinate
import id.my.nanclouder.nanhistory.utils.TimeFormatterWithSecond
import id.my.nanclouder.nanhistory.utils.history.LocationData
import org.osmdroid.util.BoundingBox
import java.time.ZonedDateTime
import kotlin.math.roundToInt

/**
 * Modern Compose container hosting [EventLocationMap], inspection cards, and replay controls.
 */
@Composable
fun MapHistoryView(
    locations: List<LocationData>,
    initialBounds: BoundingBox?,
    firstLocation: LocationData?,
    lastLocation: LocationData?,
    cutMode: Boolean,
    cutStart: ZonedDateTime?,
    cutEnd: ZonedDateTime?,
    replayState: ReplayUiState?,
    onSelectCutPoint: (ZonedDateTime) -> Unit,
    onUndoCut: () -> Unit,
    onViewportChanged: (BoundingBox, Double) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekReplay: (Float) -> Unit,
    onStepReplayTime: (Long) -> Unit,
    onStepReplayWaypoint: (Int) -> Unit,
    onReplaySpeedChanged: (Float) -> Unit,
    onToggleFollowCamera: () -> Unit,
    onToggleImmersive: () -> Unit,
    onCloseReplay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showPoints by rememberSaveable { mutableStateOf(false) }
    var showData by rememberSaveable { mutableStateOf(false) }
    var dataMode by rememberSaveable { mutableStateOf(DataMode.SPEED) }
    var zoomLevel by remember { mutableDoubleStateOf(15.0) }

    var isSelected by remember { mutableStateOf(false) }
    var selectedSpeed by remember { mutableIntStateOf(0) }
    var selectedTime by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf<Coordinate?>(null) }
    var selectedAltitude by remember { mutableStateOf<String?>(null) }
    var selectedAccuracy by remember { mutableStateOf<String?>(null) }

    val isReplaying = replayState?.isActive == true

    Box(modifier = modifier.fillMaxSize()) {
        EventLocationMap(
            locations = locations,
            initialBounds = initialBounds,
            firstLocation = firstLocation,
            lastLocation = lastLocation,
            cutMode = cutMode,
            cutStart = cutStart,
            cutEnd = cutEnd,
            showPoints = showPoints,
            showData = showData,
            dataMode = dataMode,
            replayState = replayState,
            onSelectCutPoint = onSelectCutPoint,
            onSegmentClick = { seg ->
                if (!isReplaying) {
                    selectedTime = "${TimeFormatterWithSecond.format(seg.start)} - ${
                        TimeFormatterWithSecond.format(seg.end)
                    }"
                    selectedSpeed = seg.speed.roundToInt()
                    val firstData = seg.locationData.firstOrNull()
                    val lastData = seg.locationData.lastOrNull()
                    val firstAlt = firstData?.altitude
                    val lastAlt = lastData?.altitude
                    val firstAcc = firstData?.accuracy
                    val lastAcc = lastData?.accuracy
                    selectedLocation = firstData?.location
                    selectedAltitude = if (firstAlt != null && lastAlt != null) {
                        "${(firstAlt * 10.0).roundToInt() / 10.0} - " +
                            "${(lastAlt * 10.0).roundToInt() / 10.0} m"
                    } else null
                    selectedAccuracy = if (firstAcc != null && lastAcc != null) {
                        "${(firstAcc * 10.0).roundToInt() / 10.0} - " +
                            "${(lastAcc * 10.0).roundToInt() / 10.0} m"
                    } else null
                    isSelected = true
                }
            },
            onViewportChanged = { box, zoom ->
                zoomLevel = zoom
                onViewportChanged(box, zoom)
            },
            onFollowCameraDismissed = {
                if (replayState?.isFollowCamera == true) {
                    onToggleFollowCamera()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isReplaying) {
            // Replay Telemetry HUD
            ReplayTelemetryHud(
                telemetry = replayState?.currentTelemetry,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            )

            // Replay Bottom Sheet Controls (hidden in immersive mode unless tapped)
            if (replayState?.isImmersive != true) {
                ReplayBottomSheet(
                    replayState = replayState ?: ReplayUiState(),
                    onTogglePlayPause = onTogglePlayPause,
                    onSeek = onSeekReplay,
                    onStepTime = onStepReplayTime,
                    onStepWaypoint = onStepReplayWaypoint,
                    onSpeedChanged = onReplaySpeedChanged,
                    onToggleFollowCamera = onToggleFollowCamera,
                    onToggleImmersive = onToggleImmersive,
                    onClose = onCloseReplay,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        } else {
            // Normal Inspection Segment Card
            if (isSelected) {
                SelectedSegmentInfoCard(
                    selectedTime = selectedTime,
                    selectedSpeed = selectedSpeed,
                    selectedAltitude = selectedAltitude,
                    selectedAccuracy = selectedAccuracy,
                    location = selectedLocation,
                    onClose = { isSelected = false },
                    modifier = Modifier.align(Alignment.TopCenter),
                    context = context
                )
            }

            // Normal Bottom Bar Controls
            EventLocationBottomBar(
                zoomLevel = zoomLevel,
                isMaxDetail = zoomLevel >= 15.0,
                cutMode = cutMode,
                showPoints = showPoints,
                showData = showData,
                dataMode = dataMode,
                canUndoCut = cutStart != null || cutEnd != null,
                onTogglePoints = { showPoints = !showPoints },
                onToggleData = { showData = !showData },
                onSelectDataMode = { dataMode = it },
                onUndoCut = onUndoCut,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
