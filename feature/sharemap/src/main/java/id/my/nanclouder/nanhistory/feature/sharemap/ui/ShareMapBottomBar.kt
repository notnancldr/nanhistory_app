package id.my.nanclouder.nanhistory.feature.sharemap.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.my.nanclouder.nanhistory.feature.sharemap.R
import id.my.nanclouder.nanhistory.feature.sharemap.canvas.CanvasElement
import id.my.nanclouder.nanhistory.feature.sharemap.canvas.CanvasElementType
import id.my.nanclouder.nanhistory.feature.sharemap.canvas.ShareCardAspectRatio

/**
 * Docked bottom toolbar for the ShareMap studio screen.
 */
@Composable
fun ShareMapBottomBar(
    currentAspect: ShareCardAspectRatio,
    selectedElement: CanvasElement?,
    isMapAdjustMode: Boolean,
    onSelectAspect: (ShareCardAspectRatio) -> Unit,
    onAddElement: (CanvasElementType) -> Unit,
    onDuplicateElement: (String) -> Unit,
    onDeleteElement: (String) -> Unit,
    onOpenStyle: (CanvasElement) -> Unit,
    onLayerUp: (CanvasElement) -> Unit,
    onLayerDown: (CanvasElement) -> Unit,
    onFitRoute: () -> Unit,
    onDoneAdjustMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var addMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isMapAdjustMode) {
                // Map Adjust Mode Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.share_map_adjust_mode),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onFitRoute) {
                            Icon(Icons.Rounded.CropFree, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.share_map_fit_route), fontSize = 12.sp)
                        }

                        Button(onClick = onDoneAdjustMap) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.share_map_done), fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // Selected Element Quick Action Bar
                if (selectedElement != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Selected: ${selectedElement.type.name.replace("_", " ")}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onOpenStyle(selectedElement) }) {
                                    Icon(
                                        Icons.Rounded.Edit,
                                        contentDescription = "Edit Style",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = { onDuplicateElement(selectedElement.id) }) {
                                    Icon(
                                        Icons.Rounded.Layers,
                                        contentDescription = "Duplicate",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = { onLayerUp(selectedElement) }) {
                                    Icon(
                                        Icons.Rounded.KeyboardArrowUp,
                                        contentDescription = "Bring Forward",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = { onLayerDown(selectedElement) }) {
                                    Icon(
                                        Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = "Send Backward",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteElement(selectedElement.id) },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "Delete",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Main Studio Controls Row (Aspect Ratios + Add Element)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShareCardAspectRatio.values().forEach { aspect ->
                        FilterChip(
                            selected = currentAspect == aspect,
                            onClick = { onSelectAspect(aspect) },
                            label = { Text(aspect.label, fontSize = 12.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Add Element Dropdown Menu
                    Box {
                        Button(onClick = { addMenuExpanded = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.share_map_add_element), fontSize = 12.sp)
                        }

                        DropdownMenu(
                            expanded = addMenuExpanded,
                            onDismissRequest = { addMenuExpanded = false }
                        ) {
                            CanvasElementType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name.replace("_", " ")) },
                                    onClick = {
                                        onAddElement(type)
                                        addMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
