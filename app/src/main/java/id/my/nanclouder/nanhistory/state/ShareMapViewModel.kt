package id.my.nanclouder.nanhistory.state

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import id.my.nanclouder.nanhistory.utils.getLocationData
import id.my.nanclouder.nanhistory.utils.history.EventRange
import id.my.nanclouder.nanhistory.utils.history.getLocations
import id.my.nanclouder.nanhistory.utils.share.CanvasElement
import id.my.nanclouder.nanhistory.utils.share.CanvasElementType
import id.my.nanclouder.nanhistory.utils.share.ShareCanvasPresets
import id.my.nanclouder.nanhistory.utils.share.ShareCardAspectRatio
import id.my.nanclouder.nanhistory.utils.share.ShareTokenResolver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class ShareCanvasSnapshot(
    val elements: List<CanvasElement>,
    val centerLat: Double?,
    val centerLon: Double?,
    val zoom: Double?,
    val bgTheme: String,
    val previewWidthDp: Float?,
    val previewHeightDp: Float?
)

class ShareMapViewModel(
    private val eventId: String,
    private val eventRepository: EventRepository,
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareMapUiState())
    val uiState: StateFlow<ShareMapUiState> = _uiState.asStateFlow()

    private val undoStack = ArrayDeque<ShareCanvasSnapshot>()
    private val redoStack = ArrayDeque<ShareCanvasSnapshot>()

    init {
        loadEvent()
    }

    private fun getCurrentSnapshot(): ShareCanvasSnapshot {
        val s = _uiState.value
        return ShareCanvasSnapshot(
            elements = s.elements,
            centerLat = s.customMapCenterLat,
            centerLon = s.customMapCenterLon,
            zoom = s.customMapZoom,
            bgTheme = s.backgroundMapTheme,
            previewWidthDp = s.previewWidthDp,
            previewHeightDp = s.previewHeightDp
        )
    }

    private fun recordHistory() {
        undoStack.addLast(getCurrentSnapshot())
        if (undoStack.size > 20) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        updateUndoRedoAvailability()
    }

    private fun updateUndoRedoAvailability() {
        _uiState.update {
            it.copy(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(getCurrentSnapshot())
        val previous = undoStack.removeLast()
        _uiState.update {
            it.copy(
                elements = previous.elements,
                customMapCenterLat = previous.centerLat,
                customMapCenterLon = previous.centerLon,
                customMapZoom = previous.zoom,
                backgroundMapTheme = previous.bgTheme,
                previewWidthDp = previous.previewWidthDp,
                previewHeightDp = previous.previewHeightDp
            )
        }
        updateUndoRedoAvailability()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(getCurrentSnapshot())
        val next = redoStack.removeLast()
        _uiState.update {
            it.copy(
                elements = next.elements,
                customMapCenterLat = next.centerLat,
                customMapCenterLon = next.centerLon,
                customMapZoom = next.zoom,
                backgroundMapTheme = next.bgTheme,
                previewWidthDp = next.previewWidthDp,
                previewHeightDp = next.previewHeightDp
            )
        }
        updateUndoRedoAvailability()
    }

    fun loadEvent() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true) }
            val historyEvent = eventRepository.getEventById(eventId)
            if (historyEvent == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            val locations = historyEvent.getLocations(context)
            val historyLocations = locations.getLocationData()

            val duration = if (historyEvent is EventRange) {
                historyEvent.end.toEpochSecond() - historyEvent.time.toEpochSecond()
            } else 0L

            val dateStr = if (historyEvent is EventRange) {
                DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm").format(historyEvent.time)
            } else ""

            val dist = historyLocations.sumOf { it.distance.toDouble() }
            val maxSp = if (locations.isNotEmpty()) {
                kotlin.math.round((locations.maxOfOrNull { it.speed ?: 0f } ?: 0f) * 10) / 10
            } else 0f
            val avgSp = if (duration > 0 && dist > 0) {
                ((dist / 1000.0 / (duration / 3600.0)) * 10).roundToInt() / 10f
            } else 0f

            val metrics = mapOf(
                "distance" to String.format("%.2f km", dist / 1000.0),
                "duration" to "${duration / 60}m",
                "avg_speed" to String.format("%.1f km/h", avgSp),
                "max_speed" to String.format("%.1f km/h", maxSp)
            )

            val elevationGain = ShareTokenResolver.calculateElevationGain(locations)
            val initialAspect = ShareCardAspectRatio.SQUARE
            val defaultElements = ShareCanvasPresets.getDefaultElements(initialAspect)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    eventTitle = historyEvent.title,
                    dateString = dateStr,
                    eventData = historyEvent,
                    locations = locations,
                    metrics = metrics,
                    elevationGainMeters = elevationGain,
                    aspectRatio = initialAspect,
                    elements = defaultElements
                )
            }
        }
    }

    fun setAspectRatio(aspect: ShareCardAspectRatio) {
        if (_uiState.value.aspectRatio == aspect) return
        recordHistory()
        val clampedElements = _uiState.value.elements.map { it.clamped() }
        _uiState.update {
            it.copy(
                aspectRatio = aspect,
                elements = clampedElements
            )
        }
    }

    fun resetLayout() {
        recordHistory()
        val defaultElements = ShareCanvasPresets.getDefaultElements(_uiState.value.aspectRatio)
        _uiState.update {
            it.copy(
                elements = defaultElements,
                selectedElementId = null,
                customMapCenterLat = null,
                customMapCenterLon = null,
                customMapZoom = null,
                previewWidthDp = null,
                previewHeightDp = null
            )
        }
    }

    fun selectElement(id: String?) {
        _uiState.update { it.copy(selectedElementId = id) }
    }

    fun updateElement(element: CanvasElement) {
        _uiState.update { state ->
            val updated = state.elements.map { if (it.id == element.id) element else it }
            state.copy(elements = updated)
        }
    }

    fun onTransformCommit() {
        recordHistory()
    }

    fun deleteElement(id: String) {
        recordHistory()
        _uiState.update { state ->
            state.copy(
                elements = state.elements.filter { it.id != id },
                selectedElementId = if (state.selectedElementId == id) null else state.selectedElementId
            )
        }
    }

    fun duplicateElement(id: String) {
        val target = _uiState.value.elements.find { it.id == id } ?: return
        recordHistory()
        val duplicated = target.copy(
            id = java.util.UUID.randomUUID().toString(),
            xPercent = (target.xPercent + 0.05f).coerceIn(0f, 1f - target.widthPercent),
            yPercent = (target.yPercent + 0.05f).coerceIn(0f, 1f - target.heightPercent),
            zIndex = target.zIndex + 1f
        )
        _uiState.update { state ->
            state.copy(
                elements = state.elements + duplicated,
                selectedElementId = duplicated.id
            )
        }
    }

    fun addElement(type: CanvasElementType) {
        recordHistory()
        val newElem = CanvasElement(
            type = type,
            xPercent = 0.15f,
            yPercent = 0.35f,
            widthPercent = 0.7f,
            heightPercent = 0.15f
        )
        _uiState.update { state ->
            state.copy(
                elements = state.elements + newElem,
                selectedElementId = newElem.id
            )
        }
    }

    fun openStyleSheet(element: CanvasElement?) {
        _uiState.update { it.copy(inspectingElement = element) }
    }

    fun showExportSheet(show: Boolean) {
        _uiState.update { it.copy(showExportSheet = show) }
    }

    fun toggleBackgroundMap() {
        _uiState.update { it.copy(showBackgroundMap = !it.showBackgroundMap) }
    }

    fun setBackgroundMapTheme(theme: String) {
        if (_uiState.value.backgroundMapTheme == theme) return
        recordHistory()
        _uiState.update { it.copy(backgroundMapTheme = theme) }
    }

    fun setMapAdjustMode(enabled: Boolean) {
        _uiState.update { it.copy(isMapAdjustMode = enabled, selectedElementId = null) }
    }

    fun updateMapCamera(centerLat: Double?, centerLon: Double?, zoom: Double?) {
        _uiState.update {
            it.copy(
                customMapCenterLat = centerLat,
                customMapCenterLon = centerLon,
                customMapZoom = zoom
            )
        }
    }

    fun commitMapCamera(
        centerLat: Double?,
        centerLon: Double?,
        zoom: Double?,
        previewWidthDp: Float?,
        previewHeightDp: Float?
    ) {
        recordHistory()
        _uiState.update {
            it.copy(
                customMapCenterLat = centerLat,
                customMapCenterLon = centerLon,
                customMapZoom = zoom,
                previewWidthDp = previewWidthDp,
                previewHeightDp = previewHeightDp
            )
        }
    }

    fun resetMapCamera() {
        recordHistory()
        _uiState.update {
            it.copy(
                customMapCenterLat = null,
                customMapCenterLon = null,
                customMapZoom = null,
                previewWidthDp = null,
                previewHeightDp = null
            )
        }
    }
}
