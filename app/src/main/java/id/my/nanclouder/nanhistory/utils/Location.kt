package id.my.nanclouder.nanhistory.utils

import kotlin.math.pow

fun simplifyPoints(
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
        val leftPoints = simplifyPoints(points.subList(0, maxIndex + 1), epsilon)
        val rightPoints = simplifyPoints(points.subList(maxIndex, end + 1), epsilon)
        leftPoints.dropLast(1) + rightPoints
    } else {
        listOf(points[0], points[end])
    }
}

private fun perpendicularDistance(
    point: Coordinate,
    lineStart: Coordinate,
    lineEnd: Coordinate
): Double {
    val numerator = kotlin.math.abs(
        (lineEnd.latitude - lineStart.latitude) * point.longitude -
                (lineEnd.longitude - lineStart.longitude) * point.latitude +
                lineEnd.longitude * lineStart.latitude -
                lineEnd.latitude * lineStart.longitude
    )
    val denominator = kotlin.math.sqrt(
        (lineEnd.latitude - lineStart.latitude).pow(2) +
                (lineEnd.longitude - lineStart.longitude).pow(2)
    )
    return if (denominator == 0.0) 0.0 else numerator / denominator
}