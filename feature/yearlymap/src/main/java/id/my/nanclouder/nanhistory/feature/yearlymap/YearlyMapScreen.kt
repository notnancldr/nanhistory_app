package id.my.nanclouder.nanhistory.feature.yearlymap

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.my.nanclouder.nanhistory.core.common.di.CoreContainerProvider
import id.my.nanclouder.nanhistory.core.common.navigation.IntentRegistry
import id.my.nanclouder.nanhistory.core.common.utils.getActivity
import id.my.nanclouder.nanhistory.ui.map.DateRangePickerModal

/**
 * Stateful screen container connecting [YearlyMapViewModel] to [YearlyMapContent] and [CorridorMap].
 */
@Composable
fun YearlyMapScreen(
    modifier: Modifier = Modifier,
    viewModel: YearlyMapViewModel = run {
        val context = LocalContext.current
        val container = (context.applicationContext as CoreContainerProvider).coreContainer
        remember {
            YearlyMapViewModel(
                eventRepository = container.eventRepository,
                locationRepository = container.locationRepository,
                corridorRepository = container.corridorRepository,
                context = context.applicationContext
            )
        }
    }
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDateRangePicker by rememberSaveable { mutableStateOf(false) }

    YearlyMapContent(
        uiState = uiState,
        onTimeRangeSelected = { viewModel.setTimeRange(it) },
        onCustomRangeClicked = { showDateRangePicker = true },
        onDismissBottomSheet = { viewModel.dismissBottomSheet() },
        onEventClicked = { eventId ->
            context.startActivity(IntentRegistry.eventDetail(context, eventId))
        },
        onBackClicked = { context.getActivity()?.finish() },
        onRefreshCurrentRange = { viewModel.refreshCurrentRange() },
        onClearAllCaches = { viewModel.clearAllCachesAndRecompute() },
        modifier = modifier,
        mapContent = {
            CorridorMap(
                render = uiState.render,
                timeRange = uiState.timeRange,
                initialBounds = uiState.initialBounds,
                onViewportChanged = { bounds, zoom ->
                    viewModel.onViewportChanged(bounds, zoom)
                },
                onEdgeSelected = { edgeId ->
                    viewModel.onEdgeSelected(edgeId)
                }
            )
        }
    )

    if (showDateRangePicker) {
        DateRangePickerModal(
            onDateRangeSelected = { start, end ->
                viewModel.setTimeRange(TimeRangeSelection.Custom(start, end))
                showDateRangePicker = false
            },
            onDismiss = { showDateRangePicker = false }
        )
    }
}
