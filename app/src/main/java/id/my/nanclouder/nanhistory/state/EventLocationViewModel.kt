package id.my.nanclouder.nanhistory.state

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.my.nanclouder.nanhistory.db.AppDao
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toEventEntity
import id.my.nanclouder.nanhistory.db.toHistoryEvent
import id.my.nanclouder.nanhistory.db.toLocationData
import id.my.nanclouder.nanhistory.db.toLocationEntity
import id.my.nanclouder.nanhistory.utils.history.EventRange
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import id.my.nanclouder.nanhistory.utils.history.LocationData
import id.my.nanclouder.nanhistory.utils.history.generateEventId
import id.my.nanclouder.nanhistory.utils.matchOrNull
import id.my.nanclouder.nanhistory.utils.signature.generateSignature
import id.my.nanclouder.nanhistory.utils.signature.validateSignature
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.BoundingBox
import java.io.File
import java.time.ZonedDateTime
import kotlin.math.roundToInt

data class EventLocationUiState(
    val event: HistoryEvent? = null,
    val initialBounds: BoundingBox? = null,
    val firstLocation: LocationData? = null,
    val lastLocation: LocationData? = null,
    val renderedLocations: List<LocationData> = emptyList(),
    val isRecording: Boolean = false,
    val cutMode: Boolean = false,
    val cutStart: ZonedDateTime? = null,
    val cutEnd: ZonedDateTime? = null,
    val isLoading: Boolean = false,
)

class EventLocationViewModel(
    private val dao: AppDao,
    private val eventId: String,
    startAsCutMode: Boolean = false,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        EventLocationUiState(cutMode = startAsCutMode)
    )
    val uiState: StateFlow<EventLocationUiState> = _uiState.asStateFlow()

    private var debounceJob: Job? = null
    private var lastQueriedBounds: BoundingBox? = null
    private var lastQueriedStep: Int = -1

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true) }
            dao.getEventFlowById(eventId).collect { entity ->
                val historyEvent = entity?.toHistoryEvent()
                val isRecording = matchOrNull<Boolean>(historyEvent?.metadata?.get("recording")) ?: false

                val bounds = dao.getLocationBoundsByEventId(eventId)
                val first = dao.getFirstLocationByEventId(eventId)?.toLocationData()
                val last = dao.getLastLocationByEventId(eventId)?.toLocationData()

                val initialBoundingBox = if (bounds?.minLat != null && bounds.maxLat != null &&
                    bounds.minLon != null && bounds.maxLon != null
                ) {
                    val latSpan = (bounds.maxLat - bounds.minLat).coerceAtLeast(0.001)
                    val lonSpan = (bounds.maxLon - bounds.minLon).coerceAtLeast(0.001)
                    val paddingFactor = 0.2

                    BoundingBox(
                        (bounds.maxLat + latSpan * paddingFactor).coerceIn(-90.0, 90.0),
                        (bounds.maxLon + lonSpan * paddingFactor).coerceIn(-180.0, 180.0),
                        (bounds.minLat - latSpan * paddingFactor).coerceIn(-90.0, 90.0),
                        (bounds.minLon - lonSpan * paddingFactor).coerceIn(-180.0, 180.0)
                    )
                } else null

                _uiState.update {
                    it.copy(
                        event = historyEvent,
                        initialBounds = initialBoundingBox,
                        firstLocation = first,
                        lastLocation = last,
                        isRecording = isRecording,
                        isLoading = false
                    )
                }

                if (initialBoundingBox != null && _uiState.value.renderedLocations.isEmpty()) {
                    updateViewport(initialBoundingBox, 15.0)
                }
            }
        }
    }

    fun updateViewport(box: BoundingBox, zoomLevel: Double) {
        val roundedZoom = zoomLevel.roundToInt()
        val step = calculateStep(roundedZoom)

        val latPadding = (box.latitudeSpan * 0.2).coerceAtLeast(0.0001)
        val lonPadding = (box.longitudeSpan * 0.2).coerceAtLeast(0.0001)
        val minLat = (box.latSouth - latPadding).coerceIn(-90.0, 90.0)
        val maxLat = (box.latNorth + latPadding).coerceIn(-90.0, 90.0)
        val minLon = (box.lonWest - lonPadding).coerceIn(-180.0, 180.0)
        val maxLon = (box.lonEast + lonPadding).coerceIn(-180.0, 180.0)

        debounceJob?.cancel()
        debounceJob = viewModelScope.launch(ioDispatcher) {
            delay(200)

            val locations = dao.getRenderedLocations(
                id = eventId,
                minLat = minLat,
                maxLat = maxLat,
                minLon = minLon,
                maxLon = maxLon,
                step = step
            ).map { it.toLocationData() }

            lastQueriedBounds = box
            lastQueriedStep = step

            _uiState.update { it.copy(renderedLocations = locations) }
        }
    }

    fun toggleCutMode() {
        _uiState.update {
            val newCutMode = !it.cutMode
            it.copy(
                cutMode = newCutMode,
                cutStart = if (!newCutMode) null else it.cutStart,
                cutEnd = if (!newCutMode) null else it.cutEnd
            )
        }
    }

    fun setCutPoint(time: ZonedDateTime) {
        _uiState.update { current ->
            if (!current.cutMode) return@update current
            if (time == current.cutStart || time == current.cutEnd) return@update current

            when {
                current.cutStart == null -> current.copy(cutStart = time)
                current.cutEnd == null -> {
                    val (start, end) = if (time < current.cutStart) {
                        time to current.cutStart
                    } else {
                        current.cutStart to time
                    }
                    current.copy(cutStart = start, cutEnd = end)
                }
                else -> current
            }
        }
    }

    fun undoCutSelection() {
        _uiState.update { current ->
            when {
                current.cutEnd != null -> current.copy(cutEnd = null)
                current.cutStart != null -> current.copy(cutStart = null)
                else -> current
            }
        }
    }

    fun cutEvent(
        context: Context,
        onSuccess: (EventRange) -> Unit
    ) {
        val currentState = _uiState.value
        val eventData = currentState.event as? EventRange ?: return
        val cutStart = currentState.cutStart ?: return
        val cutEnd = currentState.cutEnd ?: return

        viewModelScope.launch(ioDispatcher) {
            val newEvent = EventRange(
                title = "Cut of ${eventData.title}",
                description = eventData.description +
                    if (eventData.description.isBlank()) ""
                    else "\nCut of ${eventData.title}",
                time = cutStart,
                favorite = eventData.favorite,
                tags = eventData.tags,
                end = cutEnd,
                locationDescriptions = eventData.locationDescriptions.filter {
                    it.key >= cutStart && it.key <= cutEnd
                }.toMutableMap(),
                metadata = eventData.metadata.toMutableMap(),
                versionNumber = eventData.versionNumber
            ).apply {
                metadata["original_event_id"] = eventData.id
                metadata["original_event_time"] = eventData.time.toOffsetDateTime().toString()
                metadata["original_event_end"] = eventData.end.toOffsetDateTime().toString()
                if (metadata["root_event_id"] == null) metadata["root_event_id"] = eventData.id
                if (metadata["root_event_time"] == null) {
                    metadata["root_event_time"] = eventData.time.toOffsetDateTime().toString()
                }
                if (metadata["root_event_end"] == null) {
                    metadata["root_event_end"] = eventData.end.toOffsetDateTime().toString()
                }
            }

            val startMillis = cutStart.toInstant().toEpochMilli()
            val endMillis = cutEnd.toInstant().toEpochMilli()
            val sliceEntities = dao.getLocationsBetweenTimestamps(eventData.id, startMillis, endMillis)
            val locationsData = sliceEntities.map { it.toLocationData().toLocationEntity(newEvent.id) }

            if (eventData.audio != null) {
                val audioFile = File(context.filesDir, "audio/${eventData.audio}")
                audioFile.parentFile?.let {
                    val id = generateEventId()
                    val targetPath = "${it.name}/$id.m4a"
                    val targetFile = File(it, "/$id.m4a")
                    Log.d("NanHistoryDebug", "AUDIO COPY | FROM: ${audioFile.absolutePath} | TO: ${targetFile.absolutePath}")
                    audioFile.copyTo(targetFile)
                    newEvent.audio = targetPath
                }
            }

            if (eventData.validateSignature(context = context)) {
                newEvent.generateSignature(context, true)
            }

            dao.insertEvent(newEvent.toEventEntity())
            dao.insertLocations(locationsData)

            _uiState.update {
                it.copy(cutMode = false, cutStart = null, cutEnd = null)
            }

            onSuccess(newEvent)
        }
    }

    companion object {
        fun calculateStep(zoomLevel: Int): Int {
            return (1 shl maxOf(0, 15 - zoomLevel)).coerceAtMost(2048)
        }
    }
}
