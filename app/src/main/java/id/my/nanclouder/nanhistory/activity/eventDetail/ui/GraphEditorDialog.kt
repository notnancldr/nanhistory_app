package id.my.nanclouder.nanhistory.activity.eventDetail.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.ui.graph.MultiSeriesChart
import id.my.nanclouder.nanhistory.ui.graph.RenderedSeries
import id.my.nanclouder.nanhistory.utils.HistoryLocationData
import id.my.nanclouder.nanhistory.utils.TimeFormatterWithSecond
import id.my.nanclouder.nanhistory.utils.graph.ColorPreset
import id.my.nanclouder.nanhistory.utils.graph.DefaultGraphPresets
import id.my.nanclouder.nanhistory.utils.graph.GraphConfig
import id.my.nanclouder.nanhistory.utils.graph.GraphExpressionEvaluator
import id.my.nanclouder.nanhistory.utils.graph.GraphScaleMode
import id.my.nanclouder.nanhistory.utils.graph.SeriesConfig
import id.my.nanclouder.nanhistory.domain.logic.graph.SeriesDataSource
import id.my.nanclouder.nanhistory.utils.graph.StandardProperty
import java.util.UUID

/**
 * Full editor dialog for creating or customizing a multi-series graph.
 */
@Composable
fun GraphEditorDialog(
    initialGraph: GraphConfig?,
    locationData: List<HistoryLocationData>,
    allColorPresets: List<ColorPreset>,
    onDismissRequest: () -> Unit,
    onSaveGraph: (GraphConfig) -> Unit,
    onSaveNewCustomColor: (ColorPreset) -> Unit,
    onDeleteCustomColor: (String) -> Unit
) {
    var graphTitle by remember {
        mutableStateOf(initialGraph?.title ?: "Graph 1")
    }

    var scaleMode by remember {
        mutableStateOf(initialGraph?.scaleMode ?: GraphScaleMode.NORMALIZED)
    }

    val seriesList = remember {
        mutableStateListOf<SeriesConfig>().apply {
            if (initialGraph != null && initialGraph.series.isNotEmpty()) {
                addAll(initialGraph.series)
            } else {
                add(
                    SeriesConfig(
                        name = "Speed",
                        dataSource = SeriesDataSource.Standard(StandardProperty.SPEED),
                        colorPresetId = DefaultGraphPresets.COLOR_PRESET_SPEED.id
                    )
                )
            }
        }
    }

    var activeSeriesIndexForDataSource by remember { mutableIntStateOf(-1) }
    var activeSeriesIndexForColor by remember { mutableIntStateOf(-1) }
    var editingCustomSeriesIndex by remember { mutableIntStateOf(-1) }
    var showNewColorDialog by remember { mutableStateOf(false) }

    fun resolveColorStops(series: SeriesConfig): Map<Float, Color> {
        val custom = series.customColorStops
        if (!custom.isNullOrEmpty()) {
            return custom.mapValues { Color(it.value) }
        }
        val preset = allColorPresets.firstOrNull { it.id == series.colorPresetId }
            ?: DefaultGraphPresets.COLOR_PRESET_SPEED
        return preset.stops.mapValues { Color(it.value) }
    }

    val renderedSeriesList = remember(seriesList.toList(), locationData) {
        seriesList.map { series ->
            val values = when (val ds = series.dataSource) {
                is SeriesDataSource.Standard -> {
                    GraphExpressionEvaluator.evaluate(ds.property.key, locationData)
                }
                is SeriesDataSource.Custom -> {
                    GraphExpressionEvaluator.evaluate(ds.expression, locationData)
                }
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

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Title & Done Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedTextField(
                        value = graphTitle,
                        onValueChange = { graphTitle = it },
                        label = { Text(stringResource(R.string.graph_name_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.titleMedium
                    )

                    Button(
                        onClick = {
                            val finalGraph = GraphConfig(
                                id = initialGraph?.id ?: UUID.randomUUID().toString(),
                                title = graphTitle.ifBlank { "Graph" },
                                series = seriesList.toList(),
                                scaleMode = scaleMode
                            )
                            onSaveGraph(finalGraph)
                        },
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Text(stringResource(R.string.graph_done))
                    }
                }

                HorizontalDivider()

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Rounded.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.graph_scale_mode) + ":",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Scale Mode Selector
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = scaleMode == GraphScaleMode.NORMALIZED,
                        onClick = { scaleMode = GraphScaleMode.NORMALIZED },
                        label = { Text(stringResource(R.string.graph_scale_normalized)) }
                    )
                    FilterChip(
                        selected = scaleMode == GraphScaleMode.SHARED_RAW,
                        onClick = { scaleMode = GraphScaleMode.SHARED_RAW },
                        label = { Text(stringResource(R.string.graph_scale_shared)) }
                    )
                    FilterChip(
                        selected = scaleMode == GraphScaleMode.DUAL_AXIS,
                        onClick = { scaleMode = GraphScaleMode.DUAL_AXIS },
                        label = { Text(stringResource(R.string.graph_scale_dual)) }
                    )
                }

                HorizontalDivider()

                // Flowing Series Rows/Chips
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    seriesList.forEachIndexed { sIdx, series ->
                        SeriesConfigRow(
                            series = series,
                            isDataSourceMenuOpen = activeSeriesIndexForDataSource == sIdx,
                            onOpenDataSourceMenu = { activeSeriesIndexForDataSource = sIdx },
                            onCloseDataSourceMenu = { activeSeriesIndexForDataSource = -1 },
                            onSelectStandardProperty = { prop ->
                                seriesList[sIdx] = series.copy(
                                    name = prop.name.lowercase().replaceFirstChar { it.uppercase() },
                                    dataSource = SeriesDataSource.Standard(prop)
                                )
                            },
                            onCustomClick = { editingCustomSeriesIndex = sIdx },
                            isColorMenuOpen = activeSeriesIndexForColor == sIdx,
                            onOpenColorMenu = { activeSeriesIndexForColor = sIdx },
                            onCloseColorMenu = { activeSeriesIndexForColor = -1 },
                            allColorPresets = allColorPresets,
                            onSelectPreset = { pId ->
                                seriesList[sIdx] = series.copy(colorPresetId = pId, customColorStops = null)
                            },
                            onNewColorClick = { showNewColorDialog = true },
                            onDeleteCustomColor = onDeleteCustomColor,
                            canRemove = seriesList.size > 1,
                            onRemoveSeries = { seriesList.removeAt(sIdx) }
                        )
                    }

                    // [+ Add Series] Button (up to 5 series)
                    if (seriesList.size < 5) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                val newIndex = seriesList.size + 1
                                seriesList.add(
                                    SeriesConfig(
                                        name = "Series $newIndex",
                                        dataSource = SeriesDataSource.Standard(StandardProperty.SPEED),
                                        colorPresetId = DefaultGraphPresets.COLOR_PRESET_SPEED.id
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            label = { Text(stringResource(R.string.graph_add_series)) }
                        )
                    }
                }

                // Preview Label & Canvas
                Text(
                    text = stringResource(R.string.graph_preview),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    MultiSeriesChart(
                        seriesList = renderedSeriesList,
                        scaleMode = scaleMode,
                        timestamps = timestamps,
                        height = 220.dp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }

    // Custom Expression Dialog
    if (editingCustomSeriesIndex in seriesList.indices) {
        val series = seriesList[editingCustomSeriesIndex]
        val currentExpr = (series.dataSource as? SeriesDataSource.Custom)?.expression ?: "speed - speed[-1]"
        val currentUnit = (series.dataSource as? SeriesDataSource.Custom)?.unit ?: "km/h"

        CustomExpressionDialog(
            initialExpression = currentExpr,
            initialUnit = currentUnit,
            onDismissRequest = { editingCustomSeriesIndex = -1 },
            onApply = { newExpr, newUnit ->
                seriesList[editingCustomSeriesIndex] = series.copy(
                    name = "Custom",
                    dataSource = SeriesDataSource.Custom(newExpr, newUnit)
                )
                editingCustomSeriesIndex = -1
            }
        )
    }

    // New Color Palette Dialog
    if (showNewColorDialog) {
        ColorPickerDialog(
            onDismissRequest = { showNewColorDialog = false },
            onSelected = { colors, name ->
                val newPreset = ColorPreset(
                    id = "custom_${UUID.randomUUID()}",
                    name = name.ifBlank { "Custom" },
                    category = id.my.nanclouder.nanhistory.utils.graph.ColorPresetCategory.CUSTOM,
                    stops = colors.mapValues { it.value.value.toLong() },
                )
                onSaveNewCustomColor(newPreset)
                showNewColorDialog = false
            }
        )
    }
}
