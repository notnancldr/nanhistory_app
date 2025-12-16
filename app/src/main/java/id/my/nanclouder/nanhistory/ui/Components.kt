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
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import id.my.nanclouder.nanhistory.ImportProgressStage
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.TagDetailActivity
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.DayTagCrossRef
import id.my.nanclouder.nanhistory.db.EventTagCrossRef
import id.my.nanclouder.nanhistory.db.TagEntity
import id.my.nanclouder.nanhistory.db.toHistoryTag
import id.my.nanclouder.nanhistory.utils.history.HistoryTag
import id.my.nanclouder.nanhistory.utils.history.generateTagId
import id.my.nanclouder.nanhistory.utils.readableTimeHours
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
import kotlin.math.round

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
    val newUI = Config.appearanceNewUI.getCache()
    if (newUI) AudioPlayer_New(path)
    else AudioPlayer_Old(path)
}

@Composable
fun AudioPlayer_Old(path: String) {
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
fun AudioPlayer_New(path: String) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var recordDuration by remember { mutableIntStateOf(0) }
    var userIsSeeking by remember { mutableStateOf(false) }
    var audioAvailable by remember { mutableStateOf(true) }

    val context = LocalContext.current

    val mediaPlayer = remember {
        if (android.os.Build.VERSION.SDK_INT >= 34) MediaPlayer(context).apply {
            try {
                setDataSource(path)
                prepare()
            } catch (e: IOException) {
                audioAvailable = false
            }
        } else {
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
            delay(100L)
        }
    }

    DisposableEffect(Unit) {
        mediaPlayer?.setOnCompletionListener {
            isPlaying = false
            progress = 0
        }

        onDispose {
            mediaPlayer?.release()
        }
    }

    val file = File(path)
    val progressSeconds = Duration.ofMillis(progress.toLong())
    val durationSeconds = Duration.ofMillis(recordDuration.toLong())
    val progressPercent = if (recordDuration > 0) (progress.toFloat() / recordDuration) * 100 else 0f

    if (file.isFile) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play/Pause Button
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        tonalElevation = 4.dp
                    ) {
                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                if (!isPlaying) painterResource(R.drawable.ic_play_arrow_filled)
                                else painterResource(R.drawable.ic_pause_filled),
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Time Display
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${readableTimeHours(progressSeconds)} / ${readableTimeHours(durationSeconds)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${round(progressPercent)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Progress Slider with custom styling
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        valueRange = 0f..recordDuration.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .padding(4.dp)
                    )
//
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween
//                    ) {
//                        Text(
//                            readableTimeHours(progressSeconds),
//                            style = MaterialTheme.typography.labelSmall,
//                            color = MaterialTheme.colorScheme.outline
//                        )
//                        Text(
//                            readableTimeHours(durationSeconds),
//                            style = MaterialTheme.typography.labelSmall,
//                            color = MaterialTheme.colorScheme.outline
//                        )
//                    }
                }
            }
        }
    } else {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.errorContainer,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    color = MaterialTheme.colorScheme.error
                ) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "Audio file unavailable",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        if (file.isDirectory) "The audio file is a directory"
                        else "Audio file not found at path",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun ComponentPlaceholder(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")

    val width = 128.dp

    val offset by infiniteTransition.animateValue(
        initialValue = 0.dp,
        targetValue = width * 2,
        label = "offset",
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val tileSize = with(LocalDensity.current) {
        width.toPx()
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

@Composable
fun TagPickerDialog(
    state: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    eventIds: List<String> = emptyList(),
    dayDates: List<LocalDate> = emptyList(),
) {
    val newUI = Config.appearanceNewUI.get(LocalContext.current)
    if (newUI) {
        TagPickerDialog_New(
            state = state,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            eventIds = eventIds,
            dayDates = dayDates
        )
    }
    else {
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
        val haptic = LocalHapticFeedback.current
        val scope = rememberCoroutineScope()

        val listState = rememberLazyListState()

        val db = AppDatabase.getInstance(context)
        val dao = db.appDao()

        val tagList by dao.getAllTags().map {
            it.map { tag -> tag.toHistoryTag() }
        }.collectAsState(emptyList())

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
                // Header
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
                        onClick = { newTagDialogState = true },
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

                // Divider
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Tags List
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
                                .clickable {
                                    itemOnClick(tagData)
                                },
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
                                // Tag Color Indicator
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            color = tagData.tint,
                                            shape = RoundedCornerShape(3.dp)
                                        )
                                )

                                // Tag Name
                                Text(
                                    text = tagData.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected)
                                        tagData.tint
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                // Checkmark
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
                    }
                    else {
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

                // Divider
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Action Buttons
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

            TagEditorDialog(
                state = newTagDialogState,
                onDismissRequest = { newTagDialogState = false }
            )
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

@Composable
fun TagEditorDialog(
    state: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    tagId: String? = null
) {
    val newUI = Config.appearanceNewUI.get(LocalContext.current)
    if (newUI) {
        TagEditorDialog_New(
            state = state,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            tagId = tagId
        )
    }
    else {
        TagEditorDialog_Old(
            state = state,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            tagId = tagId
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditorDialog_New(
    state: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    tagId: String? = null
) {
    if (state) BasicAlertDialog(
        onDismissRequest = {},
        modifier = modifier.fillMaxWidth(0.9f)
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
                .clip(RoundedCornerShape(24.dp))
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                Modifier
                    .fillMaxHeight(0.9f)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        if (tagId == null) "Create Tag" else "Edit Tag",
                        style = MaterialTheme.typography.headlineSmall
                    )
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

                // Divider
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Content
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Tag Preview
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
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

                    // Name Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tag Name") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    // Description Field
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    // Color Field
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tint,
                            onValueChange = {
                                tint = it.take(1).filter { c -> c == '#' } + it.removePrefix("#").take(6)
                                strToColor()?.let { c ->
                                    colorController.selectByColor(c, false)
                                }
                            },
                            leadingIcon = {
                                if (isColorValid) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                color = strToColor()!!,
                                                shape = RoundedCornerShape(3.dp)
                                            )
                                    )
                                } else {
                                    Icon(
                                        Icons.Rounded.Close,
                                        "Invalid Color",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showColorPicker = !showColorPicker },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.ArrowDropDown,
                                        contentDescription = "Color Picker",
                                        modifier = Modifier.rotate(if (showColorPicker) 180f else 0f),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            label = { Text("Color (Hex)") },
                            supportingText = if (!isColorValid) ({
                                Text(
                                    "Invalid hex color",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }) else null,
                            isError = !isColorValid,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading && !showColorPicker,
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )

                        // Color Picker
                        AnimatedVisibility(
                            visible = showColorPicker,
                            modifier = Modifier.fillMaxWidth(),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    HsvColorPicker(
                                        controller = colorController,
                                        modifier = Modifier.size(100.dp),
                                        onColorChanged = {
                                            if (showColorPicker) tint = "#" + it.hexCode.takeLast(6)
                                        },
                                        initialColor = strToColor()
                                    )
                                    BrightnessSlider(
                                        controller = colorController,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp),
                                        borderRadius = 100.dp,
                                        borderSize = 0.dp,
                                        initialColor = strToColor()
                                    )
                                }
                            }
                        }
                    }
                }

                // Divider
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Action Buttons
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
                        enabled = (
                                name.isNotBlank() &&
                                        description.isNotBlank() &&
                                        isColorValid &&
                                        !isLoading &&
                                        !isProcessing
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
                        },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(if (tagId == null) "Create" else "Save")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditorDialog_Old(
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

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer

    BasicAlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val context = LocalContext.current

                val title = when {
                    dataProcessStage == ImportProgressStage.Migrate -> "Updating Data"
                    dataProcessType == DataProcessService.OPERATION_IMPORT -> "Importing Data"
                    else -> "Processing"
                }

                val text = when {
                    dataProcessStage == ImportProgressStage.Migrate -> "Migrating: $migrationName"
                    dataProcessStage == ImportProgressStage.Init -> "Initializing..."
                    dataProcessStage == ImportProgressStage.Decrypt -> "Decrypting your backup..."
                    dataProcessStage == ImportProgressStage.Extract -> "Extracting files..."
                    else -> "Importing data..."
                }

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

                val gradientColors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )

                val gradient = with(LocalDensity.current) {
                    Brush.horizontalGradient(
                        colors = gradientColors,
                        startX = 0f + offset.toPx(),
                        endX = tileSize + offset.toPx(),
                        tileMode = TileMode.Mirror
                    )
                }

                // Animated Icon with Glow Effect
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        contentDescription = "Processing Data",
                        painter = painterResource(R.drawable.ic_cloud_download),
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer(alpha = 0.99f)
                            .drawWithCache {
                                onDrawWithContent {
                                    drawContent()
                                    drawRect(gradient, blendMode = BlendMode.SrcAtop)
                                }
                            },
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Box(modifier = Modifier.height(24.dp))

                // Title
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(modifier = Modifier.height(24.dp))

                // Progress Bar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Percentage Text
                    val progressPercentage = if (dataProcessProgressMax > 0) {
                        ((dataProcessProgress * 100f) / dataProcessProgressMax).toInt()
                    } else {
                        0
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            getProgressLabel(dataProcessStage),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$progressPercentage%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Custom Progress Bar
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                    ) {

                        // Background
                        drawLine(
                            color = primaryContainerColor,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = size.height,
                            cap = StrokeCap.Round
                        )

                        // Progress
                        if (dataProcessProgressMax > 0) {
                            drawLine(
                                color = primaryColor,
                                start = Offset(0f, size.height / 2),
                                end = Offset(size.width * (dataProcessProgress.toFloat() / dataProcessProgressMax), size.height / 2),
                                strokeWidth = size.height,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                Box(modifier = Modifier.height(24.dp))

                // Cancel Button
                if (dataProcessType == DataProcessService.OPERATION_IMPORT) {
                    val isCancelDisabled = dataProcessStage == ImportProgressStage.Done ||
                            dataProcessStage == ImportProgressStage.Extract ||
                            dataProcessStage == ImportProgressStage.Migrate

                    Button(
                        enabled = !isCancelDisabled,
                        onClick = {
                            val cancelIntent = Intent(context, DataProcessService::class.java).apply {
                                action = DataProcessService.ACTION_CANCEL_SERVICE
                            }
                            context.startService(cancelIntent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            "Cancel",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

private fun getProgressLabel(stage: ImportProgressStage?): String = when (stage) {
    ImportProgressStage.Init -> "Initializing..."
    ImportProgressStage.Decrypt -> "Decrypting backup..."
    ImportProgressStage.Extract -> "Extracting files..."
    ImportProgressStage.Migrate -> "Updating data..."
    else -> "Processing..."
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

class TagDetailDialogState {
    private var _isOpen by mutableStateOf(false)
    private var _tagId by mutableStateOf<String?>(null)

    val isOpen get() = _isOpen
    val tagId get() = _tagId

    fun open(tagId: String) {
        _tagId = tagId
        _isOpen = true
    }

    fun close() {
        _isOpen = false
        _tagId = null
    }
}

@Composable
fun rememberTagDetailDialogState() =
    remember { TagDetailDialogState() }

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
                Modifier
                    .padding(24.dp),
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
                    text = "${
                        if (useCount == 0) "No" else useCount
                    } event${if (useCount == 1) "" else "s"} using this tag",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleSmall
                )
                TextButton(
                    onClick = {
                        state.close()
                        val intent = Intent(context, TagDetailActivity::class.java)
                        intent.putExtra("tagId", tagData.id)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Detail")
                }
            }
            else ComponentPlaceholder(Modifier.fillMaxWidth().height(128.dp))
        }

    }
}