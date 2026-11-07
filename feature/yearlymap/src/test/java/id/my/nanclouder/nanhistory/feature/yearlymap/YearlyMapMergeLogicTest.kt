package id.my.nanclouder.nanhistory.feature.yearlymap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class YearlyMapMergeLogicTest {

    @Test
    fun distanceMeters_equatorOneDegree_isRoughly111km() {
        val p1 = MapPoint(0.0, 0.0)
        val p2 = MapPoint(1.0, 0.0)
        val dist = YearlyMapMergeLogic.distanceMeters(p1, p2)
        assertTrue("Distance should be around 111km, got $dist", abs(dist - 111195.0) < 500.0)
    }

    @Test
    fun perpendicularDistance_pointOnSegment_returnsZero() {
        val a = MapPoint(0.0, 0.0)
        val b = MapPoint(0.0, 0.01)
        val p = MapPoint(0.0, 0.005)
        val dist = YearlyMapMergeLogic.perpendicularDistance(p, a, b)
        assertTrue("Perpendicular distance should be near zero, got $dist", dist < 0.1)
    }

    @Test
    fun ramerDouglasPeucker_straightLine_reducesToTwoPoints() {
        val points = listOf(
            MapPoint(-6.2, 106.8),
            MapPoint(-6.2001, 106.8),
            MapPoint(-6.2002, 106.8),
            MapPoint(-6.2003, 106.8),
            MapPoint(-6.2004, 106.8)
        )
        val simplified = YearlyMapMergeLogic.ramerDouglasPeucker(points, epsilonMeters = 5.0)
        assertEquals(2, simplified.size)
        assertEquals(points.first(), simplified.first())
        assertEquals(points.last(), simplified.last())
    }

    @Test
    fun resample_splitsLongSegment() {
        val points = listOf(
            MapPoint(-6.2000, 106.8000),
            MapPoint(-6.2010, 106.8000)
        )
        val resampled = YearlyMapMergeLogic.resample(points, intervalMeters = 20.0)
        assertTrue("Should produce multiple points, got ${resampled.size}", resampled.size >= 5)
        assertEquals(points.first(), resampled.first())
        assertEquals(points.last(), resampled.last())
    }

    @Test
    fun oneKmTrack_resamplesToRoughlyFiftyNodes() {
        // ~1000 meters along longitude
        val pStart = MapPoint(0.0, 0.0)
        val pEnd = MapPoint(0.0, 0.009)
        val dist = YearlyMapMergeLogic.distanceMeters(pStart, pEnd)
        assertTrue("Distance should be ~1000m, was $dist", abs(dist - 1000.0) < 50.0)

        val resampled = YearlyMapMergeLogic.resample(listOf(pStart, pEnd), intervalMeters = 20.0)
        // 1000m / 20m = 50 intervals -> 51 nodes
        assertTrue("Expected ~51 nodes for 1 km track, got ${resampled.size}", resampled.size in 48..54)
    }

    @Test
    fun mergeParallelCorridors_collapsesCloseParallelLines() {
        val track1 = listOf(
            MapPoint(-6.2000, 106.80000),
            MapPoint(-6.2010, 106.80000)
        )
        val track2 = listOf(
            MapPoint(-6.2000, 106.80009),
            MapPoint(-6.2010, 106.80009)
        )

        val tracks = listOf(
            Pair("event1", track1),
            Pair("event2", track2)
        )

        val merged = YearlyMapMergeLogic.mergeParallelCorridors(
            tracks = tracks,
            corridorToleranceMeters = 25.0,
            rdpEpsilonMeters = 5.0,
            resampleIntervalMeters = 20.0
        )

        assertTrue("Should produce merged corridors", merged.isNotEmpty())
        merged.forEach { corridor ->
            assertTrue(
                "Each corridor segment should merge both events: ${corridor.eventIds}",
                corridor.eventIds.contains("event1") && corridor.eventIds.contains("event2")
            )
        }
    }

    @Test
    fun mergeParallelCorridors_staggeredPhaseStarts() {
        val track1 = listOf(
            MapPoint(-6.20000, 106.80000),
            MapPoint(-6.20100, 106.80000)
        )
        val track2 = listOf(
            MapPoint(-6.20015, 106.80008),
            MapPoint(-6.20100, 106.80008)
        )

        val tracks = listOf(
            Pair("event1", track1),
            Pair("event2", track2)
        )

        val merged = YearlyMapMergeLogic.mergeParallelCorridors(
            tracks = tracks,
            corridorToleranceMeters = 25.0,
            rdpEpsilonMeters = 5.0,
            resampleIntervalMeters = 20.0
        )

        val multiEventSegments = merged.filter { it.eventIds.size == 2 }
        assertTrue("Overlapping middle sections should merge both events", multiEventSegments.isNotEmpty())
    }

    @Test
    fun mergeParallelCorridors_bidirectionalMerging() {
        val track1 = listOf(
            MapPoint(-6.2000, 106.80000),
            MapPoint(-6.2010, 106.80000)
        )
        val track2 = listOf(
            MapPoint(-6.2010, 106.80009),
            MapPoint(-6.2000, 106.80009)
        )

        val tracks = listOf(
            Pair("event1", track1),
            Pair("event2", track2)
        )

        val merged = YearlyMapMergeLogic.mergeParallelCorridors(
            tracks = tracks,
            corridorToleranceMeters = 25.0,
            rdpEpsilonMeters = 5.0,
            resampleIntervalMeters = 20.0
        )

        assertTrue("Should merge bidirectional tracks", merged.isNotEmpty())
        merged.forEach { corridor ->
            assertTrue(
                "Should combine event IDs from opposite directions",
                corridor.eventIds.contains("event1") && corridor.eventIds.contains("event2")
            )
        }
    }

    @Test
    fun mergeParallelCorridors_farApartTracks_doNotMerge() {
        val track1 = listOf(
            MapPoint(-6.2000, 106.8000),
            MapPoint(-6.2010, 106.8000)
        )
        val track2 = listOf(
            MapPoint(-6.2000, 106.8100), // ~1.1 km away
            MapPoint(-6.2010, 106.8100)
        )

        val tracks = listOf(
            Pair("event1", track1),
            Pair("event2", track2)
        )

        val merged = YearlyMapMergeLogic.mergeParallelCorridors(
            tracks = tracks,
            corridorToleranceMeters = 25.0,
            rdpEpsilonMeters = 5.0,
            resampleIntervalMeters = 20.0
        )

        merged.forEach { corridor ->
            assertEquals(1, corridor.eventIds.size)
        }
    }

    @Test
    fun splitAtGpsGaps_fiveKmGap_producesTwoSegmentsNoEdgeAcrossGap() {
        // City A points
        val cityA1 = MapPoint(-6.2000, 106.8000, timestamp = 1000L)
        val cityA2 = MapPoint(-6.2005, 106.8000, timestamp = 2000L)
        // City B points ~6 km away
        val cityB1 = MapPoint(-6.2550, 106.8000, timestamp = 3000L)
        val cityB2 = MapPoint(-6.2555, 106.8000, timestamp = 4000L)

        val track = listOf(cityA1, cityA2, cityB1, cityB2)
        val tracks = listOf(Pair("flight", track))

        val corridors = YearlyMapMergeLogic.mergeParallelCorridors(tracks)

        assertEquals("Should produce exactly 2 separate segments across a 5km+ gap", 2, corridors.size)
        // Ensure no corridor bridges City A and City B
        corridors.forEach { c ->
            val dist = YearlyMapMergeLogic.distanceMeters(c.start, c.end)
            assertTrue("No corridor edge should bridge the 5km gap, but length was $dist", dist < 2000.0)
        }
    }

    @Test
    fun splitAtGpsGaps_timeGapWithDisplacement_splitsTrack() {
        val p1 = MapPoint(-6.2000, 106.8000, timestamp = 1000L)
        val p2 = MapPoint(-6.2005, 106.8000, timestamp = 2000L)
        // Lost GPS for 180s (>120s) with 300m displacement (>200m)
        val p3 = MapPoint(-6.2032, 106.8000, timestamp = 182000L)
        val p4 = MapPoint(-6.2037, 106.8000, timestamp = 183000L)

        val subtracks = YearlyMapMergeLogic.splitAtGpsGaps(listOf(p1, p2, p3, p4))
        assertEquals(2, subtracks.size)
        assertEquals(listOf(p1, p2), subtracks[0])
        assertEquals(listOf(p3, p4), subtracks[1])
    }

    @Test
    fun roadCurveWithNoise_preservesCurveShape() {
        // 90-degree road curve of radius 150m around (-6.2000, 106.8000)
        val centerLat = -6.2000
        val centerLon = 106.8000
        val radiusDeg = 0.00135 // ~150 meters
        val points = mutableListOf<MapPoint>()

        for (i in 0..15) {
            val theta = (i / 15.0) * (Math.PI / 2.0)
            // Add deterministic 3m noise
            val noise = if (i % 2 == 0) 0.000025 else -0.000025
            val lat = centerLat + (radiusDeg + noise) * sin(theta)
            val lon = centerLon + (radiusDeg + noise) * cos(theta)
            points.add(MapPoint(lat, lon))
        }

        val corridors = YearlyMapMergeLogic.mergeParallelCorridors(listOf(Pair("trip1", points)))
        assertTrue("Corridors should not be empty", corridors.isNotEmpty())

        val allPoints = corridors.flatMap { it.allPoints }
        // The midpoint along the curve should be visibly displaced from the direct chord
        val startPt = allPoints.first()
        val endPt = allPoints.last()
        val chordMid = MapPoint((startPt.lat + endPt.lat) / 2.0, (startPt.lon + endPt.lon) / 2.0)

        val maxDeviationFromChord = allPoints.maxOf { YearlyMapMergeLogic.distanceMeters(it, chordMid) }
        assertTrue("Curved road corridor should preserve arch displacement from chord", maxDeviationFromChord > 20.0)
    }

    @Test
    fun accuracyFiltering_dropsPoorAccuracyPoints() {
        val p1 = MapPoint(-6.2000, 106.8000, accuracy = 8.0f)
        val pSpike = MapPoint(-6.2050, 106.8050, accuracy = 120.0f) // > 50m error spike
        val p2 = MapPoint(-6.2005, 106.8000, accuracy = 10.0f)

        val corridors = YearlyMapMergeLogic.mergeParallelCorridors(listOf(Pair("trip1", listOf(p1, pSpike, p2))))
        assertEquals(1, corridors.size)
        // End points should connect p1 and p2 directly without the 120m spike
        val dist = YearlyMapMergeLogic.distanceMeters(corridors[0].start, corridors[0].end)
        assertTrue("Corridor should ignore >50m accuracy spike, distance was $dist", dist < 150.0)
    }

    @Test
    fun contractDegree2Nodes_collapsesDegree2ChainsIntoPolylines() {
        val n0 = MapPoint(-6.200, 106.800)
        val n1 = MapPoint(-6.201, 106.801)
        val n2 = MapPoint(-6.202, 106.802)
        val n3 = MapPoint(-6.203, 106.803)

        val rawEdges = mapOf(
            Pair(n0, n1) to setOf("e1"),
            Pair(n1, n2) to setOf("e1"),
            Pair(n2, n3) to setOf("e1")
        )

        val contracted = YearlyMapMergeLogic.contractDegree2Nodes(rawEdges)
        assertEquals(1, contracted.size)
        val seg = contracted[0]
        assertEquals(n0, seg.start)
        assertEquals(n3, seg.end)
        assertEquals(listOf(n1, n2), seg.intermediatePoints)
        assertEquals(4, seg.allPoints.size)
    }
}
