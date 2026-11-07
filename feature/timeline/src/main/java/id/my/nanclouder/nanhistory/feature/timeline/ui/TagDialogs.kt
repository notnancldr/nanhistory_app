package id.my.nanclouder.nanhistory.feature.timeline.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.core.common.navigation.IntentRegistry
import id.my.nanclouder.nanhistory.core.ui.ComponentPlaceholder
import id.my.nanclouder.nanhistory.core.ui.tags.TagDetailDialogState
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.DayTagCrossRef
import id.my.nanclouder.nanhistory.db.EventTagCrossRef
import id.my.nanclouder.nanhistory.db.toHistoryTag
import id.my.nanclouder.nanhistory.domain.model.HistoryTag
import id.my.nanclouder.nanhistory.utils.history.tint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun ColorIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.size(24.dp)
    ) {
        drawCircle(
            color = Color.Gray,
            style = Stroke(width = 3.0f * density),
            radius = size.minDimension / 3.0f
        )
        drawCircle(
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Fill,
            radius = size.minDimension / 3.0f
        )
    }
}

@Composable
fun TagPickerDialog(
    state: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    eventIds: List<String> = emptyList(),
    dayDates: List<LocalDate> = emptyList(),
) {
    val useOldUi = Config.appearanceOldUi.get(LocalContext.current)
    if (!useOldUi) {
        TagPickerDialog_New(
            state = state,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            eventIds = eventIds,
            dayDates = dayDates
        )
    } else {
        TagPickerDialog_Old(
            state = state,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            eventIds = eventIds,
            dayDates = dayDates
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagPickerDialog_New(
    state: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    eventIds: List<String> = emptyList(),
    dayDates: List<LocalDate> = emptyList(),
) {
    if (state) BasicAlertDialog(
        onDismissRequest = {},
        modifier = modifier.fillMaxWidth(0.9f),
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val listState = rememberLazyListState()

        val db = AppDatabase.getInstance(context)
        val dao = db.appDao()

        val tagFlow by dao.getAllTags().collectAsState(emptyList())
        val tagList = tagFlow.map { it.toHistoryTag() }

        var oldSelected by remember { mutableStateOf<List<String>?>(null) }
        val selectedItems = remember { mutableStateListOf<HistoryTag>() }

        BackHandler {
            onDismissRequest()
        }

        LaunchedEffect(Unit) {
            oldSelected = dao.getTagIdsMatchingAllEventIds(eventIds).first()
            selectedItems.addAll(tagList.filter { it.id in oldSelected!! })
        }

        val itemOnClick = { tag: HistoryTag ->
            if (selectedItems.contains(tag))
                selectedItems.remove(tag)
            else selectedItems.add(tag)
        }

        var isProcessing by rememberSaveable { mutableStateOf(false) }

        Surface(
            Modifier
                .clip(RoundedCornerShape(24.dp))
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.85f)
                    .fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        "Select Tags",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            context.startActivity(IntentRegistry.tagDetail(context, ""))
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            "New Tag",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    if (oldSelected != null) items(tagList.size) { index ->
                        val tagData = tagList[index]
                        val isSelected = selectedItems.contains(tagData)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { itemOnClick(tagData) },
                            color = if (isSelected)
                                tagData.tint.copy(alpha = 0.12f)
                            else
                                Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            color = tagData.tint,
                                            shape = RoundedCornerShape(3.dp)
                                        )
                                )

                                Text(
                                    text = tagData.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected)
                                        tagData.tint
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        "Selected",
                                        tint = tagData.tint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        items(3) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    ComponentPlaceholder(Modifier.size(12.dp))
                                    ComponentPlaceholder(Modifier.fillMaxWidth(0.6f).height(16.dp))
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        enabled = !isProcessing,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            isProcessing = true
                            scope.launch(Dispatchers.IO) {
                                val selectedItemsIds = selectedItems.map { it.id }

                                val insertedEventTag = mutableListOf<EventTagCrossRef>()
                                val insertedDayTag = mutableListOf<DayTagCrossRef>()
                                val deletedEventTag = mutableListOf<EventTagCrossRef>()
                                val deletedDayTag = mutableListOf<DayTagCrossRef>()

                                for (eventId in eventIds) {
                                    for (tag in selectedItems) {
                                        val eventTagCrossRef = EventTagCrossRef(
                                            eventId = eventId,
                                            tagId = tag.id
                                        )
                                        if (tag.id !in oldSelected!!)
                                            insertedEventTag.add(eventTagCrossRef)
                                    }
                                    for (tagId in oldSelected!!) {
                                        val eventTagCrossRef = EventTagCrossRef(
                                            eventId = eventId,
                                            tagId = tagId
                                        )
                                        if (tagId !in selectedItemsIds)
                                            deletedEventTag.add(eventTagCrossRef)
                                    }
                                }
                                for (date in dayDates) {
                                    for (tag in selectedItems) {
                                        val dayTagCrossRef = DayTagCrossRef(
                                            date = date,
                                            tagId = tag.id
                                        )
                                        if (tag.id !in oldSelected!!)
                                            insertedDayTag.add(dayTagCrossRef)
                                    }
                                    for (tagId in oldSelected!!) {
                                        val dayTagCrossRef = DayTagCrossRef(
                                            date = date,
                                            tagId = tagId
                                        )
                                        if (tagId !in selectedItemsIds)
                                            deletedDayTag.add(dayTagCrossRef)
                                    }
                                }

                                dao.insertEventTagCrossRefs(insertedEventTag)
                                dao.insertDayTagCrossRefs(insertedDayTag)
                                dao.deleteEventTagCrossRefs(deletedEventTag)
                                dao.deleteDayTagCrossRefs(deletedDayTag)

                                onDismissRequest()
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.height(40.dp)
                    ) {
                        if (!isProcessing) {
                            Text("Confirm")
                        } else {
                            Icon(
                                Icons.Rounded.MoreVert,
                                "Loading",
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(90f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagPickerDialog_Old(
    state: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    eventIds: List<String> = emptyList(),
    dayDates: List<LocalDate> = emptyList(),
) {
    if (state) BasicAlertDialog(
        onDismissRequest = {},
        modifier = modifier,
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val listState = rememberLazyListState()

        val db = AppDatabase.getInstance(context)
        val dao = db.appDao()

        val tagFlow by dao.getAllTags().collectAsState(emptyList())
        val tagList = tagFlow.map { it.toHistoryTag() }

        var oldSelected by remember { mutableStateOf<List<String>?>(null) }
        val selectedItems = remember { mutableStateListOf<HistoryTag>() }

        BackHandler {
            onDismissRequest()
        }

        LaunchedEffect(Unit) {
            oldSelected = dao.getTagIdsMatchingAllEventIds(eventIds).first()
            selectedItems.addAll(tagList.filter { it.id in oldSelected!! })
        }

        val itemOnClick = { tag: HistoryTag ->
            if (selectedItems.contains(tag))
                selectedItems.remove(tag)
            else selectedItems.add(tag)
        }

        var isProcessing by rememberSaveable { mutableStateOf(false) }

        Surface(
            Modifier.clip(RoundedCornerShape(32.dp)),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxHeight(0.8f)
                    .fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Rounded.Close, "Close")
                        }
                        Text("Select Tag", style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(onClick = {
                        context.startActivity(IntentRegistry.tagDetail(context, ""))
                    }) {
                        Icon(Icons.Rounded.Add, "New Tag")
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .padding(8.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    if (oldSelected != null) items(tagList.size) { index ->
                        val tagData = tagList[index]
                        val isSelected = selectedItems.contains(tagData)
                        ListItem(
                            leadingContent = {
                                ColorIcon(tagData.tint)
                            },
                            headlineContent = {
                                Text(text = tagData.name)
                            },
                            trailingContent = {
                                if (isSelected) Icon(
                                    Icons.Rounded.Check,
                                    "Selected"
                                )
                            },
                            modifier = Modifier.clickable { itemOnClick(tagData) },
                            colors = ListItemDefaults.colors(
                                containerColor = if (!isSelected) Color.Transparent else MaterialTheme.colorScheme.secondaryContainer,
                                headlineColor = if (!isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                                trailingIconColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    } else {
                        items(3) {
                            ListItem(
                                leadingContent = { ComponentPlaceholder(Modifier.size(24.dp)) },
                                headlineContent = { ComponentPlaceholder(Modifier.size(72.dp, 16.dp)) }
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Button(
                        onClick = {
                            isProcessing = true
                            scope.launch(Dispatchers.IO) {
                                val selectedItemsIds = selectedItems.map { it.id }

                                val insertedEventTag = mutableListOf<EventTagCrossRef>()
                                val insertedDayTag = mutableListOf<DayTagCrossRef>()
                                val deletedEventTag = mutableListOf<EventTagCrossRef>()
                                val deletedDayTag = mutableListOf<DayTagCrossRef>()

                                for (eventId in eventIds) {
                                    for (tag in selectedItems) {
                                        val eventTagCrossRef = EventTagCrossRef(
                                            eventId = eventId,
                                            tagId = tag.id
                                        )
                                        if (tag.id !in oldSelected!!)
                                            insertedEventTag.add(eventTagCrossRef)
                                    }
                                    for (tagId in oldSelected!!) {
                                        val eventTagCrossRef = EventTagCrossRef(
                                            eventId = eventId,
                                            tagId = tagId
                                        )
                                        if (tagId !in selectedItemsIds)
                                            deletedEventTag.add(eventTagCrossRef)
                                    }
                                }
                                for (date in dayDates) {
                                    for (tag in selectedItems) {
                                        val dayTagCrossRef = DayTagCrossRef(
                                            date = date,
                                            tagId = tag.id
                                        )
                                        if (tag.id !in oldSelected!!)
                                            insertedDayTag.add(dayTagCrossRef)
                                    }
                                    for (tagId in oldSelected!!) {
                                        val dayTagCrossRef = DayTagCrossRef(
                                            date = date,
                                            tagId = tagId
                                        )
                                        if (tagId !in selectedItemsIds)
                                            deletedDayTag.add(dayTagCrossRef)
                                    }
                                }

                                dao.insertEventTagCrossRefs(insertedEventTag)
                                dao.insertDayTagCrossRefs(insertedDayTag)
                                dao.deleteEventTagCrossRefs(deletedEventTag)
                                dao.deleteDayTagCrossRefs(deletedDayTag)

                                onDismissRequest()
                            }
                        },
                        enabled = !isProcessing
                    ) {
                        if (!isProcessing) Text("Confirm")
                        else Icon(Icons.Rounded.MoreVert, "Loading", Modifier.rotate(90f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagDetailDialog(state: TagDetailDialogState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

    if (state.isOpen) BasicAlertDialog(
        onDismissRequest = {
            state.close()
        },
        modifier = modifier
    ) {
        val tagFlow by dao.getTagById(state.tagId!!).collectAsState(null)
        val tagData = tagFlow

        Surface(
            Modifier.clip(RoundedCornerShape(32.dp)),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            val useCountFlow = tagData?.let {
                dao.getEventCountForTag(tagData.id).collectAsState(0)
            }

            val useCount = useCountFlow?.value ?: 0
            BackHandler {
                state.close()
            }
            if (tagData != null) Column(
                Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tagData.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    ColorIcon(tagData.tint)
                }
                Text(
                    text = tagData.description,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${if (useCount == 0) "No" else useCount} event${if (useCount == 1) "" else "s"} using this tag",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleSmall
                )
                TextButton(
                    onClick = {
                        state.close()
                        context.startActivity(IntentRegistry.tagDetail(context, tagData.id))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Detail")
                }
            } else {
                ComponentPlaceholder(Modifier.fillMaxWidth().height(128.dp))
            }
        }
    }
}
