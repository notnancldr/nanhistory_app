package id.my.nanclouder.nanhistory.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

@Composable
fun LineChart(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 300.dp,
    width: Dp = Dp.Unspecified,
    strokeWidth: Dp = 2.dp,
    showGrid: Boolean = true,
    showAxis: Boolean = true,
    label: String = "",

    gridColor: Color = Color.LightGray.copy(alpha = 0.5f),
    axisColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueLabels: List<String>? = null,
    showValueLabels: Boolean = true,
    valueLabelColor: Color = color
) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    if (values.isEmpty()) {
        Text("No data available")
        return
    }

    val maxValue = ceil(values.maxOrNull() ?: 0f).toInt()
    val minValue = floor(values.minOrNull() ?: 0f).toInt()
    val range = if (maxValue > minValue) maxValue - minValue else 1

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        val labelHeight = 40f
        val canvasWidth = size.width
        val canvasHeight = size.height
        val padding = 15.dp.toPx()
        val pointSpacing = (canvasWidth - 2 * padding) / (values.size - 1).coerceAtLeast(1)
        val drawableHeight = canvasHeight - labelHeight
        val yAxisLabelWidth = 40f

        // Draw grid aligned with actual values
        if (showGrid) {
            val gridLines = 4
            repeat(gridLines + 1) { i ->
                val gridValue = minValue + (i * range / gridLines)
                val normalizedValue = (gridValue - minValue) / range.toFloat()
                val y = drawableHeight - (normalizedValue * (drawableHeight - padding))

                drawLine(
                    color = gridColor,
                    start = Offset(yAxisLabelWidth, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1f
                )

                // Draw y-axis value labels
                drawContext.canvas.nativeCanvas.apply {
                    val textPaint = Paint().apply {
                        this.color = axisColor.toArgb()
                        textSize = 28f
                        textAlign = Paint.Align.RIGHT
                    }
                    drawText(
                        gridValue.toString(),
                        yAxisLabelWidth - 8f,
                        y + 10f,
                        textPaint
                    )
                }
            }
        }

        // Draw axes
        if (showAxis) {
            // X-axis
            drawLine(
                color = axisColor,
                start = Offset(yAxisLabelWidth, drawableHeight),
                end = Offset(canvasWidth, drawableHeight),
                strokeWidth = 2f
            )
            // Y-axis
            drawLine(
                color = axisColor,
                start = Offset(yAxisLabelWidth, 0f),
                end = Offset(yAxisLabelWidth, drawableHeight),
                strokeWidth = 2f
            )
        }

        // Draw line and points
        val points = values.mapIndexed { index, value ->
            val x = yAxisLabelWidth + padding + index * pointSpacing
            val normalizedValue = (value - minValue) / range.toFloat()
            val y = drawableHeight - (normalizedValue * (drawableHeight - padding))
            Offset(x, y)
        }

        // Draw connected line
        for (i in 0 until points.size - 1) {
            drawLine(
                color = color,
                start = points[i],
                end = points[i + 1],
                strokeWidth = strokeWidth.toPx()
            )
        }

        // Draw points and labels
        points.forEachIndexed { index, point ->
            drawCircle(
                color = color,
                radius = (strokeWidth.toPx() * 2.5f),
                center = point
            )
            drawCircle(
                color = backgroundColor,
                radius = (strokeWidth.toPx()),
                center = point
            )

            // Draw value label below x-axis
            if (showValueLabels) {
                val labelText = valueLabels?.getOrNull(index) ?: values[index].toString()
                drawContext.canvas.nativeCanvas.apply {
                    val textPaint = Paint().apply {
                        this.color = valueLabelColor.toArgb()
                        textSize = 32f
                        textAlign = Paint.Align.CENTER
                    }
                    drawText(
                        labelText,
                        point.x,
                        drawableHeight + 40f,
                        textPaint
                    )
                }
            }
        }
    }

    if (label.isNotEmpty()) {
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}