package id.my.nanclouder.nanhistory.state

import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import id.my.nanclouder.nanhistory.utils.history.LocationData
import id.my.nanclouder.nanhistory.utils.share.CanvasElement
import id.my.nanclouder.nanhistory.utils.share.ShareCardAspectRatio

/**
 * Immutable UI state model for the dedicated ShareMap editing studio.
 */
data class ShareMapUiState(
    val isLoading: Boolean = true,
    val eventTitle: String = "",
    val dateString: String = "",
    val eventData: HistoryEvent? = null,
    val locations: List<LocationData> = emptyList(),
    val metrics: Map<String, String> = emptyMap(),
    val aspectRatio: ShareCardAspectRatio = ShareCardAspectRatio.SQUARE,
    val elements: List<CanvasElement> = emptyList(),
    val selectedElementId: String? = null,
    val inspectingElement: CanvasElement? = null,
    val showExportSheet: Boolean = false,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val showBackgroundMap: Boolean = true,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
)
