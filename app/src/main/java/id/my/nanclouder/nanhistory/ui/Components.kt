package id.my.nanclouder.nanhistory.ui

import android.content.Intent
import android.media.MediaPlayer
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import id.my.nanclouder.nanhistory.ImportProgressStage
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.DayTagCrossRef
import id.my.nanclouder.nanhistory.db.EventTagCrossRef
import id.my.nanclouder.nanhistory.db.TagEntity
import id.my.nanclouder.nanhistory.db.toHistoryTag
import id.my.nanclouder.nanhistory.lib.history.HistoryTag
import id.my.nanclouder.nanhistory.lib.history.generateTagId
import id.my.nanclouder.nanhistory.lib.readableTimeHours
import id.my.nanclouder.nanhistory.service.DataProcessService
import id.my.nanclouder.nanhistory.ui.tags.TagsView
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

@Composable
fun ToggleButton(
    onClick: () -> Unit,
    active: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val buttonPadding = PaddingValues(horizontal = 8.dp)
    if (active) Button(
        onClick = onClick,
        content = content,
        contentPadding = buttonPadding
    )
    else OutlinedButton(
        onClick = onClick,
        content = content,
        contentPadding = buttonPadding
    )
}

@Composable
fun RequestMultiplePermissions(
    permissions: List<String>,
    onPermissionsResult: (Map<String, Boolean>) -> Unit
) {
    Log.d("NanHistoryDebug", "Requesting permissions... $permissions")
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result: Map<String, Boolean> ->
        Log.d("NanHistoryDebug", "Result permission request $result")
        onPermissionsResult(result)
    }

    LaunchedEffect(Unit) {
        delay(500L)
        launcher.launch(permissions.toTypedArray())
    }
}

@Composable
fun AudioPlayer(path: String) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) } // progress: 0.0 to 1.0
    var recordDuration by remember { mutableIntStateOf(0) }
    var userIsSeeking by remember { mutableStateOf(false) }

    var audioAvailable by remember { mutableStateOf(true) }

    val context = LocalContext.current

    // Create MediaPlayer instance with the file path
    val mediaPlayer = remember {
        if (android.os.Build.VERSION.SDK_INT >= 34) MediaPlayer(context).apply {
            try {
                setDataSource(path)
                prepare()
            }
            catch (e: IOException) {
                audioAvailable = false
//                path = null
            }
        }
        else {
            audioAvailable = false
            null
        }
    }

    LaunchedEffect(Unit) {
        recordDuration = mediaPlayer?.duration ?: 0
    }

    LaunchedEffect(isPlaying, userIsSeeking) {
        if (isPlaying) {
            mediaPlayer?.start()
        } else {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer.pause()
            }
        }
        while (isPlaying && !userIsSeeking) {
            val current = mediaPlayer?.currentPosition
            progress = current ?: 0
            delay(100L) // Update every half second
        }
    }

    DisposableEffect(Unit) {
        mediaPlayer?.setOnCompletionListener {
            isPlaying = false // Update button when playback ends
            progress = 0
        }

        onDispose {
            mediaPlayer?.release()
        }
    }

    val file = File(path)

    if (file.isFile) ListItem(
        headlineContent = {
            Slider(
                value = progress.toFloat(),
                onValueChange = {
                    userIsSeeking = true
                    progress = it.toInt()
                },
                onValueChangeFinished = {
                    mediaPlayer?.seekTo(progress)
                    userIsSeeking = false
                },
                colors = SliderDefaults.colors(
                    inactiveTrackColor = Color.Gray
                ),
                valueRange = 0f..recordDuration.toFloat(),
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
        },
        leadingContent = {
            IconButton(
                onClick = { isPlaying = !isPlaying }
            ) {
                if (!isPlaying) Icon(painterResource(R.drawable.ic_play_arrow_filled), "Play")
                else Icon(painterResource(R.drawable.ic_pause_filled), "Pause")
            }
        },
        trailingContent = {
            val progressSeconds = Duration.ofMillis(progress.toLong())
            val durationSeconds = Duration.ofMillis(recordDuration.toLong())
            Text("${readableTimeHours(progressSeconds)}/${readableTimeHours(durationSeconds)}")
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier
            .padding(8.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
    )
    else ListItem(
        headlineContent = {
            if (file.isDirectory) Text("Audio file is invalid (is directory)")
            else Text("Audio file is not found")
        },
        leadingContent = {
            Icon(Icons.Rounded.Warning, "Error")
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier
            .padding(8.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(8.dp))
    )
}

@Composable
fun ComponentPlaceholder(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")

    val offset by infiniteTransition.animateValue(
        initialValue = 0.dp,
        targetValue = 128.dp,
        label = "offset",
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val tileSize = with(LocalDensity.current) {
        64.dp.toPx()
    }

    val listColors = listOf(
        Color(0xFF707070),
        Color(0xFF909090)
    )

    val gradient = with(LocalDensity.current) {
        Brush.horizontalGradient(
            colors = listColors,
            startX = 0f + offset.toPx(),
            endX = tileSize + offset.toPx(),
            tileMode = TileMode.Mirror
        )
    }

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "placeholderAlpha"
    )

    Box(
        modifier
             .alpha(alpha)
            .background(gradient, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
    )
}

@Composable
fun ColorIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.size(24.dp)
    ) {
        // outer circle for stroke
        drawCircle(
            color = Color.Gray,
            style = Stroke(
                width = 3.0f * density
            ),
            radius = size.minDimension / 3.0f
        )
        // inner circle for fill
        drawCircle(
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Fill,
            radius = size.minDimension / 3.0f
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagPickerDialog(
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
        val haptic = LocalHapticFeedback.current
        val scope = rememberCoroutineScope()

        val listState = rememberLazyListState()

        val db = AppDatabase.getInstance(context)
        val dao = db.appDao()

        val tagList by dao.getAllTags().map {
            it.map { tag -> tag.toHistoryTag() }
        }.collectAsState(emptyList())

        // val selectedItems by dao.getTagIdsMatchingAllEventIds(eventIds)
        //     .collectAsState(emptyList())

        var oldSelected by remember { mutableStateOf<List<String>?>(null) }
        val selectedItems = remember { mutableStateListOf<HistoryTag>() }

        var newTagDialogState by remember { mutableStateOf(false) }

        BackHandler {
            onDismissRequest()
        }

        LaunchedEffect(Unit) {
            oldSelected = dao.getTagIdsMatchingAllEventIds(eventIds)
                .first()
            selectedItems.addAll(tagList.filter { it.id in oldSelected!! })
        }

        val itemOnClick = { tag: HistoryTag ->
             if (selectedItems.contains(tag))
                 selectedItems.remove(tag)
             else selectedItems.add(tag)
        }

        val darkTheme = isSystemInDarkTheme()

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
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                    ) {
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Rounded.Close, "Close")
                        }
                        Text("Select Tag", style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(onClick = { newTagDialogState = true }) {
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
                            modifier = Modifier
                                .clickable {
                                    itemOnClick(tagData)
                                },
                            colors = ListItemDefaults.colors(
                                containerColor =
                                    if (!isSelected)
                                        Color.Transparent
                                    else
                                        MaterialTheme.colorScheme.secondaryContainer,
                                headlineColor =
                                    if (!isSelected)
                                        MaterialTheme.colorScheme.onSurface
                                    else
                                        MaterialTheme.colorScheme.primary,
                                trailingIconColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    else {
                        items(3) {
                            ListItem(
                                leadingContent = {
                                    ComponentPlaceholder(Modifier.size(24.dp))
                                },
                                headlineContent = {
                                    ComponentPlaceholder(Modifier.size(72.dp, 16.dp))
                                }
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
            TagEditorDialog(
                state = newTagDialogState,
                onDismissRequest = { newTagDialogState = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditorDialog(
    state: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    tagId: String? = null
) {
    if (state) BasicAlertDialog(
        onDismissRequest = {},
        modifier = modifier
    ) {
        val context = LocalContext.current
        val haptic = LocalHapticFeedback.current
        val scope = rememberCoroutineScope()

        val db = AppDatabase.getInstance(context)
        val dao = db.appDao()

        var tag by remember { mutableStateOf<TagEntity?>(null) }
        var isLoading by remember { mutableStateOf(true) }

        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var tint by remember { mutableStateOf("#ffffff") }

        val colorController = rememberColorPickerController()

        if (tagId != null) {
            LaunchedEffect(Unit) {
                tag = dao.getTagById(tagId).first()
                name = tag!!.name
                description = tag!!.description
                tint = "#" + tag!!.tint.toArgb().toUInt().toString(16)
                isLoading = false
            }
        }
        else {
            isLoading = false
        }

        val strToColor = {
            tint.removePrefix("#").toLongOrNull(16)?.let {
                Color(0xff000000 + it)
            }
        }

        val isColorValid = strToColor() != null

        var isProcessing by rememberSaveable { mutableStateOf(false) }

        var showColorPicker by remember { mutableStateOf(false) }

        BackHandler {
            onDismissRequest()
        }

        Surface(
            Modifier
                 .clip(RoundedCornerShape(32.dp))
        ) {
            Column(
                Modifier
                    .padding(8.dp)
                    .fillMaxHeight(0.8f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Rounded.Close, "Close")
                    }
                    Text(if (tagId == null) "Add New Tag" else "Edit Tag", style = MaterialTheme.typography.titleLarge)
                }
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val textFieldShape = RoundedCornerShape(16.dp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(96.dp)
                    ) {
                        val dummyTag = HistoryTag(
                            id = "dummy",
                            name = name.ifBlank { "Tag name" },
                            description = description,
                            tint = strToColor() ?: Color.Transparent
                        )
                        TagPreview(
                            tag = dummyTag,
                            darkTheme = false,
                            modifier = Modifier.weight(1f)
                        )
                        TagPreview(
                            tag = dummyTag,
                            darkTheme = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        shape = textFieldShape
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        shape = textFieldShape
                    )
                    OutlinedTextField(
                        value = tint,
                        onValueChange = {
                            tint = it.take(1).filter { c -> c == '#' } + it.removePrefix("#").take(6)
                            strToColor()?.let { c ->
                                colorController.selectByColor(c, false)
                            }
                        },
                        leadingIcon = {
                            if (isColorValid) ColorIcon(
                                color = strToColor()!!,
                            ) else Icon(
                                Icons.Rounded.Close,
                                "Invalid Color",
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showColorPicker = !showColorPicker }) {
                                Icon(
                                    Icons.Rounded.ArrowDropDown,
                                    contentDescription = "Color Picker",
                                    modifier = Modifier.rotate(if (showColorPicker) 180f else 0f)
                                )
                            }
                        },
                        label = { Text("Color") },
                        supportingText = if (!isColorValid) ({
                            Text("Invalid hex color!")
                        }) else null,
                        isError = !isColorValid,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && !showColorPicker,
                        shape = textFieldShape
                    )
                    AnimatedVisibility(
                        visible = showColorPicker,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.height(200.dp)) {
                            Box(Modifier.height(8.dp))
                            HsvColorPicker(
                                controller = colorController,
                                modifier = Modifier
                                    .size(100.dp)
                                    .align(Alignment.CenterHorizontally),
                                onColorChanged = {
                                    if (showColorPicker) tint = "#" + it.hexCode.takeLast(6)
                                },
                                initialColor = strToColor()
                            )
                            Box(Modifier.height(8.dp))
                            BrightnessSlider(
                                controller = colorController,
                                modifier = Modifier
                                    .height(24.dp),
                                borderRadius = 100.dp,
                                borderSize = 0.dp,
                                initialColor = strToColor()
                            )
                        }
                    }

                    Box(Modifier.height(8.dp))

                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Button(
                        enabled = (
                                name.isNotBlank() &&
                                        description.isNotBlank() &&
                                        isColorValid &&
                                        !isLoading
                                ),
                        onClick = {
                            isProcessing = true
                            scope.launch(Dispatchers.IO) {
                                val newTag = TagEntity(
                                    id = tag?.id ?: generateTagId(),
                                    name = name.trim(),
                                    description = description.trim(),
                                    created = ZonedDateTime.now(),
                                    tint = strToColor()!!
                                )
                                dao.insertTag(newTag)
                                onDismissRequest()
                            }
                        }
                    ) {
                        Text(if (tagId == null) "Add" else "Edit")
                    }
                }
            }
        }
    }
}

@Composable
fun TagPreview(
    tag: HistoryTag,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        NanHistoryTheme(
            darkTheme = darkTheme,
            dynamicColor = false
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    TagsView(tags = listOf(tag), darkTheme = darkTheme)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataProcessDialog() {
    val dataProcessType by DataProcessService.ServiceState.operationType.collectAsState()
    val dataProcessStage by DataProcessService.ImportState.stage.collectAsState()
    val migrationName by DataProcessService.ImportState.migrationName.collectAsState()

    val dataProcessProgress by DataProcessService.ServiceState.progress.collectAsState()
    val dataProcessProgressMax by DataProcessService.ServiceState.progressMax.collectAsState()

    BasicAlertDialog(
        // BasicAlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .requiredWidthIn(max = 296.dp)
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val context = LocalContext.current
//                val infiniteTransition = rememberInfiniteTransition(label = "rotation")
//
                val title =
                    if (dataProcessStage == ImportProgressStage.Migrate) {
                        "Updating Data"
                    }
                    else if (dataProcessType == DataProcessService.OPERATION_IMPORT) {
                        "Importing Data"
                    }
                    else {
                        "Processing"
                    }

                val text =
                    if (dataProcessStage == ImportProgressStage.Migrate) {
                        "Migrating data: $migrationName"
                    }
                    else {
                        if (dataProcessStage == ImportProgressStage.Init) "Initializing"
                        else if (dataProcessStage == ImportProgressStage.Decrypt) "Decrypting"
                        else if (dataProcessStage == ImportProgressStage.Extract) "Extracting"
                        else "Importing"
                    }

//                val iconAlpha by infiniteTransition.animateFloat(
//                    initialValue = 0.3f,
//                    targetValue = 1f,
//                    animationSpec = infiniteRepeatable(
//                        animation = tween(durationMillis = 800, easing = LinearEasing),
//                        repeatMode = RepeatMode.Reverse
//                    ),
//                    label = "iconAlpha"
//                )

                val infiniteTransition = rememberInfiniteTransition(label = "gradient")

                val offset by infiniteTransition.animateValue(
                    initialValue = 0.dp,
                    targetValue = 128.dp,
                    label = "offset",
                    typeConverter = Dp.VectorConverter,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 800, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )

                val tileSize = with(LocalDensity.current) {
                    64.dp.toPx()
                }

                val listColors = listOf(
                    Color(0xFF707070),
                    Color(0xFF909090)
                )

                val gradient = with(LocalDensity.current) {
                    Brush.horizontalGradient(
                        colors = listColors,
                        startX = 0f + offset.toPx(),
                        endX = tileSize + offset.toPx(),
                        tileMode = TileMode.Mirror
                    )
                }

                Icon(
                    contentDescription = "Processing Data",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(88.dp)
                        .graphicsLayer(alpha = 0.99f)
                        .drawWithCache {
                            onDrawWithContent {
                                drawContent()
                                drawRect(gradient, blendMode = BlendMode.SrcAtop)
                            }
                        },
                        //.alpha(iconAlpha)
                    painter = painterResource(R.drawable.ic_cloud_download),
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(text, textAlign = TextAlign.Center)
                val primaryColor = MaterialTheme.colorScheme.primary
                val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .padding(vertical = 4.dp)
                ) {
                    drawLine(
                        color = primaryContainerColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = size.height,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = primaryColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width * dataProcessProgress / dataProcessProgressMax, 0f),
                        strokeWidth = size.height,
                        cap = StrokeCap.Round
                    )
                }
                if (dataProcessType == DataProcessService.OPERATION_IMPORT) Button(
                    enabled = !(
                        dataProcessStage == ImportProgressStage.Done ||
                        dataProcessStage == ImportProgressStage.Extract ||
                        dataProcessStage == ImportProgressStage.Migrate
                    ),
                    onClick = {
                        val cancelIntent = Intent(context, DataProcessService::class.java).apply {
                            action = DataProcessService.ACTION_CANCEL_SERVICE
                        }
                        context.startService(cancelIntent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Cancel")
                }
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.End
//                ) {
//                    Text("${round(migrationState.progress * 100)}%")
//                }
            }
        }
    }
}

@Composable
fun SelectableButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val transitionDuration = 1000

    val cornerRadius by animateDpAsState(
        targetValue = if (selected) 64.dp else 12.dp,
        label = "cornerRadius"
    )

    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.containerColor else Color.Transparent,
        label = "bgColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.contentColor else MaterialTheme.colorScheme.primary,
        label = "contentColor"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (selected) 0.dp else 1.dp,
        label = "borderWidth"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Button(
        onClick = onClick,
        content = content,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = modifier,
        border = BorderStroke(
            width = borderWidth,
            color = Color.Gray
        ),
        enabled = enabled,
        elevation = elevation,
        contentPadding = contentPadding,
        interactionSource = interactionSource
    )

//    Surface(
//        shape = shape,
//        modifier = Modifier
//            .clip
//            .clickable { onClick() },
//        color = containerColor,
//        contentColor = contentColor,
//        border = BorderStroke(
//            width = borderWidth,
//            color = Color.Gray
//        )
//    ) {
//        Row(content = content, modifier = Modifier.padding(contentPadding))
//    }
}