package id.my.nanclouder.nanhistory.domain.logic

import id.my.nanclouder.nanhistory.domain.model.Coordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CorridorLogicTest {

    @Test
    fun simplifyCoordinates_collinearPoints_simplifiedToEndpoints() {
        val points = listOf(
            Coordinate(0.0, 0.0),
            Coordinate(0.5, 0.5),
            Coordinate(1.0, 1.0)
        )
        val simplified = CorridorLogic.simplifyCoordinates(points, epsilon = 0.001)
        assertEquals(2, simplified.size)
        assertEquals(Coordinate(0.0, 0.0), simplified.first())
        assertEquals(Coordinate(1.0, 1.0), simplified.last())
    }

    @Test
    fun simplifyCoordinates_divergentPoint_preserved() {
        val points = listOf(
            Coordinate(0.0, 0.0),
            Coordinate(0.5, 1.5),
            Coordinate(1.0, 1.0)
        )
        val simplified = CorridorLogic.simplifyCoordinates(points, epsilon = 0.001)
        assertEquals(3, simplified.size)
    }

    @Test
    fun perpendicularDistance_pointOnLine_isZero() {
        val start = Coordinate(0.0, 0.0)
        val end = Coordinate(2.0, 2.0)
        val mid = Coordinate(1.0, 1.0)
        assertEquals(0.0, CorridorLogic.perpendicularDistance(mid, start, end), 0.0001)
    }

    @Test
    fun approximateDistanceMeters_positiveDistance() {
        val c1 = Coordinate(-6.1754, 106.8272)
        val c2 = Coordinate(-6.1950, 106.8231)
        val dist = CorridorLogic.approximateDistanceMeters(c1, c2)
        assertTrue(dist in 2100.0..2300.0)
    }
}
