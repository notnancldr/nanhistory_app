package id.my.nanclouder.nanhistory.feature.sharemap

import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.model.LocationData
import id.my.nanclouder.nanhistory.feature.sharemap.canvas.CanvasElement
import id.my.nanclouder.nanhistory.feature.sharemap.canvas.ShareCardAspectRatio

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
    val elevationGainMeters: Double? = null,
    val aspectRatio: ShareCardAspectRatio = ShareCardAspectRatio.SQUARE,
    val elements: List<CanvasElement> = emptyList(),
    val selectedElementId: String? = null,
    val inspectingElement: CanvasElement? = null,
    val showExportSheet: Boolean = false,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val showBackgroundMap: Boolean = true,
    val backgroundMapTheme: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val customMapCenterLat: Double? = null,
    val customMapCenterLon: Double? = null,
    val customMapZoom: Double? = null,
    val previewWidthDp: Float? = null,
    val previewHeightDp: Float? = null,
    val isMapAdjustMode: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
)
