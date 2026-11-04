package id.my.nanclouder.nanhistory.state

import id.my.nanclouder.nanhistory.utils.Coordinate
import id.my.nanclouder.nanhistory.utils.history.LocationData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class ReplayInterpolatorTest {

    @Test
    fun calculateDistanceMeters_accurateForKnownCoordinates() {
        // Jakarta Monas to Bundaran HI: approx 2.0 km
        val monas = Coordinate(-6.1754, 106.8272)
        val bundaranHi = Coordinate(-6.1950, 106.8231)
        val dist = ReplayInterpolator.calculateDistanceMeters(monas, bundaranHi)

        assertTrue(dist in 2100.0..2300.0)
    }

    @Test
    fun calculateBearing_accurateDirection() {
        // Due North
        val p1 = Coordinate(0.0, 0.0)
        val pNorth = Coordinate(1.0, 0.0)
        val bearingNorth = ReplayInterpolator.calculateBearing(p1, pNorth)
        assertEquals(0f, bearingNorth, 0.1f)

        // Due East
        val pEast = Coordinate(0.0, 1.0)
        val bearingEast = ReplayInterpolator.calculateBearing(p1, pEast)
        assertEquals(90f, bearingEast, 0.1f)

        // Due South
        val pSouth = Coordinate(-1.0, 0.0)
        val bearingSouth = ReplayInterpolator.calculateBearing(p1, pSouth)
        assertEquals(180f, bearingSouth, 0.1f)

        // Due West
        val pWest = Coordinate(0.0, -1.0)
        val bearingWest = ReplayInterpolator.calculateBearing(p1, pWest)
        assertEquals(270f, bearingWest, 0.1f)
    }

    @Test
    fun computeCumulativeDistances_monotonicallyIncreases() {
        val now = ZonedDateTime.now()
        val locs = listOf(
            LocationData(now, Coordinate(0.0, 0.0)),
            LocationData(now.plusSeconds(10), Coordinate(0.001, 0.0)),
            LocationData(now.plusSeconds(20), Coordinate(0.002, 0.0))
        )
        val dists = ReplayInterpolator.computeCumulativeDistances(locs)

        assertEquals(3, dists.size)
        assertEquals(0.0, dists[0], 0.001)
        assertTrue(dists[1] > 0.0)
        assertTrue(dists[2] > dists[1])
    }

    @Test
    fun interpolateAt_midpointInterpolatesCoordinatesAndTelemetry() {
        val now = ZonedDateTime.now()
        val locs = listOf(
            LocationData(
                time = now,
                location = Coordinate(10.0, 20.0),
                altitude = 100.0,
                accuracy = 5f,
                speed = 10f
            ),
            LocationData(
                time = now.plusSeconds(10),
                location = Coordinate(10.01, 20.02),
                altitude = 200.0,
                accuracy = 15f,
                speed = 20f
            )
        )
        val dists = ReplayInterpolator.computeCumulativeDistances(locs)

        // Interpolate at exactly halfway (5 seconds)
        val midTime = now.plusSeconds(5)
        val telemetry = ReplayInterpolator.interpolateAt(locs, midTime, dists)

        assertNotNull(telemetry)
        assertEquals(10.005, telemetry!!.coordinate.latitude, 0.0001)
        assertEquals(20.01, telemetry.coordinate.longitude, 0.0001)
        assertEquals(150.0, telemetry.altitudeMeters!!, 0.01)
        assertEquals(10f, telemetry.accuracyMeters!!, 0.1f)
        assertEquals(dists[1] / 2.0, telemetry.distanceMeters, 1.0)
    }

    @Test
    fun detectDwellPoints_detectsExtendedStop() {
        val now = ZonedDateTime.now()
        val locs = listOf(
            LocationData(now, Coordinate(0.0, 0.0)),
            LocationData(now.plusSeconds(30), Coordinate(0.00001, 0.00001)),
            LocationData(now.plusSeconds(70), Coordinate(0.00002, 0.00001)), // 70s stationary
            LocationData(now.plusSeconds(120), Coordinate(0.01, 0.01)) // moved far away
        )

        val dwells = ReplayInterpolator.detectDwellPoints(locs, minDwellSeconds = 60, maxRadiusMeters = 30f)
        assertEquals(1, dwells.size)
        assertEquals(locs[0].location, dwells[0].location)
    }
}
