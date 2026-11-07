package id.my.nanclouder.nanhistory.feature.sharemap.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.model.LocationData
import kotlin.math.roundToInt

@Composable
fun ShareCanvasTransformer(
    modifier: Modifier = Modifier,
    elements: List<CanvasElement>,
    selectedElementId: String?,
    eventTitle: String,
    dateString: String,
    eventData: HistoryEvent? = null,
    metricsText: Map<String, String>,
    locations: List<LocationData>,
    elevationGainMeters: Double? = null,
    isMapAdjustMode: Boolean = false,
    onSelectElement: (String?) -> Unit,
    onUpdateElement: (CanvasElement) -> Unit,
    onDeleteElement: (String) -> Unit,
    onOpenStyleSheet: (CanvasElement) -> Unit,
    onTransformCommit: (() -> Unit)? = null,
    isExporting: Boolean = false,
) {
    val density = LocalDensity.current

    val rootPointerModifier = if (!isExporting && !isMapAdjustMode) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { onSelectElement(null) })
        }
    } else Modifier

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .then(rootPointerModifier)
    ) {
        val canvasWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(100f)
        val canvasHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(100f)

        elements.sortedBy { it.zIndex }.forEach { element ->
            val isSelected = !isExporting && !isMapAdjustMode && element.id == selectedElementId
            val currentElement by rememberUpdatedState(element)

            val elementLeftPx = element.xPercent * canvasWidthPx
            val elementTopPx = element.yPercent * canvasHeightPx
            val elementWidthPx = element.widthPercent * canvasWidthPx
            val elementHeightPx = element.heightPercent * canvasHeightPx

            val elementWidthDp = with(density) { elementWidthPx.toDp() }
            val elementHeightDp = with(density) { elementHeightPx.toDp() }

            val elementPointerModifier = if (!isExporting && !isMapAdjustMode) {
                Modifier
                    .pointerInput(element.id) {
                        detectTapGestures(onTap = { onSelectElement(element.id) })
                    }
                    .pointerInput(element.id) {
                        detectDragGestures(
                            onDragStart = { onSelectElement(element.id) },
                            onDragEnd = { onTransformCommit?.invoke() },
                            onDragCancel = { onTransformCommit?.invoke() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val deltaXPercent = dragAmount.x / canvasWidthPx
                                val deltaYPercent = dragAmount.y / canvasHeightPx
                                val curr = currentElement
                                val newX = (curr.xPercent + deltaXPercent).coerceIn(0f, 1f - curr.widthPercent)
                                val newY = (curr.yPercent + deltaYPercent).coerceIn(0f, 1f - curr.heightPercent)
                                onUpdateElement(curr.copy(xPercent = newX, yPercent = newY))
                            }
                        )
                    }
            } else Modifier

            Box(
                modifier = Modifier
                    .offset { IntOffset(elementLeftPx.roundToInt(), elementTopPx.roundToInt()) }
                    .width(elementWidthDp)
                    .height(elementHeightDp)
                    .zIndex(if (isSelected) 100f else element.zIndex)
                    .graphicsLayer {
                        scaleX = element.scale
                        scaleY = element.scale
                        rotationZ = element.rotationDegrees
                    }
                    .then(elementPointerModifier)
            ) {
                // Element Content Container
                val bgCol = parseHexToColor(element.style.backgroundColorHex, Color(0x66000000))
                val borderCol = parseHexToColor(element.style.borderColorHex, Color.Transparent)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(element.style.cornerRadiusDp.dp))
                        .background(bgCol)
                        .then(
                            if (element.style.borderWidthDp > 0f) {
                                Modifier.border(
                                    width = element.style.borderWidthDp.dp,
                                    color = borderCol,
                                    shape = RoundedCornerShape(element.style.cornerRadiusDp.dp)
                                )
                            } else Modifier
                        )
                        .padding(element.style.paddingDp.dp)
                ) {
                    RenderElementContent(
                        element = element,
                        eventTitle = eventTitle,
                        dateString = dateString,
                        eventData = eventData,
                        metricsText = metricsText,
                        locations = locations,
                        elevationGainMeters = elevationGainMeters
                    )
                }

                // Interactive Selection Bounding Box & Action Overlay
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(element.style.cornerRadiusDp.dp)
                            )
                    )

                    // Corner Resize Handles
                    CornerHandle(
                        modifier = Modifier.align(Alignment.TopStart),
                        onDragEnd = { onTransformCommit?.invoke() },
                        onDrag = { dx, dy ->
                            val curr = currentElement
                            val newW = (curr.widthPercent - dx / canvasWidthPx).coerceIn(0.1f, 1f)
                            val newH = (curr.heightPercent - dy / canvasHeightPx).coerceIn(0.05f, 1f)
                            val newX = (curr.xPercent + dx / canvasWidthPx).coerceIn(0f, 1f - newW)
                            val newY = (curr.yPercent + dy / canvasHeightPx).coerceIn(0f, 1f - newH)
                            onUpdateElement(
                                curr.copy(xPercent = newX, yPercent = newY, widthPercent = newW, heightPercent = newH)
                            )
                        }
                    )
                    CornerHandle(
                        modifier = Modifier.align(Alignment.TopEnd),
                        onDragEnd = { onTransformCommit?.invoke() },
                        onDrag = { dx, dy ->
                            val curr = currentElement
                            val newW = (curr.widthPercent + dx / canvasWidthPx).coerceIn(0.1f, 1f - curr.xPercent)
                            val newH = (curr.heightPercent - dy / canvasHeightPx).coerceIn(0.05f, 1f)
                            val newY = (curr.yPercent + dy / canvasHeightPx).coerceIn(0f, 1f - newH)
                            onUpdateElement(curr.copy(yPercent = newY, widthPercent = newW, heightPercent = newH))
                        }
                    )
                    CornerHandle(
                        modifier = Modifier.align(Alignment.BottomStart),
                        onDragEnd = { onTransformCommit?.invoke() },
                        onDrag = { dx, dy ->
                            val curr = currentElement
                            val newW = (curr.widthPercent - dx / canvasWidthPx).coerceIn(0.1f, 1f)
                            val newH = (curr.heightPercent + dy / canvasHeightPx).coerceIn(0.05f, 1f - curr.yPercent)
                            val newX = (curr.xPercent + dx / canvasWidthPx).coerceIn(0f, 1f - newW)
                            onUpdateElement(curr.copy(xPercent = newX, widthPercent = newW, heightPercent = newH))
                        }
                    )
                    CornerHandle(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        onDragEnd = { onTransformCommit?.invoke() },
                        onDrag = { dx, dy ->
                            val curr = currentElement
                            val newW = (curr.widthPercent + dx / canvasWidthPx).coerceIn(0.1f, 1f - curr.xPercent)
                            val newH = (curr.heightPercent + dy / canvasHeightPx).coerceIn(0.05f, 1f - curr.yPercent)
                            onUpdateElement(curr.copy(widthPercent = newW, heightPercent = newH))
                        }
                    )

                    // Quick Action Bar on top of element
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-40).dp)
                            .shadow(4.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(onClick = { onOpenStyleSheet(element) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { onUpdateElement(element.copy(zIndex = element.zIndex + 1f)) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = "Up",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val newZ = (element.zIndex - 1f).coerceAtLeast(0f)
                                    onUpdateElement(element.copy(zIndex = newZ))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Down",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteElement(element.id) },
                                modifier = Modifier.size(32.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "Del",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CornerHandle(
    modifier: Modifier = Modifier,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onDragEnd: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onDragEnd?.invoke() },
                    onDragCancel = { onDragEnd?.invoke() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .border(2.dp, Color.White, CircleShape)
        )
    }
}
