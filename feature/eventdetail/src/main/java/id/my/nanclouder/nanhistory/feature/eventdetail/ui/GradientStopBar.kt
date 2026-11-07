package id.my.nanclouder.nanhistory.feature.eventdetail.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Interactive gradient preview bar with draggable color stop handles.
 */
@Composable
fun GradientStopBar(
    stops: List<Pair<Float, Color>>,
    selectedIdx: Int,
    onSelectStop: (Int) -> Unit,
    onMoveStop: (idx: Int, newPos: Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f
) {
    val handleRadius = 10.dp
    val barHeight = 28.dp

    val safeStops = if (stops.size <= 1) {
        stops + (0 until (2 - stops.size)).map { i ->
            (valueRange.start + i.toFloat()) to (stops.firstOrNull()?.second ?: Color.White)
        }
    } else stops

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight + handleRadius * 2)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .align(Alignment.TopCenter)
                .padding(horizontal = handleRadius)
        ) {
            val sorted = safeStops.sortedBy { it.first }
            val range = valueRange.endInclusive - valueRange.start
            val normStops = sorted.map { (pos, color) ->
                ((pos - valueRange.start) / range.coerceAtLeast(1e-6f)) to color
            }
            val brush = Brush.horizontalGradient(
                colorStops = normStops.map { (p, c) -> p to c }.toTypedArray()
            )
            drawRoundRect(brush = brush, cornerRadius = CornerRadius(8.dp.toPx()))
        }

        val density = LocalDensity.current
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            val trackWidthPx = with(density) { (maxWidth - handleRadius * 2).toPx() }
            val rangeSpan = valueRange.endInclusive - valueRange.start

            stops.forEachIndexed { idx, (pos, color) ->
                val normPos = (pos - valueRange.start) / rangeSpan.coerceAtLeast(1e-6f)
                val offsetX = handleRadius + (maxWidth - handleRadius * 2) * normPos

                Box(
                    modifier = Modifier
                        .offset(x = offsetX - handleRadius, y = 0.dp)
                        .size(handleRadius * 2)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (idx == selectedIdx) 2.5.dp else 1.dp,
                            color = if (idx == selectedIdx) Color.White else Color.LightGray,
                            shape = CircleShape
                        )
                        .pointerInput(idx) {
                            detectTapGestures { onSelectStop(idx) }
                        }
                        .pointerInput(idx, trackWidthPx, rangeSpan) {
                            detectDragGestures(
                                onDragStart = { onSelectStop(idx) }
                            ) { change, dragAmount ->
                                change.consume()
                                val currentPos = stops.getOrNull(idx)?.first ?: return@detectDragGestures
                                val delta = dragAmount.x / trackWidthPx * rangeSpan
                                val newPos = (currentPos + delta).coerceIn(valueRange.start, valueRange.endInclusive)
                                onMoveStop(idx, newPos)
                            }
                        }
                )
            }
        }
    }
}
