package id.my.nanclouder.nanhistory.ui.map

import org.junit.Assert.assertEquals
import org.junit.Test

class MapZoomUtilsTest {

    @Test
    fun osmEquivalentZoom_addsOneLevelOffset() {
        assertEquals(5.0, MapZoomUtils.osmEquivalentZoom(4.0), 0.001)
        assertEquals(16.0, MapZoomUtils.osmEquivalentZoom(15.0), 0.001)
        assertEquals(1.0, MapZoomUtils.osmEquivalentZoom(0.0), 0.001)
    }

    @Test
    fun mapLibreEquivalentZoom_subtractsOneLevelOffset() {
        assertEquals(4.0, MapZoomUtils.mapLibreEquivalentZoom(5.0), 0.001)
        assertEquals(15.0, MapZoomUtils.mapLibreEquivalentZoom(16.0), 0.001)
        assertEquals(0.0, MapZoomUtils.mapLibreEquivalentZoom(1.0), 0.001)
    }
}
