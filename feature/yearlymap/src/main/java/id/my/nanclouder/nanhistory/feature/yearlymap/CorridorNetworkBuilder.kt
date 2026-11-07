package id.my.nanclouder.nanhistory.feature.yearlymap

import org.osmdroid.util.BoundingBox

/**
 * Builds an immutable, topologically indexed [CorridorNetwork] from a collection of merged [CorridorSegment]s.
 * Precomputes node indices, deduplicated event ID pool, bounding box, and total network distance.
 */
object CorridorNetworkBuilder {

    /**
     * Converts a list of [CorridorSegment] into a unified [CorridorNetwork].
     * Computes global bounding box, total distance in meters, and peak trip count across all edges.
     */
    fun buildNetwork(segments: List<CorridorSegment>): CorridorNetwork {
        if (segments.isEmpty()) {
            return CorridorNetwork(
                nodes = emptyList(),
                edges = emptyList(),
                eventIds = emptyArray(),
                maxCount = 1,
                bounds = BoundingBox(0.0, 0.0, 0.0, 0.0),
                totalDistanceMeters = 0.0
            )
        }

        val nodeIndexMap = LinkedHashMap<MapPoint, Int>()
        val nodes = ArrayList<MapPoint>()
        fun getOrAddNode(point: MapPoint): Int {
            return nodeIndexMap.getOrPut(point) {
                val idx = nodes.size
                nodes.add(point)
                idx
            }
        }

        val eventIdMap = LinkedHashMap<String, Int>()
        val eventIdList = ArrayList<String>()
        fun getOrAddEventId(id: String): Int {
            return eventIdMap.getOrPut(id) {
                val idx = eventIdList.size
                eventIdList.add(id)
                idx
            }
        }

        var minLat = 90.0
        var maxLat = -90.0
        var minLon = 180.0
        var maxLon = -180.0
        var totalDistance = 0.0
        var maxCount = 1

        val edges = ArrayList<NetworkEdge>(segments.size)

        for (segment in segments) {
            val u = getOrAddNode(segment.start)
            val v = getOrAddNode(segment.end)

            val intermediateIndices = IntArray(segment.intermediatePoints.size) { i ->
                getOrAddNode(segment.intermediatePoints[i])
            }

            val eventIndices = segment.eventIds
                .map { getOrAddEventId(it) }
                .sorted()
                .toIntArray()

            val count = maxOf(segment.count, eventIndices.size)
            if (count > maxCount) {
                maxCount = count
            }

            // Update bounds and accumulate distance across polyline points
            var lastPt = segment.start
            minLat = minOf(minLat, lastPt.lat)
            maxLat = maxOf(maxLat, lastPt.lat)
            minLon = minOf(minLon, lastPt.lon)
            maxLon = maxOf(maxLon, lastPt.lon)

            for (interm in segment.intermediatePoints) {
                totalDistance += YearlyMapMergeLogic.distanceMeters(lastPt, interm)
                minLat = minOf(minLat, interm.lat)
                maxLat = maxOf(maxLat, interm.lat)
                minLon = minOf(minLon, interm.lon)
                maxLon = maxOf(maxLon, interm.lon)
                lastPt = interm
            }
            totalDistance += YearlyMapMergeLogic.distanceMeters(lastPt, segment.end)
            minLat = minOf(minLat, segment.end.lat)
            maxLat = maxOf(maxLat, segment.end.lat)
            minLon = minOf(minLon, segment.end.lon)
            maxLon = maxOf(maxLon, segment.end.lon)

            edges.add(
                NetworkEdge(
                    u = u,
                    v = v,
                    intermediateNodeIndices = intermediateIndices,
                    eventIndices = eventIndices,
                    count = count
                )
            )
        }

        val bounds = if (nodes.isEmpty()) {
            BoundingBox(0.0, 0.0, 0.0, 0.0)
        } else {
            BoundingBox(maxLat, maxLon, minLat, minLon)
        }

        return CorridorNetwork(
            nodes = nodes,
            edges = edges,
            eventIds = eventIdList.toTypedArray(),
            maxCount = maxCount,
            bounds = bounds,
            totalDistanceMeters = totalDistance
        )
    }
}
