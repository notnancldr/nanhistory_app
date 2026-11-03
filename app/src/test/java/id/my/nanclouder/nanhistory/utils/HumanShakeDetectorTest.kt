package id.my.nanclouder.nanhistory.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HumanShakeDetectorTest {

    private lateinit var detector: HumanShakeDetector

    @Before
    fun setUp() {
        detector = HumanShakeDetector().apply {
            accelThreshold = 20.0f
            gyroThreshold = 3.0f
            minDurationMs = 200L
            minDirectionChanges = 2
        }
    }

    @Test
    fun isPotentialShake_belowThreshold_returnsFalse() {
        // Resting gravity (~9.8 m/s²)
        assertFalse(detector.isPotentialShake(0f, 9.8f, 0f))
    }

    @Test
    fun isPotentialShake_atOrAboveThreshold_returnsTrue() {
        // High acceleration (25 m/s²)
        assertTrue(detector.isPotentialShake(20f, 15f, 0f))
    }

    @Test
    fun update_insufficientGyro_returnsFalse() {
        // Strong accel, but low gyro
        val shake = detector.update(
            ax = 25f, ay = 5f, az = 0f,
            gx = 0.5f, gy = 0.5f, gz = 0f,
            timestampMs = 1000L
        )
        assertFalse(shake)
    }

    @Test
    fun update_validShakeSequence_returnsTrue() {
        // Step 1: positive X, high accel + gyro
        assertFalse(
            detector.update(
                ax = 25f, ay = 0f, az = 0f,
                gx = 3.5f, gy = 0f, gz = 0f,
                timestampMs = 1000L
            )
        )

        // Step 2: direction reversal 1 (negative X)
        assertFalse(
            detector.update(
                ax = -25f, ay = 0f, az = 0f,
                gx = 3.5f, gy = 0f, gz = 0f,
                timestampMs = 1120L
            )
        )

        // Step 3: direction reversal 2 (positive X), duration = 250ms >= 200ms
        val detected = detector.update(
            ax = 25f, ay = 0f, az = 0f,
            gx = 3.5f, gy = 0f, gz = 0f,
            timestampMs = 1250L
        )

        assertTrue(detected)
    }

    @Test
    fun update_belowAccelThreshold_resetsDetector() {
        // Candidate start
        detector.update(
            ax = 25f, ay = 0f, az = 0f,
            gx = 3.5f, gy = 0f, gz = 0f,
            timestampMs = 1000L
        )

        // Accel drops below threshold
        assertFalse(
            detector.update(
                ax = 5f, ay = 0f, az = 0f,
                gx = 0f, gy = 0f, gz = 0f,
                timestampMs = 1050L
            )
        )

        // Fresh high accel afterwards starts timer at new timestamp
        assertFalse(
            detector.update(
                ax = 25f, ay = 0f, az = 0f,
                gx = 3.5f, gy = 0f, gz = 0f,
                timestampMs = 1100L
            )
        )
    }
}
