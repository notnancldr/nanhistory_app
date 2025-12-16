package id.my.nanclouder.nanhistory.utils

import id.my.nanclouder.nanhistory.calculateDistance
import id.my.nanclouder.nanhistory.calculateSpeed
import org.osmdroid.util.GeoPoint
import java.time.ZonedDateTime

data class HistoryLocationData(
    val speed: Float,
    val acceleration: Float,
    val points: List<Coordinate>,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val distance: Float,
)

fun Map<ZonedDateTime, Coordinate>.getLocationData(): List<HistoryLocationData> {
    val keys = this.keys.sorted()
    if (keys.size < 2) return emptyList()

    // Calculate speeds and times for all segments in one pass
    val segments = mutableListOf<Triple<Float, Float, ZonedDateTime>>() // speed, timeHours, endTime

    for (i in 1 until keys.size) {
        val prevKey = keys[i - 1]
        val currKey = keys[i]

        val firstPoint = this[prevKey]!!
        val secondPoint = this[currKey]!!

        val timeMillis = currKey.toInstant().toEpochMilli() - prevKey.toInstant().toEpochMilli()
        val timeHours = timeMillis / 3600000f

        val geoPointA = GeoPoint(firstPoint.latitude, firstPoint.longitude)
        val geoPointB = GeoPoint(secondPoint.latitude, secondPoint.longitude)

        val speed = calculateSpeed(geoPointA, geoPointB, timeHours)
        segments.add(Triple(speed, timeHours, currKey))
    }

    // Build result with acceleration calculation
    val result = mutableListOf<HistoryLocationData>()

    for (i in 1 until keys.size) {
        val prevKey = keys[i - 1]
        val currKey = keys[i]

        val firstPoint = this[prevKey]!!
        val secondPoint = this[currKey]!!

        val (speed, timeHours, _) = segments[i - 1]
        val distance = calculateDistance(
            GeoPoint(firstPoint.latitude, firstPoint.longitude),
            GeoPoint(secondPoint.latitude, secondPoint.longitude)
        )

        // Calculate acceleration: (speed_ahead - speed_behind) / time_between
        val acceleration = if (i > 1 && i < segments.size) {
            val speedBehind = segments[i - 2].first
            val speedAhead = segments[i].first
            val timeTotal = segments[i - 2].second + timeHours + segments[i].second

            (speedAhead - speedBehind) / timeTotal
        } else if (i < segments.size) {
            val speedAhead = segments[i].first
            val timeTotal = timeHours + segments[i].second

            (speedAhead - speed) / timeTotal
        } else if (i > 1) {
            val speedBehind = segments[i - 2].first
            val timeTotal = segments[i - 2].second + timeHours

            (speed - speedBehind) / timeTotal
        } else {
            0f
        }

        result.add(
            HistoryLocationData(
                speed = speed,
                acceleration = acceleration,
                points = listOf(firstPoint, secondPoint),
                start = prevKey,
                end = currKey,
                distance = distance
            )
        )
    }

    return result
}