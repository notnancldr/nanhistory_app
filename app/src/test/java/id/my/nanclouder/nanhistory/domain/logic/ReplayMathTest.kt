package id.my.nanclouder.nanhistory.domain.logic

import id.my.nanclouder.nanhistory.domain.model.Coordinate
import id.my.nanclouder.nanhistory.domain.model.LocationData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class ReplayMathTest {

    @Test
    fun calculateDistanceMeters_knownLocations() {
        val monas = Coordinate(-6.1754, 106.8272)
        val bundaranHi = Coordinate(-6.1950, 106.8231)
        val dist = ReplayMath.calculateDistanceMeters(monas, bundaranHi)

        assertTrue(dist in 2100.0..2300.0)
    }

    @Test
    fun calculateBearing_cardinalDirections() {
        val origin = Coordinate(0.0, 0.0)
        assertEquals(0f, ReplayMath.calculateBearing(origin, Coordinate(1.0, 0.0)), 0.1f)
        assertEquals(90f, ReplayMath.calculateBearing(origin, Coordinate(0.0, 1.0)), 0.1f)
        assertEquals(180f, ReplayMath.calculateBearing(origin, Coordinate(-1.0, 0.0)), 0.1f)
        assertEquals(270f, ReplayMath.calculateBearing(origin, Coordinate(0.0, -1.0)), 0.1f)
    }

    @Test
    fun interpolateAt_midpointInterpolation() {
        val now = ZonedDateTime.now()
        val loc1 = LocationData(
            time = now,
            location = Coordinate(0.0, 0.0),
            speed = 10f,
            altitude = 100.0,
            accuracy = 5f
        )
        val loc2 = LocationData(
            time = now.plusSeconds(10),
            location = Coordinate(1.0, 1.0),
            speed = 20f,
            altitude = 200.0,
            accuracy = 10f
        )

        val locs = listOf(loc1, loc2)
        val distances = ReplayMath.computeCumulativeDistances(locs)
        val midpoint = ReplayMath.interpolateAt(locs, now.plusSeconds(5), distances)

        assertNotNull(midpoint)
        assertEquals(0.5, midpoint!!.coordinate.latitude, 0.001)
        assertEquals(0.5, midpoint.coordinate.longitude, 0.001)
        assertEquals(150.0, midpoint.altitudeMeters!!, 0.001)
        assertEquals(7.5f, midpoint.accuracyMeters!!, 0.001f)
    }
}
