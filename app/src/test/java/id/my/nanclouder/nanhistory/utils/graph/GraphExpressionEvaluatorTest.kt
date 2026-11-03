package id.my.nanclouder.nanhistory.utils.graph

import id.my.nanclouder.nanhistory.utils.Coordinate
import id.my.nanclouder.nanhistory.utils.HistoryLocationData
import id.my.nanclouder.nanhistory.utils.history.LocationData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class GraphExpressionEvaluatorTest {

    private fun createSampleData(): List<HistoryLocationData> {
        val now = ZonedDateTime.now()
        val p1 = HistoryLocationData(
            speed = 10f,
            acceleration = 1f,
            distance = 15f,
            points = listOf(Coordinate(10.0, 20.0), Coordinate(10.001, 20.001)),
            start = now.minusSeconds(10),
            end = now.minusSeconds(8),
            locationData = listOf(
                LocationData(
                    time = now.minusSeconds(8),
                    location = Coordinate(10.001, 20.001),
                    speed = 10f,
                    altitude = 100.0,
                    bearing = 90f
                )
            )
        )
        val p2 = HistoryLocationData(
            speed = 25f,
            acceleration = 7.5f,
            distance = 30f,
            points = listOf(Coordinate(10.001, 20.001), Coordinate(10.003, 20.003)),
            start = now.minusSeconds(8),
            end = now.minusSeconds(6),
            locationData = listOf(
                LocationData(
                    time = now.minusSeconds(6),
                    location = Coordinate(10.003, 20.003),
                    speed = 25f,
                    altitude = 140.0,
                    bearing = 95f
                )
            )
        )
        val p3 = HistoryLocationData(
            speed = 40f,
            acceleration = 7.5f,
            distance = 50f,
            points = listOf(Coordinate(10.003, 20.003), Coordinate(10.006, 20.006)),
            start = now.minusSeconds(6),
            end = now.minusSeconds(4),
            locationData = listOf(
                LocationData(
                    time = now.minusSeconds(4),
                    location = Coordinate(10.006, 20.006),
                    speed = 40f,
                    altitude = 180.0,
                    bearing = 100f
                )
            )
        )
        return listOf(p1, p2, p3)
    }

    @Test
    fun testBasicMathEvaluation() {
        val data = createSampleData()
        val res = GraphExpressionEvaluator.evaluate("10 + 5 * 2", data)
        assertEquals(3, res.size)
        assertEquals(20f, res[0], 0.001f)
    }

    @Test
    fun testPropertyExtraction() {
        val data = createSampleData()
        val speeds = GraphExpressionEvaluator.evaluate("speed", data)
        assertEquals(listOf(10f, 25f, 40f), speeds)

        val altitudes = GraphExpressionEvaluator.evaluate("altitude", data)
        assertEquals(listOf(100f, 140f, 180f), altitudes)
    }

    @Test
    fun testSiblingComparisons() {
        val data = createSampleData()
        // speed - speed[-1]
        // Index 0: 10 - 10 = 0 (boundary clamps to 0)
        // Index 1: 25 - 10 = 15
        // Index 2: 40 - 25 = 15
        val diffs = GraphExpressionEvaluator.evaluate("speed - speed[-1]", data)
        assertEquals(0f, diffs[0], 0.001f)
        assertEquals(15f, diffs[1], 0.001f)
        assertEquals(15f, diffs[2], 0.001f)
    }

    @Test
    fun testSiblingAheadComparison() {
        val data = createSampleData()
        // altitude[+1] - altitude
        // Index 0: 140 - 100 = 40
        // Index 1: 180 - 140 = 40
        // Index 2: 180 - 180 = 0 (clamps to end)
        val diffs = GraphExpressionEvaluator.evaluate("altitude[+1] - altitude", data)
        assertEquals(40f, diffs[0], 0.001f)
        assertEquals(40f, diffs[1], 0.001f)
        assertEquals(0f, diffs[2], 0.001f)
    }

    @Test
    fun testMathFunctions() {
        val data = createSampleData()
        val absVal = GraphExpressionEvaluator.evaluate("abs(-42)", data)
        assertEquals(42f, absVal[0], 0.001f)

        val sqrtVal = GraphExpressionEvaluator.evaluate("sqrt(100)", data)
        assertEquals(10f, sqrtVal[0], 0.001f)

        val maxVal = GraphExpressionEvaluator.evaluate("max(speed, 30)", data)
        assertEquals(30f, maxVal[0], 0.001f) // max(10, 30) = 30
        assertEquals(30f, maxVal[1], 0.001f) // max(25, 30) = 30
        assertEquals(40f, maxVal[2], 0.001f) // max(40, 30) = 40
    }

    @Test
    fun testDynamicPropertyFunction() {
        val data = createSampleData()
        val result = GraphExpressionEvaluator.evaluate("property(\"speed\")", data)
        assertEquals(listOf(10f, 25f, 40f), result)
    }

    @Test
    fun testMovingAverageOperator() {
        val data = createSampleData()
        // speeds: [10, 25, 40]

        // 1. Centered window: avg(speed, 3)
        // idx 0: (10 + 25) / 2 = 17.5
        // idx 1: (10 + 25 + 40) / 3 = 25.0
        // idx 2: (25 + 40) / 2 = 32.5
        val centered = GraphExpressionEvaluator.evaluate("avg(speed, 3)", data)
        assertEquals(3, centered.size)
        assertEquals(17.5f, centered[0], 0.001f)
        assertEquals(25.0f, centered[1], 0.001f)
        assertEquals(32.5f, centered[2], 0.001f)

        // 2. Trailing window: avg(speed, 2, 0)
        // idx 0: 10 / 1 = 10.0
        // idx 1: (10 + 25) / 2 = 17.5
        // idx 2: (10 + 25 + 40) / 3 = 25.0
        val trailing = GraphExpressionEvaluator.evaluate("avg(speed, 2, 0)", data)
        assertEquals(10.0f, trailing[0], 0.001f)
        assertEquals(17.5f, trailing[1], 0.001f)
        assertEquals(25.0f, trailing[2], 0.001f)

        // 3. Aliases smooth and mean
        val smoothed = GraphExpressionEvaluator.evaluate("smooth(speed, 3)", data)
        assertEquals(centered, smoothed)
    }

    @Test
    fun testValidation() {
        assertTrue(GraphExpressionEvaluator.validate("speed * 2 + altitude").isSuccess)
        assertTrue(GraphExpressionEvaluator.validate("speed - speed[-1]").isSuccess)
        assertTrue(GraphExpressionEvaluator.validate("avg(speed, 5)").isSuccess)
        assertTrue(GraphExpressionEvaluator.validate("avg(speed, 3, 0)").isSuccess)
        assertFalse(GraphExpressionEvaluator.validate("speed +").isSuccess)
        assertFalse(GraphExpressionEvaluator.validate("").isSuccess)
        assertFalse(GraphExpressionEvaluator.validate("unknownFunc(10)").isSuccess)
    }
}
