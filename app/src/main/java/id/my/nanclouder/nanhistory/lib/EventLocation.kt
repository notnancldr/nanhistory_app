package id.my.nanclouder.nanhistory.lib

import id.my.nanclouder.nanhistory.calculateDistance
import id.my.nanclouder.nanhistory.calculateSpeed
import org.osmdroid.util.GeoPoint
import java.time.ZonedDateTime

data class HistoryLocationData(
    val speed: Float,
    val points: List<Coordinate>,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val distance: Float
)

fun Map<ZonedDateTime, Coordinate>.getLocationData(): List<HistoryLocationData> {
    val result: MutableList<HistoryLocationData> = mutableListOf()

    var prevKey: ZonedDateTime? = null
    for (key in this.keys.sorted()) {
        if (prevKey != null) {
            val firstPoint = this[prevKey]!!
            val secondPoint = this[key]!!

            val points = listOf(
                firstPoint,
                secondPoint
            ).map { GeoPoint( it.latitude, it.longitude ) }

            val time = (key.toInstant().toEpochMilli() - prevKey.toInstant().toEpochMilli())
            val timeHours = time / 3600000f

            val speed = calculateSpeed(points.first(), points.last(), timeHours)
            val distance = calculateDistance(points.first(), points.last())

//            Log.d("NanHistoryDebug", "Speed: $speed, Time: $time | $timeHours ($prevKey :: $key)")

            result.add(HistoryLocationData(
                speed = speed,
                points = listOf(firstPoint, secondPoint),
                start = prevKey,
                end = key,
                distance = distance
            ))
        }
        prevKey = key
    }
    return result.toList()
}