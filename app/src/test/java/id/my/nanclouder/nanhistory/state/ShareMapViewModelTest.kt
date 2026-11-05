package id.my.nanclouder.nanhistory.state

import id.my.nanclouder.nanhistory.utils.share.CanvasElement
import id.my.nanclouder.nanhistory.utils.share.CanvasElementType
import id.my.nanclouder.nanhistory.utils.share.ElementStyle
import id.my.nanclouder.nanhistory.utils.share.ShareCanvasPresets
import id.my.nanclouder.nanhistory.utils.share.ShareCardAspectRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareMapViewModelTest {

    @Test
    fun canvasElement_clampedCoercesCoordinatesWithinBounds() {
        val outOfBounds = CanvasElement(
            id = "elem-1",
            type = CanvasElementType.TITLE,
            xPercent = 1.5f,
            yPercent = -0.5f,
            widthPercent = 2.0f,
            heightPercent = 0.01f
        )

        val clamped = outOfBounds.clamped()

        assertEquals(1.0f, clamped.widthPercent, 0.001f)
        assertEquals(0.04f, clamped.heightPercent, 0.001f)
        assertEquals(0.0f, clamped.xPercent, 0.001f)
        assertEquals(0.0f, clamped.yPercent, 0.001f)
    }

    @Test
    fun shareCanvasPresets_returnsNonEmptyForAspectRatios() {
        val squarePresets = ShareCanvasPresets.getDefaultElements(ShareCardAspectRatio.SQUARE)
        assertTrue(squarePresets.isNotEmpty())
        assertTrue(squarePresets.any { it.type == CanvasElementType.TITLE })
        assertTrue(squarePresets.any { it.type == CanvasElementType.METRICS_GRID })

        val storyPresets = ShareCanvasPresets.getDefaultElements(ShareCardAspectRatio.STORY)
        assertTrue(storyPresets.isNotEmpty())
        assertTrue(storyPresets.any { it.type == CanvasElementType.TITLE })
        assertTrue(storyPresets.any { it.type == CanvasElementType.METRICS_GRID })

        val landscapePresets = ShareCanvasPresets.getDefaultElements(ShareCardAspectRatio.LANDSCAPE)
        assertTrue(landscapePresets.isNotEmpty())
        assertTrue(landscapePresets.any { it.type == CanvasElementType.TITLE })
        assertTrue(landscapePresets.any { it.type == CanvasElementType.METRICS_GRID })
    }

    @Test
    fun shareMapUiState_undoRedoSimulation() {
        val undoStack = ArrayDeque<List<CanvasElement>>()
        val redoStack = ArrayDeque<List<CanvasElement>>()
        var currentState = ShareMapUiState(
            elements = ShareCanvasPresets.getDefaultElements(ShareCardAspectRatio.SQUARE)
        )

        val initialElements = currentState.elements

        // Mutation: Add element
        undoStack.addLast(currentState.elements)
        redoStack.clear()
        val newElem = CanvasElement(
            type = CanvasElementType.MARKDOWN_TEXT,
            xPercent = 0.1f,
            yPercent = 0.2f,
            widthPercent = 0.5f,
            heightPercent = 0.2f
        )
        currentState = currentState.copy(elements = currentState.elements + newElem)
        assertEquals(initialElements.size + 1, currentState.elements.size)
        assertTrue(undoStack.isNotEmpty())
        assertTrue(redoStack.isEmpty())

        // Undo
        redoStack.addLast(currentState.elements)
        val previous = undoStack.removeLast()
        currentState = currentState.copy(elements = previous)
        assertEquals(initialElements.size, currentState.elements.size)
        assertTrue(undoStack.isEmpty())
        assertTrue(redoStack.isNotEmpty())

        // Redo
        undoStack.addLast(currentState.elements)
        val next = redoStack.removeLast()
        currentState = currentState.copy(elements = next)
        assertEquals(initialElements.size + 1, currentState.elements.size)
        assertTrue(undoStack.isNotEmpty())
        assertTrue(redoStack.isEmpty())
    }

    @Test
    fun shareMapUiState_duplicateElementSimulation() {
        val target = CanvasElement(
            id = "orig-id",
            type = CanvasElementType.TITLE,
            xPercent = 0.1f,
            yPercent = 0.1f,
            widthPercent = 0.8f,
            heightPercent = 0.12f,
            zIndex = 1f
        )

        val duplicated = target.copy(
            id = java.util.UUID.randomUUID().toString(),
            xPercent = (target.xPercent + 0.05f).coerceIn(0f, 1f - target.widthPercent),
            yPercent = (target.yPercent + 0.05f).coerceIn(0f, 1f - target.heightPercent),
            zIndex = target.zIndex + 1f
        )

        assertNotEquals(target.id, duplicated.id)
        assertEquals(target.type, duplicated.type)
        assertEquals(0.15f, duplicated.xPercent, 0.001f)
        assertEquals(0.15f, duplicated.yPercent, 0.001f)
        assertEquals(2f, duplicated.zIndex, 0.001f)
    }

    @Test
    fun shareCardAspectRatio_ratiosAreAccurate() {
        assertEquals(1.0f, ShareCardAspectRatio.SQUARE.ratio, 0.001f)
        assertEquals(9f / 16f, ShareCardAspectRatio.STORY.ratio, 0.001f)
        assertEquals(16f / 9f, ShareCardAspectRatio.LANDSCAPE.ratio, 0.001f)
    }
}
