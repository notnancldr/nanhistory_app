package id.my.nanclouder.nanhistory.core.common.location

import android.location.Location
import id.my.nanclouder.nanhistory.domain.model.Coordinate
import id.my.nanclouder.nanhistory.domain.model.HistoryLocationData
import id.my.nanclouder.nanhistory.domain.model.LocationData
import java.time.ZonedDateTime

fun calculateDistance(startLat: Double, startLon: Double, endLat: Double, endLon: Double): Float {
    val results = FloatArray(1)
    Location.distanceBetween(startLat, startLon, endLat, endLon, results)
    return results[0]
}

fun calculateDistance(startPoint: Coordinate, endPoint: Coordinate): Float =
    calculateDistance(startPoint.latitude, startPoint.longitude, endPoint.latitude, endPoint.longitude)

fun calculateSpeed(startPoint: Coordinate, endPoint: Coordinate, timeHours: Float): Float =
    calculateDistance(startPoint, endPoint) / 1000f / timeHours

fun Map<ZonedDateTime, Coordinate>.getLocationData(): List<HistoryLocationData> =
    map { LocationData(time = it.key, location = it.value) }.getLocationData()

fun List<LocationData>.getLocationData(): List<HistoryLocationData> {
    val sorted = sortedBy { it.time }
    if (size < 2) return emptyList()

    val segments = mutableListOf<Triple<Float, Float, ZonedDateTime>>() // speed, timeHours, endTime

    for (i in 1 until sorted.size) {
        val prevItem = sorted[i - 1]
        val currItem = sorted[i]

        val firstPoint = prevItem.location
        val secondPoint = currItem.location

        val timeMillis = currItem.time.toInstant().toEpochMilli() - prevItem.time.toInstant().toEpochMilli()
        val timeHours = timeMillis / 3600000f

        val speed = if (timeHours > 0f) {
            (calculateDistance(firstPoint.latitude, firstPoint.longitude, secondPoint.latitude, secondPoint.longitude) / 1000f) / timeHours
        } else 0f
        segments.add(Triple(speed, timeHours, currItem.time))
    }

    val result = mutableListOf<HistoryLocationData>()

    for (i in 1 until sorted.size) {
        val prevItem = sorted[i - 1]
        val currItem = sorted[i]

        val firstPoint = prevItem.location
        val secondPoint = currItem.location

        val (speed, timeHours, _) = segments[i - 1]
        val distance = calculateDistance(
            firstPoint.latitude,
            firstPoint.longitude,
            secondPoint.latitude,
            secondPoint.longitude
        )

        val acceleration = if (i > 1 && i < segments.size) {
            val speedBehind = segments[i - 2].first
            val speedAhead = segments[i].first
            val timeTotal = segments[i - 2].second + timeHours + segments[i].second

            if (timeTotal > 0f) (speedAhead - speedBehind) / timeTotal else 0f
        } else if (i < segments.size) {
            val speedAhead = segments[i].first
            val timeTotal = timeHours + segments[i].second

            if (timeTotal > 0f) (speedAhead - speed) / timeTotal else 0f
        } else if (i > 1) {
            val speedBehind = segments[i - 2].first
            val timeTotal = segments[i - 2].second + timeHours

            if (timeTotal > 0f) (speed - speedBehind) / timeTotal else 0f
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
                locationData = listOf(prevItem, currItem)
            )
        )
    }

    return result
}
