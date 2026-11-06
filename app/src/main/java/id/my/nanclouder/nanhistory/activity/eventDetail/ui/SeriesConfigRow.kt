package id.my.nanclouder.nanhistory.activity.eventDetail.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.utils.graph.ColorPreset
import id.my.nanclouder.nanhistory.utils.graph.SeriesConfig
import id.my.nanclouder.nanhistory.domain.logic.graph.SeriesDataSource
import id.my.nanclouder.nanhistory.utils.graph.StandardProperty

/**
 * Row displaying chips for configuring a single series' data source and color preset.
 */
@Composable
fun SeriesConfigRow(
    series: SeriesConfig,
    isDataSourceMenuOpen: Boolean,
    onOpenDataSourceMenu: () -> Unit,
    onCloseDataSourceMenu: () -> Unit,
    onSelectStandardProperty: (StandardProperty) -> Unit,
    onCustomClick: () -> Unit,
    isColorMenuOpen: Boolean,
    onOpenColorMenu: () -> Unit,
    onCloseColorMenu: () -> Unit,
    allColorPresets: List<ColorPreset>,
    onSelectPreset: (String) -> Unit,
    onNewColorClick: () -> Unit,
    onDeleteCustomColor: (String) -> Unit,
    canRemove: Boolean,
    onRemoveSeries: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val dataIcon: ImageVector = when (val ds = series.dataSource) {
            is SeriesDataSource.Standard -> when (ds.property) {
                StandardProperty.SPEED -> Icons.Rounded.Speed
                StandardProperty.ALTITUDE -> Icons.Rounded.Landscape
                else -> Icons.Rounded.Speed
            }
            is SeriesDataSource.Custom -> Icons.Rounded.Functions
        }

        Box {
            FilterChip(
                selected = true,
                onClick = onOpenDataSourceMenu,
                leadingIcon = {
                    Icon(dataIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                label = { Text(series.name) }
            )

            DataSourceMenu(
                expanded = isDataSourceMenuOpen,
                onDismissRequest = onCloseDataSourceMenu,
                onSelectStandardProperty = onSelectStandardProperty,
                onCustomClick = onCustomClick
            )
        }

        val colorPresetName = allColorPresets.firstOrNull { it.id == series.colorPresetId }?.name ?: "Color"
        Box {
            FilterChip(
                selected = false,
                onClick = onOpenColorMenu,
                leadingIcon = {
                    Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                label = { Text(colorPresetName) }
            )

            ColorSelectionMenu(
                expanded = isColorMenuOpen,
                onDismissRequest = onCloseColorMenu,
                allColorPresets = allColorPresets,
                onSelectPreset = onSelectPreset,
                onNewColorClick = onNewColorClick,
                onDeleteCustomColor = onDeleteCustomColor
            )
        }

        if (canRemove) {
            IconButton(
                onClick = onRemoveSeries,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.graph_remove_series),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
