package id.my.nanclouder.nanhistory.activity.eventDetail.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.ui.graph.MultiSeriesChart
import id.my.nanclouder.nanhistory.ui.graph.RenderedSeries
import id.my.nanclouder.nanhistory.utils.HistoryLocationData
import id.my.nanclouder.nanhistory.utils.TimeFormatterWithSecond
import id.my.nanclouder.nanhistory.utils.graph.DefaultGraphPresets
import id.my.nanclouder.nanhistory.utils.graph.GraphConfig
import id.my.nanclouder.nanhistory.utils.graph.GraphExpressionEvaluator
import id.my.nanclouder.nanhistory.utils.graph.GraphPreferencesRepository
import id.my.nanclouder.nanhistory.utils.graph.GraphScaleMode
import id.my.nanclouder.nanhistory.utils.graph.SeriesConfig
import id.my.nanclouder.nanhistory.domain.logic.graph.SeriesDataSource

@Composable
fun TripGraphs(
    locationData: List<HistoryLocationData>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val repository = remember { GraphPreferencesRepository(context) }
    val graphs by repository.graphs.collectAsState()

    var showPresetChooser by remember { mutableStateOf(false) }
    var editingGraph by remember { mutableStateOf<GraphConfig?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Surface(
        onClick = { expanded = !expanded },
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.trip_graphs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Icon(
                imageVector = if (!expanded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                contentDescription = if (!expanded) "Expand" else "Collapse"
            )
        }
    }

    AnimatedVisibility(visible = expanded, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (graphs.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.trip_graphs_empty_prompt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showPresetChooser = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(stringResource(R.string.graph_add))
                        }
                    }
                }
            } else {
                graphs.forEach { graph ->
                    GraphCard(
                        graph = graph,
                        locationData = locationData,
                        allColorPresets = repository.getAllColorPresets(),
                        onEditClick = {
                            editingGraph = graph
                            showEditor = true
                        },
                        onDeleteClick = {
                            repository.deleteGraph(graph.id)
                        },
                        onScaleModeChange = { newScale ->
                            repository.updateGraph(graph.copy(scaleMode = newScale))
                        }
                    )
                }

                FilledTonalButton(
                    onClick = { showPresetChooser = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(stringResource(R.string.graph_add))
                }
            }
        }
    }

    // Preset Chooser Dialog
    if (showPresetChooser) {
        PresetChooserDialog(
            onDismissRequest = { showPresetChooser = false },
            onSelectPreset = { presetGraph ->
                repository.addGraph(presetGraph)
                showPresetChooser = false
            },
            onCreateFromScratch = {
                showPresetChooser = false
                editingGraph = null
                showEditor = true
            }
        )
    }

    // Graph Editor Dialog
    if (showEditor) {
        GraphEditorDialog(
            initialGraph = editingGraph,
            locationData = locationData,
            allColorPresets = repository.getAllColorPresets(),
            onDismissRequest = {
                showEditor = false
                editingGraph = null
            },
            onSaveGraph = { savedGraph ->
                if (editingGraph != null) {
                    repository.updateGraph(savedGraph)
                } else {
                    repository.addGraph(savedGraph)
                }
                showEditor = false
                editingGraph = null
            },
            onSaveNewCustomColor = { customColor ->
                repository.saveCustomColor(customColor)
            },
            onDeleteCustomColor = { customColorId ->
                repository.deleteCustomColor(customColorId)
            }
        )
    }
}

@Composable
private fun GraphCard(
    graph: GraphConfig,
    locationData: List<HistoryLocationData>,
    allColorPresets: List<id.my.nanclouder.nanhistory.utils.graph.ColorPreset>,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onScaleModeChange: (GraphScaleMode) -> Unit
) {
    var scaleMenuExpanded by remember { mutableStateOf(false) }

    fun resolveColorStops(series: SeriesConfig): Map<Float, Color> {
        val custom = series.customColorStops
        if (!custom.isNullOrEmpty()) {
            return custom.mapValues { Color(it.value) }
        }
        val preset = allColorPresets.firstOrNull { it.id == series.colorPresetId }
            ?: DefaultGraphPresets.COLOR_PRESET_SPEED
        return preset.stops.mapValues { Color(it.value) }
    }

    val renderedSeriesList = remember(graph.series, locationData) {
        graph.series.map { series ->
            val values = when (val ds = series.dataSource) {
                is SeriesDataSource.Standard -> GraphExpressionEvaluator.evaluate(ds.property.key, locationData)
                is SeriesDataSource.Custom -> GraphExpressionEvaluator.evaluate(ds.expression, locationData)
            }
            val unit = when (val ds = series.dataSource) {
                is SeriesDataSource.Standard -> ds.property.defaultUnit
                is SeriesDataSource.Custom -> ds.unit
            }
            RenderedSeries(
                name = series.name,
                values = values,
                unit = unit,
                colorStops = resolveColorStops(series)
            )
        }
    }

    val timestamps = remember(locationData) {
        locationData.map { it.start.format(TimeFormatterWithSecond) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Title + Scale mode + Edit pencil + Delete
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = graph.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        IconButton(
                            onClick = { scaleMenuExpanded = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Tune,
                                contentDescription = stringResource(R.string.graph_scale_mode),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = scaleMenuExpanded,
                            onDismissRequest = { scaleMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.graph_scale_normalized)) },
                                onClick = {
                                    onScaleModeChange(GraphScaleMode.NORMALIZED)
                                    scaleMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.graph_scale_shared)) },
                                onClick = {
                                    onScaleModeChange(GraphScaleMode.SHARED_RAW)
                                    scaleMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.graph_scale_dual)) },
                                onClick = {
                                    onScaleModeChange(GraphScaleMode.DUAL_AXIS)
                                    scaleMenuExpanded = false
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.graph_edit_title),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.graph_delete),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Box(modifier = Modifier.height(4.dp))

            MultiSeriesChart(
                seriesList = renderedSeriesList,
                scaleMode = graph.scaleMode,
                timestamps = timestamps,
                height = 220.dp
            )
        }
    }
}
