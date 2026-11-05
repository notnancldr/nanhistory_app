package id.my.nanclouder.nanhistory.state

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.my.nanclouder.nanhistory.db.AppDao
import id.my.nanclouder.nanhistory.db.toHistoryEvent
import id.my.nanclouder.nanhistory.utils.getLocationData
import id.my.nanclouder.nanhistory.utils.history.EventRange
import id.my.nanclouder.nanhistory.utils.share.CanvasElement
import id.my.nanclouder.nanhistory.utils.share.CanvasElementType
import id.my.nanclouder.nanhistory.utils.share.ShareCanvasPresets
import id.my.nanclouder.nanhistory.utils.share.ShareCardAspectRatio
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class ShareMapViewModel(
    private val eventId: String,
    private val dao: AppDao,
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareMapUiState())
    val uiState: StateFlow<ShareMapUiState> = _uiState.asStateFlow()

    private val undoStack = ArrayDeque<List<CanvasElement>>()
    private val redoStack = ArrayDeque<List<CanvasElement>>()

    init {
        loadEvent()
    }

    private fun recordHistory() {
        val currentElements = _uiState.value.elements
        undoStack.addLast(currentElements)
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
        val currentElements = _uiState.value.elements
        redoStack.addLast(currentElements)
        val previousElements = undoStack.removeLast()
        _uiState.update { it.copy(elements = previousElements) }
        updateUndoRedoAvailability()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val currentElements = _uiState.value.elements
        undoStack.addLast(currentElements)
        val nextElements = redoStack.removeLast()
        _uiState.update { it.copy(elements = nextElements) }
        updateUndoRedoAvailability()
    }

    fun loadEvent() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true) }
            val eventWithTags = dao.getEventById(eventId)
            if (eventWithTags == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            val historyEvent = eventWithTags.toHistoryEvent()
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
                selectedElementId = null
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
}
