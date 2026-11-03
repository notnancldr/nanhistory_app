package id.my.nanclouder.nanhistory.activity.eventLocation.ui

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
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.activity.eventDetail.getActivity
import id.my.nanclouder.nanhistory.state.EventLocationUiState
import id.my.nanclouder.nanhistory.state.EventLocationViewModel
import id.my.nanclouder.nanhistory.ui.ComponentPlaceholder
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import id.my.nanclouder.nanhistory.utils.Coordinate
import id.my.nanclouder.nanhistory.utils.history.EventPoint
import id.my.nanclouder.nanhistory.utils.history.EventRange
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import id.my.nanclouder.nanhistory.utils.history.LocationData
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
        onViewportChanged = { box, zoom -> viewModel.updateViewport(box, zoom) }
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
    onViewportChanged: (BoundingBox, Double) -> Unit = { _, _ -> }
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val eventData = uiState.event

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        topBar = {
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
                    if (eventData is EventRange) {
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
                                    painterResource(R.drawable.ic_content_cut),
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val hasData = uiState.renderedLocations.isNotEmpty() || uiState.initialBounds != null
            if (hasData) {
                MapHistoryView(
                    locations = uiState.renderedLocations,
                    initialBounds = uiState.initialBounds,
                    firstLocation = uiState.firstLocation,
                    lastLocation = uiState.lastLocation,
                    cutMode = uiState.cutMode,
                    cutStart = uiState.cutStart,
                    cutEnd = uiState.cutEnd,
                    onSelectCutPoint = onSelectCutPoint,
                    onUndoCut = onUndoCut,
                    onViewportChanged = onViewportChanged
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
