package id.my.nanclouder.nanhistory.feature.record.engine

import id.my.nanclouder.nanhistory.feature.record.engine.AdaptiveSamplingPolicy
import id.my.nanclouder.nanhistory.feature.record.engine.SpeedBucket
import id.my.nanclouder.nanhistory.feature.record.engine.StationaryDetector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdaptiveLocationEngineTest {

    private lateinit var samplingPolicy: AdaptiveSamplingPolicy
    private lateinit var stationaryDetector: StationaryDetector

    @Before
    fun setUp() {
        samplingPolicy = AdaptiveSamplingPolicy(hysteresisThreshold = 3)
        stationaryDetector = StationaryDetector(stationaryDurationMs = 90_000L, speedThresholdMps = 0.5f)
    }

    @Test
    fun adaptiveSampling_hysteresisRequiresThreeConsecutiveFixes() {
        assertEquals(SpeedBucket.CYCLING, samplingPolicy.currentBucket)
        assertEquals(4_000L, samplingPolicy.getRequestInterval())

        // 1st driving fix
        assertFalse(samplingPolicy.onNewSpeed(10.0f))
        assertEquals(SpeedBucket.CYCLING, samplingPolicy.currentBucket)

        // 2nd driving fix
        assertFalse(samplingPolicy.onNewSpeed(12.0f))
        assertEquals(SpeedBucket.CYCLING, samplingPolicy.currentBucket)

        // 3rd driving fix -> should switch!
        assertTrue(samplingPolicy.onNewSpeed(9.5f))
        assertEquals(SpeedBucket.DRIVING, samplingPolicy.currentBucket)
        assertEquals(1_500L, samplingPolicy.getRequestInterval())
    }

    @Test
    fun adaptiveSampling_fluctuationDoesNotTriggerSwitch() {
        // 1st driving fix
        assertFalse(samplingPolicy.onNewSpeed(10.0f))
        // Interrupted by cycling fix
        assertFalse(samplingPolicy.onNewSpeed(4.0f))
        // 1st driving fix again
        assertFalse(samplingPolicy.onNewSpeed(10.0f))
        assertEquals(SpeedBucket.CYCLING, samplingPolicy.currentBucket)
    }

    @Test
    fun adaptiveSampling_screenStateDelay() {
        assertEquals(2_000L, samplingPolicy.getMaxUpdateDelay(isScreenOn = true))
        assertEquals(30_000L, samplingPolicy.getMaxUpdateDelay(isScreenOn = false))
    }

    @Test
    fun stationaryDetector_triggersAfterDurationAtAnchor() {
        val lat = -6.2088
        val lon = 106.8456

        // Fix 1 at t=0: establishes anchor
        assertFalse(stationaryDetector.update(lat, lon, accuracy = 5f, speedMps = 0.1f, timestampMs = 0L))
        assertFalse(stationaryDetector.isStationary)

        // Fix 2 at t=45s: still near anchor
        assertFalse(
            stationaryDetector.update(lat + 0.00001, lon, accuracy = 5f, speedMps = 0.1f, timestampMs = 45_000L)
        )
        assertFalse(stationaryDetector.isStationary)

        // Fix 3 at t=90s: reached 90s stationary threshold
        val transitioned = stationaryDetector.update(
            lat + 0.00002, lon, accuracy = 5f, speedMps = 0.0f, timestampMs = 90_000L
        )
        assertTrue(transitioned)
        assertTrue(stationaryDetector.isStationary)

        // Subsequent fix while stationary returns false (transition already occurred)
        assertFalse(
            stationaryDetector.update(lat + 0.00001, lon, accuracy = 5f, speedMps = 0.0f, timestampMs = 95_000L)
        )
        assertTrue(stationaryDetector.isStationary)
    }

    @Test
    fun stationaryDetector_resetsOnMovement() {
        val lat = -6.2088
        val lon = 106.8456

        // Enter stationary
        stationaryDetector.update(lat, lon, accuracy = 5f, speedMps = 0.1f, timestampMs = 0L)
        stationaryDetector.update(lat, lon, accuracy = 5f, speedMps = 0.1f, timestampMs = 90_000L)
        assertTrue(stationaryDetector.isStationary)

        // Move 200m away at speed 3.0 m/s
        val movedLat = lat + 0.002
        assertFalse(stationaryDetector.update(movedLat, lon, accuracy = 5f, speedMps = 3.0f, timestampMs = 95_000L))
        assertFalse(stationaryDetector.isStationary)
    }
}
