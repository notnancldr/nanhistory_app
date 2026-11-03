package id.my.nanclouder.nanhistory.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigTest {

    @Test
    fun verifyLocationAdaptiveIntervalDefaults() {
        assertEquals("locationAdaptiveIntervalEnabled", Config.locationAdaptiveIntervalEnabled.name)
        assertTrue(Config.Default.LOCATION_ADAPTIVE_INTERVAL_ENABLED)
    }

    @Test
    fun verifyLocationStationaryDetectionDefaults() {
        assertEquals("locationStationaryDetectionEnabled", Config.locationStationaryDetectionEnabled.name)
        assertTrue(Config.Default.LOCATION_STATIONARY_DETECTION_ENABLED)
    }
}
