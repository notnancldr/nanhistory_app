package id.my.nanclouder.nanhistory.activity.eventDetail.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.utils.graph.ColorPreset
import id.my.nanclouder.nanhistory.utils.graph.ColorPresetCategory

/**
 * Dropdown menu for selecting preset colors, quick colors, or custom colors.
 */
@Composable
fun ColorSelectionMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    allColorPresets: List<ColorPreset>,
    onSelectPreset: (presetId: String) -> Unit,
    onNewColorClick: () -> Unit,
    onDeleteCustomColor: (presetId: String) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        // --- 1. Preset Colors ---
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(
                text = stringResource(R.string.color_section_preset),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val presets = allColorPresets.filter { it.category == ColorPresetCategory.PRESET }
        presets.forEach { preset ->
            val icon: ImageVector = when (preset.name.lowercase()) {
                "altitude" -> Icons.Rounded.Landscape
                else -> Icons.Rounded.Speed
            }
            DropdownMenuItem(
                text = { Text(preset.name, style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                onClick = {
                    onSelectPreset(preset.id)
                    onDismissRequest()
                }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // --- 2. Quick Colors ---
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(
                text = stringResource(R.string.color_section_quick),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val quickColors = allColorPresets.filter { it.category == ColorPresetCategory.QUICK }
        quickColors.forEach { preset ->
            DropdownMenuItem(
                text = { Text(preset.name, style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    onSelectPreset(preset.id)
                    onDismissRequest()
                }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // --- 3. Custom Colors ---
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(
                text = stringResource(R.string.color_section_custom),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val customColors = allColorPresets.filter { it.category == ColorPresetCategory.CUSTOM }
        customColors.forEach { customPreset ->
            DropdownMenuItem(
                text = { Text(customPreset.name, style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { onDeleteCustomColor(customPreset.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                onClick = {
                    onSelectPreset(customPreset.id)
                    onDismissRequest()
                }
            )
        }

        DropdownMenuItem(
            text = {
                Text(
                    stringResource(R.string.color_custom_new),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = {
                onDismissRequest()
                onNewColorClick()
            }
        )
    }
}
