package id.my.nanclouder.nanhistory.utils.share

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.skydoves.colorpicker.compose.AlphaTile
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.config.Config

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
    var borderWidth by remember(element.id) { mutableFloatStateOf(element.style.borderWidthDp) }
    var hasShadow by remember(element.id) { mutableStateOf(element.style.hasShadow) }
    var selectedTextColor by remember(element.id) { mutableStateOf(element.style.textColorHex) }
    var selectedBgColor by remember(element.id) { mutableStateOf(element.style.backgroundColorHex) }
    var selectedBorderColor by remember(element.id) { mutableStateOf(element.style.borderColorHex) }
    var selectedMapTheme by remember(element.id) { mutableStateOf(element.style.mapTheme) }
    var rawProps by remember(element.id) { mutableStateOf(element.style.rawCustomProperties) }

    // Target for active color picker: "background", "border", "text", or null
    var activeColorPicker by remember { mutableStateOf<String?>(null) }
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
                    text = stringResource(R.string.share_element_style_title, element.type.name),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            // Template Formatting for Text Elements
            if (element.type == CanvasElementType.TITLE ||
                element.type == CanvasElementType.MARKDOWN_TEXT ||
                element.type == CanvasElementType.SINGLE_METRIC
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.share_template_label), style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = templateText,
                        onValueChange = {
                            templateText = it
                            onUpdateElement(element.copy(style = element.style.copy(templateFormat = it)))
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
                        val tokens = listOf(
                            "{title}", "{date}", "{start_time}", "{end_time}",
                            "{elevation_gain}", "{distance}", "{duration}",
                            "{avg_speed}", "{max_speed}", "{description}"
                        )
                        tokens.forEach { token ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val newText = if (templateText.isEmpty()) token else "$templateText $token"
                                    templateText = newText
                                    onUpdateElement(element.copy(style = element.style.copy(templateFormat = newText)))
                                },
                                label = { Text(token, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // Floating Map Theme
            if (element.type == CanvasElementType.FLOATING_MAP) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.share_floating_map_theme),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SYSTEM", "LIGHT", "DARK").forEach { theme ->
                            FilterChip(
                                selected = selectedMapTheme == theme,
                                onClick = {
                                    selectedMapTheme = theme
                                    onUpdateElement(element.copy(style = element.style.copy(mapTheme = theme)))
                                },
                                label = {
                                    val labelRes = when (theme) {
                                        "LIGHT" -> R.string.share_map_theme_light
                                        "DARK" -> R.string.share_map_theme_dark
                                        else -> R.string.share_map_theme_system
                                    }
                                    Text(stringResource(labelRes))
                                }
                            )
                        }
                    }
                }
            }

            // Background Color Picker Section
            ColorPickerRow(
                label = stringResource(R.string.share_color_background),
                hexColor = selectedBgColor,
                onOpenPicker = { activeColorPicker = "background" },
                onSetTransparent = {
                    selectedBgColor = "#00000000"
                    onUpdateElement(element.copy(style = element.style.copy(backgroundColorHex = "#00000000")))
                }
            )

            // Border & Outline Section
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.share_border_width, borderWidth.toInt()),
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = borderWidth,
                    onValueChange = {
                        borderWidth = it
                        onUpdateElement(element.copy(style = element.style.copy(borderWidthDp = it)))
                    },
                    valueRange = 0f..16f
                )

                ColorPickerRow(
                    label = stringResource(R.string.share_color_border),
                    hexColor = selectedBorderColor,
                    onOpenPicker = { activeColorPicker = "border" },
                    onSetTransparent = {
                        selectedBorderColor = "#00000000"
                        onUpdateElement(element.copy(style = element.style.copy(borderColorHex = "#00000000")))
                    }
                )
            }

            // Text Color Section (for elements containing text)
            if (element.type != CanvasElementType.FLOATING_MAP) {
                ColorPickerRow(
                    label = stringResource(R.string.share_color_text),
                    hexColor = selectedTextColor,
                    onOpenPicker = { activeColorPicker = "text" }
                )
            }

            // Sliders: Font Size, Corner Radius, Padding
            if (element.type != CanvasElementType.FLOATING_MAP) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.share_font_size, fontSize.toInt()),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = fontSize,
                        onValueChange = {
                            fontSize = it
                            onUpdateElement(element.copy(style = element.style.copy(fontSizeSp = it)))
                        },
                        valueRange = 10f..36f
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.share_corner_radius, cornerRadius.toInt()),
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = cornerRadius,
                    onValueChange = {
                        cornerRadius = it
                        onUpdateElement(element.copy(style = element.style.copy(cornerRadiusDp = it)))
                    },
                    valueRange = 0f..32f
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.share_padding, paddingValue.toInt()),
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = paddingValue,
                    onValueChange = {
                        paddingValue = it
                        onUpdateElement(element.copy(style = element.style.copy(paddingDp = it)))
                    },
                    valueRange = 0f..24f
                )
            }

            // Drop Shadow Toggle
            if (element.type != CanvasElementType.FLOATING_MAP) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.share_drop_shadow), style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = hasShadow,
                        onCheckedChange = {
                            hasShadow = it
                            onUpdateElement(element.copy(style = element.style.copy(hasShadow = it)))
                        }
                    )
                }
            }

            // Advanced Experimental Editor (gated by config)
            if (showAdvanced) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Experimental Raw Style Properties", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = rawProps,
                        onValueChange = {
                            rawProps = it
                            onUpdateElement(element.copy(style = element.style.copy(rawCustomProperties = it)))
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

    // Modal Dedicated Color Picker Dialog
    when (activeColorPicker) {
        "background" -> {
            ShareElementColorPickerDialog(
                title = stringResource(R.string.share_color_background),
                initialHexColor = selectedBgColor,
                onDismissRequest = { activeColorPicker = null },
                onColorSelected = { hex ->
                    selectedBgColor = hex
                    onUpdateElement(element.copy(style = element.style.copy(backgroundColorHex = hex)))
                    activeColorPicker = null
                }
            )
        }
        "border" -> {
            ShareElementColorPickerDialog(
                title = stringResource(R.string.share_color_border),
                initialHexColor = selectedBorderColor,
                onDismissRequest = { activeColorPicker = null },
                onColorSelected = { hex ->
                    selectedBorderColor = hex
                    onUpdateElement(element.copy(style = element.style.copy(borderColorHex = hex)))
                    activeColorPicker = null
                }
            )
        }
        "text" -> {
            ShareElementColorPickerDialog(
                title = stringResource(R.string.share_color_text),
                initialHexColor = selectedTextColor,
                onDismissRequest = { activeColorPicker = null },
                onColorSelected = { hex ->
                    selectedTextColor = hex
                    onUpdateElement(element.copy(style = element.style.copy(textColorHex = hex)))
                    activeColorPicker = null
                }
            )
        }
    }
}

@Composable
private fun ColorPickerRow(
    label: String,
    hexColor: String,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier,
    onSetTransparent: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                    .clickable { onOpenPicker() }
            ) {
                AlphaTile(
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    selectedColor = parseHexToColor(hexColor)
                )
            }

            Text(
                text = hexColor.uppercase(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            if (onSetTransparent != null && hexColor != "#00000000") {
                OutlinedButton(onClick = onSetTransparent) {
                    Text(stringResource(R.string.share_color_transparent), fontSize = 12.sp)
                }
            }

            OutlinedButton(onClick = onOpenPicker) {
                Icon(Icons.Rounded.Colorize, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pick", fontSize = 12.sp)
            }
        }
    }
}
