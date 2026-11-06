package id.my.nanclouder.nanhistory.domain.logic

import id.my.nanclouder.nanhistory.domain.model.Coordinate
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Pure domain mathematics for Ramer-Douglas-Peucker line simplification,
 * perpendicular distance calculation, and equirectangular distance estimation.
 */
object CorridorLogic {
    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Simplifies a trajectory using the Ramer-Douglas-Peucker (RDP) algorithm with [epsilon] threshold.
     */
    fun simplifyCoordinates(
        points: List<Coordinate>,
        epsilon: Double = 0.00005
    ): List<Coordinate> {
        if (points.size <= 2) return points

        var maxDistance = 0.0
        var maxIndex = 0
        val end = points.size - 1

        for (i in 1 until end) {
            val distance = perpendicularDistance(points[i], points[0], points[end])
            if (distance > maxDistance) {
                maxDistance = distance
                maxIndex = i
            }
        }

        return if (maxDistance > epsilon) {
            val leftPoints = simplifyCoordinates(points.subList(0, maxIndex + 1), epsilon)
            val rightPoints = simplifyCoordinates(points.subList(maxIndex, end + 1), epsilon)
            leftPoints.dropLast(1) + rightPoints
        } else {
            listOf(points[0], points[end])
        }
    }

    /**
     * Calculates perpendicular distance from [point] to the line segment between [lineStart] and [lineEnd].
     */
    fun perpendicularDistance(
        point: Coordinate,
        lineStart: Coordinate,
        lineEnd: Coordinate
    ): Double {
        val numerator = abs(
            (lineEnd.latitude - lineStart.latitude) * point.longitude -
                    (lineEnd.longitude - lineStart.longitude) * point.latitude +
                    lineEnd.longitude * lineStart.latitude -
                    lineEnd.latitude * lineStart.longitude
        )
        val denominator = sqrt(
            (lineEnd.latitude - lineStart.latitude).pow(2) +
                    (lineEnd.longitude - lineStart.longitude).pow(2)
        )
        return if (denominator == 0.0) 0.0 else numerator / denominator
    }

    /**
     * Calculates equirectangular approximate distance in meters between two coordinates.
     */
    fun approximateDistanceMeters(c1: Coordinate, c2: Coordinate): Double {
        val latMid = Math.toRadians((c1.latitude + c2.latitude) / 2.0)
        val dx = Math.toRadians(c2.longitude - c1.longitude) * EARTH_RADIUS_METERS * cos(latMid)
        val dy = Math.toRadians(c2.latitude - c1.latitude) * EARTH_RADIUS_METERS
        return hypot(dx, dy)
    }
}
