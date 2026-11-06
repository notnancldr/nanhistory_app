package id.my.nanclouder.nanhistory.ui.map

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.my.nanclouder.nanhistory.NanHistoryApplication
import id.my.nanclouder.nanhistory.activity.eventDetail.EventDetailActivity
import id.my.nanclouder.nanhistory.state.TimeRangeSelection
import id.my.nanclouder.nanhistory.state.YearlyMapViewModel
import id.my.nanclouder.nanhistory.ui.map.maplibre.CorridorMap

/**
 * Stateful screen container connecting [YearlyMapViewModel] to [YearlyMapContent] and [CorridorMap].
 */
@Composable
fun YearlyMapScreen(
    modifier: Modifier = Modifier,
    viewModel: YearlyMapViewModel = run {
        val context = LocalContext.current
        val container = (context.applicationContext as NanHistoryApplication).container
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
            val intent = Intent(context, EventDetailActivity::class.java).apply {
                putExtra("eventId", eventId)
            }
            context.startActivity(intent)
        },
        onBackClicked = { context.findActivity()?.finish() },
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

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
