package id.my.nanclouder.nanhistory.utils.share

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.config.Config
import androidx.core.graphics.toColorInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareElementInspector(
    element: CanvasElement,
    onUpdateElement: (CanvasElement) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var templateText by remember(element.id) { mutableStateOf(element.style.templateFormat) }
    var fontSize by remember(element.id) { mutableFloatStateOf(element.style.fontSizeSp) }
    var cornerRadius by remember(element.id) { mutableFloatStateOf(element.style.cornerRadiusDp) }
    var paddingValue by remember(element.id) { mutableFloatStateOf(element.style.paddingDp) }
    var hasShadow by remember(element.id) { mutableStateOf(element.style.hasShadow) }
    var selectedTextColor by remember(element.id) { mutableStateOf(element.style.textColorHex) }
    var selectedBgColor by remember(element.id) { mutableStateOf(element.style.backgroundColorHex) }
    var rawProps by remember(element.id) { mutableStateOf(element.style.rawCustomProperties) }

    val showAdvanced = Config.experimentalAdvancedShareStyle.getCache()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Element Style: ${element.type.name}",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            // Template Formatting for Text & Title
            if (element.type == CanvasElementType.TITLE ||
                element.type == CanvasElementType.MARKDOWN_TEXT ||
                element.type == CanvasElementType.SINGLE_METRIC
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Format Template (Markdown supported)", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = templateText,
                        onValueChange = {
                            templateText = it
                            onUpdateElement(
                                element.copy(style = element.style.copy(templateFormat = it))
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    // Quick Token Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("{title}", "{date}", "{distance}", "{duration}", "{avg_speed}", "{max_speed}").forEach { token ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val newText = if (templateText.isEmpty()) token else "$templateText $token"
                                    templateText = newText
                                    onUpdateElement(
                                        element.copy(style = element.style.copy(templateFormat = newText))
                                    )
                                },
                                label = { Text(token, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // Font Size Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Font Size: ${fontSize.toInt()} sp", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = fontSize,
                    onValueChange = {
                        fontSize = it
                        onUpdateElement(
                            element.copy(style = element.style.copy(fontSizeSp = it))
                        )
                    },
                    valueRange = 10f..36f
                )
            }

            // Corner Radius Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Corner Radius: ${cornerRadius.toInt()} dp", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = cornerRadius,
                    onValueChange = {
                        cornerRadius = it
                        onUpdateElement(
                            element.copy(style = element.style.copy(cornerRadiusDp = it))
                        )
                    },
                    valueRange = 0f..32f
                )
            }

            // Padding Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Padding: ${paddingValue.toInt()} dp", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = paddingValue,
                    onValueChange = {
                        paddingValue = it
                        onUpdateElement(
                            element.copy(style = element.style.copy(paddingDp = it))
                        )
                    },
                    valueRange = 0f..24f
                )
            }

            // Background Presets
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Background Style", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val bgPresets = listOf(
                        "Transparent" to "#00000000",
                        "Subtle Dark" to "#40000000",
                        "Dark Pill" to "#80000000",
                        "Solid Dark" to "#E6101010",
                        "Light Pill" to "#CCFFFFFF"
                    )
                    bgPresets.forEach { (label, hex) ->
                        FilterChip(
                            selected = selectedBgColor == hex,
                            onClick = {
                                selectedBgColor = hex
                                onUpdateElement(
                                    element.copy(style = element.style.copy(backgroundColorHex = hex))
                                )
                            },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Text Color Swatches
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Text Color", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colors = listOf("#FFFFFF", "#F44336", "#4CAF50", "#2196F3", "#FFEB3B", "#FF9800", "#101010")
                    colors.forEach { hex ->
                        val color = Color(hex.toColorInt())
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedTextColor == hex) 3.dp else 1.dp,
                                    color = if (selectedTextColor == hex) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedTextColor = hex
                                    onUpdateElement(
                                        element.copy(style = element.style.copy(textColorHex = hex))
                                    )
                                }
                        )
                    }
                }
            }

            // Drop Shadow Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Text Drop Shadow", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = hasShadow,
                    onCheckedChange = {
                        hasShadow = it
                        onUpdateElement(
                            element.copy(style = element.style.copy(hasShadow = it))
                        )
                    }
                )
            }

            // Advanced Experimental Editor (gated by config)
            if (showAdvanced) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Experimental Raw Style Properties", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = rawProps,
                        onValueChange = {
                            rawProps = it
                            onUpdateElement(
                                element.copy(style = element.style.copy(rawCustomProperties = it))
                            )
                        },
                        placeholder = { Text("e.g. {\"customFilter\":\"contrast(1.2)\"}") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
