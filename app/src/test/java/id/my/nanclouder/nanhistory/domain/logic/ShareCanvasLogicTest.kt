package id.my.nanclouder.nanhistory.domain.logic

import id.my.nanclouder.nanhistory.domain.model.Coordinate
import id.my.nanclouder.nanhistory.domain.model.EventRange
import id.my.nanclouder.nanhistory.domain.model.LocationData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ShareCanvasLogicTest {

    @Test
    fun calculateDragPosition_clampsWithinCanvasBounds() {
        // Dragging out of bounds to the left/top
        val (x1, y1) = ShareCanvasLogic.calculateDragPosition(
            xPercent = 0.1f,
            yPercent = 0.1f,
            widthPercent = 0.2f,
            heightPercent = 0.2f,
            deltaXPx = -200f,
            deltaYPx = -200f,
            canvasWidthPx = 1000f,
            canvasHeightPx = 1000f
        )
        assertEquals(0f, x1, 0.001f)
        assertEquals(0f, y1, 0.001f)

        // Dragging out of bounds to the right/bottom
        val (x2, y2) = ShareCanvasLogic.calculateDragPosition(
            xPercent = 0.7f,
            yPercent = 0.7f,
            widthPercent = 0.2f,
            heightPercent = 0.2f,
            deltaXPx = 500f,
            deltaYPx = 500f,
            canvasWidthPx = 1000f,
            canvasHeightPx = 1000f
        )
        assertEquals(0.8f, x2, 0.001f)
        assertEquals(0.8f, y2, 0.001f)
    }

    @Test
    fun calculateResize_clampsWithinMinAndMax() {
        val (w, h) = ShareCanvasLogic.calculateResize(
            widthPercent = 0.2f,
            heightPercent = 0.2f,
            xPercent = 0.8f,
            yPercent = 0.8f,
            deltaXPx = 300f,
            deltaYPx = 300f,
            canvasWidthPx = 1000f,
            canvasHeightPx = 1000f
        )
        // 1f - 0.8f = 0.2f max
        assertEquals(0.2f, w, 0.001f)
        assertEquals(0.2f, h, 0.001f)
    }

    @Test
    fun calculateElevationGain_sumsAscents() {
        val time = ZonedDateTime.of(2026, 5, 1, 10, 0, 0, 0, ZoneId.of("UTC"))
        val c = Coordinate(0.0, 0.0)
        val locs = listOf(
            LocationData(location = c, time = time, altitude = 100.0),
            LocationData(location = c, time = time, altitude = 160.0),
            LocationData(location = c, time = time, altitude = 140.0),
            LocationData(location = c, time = time, altitude = 190.0)
        )
        // +60 + 50 = 110.0
        val gain = ShareCanvasLogic.calculateElevationGain(locs)
        assertEquals(110.0, gain!!, 0.001)
    }

    @Test
    fun resolveTokens_replacesStandardPlaceholders() {
        val start = ZonedDateTime.of(2026, 5, 1, 8, 30, 0, 0, ZoneId.of("UTC"))
        val end = ZonedDateTime.of(2026, 5, 1, 11, 45, 0, 0, ZoneId.of("UTC"))

        val event = EventRange(
            title = "Morning Ride",
            description = "Coast",
            time = start,
            end = end
        )

        val template = "{title} - {distance} in {duration}"
        val metrics = mapOf("distance" to "42.5 km", "duration" to "1h 30m")
        val resolved = ShareCanvasLogic.resolveTokens(
            template = template,
            eventTitle = event.title,
            dateString = "May 01",
            eventData = event,
            metricsText = metrics
        )

        assertEquals("Morning Ride - 42.5 km in 1h 30m", resolved)
    }
}
