package id.my.nanclouder.nanhistory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            NanHistoryTheme {
                // A surface container using the 'background' color from the theme
                SettingsView()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSlider(
    configValue: Config.IntValue,
    title: String,
    description: String,
    valueUnit: String,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 1,
    icon: Painter ? = null,
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
//            Text(title)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView() {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            context.getActivity()!!.finish()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
        ) {
            ListItem(
                leadingContent = {
                     Icon(painterResource(R.drawable.ic_location), "Location")
                },
                headlineContent = {
                    Text("Location")
                }
            )
            SettingsSlider(
                title = "Accuracy Threshold",
                description = "Minimum accuracy of a location update.",
                configValue = Config.locationAccuracyThreshold,
                valueUnit = "m",
                valueRange = 5f..200f,
                steps = 0
            )
            SettingsSlider(
                title = "Minimum Distance",
                description = "The closest distance to update the location.",
                configValue = Config.locationMinimumDistance,
                valueUnit = "m",
                valueRange = 0f..200f,
                steps = 0
            )
            SettingsSlider(
                title = "Update Interval",
                description = "Shortest interval between location updates.",
                configValue = Config.locationUpdateInterval,
                valueUnit = "s",
                valueRange = 1f..60f,
                steps = 0
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    NanHistoryTheme {
        SettingsView()
    }
}