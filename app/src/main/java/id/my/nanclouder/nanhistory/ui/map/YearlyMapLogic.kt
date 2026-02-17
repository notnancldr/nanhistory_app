package id.my.nanclouder.nanhistory.ui.map

import android.content.Context
import id.my.nanclouder.nanhistory.db.AppDao
import id.my.nanclouder.nanhistory.db.toHistoryEvent
import id.my.nanclouder.nanhistory.utils.history.TransportationType
import id.my.nanclouder.nanhistory.utils.history.LocationData
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import id.my.nanclouder.nanhistory.utils.history.EventRange
import id.my.nanclouder.nanhistory.utils.toGeoPoint
import java.time.ZonedDateTime
import java.time.LocalDate
import org.osmdroid.util.GeoPoint
import kotlin.math.*
import kotlinx.coroutines.flow.first
import java.time.format.DateTimeFormatter
import android.util.Log

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield

data class Segment(val start: GeoPoint, val end: GeoPoint) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Segment) return false
        // Order shouldn't matter for valid path, but for directed path it does.
        // For "frequency", A->B and B->A might be considered same or different.
        // If we want to capture "trips", direction usually matters.
        // But for visual stacking, maybe we want canonical direction?
        // Let's stick to directed for now.
        return start == other.start && end == other.end
    }
    
    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + end.hashCode()
        return result
    }
}

data class YearlyMapData(
    val segments: Map<Segment, List<String>>, // Segment -> List of Event IDs
    val transportStats: Map<TransportationType, Float> // Type -> Percentage (0-1)
)

data class YearlyMapLoadingState(
    val data: YearlyMapData? = null,
    val progress: Float = 0f,
    val firstLocation: GeoPoint? = null,
    val isComplete: Boolean = false
)

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
    
    // 1. Fetch Events
    val eventsFlow = dao.getEventsInRange(startOfYear.format(formatter), endOfYear.format(formatter))
    val events = eventsFlow.first().map { it.toHistoryEvent() }
    
    if (events.isEmpty()) {
        emit(YearlyMapLoadingState(isComplete = true, progress = 1f))
        return@flow
    }

    emit(YearlyMapLoadingState(progress = 0.05f)) // 5% for fetching

    // 2. Process Stats
    val transportDuration = mutableMapOf<TransportationType, Long>()
    var totalDuration = 0L
    
    events.forEach { event ->
        val type = if (event is EventRange) event.transportationType else TransportationType.Unspecified
        val duration = if (event is EventRange) {
             java.time.Duration.between(event.time, event.end).toMillis()
        } else {
             0L
        }
        
        if (duration > 0) {
            transportDuration[type] = transportDuration.getOrDefault(type, 0L) + duration
            totalDuration += duration
        }
    }
    
    val transportStats = if (totalDuration > 0) {
        transportDuration.mapValues { it.value.toFloat() / totalDuration }
    } else {
        emptyMap()
    }
    
    emit(YearlyMapLoadingState(progress = 0.10f)) // 10% for stats

    // 3. Process Map Segments with Dynamic Clustering
    
    // Clustering structures
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

    val metersPerDegree = 111132.0
    val clusterRadiusDeg = precisionMeters / metersPerDegree
    val cellSize = clusterRadiusDeg 
    
    val grid = mutableMapOf<String, MutableList<ClusterNode>>()
    val nodes = mutableListOf<ClusterNode>()
    var nextNodeId = 0

    fun getGridKey(lat: Double, lon: Double): String =
        "${(lat / cellSize).toLong()},${(lon / cellSize).toLong()}"
    
    fun findNearestNode(lat: Double, lon: Double): ClusterNode? {
        val cLatIdx = (lat / cellSize).toLong()
        val cLonIdx = (lon / cellSize).toLong()
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
    
    data class NodeSegment(val from: Int, val to: Int)
    val tempSegmentMap = mutableMapOf<NodeSegment, MutableList<String>>()
    
    var processedEvents = 0
    var totalPointsProcessed = 0
    var firstLoc: GeoPoint? = null
    
    val totalEvents = events.size
    
    events.forEachIndexed { index, event ->
        val locations = event.getLocations(context)
        if (locations.isNotEmpty()) {
            processedEvents++
            val rawPoints = locations.map { it.location.toGeoPoint() }
            
            if (firstLoc == null && rawPoints.isNotEmpty()) {
                firstLoc = rawPoints[0]
            }
            
            val simplifiedPoints = ramerDouglasPeucker(rawPoints, simplificationCm / 100.0)
            totalPointsProcessed += simplifiedPoints.size
            
            val eventNodeIds = mutableListOf<Int>()
            
            simplifiedPoints.forEach { p ->
                val existing = findNearestNode(p.latitude, p.longitude)
                if (existing != null) {
                    existing.merge(p.latitude, p.longitude)
                    eventNodeIds.add(existing.id)
                } else {
                    val newNode = ClusterNode(nextNodeId++, p.latitude, p.longitude)
                    nodes.add(newNode)
                    grid.computeIfAbsent(getGridKey(newNode.lat, newNode.lon)) { mutableListOf() }.add(newNode)
                    eventNodeIds.add(newNode.id)
                }
            }
            
            for (i in 0 until eventNodeIds.size - 1) {
                val fromId = eventNodeIds[i]
                val toId = eventNodeIds[i+1]
                if (fromId != toId) {
                    tempSegmentMap.computeIfAbsent(NodeSegment(fromId, toId)) { mutableListOf() }.add(event.id)
                }
            }
        }
        
        // Emit progress every 10 events or so
        if (index % 10 == 0 || index == totalEvents - 1) {
            // Reconstruct Segment Map
            // Note: This reconstruction might become expensive as nodes grow. 
            // Optimally we'd only do this less frequently or optimize the structure.
            // For now, let's try every 20-50 events if slow.
            // User asked for "data already visualized while still processing".
            
            val currentSegmentMap = mutableMapOf<Segment, MutableList<String>>()
            tempSegmentMap.forEach { (nodeSeg, ids) ->
                 val nodeA = nodes[nodeSeg.from]
                 val nodeB = nodes[nodeSeg.to]
                 val segment = Segment(nodeA.toGeoPoint(), nodeB.toGeoPoint())
                 currentSegmentMap.computeIfAbsent(segment) { mutableListOf() }.addAll(ids)
            }
            
            val currentData = YearlyMapData(currentSegmentMap, transportStats)
            val currentProgress = 0.10f + (0.90f * (index + 1) / totalEvents)
            
            emit(YearlyMapLoadingState(
                data = currentData, 
                progress = currentProgress,
                firstLocation = firstLoc,
                isComplete = false
            ))
            yield() // Allow cancellation/UI updates
        }
    }

    // Final emission
    val finalSegmentMap = mutableMapOf<Segment, MutableList<String>>()
    tempSegmentMap.forEach { (nodeSeg, ids) ->
         val nodeA = nodes[nodeSeg.from]
         val nodeB = nodes[nodeSeg.to]
         val segment = Segment(nodeA.toGeoPoint(), nodeB.toGeoPoint())
         finalSegmentMap.computeIfAbsent(segment) { mutableListOf() }.addAll(ids)
    }

    emit(YearlyMapLoadingState(
        data = YearlyMapData(finalSegmentMap, transportStats),
        progress = 1.0f,
        firstLocation = firstLoc,
        isComplete = true
    ))
    
    Log.d("YearlyMapDebug", "Finished async load in ${System.currentTimeMillis() - startTime}ms")
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
