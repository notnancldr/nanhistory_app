package id.my.nanclouder.nanhistory.utils

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import id.my.nanclouder.nanhistory.config.Config
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun BoxScope.QuickScroll(
    listState: LazyListState
) {
    val newUI = Config.appearanceNewUI.get(LocalContext.current)
    if (newUI) {
        EnhancedQuickScroll(listState)
    } else {
        QuickScroll_Old(listState)
    }
}

@Composable
private fun BoxScope.EnhancedQuickScroll(
    listState: LazyListState
) {
    var isDragging by remember { mutableStateOf(false) }
    val isScrolling = listState.isScrollInProgress || isDragging

    val haptic = LocalHapticFeedback.current

    val targetAlpha = if (isDragging) 1f else if (isScrolling) 0.6f else 0f
    val duration = if (isScrolling) 150 else 500
    val delay = if (isScrolling) 0 else 1200

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(delayMillis = delay, durationMillis = duration)
    )

    val scope = rememberCoroutineScope()
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    var containerHeight by remember { mutableIntStateOf(0) }
    var handleHeight by remember { mutableIntStateOf(0) }

    var previousIndex by remember { mutableIntStateOf(0) }

    var layoutInfo by remember { mutableStateOf<LazyListLayoutInfo?>(null) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo = it }
    }

    val currentLayoutInfo = layoutInfo
    val viewPortHeight = currentLayoutInfo?.viewportEndOffset?.minus(currentLayoutInfo.viewportStartOffset) ?: 0
    val totalItems = currentLayoutInfo?.totalItemsCount ?: 0

    val thumbHeight = 56.dp
    val thumbWidth = 5.dp
    val dragHandleShape = RoundedCornerShape(8.dp)
    val firstVisibleElementIndex = currentLayoutInfo?.visibleItemsInfo?.firstOrNull()?.index ?: 0

    val density = LocalDensity.current

    val maxOffset = remember(containerHeight, thumbHeight) {
        with(density) {
            maxOf(0f, (containerHeight - thumbHeight.toPx()))
        }
    }

    val scrollProgress = if (totalItems > 0) {
        (firstVisibleElementIndex.toFloat() / totalItems.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val scrollbarOffsetY = scrollProgress * maxOffset

    if (!isDragging) dragOffsetY = scrollbarOffsetY

    // Outer container for tap detection
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(24.dp)
            .align(Alignment.CenterEnd)
            .onSizeChanged { newSize ->
                containerHeight = newSize.height
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newOffset = (offset.y - (thumbHeight / 2).toPx()).coerceIn(0f, maxOffset)
                    dragOffsetY = newOffset
                    val scrollProgress = dragOffsetY / maxOffset
                    val visibleCount = currentLayoutInfo?.visibleItemsInfo?.size ?: 1
                    val maxIndex = (totalItems - visibleCount).coerceAtLeast(0)
                    val itemIndex = (scrollProgress * maxIndex).toInt()
                        .coerceIn(0, maxIndex)
                    scope.launch {
                        listState.scrollToItem(itemIndex)
                    }
                }
            }
    ) {
        // Scrollbar thumb
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp)
                .offset {
                    IntOffset(0, dragOffsetY.roundToInt())
                }
                .height(thumbHeight)
                .width(thumbWidth)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        dragOffsetY = (dragOffsetY + delta).coerceIn(0f, maxOffset)
                        val scrollProgress = dragOffsetY / maxOffset
                        val visibleCount = currentLayoutInfo?.visibleItemsInfo?.size ?: 1
                        val maxIndex = (totalItems - visibleCount).coerceAtLeast(0)
                        val itemIndex = (scrollProgress * maxIndex).toInt()
                            .coerceIn(0, maxIndex)
                        if (itemIndex != previousIndex) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            previousIndex = itemIndex
                        }
                        scope.launch {
                            listState.scrollToItem(itemIndex)
                        }
                    },
                    onDragStarted = { isDragging = true },
                    onDragStopped = { isDragging = false }
                )
                .graphicsLayer { this.alpha = alpha }
                .clip(dragHandleShape),
            color = if (isDragging) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
            shape = dragHandleShape,
            shadowElevation = if (isDragging) 8.dp else 0.dp
        ) {}

        // Percentage indicator during drag
        if (isDragging) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset {
                        IntOffset(-64, dragOffsetY.roundToInt())
                    }
                    .width(28.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 4.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "${(scrollProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun QuickScroll_Old(
    listState: LazyListState
) {
    var isDragging by remember { mutableStateOf(false) }
    val isScrolling = listState.isScrollInProgress || isDragging

    val haptic = LocalHapticFeedback.current

    val targetAlpha = if (isDragging) 1f else if (isScrolling) 0.5f else 0f
    val duration = if (isScrolling) 150 else 500
    val delay = if (isScrolling) 0 else 1000

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(delayMillis = delay, durationMillis = duration)
    )

    val scope = rememberCoroutineScope()
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    var containerHeight by remember { mutableIntStateOf(0) }
    var handleHeight by remember { mutableIntStateOf(0) }

    var previousIndex by remember { mutableIntStateOf(0) }

    var layoutInfo by remember { mutableStateOf<LazyListLayoutInfo?>(null) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo = it }
    }

    val averageItemHeight = remember(layoutInfo) {
        val viewPortHeight = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
        if (viewPortHeight <= 0) {
            1
        } else {
            viewPortHeight / listState.layoutInfo.totalItemsCount
        }
    }

    val onPositionUpdated: (yPos: Int) -> Unit = {
        val itemIndex = (it / averageItemHeight).coerceIn(0, listState.layoutInfo.totalItemsCount)

        if (itemIndex != previousIndex) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            previousIndex = itemIndex
        }
        scope.launch {
            // TODO: or listState.animateScrollToItem(itemIndex)
            listState.scrollToItem(itemIndex)
        }
    }

    val maxOffset = remember(containerHeight, handleHeight) {
        (containerHeight - handleHeight).toFloat()
    }

    val thumbHeight = 48.dp

    val dragHandleShape = RoundedCornerShape(8.dp)
    val needDrawScrollbar = isScrolling || alpha > 0.0f
    val firstVisibleElementIndex = layoutInfo?.visibleItemsInfo?.firstOrNull()?.index


    if (firstVisibleElementIndex != null) {
        val scrollbarOffsetY = firstVisibleElementIndex * averageItemHeight

        if (!isDragging) dragOffsetY = scrollbarOffsetY.toFloat()
        Row(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    containerHeight = it.height
                }
                .graphicsLayer { this.alpha = alpha }
                .zIndex(99f),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .offset {
                        IntOffset(0, dragOffsetY.roundToInt())
                    }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            dragOffsetY = (dragOffsetY + delta).coerceIn(0f, maxOffset)
                            onPositionUpdated(dragOffsetY.roundToInt())
                        },
                        onDragStarted = { isDragging = true },
                        onDragStopped = { isDragging = false }
                    )
                    .defaultMinSize(
                        minWidth = 24.dp,
                        minHeight = thumbHeight
                    )
                    .onSizeChanged {
                        handleHeight = it.height
                    }
                    .padding(8.dp)
                    .zIndex(99f),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    Modifier
                        .height(thumbHeight)
                        .width(4.dp)
                        .clip(dragHandleShape)
                        .background(
                            if (!isDragging) Color.Gray
                            else MaterialTheme.colorScheme.primaryContainer
                        )

                        .zIndex(99f)
                )
            }
        }
    }
}