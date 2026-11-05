package id.my.nanclouder.nanhistory.ui.map

import id.my.nanclouder.nanhistory.ui.map.maplibre.TripMapSnapshotter
import id.my.nanclouder.nanhistory.utils.Coordinate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripMapSnapshotterTest {

    @Test
    fun calculateCenterAndZoom_emptyList_returnsDefaultRegion() {
        val region = TripMapSnapshotter.calculateCenterAndZoom(emptyList(), 1080, 1080)
        assertEquals(0.0, region.centerLat, 0.001)
        assertEquals(0.0, region.centerLon, 0.001)
        assertEquals(1.0, region.zoom, 0.001)
    }

    @Test
    fun calculateCenterAndZoom_singleCoordinate_returnsCoordinateWithSafeZoom() {
        val coords = listOf(Coordinate(latitude = -7.2504, longitude = 112.7688))
        val region = TripMapSnapshotter.calculateCenterAndZoom(coords, 1080, 1080)
        assertEquals(-7.2504, region.centerLat, 0.001)
        assertEquals(112.7688, region.centerLon, 0.001)
        assertTrue(region.zoom >= 10.0)
    }

    @Test
    fun calculateCenterAndZoom_validCoordinates_returnsCorrectCenterAndZoom() {
        val coords = listOf(
            Coordinate(latitude = -6.2000, longitude = 106.8000),
            Coordinate(latitude = -6.2100, longitude = 106.8200)
        )

        val region = TripMapSnapshotter.calculateCenterAndZoom(coords, 1080, 1080)
        assertEquals(-6.2050, region.centerLat, 0.001)
        assertEquals(106.8100, region.centerLon, 0.001)
        assertTrue(region.zoom in 10.0..18.0)
    }

    @Test
    fun calculateCenterAndZoom_higherResolution_increasesZoomProportionally() {
        val coords = listOf(
            Coordinate(latitude = -6.2000, longitude = 106.8000),
            Coordinate(latitude = -6.2100, longitude = 106.8200)
        )
        val region1080 = TripMapSnapshotter.calculateCenterAndZoom(coords, 1080, 1080)
        val region2160 = TripMapSnapshotter.calculateCenterAndZoom(coords, 2160, 2160)
        val region3840 = TripMapSnapshotter.calculateCenterAndZoom(coords, 3840, 3840)

        assertEquals(1.0, region2160.zoom - region1080.zoom, 0.05)
        assertEquals(1.83, region3840.zoom - region1080.zoom, 0.05)
    }

    @Test
    fun resolveRegion_customCamera_retainsCustomCameraExactValues() {
        val coords = listOf(Coordinate(-6.2, 106.8))
        val region = TripMapSnapshotter.resolveRegion(
            coords = coords,
            widthPx = 1080,
            heightPx = 1080,
            customCenterLon = 106.85,
            customCenterLat = -6.25,
            customZoom = 12.0
        )
        assertEquals(-6.25, region.centerLat, 0.001)
        assertEquals(106.85, region.centerLon, 0.001)
        assertEquals(12.0, region.zoom, 0.001)
    }

    @Test
    fun resolveRegion_noCustomCamera_delegatesToCalculateCenterAndZoom() {
        val coords = listOf(
            Coordinate(latitude = -6.2000, longitude = 106.8000),
            Coordinate(latitude = -6.2100, longitude = 106.8200)
        )
        val resolved = TripMapSnapshotter.resolveRegion(
            coords = coords,
            widthPx = 1080,
            heightPx = 1080,
            customCenterLon = null,
            customCenterLat = null,
            customZoom = null
        )
        val calculated = TripMapSnapshotter.calculateCenterAndZoom(coords, 1080, 1080)
        assertEquals(calculated.centerLat, resolved.centerLat, 0.001)
        assertEquals(calculated.centerLon, resolved.centerLon, 0.001)
        assertEquals(calculated.zoom, resolved.zoom, 0.001)
    }

    @Test
    fun captureMapSnapshot_emptyLocations_returnsNullImmediately() = runBlocking {
        val result = TripMapSnapshotter.captureMapSnapshot(
            locations = emptyList(),
            widthPx = 1080,
            heightPx = 1080,
            isDark = false
        )
        assertNull(result)
    }

    @Test
    fun captureMapSnapshot_invalidDimensions_returnsNullImmediately() = runBlocking {
        val result = TripMapSnapshotter.captureMapSnapshot(
            locations = emptyList(),
            widthPx = 0,
            heightPx = -10,
            isDark = false
        )
        assertNull(result)
    }
}
