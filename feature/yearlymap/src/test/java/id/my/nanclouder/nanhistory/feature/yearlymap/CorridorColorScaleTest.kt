package id.my.nanclouder.nanhistory.feature.yearlymap

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CorridorColorScaleTest {

    @Test
    fun calculateT_boundaryConditions() {
        // Single trip or invalid maxCount always returns 0f
        assertEquals(0f, CorridorColorScale.calculateT(count = 1, maxCount = 1), 0.0001f)
        assertEquals(0f, CorridorColorScale.calculateT(count = 1, maxCount = 100), 0.0001f)
        assertEquals(0f, CorridorColorScale.calculateT(count = 0, maxCount = 100), 0.0001f)

        // Multiple trips: count = 2 with maxCount = 100 should start above 0.08
        val tAtTwo = CorridorColorScale.calculateT(count = 2, maxCount = 100)
        assertTrue("t for count=2 should exceed 0.08", tAtTwo >= 0.08f)

        // Logarithmic scale: count = 10 with maxCount = 100 is halfway in log space (ln(10)/ln(100) = 0.5)
        // 0.08 + 0.92 * 0.5 = 0.54
        assertEquals(0.54f, CorridorColorScale.calculateT(count = 10, maxCount = 100), 0.001f)

        // Max count always returns 1.0f
        assertEquals(1.0f, CorridorColorScale.calculateT(count = 100, maxCount = 100), 0.0001f)
    }

    @Test
    fun calculateStrokeWidth_interpolatesBetweenThreeAndSevenDp() {
        assertEquals(3.0.dp, CorridorColorScale.calculateStrokeWidth(0.0f))
        assertEquals(5.0.dp, CorridorColorScale.calculateStrokeWidth(0.5f))
        assertEquals(7.0.dp, CorridorColorScale.calculateStrokeWidth(1.0f))
    }

    @Test
    fun calculateColorFromT_producesExpectedHuesAndValues() {
        val colorAtZero = CorridorColorScale.calculateColorFromT(0f)
        // Blue dominant (hue 240)
        assertTrue("Color at t=0 should be blue dominant", colorAtZero.blue > colorAtZero.red)

        val colorAtOne = CorridorColorScale.calculateColorFromT(1f)
        // Red dominant (hue 20)
        assertTrue("Color at t=1 should be red dominant", colorAtOne.red > colorAtOne.blue)
    }

    @Test
    fun colorStops_containNineAscendingStops() {
        val stops = CorridorColorScale.COLOR_STOPS
        assertEquals(9, stops.size)
        assertEquals(0.000f, stops[0].first, 0.001f)
        assertEquals(0.080f, stops[1].first, 0.001f)
        assertEquals(0.200f, stops[2].first, 0.001f)
        assertEquals(0.350f, stops[3].first, 0.001f)
        assertEquals(0.500f, stops[4].first, 0.001f)
        assertEquals(0.650f, stops[5].first, 0.001f)
        assertEquals(0.800f, stops[6].first, 0.001f)
        assertEquals(0.900f, stops[7].first, 0.001f)
        assertEquals(1.000f, stops[8].first, 0.001f)
    }
}
