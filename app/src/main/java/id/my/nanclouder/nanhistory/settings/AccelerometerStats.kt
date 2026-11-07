package id.my.nanclouder.nanhistory.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import id.my.nanclouder.nanhistory.domain.model.sensor.AccelerometerChange

@Composable
fun AccelerometerGraph(
    data: List<AccelerometerChange>,
    modifier: Modifier = Modifier,
    showLegend: Boolean = true
) {
    id.my.nanclouder.nanhistory.feature.settings.AccelerometerGraph(data, modifier, showLegend)
}

fun parseAccelerometerData(data: String): List<List<AccelerometerChange>> =
    id.my.nanclouder.nanhistory.feature.settings.parseAccelerometerData(data)
