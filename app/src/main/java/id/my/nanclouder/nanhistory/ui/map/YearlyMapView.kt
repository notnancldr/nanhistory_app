package id.my.nanclouder.nanhistory.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import id.my.nanclouder.nanhistory.feature.yearlymap.YearlyMapViewModel

@Composable
fun YearlyMapView(
    modifier: Modifier = Modifier,
    viewModel: YearlyMapViewModel? = null
) {
    id.my.nanclouder.nanhistory.feature.yearlymap.YearlyMapView(
        modifier = modifier,
        viewModel = viewModel
    )
}
