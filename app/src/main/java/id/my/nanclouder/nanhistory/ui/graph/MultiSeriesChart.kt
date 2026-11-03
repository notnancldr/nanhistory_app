package id.my.nanclouder.nanhistory.ui.graph

import android.content.res.Configuration
import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.utils.graph.GraphScaleMode
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

@Composable
fun MultiSeriesChart(
    seriesList: List<RenderedSeries>,
    modifier: Modifier = Modifier,
    scaleMode: GraphScaleMode = GraphScaleMode.NORMALIZED,
    timestamps: List<String> = emptyList(),
    height: Dp = 240.dp,
    showGrid: Boolean = true,
    showAxis: Boolean = true
) {
    if (seriesList.isEmpty() || seriesList.all { it.values.isEmpty() }) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(16.dp)
        ) {
            Text(
                text = "No data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val pointCount = seriesList.maxOf { it.values.size }
    if (pointCount < 2) return

    val backgroundColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tooltipBgColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f)
    val tooltipTextColor = MaterialTheme.colorScheme.onSurface

    var activeIndex by remember { mutableIntStateOf(-1) }
    val animatedX = remember { Animatable(-1f) }
    val scope = rememberCoroutineScope()

    val padTop = 16.dp
    val padBottom = 24.dp
    val padLeft = 36.dp
    val padRight = if (scaleMode == GraphScaleMode.DUAL_AXIS && seriesList.size > 1) 36.dp else 16.dp

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(backgroundColor)
            .pointerInput(pointCount) {
                val padLeftPx = padLeft.toPx()
                val padRightPx = padRight.toPx()
                val drawW = size.width - padLeftPx - padRightPx
                val spacing = drawW / (pointCount - 1).coerceAtLeast(1)

                fun resolve(touchX: Float): Int {
                    val relativeX = touchX - padLeftPx
                    return (0 until pointCount)
                        .minByOrNull { abs(relativeX - it * spacing) }
                        ?.coerceIn(0, pointCount - 1) ?: 0
                }

                detectTapGestures(
                    onPress = { offset ->
                        activeIndex = resolve(offset.x)
                        scope.launch { animatedX.snapTo(offset.x) }
                    }
                )
            }
            .pointerInput(pointCount) {
                val padLeftPx = padLeft.toPx()
                val padRightPx = padRight.toPx()
                val drawW = size.width - padLeftPx - padRightPx
                val spacing = drawW / (pointCount - 1).coerceAtLeast(1)

                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val idx = ((offset.x - padLeftPx) / spacing).toInt().coerceIn(0, pointCount - 1)
                        activeIndex = idx
                        val targetX = padLeftPx + idx * spacing
                        scope.launch { animatedX.snapTo(targetX) }
                    },
                    onHorizontalDrag = { change, _ ->
                        val idx = ((change.position.x - padLeftPx) / spacing).toInt().coerceIn(0, pointCount - 1)
                        if (idx != activeIndex) {
                            activeIndex = idx
                            val targetX = padLeftPx + idx * spacing
                            scope.launch {
                                animatedX.animateTo(targetX, spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        }
                    },
                    onDragEnd = { activeIndex = -1 },
                    onDragCancel = { activeIndex = -1 }
                )
            }
    ) {
        val padLeftPx = padLeft.toPx()
        val padRightPx = padRight.toPx()
        val padTopPx = padTop.toPx()
        val padBottomPx = padBottom.toPx()

        val drawW = size.width - padLeftPx - padRightPx
        val drawH = size.height - padTopPx - padBottomPx
        val pointSpacing = drawW / (pointCount - 1).coerceAtLeast(1)

        translate(left = padLeftPx, top = padTopPx) {
            // Draw 4 horizontal grid lines
            if (showGrid) {
                val gridLines = 4
                repeat(gridLines + 1) { i ->
                    val y = drawH * (1f - i / gridLines.toFloat())
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(drawW, y),
                        strokeWidth = 1f
                    )
                }
            }

            // Draw primary Y-axis text labels based on scale mode
            drawYAxisLabels(
                scope = this,
                seriesList = seriesList,
                scaleMode = scaleMode,
                drawW = drawW,
                drawH = drawH,
                axisColor = axisColor
            )

            // Draw Axes
            if (showAxis) {
                drawLine(color = axisColor, start = Offset(0f, drawH), end = Offset(drawW, drawH), strokeWidth = 1.5f)
                drawLine(color = axisColor, start = Offset(0f, 0f), end = Offset(0f, drawH), strokeWidth = 1.5f)
                if (scaleMode == GraphScaleMode.DUAL_AXIS && seriesList.size > 1) {
                    drawLine(color = axisColor, start = Offset(drawW, 0f), end = Offset(drawW, drawH), strokeWidth = 1.5f)
                }
            }

            // Draw Each Series Line & Gradient Fill
            seriesList.forEachIndexed { seriesIdx, series ->
                if (series.values.isEmpty()) return@forEachIndexed
                val (minVal, maxVal) = resolveSeriesRange(series, seriesIdx, seriesList, scaleMode)
                val range = if (maxVal > minVal) maxVal - minVal else 1f

                val points = series.values.mapIndexed { idx, v ->
                    val x = idx * pointSpacing
                    val norm = ((v - minVal) / range).coerceIn(0f, 1f)
                    Offset(x, drawH - norm * drawH)
                }

                if (points.isNotEmpty()) {
                    val linePath = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                    }

                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(points.last().x, drawH)
                        lineTo(points.first().x, drawH)
                        close()
                    }

                    val (strokeBrush, fillBrush) = createSeriesBrushes(series, minVal, maxVal, drawH)
                    drawPath(fillPath, fillBrush)
                    drawPath(
                        linePath,
                        strokeBrush,
                        style = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
                    )
                }
            }

            // Active Index Scrubbing Tooltip & Dot Indicators
            if (activeIndex in 0 until pointCount) {
                val scrubX = (activeIndex * pointSpacing).coerceIn(0f, drawW)
                drawLine(
                    color = axisColor.copy(alpha = 0.5f),
                    start = Offset(scrubX, 0f),
                    end = Offset(scrubX, drawH),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                )

                seriesList.forEachIndexed { sIdx, series ->
                    val v = series.values.getOrNull(activeIndex) ?: return@forEachIndexed
                    val (minVal, maxVal) = resolveSeriesRange(series, sIdx, seriesList, scaleMode)
                    val range = if (maxVal > minVal) maxVal - minVal else 1f
                    val norm = ((v - minVal) / range).coerceIn(0f, 1f)
                    val dotY = drawH - norm * drawH
                    val dotColor = series.colorStops.values.lastOrNull() ?: series.fallbackColor

                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(scrubX, dotY))
                    drawCircle(color = dotColor, radius = 3.5.dp.toPx(), center = Offset(scrubX, dotY))
                }

                drawMultiSeriesTooltip(
                    scope = this,
                    scrubX = scrubX,
                    activeIndex = activeIndex,
                    seriesList = seriesList,
                    timestamps = timestamps,
                    drawW = drawW,
                    bgColor = tooltipBgColor,
                    textColor = tooltipTextColor
                )
            }
        }
    }
}

private fun resolveSeriesRange(
    series: RenderedSeries,
    seriesIdx: Int,
    seriesList: List<RenderedSeries>,
    scaleMode: GraphScaleMode
): Pair<Float, Float> {
    return when (scaleMode) {
        GraphScaleMode.SHARED_RAW -> {
            val allVals = seriesList.flatMap { it.values }
            (allVals.minOrNull() ?: 0f) to (allVals.maxOrNull() ?: 1f)
        }
        else -> (series.values.minOrNull() ?: 0f) to (series.values.maxOrNull() ?: 1f)
    }
}

private fun createSeriesBrushes(
    series: RenderedSeries,
    minVal: Float,
    maxVal: Float,
    drawH: Float
): Pair<Brush, Brush> {
    val stops = series.colorStops
    val fallback = series.fallbackColor
    val range = if (maxVal > minVal) maxVal - minVal else 1f

    if (stops.isEmpty()) {
        val sBrush = Brush.verticalGradient(listOf(fallback, fallback))
        val fBrush = Brush.verticalGradient(listOf(fallback.copy(alpha = 0.25f), fallback.copy(alpha = 0.05f)))
        return sBrush to fBrush
    }

    val sorted = stops.entries.sortedBy { it.key }
    val strokeColorStops = sorted.map { (v, c) ->
        val stop = ((v - minVal) / range).coerceIn(0f, 1f)
        stop to c
    }.toTypedArray()

    val fillColorStops = sorted.map { (v, c) ->
        val stop = ((v - minVal) / range).coerceIn(0f, 1f)
        stop to c.copy(alpha = 0.22f)
    }.toTypedArray()

    val strokeBrush = Brush.verticalGradient(colorStops = strokeColorStops, startY = drawH, endY = 0f)
    val fillBrush = Brush.verticalGradient(colorStops = fillColorStops, startY = drawH, endY = 0f)
    return strokeBrush to fillBrush
}

private fun drawYAxisLabels(
    scope: DrawScope,
    seriesList: List<RenderedSeries>,
    scaleMode: GraphScaleMode,
    drawW: Float,
    drawH: Float,
    axisColor: Color
) {
    val textPaint = Paint().apply {
        color = axisColor.toArgb()
        textSize = 24f
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
    }

    val gridLines = 4
    val s0 = seriesList.firstOrNull() ?: return
    val (minVal, maxVal) = resolveSeriesRange(s0, 0, seriesList, scaleMode)
    val range = if (maxVal > minVal) maxVal - minVal else 1f

    repeat(gridLines + 1) { i ->
        val fraction = i / gridLines.toFloat()
        val y = drawH * (1f - fraction)
        val value = when (scaleMode) {
            GraphScaleMode.NORMALIZED -> "${(fraction * 100).toInt()}%"
            GraphScaleMode.SHARED_RAW, GraphScaleMode.DUAL_AXIS -> {
                val v = minVal + fraction * range
                round(v).toInt().toString()
            }
        }
        scope.drawContext.canvas.nativeCanvas.drawText(value, -8f, y + 8f, textPaint)
    }

    if (scaleMode == GraphScaleMode.DUAL_AXIS && seriesList.size > 1) {
        val s1 = seriesList[1]
        val s1Min = s1.values.minOrNull() ?: 0f
        val s1Max = s1.values.maxOrNull() ?: 1f
        val s1Range = if (s1Max > s1Min) s1Max - s1Min else 1f

        val rightPaint = Paint().apply {
            color = axisColor.toArgb()
            textSize = 24f
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }

        repeat(gridLines + 1) { i ->
            val fraction = i / gridLines.toFloat()
            val y = drawH * (1f - fraction)
            val v = s1Min + fraction * s1Range
            scope.drawContext.canvas.nativeCanvas.drawText(round(v).toInt().toString(), drawW + 8f, y + 8f, rightPaint)
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun MultiSeriesChartPreview() {
    MaterialTheme {
        MultiSeriesChart(
            seriesList = listOf(
                RenderedSeries(
                    name = "Speed",
                    values = listOf(10f, 25f, 50f, 40f, 60f, 80f, 30f),
                    unit = "km/h",
                    fallbackColor = Color(0xFF00AAFF)
                ),
                RenderedSeries(
                    name = "Altitude",
                    values = listOf(100f, 120f, 130f, 180f, 220f, 210f, 250f),
                    unit = "m",
                    fallbackColor = Color(0xFF4CAF50)
                )
            ),
            scaleMode = GraphScaleMode.NORMALIZED,
            timestamps = listOf("12:00", "12:05", "12:10", "12:15", "12:20", "12:25", "12:30")
        )
    }
}
