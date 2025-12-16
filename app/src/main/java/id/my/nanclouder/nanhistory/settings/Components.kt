package id.my.nanclouder.nanhistory.settings

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.utils.withHaptic
import kotlin.math.absoluteValue
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSlider(
    modifier: Modifier = Modifier,
    configValue: Config.IntValue,
    title: String,
    description: String,
    valueUnit: String,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 1,
    icon: Painter? = null,
) {
    val newUI by Config.appearanceNewUI.getState()
    if (newUI) SettingsSlider_New(
        modifier = modifier,
        configValue = configValue,
        title = title,
        description = description,
        valueUnit = valueUnit,
        valueRange = valueRange,
        steps = steps,
        icon = icon
    )
    else SettingsSlider_Old(
        modifier = modifier,
        configValue = configValue,
        title = title,
        description = description,
        valueUnit = valueUnit,
        valueRange = valueRange,
        steps = steps,
        icon = icon
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSlider_Old(
    modifier: Modifier = Modifier,
    configValue: Config.IntValue,
    title: String,
    description: String,
    valueUnit: String,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 1,
    icon: Painter? = null,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val sliderState = remember {
        SliderState(value = configValue.get(context).toFloat(), valueRange = valueRange, steps = steps)
    }

    var previousInt by remember { mutableIntStateOf(sliderState.value.toInt()) }

    sliderState.onValueChangeFinished = {
        configValue.set(context, sliderState.value.toInt())
    }
    if ((sliderState.value.toInt() - previousInt).absoluteValue >= 1) {
        previousInt = sliderState.value.toInt()
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    ListItem(
        leadingContent = {
            if (icon != null) Icon(icon, "Icon")
        },
        overlineContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title)
                Text(
                    "${sliderState.value.toInt()} $valueUnit",
                    modifier = Modifier
                        .width(56.dp),
                    textAlign = TextAlign.End
                )
            }
        },
        headlineContent = {
            Slider(state = sliderState, modifier = Modifier.height(32.dp))
        },
        supportingContent = {
            Text(description)
        },
        trailingContent = { },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSlider_New(
    modifier: Modifier = Modifier,
    configValue: Config.IntValue,
    title: String,
    description: String,
    valueUnit: String,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 1,
    icon: Painter? = null,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val sliderState = remember {
        SliderState(value = configValue.get(context).toFloat(), valueRange = valueRange, steps = steps)
    }

    var previousInt by remember { mutableIntStateOf(sliderState.value.toInt()) }

    sliderState.onValueChangeFinished = {
        configValue.set(context, sliderState.value.toInt())
    }
    if ((sliderState.value.toInt() - previousInt).absoluteValue >= 1) {
        previousInt = sliderState.value.toInt()
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        ListItem(
            leadingContent = {
                if (icon != null) Icon(
                    icon,
                    "Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                )
            },
            overlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${sliderState.value.toInt()} $valueUnit",
                        modifier = Modifier.width(64.dp),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            headlineContent = {
                Slider(state = sliderState, modifier = Modifier.height(36.dp))
            },
            supportingContent = {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = { },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SettingsNumInput(
    modifier: Modifier = Modifier,
    configValue: Config.IntValue,
    title: String,
    description: String,
    valueUnit: String,
    valueRange: IntRange = 0..1,
    icon: Painter? = null,
) {
    val newUI by Config.appearanceNewUI.getState()
    if (newUI) SettingsNumInput_New(
        modifier = modifier,
        configValue = configValue,
        title = title,
        description = description,
        valueUnit = valueUnit,
        valueRange = valueRange,
        icon = icon
    )
    else SettingsNumInput_Old(
        modifier = modifier,
        configValue = configValue,
        title = title,
        description = description,
        valueUnit = valueUnit,
        valueRange = valueRange,
        icon = icon
    )
}

@Composable
fun SettingsNumInput_Old(
    modifier: Modifier = Modifier,
    configValue: Config.IntValue,
    title: String,
    description: String,
    valueUnit: String,
    valueRange: IntRange = 0..1,
    icon: Painter? = null,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue("${configValue.get(context)}"))
    }

    fun saveData(value: TextFieldValue = textFieldValue, changeValue: Boolean = false) {
        val intValue = value.text.toIntOrNull()?.coerceIn(valueRange) ?: valueRange.first
        if (changeValue) textFieldValue = TextFieldValue(
            "$intValue",
            textFieldValue.selection,
            textFieldValue.composition
        )
        configValue.set(context, intValue)
    }

    ListItem(
        leadingContent = {
            if (icon != null) Icon(icon, "Icon")
        },
        headlineContent = {
            Text(title)
        },
        supportingContent = {
            Text(description)
        },
        trailingContent = {
            OutlinedTextField(
                textFieldValue,
                onValueChange = {
                    textFieldValue = it
                    saveData(it)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        saveData(changeValue = true)
                        keyboard?.hide()
                        focusManager.clearFocus()
                    }
                ),
                suffix = {
                    Text(valueUnit)
                },
                modifier = Modifier.width(96.dp),
                singleLine = true
            )
        },
        modifier = modifier
    )
}

@Composable
fun SettingsNumInput_New(
    modifier: Modifier = Modifier,
    configValue: Config.IntValue,
    title: String,
    description: String,
    valueUnit: String,
    valueRange: IntRange = 0..1,
    icon: Painter? = null,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue("${configValue.get(context)}"))
    }

    fun saveData(value: TextFieldValue = textFieldValue, changeValue: Boolean = false) {
        val intValue = value.text.toIntOrNull()?.coerceIn(valueRange) ?: valueRange.first
        if (changeValue) textFieldValue = TextFieldValue(
            "$intValue",
            textFieldValue.selection,
            textFieldValue.composition
        )
        configValue.set(context, intValue)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        ListItem(
            leadingContent = {
                if (icon != null) Icon(
                    icon,
                    "Icon",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            headlineContent = {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            },
            supportingContent = {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                OutlinedTextField(
                    textFieldValue,
                    onValueChange = {
                        textFieldValue = it
                        saveData(it)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            saveData(changeValue = true)
                            keyboard?.hide()
                            focusManager.clearFocus()
                        }
                    ),
                    suffix = {
                        Text(valueUnit)
                    },
                    modifier = Modifier.width(100.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SettingsSwitch(
    modifier: Modifier = Modifier,
    configValue: Config.BooleanValue,
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    icon: Painter? = null,
    onUpdated: ((Boolean) -> Unit)? = null
) {
    val newUI by Config.appearanceNewUI.getState()
    if (newUI) SettingsSwitch_New(
        modifier = modifier,
        configValue = configValue,
        title = title,
        description = description,
        enabled = enabled,
        icon = icon,
        onUpdated = onUpdated
    )
    else SettingsSwitch_Old(
        modifier = modifier,
        configValue = configValue,
        title = title,
        description = description,
        enabled = enabled,
        icon = icon,
        onUpdated = onUpdated
    )
}

@Composable
fun SettingsSwitch_Old(
    modifier: Modifier = Modifier,
    configValue: Config.BooleanValue,
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    icon: Painter? = null,
    onUpdated: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var active by rememberSaveable { mutableStateOf(configValue.get(context)) }

    val color = if (enabled) Color.Unspecified else Color.Gray

    ListItem(
        leadingContent = {
            if (icon != null) Icon(icon, "Icon")
        },
        headlineContent = {
            Text(title, color = color)
        },
        supportingContent = if (description != null) ({
            Text(description, color = color)
        }) else null,
        trailingContent = {
            Switch(
                checked = active,
                onCheckedChange = withHaptic<Boolean>(haptic) {
                    active = it
                    configValue.set(context, active)
                    onUpdated?.invoke(it)
                },
                enabled = enabled,
                modifier = Modifier.height(32.dp)
            )
        },
        modifier = modifier
    )
}

@Composable
fun SettingsSwitch_New(
    modifier: Modifier = Modifier,
    configValue: Config.BooleanValue,
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    icon: Painter? = null,
    onUpdated: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var active by rememberSaveable { mutableStateOf(configValue.get(context)) }

    val backgroundColor by animateColorAsState(
        targetValue = if (active)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        label = "backgroundColor"
    )

    val textColor = if (enabled)
        MaterialTheme.colorScheme.onSurface
    else
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        ListItem(
            leadingContent = {
                if (icon != null) Icon(
                    icon,
                    "Icon",
                    tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                )
            },
            headlineContent = {
                Text(
                    title,
                    color = textColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            supportingContent = if (description != null) ({
                Text(
                    description,
                    color = textColor.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }) else null,
            trailingContent = {
                Switch(
                    checked = active,
                    onCheckedChange = withHaptic<Boolean>(haptic) {
                        active = it
                        configValue.set(context, active)
                        onUpdated?.invoke(it)
                    },
                    enabled = enabled,
                    modifier = Modifier.height(32.dp)
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <E : Enum<E>> SettingsDropdown(
    modifier: Modifier = Modifier,
    configValue: Config.EnumValue<E>,
    enumClass: Class<E>,
    title: String,
    description: String,
    icon: Painter? = null,
    onUpdated: ((E) -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var expanded by remember { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf(configValue.get(context)) }

    val allEnumValues = remember {
        (enumClass.enumConstants as Array<E>?)?.toList() ?: emptyList()
    }

    Log.d("NanHistoryDebug", "enumValues: $allEnumValues")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        ListItem(
            leadingContent = {
                if (icon != null) Icon(
                    icon,
                    "Icon",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            headlineContent = {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            },
            supportingContent = {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selected.name,
                        onValueChange = {},
                        modifier = Modifier
                            .width(120.dp)
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        readOnly = true,
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.labelMedium,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        allEnumValues.forEach { enumValue ->
                            DropdownMenuItem(
                                text = {
                                    Text(enumValue.name, style = MaterialTheme.typography.bodyMedium)
                                },
                                onClick = withHaptic(haptic) {
                                    selected = enumValue
                                    configValue.set(context, enumValue)
                                    onUpdated?.invoke(enumValue)
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

@Composable
fun CategoryHeader(icon: Painter, iconDescription: String, title: String) {
    val newUI by Config.appearanceNewUI.getState()
    if (newUI) CategoryHeader_New(icon = icon, iconDescription = iconDescription, title = title)
    else CategoryHeader_Old(icon = icon, iconDescription = iconDescription, title = title)
}

@Composable
fun CategoryHeader_Old(icon: Painter, iconDescription: String, title: String) {
    ListItem(
        leadingContent = {
            Icon(icon, iconDescription)
        },
        headlineContent = {
            Text(title)
        }
    )
}

@Composable
fun CategoryHeader_New(icon: Painter, iconDescription: String, title: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp)
    ) {
        ListItem(
            leadingContent = {
                Icon(
                    icon,
                    iconDescription,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            headlineContent = {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        )
    }
}

@Composable
fun BarChart(segments: List<Pair<Float, Color>>, modifier: Modifier = Modifier, threshold: Float = 5f) {
    val newUI by Config.appearanceNewUI.getState()
    if (newUI) BarChart_New(segments = segments, modifier = modifier, threshold = threshold)
    else BarChart_Old(segments = segments, modifier = modifier, threshold = threshold)
}

@Composable
fun BarChart_Old(segments: List<Pair<Float, Color>>, modifier: Modifier = Modifier, threshold: Float = 5f) {
    val shownSegments = segments.filter { it.first > threshold }
    val total = shownSegments.sumOf { it.first.toDouble() }.toFloat()
    var offset = 0f

    Canvas(modifier = modifier.clip(RoundedCornerShape(32.dp)).background(Color.White)) {
        val barWidth = size.width
        for ((index, segment) in shownSegments.withIndex()) {
            val (value, color) = segment
            val percentage = (value / total)
            val gap = if (index < shownSegments.size - 1) 10 else 0
            val elementWidth = max(percentage * barWidth - gap, 0f)
            drawIntoCanvas { _ ->
                drawRect(
                    color = color,
                    topLeft = Offset(offset, 0f),
                    size = Size(elementWidth, size.height)
                )
            }
            offset += elementWidth + gap
        }
    }
}

@Composable
fun BarChart_New(
    segments: List<Pair<Float, Color>>,
    modifier: Modifier = Modifier,
    threshold: Float = 5f
) {
    val shownSegments = segments.filter { it.first > threshold }
    val total = shownSegments.sumOf { it.first.toDouble() }.toFloat()
    var offset = 0f

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        val barWidth = size.width
        val segmentGap = 8f

        for ((index, segment) in shownSegments.withIndex()) {
            val (value, color) = segment
            val percentage = (value / total)
            val gap = if (index < shownSegments.size - 1) segmentGap else 0f
            val elementWidth = maxOf(percentage * barWidth - gap, 0f)

            // Draw segment with smooth corners
            drawRoundRect(
                color = color,
                topLeft = Offset(offset, 0f),
                size = Size(elementWidth, size.height),
                cornerRadius = CornerRadius(12f, 12f)
            )

            offset += elementWidth + gap
        }
    }
}

@Composable
fun PieChart(segments: List<Pair<Float, Color>>, modifier: Modifier = Modifier) {
    val total = segments.sumOf { it.first.toDouble() }.toFloat()
    var startAngle = 0f

    Canvas(modifier = modifier) {
        for ((value, color) in segments) {
            val sweepAngle = max((value / total) * 360f, 0f)
            drawIntoCanvas { _ ->
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = size.width / 5)
                )
            }
            startAngle += sweepAngle
        }
    }
}

