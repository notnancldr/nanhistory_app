package id.my.nanclouder.nanhistory.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CorridorGeoJsonBuilderTest {

    @Test
    fun build_emptyList_returnsEmptyFeatureCollection() {
        val (geoJson, eventIdsByEdge) = CorridorGeoJsonBuilder.build(emptyList())
        assertEquals(CorridorRender.EMPTY_FEATURE_COLLECTION, geoJson)
        assertTrue(eventIdsByEdge.isEmpty())
    }

    @Test
    fun build_singleSegment_formatsPropertiesAndRoundsCoordinatesToSixDecimals() {
        val segments = listOf(
            CorridorSegment(
                start = MapPoint(lat = -6.200000123, lon = 106.800000987),
                end = MapPoint(lat = -6.201234567, lon = 106.801234567),
                eventIds = setOf("event-1", "event-2")
            )
        )

        val (geoJson, eventIdsByEdge) = CorridorGeoJsonBuilder.build(segments)

        // Supports both stable short id "e0" and legacy "edge_0"
        assertEquals(setOf("event-1", "event-2"), eventIdsByEdge["e0"])
        assertEquals(setOf("event-1", "event-2"), eventIdsByEdge["edge_0"])

        assertTrue(geoJson.startsWith("""{"type":"FeatureCollection","features":["""))
        assertTrue(geoJson.endsWith("]}"))
        assertTrue(geoJson.contains(""""id":"e0""""))
        // Dropped redundant count property, formatted t to 3 decimals
        assertFalse(geoJson.contains(""""count":"""))
        assertTrue(geoJson.contains(""""t":"""))
        // Coordinates must be formatted to 6 decimal places
        assertTrue(geoJson.contains("[106.800001,-6.200000]"))
        assertTrue(geoJson.contains("[106.801235,-6.201235]"))
        // Must NEVER contain eventIds inside the GeoJSON string
        assertFalse(geoJson.contains("event-1"))
        assertFalse(geoJson.contains("event-2"))
    }

    @Test
    fun build_multipleSegments_calculatesProgressivelyScaledTValues() {
        val segments = listOf(
            CorridorSegment(
                start = MapPoint(-6.2, 106.8),
                end = MapPoint(-6.21, 106.81),
                eventIds = setOf("e1") // count 1 -> t = 0.000
            ),
            CorridorSegment(
                start = MapPoint(-6.21, 106.81),
                end = MapPoint(-6.22, 106.82),
                eventIds = setOf("e1", "e2", "e3")
            ),
            CorridorSegment(
                start = MapPoint(-6.22, 106.82),
                end = MapPoint(-6.23, 106.83),
                eventIds = setOf("e1", "e2", "e3", "e4", "e5") // count 5 == max -> t = 1.000
            )
        )

        val (geoJson, eventIdsByEdge) = CorridorGeoJsonBuilder.build(segments)

        assertEquals(setOf("e1"), eventIdsByEdge["e0"])
        assertEquals(setOf("e1", "e2", "e3"), eventIdsByEdge["e1"])
        assertEquals(setOf("e1", "e2", "e3", "e4", "e5"), eventIdsByEdge["e2"])

        assertTrue(geoJson.contains(""""id":"e0","properties":{"id":"e0","t":0.000}"""))
        assertTrue(geoJson.contains(""""id":"e2","properties":{"id":"e2","t":1.000}"""))
    }

    @Test
    fun buildRender_serializesContractedPolylineCoordinates() {
        val segmentWithIntermediates = CorridorSegment(
            start = MapPoint(-6.20, 106.80),
            end = MapPoint(-6.22, 106.82),
            eventIds = setOf("trip-1"),
            intermediatePoints = listOf(
                MapPoint(-6.21, 106.81)
            )
        )

        val render = CorridorGeoJsonBuilder.buildRender(1L, listOf(segmentWithIntermediates))
        assertEquals(1, render.eventIdPool.size)
        assertEquals("trip-1", render.eventIdPool[0])
        assertEquals(1, render.edgeEventIndices.size)

        // GeoJSON should contain all 3 points in order
        assertTrue(render.geoJson.contains("[106.800000,-6.200000],[106.810000,-6.210000],[106.820000,-6.220000]"))
    }
}
