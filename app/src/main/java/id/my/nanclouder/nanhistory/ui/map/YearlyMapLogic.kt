package id.my.nanclouder.nanhistory.ui.map

import android.content.Context
import id.my.nanclouder.nanhistory.db.AppDao
import id.my.nanclouder.nanhistory.db.toHistoryEvent
import id.my.nanclouder.nanhistory.utils.history.TransportationType
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import id.my.nanclouder.nanhistory.utils.history.EventRange
import id.my.nanclouder.nanhistory.utils.toGeoPoint
import java.time.LocalDate
import org.osmdroid.util.GeoPoint
import kotlin.math.*
import kotlinx.coroutines.flow.first
import java.time.format.DateTimeFormatter
import android.util.Log

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.Flow

// Serializable version of GeoPoint for storage
data class SerializableGeoPoint(
    val lat: Double,
    val lon: Double
) {
    fun toGeoPoint() = GeoPoint(lat, lon)
}

fun GeoPoint.toSerializable() = SerializableGeoPoint(latitude, longitude)

data class Segment(val start: SerializableGeoPoint, val end: SerializableGeoPoint) {
    // Helper constructor for GeoPoints
    constructor(start: GeoPoint, end: GeoPoint) : this(start.toSerializable(), end.toSerializable())
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Segment) return false
        // Undirected equality
        return (start == other.start && end == other.end) || 
               (start == other.end && end == other.start)
    }
    
    override fun hashCode(): Int {
        // Order-independent hash code
        return start.hashCode() + end.hashCode()
    }
}

data class YearlyMapData(
    // Gson has trouble with complex map keys usually. 
    // We will use a List of SegmentData for serialization if needed, or enable complex map key serialization.
    // For simplicity in Logic, we keep Map. For Cache, we might map it.
    val segments: Map<Segment, List<String>>, 
    val transportStats: Map<TransportationType, Float> 
)

data class YearlyMapLoadingState(
    val data: YearlyMapData? = null,
    val progress: Float = 0f,
    val firstLocation: GeoPoint? = null,
    val isComplete: Boolean = false
)

class ClusterNode(
    val id: Int,
    var lat: Double,
    var lon: Double,
    var weight: Int = 1
) {
    fun merge(otherLat: Double, otherLon: Double) {
        lat = (lat * weight + otherLat) / (weight + 1)
        lon = (lon * weight + otherLon) / (weight + 1)
        weight++
    }
    fun toGeoPoint() = GeoPoint(lat, lon)
}


data class NodeSegment(
    val from: Int, 
    val to: Int
)

data class YearlyMapPersistentState(
    val nodes: MutableList<ClusterNode> = mutableListOf(),
    // Use String key "from,to" for easier JSON serialization of Map keys
    val segments: MutableMap<String, MutableList<String>> = mutableMapOf(), 
    val transportStats: MutableMap<TransportationType, Long> = mutableMapOf(), // Store durations (Long) not percentages
    var lastEventTime: Long = 0L
)

suspend fun processEventsToState(
    context: Context,
    newEvents: List<HistoryEvent>,
    state: YearlyMapPersistentState,
    precisionMeters: Int,
    onProgress: (Float) -> Unit = {}
): YearlyMapPersistentState {
    
    val metersPerDegree = 111132.0
    val clusterRadiusDeg = precisionMeters / metersPerDegree

    // Rebuild grid from existing nodes for fast lookup
    val grid = mutableMapOf<String, MutableList<ClusterNode>>()
    
    fun getGridKey(lat: Double, lon: Double): String =
        "${(lat / clusterRadiusDeg).toLong()},${(lon / clusterRadiusDeg).toLong()}"
        
    state.nodes.forEach { node ->
        grid.computeIfAbsent(getGridKey(node.lat, node.lon)) { mutableListOf() }.add(node)
    }
    
    var nextNodeId = if (state.nodes.isNotEmpty()) state.nodes.maxOf { it.id } + 1 else 0
    
    fun findNearestNode(lat: Double, lon: Double): ClusterNode? {
        val cLatIdx = (lat / clusterRadiusDeg).toLong()
        val cLonIdx = (lon / clusterRadiusDeg).toLong()
        var nearest: ClusterNode? = null
        var minDistSq = clusterRadiusDeg * clusterRadiusDeg
        
        for (i in -1..1) {
            for (j in -1..1) {
                val key = "${cLatIdx + i},${cLonIdx + j}"
                grid[key]?.forEach { node ->
                    val dy = node.lat - lat
                    val dx = (node.lon - lon) * cos(Math.toRadians(lat))
                    val distSq = dy*dy + dx*dx
                    if (distSq < minDistSq) {
                        minDistSq = distSq
                        nearest = node
                    }
                }
            }
        }
        return nearest
    }

    val totalEvents = newEvents.size
    var processedCount = 0

    newEvents.forEachIndexed { index, event ->
        // Update stats
        val type = if (event is EventRange) event.transportationType else TransportationType.Unspecified
        val duration = if (event is EventRange) {
             java.time.Duration.between(event.time, event.end).toMillis()
        } else {
             0L
        }
        if (duration > 0) {
            state.transportStats[type] = state.transportStats.getOrDefault(type, 0L) + duration
        }
        
        // Update Last Event Time
        val eventEndTime = if (event is EventRange) event.end.toInstant().toEpochMilli() else event.time.toInstant().toEpochMilli()
        if (eventEndTime > state.lastEventTime) {
            state.lastEventTime = eventEndTime
        }

        // Process Locations
        val locations = event.getLocations(context)
        if (locations.isNotEmpty()) {
            val rawPoints = locations.map { it.location.toGeoPoint() }
            
            // Resample
            val resampledPoints = resample(rawPoints, precisionMeters.toDouble())
            
            val eventNodeIds = mutableListOf<Int>()
            
            resampledPoints.forEach { p ->
                val existing = findNearestNode(p.latitude, p.longitude)
                if (existing != null) {
                    existing.merge(p.latitude, p.longitude)
                    eventNodeIds.add(existing.id)
                } else {
                    val newNode = ClusterNode(nextNodeId++, p.latitude, p.longitude)
                    state.nodes.add(newNode)
                    grid.computeIfAbsent(getGridKey(newNode.lat, newNode.lon)) { mutableListOf() }.add(newNode)
                    eventNodeIds.add(newNode.id)
                }
            }
            
            for (i in 0 until eventNodeIds.size - 1) {
                val fromId = eventNodeIds[i]
                val toId = eventNodeIds[i+1]
                if (fromId != toId) {
                    // Canonical key (min,max) to treat directions as same segment
                    val key = if (fromId < toId) "$fromId,$toId" else "$toId,$fromId"
                    state.segments.computeIfAbsent(key) { mutableListOf() }.add(event.id)
                }
            }
        }
        
        processedCount++
        if (totalEvents > 0) {
            onProgress(index.toFloat() / totalEvents)
        }
    }
    
    return state
}

fun stateToData(state: YearlyMapPersistentState): YearlyMapData {
    val nodeMap = state.nodes.associateBy { it.id }
    val segmentMap = mutableMapOf<Segment, List<String>>()
    
    state.segments.forEach { (key, ids) ->
        val parts = key.split(",")
        if (parts.size == 2) {
            val fromId = parts[0].toIntOrNull()
            val toId = parts[1].toIntOrNull()
            
            val nodeA = nodeMap[fromId]
            val nodeB = nodeMap[toId]
            
            if (nodeA != null && nodeB != null) {
                val segment = Segment(nodeA.toGeoPoint(), nodeB.toGeoPoint())
                // Merge if multiple node-segments map to same GeoPoint segment (unlikely with unique nodes, but possible if nodes overlap perfectly)
                // Actually Segment uses Value equality on GeoPoint.
                // We want to combine lists if segments are geometrically identical?
                // Yes, map helper does this.
                val existing = segmentMap[segment]
                if (existing != null) {
                    segmentMap[segment] = existing + ids
                } else {
                    segmentMap[segment] = ids
                }
            }
        }
    }
    
    val totalDuration = state.transportStats.values.sum()
    val transportStats = if (totalDuration > 0) {
        state.transportStats.mapValues { it.value.toFloat() / totalDuration }
    } else {
        emptyMap()
    }
    
    return YearlyMapData(segmentMap, transportStats)
}

fun loadYearlyMapData(
    context: Context,
    dao: AppDao,
    year: Int,
    precisionMeters: Int,
    simplificationCm: Int
): Flow<YearlyMapLoadingState> = flow {
    val startTime = System.currentTimeMillis()
    Log.d("YearlyMapDebug", "Starting loadYearlyMapData (Async) for year $year")

    val startOfYear = LocalDate.of(year, 1, 1)
    val endOfYear = LocalDate.of(year, 12, 31)
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    
    // 1. Try Load Cache
    var state = YearlyMapCacheManager.loadCache(context, year)
    
    // get first location for centering (placeholder, updated later)
    var firstLoc: GeoPoint? = null

    if (state != null) {
        Log.d("YearlyMapDebug", "Loaded cache with ${state.nodes.size} nodes")
        // Emit cached state immediately
        val cachedData = stateToData(state)
        // Try to find a first location from cache if possible? 
        // Or just wait for events? Use first node?
        if (state.nodes.isNotEmpty()) {
             firstLoc = state.nodes.first().toGeoPoint()
        }
        
        emit(YearlyMapLoadingState(
            data = cachedData, 
            progress = 0.5f, // Arbitrary "halfway" since we have data
            firstLocation = firstLoc,
            isComplete = false
        ))
    } else {
        state = YearlyMapPersistentState()
    }
    
    // 2. Fetch Events
    val eventsFlow = dao.getEventsInRange(startOfYear.format(formatter), endOfYear.format(formatter))
    val allEvents = eventsFlow.first().map { it.toHistoryEvent() }
    
    if (allEvents.isEmpty() && state!!.nodes.isEmpty()) {
        emit(YearlyMapLoadingState(isComplete = true, progress = 1f))
        return@flow
    }

    // Filter events that are newer than cache
    val newEvents = allEvents.filter { 
        val eventTime = if (it is EventRange) it.end.toInstant().toEpochMilli() else it.time.toInstant().toEpochMilli()
        eventTime > state!!.lastEventTime
    }
    
    if (firstLoc == null) {
         val firstEvent = allEvents.firstOrNull { it.locationPath != null }
         if (firstEvent != null) {
            val locs = firstEvent.getLocations(context)
            if (locs.isNotEmpty()) firstLoc = locs[0].location.toGeoPoint()
         }
    }

    if (newEvents.isNotEmpty()) {
        Log.d("YearlyMapDebug", "Processing ${newEvents.size} new events")
        emit(YearlyMapLoadingState(progress = if(state!!.nodes.isEmpty()) 0.05f else 0.5f, firstLocation = firstLoc)) 

        processEventsToState(context, newEvents, state!!, precisionMeters) { progress ->
            // Scale progress based on whether we started from cache or scratch
            val baseProgress = if(state!!.nodes.isEmpty()) 0.0f else 0.5f
            val range = 1.0f - baseProgress
            val currentProgress = baseProgress + (progress * range) * 0.9f 
            
            if (currentProgress * 100 % 10 < 1) { // Emit every ~10%
                 // Parsing state to data can be heavy
                 // emit(YearlyMapLoadingState(data = data, ...))
            }
        }
        
        // Save Cache
        YearlyMapCacheManager.saveCache(context, year, state!!)
    } else {
        Log.d("YearlyMapDebug", "No new events to process")
    }
    
    val finalData = stateToData(state!!)
    emit(YearlyMapLoadingState(
        data = finalData,
        progress = 1.0f,
        firstLocation = firstLoc,
        isComplete = true
    ))
    
    Log.d("YearlyMapDebug", "Finished async load in ${System.currentTimeMillis() - startTime}ms")
}


fun resample(points: List<GeoPoint>, intervalMeters: Double): List<GeoPoint> {
    if (points.size < 2) return points
    
    val result = mutableListOf<GeoPoint>()
    result.add(points[0])
    
    for (i in 0 until points.size - 1) {
        val p1 = points[i]
        val p2 = points[i+1]
        
        val dist = distanceMeters(p1, p2)
        if (dist > intervalMeters) {
            val numSegments = (dist / intervalMeters).toInt()
            for (j in 1..numSegments) {
                val fraction = j.toDouble() / (numSegments + 1)
                result.add(interpolate(p1, p2, fraction))
            }
        }
        result.add(p2)
    }
    return result
}

fun interpolate(p1: GeoPoint, p2: GeoPoint, fraction: Double): GeoPoint {
    val lat = p1.latitude + (p2.latitude - p1.latitude) * fraction
    val lon = p1.longitude + (p2.longitude - p1.longitude) * fraction
    return GeoPoint(lat, lon)
}

fun distanceMeters(p1: GeoPoint, p2: GeoPoint): Double {
    val metersPerDegree = 111132.0
    val dy = p2.latitude - p1.latitude
    val dx = (p2.longitude - p1.longitude) * cos(Math.toRadians((p1.latitude + p2.latitude) / 2))
    return sqrt(dy*dy + dx*dx) * metersPerDegree
}


// Ramer-Douglas-Peucker algorithm implementation
fun ramerDouglasPeucker(points: List<GeoPoint>, epsilon: Double): List<GeoPoint> {
    if (points.size < 3) return points

    var dmax = 0.0
    var index = 0
    val end = points.size - 1

    for (i in 1 until end) {
        val d = perpendicularDistance(points[i], points[0], points[end])
        if (d > dmax) {
            index = i
            dmax = d
        }
    }

    return if (dmax > epsilon) {
        val recResults1 = ramerDouglasPeucker(points.subList(0, index + 1), epsilon)
        val recResults2 = ramerDouglasPeucker(points.subList(index, end + 1), epsilon)

        // Build the result list
        recResults1.dropLast(1) + recResults2
    } else {
        listOf(points[0], points[end])
    }
}

fun perpendicularDistance(pt: GeoPoint, lineStart: GeoPoint, lineEnd: GeoPoint): Double {
    // Basic conversion to meters for distance check
    // Or simpler: just use degrees if epsilon is in degrees.
    // User requested settings in meters/cm.
    // Distance in meters logic:
    // This is computationally expensive if we use proper spherical distance.
    // Approximation: Equirectangular projection
    
    val num = abs((lineEnd.longitude - lineStart.longitude) * (lineStart.latitude - pt.latitude) - (lineStart.longitude - pt.longitude) * (lineEnd.latitude - lineStart.latitude))
    val den = sqrt((lineEnd.longitude - lineStart.longitude).pow(2) + (lineEnd.latitude - lineStart.latitude).pow(2))
    
    // conversion factor (approx degrees to meters)
    val metersPerDegree = 111132.0
    
    // Distance in degrees
    val dDeg = if (den == 0.0) 0.0 else num / den
    
    return dDeg * metersPerDegree
}
