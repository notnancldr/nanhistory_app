package id.my.nanclouder.nanhistory.state

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.my.nanclouder.nanhistory.domain.model.CorridorSnapshot
import id.my.nanclouder.nanhistory.domain.model.EventRange
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.model.LocationBounds
import id.my.nanclouder.nanhistory.domain.model.TransportationType
import id.my.nanclouder.nanhistory.domain.repository.CorridorRepository
import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import id.my.nanclouder.nanhistory.domain.repository.LocationRepository
import id.my.nanclouder.nanhistory.ui.map.CorridorNetwork
import id.my.nanclouder.nanhistory.ui.map.CorridorNetworkBuilder
import id.my.nanclouder.nanhistory.ui.map.CorridorRender
import id.my.nanclouder.nanhistory.ui.map.CorridorSegment
import id.my.nanclouder.nanhistory.ui.map.MapPoint
import id.my.nanclouder.nanhistory.ui.map.YearlyMapCacheManager
import id.my.nanclouder.nanhistory.ui.map.YearlyMapMergeLogic
import id.my.nanclouder.nanhistory.utils.map.YearlyMapPerfTracer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.BoundingBox

/**
 * ViewModel managing whole-range corridor network generation, binary caching, and decoupled vector map serving.
 */
class YearlyMapViewModel(
    private val eventRepository: EventRepository,
    private val locationRepository: LocationRepository,
    private val corridorRepository: CorridorRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(YearlyMapUiState())
    val uiState: StateFlow<YearlyMapUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var renderVersion: Long = 0L

    init {
        loadRangeData(_uiState.value.timeRange)
    }

    /**
     * Refreshes the currently active range by clearing its persistent caches and re-aggregating.
     */
    fun refreshCurrentRange() {
        val rangeKey = getRangeKey(_uiState.value.timeRange)
        context?.let { YearlyMapCacheManager.clearRangeCache(it, rangeKey) }
        viewModelScope.launch(ioDispatcher) {
            corridorRepository?.deleteCorridorNetwork(rangeKey)
            loadRangeData(_uiState.value.timeRange)
        }
    }

    /**
     * Clears all cached corridor networks across disk and database, then recomputes the active range.
     */
    fun clearAllCachesAndRecompute() {
        context?.let { YearlyMapCacheManager.clearAllCorridorCaches(it) }
        viewModelScope.launch(ioDispatcher) {
            corridorRepository?.deleteAllCorridorNetworks()
            loadRangeData(_uiState.value.timeRange)
        }
    }

    /**
     * Updates the active time range selection and loads the range network.
     */
    fun setTimeRange(selection: TimeRangeSelection) {
        loadJob?.cancel()
        renderVersion++
        _uiState.update {
            it.copy(
                timeRange = selection,
                corridors = emptyList(),
                render = CorridorRender(version = renderVersion),
                selectedEdge = null,
                network = null,
                maxTripCount = 1
            )
        }
        loadRangeData(selection)
    }

    /**
     * Decoupled viewport notification from the map.
     */
    fun onViewportChanged(boundingBox: BoundingBox, zoomLevel: Double) {
        // Maintained for API compatibility; map handles vector panning on GPU
    }

    /**
     * Selects an edge by its id and updates event details for the bottom sheet.
     */
    fun onEdgeSelected(edgeId: String?) {
        if (edgeId == null) {
            dismissBottomSheet()
            return
        }
        viewModelScope.launch(ioDispatcher) {
            val eventIds = _uiState.value.render.getEventIdsForEdge(edgeId)
            val events = eventIds.mapNotNull { id ->
                eventRepository.getEventById(id)
            }
            _uiState.update {
                it.copy(
                    selectedEdge = edgeId,
                    selectedSegmentEvents = events,
                    isBottomSheetVisible = true
                )
            }
        }
    }

    /**
     * Selects corridor segment event IDs and opens the bottom sheet with event details.
     */
    fun selectSegment(eventIds: Set<String>) {
        viewModelScope.launch(ioDispatcher) {
            val events = eventIds.mapNotNull { id ->
                eventRepository.getEventById(id)
            }
            _uiState.update {
                it.copy(
                    selectedSegmentEvents = events,
                    isBottomSheetVisible = true
                )
            }
        }
    }

    /**
     * Dismisses the events bottom sheet.
     */
    fun dismissBottomSheet() {
        _uiState.update {
            it.copy(
                selectedEdge = null,
                isBottomSheetVisible = false,
                selectedSegmentEvents = emptyList()
            )
        }
    }

    private fun loadRangeData(selection: TimeRangeSelection) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true, networkComputeProgress = 0.05f) }
            val rangeKey = getRangeKey(selection)
            val (startStr, endStr) = getRangeDates(selection)

            // Fast path: load precomputed binary network snapshot (<50ms)
            var cachedNetwork = context?.let { YearlyMapCacheManager.loadNetworkBinary(it, rangeKey) }
            if (cachedNetwork == null) {
                val dbEntity = corridorRepository?.getCorridorNetwork(rangeKey, YearlyMapCacheManager.ALGO_VERSION)
                if (dbEntity != null) {
                    try {
                        cachedNetwork = CorridorNetwork.readFrom(ByteArrayInputStream(dbEntity.networkBlob))
                    } catch (_: Throwable) {
                        corridorRepository.deleteCorridorNetwork(rangeKey)
                    }
                }
            }
            if (cachedNetwork != null) {
                renderVersion++
                val cachedRender = cachedNetwork.toCorridorRender(renderVersion)
                val events = eventRepository.getEventsInDateRange(startStr, endStr)
                val stats = computeTransportStats(events)

                _uiState.update {
                    it.copy(
                        initialBounds = cachedNetwork.bounds,
                        render = cachedRender,
                        network = cachedNetwork,
                        maxTripCount = cachedNetwork.maxCount,
                        transportStats = stats,
                        isLoading = false,
                        networkComputeProgress = null
                    )
                }
                return@launch
            }

            // Fallback provisional render if present
            val provisionalRender = context?.let { YearlyMapCacheManager.loadRender(it, rangeKey) }
            if (provisionalRender != null) {
                _uiState.update { it.copy(render = provisionalRender) }
            }

            val bounds = locationRepository.getLocationBoundsInRange(startStr ?: "", endStr ?: "")
            val paddedBounds = bounds?.toPaddedBoundingBox()

            val events = eventRepository.getEventsInDateRange(startStr, endStr)
            val stats = computeTransportStats(events)

            _uiState.update {
                it.copy(
                    initialBounds = paddedBounds,
                    transportStats = stats,
                    networkComputeProgress = 0.25f
                )
            }

            if (paddedBounds == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        networkComputeProgress = null
                    )
                }
                return@launch
            }

            val tFetch0 = System.currentTimeMillis()
            val locations = locationRepository.getRenderedYearlyLocations(
                startDate = startStr,
                endDate = endStr,
                minLat = paddedBounds.latSouth,
                maxLat = paddedBounds.latNorth,
                minLon = paddedBounds.lonWest,
                maxLon = paddedBounds.lonEast,
                step = 1
            )
            val fetchMs = System.currentTimeMillis() - tFetch0
            _uiState.update { it.copy(networkComputeProgress = 0.50f) }

            val tracks = locations.groupBy { it.eventId ?: "" }.map { (eventId, locs) ->
                Pair(eventId, locs.map { MapPoint(it.location.latitude, it.location.longitude, it.time.toInstant().toEpochMilli(), it.accuracy) })
            }

            val currentJob = coroutineContext[Job]
            val tMerge0 = System.currentTimeMillis()
            val mergedCorridors = YearlyMapMergeLogic.mergeParallelCorridors(
                tracks = tracks,
                checkCancelled = { currentJob?.ensureActive() }
            )
            val mergeMs = System.currentTimeMillis() - tMerge0
            _uiState.update { it.copy(networkComputeProgress = 0.80f) }

            val network = withContext(computationDispatcher) {
                CorridorNetworkBuilder.buildNetwork(mergedCorridors)
            }
            context?.let { YearlyMapCacheManager.saveNetworkBinary(it, rangeKey, network) }

            try {
                val baos = ByteArrayOutputStream()
                network.writeTo(baos)
                corridorRepository?.insertCorridorNetwork(
                    CorridorSnapshot(
                        rangeKey = rangeKey,
                        startDate = startStr,
                        endDate = endStr,
                        version = YearlyMapCacheManager.ALGO_VERSION,
                        updatedAt = System.currentTimeMillis(),
                        boundsNorth = network.bounds.latNorth,
                        boundsSouth = network.bounds.latSouth,
                        boundsEast = network.bounds.lonEast,
                        boundsWest = network.bounds.lonWest,
                        maxCount = network.maxCount,
                        totalDistanceMeters = network.totalDistanceMeters,
                        networkBlob = baos.toByteArray()
                    )
                )
            } catch (_: Throwable) {
                // Non-fatal if DB insert fails
            }

            renderVersion++
            val tBuild0 = System.currentTimeMillis()
            val newRender = withContext(computationDispatcher) {
                network.toCorridorRender(renderVersion)
            }
            val buildMs = System.currentTimeMillis() - tBuild0
            context?.let { YearlyMapCacheManager.saveRender(it, rangeKey, newRender) }

            YearlyMapPerfTracer.recordMetrics(
                listOf(
                    YearlyMapPerfTracer.StageMetric("RoomFetchRange", fetchMs, "${locations.size} rows"),
                    YearlyMapPerfTracer.StageMetric("CorridorMergeRange", mergeMs, "${mergedCorridors.size} corridors"),
                    YearlyMapPerfTracer.StageMetric("NetworkBuildRange", buildMs, "${network.edges.size} edges")
                )
            )

            _uiState.update {
                it.copy(
                    initialBounds = paddedBounds,
                    corridors = mergedCorridors,
                    render = newRender,
                    network = network,
                    maxTripCount = network.maxCount,
                    isLoading = false,
                    networkComputeProgress = null
                )
            }
        }
    }

    private fun computeTransportStats(events: List<HistoryEvent>): Map<TransportationType, Float> {
        val counts = mutableMapOf<TransportationType, Long>()
        var total = 0L
        events.forEach { event ->
            val type = (event as? EventRange)?.transportationType ?: TransportationType.Unspecified
            if (type != TransportationType.Unspecified) {
                counts[type] = (counts[type] ?: 0L) + 1L
                total += 1L
            }
        }
        return if (total > 0L) {
            counts.mapValues { it.value.toFloat() / total }
        } else {
            emptyMap()
        }
    }

    private fun LocationBounds.toPaddedBoundingBox(): BoundingBox? {
        val minLatitude = minLat ?: return null
        val maxLatitude = maxLat ?: return null
        val minLongitude = minLon ?: return null
        val maxLongitude = maxLon ?: return null
        val latSpan = maxLatitude - minLatitude
        val lonSpan = maxLongitude - minLongitude
        val latMargin = if (latSpan > 0.0) latSpan * 0.2 else 0.01
        val lonMargin = if (lonSpan > 0.0) lonSpan * 0.2 else 0.01
        return BoundingBox(
            (maxLatitude + latMargin).coerceAtMost(85.0),
            (maxLongitude + lonMargin).coerceAtMost(180.0),
            (minLatitude - latMargin).coerceAtLeast(-85.0),
            (minLongitude - lonMargin).coerceAtLeast(-180.0)
        )
    }

    private fun getRangeKey(selection: TimeRangeSelection): String = when (selection) {
        is TimeRangeSelection.Year -> "year_${selection.year}"
        is TimeRangeSelection.AllTime -> "all_time"
        is TimeRangeSelection.Custom -> "custom_${selection.start}_${selection.end}"
    }

    private fun getRangeDates(selection: TimeRangeSelection): Pair<String?, String?> = when (selection) {
        is TimeRangeSelection.Year -> Pair("${selection.year}-01-01", "${selection.year}-12-31")
        is TimeRangeSelection.AllTime -> Pair(null, null)
        is TimeRangeSelection.Custom -> Pair(selection.start.toString(), selection.end.toString())
    }

    /**
     * Calculates sampling step for backward-compatible downsampling.
     */
    fun calculateStep(zoomLevel: Double): Int = when {
        zoomLevel >= 16.0 -> 1
        zoomLevel >= 14.0 -> 2
        zoomLevel >= 12.0 -> 5
        zoomLevel >= 10.0 -> 10
        zoomLevel >= 8.0 -> 25
        else -> 50
    }
}
