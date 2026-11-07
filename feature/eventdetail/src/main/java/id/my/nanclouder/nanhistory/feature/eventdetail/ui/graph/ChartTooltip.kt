package id.my.nanclouder.nanhistory.feature.eventdetail.ui.graph

import android.graphics.Paint
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.round

/**
 * Renderable series data for the multi-series chart.
 */
data class RenderedSeries(
    val name: String,
    val values: List<Float>,
    val unit: String = "",
    val colorStops: Map<Float, Color> = emptyMap(),
    val fallbackColor: Color = Color(0xFF00AAFF)
)

/**
 * Helper to measure and draw scrubbed tooltip displaying all active series values and timestamps.
 */
fun drawMultiSeriesTooltip(
    scope: DrawScope,
    scrubX: Float,
    activeIndex: Int,
    seriesList: List<RenderedSeries>,
    timestamps: List<String>,
    drawW: Float,
    bgColor: Color,
    textColor: Color
) {
    val titleText = timestamps.getOrNull(activeIndex)
    val lines = mutableListOf<String>()
    val dotColors = mutableListOf<Color>()

    if (!titleText.isNullOrBlank()) {
        lines.add(titleText)
        dotColors.add(Color.Transparent)
    }

    seriesList.forEach { s ->
        val v = s.values.getOrNull(activeIndex)
        if (v != null) {
            val rounded = round(v * 10f) / 10f
            val u = if (s.unit.isNotBlank()) " ${s.unit}" else ""
            lines.add("${s.name}: $rounded$u")
            dotColors.add(s.colorStops.values.lastOrNull() ?: s.fallbackColor)
        }
    }

    if (lines.isEmpty()) return

    val paint = Paint().apply {
        color = textColor.toArgb()
        textSize = 28f
        isAntiAlias = true
    }

    val maxTextW = lines.maxOf { paint.measureText(it) }
    val padH = 16f
    val lineSpacing = 12f
    val tipW = maxTextW + padH * 2 + 20f
    val tipH = lines.size * 28f + (lines.size - 1) * lineSpacing + padH * 2

    val clampedX = (scrubX - tipW / 2f).coerceIn(0f, drawW - tipW)
    val clampedY = 10f

    with(scope) {
        val dotRadiusPx = 4.dp.toPx()

        drawRoundRect(
            color = bgColor,
            topLeft = Offset(clampedX, clampedY),
            size = Size(tipW, tipH),
            cornerRadius = CornerRadius(12f)
        )

        drawContext.canvas.nativeCanvas.apply {
            lines.forEachIndexed { idx, line ->
                val y = clampedY + padH + 24f + idx * (28f + lineSpacing)
                val dotColor = dotColors[idx]
                if (dotColor != Color.Transparent) {
                    drawCircle(
                        color = dotColor,
                        radius = dotRadiusPx,
                        center = Offset(clampedX + padH + 4f, y - 8f)
                    )
                    drawText(line, clampedX + padH + 18f, y, paint)
                } else {
                    drawText(line, clampedX + padH, y, paint)
                }
            }
        }
    }
}
