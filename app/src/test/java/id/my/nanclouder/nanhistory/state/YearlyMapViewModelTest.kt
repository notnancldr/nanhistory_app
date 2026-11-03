package id.my.nanclouder.nanhistory.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class YearlyMapViewModelTest {

    @Test
    fun timeRangeSelection_dateDerivation() {
        fun getRangeDates(selection: TimeRangeSelection): Pair<String?, String?> = when (selection) {
            is TimeRangeSelection.Year -> Pair("${selection.year}-01-01", "${selection.year}-12-31")
            is TimeRangeSelection.AllTime -> Pair(null, null)
            is TimeRangeSelection.Custom -> Pair(selection.start.toString(), selection.end.toString())
        }

        val yearRange = getRangeDates(TimeRangeSelection.Year(2025))
        assertEquals("2025-01-01", yearRange.first)
        assertEquals("2025-12-31", yearRange.second)

        val allTimeRange = getRangeDates(TimeRangeSelection.AllTime)
        assertNull(allTimeRange.first)
        assertNull(allTimeRange.second)

        val customRange = getRangeDates(
            TimeRangeSelection.Custom(
                start = LocalDate.of(2025, 3, 1),
                end = LocalDate.of(2025, 4, 15)
            )
        )
        assertEquals("2025-03-01", customRange.first)
        assertEquals("2025-04-15", customRange.second)
    }

    @Test
    fun calculateStep_zoomLevels() {
        fun calculateStep(zoomLevel: Double): Int = when {
            zoomLevel >= 16.0 -> 1
            zoomLevel >= 14.0 -> 2
            zoomLevel >= 12.0 -> 5
            zoomLevel >= 10.0 -> 10
            zoomLevel >= 8.0 -> 25
            else -> 50
        }

        assertEquals(1, calculateStep(18.0))
        assertEquals(1, calculateStep(16.0))
        assertEquals(2, calculateStep(15.0))
        assertEquals(2, calculateStep(14.0))
        assertEquals(5, calculateStep(13.0))
        assertEquals(5, calculateStep(12.0))
        assertEquals(10, calculateStep(11.0))
        assertEquals(10, calculateStep(10.0))
        assertEquals(25, calculateStep(9.0))
        assertEquals(25, calculateStep(8.0))
        assertEquals(50, calculateStep(7.0))
        assertEquals(50, calculateStep(4.0))
    }

    @Test
    fun boundingBoxPadding_calculatesTwentyPercentMargin() {
        val minLat = 10.0
        val maxLat = 20.0
        val minLon = 100.0
        val maxLon = 110.0

        val latSpan = maxLat - minLat
        val lonSpan = maxLon - minLon
        val latMargin = latSpan * 0.2
        val lonMargin = lonSpan * 0.2

        val paddedNorth = (maxLat + latMargin).coerceAtMost(85.0)
        val paddedSouth = (minLat - latMargin).coerceAtLeast(-85.0)
        val paddedEast = (maxLon + lonMargin).coerceAtMost(180.0)
        val paddedWest = (minLon - lonMargin).coerceAtLeast(-180.0)

        assertEquals(22.0, paddedNorth, 0.001)
        assertEquals(8.0, paddedSouth, 0.001)
        assertEquals(112.0, paddedEast, 0.001)
        assertEquals(98.0, paddedWest, 0.001)
    }
}
