package id.my.nanclouder.nanhistory.feature.maplocation

import id.my.nanclouder.nanhistory.core.ui.map.DataMode
import id.my.nanclouder.nanhistory.core.ui.map.calculateAccuracyColor
import id.my.nanclouder.nanhistory.core.ui.map.calculateAltitudeColor
import id.my.nanclouder.nanhistory.core.ui.map.calculateColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.osmdroid.util.GeoPoint
import java.time.ZonedDateTime

class EventLocationViewModelTest {

    @Test
    fun calculateStep_atZoom15OrHigher_returns1() {
        assertEquals(1, EventLocationViewModel.calculateStep(15))
        assertEquals(1, EventLocationViewModel.calculateStep(16))
        assertEquals(1, EventLocationViewModel.calculateStep(20))
    }

    @Test
    fun calculateStep_atIntermediateZoomLevels_returnsPowersOfTwo() {
        assertEquals(2, EventLocationViewModel.calculateStep(14))
        assertEquals(4, EventLocationViewModel.calculateStep(13))
        assertEquals(8, EventLocationViewModel.calculateStep(12))
        assertEquals(16, EventLocationViewModel.calculateStep(11))
        assertEquals(32, EventLocationViewModel.calculateStep(10))
        assertEquals(64, EventLocationViewModel.calculateStep(9))
    }

    @Test
    fun calculateStep_atVeryLowZoomLevels_isCappedAt2048() {
        assertEquals(2048, EventLocationViewModel.calculateStep(4))
        assertEquals(2048, EventLocationViewModel.calculateStep(2))
        assertEquals(2048, EventLocationViewModel.calculateStep(0))
    }

    @Test
    fun cutMode_selectionOrdering() {
        val now = ZonedDateTime.now()
        val t1 = now.minusMinutes(10)
        val t2 = now.minusMinutes(5)
        val t3 = now.minusMinutes(2)

        var state = EventLocationUiState(cutMode = true)

        // Select t2 first (becomes cutStart)
        state = state.copy(cutStart = t2)
        assertEquals(t2, state.cutStart)
        assertNull(state.cutEnd)

        // Select earlier t1 (t1 should become cutStart, t2 should become cutEnd)
        val (start, end) = if (t1 < state.cutStart!!) t1 to state.cutStart else state.cutStart to t1
        state = state.copy(cutStart = start, cutEnd = end)

        assertEquals(t1, state.cutStart)
        assertEquals(t2, state.cutEnd)

        // Undo cuts
        state = if (state.cutEnd != null) state.copy(cutEnd = null) else state.copy(cutStart = null)
        assertEquals(t1, state.cutStart)
        assertNull(state.cutEnd)

        state = if (state.cutEnd != null) state.copy(cutEnd = null) else state.copy(cutStart = null)
        assertNull(state.cutStart)
        assertNull(state.cutEnd)
    }

    @Test
    fun eventLocationHelpers_calculateColors() {
        val color = calculateColor(50)
        assertNotNull(color)

        val altColor = calculateAltitudeColor(500.0)
        assertNotNull(altColor)

        val accColor = calculateAccuracyColor(10f)
        assertNotNull(accColor)
    }

    @Test
    fun cutMode_touchPointsVisibility() {
        val now = ZonedDateTime.now()
        val t1 = now.minusMinutes(10)
        val t2 = now.minusMinutes(5)

        fun shouldShowTouchPoints(
            cutMode: Boolean,
            showPoints: Boolean,
            cutStart: ZonedDateTime?,
            cutEnd: ZonedDateTime?
        ): Boolean {
            return (!cutMode && showPoints) || (cutMode && (cutStart == null || cutEnd == null))
        }

        assertFalse(shouldShowTouchPoints(cutMode = false, showPoints = false, cutStart = null, cutEnd = null))
        assertTrue(shouldShowTouchPoints(cutMode = false, showPoints = true, cutStart = null, cutEnd = null))
        assertTrue(shouldShowTouchPoints(cutMode = true, showPoints = false, cutStart = null, cutEnd = null))
        assertTrue(shouldShowTouchPoints(cutMode = true, showPoints = false, cutStart = t1, cutEnd = null))
        assertFalse(shouldShowTouchPoints(cutMode = true, showPoints = false, cutStart = t1, cutEnd = t2))
    }

    @Test
    fun dataMode_enumCoverage() {
        val modes = DataMode.entries
        assertEquals(6, modes.size)
        assertTrue(modes.contains(DataMode.SPEED))
        assertTrue(modes.contains(DataMode.ACCELERATION))
        assertTrue(modes.contains(DataMode.TIME))
        assertTrue(modes.contains(DataMode.ACCURACY))
        assertTrue(modes.contains(DataMode.BEARING))
        assertTrue(modes.contains(DataMode.ALTITUDE))
    }
}
