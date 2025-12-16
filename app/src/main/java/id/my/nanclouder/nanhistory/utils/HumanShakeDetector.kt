package id.my.nanclouder.nanhistory.utils

import android.util.Log
import kotlin.math.sqrt

class HumanShakeDetector {

    var accelThreshold = 1f        // Increased from 2.5f - requires stronger acceleration
    var gyroThreshold = 0.2f        // Increased from 1.0f - requires faster rotation
    var minDurationMs = 100L        // Increased from 150L - requires longer shake duration
    var minDirectionChanges = 2     // Increased from 2 - requires more direction reversals

    private var startTime = 0L
    private var lastSignX: Int? = null
    private var directionChanges = 0

    fun reset() {
        startTime = 0L
        lastSignX = null
        directionChanges = 0
    }

    /**
     * Returns true if a human shake is detected.
     * Tuned for less sensitivity to avoid false positives.
     */
    fun update(
        ax: Float, ay: Float, az: Float,
        gx: Float, gy: Float, gz: Float,
        timestampMs: Long
    ): Boolean {
        val magnitude = sqrt(ax*ax + ay*ay + az*az)

        // If acceleration is below threshold, reset detector
        if (magnitude < accelThreshold) {
            reset()
            return false
        }

        // Start timing the shake
        if (startTime == 0L) startTime = timestampMs

        // Track direction changes in X axis
        val signX = if (ax >= 0) 1 else -1
        if (lastSignX != null && lastSignX != signX) directionChanges++
        lastSignX = signX

        val gyroMag = sqrt(gx*gx + gy*gy + gz*gz)
        val duration = timestampMs - startTime

        // All conditions must be met for shake detection
        val isHuman = magnitude >= accelThreshold &&
                gyroMag >= gyroThreshold &&
                directionChanges >= minDirectionChanges &&
                duration >= minDurationMs

        if (isHuman) reset()
        return isHuman
    }
}