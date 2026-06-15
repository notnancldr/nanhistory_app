package id.my.nanclouder.nanhistory.ui

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
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

@Composable
fun FilledLineChart(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    unit: String? = null,
    height: Dp = 300.dp,
    strokeWidth: Dp = 2.dp,
    showGrid: Boolean = true,
    showAxis: Boolean = true,
    label: String = "",
    data: List<List<TooltipData>> = emptyList(),
    // Map of value → Color for gradient stops; null = flat color
    // e.g. mapOf(0f to Color.Blue, 50f to Color.Green, 100f to Color.Red)
    gradientColors: Map<Float, Color>? = null,
    strokeGradientColors: Map<Float, Color>? = null,
    gridColor: Color = Color.LightGray.copy(alpha = 0.5f),
    axisColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueLabels: List<String>? = null,
    showValueLabels: Boolean = true,
    valueLabelColor: Color = color,
    tooltipBackgroundColor: Color = Color.Black.copy(alpha = 0.75f),
    tooltipTextColor: Color = Color.White,
) {
    val backgroundColor = MaterialTheme.colorScheme.surface

    if (values.isEmpty()) {
        Text("No data available")
        return
    }

    val maxValue = ceil(values.maxOrNull() ?: 0f).toInt()
    val minValue = floor(values.minOrNull() ?: 0f).toInt()
    val range = if (maxValue > minValue) maxValue - minValue else 1

    var activeIndex by remember { mutableIntStateOf(-1) }
    val animatedX = remember { Animatable(-1f) }
    val animatedY = remember { Animatable(-1f) }
    val scope = rememberCoroutineScope()

    // Padding constants — must match what we apply inside DrawScope
    val padTopPx = 16.dp
    val padLeftPx = 16.dp
    val yAxisLabelWidthDp = 8.dp

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(backgroundColor)
            .pointerInput(values.size) {
                // Gesture coords are in raw canvas pixels (no padding applied)
                val pad = padLeftPx.toPx()
                val yAxisW = yAxisLabelWidthDp.toPx()
                val drawW = size.width - pad * 2   // drawable width inside padding
                val drawH = size.height - pad * 2  // drawable height inside padding
                val labelH = 40f
                val spacing = drawW / (values.size - 1).coerceAtLeast(1)

                fun snap(idx: Int): Pair<Float, Float> {
                    val x = pad + yAxisW + idx * spacing
                    val norm = (values[idx] - minValue) / range.toFloat()
                    val y = pad + (drawH - labelH) - norm * (drawH - labelH)
                    return x to y
                }

                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        // Subtract padding + yAxis offset before resolving index
                        val idx = resolveIndex(offset.x - pad - yAxisW, values.size, drawW - yAxisW)
                        activeIndex = idx
                        val (sx, sy) = snap(idx)
                        scope.launch { animatedX.snapTo(sx) }
                        scope.launch { animatedY.snapTo(sy) }
                    },
                    onHorizontalDrag = { change, _ ->
                        val idx = resolveIndex(change.position.x - pad - yAxisW, values.size, drawW - yAxisW)
                        if (idx != activeIndex) {
                            activeIndex = idx
                            val (tx, ty) = snap(idx)
                            scope.launch {
                                launch { animatedX.animateTo(tx, spring(stiffness = Spring.StiffnessMediumLow)) }
                                launch { animatedY.animateTo(ty, spring(stiffness = Spring.StiffnessMediumLow)) }
                            }
                        }
                    },
                    onDragEnd = { activeIndex = -1 },
                    onDragCancel = { activeIndex = -1 }
                )
            }
    ) {
        val padLeft = padLeftPx.toPx()
        val padTop = padTopPx.toPx()
        val yAxisLabelWidth = yAxisLabelWidthDp.toPx()
        val labelHeight = 40f
        val canvasWidth = size.width - padLeft * 2
        val canvasHeight = size.height - padLeft * 2
        val drawableHeight = canvasHeight - labelHeight
        val pointSpacing = canvasWidth / (values.size - 1).coerceAtLeast(1)

        // All drawing is offset by padding so coordinates are consistent with gestures
        translate(left = padLeft, top = padTop) {

            // ── Grid ──────────────────────────────────────────────────────────
            if (showGrid) {
                val gridLines = 4
                repeat(gridLines + 1) { i ->
                    val gridValue = minValue + (i * range / gridLines)
                    val norm = (gridValue - minValue) / range.toFloat()
                    val y = drawableHeight - norm * drawableHeight
                    drawLine(color = gridColor, start = Offset(yAxisLabelWidth, y), end = Offset(canvasWidth, y), strokeWidth = 1f)
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            gridValue.toString(),
                            yAxisLabelWidth - 8f,
                            y + 10f,
                            Paint().apply {
                                this.color = axisColor.toArgb()
                                textSize = 28f
                                textAlign = Paint.Align.RIGHT
                            }
                        )
                    }
                }
            }

            // ── Axes ──────────────────────────────────────────────────────────
            if (showAxis) {
                drawLine(color = axisColor, start = Offset(yAxisLabelWidth, drawableHeight), end = Offset(canvasWidth, drawableHeight), strokeWidth = 2f)
                drawLine(color = axisColor, start = Offset(yAxisLabelWidth, 0f), end = Offset(yAxisLabelWidth, drawableHeight), strokeWidth = 2f)
            }

            // ── Points ────────────────────────────────────────────────────────
            val points = values.mapIndexed { idx, value ->
                val x = yAxisLabelWidth + idx * pointSpacing
                val norm = (value - minValue) / range.toFloat()
                Offset(x, drawableHeight - norm * drawableHeight)
            }

            // ── Paths ─────────────────────────────────────────────────────────
            val linePath = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
            }
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(points.last().x, drawableHeight)
                lineTo(points.first().x, drawableHeight)
                close()
            }

            // ── Resolve brushes ───────────────────────────────────────────────────
            // Convert value→Color map into Brush stop positions (0f=bottom, 1f=top).
            // Stop position = (value - min) / range, sorted ascending for the Brush API.
            fun buildColorStops(alpha: Float = 0.45f): Array<Pair<Float, Color>> {
                if (gradientColors.isNullOrEmpty()) {
                    return arrayOf(0f to color.copy(alpha = alpha), 1f to color.copy(alpha = alpha))
                }
                return gradientColors.entries
                    .sortedBy { it.key }
                    .map { (value, c) ->
                        val stop = ((value - minValue) / range.toFloat()).coerceIn(0f, 1f)
                        stop to c.copy(alpha = alpha)
                    }
                    .toTypedArray()
            }

            fun buildStrokeColorStops(alpha: Float = 1f): Array<Pair<Float, Color>> {
                if (strokeGradientColors.isNullOrEmpty()) {
                    if (!gradientColors.isNullOrEmpty()) {
                        buildColorStops()
                    }
                    return arrayOf(0f to color.copy(alpha = alpha), 1f to color.copy(alpha = alpha))
                }
                return strokeGradientColors.entries
                    .sortedBy { it.key }
                    .map { (value, c) ->
                        val stop = ((value - minValue) / range.toFloat()).coerceIn(0f, 1f)
                        stop to c.copy(alpha = alpha)
                    }
                    .toTypedArray()
            }

            val strokeBrush: Brush = Brush.verticalGradient(
                colorStops = buildStrokeColorStops(),
                startY = drawableHeight, // 0f stop = minValue = bottom
                endY = 0f               // 1f stop = maxValue = top
            )
            val fillBrush: Brush = Brush.verticalGradient(
                colorStops = buildColorStops(),
                startY = drawableHeight,
                endY = 0f
            )

            drawPath(fillPath, fillBrush)
            drawPath(linePath, strokeBrush, style = Stroke(width = strokeWidth.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round))

            // ── Data point dots ───────────────────────────────────────────────
            points.forEachIndexed { idx, point ->
                if (showValueLabels) {
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            valueLabels?.getOrNull(idx) ?: values[idx].toString(),
                            point.x,
                            drawableHeight + 40f,
                            Paint().apply {
                                this.color = valueLabelColor.toArgb()
                                textSize = 32f
                                textAlign = Paint.Align.CENTER
                            }
                        )
                    }
                }
            }

            // ── Animated tooltip ──────────────────────────────────────────────
            if (activeIndex >= 0 && activeIndex < points.size) {
                // animatedX/Y are in raw canvas coords; subtract pad to get translated coords
                val ax = animatedX.value - padLeft
                val ay = animatedY.value - padLeft

                drawLine(
                    color = color.copy(alpha = 0.4f),
                    start = Offset(ax, 0f),
                    end = Offset(ax, drawableHeight),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                )
                drawCircle(color = Color.White, radius = strokeWidth.toPx() * 3f, center = Offset(ax, ay))
                drawCircle(brush = strokeBrush, radius = strokeWidth.toPx() * 2f, center = Offset(ax, ay))

                drawTooltip(
                    scope = this,
                    point = Offset(ax, ay),
                    data = listOf(
                        TooltipData(
                            value = values[activeIndex],
                            rounding = 2,
                            unit = unit,
                        ),
                        *(data.getOrNull(activeIndex) ?: emptyList()).toTypedArray(),
                    ),

                    canvasWidth = canvasWidth,
                    yAxisLabelWidth = yAxisLabelWidth,
                    bgColor = tooltipBackgroundColor,
                    textColor = tooltipTextColor,
                )
            }
        } // end translate
    }

    if (label.isNotEmpty()) {
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun resolveIndex(touchX: Float, count: Int, drawWidth: Float): Int {
    val spacing = drawWidth / (count - 1).coerceAtLeast(1)
    return (0 until count)
        .minByOrNull { abs(touchX - it * spacing) }
        ?.coerceIn(0, count - 1) ?: 0
}

data class TooltipData(
    val value: Any,
    val rounding: Int? = null,
    val label: String? = null,
    val unit: String? = null,
)

private fun drawTooltip(
    scope: DrawScope,
    point: Offset,
    data: List<TooltipData>,
    canvasWidth: Float,
    yAxisLabelWidth: Float,
    bgColor: Color,
    textColor: Color,
) {
    val fontSize = 34f
    val paint = Paint().apply {
        this.color = textColor.toArgb()
        textSize = fontSize
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
    }

    with(scope) {
        // 1. Build a list of individual lines instead of one giant string
        val lines = data.map { entry ->
            val value = entry.value.let {
                if (it is Float && entry.rounding != null) {
                    round(it * 10f.pow(entry.rounding)) / 10f.pow(entry.rounding)
                } else if (it is Double && entry.rounding != null) {
                    round(it * 10f.pow(entry.rounding)) / 10f.pow(entry.rounding)
                } else it
            }.toString()

            val labelPrefix = entry.label?.let { "$it: " } ?: ""
            val unitSuffix = entry.unit?.let { " $it" } ?: ""

            "$labelPrefix$value$unitSuffix"
        }

        if (lines.isEmpty()) return

        // 2. Measure bounds accurately
        // Find the widest line to determine tooltip width
        val maxTextW = lines.maxOf { paint.measureText(it) }
        val padH = 14f
        val lineSpacing = 10f // Space between text lines

        val tipW = maxTextW + (padH * 2)
        // Total height = (font size * total lines) + spacing between lines + vertical padding
        val tipH = (fontSize * lines.size) + (lineSpacing * (lines.size - 1)) + (padH * 2)

        // 3. Position clamping
        val rawX = point.x - tipW / 2f
        val clampedX = rawX.coerceIn(yAxisLabelWidth, canvasWidth - tipW)

        val topY = point.y - tipH - 20f
        val bottomY = point.y + 20f

        val clampedY = if (topY >= 20f) topY else bottomY

        // 4. Draw background
        drawRoundRect(
            color = bgColor,
            topLeft = Offset(clampedX, clampedY),
            size = Size(tipW, tipH),
            cornerRadius = CornerRadius(8f)
        )

        // 5. Draw each line of text with manual Y offsetting
        drawContext.canvas.nativeCanvas.apply {
            lines.forEachIndexed { index, line ->
                // Calculate the baseline for the current line
                val textY = clampedY + padH + fontSize + (index * (fontSize + lineSpacing)) - 4f
                drawText(line, clampedX + padH, textY, paint)
            }
        }
    }
}