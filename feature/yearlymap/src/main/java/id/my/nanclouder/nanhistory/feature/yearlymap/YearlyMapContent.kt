package id.my.nanclouder.nanhistory.feature.yearlymap

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.feature.yearlymap.R
import id.my.nanclouder.nanhistory.core.ui.list.SimpleEventListItem
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import java.time.Year
import kotlinx.coroutines.launch

/**
 * Stateless UI content container for the aggregate trip screen.
 * Hosts navigation, controls, overlays, bottom sheet, and an isolated [mapContent] slot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearlyMapContent(
    uiState: YearlyMapUiState,
    onTimeRangeSelected: (TimeRangeSelection) -> Unit,
    onCustomRangeClicked: () -> Unit,
    onDismissBottomSheet: () -> Unit,
    onEventClicked: (String) -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    onRefreshCurrentRange: () -> Unit = {},
    onClearAllCaches: () -> Unit = {},
    mapContent: @Composable () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }
    var expandedOptionsMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    val years = remember { (2020..Year.now().value).toList().reversed() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val cacheClearedMsg = stringResource(R.string.corridor_cache_cleared)

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_corridor_cache_title)) },
            text = { Text(stringResource(R.string.clear_corridor_cache_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearAllCaches()
                        scope.launch {
                            snackbarHostState.showSnackbar(cacheClearedMsg)
                        }
                    }
                ) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (uiState.isBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismissBottomSheet,
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.events_on_path, uiState.selectedSegmentEvents.size),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn {
                    items(uiState.selectedSegmentEvents, key = { it.id }) { event ->
                        SimpleEventListItem(
                            event = event,
                            modifier = Modifier.clickable { onEventClicked(event.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { expandedMenu = true }
                            .padding(8.dp)
                    ) {
                        val titleText = when (val sel = uiState.timeRange) {
                            is TimeRangeSelection.Year -> stringResource(
                                R.string.trips_in_year,
                                sel.year.toString()
                            )
                            is TimeRangeSelection.AllTime -> stringResource(R.string.trips_all_time)
                            is TimeRangeSelection.Custom -> stringResource(
                                R.string.trips_custom_range,
                                sel.start.toString(),
                                sel.end.toString()
                            )
                        }
                        Text(titleText)
                        Icon(
                            Icons.Rounded.ArrowDropDown,
                            contentDescription = stringResource(R.string.select_time_range)
                        )

                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.time_range_all_time)) },
                                onClick = {
                                    onTimeRangeSelected(TimeRangeSelection.AllTime)
                                    expandedMenu = false
                                }
                            )
                            HorizontalDivider()
                            years.forEach { year ->
                                DropdownMenuItem(
                                    text = { Text(year.toString()) },
                                    onClick = {
                                        onTimeRangeSelected(TimeRangeSelection.Year(year))
                                        expandedMenu = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.time_range_custom)) },
                                onClick = {
                                    onCustomRangeClicked()
                                    expandedMenu = false
                                }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefreshCurrentRange) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.refresh_corridors)
                        )
                    }
                    IconButton(onClick = { expandedOptionsMenu = true }) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.clear_corridor_cache)
                        )
                    }
                    DropdownMenu(
                        expanded = expandedOptionsMenu,
                        onDismissRequest = { expandedOptionsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.clear_corridor_cache)) },
                            onClick = {
                                expandedOptionsMenu = false
                                showClearDialog = true
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            mapContent()

            val progress = uiState.networkComputeProgress
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                )
            } else if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                )
            }

            if (uiState.maxTripCount > 1 &&
                uiState.render.geoJson != CorridorRender.EMPTY_FEATURE_COLLECTION
            ) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.trip_frequency),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(96.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        CorridorColorScale.COLOR_STOPS.map { it.second }
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.width(96.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "1",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${uiState.maxTripCount}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            if (uiState.transportStats.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .fillMaxWidth(0.75f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.transport_modes),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        uiState.transportStats.entries
                            .sortedByDescending { it.value }
                            .take(3)
                            .forEach { (type, pct) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(type.name, style = MaterialTheme.typography.bodySmall)
                                    Text("${(pct * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun YearlyMapContentLightPreview() {
    NanHistoryTheme(darkTheme = false) {
        YearlyMapContent(
            uiState = YearlyMapUiState(),
            onTimeRangeSelected = {},
            onCustomRangeClicked = {},
            onDismissBottomSheet = {},
            onEventClicked = {},
            onBackClicked = {},
            mapContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun YearlyMapContentDarkPreview() {
    NanHistoryTheme(darkTheme = true) {
        YearlyMapContent(
            uiState = YearlyMapUiState(),
            onTimeRangeSelected = {},
            onCustomRangeClicked = {},
            onDismissBottomSheet = {},
            onEventClicked = {},
            onBackClicked = {},
            mapContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        )
    }
}
