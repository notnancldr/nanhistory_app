package id.my.nanclouder.nanhistory.ui.map

import id.my.nanclouder.nanhistory.utils.map.YearlyMapPerfTracer
import kotlin.math.cos
import kotlin.math.hypot

/**
 * Pure 2D coordinate point with optional timestamp and accuracy for map geometry and corridor clustering.
 */
data class MapPoint(
    val lat: Double,
    val lon: Double,
    val timestamp: Long? = null,
    val accuracy: Float? = null
) : Comparable<MapPoint> {
    override fun compareTo(other: MapPoint): Int {
        val latComp = lat.compareTo(other.lat)
        return if (latComp != 0) latComp else lon.compareTo(other.lon)
    }
}

/**
 * Represents a corridor polyline between [start] and [end] with optional [intermediatePoints].
 */
data class CorridorSegment(
    val start: MapPoint,
    val end: MapPoint,
    val eventIds: Set<String>,
    internal var count: Int = eventIds.size,
    val intermediatePoints: List<MapPoint> = emptyList()
) {
    val allPoints: List<MapPoint>
        get() = if (intermediatePoints.isEmpty()) {
            listOf(start, end)
        } else {
            val list = ArrayList<MapPoint>(intermediatePoints.size + 2)
            list.add(start)
            list.addAll(intermediatePoints)
            list.add(end)
            list
        }
}

/**
 * Geometric and parallel corridor merging logic using corridor vertex network snapping,
 * GPS gap detection, nearest-node snapping, self-snapping prevention, and degree-2 node contraction.
 */
object YearlyMapMergeLogic {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates equirectangular approximate distance in meters between two coordinates.
     */
    fun distanceMeters(p1: MapPoint, p2: MapPoint): Double {
        val latMid = Math.toRadians((p1.lat + p2.lat) / 2.0)
        val dx = Math.toRadians(p2.lon - p1.lon) * EARTH_RADIUS_METERS * cos(latMid)
        val dy = Math.toRadians(p2.lat - p1.lat) * EARTH_RADIUS_METERS
        return hypot(dx, dy)
    }

    /**
     * Computes the perpendicular distance from point [p] to the line segment between [a] and [b].
     */
    fun perpendicularDistance(p: MapPoint, a: MapPoint, b: MapPoint): Double {
        val latMid = Math.toRadians((a.lat + b.lat + p.lat) / 3.0)
        val ax = Math.toRadians(a.lon) * EARTH_RADIUS_METERS * cos(latMid)
        val ay = Math.toRadians(a.lat) * EARTH_RADIUS_METERS
        val bx = Math.toRadians(b.lon) * EARTH_RADIUS_METERS * cos(latMid)
        val by = Math.toRadians(b.lat) * EARTH_RADIUS_METERS
        val px = Math.toRadians(p.lon) * EARTH_RADIUS_METERS * cos(latMid)
        val py = Math.toRadians(p.lat) * EARTH_RADIUS_METERS

        val dx = bx - ax
        val dy = by - ay
        val lengthSq = dx * dx + dy * dy
        if (lengthSq <= 1e-6) {
            return hypot(px - ax, py - ay)
        }

        val t = ((px - ax) * dx + (py - ay) * dy) / lengthSq
        val clampedT = t.coerceIn(0.0, 1.0)
        val projX = ax + clampedT * dx
        val projY = ay + clampedT * dy
        return hypot(px - projX, py - projY)
    }

    /**
     * Simplifies a polyline using the Ramer-Douglas-Peucker algorithm.
     */
    fun ramerDouglasPeucker(points: List<MapPoint>, epsilonMeters: Double): List<MapPoint> {
        if (points.size <= 2) return points

        var maxDistance = 0.0
        var maxIndex = 0
        val first = points.first()
        val last = points.last()

        for (i in 1 until points.size - 1) {
            val dist = perpendicularDistance(points[i], first, last)
            if (dist > maxDistance) {
                maxDistance = dist
                maxIndex = i
            }
        }

        return if (maxDistance > epsilonMeters) {
            val left = ramerDouglasPeucker(points.subList(0, maxIndex + 1), epsilonMeters)
            val right = ramerDouglasPeucker(points.subList(maxIndex, points.size), epsilonMeters)
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }

    /**
     * Resamples polyline points into fixed intervals of approximately [intervalMeters].
     */
    fun resample(points: List<MapPoint>, intervalMeters: Double): List<MapPoint> {
        if (points.size < 2 || intervalMeters <= 0.0) return points
        val result = mutableListOf<MapPoint>()
        result.add(points[0])

        var currentPoint = points[0]
        var i = 1
        while (i < points.size) {
            val nextPoint = points[i]
            val segDist = distanceMeters(currentPoint, nextPoint)
            if (segDist >= intervalMeters) {
                val ratio = intervalMeters / segDist
                val interpLat = currentPoint.lat + (nextPoint.lat - currentPoint.lat) * ratio
                val interpLon = currentPoint.lon + (nextPoint.lon - currentPoint.lon) * ratio
                val interpPoint = MapPoint(interpLat, interpLon)
                result.add(interpPoint)
                currentPoint = interpPoint
            } else {
                currentPoint = nextPoint
                i++
            }
        }
        if (result.last() != points.last()) {
            result.add(points.last())
        }
        return result
    }

    /**
     * Splits a raw track into contiguous subtracks at GPS gaps:
     * - Distance displacement >= 5000m (5km).
     * - Time gap > 120s with displacement > 200m.
     * - Implied speed > 70 m/s (~250 km/h).
     */
    fun splitAtGpsGaps(points: List<MapPoint>): List<List<MapPoint>> {
        if (points.size < 2) return if (points.size == 1) listOf(points) else emptyList()
        val subtracks = mutableListOf<List<MapPoint>>()
        var currentSubtrack = mutableListOf<MapPoint>()
        currentSubtrack.add(points[0])

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val dist = distanceMeters(p1, p2)

            var isGap = false
            if (dist >= 5000.0) {
                isGap = true
            } else if (p1.timestamp != null && p2.timestamp != null) {
                val dtSec = (p2.timestamp - p1.timestamp) / 1000.0
                if (dtSec > 0.0) {
                    if (dtSec > 120.0 && dist > 200.0) {
                        isGap = true
                    } else if (dist / dtSec > 70.0) {
                        isGap = true
                    }
                }
            }

            if (isGap) {
                if (currentSubtrack.size >= 2) {
                    subtracks.add(currentSubtrack)
                }
                currentSubtrack = mutableListOf(p2)
            } else {
                currentSubtrack.add(p2)
            }
        }
        if (currentSubtrack.size >= 2) {
            subtracks.add(currentSubtrack)
        }
        return subtracks
    }

    /**
     * Contracts sequences of degree-2 vertices that share the identical event IDs set into single polyline edges.
     */
    fun contractDegree2Nodes(
        rawEdges: Map<Pair<MapPoint, MapPoint>, Set<String>>
    ): List<CorridorSegment> = YearlyMapContraction.contractDegree2Nodes(rawEdges)

    /**
     * Merges tracks that run along parallel corridors using corridor vertex network snapping,
     * collapsing overlapping tracks into canonical centerline edges.
     */
    fun mergeParallelCorridors(
        tracks: List<Pair<String, List<MapPoint>>>,
        corridorToleranceMeters: Double = 25.0,
        rdpEpsilonMeters: Double = 5.0,
        resampleIntervalMeters: Double = 20.0,
        checkCancelled: () -> Unit = {}
    ): List<CorridorSegment> {
        val existingVertices = mutableListOf<MapPoint>()
        val spatialIndex = mutableMapOf<Pair<Int, Int>, MutableList<Int>>()
        val bucketSizeDeg = 0.001 // ~100m grid cell

        fun bucketKey(p: MapPoint): Pair<Int, Int> =
            Pair((p.lat / bucketSizeDeg).toInt(), (p.lon / bucketSizeDeg).toInt())

        val subtrackVisitedDist = mutableMapOf<MapPoint, Double>()

        fun findOrCreateVertex(p: MapPoint, currentPathDist: Double): MapPoint {
            val key = bucketKey(p)
            var closestVertex: MapPoint? = null
            var minDistance = corridorToleranceMeters

            for (dx in -1..1) {
                for (dy in -1..1) {
                    val neighborKey = Pair(key.first + dx, key.second + dy)
                    val indices = spatialIndex[neighborKey] ?: continue
                    for (idx in indices) {
                        val candidate = existingVertices[idx]
                        val dist = distanceMeters(p, candidate)
                        if (dist <= minDistance) {
                            val lastDist = subtrackVisitedDist[candidate]
                            if (lastDist != null && (currentPathDist - lastDist) < 2.0 * corridorToleranceMeters) {
                                continue
                            }
                            minDistance = dist
                            closestVertex = candidate
                        }
                    }
                }
            }

            val chosen = if (closestVertex != null) {
                closestVertex
            } else {
                val newIndex = existingVertices.size
                existingVertices.add(p)
                spatialIndex.getOrPut(key) { mutableListOf() }.add(newIndex)
                p
            }

            subtrackVisitedDist[chosen] = currentPathDist
            return chosen
        }

        val rawEdgeMap = LinkedHashMap<Pair<MapPoint, MapPoint>, MutableSet<String>>()

        // Deterministic processing order: timestamp ascending, then eventId
        val sortedTracks = tracks.sortedWith(
            compareBy(
                { it.second.firstOrNull()?.timestamp ?: 0L },
                { it.first }
            )
        )

        YearlyMapPerfTracer.beginSection("YearlyMap.MergePipeline")
        sortedTracks.forEach { (eventId, rawPoints) ->
            checkCancelled()
            if (rawPoints.size < 2) return@forEach

            // Filter poor accuracy points (>50m) before simplification
            val accuracyFiltered = rawPoints.filter { it.accuracy == null || it.accuracy <= 50.0f }
            if (accuracyFiltered.size < 2) return@forEach

            // Split tracks across GPS gaps
            val subtracks = splitAtGpsGaps(accuracyFiltered)

            for (subtrack in subtracks) {
                checkCancelled()
                if (subtrack.size < 2) continue

                YearlyMapPerfTracer.beginSection("YearlyMap.RDP")
                val simplified = ramerDouglasPeucker(subtrack, rdpEpsilonMeters)
                YearlyMapPerfTracer.endSection()

                YearlyMapPerfTracer.beginSection("YearlyMap.Resample")
                val resampled = resample(simplified, resampleIntervalMeters)
                YearlyMapPerfTracer.endSection()

                YearlyMapPerfTracer.beginSection("YearlyMap.Snap")
                subtrackVisitedDist.clear()
                var currentPathDist = 0.0
                var lastNode: MapPoint? = null

                for (idx in resampled.indices) {
                    val pt = resampled[idx]
                    if (idx > 0) {
                        currentPathDist += distanceMeters(resampled[idx - 1], pt)
                    }
                    val node = findOrCreateVertex(pt, currentPathDist)
                    if (lastNode != null && lastNode != node) {
                        val canonicalEdge = if (lastNode <= node) {
                            Pair(lastNode, node)
                        } else {
                            Pair(node, lastNode)
                        }
                        val eventIds = rawEdgeMap.getOrPut(canonicalEdge) { LinkedHashSet() }
                        eventIds.add(eventId)
                    }
                    lastNode = node
                }
                YearlyMapPerfTracer.endSection()
            }
        }
        YearlyMapPerfTracer.endSection()

        YearlyMapPerfTracer.beginSection("YearlyMap.ContractDegree2")
        val contracted = contractDegree2Nodes(rawEdgeMap)
        YearlyMapPerfTracer.endSection()

        return contracted
    }
}
