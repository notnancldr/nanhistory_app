package id.my.nanclouder.nanhistory.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.getActivity
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSlider(
    configValue: Config.IntValue,
    title: String,
    description: String,
    valueUnit: String,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 1,
    icon: Painter? = null,
) {
    val context = LocalContext.current

    val sliderState = remember {
        SliderState(value = configValue.get(context).toFloat(),valueRange = valueRange, steps = steps)
    }
    sliderState.onValueChangeFinished = {
        configValue.set(context, sliderState.value.toInt())
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
        trailingContent = {  }
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
fun PieChart(segments: List<Pair<Float, Color>>, modifier: Modifier = Modifier) {
    val total = segments.sumOf { it.first.toDouble() }.toFloat()
    var startAngle = 0f

    Canvas(modifier = modifier) {
        for ((value, color) in segments) {
            val sweepAngle = (value / total) * 360f
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

