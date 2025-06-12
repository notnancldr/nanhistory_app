package id.my.nanclouder.nanhistory.settings

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.lib.withHaptic
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
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val sliderState = remember {
        SliderState(value = configValue.get(context).toFloat(),valueRange = valueRange, steps = steps)
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
        trailingContent = {  },
        modifier = modifier
    )
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
//        overlineContent = {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text(title)
//                Text(
//                    "${sliderState.value.toInt()} $valueUnit",
//                    modifier = Modifier
//                        .width(56.dp),
//                    textAlign = TextAlign.End
//                )
//            }
//        },
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
fun SettingsSwitch(
    modifier: Modifier = Modifier,
    configValue: Config.BooleanValue,
    title: String,
    description: String,
    enabled: Boolean = true,
    icon: Painter? = null,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var active by rememberSaveable { mutableStateOf(configValue.get(context)) }

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
            Switch(
                checked = active,
                onCheckedChange = withHaptic<Boolean>(haptic) {
                    active = it
                    configValue.set(context, active)
                },
                enabled = enabled,
                modifier = Modifier.height(32.dp)
            )
        },
        modifier = modifier
    )
}

@Composable
fun CategoryHeader(icon: Painter, iconDescription: String, title: String) {
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
fun BarChart(segments: List<Pair<Float, Color>>, modifier: Modifier = Modifier, threshold: Float = 5f) {
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

