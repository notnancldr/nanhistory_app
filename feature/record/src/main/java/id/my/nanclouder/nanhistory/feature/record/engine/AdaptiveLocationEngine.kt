package id.my.nanclouder.nanhistory.feature.record.engine

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class SpeedBucket(val intervalMs: Long) {
    DRIVING(1_500L),    // > 8.0 m/s (~28.8 km/h)
    CYCLING(4_000L),    // 2.0 - 8.0 m/s (~7.2 - 28.8 km/h)
    WALKING(8_000L),    // 0.5 - 2.0 m/s (~1.8 - 7.2 km/h)
    CREEPING(15_000L)   // < 0.5 m/s (~1.8 km/h)
}

/**
 * Manages speed-based sampling intervals with a hysteresis counter to avoid request thrashing.
 */
class AdaptiveSamplingPolicy(
    val hysteresisThreshold: Int = 3
) {
    var currentBucket: SpeedBucket = SpeedBucket.CYCLING
        private set

    private var candidateBucket: SpeedBucket? = null
    private var candidateCount: Int = 0

    fun classifySpeed(speedMps: Float?): SpeedBucket {
        val speed = speedMps ?: return currentBucket
        return when {
            speed > 8.0f -> SpeedBucket.DRIVING
            speed in 2.0f..8.0f -> SpeedBucket.CYCLING
            speed in 0.5f..<2.0f -> SpeedBucket.WALKING
            else -> SpeedBucket.CREEPING
        }
    }

    /**
     * Feeds a new speed observation. Returns true if the active sampling bucket changed.
     */
    fun onNewSpeed(speedMps: Float?): Boolean {
        val targetBucket = classifySpeed(speedMps)
        if (targetBucket == currentBucket) {
            candidateBucket = null
            candidateCount = 0
            return false
        }

        if (targetBucket == candidateBucket) {
            candidateCount++
            if (candidateCount >= hysteresisThreshold) {
                currentBucket = targetBucket
                candidateBucket = null
                candidateCount = 0
                return true
            }
        } else {
            candidateBucket = targetBucket
            candidateCount = 1
        }
        return false
    }

    fun getRequestInterval(): Long = currentBucket.intervalMs

    fun getMaxUpdateDelay(isScreenOn: Boolean): Long = if (isScreenOn) 2_000L else 30_000L

    fun reset() {
        currentBucket = SpeedBucket.CYCLING
        candidateBucket = null
        candidateCount = 0
    }
}

/**
 * Detects whether the device has remained stationary at an anchor point over a duration.
 */
class StationaryDetector(
    val stationaryDurationMs: Long = 90_000L,
    val speedThresholdMps: Float = 0.5f
) {
    var anchorLatitude: Double? = null
        private set
    var anchorLongitude: Double? = null
        private set
    var anchorTimestampMs: Long = 0L
        private set
    var isStationary: Boolean = false
        private set

    /**
     * Evaluates a location fix against the anchor point.
     * Returns true on the exact transition to stationary mode.
     */
    fun update(
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        speedMps: Float?,
        timestampMs: Long
    ): Boolean {
        val anchorLat = anchorLatitude
        val anchorLon = anchorLongitude

        if (anchorLat == null || anchorLon == null) {
            anchorLatitude = latitude
            anchorLongitude = longitude
            anchorTimestampMs = timestampMs
            isStationary = false
            return false
        }

        val displacement = computeDistanceMeters(anchorLat, anchorLon, latitude, longitude)
        val maxAllowedDrift = maxOf(10.0, accuracy.toDouble())
        val isMoving = (speedMps != null && speedMps >= speedThresholdMps) || displacement > maxAllowedDrift

        if (isMoving) {
            anchorLatitude = latitude
            anchorLongitude = longitude
            anchorTimestampMs = timestampMs
            isStationary = false
            return false
        }

        if (timestampMs - anchorTimestampMs >= stationaryDurationMs) {
            if (!isStationary) {
                isStationary = true
                return true
            }
        }

        return false
    }

    fun reset() {
        anchorLatitude = null
        anchorLongitude = null
        anchorTimestampMs = 0L
        isStationary = false
    }

    companion object {
        fun computeDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val earthRadius = 6371000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return earthRadius * c
        }
    }
}
