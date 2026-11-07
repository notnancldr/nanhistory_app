package id.my.nanclouder.nanhistory.feature.sharemap.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.my.nanclouder.nanhistory.feature.sharemap.R

/**
 * Horizontally scrollable action bar located immediately below the TopAppBar.
 * Houses history management (Undo, Redo, Reset) and quick canvas/map settings.
 */
@Composable
fun ShareMapActionBar(
    canUndo: Boolean,
    canRedo: Boolean,
    showBackgroundMap: Boolean,
    backgroundMapTheme: String,
    isMapAdjustMode: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onReset: () -> Unit,
    onToggleBackgroundMap: () -> Unit,
    onSelectMapTheme: (String) -> Unit,
    onToggleMapAdjustMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var themeMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // History Management: Undo, Redo, Reset
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = stringResource(R.string.share_undo),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Redo,
                    contentDescription = stringResource(R.string.share_redo),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onReset,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.share_reset),
                    modifier = Modifier.size(20.dp)
                )
            }

            VerticalDivider(modifier = Modifier.height(24.dp))

            // Background Map Toggle
            FilterChip(
                selected = showBackgroundMap,
                onClick = onToggleBackgroundMap,
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Map,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text(stringResource(R.string.share_map_background_map), fontSize = 12.sp) }
            )

            // Background Map Theme Dropdown Chip
            if (showBackgroundMap) {
                Box {
                    val themeIcon = when (backgroundMapTheme) {
                        "LIGHT" -> Icons.Rounded.LightMode
                        "DARK" -> Icons.Rounded.DarkMode
                        else -> Icons.Rounded.SettingsBrightness
                    }
                    val themeLabel = when (backgroundMapTheme) {
                        "LIGHT" -> stringResource(R.string.share_map_theme_light)
                        "DARK" -> stringResource(R.string.share_map_theme_dark)
                        else -> stringResource(R.string.share_map_theme_system)
                    }

                    FilterChip(
                        selected = backgroundMapTheme != "SYSTEM",
                        onClick = { themeMenuExpanded = true },
                        leadingIcon = {
                            Icon(themeIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(themeLabel, fontSize = 12.sp) }
                    )

                    DropdownMenu(
                        expanded = themeMenuExpanded,
                        onDismissRequest = { themeMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.share_map_theme_system)) },
                            leadingIcon = { Icon(Icons.Rounded.SettingsBrightness, contentDescription = null) },
                            onClick = {
                                onSelectMapTheme("SYSTEM")
                                themeMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.share_map_theme_light)) },
                            leadingIcon = { Icon(Icons.Rounded.LightMode, contentDescription = null) },
                            onClick = {
                                onSelectMapTheme("LIGHT")
                                themeMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.share_map_theme_dark)) },
                            leadingIcon = { Icon(Icons.Rounded.DarkMode, contentDescription = null) },
                            onClick = {
                                onSelectMapTheme("DARK")
                                themeMenuExpanded = false
                            }
                        )
                    }
                }

                // Adjust Map Mode Toggle
                FilterChip(
                    selected = isMapAdjustMode,
                    onClick = onToggleMapAdjustMode,
                    leadingIcon = {
                        Icon(Icons.Rounded.ZoomOutMap, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text(stringResource(R.string.share_map_adjust_mode), fontSize = 12.sp) }
                )
            }
        }
    }
}

@Preview(name = "Light Mode")
@Composable
private fun ShareMapActionBarPreview() {
    MaterialTheme {
        ShareMapActionBar(
            canUndo = true,
            canRedo = false,
            showBackgroundMap = true,
            backgroundMapTheme = "SYSTEM",
            isMapAdjustMode = false,
            onUndo = {},
            onRedo = {},
            onReset = {},
            onToggleBackgroundMap = {},
            onSelectMapTheme = {},
            onToggleMapAdjustMode = {}
        )
    }
}

@Preview(name = "Dark Mode", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ShareMapActionBarDarkPreview() {
    MaterialTheme {
        ShareMapActionBar(
            canUndo = true,
            canRedo = true,
            showBackgroundMap = true,
            backgroundMapTheme = "DARK",
            isMapAdjustMode = true,
            onUndo = {},
            onRedo = {},
            onReset = {},
            onToggleBackgroundMap = {},
            onSelectMapTheme = {},
            onToggleMapAdjustMode = {}
        )
    }
}
