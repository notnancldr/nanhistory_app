package id.my.nanclouder.nanhistory.ui

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import id.my.nanclouder.nanhistory.EventDetailActivity
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.service.RecordService
import id.my.nanclouder.nanhistory.state.SelectionState
import id.my.nanclouder.nanhistory.ui.MentionModalState
import id.my.nanclouder.nanhistory.ui.list.EventListHeader
import id.my.nanclouder.nanhistory.ui.list.EventListItem
import id.my.nanclouder.nanhistory.ui.list.TimelineEventItem
import id.my.nanclouder.nanhistory.ui.main.EventListViewModel
import id.my.nanclouder.nanhistory.ui.main.EventSelectMode
import id.my.nanclouder.nanhistory.utils.DateFormatter
import id.my.nanclouder.nanhistory.utils.QuickScroll
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import id.my.nanclouder.nanhistory.utils.history.getFilePathFromDate
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.Executors
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EventList(
    viewModel: EventListViewModel,
    selectionState: SelectionState<HistoryEvent>,
    loadHeaderData: Boolean = true,
    lazyListState: LazyListState = rememberLazyListState(),
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
    topItem: (@Composable LazyItemScope.() -> Unit)? = null,
    onEmptyContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val expanded = remember { mutableStateListOf<LocalDate>() }

    val tagDetailDialogState = rememberTagDetailDialogState()

    val db = remember { AppDatabase.getInstance(context) }
    val dao = remember { db.appDao() }

    val dayList by viewModel.days.collectAsState(viewModel.currentDays)
    val eventListState by viewModel.events.collectAsState(viewModel.currentEvents)

    val dateList = dayList.map { it.date }

    val eventList = eventListState.filter { event -> event.time.toLocalDate() in dateList }

    val haptic = LocalHapticFeedback.current

    val selectionMode by selectionState.isSelectionMode.collectAsState()
    val selectedItems by selectionState.selectedItems.collectAsState()

    var highlightedDay by remember { mutableStateOf<LocalDate?>(null) }

    val mentionModalState = remember { mutableStateOf(MentionModalState()) }

    var gotoDay by remember { mutableStateOf<LocalDate?>(null) }

    val signatureCheckDispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()

    if (viewModel.mode == EventSelectMode.Default) viewModel.onGotoDay { date ->
        gotoDay = date
    }

    LaunchedEffect(gotoDay, dayList, eventList) {
        if (gotoDay != null && dayList.isNotEmpty() && eventList.isNotEmpty()) {

            highlightedDay = gotoDay

            val grouped = eventList.groupBy {
                dayList.find { day -> day.date == it.time.toLocalDate() }!!
            }

            var index = 0
            for ((day, events) in grouped) {
                if (day.date == gotoDay) {
                    lazyListState.scrollToItem(index)
                    break
                }
                index += events.size + 1
            }
        }
        delay(200L)
        gotoDay = null
    }

    if (!selectionMode) selectionState.clear()

    if (eventList.isNotEmpty()) Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .selectableGroup()
                .fillMaxSize()
                .zIndex(-1f),
            state = lazyListState,
        ) {
            val expandedCurrent = expanded.toList()

            if (topItem != null) {
                item { Box(Modifier.height(16.dp)) }
                item(content = topItem)
            }

            eventList.groupBy { it.time.toLocalDate() }.forEach { (date, events) ->
                stickyHeader(key = "$date") {
                    val day =
                        if (loadHeaderData) dayList.find { it.date == date }
                        else null

                    val selected = selectedItems.containsAll(events)
                    val highlighted = highlightedDay == date

                    var fading by remember { mutableStateOf(false) }
                    var isHighlighting by remember { mutableStateOf(highlighted) }

                    val highlightedColor = MaterialTheme.colorScheme.primaryContainer

                    val duration = if (fading) 1000 else 0
                    val targetColor = if (fading && highlighted) Color.Transparent else highlightedColor

                    val backgroundColor = if (isHighlighting)
                        animateColorAsState(
                            targetValue = targetColor,
                            animationSpec = tween(durationMillis = duration),
                            finishedListener = {
                                if (!fading) {
                                    fading = true
                                }
                                else {
                                    isHighlighting = false
                                    fading = false
                                }
                            }
                        )
                    else null

                    if (day != null) EventListHeader(
                        historyDay = day,
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null
                            )
                            .combinedClickable(
                                onLongClick = listItemOnLongClick@{
                                    haptic.performHapticFeedback(
                                        HapticFeedbackType.LongPress
                                    )
                                    if (selected) selectionState.deselectAll(events)
                                    else selectionState.selectAll(events)
                                },
                                onClick = {
                                    if (selectionMode) {
                                        if (selected) selectionState.deselectAll(events)
                                        else selectionState.selectAll(events)
                                    }
                                    else if (viewModel.mode != EventSelectMode.Default) {
                                        viewModel.gotoDay(date)
                                    }
                                }
                            )
                            .zIndex(0f)
                            .background(if (isHighlighting) backgroundColor!!.value else Color.Transparent),
                        expanded = expandedCurrent.contains(date),
                        eventCount = events.size,
                        selected = selected,
                    ) {
                        scope.launch { dao.toggleFavoriteDay(date) }
                    }
                    else if (loadHeaderData) ListItem(
                        headlineContent = {
                            ComponentPlaceholder(Modifier
                                .height(16.dp)
                                .width(64.dp))
                        }
                    )
                    else ListItem(
                        headlineContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    date.format(DateFormatter),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Box(Modifier.width(8.dp))
                                Text(
                                    "(${events.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.Gray
                                )
                            }
                        }
                    )
                }

                items(events.size, key = { "event-${events[it].id}-${events[it].hashCode()}" }) { index ->
                    val event = events[index]
                    val isFirst = index == 0
                    val isLast = index == events.size - 1

                    val recordEventId by RecordService.RecordState.eventId.collectAsState()

                    val selected = selectedItems.contains(event)
                    val recording = recordEventId == event.id

                    val newUI by Config.appearanceNewUI.getState()

                    if (newUI) TimelineEventItem(
                        event,
                        selected = selected,
                        recording = recording,
                        isFirst = isFirst,
                        isLast = isLast,
                        tagDetailDialogState = tagDetailDialogState,
                        mentionModalState = mentionModalState,
                        coroutineDispatcher = signatureCheckDispatcher,
                        modifier = Modifier
                            .animateItem(fadeInSpec = null, fadeOutSpec = null)
                            .combinedClickable(
                                onClick = listItemOnClick@{
                                    if (!selectionMode) {
                                        val intent =
                                            Intent(
                                                context,
                                                EventDetailActivity::class.java
                                            )
                                        intent.putExtra("eventId", event.id)
                                        intent.putExtra(
                                            "path",
                                            getFilePathFromDate(event.time.toLocalDate())
                                        )
                                        context.startActivity(intent)
                                    } else {
                                        if (recording) return@listItemOnClick
                                        if (selected) selectionState.deselect(event)
                                        else selectionState.select(event)
                                    }
                                },
                                onLongClick = listItemOnLongClick@{
                                    if (recording) return@listItemOnLongClick
                                    haptic.performHapticFeedback(
                                        HapticFeedbackType.LongPress
                                    )
                                    if (selected) selectionState.deselect(event)
                                    else selectionState.select(event)
                                }
                            )
                            .zIndex(-1f),
                    )
                    else EventListItem(
                        event,
                        selected = selected,
                        recording = recording,
                        tagDetailDialogState = tagDetailDialogState,
                        modifier = Modifier
                            .animateItem(fadeInSpec = null, fadeOutSpec = null)
                            .combinedClickable(
                                onClick = listItemOnClick@{
//                                            if (lock) return@listItemOnClick
                                    if (!selectionMode) {
                                        val intent =
                                            Intent(
                                                context,
                                                EventDetailActivity::class.java
                                            )
                                        intent.putExtra("eventId", event.id)
                                        intent.putExtra(
                                            "path",
                                            getFilePathFromDate(event.time.toLocalDate())
                                        )
                                        context.startActivity(intent)
                                    } else {
                                        if (recording) return@listItemOnClick
                                        if (selected) selectionState.deselect(event)
                                        else selectionState.select(event)
                                    }
                                },
                                onLongClick = listItemOnLongClick@{
                                    if (recording) return@listItemOnLongClick
//                                            selectionMode = true
                                    haptic.performHapticFeedback(
                                        HapticFeedbackType.LongPress
                                    )
                                    if (selected) selectionState.deselect(event)
                                    else selectionState.select(event)
                                }
                            )
                            .zIndex(-1f),
                    )
                }
            }
            item {
                Box(modifier = Modifier.height(136.dp))
            }
        }

        QuickScroll(lazyListState)
        TagDetailDialog(tagDetailDialogState)
        MentionModalHandler(mentionModalState.value)
    }
    else Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        content = onEmptyContent ?: {}
    )
}