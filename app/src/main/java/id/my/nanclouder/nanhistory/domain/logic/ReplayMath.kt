package id.my.nanclouder.nanhistory.domain.logic

import id.my.nanclouder.nanhistory.domain.model.Coordinate
import id.my.nanclouder.nanhistory.domain.model.LocationData
import java.time.ZonedDateTime
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Snapshot of telemetry metrics at the current replay instant.
 */
data class ReplayTelemetry(
    val coordinate: Coordinate,
    val timestamp: ZonedDateTime,
    val speedKmh: Float,
    val altitudeMeters: Double?,
    val accuracyMeters: Float?,
    val bearingDegrees: Float,
    val distanceMeters: Double
)

/**
 * Pure mathematics and coordinate interpolation utilities for route replay.
 */
object ReplayMath {

    val SPEED_PRESETS = listOf(0.25f, 0.5f, 1f, 2f, 5f, 10f, 25f, 50f)

    /**
     * Calculates great-circle distance between two coordinates in meters (Haversine formula).
     */
    fun calculateDistanceMeters(c1: Coordinate, c2: Coordinate): Double {
        val r = 6371000.0
        val lat1 = Math.toRadians(c1.latitude)
        val lat2 = Math.toRadians(c2.latitude)
        val dLat = Math.toRadians(c2.latitude - c1.latitude)
        val dLon = Math.toRadians(c2.longitude - c1.longitude)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Precomputes running cumulative distance along a sorted list of locations.
     */
    fun computeCumulativeDistances(locations: List<LocationData>): DoubleArray {
        if (locations.isEmpty()) return DoubleArray(0)
        val distances = DoubleArray(locations.size)
        distances[0] = 0.0

        for (i in 1 until locations.size) {
            val prev = locations[i - 1].location
            val curr = locations[i].location
            distances[i] = distances[i - 1] + calculateDistanceMeters(prev, curr)
        }
        return distances
    }

    /**
     * Calculates azimuth/bearing from [start] to [end] in degrees [0, 360).
     */
    fun calculateBearing(start: Coordinate, end: Coordinate): Float {
        val lat1 = Math.toRadians(start.latitude)
        val lon1 = Math.toRadians(start.longitude)
        val lat2 = Math.toRadians(end.latitude)
        val lon2 = Math.toRadians(end.longitude)

        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val initialBearing = Math.toDegrees(atan2(y, x))
        return ((initialBearing + 360.0) % 360.0).toFloat()
    }

    /**
     * Interpolates telemetry at [targetTime] within [locations].
     */
    fun interpolateAt(
        locations: List<LocationData>,
        targetTime: ZonedDateTime,
        cumulativeDistances: DoubleArray
    ): ReplayTelemetry? {
        if (locations.isEmpty()) return null

        val targetMillis = targetTime.toInstant().toEpochMilli()
        val firstMillis = locations.first().time.toInstant().toEpochMilli()
        val lastMillis = locations.last().time.toInstant().toEpochMilli()

        if (targetMillis <= firstMillis) {
            val first = locations.first()
            val bearing = if (locations.size > 1) {
                calculateBearing(first.location, locations[1].location)
            } else {
                first.bearing ?: 0f
            }
            return ReplayTelemetry(
                coordinate = first.location,
                timestamp = first.time,
                speedKmh = (first.speed ?: 0f) * 3.6f,
                altitudeMeters = first.altitude,
                accuracyMeters = first.accuracy,
                bearingDegrees = bearing,
                distanceMeters = 0.0
            )
        }

        if (targetMillis >= lastMillis) {
            val last = locations.last()
            val totalDistance = if (cumulativeDistances.isNotEmpty()) cumulativeDistances.last() else 0.0
            val bearing = if (locations.size > 1) {
                calculateBearing(locations[locations.size - 2].location, last.location)
            } else {
                last.bearing ?: 0f
            }
            return ReplayTelemetry(
                coordinate = last.location,
                timestamp = last.time,
                speedKmh = (last.speed ?: 0f) * 3.6f,
                altitudeMeters = last.altitude,
                accuracyMeters = last.accuracy,
                bearingDegrees = bearing,
                distanceMeters = totalDistance
            )
        }

        var low = 0
        var high = locations.size - 1
        var idx = 0

        while (low <= high) {
            val mid = (low + high) ushr 1
            val midMillis = locations[mid].time.toInstant().toEpochMilli()
            if (midMillis <= targetMillis) {
                idx = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        val nextIdx = (idx + 1).coerceAtMost(locations.size - 1)
        val p1 = locations[idx]
        val p2 = locations[nextIdx]

        val t1 = p1.time.toInstant().toEpochMilli()
        val t2 = p2.time.toInstant().toEpochMilli()
        val span = (t2 - t1).toDouble()

        val alpha = if (span > 0) ((targetMillis - t1) / span).coerceIn(0.0, 1.0) else 0.0

        val lat = p1.location.latitude + alpha * (p2.location.latitude - p1.location.latitude)
        val lon = p1.location.longitude + alpha * (p2.location.longitude - p1.location.longitude)

        val bearing = calculateBearing(p1.location, p2.location)

        val alt = when {
            p1.altitude != null && p2.altitude != null -> p1.altitude + alpha * (p2.altitude - p1.altitude)
            p1.altitude != null -> p1.altitude
            else -> p2.altitude
        }

        val acc = when {
            p1.accuracy != null && p2.accuracy != null -> (p1.accuracy + alpha * (p2.accuracy - p1.accuracy)).toFloat()
            p1.accuracy != null -> p1.accuracy
            else -> p2.accuracy
        }

        val baseDist = if (idx < cumulativeDistances.size) cumulativeDistances[idx] else 0.0
        val nextDist = if (nextIdx < cumulativeDistances.size) cumulativeDistances[nextIdx] else baseDist
        val dist = baseDist + alpha * (nextDist - baseDist)

        val speedKmh = if (span > 0) {
            val segDist = nextDist - baseDist
            val segHours = span / 3600000.0
            (segDist / 1000.0 / segHours).toFloat()
        } else {
            (p1.speed ?: 0f) * 3.6f
        }

        return ReplayTelemetry(
            coordinate = Coordinate(lat, lon),
            timestamp = targetTime,
            speedKmh = speedKmh.coerceAtLeast(0f),
            altitudeMeters = alt,
            accuracyMeters = acc,
            bearingDegrees = bearing,
            distanceMeters = dist
        )
    }

    /**
     * Detects stationary dwell points along the route where duration >= [minDwellSeconds].
     */
    fun detectDwellPoints(
        locations: List<LocationData>,
        minDwellSeconds: Long = 60,
        maxRadiusMeters: Float = 30f
    ): List<LocationData> {
        if (locations.size < 2) return emptyList()
        val dwells = mutableListOf<LocationData>()

        var clusterStart = locations[0]
        var clusterEnd = locations[0]

        for (i in 1 until locations.size) {
            val curr = locations[i]
            val dist = calculateDistanceMeters(clusterStart.location, curr.location)

            if (dist <= maxRadiusMeters) {
                clusterEnd = curr
            } else {
                val durationSec = clusterEnd.time.toEpochSecond() - clusterStart.time.toEpochSecond()
                if (durationSec >= minDwellSeconds) {
                    dwells.add(clusterStart)
                }
                clusterStart = curr
                clusterEnd = curr
            }
        }

        val durationSec = clusterEnd.time.toEpochSecond() - clusterStart.time.toEpochSecond()
        if (durationSec >= minDwellSeconds) {
            dwells.add(clusterStart)
        }

        return dwells
    }
}
