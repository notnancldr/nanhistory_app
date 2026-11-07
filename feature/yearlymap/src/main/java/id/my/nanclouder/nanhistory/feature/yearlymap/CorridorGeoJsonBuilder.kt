package id.my.nanclouder.nanhistory.feature.yearlymap

import java.util.Locale

/**
 * Builds compact GeoJSON FeatureCollection strings and indexed event lookups for corridor rendering.
 */
object CorridorGeoJsonBuilder {

    /**
     * Builds an immutable, memory-efficient [CorridorRender] containing:
     * - A compact GeoJSON string with polyline coordinates, short edge IDs, 3-decimal t values, and no redundant count.
     * - A deduplicated [CorridorRender.eventIdPool] and compact index arrays [CorridorRender.edgeEventIndices].
     */
    fun buildRender(version: Long, corridors: List<CorridorSegment>): CorridorRender {
        if (corridors.isEmpty()) {
            return CorridorRender(
                version = version,
                geoJson = CorridorRender.EMPTY_FEATURE_COLLECTION,
                eventIdPool = emptyArray(),
                edgeEventIndices = emptyArray()
            )
        }

        val eventIdMap = LinkedHashMap<String, Int>()
        for (c in corridors) {
            for (id in c.eventIds) {
                if (!eventIdMap.containsKey(id)) {
                    eventIdMap[id] = eventIdMap.size
                }
            }
        }
        val eventIdPool = eventIdMap.keys.toTypedArray()

        val edgeEventIndices = Array(corridors.size) { i ->
            val ids = corridors[i].eventIds
            val arr = IntArray(ids.size)
            var k = 0
            for (id in ids) {
                arr[k++] = eventIdMap[id] ?: 0
            }
            arr
        }

        val maxFreq = corridors.maxOfOrNull { it.eventIds.size } ?: 1
        val sb = StringBuilder(corridors.size * 180 + 64)
        sb.append("""{"type":"FeatureCollection","features":[""")

        for (i in corridors.indices) {
            val corridor = corridors[i]
            val edgeId = "e$i"
            val count = corridor.eventIds.size
            val t = CorridorColorScale.calculateT(count, maxFreq)

            if (i > 0) sb.append(',')

            val tFormatted = String.format(Locale.US, "%.3f", t)

            sb.append("""{"type":"Feature","id":"""")
                .append(edgeId)
                .append("""","properties":{"id":"""")
                .append(edgeId)
                .append("""","t":""")
                .append(tFormatted)
                .append("""},"geometry":{"type":"LineString","coordinates":[""")

            val points = corridor.allPoints
            for (pIdx in points.indices) {
                if (pIdx > 0) sb.append(',')
                val pt = points[pIdx]
                sb.append('[')
                    .append(formatCoord(pt.lon))
                    .append(',')
                    .append(formatCoord(pt.lat))
                    .append(']')
            }

            sb.append("]}}")
        }

        sb.append("]}")

        return CorridorRender(
            version = version,
            geoJson = sb.toString(),
            eventIdPool = eventIdPool,
            edgeEventIndices = edgeEventIndices
        )
    }

    /**
     * Converts a list of [CorridorSegment]s into a GeoJSON FeatureCollection and an edge-to-events map.
     */
    fun build(corridors: List<CorridorSegment>): Pair<String, Map<String, Set<String>>> {
        val render = buildRender(version = 0L, corridors = corridors)
        return Pair(render.geoJson, render.eventIdsByEdge)
    }

    private fun formatCoord(value: Double): String {
        return String.format(Locale.US, "%.6f", value)
    }
}
