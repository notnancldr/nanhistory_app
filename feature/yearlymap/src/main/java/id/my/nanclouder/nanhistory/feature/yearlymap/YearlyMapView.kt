package id.my.nanclouder.nanhistory.feature.yearlymap

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Entry composable for the yearly aggregate map.
 * Delegates to the stateful [YearlyMapScreen].
 */
@Composable
fun YearlyMapView(
    modifier: Modifier = Modifier,
    viewModel: YearlyMapViewModel? = null
) {
    if (viewModel != null) {
        YearlyMapScreen(
            modifier = modifier,
            viewModel = viewModel
        )
    } else {
        YearlyMapScreen(
            modifier = modifier
        )
    }
}
