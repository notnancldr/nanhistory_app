package id.my.nanclouder.nanhistory.feature.maplocation.ui

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.my.nanclouder.nanhistory.core.common.utils.getActivity
import id.my.nanclouder.nanhistory.core.ui.ComponentPlaceholder
import id.my.nanclouder.nanhistory.core.ui.R as CoreUiR
import id.my.nanclouder.nanhistory.domain.model.Coordinate
import id.my.nanclouder.nanhistory.domain.model.EventPoint
import id.my.nanclouder.nanhistory.domain.model.EventRange
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.model.LocationData
import id.my.nanclouder.nanhistory.feature.maplocation.EventLocationUiState
import id.my.nanclouder.nanhistory.feature.maplocation.EventLocationViewModel
import id.my.nanclouder.nanhistory.feature.maplocation.R
import id.my.nanclouder.nanhistory.feature.maplocation.ReplayUiState
import id.my.nanclouder.nanhistory.feature.maplocation.ui.replay.ReplayBottomSheet
import id.my.nanclouder.nanhistory.feature.maplocation.ui.replay.ReplayTelemetryHud
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import org.osmdroid.util.BoundingBox
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventLocationScreen(
    viewModel: EventLocationViewModel,
    modifier: Modifier = Modifier,
    startAsCutMode: Boolean = false,
    onFinish: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val savedFormat = stringResource(R.string.event_cut_saved)

    EventLocationContent(
        uiState = uiState,
        modifier = modifier,
        onBack = {
            val activity = context.getActivity()
            if (activity != null) activity.finish() else onFinish()
        },
        onToggleCutMode = { viewModel.toggleCutMode() },
        onConfirmCut = {
            viewModel.cutEvent(context) { newEvent ->
                Toast.makeText(
                    context,
                    String.format(savedFormat, newEvent.title),
                    Toast.LENGTH_SHORT
                ).show()
                if (startAsCutMode) {
                    val activity = context.getActivity()
                    if (activity != null) activity.finish() else onFinish()
                }
            }
        },
        onSelectCutPoint = { viewModel.setCutPoint(it) },
        onUndoCut = { viewModel.undoCutSelection() },
        onViewportChanged = { box, zoom -> viewModel.updateViewport(box, zoom) },
        onStartReplay = { viewModel.startReplay() },
        onTogglePlayPause = { viewModel.togglePlayPause() },
        onSeekReplay = { viewModel.seekReplay(it) },
        onStepReplayTime = { viewModel.stepTime(it) },
        onStepReplayWaypoint = { viewModel.stepWaypoint(it) },
        onReplaySpeedChanged = { viewModel.setSpeedMultiplier(it) },
        onToggleFollowCamera = { viewModel.toggleFollowCamera() },
        onToggleImmersive = { viewModel.toggleImmersive() },
        onCloseReplay = { viewModel.stopReplay() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventLocationContent(
    uiState: EventLocationUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onToggleCutMode: () -> Unit = {},
    onConfirmCut: () -> Unit = {},
    onSelectCutPoint: (ZonedDateTime) -> Unit = {},
    onUndoCut: () -> Unit = {},
    onViewportChanged: (BoundingBox, Double) -> Unit = { _, _ -> },
    onStartReplay: () -> Unit = {},
    onTogglePlayPause: () -> Unit = {},
    onSeekReplay: (Float) -> Unit = {},
    onStepReplayTime: (Long) -> Unit = {},
    onStepReplayWaypoint: (Int) -> Unit = {},
    onReplaySpeedChanged: (Float) -> Unit = {},
    onToggleFollowCamera: () -> Unit = {},
    onToggleImmersive: () -> Unit = {},
    onCloseReplay: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val eventData = uiState.event
    val isImmersiveReplay = uiState.replayState.isActive && uiState.replayState.isImmersive

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (!isImmersiveReplay) {
                TopAppBar(
                    scrollBehavior = scrollBehavior,
                    title = {
                        if (eventData != null) {
                            Text(
                                text = if (uiState.cutMode) {
                                    stringResource(R.string.cut_event_title)
                                } else {
                                    stringResource(R.string.event_map_title)
                                },
                                style = MaterialTheme.typography.headlineSmall
                            )
                        } else {
                            ComponentPlaceholder(Modifier.size(128.dp, 16.dp))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        if (eventData == null) {
                            ComponentPlaceholder(
                                Modifier.size(24.dp).padding(8.dp)
                            )
                        }
                        // Replay Button (shown when not in cut mode and not currently replaying)
                        if (eventData is EventRange && !uiState.replayState.isActive && !uiState.cutMode) {
                            IconButton(
                                onClick = onStartReplay,
                                enabled = !uiState.isRecording
                            ) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = stringResource(R.string.replay_mode),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (uiState.cutMode && eventData is EventRange) {
                            IconButton(
                                onClick = onConfirmCut,
                                enabled = uiState.cutStart != null && uiState.cutEnd != null
                            ) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = stringResource(R.string.confirm_cut),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (eventData is EventRange && !uiState.replayState.isActive) {
                            IconButton(
                                onClick = onToggleCutMode,
                                enabled = !uiState.isRecording
                            ) {
                                if (uiState.cutMode) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.exit_cut_mode),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Icon(
                                        painterResource(CoreUiR.drawable.ic_content_cut),
                                        contentDescription = stringResource(R.string.cut_mode),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors()
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val hasData = uiState.renderedLocations.isNotEmpty() ||
                uiState.initialBounds != null ||
                uiState.replayState.isActive
            if (hasData) {
                MapHistoryView(
                    locations = uiState.renderedLocations,
                    initialBounds = uiState.initialBounds,
                    firstLocation = uiState.firstLocation,
                    lastLocation = uiState.lastLocation,
                    cutMode = uiState.cutMode,
                    cutStart = uiState.cutStart,
                    cutEnd = uiState.cutEnd,
                    replayState = uiState.replayState,
                    onSelectCutPoint = onSelectCutPoint,
                    onUndoCut = onUndoCut,
                    onViewportChanged = onViewportChanged,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeekReplay = onSeekReplay,
                    onStepReplayTime = onStepReplayTime,
                    onStepReplayWaypoint = onStepReplayWaypoint,
                    onReplaySpeedChanged = onReplaySpeedChanged,
                    onToggleFollowCamera = onToggleFollowCamera,
                    onToggleImmersive = onToggleImmersive,
                    onCloseReplay = onCloseReplay
                )
            } else {
                ComponentPlaceholder(
                    Modifier.weight(1f).padding(8.dp)
                )
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun EventLocationContentPreview() {
    NanHistoryTheme {
        EventLocationContent(
            uiState = EventLocationUiState(
                event = EventPoint(
                    title = "Morning Walk",
                    description = "",
                    time = ZonedDateTime.now()
                ),
                renderedLocations = listOf(
                    LocationData(ZonedDateTime.now(), Coordinate(-6.2, 106.8)),
                    LocationData(ZonedDateTime.now().plusMinutes(5), Coordinate(-6.21, 106.81))
                )
            )
        )
    }
}
