package id.my.nanclouder.nanhistory.utils.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareCanvasModelTest {

    @Test
    fun clamped_restrictsCoordinatesWithinBounds() {
        val element = CanvasElement(
            type = CanvasElementType.TITLE,
            xPercent = 1.2f, // Out of bounds
            yPercent = -0.5f, // Negative
            widthPercent = 0.5f,
            heightPercent = 0.2f
        )

        val clamped = element.clamped()
        assertEquals(0.5f, clamped.xPercent, 0.001f) // Clamped to 1.0 - 0.5 = 0.5
        assertEquals(0.0f, clamped.yPercent, 0.001f) // Clamped to 0.0
    }

    @Test
    fun clamped_enforcesMinimumDimensions() {
        val tiny = CanvasElement(
            type = CanvasElementType.SINGLE_METRIC,
            xPercent = 0.1f,
            yPercent = 0.1f,
            widthPercent = 0.01f, // Below minimum 0.08f
            heightPercent = 0.005f // Below minimum 0.04f
        )

        val clamped = tiny.clamped()
        assertTrue(clamped.widthPercent >= 0.08f)
        assertTrue(clamped.heightPercent >= 0.04f)
    }

    @Test
    fun defaultPresets_existForAllAspectRatios() {
        for (aspect in ShareCardAspectRatio.values()) {
            val elements = ShareCanvasPresets.getDefaultElements(aspect)
            assertFalse(elements.isEmpty())
            assertTrue(elements.any { it.type == CanvasElementType.TITLE })
            assertTrue(elements.any { it.type == CanvasElementType.METRICS_GRID })
        }
    }
}
