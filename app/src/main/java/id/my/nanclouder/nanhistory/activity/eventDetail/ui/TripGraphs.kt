package id.my.nanclouder.nanhistory.activity.eventDetail.ui

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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import id.my.nanclouder.nanhistory.ui.FilledLineChart
import id.my.nanclouder.nanhistory.ui.TooltipData
import id.my.nanclouder.nanhistory.utils.HistoryLocationData
import id.my.nanclouder.nanhistory.utils.TimeFormatterWithSecond

@Composable
fun TripGraphs(
    locationData: List<HistoryLocationData>
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        onClick = {
            expanded = !expanded
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Trip Graphs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!expanded) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Expand"
                )
            }
            else {
                Icon(
                    Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Collapse"
                )
            }
        }
    }

    AnimatedVisibility(
        visible = expanded,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val colorStops = mapOf(
                0f to Color.hsl(0f, 1.00f, 0.30f),       // Dark Red
                6f to Color.hsl(15f, 1.00f, 0.30f),      // Dark Orange-Red
                10f to Color.hsl(25f, 1.00f, 0.30f),     // Bright Orange

                // --- 4 New Evenly Spaced Stops ---
                29.6f to Color.hsl(74f, 1.00f, 0.30f),   // Lime Green / Yellow
                49.2f to Color.hsl(123f, 1.00f, 0.30f),  // Bright Green
                68.8f to Color.hsl(172f, 1.00f, 0.30f),  // Cyan / Teal
                88.4f to Color.hsl(221f, 1.00f, 0.30f),  // Bright Blue
                // ---------------------------------

                108f to Color.hsl(270f, 1.00f, 0.30f)    // Bright Purple
            )

            FilledLineChart(
                locationData.map { it.speed },
                unit = "km/h",
                color = MaterialTheme.colorScheme.primary,
                data = locationData.map {
                    listOf(
                        TooltipData(value = it.start.format(TimeFormatterWithSecond))
                    )
                },

                gradientColors = colorStops,
                strokeGradientColors = colorStops,
                height = 240.dp,
                showValueLabels = false
            )

            ColorPickerDialog(
                onDismissRequest = {},
                onSelected = { colors, name ->

                },
                value = colorStops
            )
        }
    }
}

@Composable
fun ColorPickerDialog(
    onDismissRequest: () -> Unit,
    onSelected: (colors: Map<Float, Color>, name: String) -> Unit,
    value: Map<Float, Color> = mapOf(1f to Color.White),
) {
    var colorName by remember { mutableStateOf("New Color") }

    val stops = remember(value) {
        mutableStateListOf(*value.entries.map { it.key to it.value }.toTypedArray())
    }
    var selectedIdx by remember { mutableIntStateOf(0) }

    val colorController = rememberColorPickerController()
    var colorHex by remember { mutableStateOf("#ffffff") }

    var pickerActive by remember { mutableStateOf(false) }

    var rangeStartText by remember { mutableStateOf("0") }
    var rangeEndText by remember { mutableStateOf(value.maxOf { it.key }.toString()) }

    val rangeStart = rangeStartText.toFloatOrNull()
    val rangeEnd = rangeEndText.toFloatOrNull()
    val isRangeValid = rangeStart != null && rangeEnd != null && rangeStart < rangeEnd
    val valueRange = if (isRangeValid) rangeStart..rangeEnd else 0f..1f

    // Sync HSV → hex field when the selected stop changes (only while picker is closed)
    LaunchedEffect(selectedIdx, stops.size) {
        // if (pickerActive) return@LaunchedEffect
        val color = stops.getOrNull(selectedIdx)?.second ?: Color.White
        colorController.selectByColor(color, true)
        colorHex = "#" + color.toArgb().and(0xFFFFFF).toString(16).padStart(6, '0')
    }

    fun updateSelectedColor(color: Color) {
        val idx = selectedIdx.coerceIn(0, stops.lastIndex)
        stops[idx] = stops[idx].copy(second = color)
    }

    fun strToColor(): Color? =
        colorHex.removePrefix("#").toLongOrNull(16)?.let { Color(0xff000000 + it) }

    val isColorValid = strToColor() != null

    // Picker wheel → update stop color + hex field
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
        title = { Text("Color Picker") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = colorName,
                    onValueChange = { colorName = it },
                    label = { Text("Color Name") },
                    isError = colorName.isBlank(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                GradientStopBar(
                    stops = stops,
                    selectedIdx = selectedIdx,
                    valueRange = valueRange,
                    onSelectStop = { selectedIdx = it },
                    onMoveStop = { idx, newPos ->
                        stops[idx] = stops[idx].copy(first = newPos)
                        val sorted = stops.sortedBy { it.first }
                        val movedColor = stops[idx].second
                        stops.clear()
                        stops.addAll(sorted)
                        selectedIdx = sorted.indexOfFirst { it.second == movedColor }
                            .coerceAtLeast(0)
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val pos = ((stops.getOrNull(selectedIdx)?.first ?: valueRange.start) + 0.15f)
                                .coerceIn(valueRange.start, valueRange.endInclusive)
                            val newStop = pos to Color.Gray
                            stops.add(newStop)
                            val sorted = stops.sortedBy { it.first }
                            stops.clear()
                            stops.addAll(sorted)
                            selectedIdx = sorted.indexOfFirst { it === newStop }.coerceAtLeast(0)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("+ Add Stop", maxLines = 1) }

                    OutlinedButton(
                        enabled = stops.size > 2,
                        onClick = {
                            stops.removeAt(selectedIdx.coerceIn(0, stops.lastIndex))
                            selectedIdx = selectedIdx.coerceIn(0, stops.lastIndex)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("− Remove", maxLines = 1) }
                }

                HorizontalDivider()

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rangeStartText,
                        onValueChange = { rangeStartText = it },
                        label = { Text("Range Start") },
                        isError = rangeStart == null || (rangeEnd != null && rangeStart >= rangeEnd),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = rangeEndText,
                        onValueChange = { rangeEndText = it },
                        label = { Text("Range End") },
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
                                        .size(12.dp)
                                        .background(strToColor()!!, RoundedCornerShape(3.dp))
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Invalid Color",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    pickerActive = !pickerActive
                                    if (pickerActive) {
                                        pickerActive = true
                                        strToColor()?.let { colorController.selectByColor(it, false) }
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.ArrowDropDown,
                                    contentDescription = "Color Picker",
                                    modifier = Modifier.rotate(if (pickerActive) 180f else 0f),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        label = { Text("Color (Hex)") },
                        supportingText = if (!isColorValid) ({
                            Text(
                                "Invalid hex color",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }) else null,
                        enabled = !pickerActive,
                        isError = !isColorValid,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    Column(Modifier.height(196.dp)) {
                        AnimatedVisibility(
                            visible = pickerActive,
                            modifier = Modifier.fillMaxWidth(),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                            label = "color_picker_visibility"
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
                                        modifier = Modifier.requiredSize(100.dp),
                                        onColorChanged = {
                                            if (pickerActive) colorHex =
                                                "#" + it.hexCode.takeLast(6)
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
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        },
        confirmButton = {
            Button(onClick = {
                onSelected(stops.associate { it.first to it.second }, colorName)
            }) {
                Text("Save")
            }
        }
    )
}

@Composable
fun GradientStopBar(
    stops: List<Pair<Float, Color>>,
    selectedIdx: Int,
    onSelectStop: (Int) -> Unit,
    onMoveStop: (idx: Int, newPos: Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    val handleRadius = 10.dp
    val barHeight = 28.dp

    val safeStops = if (stops.size <= 1) {
        stops + (0 until (2 - stops.size)).map { i ->
            (valueRange.start + i.toFloat()) to (stops.firstOrNull()?.second ?: Color.White)
        }
    } else stops

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight + handleRadius * 2)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .align(Alignment.TopCenter)
                .padding(horizontal = handleRadius)
        ) {
            val sorted = safeStops.sortedBy { it.first }
            // Normalise positions to 0..1 for the gradient brush
            val range = valueRange.endInclusive - valueRange.start
            val normStops = sorted.map { (pos, color) ->
                ((pos - valueRange.start) / range.coerceAtLeast(1e-6f)) to color
            }
            val brush = Brush.horizontalGradient(
                colorStops = normStops.map { (p, c) -> p to c }.toTypedArray()
            )
            drawRoundRect(brush = brush, cornerRadius = CornerRadius(8.dp.toPx()))
        }

        val density = LocalDensity.current
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            val trackWidthPx = with(density) { (maxWidth - handleRadius * 2).toPx() }
            val rangeSpan = valueRange.endInclusive - valueRange.start

            stops.forEachIndexed { idx, (pos, color) ->
                // Normalise pos → 0..1 for layout offset
                val normPos = (pos - valueRange.start) / rangeSpan.coerceAtLeast(1e-6f)
                val offsetX = handleRadius + (maxWidth - handleRadius * 2) * normPos

                Box(
                    modifier = Modifier
                        .offset(x = offsetX - handleRadius, y = 0.dp)
                        .size(handleRadius * 2)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (idx == selectedIdx) 2.5.dp else 1.dp,
                            color = if (idx == selectedIdx) Color.White else Color.LightGray,
                            shape = CircleShape
                        )
                        .pointerInput(idx) {
                            detectTapGestures { onSelectStop(idx) }
                        }
                        .pointerInput(idx, trackWidthPx, rangeSpan) {
                            detectDragGestures(
                                onDragStart = { onSelectStop(idx) }
                            ) { change, dragAmount ->
                                change.consume()
                                // Read current pos fresh from stops to avoid stale closure
                                val currentPos = stops.getOrNull(idx)?.first ?: return@detectDragGestures
                                val delta = dragAmount.x / trackWidthPx * rangeSpan
                                val newPos = (currentPos + delta)
                                    .coerceIn(valueRange.start, valueRange.endInclusive)
                                onMoveStop(idx, newPos)
                            }
                        }
                )
            }
        }
    }
}