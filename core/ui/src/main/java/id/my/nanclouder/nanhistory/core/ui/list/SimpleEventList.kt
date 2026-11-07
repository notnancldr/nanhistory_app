package id.my.nanclouder.nanhistory.core.ui.list

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.core.common.navigation.IntentRegistry
import id.my.nanclouder.nanhistory.core.common.time.DateFormatter
import id.my.nanclouder.nanhistory.core.common.time.TimeFormatter
import id.my.nanclouder.nanhistory.core.common.time.readableTimeHours
import id.my.nanclouder.nanhistory.core.ui.selection.SelectionState
import id.my.nanclouder.nanhistory.core.ui.tags.TagsView
import id.my.nanclouder.nanhistory.domain.model.EventRange
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import java.time.Duration

@Composable
fun SimpleEventList(
    events: List<HistoryEvent>,
    selectionState: SelectionState<HistoryEvent>,
    lazyListState: LazyListState = rememberLazyListState(),
    topItem: (@Composable LazyItemScope.() -> Unit)? = null,
    onEventClick: ((HistoryEvent) -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val selectionMode by selectionState.isSelectionMode.collectAsState()
    val selectedItems by selectionState.selectedItems.collectAsState()

    val grouped = remember(events) {
        events.groupBy { it.time.toLocalDate() }.toSortedMap(reverseOrder())
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        if (topItem != null) {
            item { topItem() }
        }

        grouped.forEach { (date, dayEvents) ->
            item(key = "header-$date") {
                ListItem(
                    headlineContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(date.format(DateFormatter), style = MaterialTheme.typography.titleMedium)
                            Box(Modifier.width(8.dp))
                            Text("(${dayEvents.size})", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        }
                    }
                )
            }

            items(dayEvents.size, key = { dayEvents[it].id }) { index ->
                val event = dayEvents[index]
                val isSelected = selectedItems.contains(event)

                ListItem(
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            if (selectionMode) {
                                if (isSelected) selectionState.deselect(event)
                                else selectionState.select(event)
                            } else {
                                onEventClick?.invoke(event)
                                    ?: context.startActivity(IntentRegistry.eventDetail(context, event.id))
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isSelected) selectionState.deselect(event)
                            else selectionState.select(event)
                        }
                    ),
                    leadingContent = if (selectionMode) {
                        {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectionState.select(event)
                                    else selectionState.deselect(event)
                                }
                            )
                        }
                    } else null,
                    headlineContent = {
                        Text(event.title.ifBlank { "Untitled Event" }, style = MaterialTheme.typography.bodyLarge)
                    },
                    supportingContent = {
                        Column {
                            if (event is EventRange) {
                                val duration = Duration.between(event.time, event.end)
                                Text(
                                    "${event.time.format(TimeFormatter)} - ${event.end.format(TimeFormatter)} (${readableTimeHours(duration)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    event.time.format(TimeFormatter),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (event.tags.isNotEmpty()) {
                                Box(Modifier.height(4.dp))
                                TagsView(tags = event.tags, limit = 3)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SimpleEventListItem(
    event: HistoryEvent,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier,
        headlineContent = {
            Text(event.title.ifBlank { "Untitled Event" }, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            Column {
                if (event is EventRange) {
                    val duration = Duration.between(event.time, event.end)
                    Text(
                        "${event.time.format(TimeFormatter)} - ${event.end.format(TimeFormatter)} (${readableTimeHours(duration)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        event.time.format(TimeFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (event.tags.isNotEmpty()) {
                    Box(Modifier.height(4.dp))
                    TagsView(tags = event.tags, limit = 3)
                }
            }
        }
    )
}
