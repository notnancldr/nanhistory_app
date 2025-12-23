package id.my.nanclouder.nanhistory.utils

import id.my.nanclouder.nanhistory.calculateDistance
import id.my.nanclouder.nanhistory.calculateSpeed
import id.my.nanclouder.nanhistory.utils.history.LocationData
import org.osmdroid.util.GeoPoint
import java.time.ZonedDateTime

data class HistoryLocationData(
    val speed: Float,
    val acceleration: Float,
    val points: List<Coordinate>,
    val locationData: List<LocationData>,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val distance: Float,
)

@Deprecated(
    "Use function with List of LocationData as the receiver instead",
    ReplaceWith("fun List<LocationData>.getLocationData(): List<HistoryLocationData>")
)
fun Map<ZonedDateTime, Coordinate>.getLocationData() =
    map { LocationData(time = it.key, location = it.value) }.getLocationData()

fun List<LocationData>.getLocationData(): List<HistoryLocationData> {
    val sorted = sortedBy { it.time }
    if (size < 2) return emptyList()

    // Calculate speeds and times for all segments in one pass
    val segments = mutableListOf<Triple<Float, Float, ZonedDateTime>>() // speed, timeHours, endTime

    for (i in 1 until sorted.size) {
        val prevItem = sorted[i - 1]
        val currItem = sorted[i]

        val firstPoint = prevItem.location
        val secondPoint = currItem.location

        val timeMillis = currItem.time.toInstant().toEpochMilli() - prevItem.time.toInstant().toEpochMilli()
        val timeHours = timeMillis / 3600000f

        val geoPointA = GeoPoint(firstPoint.latitude, firstPoint.longitude)
        val geoPointB = GeoPoint(secondPoint.latitude, secondPoint.longitude)

        val speed = calculateSpeed(geoPointA, geoPointB, timeHours)
        segments.add(Triple(speed, timeHours, currItem.time))
    }

    // Build result with acceleration calculation
    val result = mutableListOf<HistoryLocationData>()

    for (i in 1 until sorted.size) {
        val prevItem = sorted[i - 1]
        val currItem = sorted[i]

        val firstPoint = prevItem.location
        val secondPoint = currItem.location

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
                start = prevItem.time,
                end = currItem.time,
                distance = distance,
                locationData = listOf(
                    prevItem,
                    currItem
                )
            )
        )
    }

    return result
}