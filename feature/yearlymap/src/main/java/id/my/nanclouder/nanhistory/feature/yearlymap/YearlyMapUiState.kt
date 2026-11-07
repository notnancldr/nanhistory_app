package id.my.nanclouder.nanhistory.feature.yearlymap

import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.model.TransportationType
import java.time.LocalDate
import java.time.Year
import org.osmdroid.util.BoundingBox

/**
 * Represents the user's selected time range for trip visualization.
 */
sealed interface TimeRangeSelection {
    data class Year(val year: Int) : TimeRangeSelection
    data object AllTime : TimeRangeSelection
    data class Custom(val start: LocalDate, val end: LocalDate) : TimeRangeSelection
}

/**
 * UI state for the yearly/range map screen.
 */
data class YearlyMapUiState(
    val timeRange: TimeRangeSelection = TimeRangeSelection.Year(Year.now().value),
    val initialBounds: BoundingBox? = null,
    val corridors: List<CorridorSegment> = emptyList(),
    val transportStats: Map<TransportationType, Float> = emptyMap(),
    val isLoading: Boolean = false,
    val selectedSegmentEvents: List<HistoryEvent> = emptyList(),
    val isBottomSheetVisible: Boolean = false,
    val render: CorridorRender = CorridorRender(),
    val selectedEdge: String? = null,
    val networkComputeProgress: Float? = null,
    val maxTripCount: Int = 1,
    val network: CorridorNetwork? = null
)
