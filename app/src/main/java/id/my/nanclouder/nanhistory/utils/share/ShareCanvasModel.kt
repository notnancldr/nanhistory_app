package id.my.nanclouder.nanhistory.utils.share

import java.util.UUID

/**
 * Supported element types in the customizable ShareMap canvas.
 */
enum class CanvasElementType {
    TITLE,
    METRICS_GRID,
    SINGLE_METRIC,
    FLOATING_MAP,
    MARKDOWN_TEXT,
    TAGS
}

/**
 * Visual styling options for an individual canvas element.
 */
data class ElementStyle(
    val backgroundColorHex: String = "#26000000",
    val cornerRadiusDp: Float = 12f,
    val paddingDp: Float = 8f,
    val borderWidthDp: Float = 0f,
    val borderColorHex: String = "#40FFFFFF",
    val textColorHex: String = "#FFFFFF",
    val fontSizeSp: Float = 14f,
    val hasShadow: Boolean = true,
    val templateFormat: String = "",
    val rawCustomProperties: String = "",
    val mapTheme: String = "SYSTEM"
)

/**
 * Movable, resizable, and stylable element positioned via normalized percentage coordinates (0..1).
 */
data class CanvasElement(
    val id: String = UUID.randomUUID().toString(),
    val type: CanvasElementType,
    val xPercent: Float,
    val yPercent: Float,
    val widthPercent: Float,
    val heightPercent: Float,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    val zIndex: Float = 0f,
    val style: ElementStyle = ElementStyle()
) {
    fun clamped(): CanvasElement {
        val w = widthPercent.coerceIn(0.08f, 1f)
        val h = heightPercent.coerceIn(0.04f, 1f)
        val x = xPercent.coerceIn(0f, 1f - w)
        val y = yPercent.coerceIn(0f, 1f - h)
        return copy(xPercent = x, yPercent = y, widthPercent = w, heightPercent = h)
    }
}

/**
 * Supported canvas aspect ratios for export and live editing.
 */
enum class ShareCardAspectRatio(val label: String, val ratio: Float) {
    SQUARE("1:1", 1.0f),
    STORY("9:16", 9f / 16f),
    LANDSCAPE("16:9", 16f / 9f)
}

/**
 * Factory for initial layout presets.
 */
object ShareCanvasPresets {

    fun getDefaultElements(aspectRatio: ShareCardAspectRatio): List<CanvasElement> {
        return when (aspectRatio) {
            ShareCardAspectRatio.SQUARE -> listOf(
                CanvasElement(
                    type = CanvasElementType.TITLE,
                    xPercent = 0.05f,
                    yPercent = 0.05f,
                    widthPercent = 0.9f,
                    heightPercent = 0.16f,
                    style = ElementStyle(
                        backgroundColorHex = "#66000000",
                        fontSizeSp = 18f,
                        templateFormat = "# {title}\n{date}"
                    )
                ),
                CanvasElement(
                    type = CanvasElementType.METRICS_GRID,
                    xPercent = 0.05f,
                    yPercent = 0.72f,
                    widthPercent = 0.9f,
                    heightPercent = 0.22f,
                    style = ElementStyle(
                        backgroundColorHex = "#80000000",
                        fontSizeSp = 13f
                    )
                )
            )
            ShareCardAspectRatio.STORY -> listOf(
                CanvasElement(
                    type = CanvasElementType.TITLE,
                    xPercent = 0.06f,
                    yPercent = 0.08f,
                    widthPercent = 0.88f,
                    heightPercent = 0.14f,
                    style = ElementStyle(
                        backgroundColorHex = "#66000000",
                        fontSizeSp = 20f,
                        templateFormat = "# {title}\n{date}"
                    )
                ),
                CanvasElement(
                    type = CanvasElementType.METRICS_GRID,
                    xPercent = 0.06f,
                    yPercent = 0.76f,
                    widthPercent = 0.88f,
                    heightPercent = 0.18f,
                    style = ElementStyle(
                        backgroundColorHex = "#80000000",
                        fontSizeSp = 14f
                    )
                )
            )
            ShareCardAspectRatio.LANDSCAPE -> listOf(
                CanvasElement(
                    type = CanvasElementType.TITLE,
                    xPercent = 0.04f,
                    yPercent = 0.06f,
                    widthPercent = 0.45f,
                    heightPercent = 0.22f,
                    style = ElementStyle(
                        backgroundColorHex = "#66000000",
                        fontSizeSp = 16f,
                        templateFormat = "# {title}\n{date}"
                    )
                ),
                CanvasElement(
                    type = CanvasElementType.METRICS_GRID,
                    xPercent = 0.04f,
                    yPercent = 0.35f,
                    widthPercent = 0.45f,
                    heightPercent = 0.55f,
                    style = ElementStyle(
                        backgroundColorHex = "#80000000",
                        fontSizeSp = 12f
                    )
                )
            )
        }
    }
}
