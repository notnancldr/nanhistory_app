package id.my.nanclouder.nanhistory.feature.eventdetail.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import id.my.nanclouder.nanhistory.feature.eventdetail.R

/**
 * Dialog allowing user to configure and save gradient stops for a color preset.
 */
@Composable
fun ColorPickerDialog(
    onDismissRequest: () -> Unit,
    onSelected: (colors: Map<Float, Color>, name: String) -> Unit,
    initialName: String = "",
    value: Map<Float, Color> = mapOf(0f to Color.Blue, 100f to Color.Red)
) {
    var colorName by remember(initialName) {
        mutableStateOf(initialName.ifBlank { "Custom Color" })
    }

    val stops = remember(value) {
        mutableStateListOf(*value.entries.map { it.key to it.value }.toTypedArray())
    }
    var selectedIdx by remember { mutableIntStateOf(0) }

    val colorController = rememberColorPickerController()
    var colorHex by remember { mutableStateOf("#ffffff") }
    var pickerActive by remember { mutableStateOf(false) }

    val initialMax = if (value.isNotEmpty()) value.maxOf { it.key } else 100f
    val initialMin = if (value.isNotEmpty()) value.minOf { it.key } else 0f
    var rangeStartText by remember { mutableStateOf(initialMin.toString()) }
    var rangeEndText by remember { mutableStateOf(initialMax.toString()) }

    val rangeStart = rangeStartText.toFloatOrNull()
    val rangeEnd = rangeEndText.toFloatOrNull()
    val isRangeValid = rangeStart != null && rangeEnd != null && rangeStart < rangeEnd
    val valueRange = if (rangeStart != null && rangeEnd != null && rangeStart < rangeEnd) {
        rangeStart..rangeEnd
    } else {
        -100f..100f
    }

    LaunchedEffect(selectedIdx, stops.size) {
        val color = stops.getOrNull(selectedIdx)?.second ?: Color.White
        colorController.selectByColor(color, true)
        colorHex = "#" + color.toArgb().and(0xFFFFFF).toString(16).padStart(6, '0')
    }

    fun updateSelectedColor(color: Color) {
        val idx = selectedIdx.coerceIn(0, stops.lastIndex)
        if (stops.isNotEmpty()) {
            stops[idx] = stops[idx].copy(second = color)
        }
    }

    fun strToColor(): Color? =
        colorHex.removePrefix("#").toLongOrNull(16)?.let { Color(0xff000000 + it) }

    val isColorValid = strToColor() != null

    LaunchedEffect(colorController.selectedColor.value) {
        if (!pickerActive) return@LaunchedEffect
        val c = colorController.selectedColor.value
        updateSelectedColor(c)
        colorHex = "#" + c.toArgb().and(0xFFFFFF).toString(16).padStart(6, '0')
    }

    LaunchedEffect(colorHex, pickerActive) {
        if (pickerActive) return@LaunchedEffect
        val c = strToColor() ?: return@LaunchedEffect
        colorController.selectByColor(c, true)
        updateSelectedColor(c)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.color_custom_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = colorName,
                    onValueChange = { colorName = it },
                    label = { Text(stringResource(R.string.color_name_label)) },
                    isError = colorName.isBlank(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                GradientStopBar(
                    stops = stops,
                    selectedIdx = selectedIdx,
                    valueRange = valueRange,
                    onSelectStop = { idx: Int -> selectedIdx = idx },
                    onMoveStop = { idx: Int, newPos: Float ->
                        stops[idx] = stops[idx].copy(first = newPos)
                        val sorted = stops.sortedBy { it.first }
                        val movedColor = stops[idx].second
                        stops.clear()
                        stops.addAll(sorted)
                        selectedIdx = sorted.indexOfFirst { it.second == movedColor }.coerceAtLeast(0)
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val cur = stops.getOrNull(selectedIdx)?.first ?: valueRange.start
                            val pos = (cur + 10f).coerceIn(valueRange.start, valueRange.endInclusive)
                            val newStop = pos to Color.Gray
                            stops.add(newStop)
                            val sorted = stops.sortedBy { it.first }
                            stops.clear()
                            stops.addAll(sorted)
                            selectedIdx = sorted.indexOfFirst { it === newStop }.coerceAtLeast(0)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.color_add_stop), maxLines = 1) }

                    OutlinedButton(
                        enabled = stops.size > 2,
                        onClick = {
                            stops.removeAt(selectedIdx.coerceIn(0, stops.lastIndex))
                            selectedIdx = selectedIdx.coerceIn(0, stops.lastIndex)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.color_remove_stop), maxLines = 1) }
                }

                HorizontalDivider()

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rangeStartText,
                        onValueChange = { rangeStartText = it },
                        label = { Text(stringResource(R.string.color_range_start)) },
                        isError = rangeStart == null || (rangeEnd != null && rangeStart >= rangeEnd),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = rangeEndText,
                        onValueChange = { rangeEndText = it },
                        label = { Text(stringResource(R.string.color_range_end)) },
                        isError = rangeEnd == null || (rangeStart != null && rangeStart >= rangeEnd),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = colorHex,
                        onValueChange = { raw ->
                            colorHex = raw.take(1).filter { it == '#' } + raw.removePrefix("#").take(6)
                            strToColor()?.let { colorController.selectByColor(it, false) }
                        },
                        leadingIcon = {
                            if (isColorValid) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(strToColor()!!, RoundedCornerShape(3.dp))
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.color_hex_invalid),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { pickerActive = !pickerActive },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.ArrowDropDown,
                                    contentDescription = "Toggle Picker",
                                    modifier = Modifier.rotate(if (pickerActive) 180f else 0f),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        label = { Text(stringResource(R.string.color_hex_label)) },
                        supportingText = if (!isColorValid) ({
                            Text(
                                stringResource(R.string.color_hex_invalid),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }) else null,
                        isError = !isColorValid,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    AnimatedVisibility(
                        visible = pickerActive,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        DisposableEffect(Unit) {
                            onDispose { pickerActive = false }
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                HsvColorPicker(
                                    controller = colorController,
                                    modifier = Modifier.requiredSize(110.dp),
                                    onColorChanged = {
                                        if (pickerActive) colorHex = "#" + it.hexCode.takeLast(6)
                                    },
                                    initialColor = strToColor()
                                )
                                BrightnessSlider(
                                    controller = colorController,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp),
                                    borderRadius = 100.dp,
                                    borderSize = 0.dp,
                                    initialColor = strToColor()
                                )
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.color_cancel))
            }
        },
        confirmButton = {
            Button(
                enabled = colorName.isNotBlank() && stops.isNotEmpty(),
                onClick = {
                    onSelected(stops.associate { it.first to it.second }, colorName)
                }
            ) {
                Text(stringResource(R.string.color_save))
            }
        }
    )
}
