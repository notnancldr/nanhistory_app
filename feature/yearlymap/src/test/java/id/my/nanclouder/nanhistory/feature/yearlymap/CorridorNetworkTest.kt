package id.my.nanclouder.nanhistory.feature.yearlymap

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class CorridorNetworkTest {

    @Test
    fun buildNetwork_emptySegments_returnsEmptyNetwork() {
        val network = CorridorNetworkBuilder.buildNetwork(emptyList())
        assertTrue(network.nodes.isEmpty())
        assertTrue(network.edges.isEmpty())
        assertEquals(0, network.eventIds.size)
        assertEquals(1, network.maxCount)
        assertEquals(0.0, network.totalDistanceMeters, 0.001)

        val render = network.toCorridorRender(version = 1L)
        assertEquals(1L, render.version)
        assertEquals(CorridorRender.EMPTY_FEATURE_COLLECTION, render.geoJson)
    }

    @Test
    fun buildNetwork_deduplicatesNodesAndEvents() {
        val pA = MapPoint(-6.2000, 106.8166)
        val pB = MapPoint(-6.2010, 106.8170)
        val pC = MapPoint(-6.2020, 106.8180)
        val pInterm = MapPoint(-6.2005, 106.8168)

        val segment1 = CorridorSegment(
            start = pA,
            end = pB,
            eventIds = setOf("ev1", "ev2"),
            count = 2,
            intermediatePoints = listOf(pInterm)
        )
        val segment2 = CorridorSegment(
            start = pB,
            end = pC,
            eventIds = setOf("ev2", "ev3", "ev4"),
            count = 3
        )

        val network = CorridorNetworkBuilder.buildNetwork(listOf(segment1, segment2))

        // 4 distinct points: pA, pB, pC, pInterm
        assertEquals(4, network.nodes.size)
        // 4 distinct events: ev1, ev2, ev3, ev4
        assertEquals(4, network.eventIds.size)
        assertEquals(2, network.edges.size)
        assertEquals(3, network.maxCount)
        assertTrue(network.totalDistanceMeters > 0.0)

        // Bounding box checks
        assertEquals(-6.2000, network.bounds.latNorth, 0.0001)
        assertEquals(-6.2020, network.bounds.latSouth, 0.0001)
        assertEquals(106.8180, network.bounds.lonEast, 0.0001)
        assertEquals(106.8166, network.bounds.lonWest, 0.0001)
    }

    @Test
    fun binarySerialization_roundtrip_preservesAllData() {
        val p1 = MapPoint(-6.2, 106.8)
        val p2 = MapPoint(-6.3, 106.9)
        val p3 = MapPoint(-6.4, 107.0)

        val seg = CorridorSegment(
            start = p1,
            end = p3,
            eventIds = setOf("event_alpha", "event_beta"),
            count = 5,
            intermediatePoints = listOf(p2)
        )

        val original = CorridorNetworkBuilder.buildNetwork(listOf(seg))

        val baos = ByteArrayOutputStream()
        original.writeTo(baos)
        val bytes = baos.toByteArray()

        val bais = ByteArrayInputStream(bytes)
        val deserialized = CorridorNetwork.readFrom(bais)

        assertEquals(original.nodes.size, deserialized.nodes.size)
        for (i in original.nodes.indices) {
            assertEquals(original.nodes[i].lat, deserialized.nodes[i].lat, 0.000001)
            assertEquals(original.nodes[i].lon, deserialized.nodes[i].lon, 0.000001)
        }

        assertArrayEquals(original.eventIds, deserialized.eventIds)
        assertEquals(original.maxCount, deserialized.maxCount)
        assertEquals(original.totalDistanceMeters, deserialized.totalDistanceMeters, 0.01)
        assertEquals(original.bounds.latNorth, deserialized.bounds.latNorth, 0.00001)
        assertEquals(original.bounds.latSouth, deserialized.bounds.latSouth, 0.00001)

        assertEquals(original.edges.size, deserialized.edges.size)
        val origEdge = original.edges[0]
        val desEdge = deserialized.edges[0]
        assertEquals(origEdge.u, desEdge.u)
        assertEquals(origEdge.v, desEdge.v)
        assertArrayEquals(origEdge.intermediateNodeIndices, desEdge.intermediateNodeIndices)
        assertArrayEquals(origEdge.eventIndices, desEdge.eventIndices)
        assertEquals(origEdge.count, desEdge.count)
    }

    @Test
    fun toCorridorRender_producesValidRender() {
        val p1 = MapPoint(-6.2, 106.8)
        val p2 = MapPoint(-6.3, 106.9)
        val seg = CorridorSegment(
            start = p1,
            end = p2,
            eventIds = setOf("e100", "e200"),
            count = 2
        )

        val network = CorridorNetworkBuilder.buildNetwork(listOf(seg))
        val render = network.toCorridorRender(version = 42L)

        assertEquals(42L, render.version)
        assertTrue(render.geoJson.contains(""""id":"e0""""))
        assertTrue(render.geoJson.contains(""""t":"""))
        assertTrue(render.geoJson.contains("106.800000,-6.200000"))

        val events = render.getEventIdsForEdge("e0")
        assertEquals(2, events.size)
        assertTrue(events.contains("e100"))
        assertTrue(events.contains("e200"))
    }
}
