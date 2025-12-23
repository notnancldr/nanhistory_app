package id.my.nanclouder.nanhistory

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toHistoryEvent
import id.my.nanclouder.nanhistory.utils.Coordinate
import id.my.nanclouder.nanhistory.utils.DateFormatter
import id.my.nanclouder.nanhistory.utils.HistoryLocationData
import id.my.nanclouder.nanhistory.utils.TimeFormatter
import id.my.nanclouder.nanhistory.utils.getLocationData
import id.my.nanclouder.nanhistory.utils.history.EventPoint
import id.my.nanclouder.nanhistory.utils.history.EventRange
import id.my.nanclouder.nanhistory.utils.history.generateEventId
import id.my.nanclouder.nanhistory.utils.history.getFilePathFromDate
import id.my.nanclouder.nanhistory.utils.history.validateSignature
import id.my.nanclouder.nanhistory.utils.shareFile
import id.my.nanclouder.nanhistory.service.RecordService
import id.my.nanclouder.nanhistory.ui.AudioPlayer
import id.my.nanclouder.nanhistory.ui.ComponentPlaceholder
import id.my.nanclouder.nanhistory.ui.CountdownTimerDialog
import id.my.nanclouder.nanhistory.ui.MentionModalHandler
import id.my.nanclouder.nanhistory.ui.MentionModalState
import id.my.nanclouder.nanhistory.ui.TagDetailDialog
import id.my.nanclouder.nanhistory.ui.TagPickerDialog
import id.my.nanclouder.nanhistory.ui.rememberTagDetailDialogState
import id.my.nanclouder.nanhistory.ui.tags.TagsView
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.round
import kotlin.math.roundToInt
import id.my.nanclouder.nanhistory.utils.FormattedText
import id.my.nanclouder.nanhistory.utils.MapShareDialog
import id.my.nanclouder.nanhistory.utils.history.LocationData
import id.my.nanclouder.nanhistory.utils.history.TransportationType
import id.my.nanclouder.nanhistory.utils.transportModel.detectTransportMode
import id.my.nanclouder.nanhistory.utils.simplifyPoints
import id.my.nanclouder.nanhistory.utils.toGeoPoint
import id.my.nanclouder.nanhistory.utils.transportModel.CalibrationModel
import id.my.nanclouder.nanhistory.utils.transportModel.CalibrationParamsPreview
import id.my.nanclouder.nanhistory.utils.transportModel.TrainingResult
import id.my.nanclouder.nanhistory.utils.transportModel.TransportMode
import id.my.nanclouder.nanhistory.utils.transportModel.getScore
import id.my.nanclouder.nanhistory.utils.transportModel.loadCalibrationModels
import id.my.nanclouder.nanhistory.utils.transportModel.recalibrateModels
import id.my.nanclouder.nanhistory.utils.transportModel.resetModels
import id.my.nanclouder.nanhistory.utils.transportModel.saveCalibrationModels
import id.my.nanclouder.nanhistory.utils.transportModel.toTransportMode
import id.my.nanclouder.nanhistory.utils.transportModel.train
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class EventDetailActivity : ComponentActivity() {
    var update: () -> Unit = {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val eventId = intent.getStringExtra("eventId") ?: generateEventId()
        val path = intent.getStringExtra("path") ?: "NULL!"
        setContent {
            var setUpdate by remember { mutableStateOf(false) }
//            update = { setUpdate = !setUpdate }
            NanHistoryTheme {
                DetailContent(eventId, path)
            }
        }
    }

//    override fun onResume() {
//        super.onResume()
//        update()
//    }
}

fun Context.getActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}


@Composable
fun DetailContent(eventId: String, path: String) {
    val newUI = Config.appearanceNewUI.getCache()
    if (newUI) DetailContent_New(eventId, path)
    else DetailContent_Old(eventId, path)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailContent_Old(eventId: String, path: String) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val tagDetailDialogState = rememberTagDetailDialogState()

    var showSignatureInfo by rememberSaveable { mutableStateOf(false) }
    var deleteDialogState by rememberSaveable { mutableStateOf(false) }

    var showInfo by remember { mutableStateOf(false) }

    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

    val eventState by dao.getEventFlowById(eventId).collectAsState(null)
    val eventData = eventState?.toHistoryEvent()

    var eventNotFound by remember { mutableStateOf(false) }

    val recordEventId by RecordService.RecordState.eventId.collectAsState()
    val recordRunning by RecordService.RecordState.isRecording.collectAsState()
    val recording = recordEventId == eventData?.id && recordRunning

    var tagDialogState by remember { mutableStateOf(false) }

    // EventRange only
    val duration = when (eventData) {
        is EventPoint -> null
        is EventRange -> eventData.end.toEpochSecond() - eventData.time.toEpochSecond()
        else -> null
    } ?: 0

    val favorite = eventData?.favorite ?: false

    var eventLocations by remember {
        mutableStateOf<List<LocationData>>(emptyList())
    }
    val locationAvailable = eventData?.locationPath != null

    var locationData by remember {
        mutableStateOf<List<HistoryLocationData>>(listOf())
    }

    LaunchedEffect(eventData) {
        eventLocations = eventData?.getLocations(context) ?: emptyList()
        locationData = eventLocations.getLocationData()
    }

    TagDetailDialog(tagDetailDialogState)

    if (!eventNotFound) Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    if (eventData != null) Column {
                        Text(
                            eventData.title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    else Column {
                        ComponentPlaceholder(
                            Modifier
                                .width(196.dp)
                                .height(24.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            context.getActivity()!!.finish()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (eventData != null) {
                        IconButton(
                            onClick = {
                                scope.launch { dao.toggleFavoriteEvent(eventId) }
                            },
                            enabled = !recording
                        ) {
                            if (favorite) Icon(
                                painterResource(R.drawable.ic_favorite_filled), "",
                                tint = Color(0xFFFF7070)
                            )
                            else Icon(
                                painterResource(R.drawable.ic_favorite), ""
                            )
                        }
                        IconButton(
                            onClick = {
                                val intent = Intent(context, EditEventActivity::class.java)
                                intent.putExtra("eventId", eventId)
                                context.startActivity(intent)
                            },
                            enabled = !recording
                        ) {
                            Icon(Icons.Rounded.Edit, "Edit")
                        }

                        var menuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuExpanded = !menuExpanded }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Info, "Info", tint = Color.Gray)
                                    },
                                    text = { Text("Info") },
                                    onClick = { showInfo = true; menuExpanded = false }
                                )
                                if (eventData.audio != null) DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Share, "Share audio", tint = Color.Gray)
                                    },
                                    text = { Text("Share Audio") },
                                    onClick = {
                                        shareFile(
                                            context,
                                            "audio/" + eventData.audio!!.removePrefix(context.filesDir.absolutePath)
                                        )
                                        menuExpanded = false
                                    }
                                )
                                if (eventData is EventRange) DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            painterResource(R.drawable.ic_content_cut),
                                            contentDescription = "Cut Event",
                                            tint = Color.Gray
                                        )
                                    },
                                    text = { Text("Cut Event") },
                                    onClick = {
                                        menuExpanded = false
                                        val intent =
                                            Intent(context, EventLocationActivity::class.java)
                                                .apply {
                                                    putExtra("eventId", eventId)
                                                    putExtra("cutMode", true)
                                                }
                                        context.startActivity(intent)
                                        // TODO
                                    },
                                    enabled = !recording
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            painterResource(R.drawable.ic_delete), "Delete"
                                        )
                                    },
                                    text = {
                                        Text("Delete")
                                    },
                                    onClick = { deleteDialogState = true; menuExpanded = false },
                                    enabled = !recording,
                                    colors = MenuItemColors(
                                        textColor = MaterialTheme.colorScheme.error,
                                        leadingIconColor = MaterialTheme.colorScheme.error,
                                        trailingIconColor = MaterialTheme.colorScheme.error,
                                        disabledTextColor = Color.Gray,
                                        disabledLeadingIconColor = Color.Gray,
                                        disabledTrailingIconColor = Color.Gray
                                    )
                                )
                            }
                        }
                    } else ComponentPlaceholder(
                        Modifier
                            .size(40.dp)
                            .padding(8.dp)
                    )
                },
                colors = if (recording) TopAppBarDefaults.largeTopAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.error
                ) else TopAppBarDefaults.largeTopAppBarColors()
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (eventData != null && !recording) {
                    if (eventData.tags.isNotEmpty()) {
                        val isTagMoreThanOne = eventData.tags.size > 1
                        Text(
                            "${eventData.tags.size} tag${if (isTagMoreThanOne) "s" else ""}",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(
                            onClick = {
                                tagDialogState = true
                            },
                            colors = IconButtonDefaults.filledIconButtonColors()
                        ) {
                            if (eventData.tags.isNotEmpty()) Icon(Icons.Rounded.Edit, "Edit tag")
                            else Icon(Icons.Rounded.Add, "Add tag")
                        }
                        VerticalDivider()
                        if (eventData.tags.isNotEmpty()) TagsView(
                            eventData.tags,
                            limit = Int.MAX_VALUE,
                            wrap = true,
                            tagDetailDialogState = tagDetailDialogState
                        )
                        else Text(
                            "No tags",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    ComponentPlaceholder(
                        Modifier
                            .width(54.dp)
                            .height(18.dp)
                            .padding(vertical = 8.dp)
                    )
                    ComponentPlaceholder(
                        Modifier
                            .width(164.dp)
                            .height(72.dp)
                            .padding(vertical = 8.dp)
                    )
                    // ComponentPlaceholder(Modifier
                    //     .width(64.dp)
                    //     .height(16.dp))
                }
            }
            Box(Modifier.height(8.dp))
            if (locationAvailable) {
                Box(
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    if (eventLocations.isNotEmpty()) {
                        MapHistoryPreview(
                            locations = eventLocations,
                            modifier = Modifier
                                .height(200.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    val intent = Intent(context, EventLocationActivity::class.java)
                                    intent.putExtra("eventId", eventData!!.id)
                                    intent.putExtra(
                                        "path",
                                        getFilePathFromDate(eventData.time.toLocalDate())
                                    )
                                    context.startActivity(intent)
                                }
                        )
                    } else ComponentPlaceholder(
                        Modifier.fillMaxSize()
                    )
                }
            }


            if (eventData != null) {
                Column(
                    Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    if (eventData is EventRange && eventLocations.size > 1) {
                        val maxSpeed = round(locationData.maxOf { it.speed } * 10) / 10
                        val distance = locationData.sumOf { item -> item.distance.toDouble() }
                        // In Km/h
                        val avgSpeed =
                            try { (distance / 100f / (duration / 3600f)).roundToInt() / 10f }
                            catch (e: Exception) { 0f }

                        ListItem(
                            headlineContent = {
                                Text("Location details")
                            },
                            supportingContent = {
                                Column {
                                    Text("Average speed: $avgSpeed Km/h")
                                    Text("Max speed: $maxSpeed Km/h")
                                    Text(
                                        "Total distance: " +
                                                if (distance < 1000) "${round(distance).toInt()} m"
                                                else "${round(distance / 100) / 10} Km"
                                    )
                                }
                            }
                        )
                    }

                    if (eventData.signature.isNotBlank()) {
                        val valid = remember { eventData.validateSignature(context = context) }
                        Surface(
                            Modifier.clickable {
                                showSignatureInfo = !showSignatureInfo
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (valid) Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = "Signed",
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .size(16.dp),
                                        tint = Color(0xFF008000)
                                    ) else Icon(
                                        Icons.Rounded.Warning,
                                        contentDescription = "Invalid signature",
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .size(16.dp),
                                        tint = Color(0xFF808000)
                                    )
                                    Text(
                                        text =
                                            if (valid) "This event is signed"
                                            else "The signature of this event is invalid",
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                                AnimatedVisibility(visible = showSignatureInfo) {
                                    Text(
                                        """Signature info:
                                 |Signature: ${eventData.signature}
                                 |Valid: $valid
                                 """.trimMargin(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .padding(start = 32.dp)
                                            .padding(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (eventData.audio != null)
                        AudioPlayer("${context.filesDir.absolutePath}/audio/${eventData.audio}")
                }

                ListItem(
                    leadingContent = {
                        Icon(painterResource(R.drawable.ic_title), "Title")
                    },
                    overlineContent = {
                        Text("Title")
                    },
                    headlineContent = {
                        Text(eventData.title)
                    },
                )
                ListItem(
                    leadingContent = {
                        Icon(painterResource(R.drawable.ic_info), "Description")
                    },
                    overlineContent = {
                        Text("Description")
                    },
                    headlineContent = {
                        if (eventData.description.isNotBlank())
                            Text(eventData.description)
                        else
                            Text("Empty", fontStyle = FontStyle.Italic)
                    },
                )
                ListItem(
                    leadingContent = {
                        Icon(painterResource(R.drawable.ic_schedule), "Time")
                    },
                    overlineContent = {
                        if (eventData !is EventRange) Text("Time")
                        else Text("Start")
                    },
                    headlineContent = {
                        Text(
                            eventData.time.format(DateFormatter) + " " +
                                    eventData.time.format(TimeFormatter) +
                                    if (eventData.time.second != 0) ":${
                                        eventData.time.second.toString().padStart(2, '0')
                                    }" else ""
                        )
                    },
                )
                if (eventData is EventRange) {
                    ListItem(
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.ic_arrow_range),
                                contentDescription = "Range",
                                modifier = Modifier.rotate(90f),
                                tint = Color.Gray
                            )
                        },
                        headlineContent = {
                            Row {
                                val durationObj = Duration.ofSeconds(duration)
                                val days = durationObj.toDays()
                                val hours = durationObj.toHoursPart()
                                val minutes = durationObj.toMinutesPart()
                                val seconds = durationObj.toSecondsPart()

                                var text = ""
                                if (days > 0) text += "${days}d "
                                if (hours > 0) text += "${hours}h "
                                if (minutes > 0) text += "${minutes}m "
                                if (seconds > 0) text += "${seconds}s"

                                if (eventData.transportationType.iconId != null) {
                                    // Text(
                                    //     "(${eventData.transportationType.name})",
                                    //     color = Color.Gray
                                    // )
                                    Icon(
                                        painterResource(eventData.transportationType.iconId!!),
                                        eventData.transportationType.name,
                                        tint = Color.Gray
                                    )
                                    Box(Modifier.width(4.dp))
                                }
                                Text(text, color = Color.Gray)
                            }
                        },
                        modifier = Modifier.height(40.dp)
                    )
                    ListItem(
                        leadingContent = {
                            Icon(painterResource(R.drawable.ic_schedule), "Time")
                        },
                        overlineContent = {
                            Text("End")
                        },
                        headlineContent = {
                            Text(
                                eventData.end.format(DateFormatter) + " " +
                                        eventData.end.format(TimeFormatter) +
                                        if (eventData.end.second != 0) ":${
                                            eventData.end.second.toString().padStart(2, '0')
                                        }" else ""
                            )
                        },
                    )
                }
            }

            val sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = false
            )

            if (showInfo && eventData != null) ModalBottomSheet(
                modifier = Modifier
                    .fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surface,
                sheetState = sheetState,
                onDismissRequest = {
                    showInfo = false
                }
            ) {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())) {
                    val timeFormat = { time: ZonedDateTime ->
                        "${time.format(DateFormatter)} ${time.format(TimeFormatter)}"
                    }

                    ListItem(
                        headlineContent = {
                            Text(
                                "Event Info",
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        leadingContent = {
                            Icon(Icons.Rounded.Info, "Info")
                        }
                    )

                    ListItem(
                        overlineContent = {
                            Text("Created")
                        },
                        headlineContent = {
                            Text(eventData.created.let(timeFormat))
                        }
                    )
                    ListItem(
                        overlineContent = {
                            Text("Last modified")
                        },
                        headlineContent = {
                            Text(eventData.modified.let(timeFormat))
                        }
                    )
                    ListItem(
                        overlineContent = {
                            Text("Event ID")
                        },
                        headlineContent = {
                            Text(eventData.id)
                        }
                    )
                    ListItem(
                        overlineContent = {
                            Text("Audio")
                        },
                        headlineContent = {
                            Text(eventData.audio ?: "-")
                        }
                    )
                    ListItem(
                        overlineContent = {
                            Text("Location data")
                        },
                        headlineContent = {
                            Text(eventData.locationPath ?: "-")
                        }
                    )
                    if (eventData is EventRange) ListItem(
                        overlineContent = {
                            Text("Location points")
                        },
                        headlineContent = {
                            Text(eventLocations.size.toString())
                        }
                    )
                    if (eventData.metadata.isNotEmpty()) {
                        Box(Modifier.height(8.dp))
                        ListItem(
                            headlineContent = { Text("Metadata", style = MaterialTheme.typography.titleMedium) }
                        )

                        for ((name, value) in eventData.metadata) {
                            ListItem(
                                overlineContent = {
                                    Text(name)
                                },
                                headlineContent = {
                                    if (value !is Double) Text(value.toString())
                                    else if (value % 1.0 == 0.0) Text(value.toInt().toString())
                                    else Text(value.toString())
                                }
                            )
                        }
                    }
                }
            }

            if (eventData != null) TagPickerDialog(
                state = tagDialogState,
                eventIds = listOf(eventData.id),
                onDismissRequest = {
                    tagDialogState = false
                }
            )

            if (deleteDialogState && eventData != null) {
                AlertDialog(
                    icon = {
                        Icon(Icons.Rounded.Warning, "Warning")
                    },
                    onDismissRequest = {
                        deleteDialogState = false
                    },
                    title = {
                        Text("Delete '${eventData.title.replace("\n", " ")}'?")
                    },
                    text = {
                        Text("This event will be moved into trash bin and will be deleted permanently" +
                                "after 30 days.")
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                deleteDialogState = false
                            }
                        ) {
                            Text("Cancel")
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch { AppDatabase.moveToTrash(dao, context, listOf(eventData.id)) }

                                context.getActivity()?.run {
                                    finish()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF910C0C),
                                contentColor = Color.White,
                                disabledContainerColor = Color.Gray,
                                disabledContentColor = Color.White
                            ),
                        ) {
                            Text("Delete")
                        }
                    }
                )
            }
        }
    }
    else Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text("Event not found")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            context.getActivity()?.finish()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues)
                .padding(PaddingValues(16.dp))
        ) {
            Text("Event not found")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailContent_New(eventId: String, path: String) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val tagDetailDialogState = rememberTagDetailDialogState()

    var showSignatureInfo by rememberSaveable { mutableStateOf(false) }
    var deleteDialogState by rememberSaveable { mutableStateOf(false) }
    var showShareMapDialog by rememberSaveable { mutableStateOf(false) }

    var showInfo by remember { mutableStateOf(false) }

    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

    val eventState by dao.getEventFlowById(eventId).collectAsState(null)
    val eventData = eventState?.toHistoryEvent()

    var eventNotFound by remember { mutableStateOf(false) }

    val recordEventId by RecordService.RecordState.eventId.collectAsState()
    val recordRunning by RecordService.RecordState.isRecording.collectAsState()
    val recording = recordEventId == eventData?.id && recordRunning

    var tagDialogState by remember { mutableStateOf(false) }

    val duration = when (eventData) {
        is EventPoint -> null
        is EventRange -> eventData.end.toEpochSecond() - eventData.time.toEpochSecond()
        else -> null
    } ?: 0

    val favorite = eventData?.favorite ?: false

    var eventLocations by remember {
        mutableStateOf<List<LocationData>>(emptyList())
    }
    val locationAvailable = eventData?.locationPath != null

    var locationData by remember {
        mutableStateOf<List<HistoryLocationData>>(listOf())
    }

    val mentionModalState = remember { mutableStateOf(MentionModalState()) }

    LaunchedEffect(eventData) {
        eventLocations = eventData?.getLocations(context) ?: emptyList()
        locationData = eventLocations.getLocationData()
    }

    TagDetailDialog(tagDetailDialogState)

    if (!eventNotFound) Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    if (eventData != null) Column {
                        FormattedText(
                            eventData.title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.headlineSmall,
                            mentionModalState = mentionModalState
                        )
                    }
                    else Column {
                        ComponentPlaceholder(
                            Modifier
                                .width(196.dp)
                                .height(24.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            context.getActivity()!!.finish()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (eventData != null) {
                        IconButton(
                            onClick = {
                                scope.launch { dao.toggleFavoriteEvent(eventId) }
                            },
                            enabled = !recording
                        ) {
                            if (favorite) Icon(
                                painterResource(R.drawable.ic_favorite_filled), "",
                                tint = Color(0xFFFF7070)
                            )
                            else Icon(
                                painterResource(R.drawable.ic_favorite), ""
                            )
                        }
                        IconButton(
                            onClick = {
                                val intent = Intent(context, EditEventActivity::class.java)
                                intent.putExtra("eventId", eventId)
                                context.startActivity(intent)
                            },
                            enabled = !recording
                        ) {
                            Icon(Icons.Rounded.Edit, "Edit")
                        }

                        var menuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuExpanded = !menuExpanded }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Info, "Info", tint = MaterialTheme.colorScheme.primary)
                                    },
                                    text = { Text("Info") },
                                    onClick = { showInfo = true; menuExpanded = false }
                                )
                                if (eventData.audio != null) DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Share, "Share audio", tint = MaterialTheme.colorScheme.primary)
                                    },
                                    text = { Text("Share Audio") },
                                    onClick = {
                                        shareFile(
                                            context,
                                            "audio/" + eventData.audio!!.removePrefix(context.filesDir.absolutePath)
                                        )
                                        menuExpanded = false
                                    }
                                )
                                if (eventData is EventRange) DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            painterResource(R.drawable.ic_content_cut),
                                            contentDescription = "Cut Event",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    text = { Text("Cut Event") },
                                    onClick = {
                                        menuExpanded = false
                                        val intent =
                                            Intent(context, EventLocationActivity::class.java)
                                                .apply {
                                                    putExtra("eventId", eventId)
                                                    putExtra("cutMode", true)
                                                }
                                        context.startActivity(intent)
                                    },
                                    enabled = !recording
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            painterResource(R.drawable.ic_delete), "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    text = {
                                        Text("Delete", color = MaterialTheme.colorScheme.error)
                                    },
                                    onClick = { deleteDialogState = true; menuExpanded = false },
                                    enabled = !recording
                                )
                            }
                        }
                    } else ComponentPlaceholder(
                        Modifier
                            .size(40.dp)
                            .padding(8.dp)
                    )
                },
                colors = if (recording) TopAppBarDefaults.largeTopAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.error
                ) else TopAppBarDefaults.largeTopAppBarColors()
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {

            if (recording) {
                RecordingIndicatorBar()
            }

            if (eventData != null) Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var showCountdownDialog by rememberSaveable { mutableStateOf(false) }
                if (eventData is EventRange && eventLocations.isNotEmpty()) {

                    Button(
                        onClick = {
                            showShareMapDialog = true
                        },
                        modifier = Modifier
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Share,
                            "Share",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Share")
                    }
                }

                Button(
                    onClick = {
                        showCountdownDialog = true
                    },
                    modifier = Modifier
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        Icons.Rounded.Schedule,
                        "Timer",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Timer")
                }

                CountdownTimerDialog(
                    eventStart = eventData.time,
                    eventEnd = (eventData as? EventRange)?.end,
                    showDialog = showCountdownDialog,
                    onDismiss = { showCountdownDialog = false }
                )
            }

            // Tags Section
            if (eventData != null && !recording) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (eventData.tags.isNotEmpty()) {
                            val isTagMoreThanOne = eventData.tags.size > 1
                            Text(
                                "${eventData.tags.size} tag${if (isTagMoreThanOne) "s" else ""}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                tonalElevation = 4.dp
                            ) {
                                IconButton(
                                    onClick = {
                                        tagDialogState = true
                                    },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        if (eventData.tags.isNotEmpty()) Icons.Rounded.Edit else Icons.Rounded.Add,
                                        if (eventData.tags.isNotEmpty()) "Edit tag" else "Add tag",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            if (eventData.tags.isNotEmpty()) TagsView(
                                eventData.tags,
                                limit = Int.MAX_VALUE,
                                wrap = true,
                                tagDetailDialogState = tagDetailDialogState,
                            )
                            else Text(
                                "No tags yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Map Section
            if (locationAvailable) {
                Box(
                    modifier = Modifier
                        .height(220.dp)
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    if (eventLocations.isNotEmpty()) {
                        MapHistoryPreview(
                            locations = eventLocations,
                            modifier = Modifier.height(220.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    val intent = Intent(context, EventLocationActivity::class.java)
                                    intent.putExtra("eventId", eventData!!.id)
                                    intent.putExtra(
                                        "path",
                                        getFilePathFromDate(eventData.time.toLocalDate())
                                    )
                                    context.startActivity(intent)
                                }
                        )
                    } else ComponentPlaceholder(
                        Modifier.fillMaxSize()
                    )
                }
            }

            // Event Details Section
            if (eventData != null) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    // Location Stats
                    if (eventData is EventRange && eventLocations.size > 1) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp)),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 2.dp
                        ) {
                            val maxSpeed = round(locationData.maxOf { it.speed } * 10) / 10
                            val distance = locationData.sumOf { item -> item.distance.toDouble() }
                            val avgSpeed = try { round((distance / 100f / (duration / 3600f)).roundToInt() / 10f) } catch(_: Exception) { 0 }

                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "Trip Stats",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    StatCard("Avg Speed", "$avgSpeed Km/h")
                                    StatCard("Max Speed", "$maxSpeed Km/h")
                                    StatCard(
                                        "Distance",
                                        if (distance < 1000) "${round(distance).toInt()} m"
                                        else "${round(distance / 100) / 10} Km"
                                    )
                                }
                            }
                        }
                        Box(Modifier.height(12.dp))
                    }


                    /////////////////////////
                    /* DEVELOPER MODE ONLY */
                    /////////////////////////

                    val showDetectedTransport by Config.developerShowDetectedTransport.getState()

                    if (showDetectedTransport && eventData is EventRange) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp)),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 2.dp
                        ) {
                            val showDetectedTransport by Config.developerShowDetectedTransport.getState()

                            var modelParams by remember { mutableStateOf(mapOf<TransportMode, CalibrationModel>()) }

                            var originalParams by remember { mutableStateOf(loadCalibrationModels(context)) }
                            var calibrationJob by remember { mutableStateOf<Job?>(null) }
                            val detected = detectTransportMode(locationData, modelParams)

                            var trainingJob by remember { mutableStateOf<Job?>(null) }
                            var trainingResult by remember { mutableStateOf<TrainingResult?>(null) }

                            LaunchedEffect(originalParams) {
                                modelParams = originalParams
                            }

                            if (showDetectedTransport) Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    "TransportDetectionModel (DEV OPTION)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Column {
                                    for (modelKey in modelParams.keys) {
                                        Text(
                                            text = "${modelKey.name} (${getScore(locationData, modelKey, modelParams)})",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (detected == modelKey) {
                                                if (eventData.transportationType == TransportationType.Unspecified) Color.Yellow
                                                else if (detected == eventData.transportationType.toTransportMode()) Color.Green
                                                else Color.Red
                                            } else Color.Unspecified
                                        )
                                        CalibrationParamsPreview(
                                            original = originalParams[modelKey],
                                            current = modelParams.getOrDefault(
                                                modelKey,
                                                CalibrationModel(
                                                    0f,0f,0f,0f,0f,0f,
                                                    0f, 0f, 0f, 0f, 0f, 0f,
                                                    0f, 0f, 0f, 0f,
                                                    0f, 0f, 0f, 0f,
                                                    0f, 0f, 0f, 0f
                                                )
                                            )
                                        )
                                        Box(Modifier.height(8.dp))
                                    }

                                    if (trainingResult != null) {
                                        Text(
                                            text = "Training Result",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Text(
                                            text = "Accuracy: ${trainingResult?.accuracy?.let { round(it * 1000)/10 }}%",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = "Total samples: ${trainingResult?.totalSamples}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = "Correct samples: ${trainingResult?.correctSamples}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                                ) {
                                    StatCard(
                                        "DetectedTransport",
                                        detected.name
                                    )
                                    StatCard(
                                        "ExpectedTransport",
                                        eventData.transportationType.toTransportMode().name
                                    )
                                }
                                if (eventData.transportationType != TransportationType.Unspecified) Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            calibrationJob = calibrationJob?.let {
                                                it.cancel()
                                                scope.launch {
                                                    delay(100)
                                                    calibrationJob = null
                                                }
                                                it
                                            } ?: scope.launch {
                                                while (detectTransportMode(locationData, modelParams) != eventData.transportationType.toTransportMode()) {
                                                    delay(10)
                                                    modelParams = recalibrateModels(
                                                        locationData,
                                                        modelParams,
                                                        eventData.transportationType.toTransportMode(),
                                                    )
                                                    saveCalibrationModels(context, modelParams)
                                                }
                                                calibrationJob = null
                                            }
                                        }
                                    ) {
                                        Text(
                                            if (calibrationJob == null) "Recalibrate"
                                            else "Recalibrating..."
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            resetModels(context)
                                            originalParams = emptyMap()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                            containerColor = MaterialTheme.colorScheme.errorContainer
                                        )
                                    ) {
                                        Text("Reset")
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        trainingJob = scope.launch {
                                            trainingResult = train(context, modelParams)
                                            modelParams = trainingResult!!.models

                                            trainingJob = null
                                        }
                                    }
                                ) {
                                    Text(
                                        if (trainingJob == null) "Train"
                                        else "Training..."
                                    )
                                }
                            }
                        }
                        Box(Modifier.height(12.dp))
                    }


                    // Signature Section
                    if (eventData.signature.isNotBlank()) {
                        val valid = remember { eventData.validateSignature(context = context) }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    showSignatureInfo = !showSignatureInfo
                                },
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        color = if (valid) Color(0xFF008000).copy(alpha = 0.2f) else Color(0xFF808000).copy(alpha = 0.2f)
                                    ) {
                                        Icon(
                                            if (valid) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                            contentDescription = if (valid) "Signed" else "Invalid signature",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(4.dp),
                                            tint = if (valid) Color(0xFF008000) else Color(0xFF808000)
                                        )
                                    }
                                    Text(
                                        text = if (valid) "Event is digitally signed" else "Signature invalid",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                AnimatedVisibility(visible = showSignatureInfo) {
                                    Text(
                                        "Signature: ${eventData.signature}\nValid: $valid",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                            }
                        }
                        Box(Modifier.height(12.dp))
                    }

                    // Audio Player
                    if (eventData.audio != null) {
                        AudioPlayer("${context.filesDir.absolutePath}/audio/${eventData.audio}")
                        Box(Modifier.height(12.dp))
                    }

                    // Details Cards
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 2.dp
                    ) {
                        Column {
                            DetailCard(
                                icon = painterResource(R.drawable.ic_title),
                                label = "Title",
                                value = eventData.title,
                                isFirst = true,
                                format = true,
                                mentionModalState = mentionModalState
                            )
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            DetailCard(
                                icon = painterResource(R.drawable.ic_info),
                                label = "Description",
                                value = if (eventData.description.isNotBlank()) eventData.description else "No description",
                                isEmpty = eventData.description.isBlank(),
                                format = true,
                                mentionModalState = mentionModalState
                            )
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            DetailCard(
                                icon = painterResource(R.drawable.ic_schedule),
                                label = if (eventData !is EventRange) "Time" else "Start",
                                value = eventData.time.format(DateFormatter) + " " +
                                        eventData.time.format(TimeFormatter) +
                                        if (eventData.time.second != 0) ":${
                                            eventData.time.second.toString().padStart(2, '0')
                                        }" else "",
                                isLast = eventData !is EventRange
                            )
                            if (eventData is EventRange) {
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                                DetailCard(
                                    icon = painterResource(R.drawable.ic_arrow_range),
                                    label = "Duration"
                                ) {
                                    val durationObj = Duration.ofSeconds(duration)
                                    val days = durationObj.toDays()
                                    val hours = durationObj.toHoursPart()
                                    val minutes = durationObj.toMinutesPart()
                                    val seconds = durationObj.toSecondsPart()

                                    var text = ""
                                    if (days > 0) text += "${days}d "
                                    if (hours > 0) text += "${hours}h "
                                    if (minutes > 0) text += "${minutes}m "
                                    if (seconds > 0) text += "${seconds}s"

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (eventData.transportationType.iconId != null) {
                                            Icon(
                                                painterResource(eventData.transportationType.iconId!!),
                                                eventData.transportationType.name,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(text)
                                    }
                                }
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                                DetailCard(
                                    icon = painterResource(R.drawable.ic_schedule),
                                    label = "End",
                                    value = eventData.end.format(DateFormatter) + " " +
                                            eventData.end.format(TimeFormatter) +
                                            if (eventData.end.second != 0) ":${
                                                eventData.end.second.toString().padStart(2, '0')
                                            }" else "",
                                    isLast = true
                                )
                            }
                        }
                    }
                }
            }

            Box(Modifier.height(16.dp))
        }
    }
    else Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text("Event not found")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            context.getActivity()?.finish()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(PaddingValues(16.dp))
        ) {
            Text("Event not found")
        }
    }
    if (showInfo && eventData != null) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
            onDismissRequest = { showInfo = false }
        ) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Event Information",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                )

                val timeFormat = { time: ZonedDateTime ->
                    "${time.format(DateFormatter)} ${time.format(TimeFormatter)}"
                }

                InfoCard("Created", eventData.created.let(timeFormat))
                InfoCard("Last Modified", eventData.modified.let(timeFormat))
                InfoCard("Event ID", eventData.id)
                InfoCard("Audio", eventData.audio ?: "—")
                InfoCard("Location Data", eventData.locationPath ?: "—")
                if (eventData is EventRange) {
                    InfoCard("Location Points", eventLocations.size.toString())
                }
                InfoCard("Event Version", eventData.versionNumber.toString())

                if (eventData.metadata.isNotEmpty()) {
                    Box(Modifier.height(12.dp))
                    Text(
                        "Metadata",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    for ((name, value) in eventData.metadata) {
                        InfoCard(name,
                            when {
                                value !is Double -> value.toString()
                                value % 1.0 == 0.0 -> value.toInt().toString()
                                else -> value.toString()
                            }
                        )
                    }
                }

                Box(Modifier.height(24.dp))
            }
        }
    }

    MentionModalHandler(mentionModalState.value)

    // Tag Picker Dialog
    if (eventData != null) TagPickerDialog(
        state = tagDialogState,
        eventIds = listOf(eventData.id),
        onDismissRequest = {
            tagDialogState = false
        }
    )

    // TODO: Update MapShareDialog to adapt new LocationData
    if (showShareMapDialog && eventData != null) MapShareDialog(
        eventData = eventData,
        onDismiss = {
            showShareMapDialog = false
        },
        eventLocations = eventLocations.associate { it.time to it.location },
        locationData = locationData
    )


    // Delete Dialog
    if (deleteDialogState && eventData != null) {
        AlertDialog(
            icon = {
                Icon(Icons.Rounded.Warning, "Warning", tint = MaterialTheme.colorScheme.error)
            },
            onDismissRequest = {
                deleteDialogState = false
            },
            title = {
                Text("Delete event?")
            },
            text = {
                Text("'${eventData.title.replace("\n", " ")}' will be moved to trash and permanently deleted after 30 days.")
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleteDialogState = false
                    }
                ) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { AppDatabase.moveToTrash(dao, context, listOf(eventData.id)) }
                        context.getActivity()?.run {
                            finish()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                ) {
                    Text("Delete")
                }
            }
        )
    }
}

@Composable
private fun RecordingIndicatorBar() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shadowElevation = 4.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Animated recording pulse
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(MaterialTheme.colorScheme.onError),
                    contentAlignment = Alignment.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 600, easing = EaseInOutCubic),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseScale"
                    )
                    Box(
                        modifier = Modifier
                            .size(12.dp * scale)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(
                                MaterialTheme.colorScheme.onError.copy(alpha = 0.3f)
                            )
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Recording in progress",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onError,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    Text(
                        "This event is currently being recorded",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError.copy(alpha = 0.8f)
                    )
                }

                Icon(
                    Icons.Rounded.Info,
                    "Recording",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Animated progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.onError.copy(alpha = 0.2f))
                )

                val infiniteTransition = rememberInfiniteTransition(label = "progress")
                val progress by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "progressFloat"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.onError)
                )
            }
        }
    }
}

@Composable
private fun DetailCard(
    icon: androidx.compose.ui.graphics.painter.Painter,
    label: String,
    value: String? = null,
    isEmpty: Boolean = false,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    format: Boolean = false,
    mentionModalState: MutableState<MentionModalState>? = null,
    content: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            label,
            modifier = Modifier
                .size(24.dp)
                .padding(top = 2.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (value != null) {
                if (!format) Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isEmpty) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                else FormattedText(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isEmpty) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    mentionModalState = mentionModalState
                )
            } else {
                content?.invoke()
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .widthIn(min = 100.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoCard(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        )
    }
}

@Composable
fun MapHistoryPreview(locations: List<LocationData>, modifier: Modifier = Modifier) {
    val geoPoints = locations.sortedBy { it.time }.map {
        GeoPoint(it.location.latitude, it.location.longitude)
    }

    val context = LocalContext.current
//        .let {geoPoints ->
//        if (geoPoints.size > 2) {
//            val coordinates = geoPoints.dropLast(1).mapIndexed { a, b -> a to b }.filter {
//                it.first % (2.0.pow((15 - zoomLevelDouble.roundToInt()))) == 0.0
//            }.map { it.second }.toMutableList()
//            coordinates.add(geoPoints.last())
//            coordinates.toList()
//        }
//        else geoPoints
//    }

    if (geoPoints.isEmpty()) return

    var renderedPoints by remember { mutableIntStateOf(0) }

    var shownPoints by remember { mutableStateOf<List<GeoPoint>>(listOf()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
//            shownPoints =
//                if (geoPoints.size > 2) {
//                    val coordinates =
//                        geoPoints.dropLast(1).mapIndexed { a, b -> a to b }.filter {
//                            // it.first % (2.0.pow((15 - zoomLevelDouble.roundToInt()))) == 0.0
//                            it.first % max((locations.size / 100), 1) == 0
//                        }.map { it.second }.toMutableList()
//                    coordinates.add(geoPoints.last())
//                    coordinates.toList()
//                } else geoPoints
            shownPoints = simplifyPoints(locations.map { it.location }).map {
                it.toGeoPoint()
            }
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth(),
        factory = { ctx ->
            Log.d("NanHistoryDebug", "RECALCULATE?")

            MapView(ctx).apply {

                setMultiTouchControls(false) // Disable pinch-to-zoom
                isClickable = false // Disable clicks
                isEnabled = false // Disable all interactions

                setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                post {
                    maxZoomLevel = 18.0
                    zoomToBoundingBox(geoPoints.toBoundingBox(), false)
                    setScrollableAreaLimitLatitude(mapCenter.latitude, mapCenter.latitude, 0)
                    setScrollableAreaLimitLongitude(mapCenter.longitude, mapCenter.longitude, 0)
                    minZoomLevel = zoomLevelDouble
                    maxZoomLevel = zoomLevelDouble

                    postOnAnimation {

                    }
                }
            }
        },
        update = { mapView ->
            Log.d("NanHistoryDebug", "RECALCULATE")

            renderedPoints = shownPoints.size
            Log.d("NanHistoryDebug", "RENDERED: $renderedPoints")

            val polyline = Polyline(mapView).apply {
                setPoints(shownPoints)
                outlinePaint.color = android.graphics.Color.BLUE
                outlinePaint.strokeWidth = 8f
                outlinePaint.strokeCap = Paint.Cap.ROUND
            }
            val polylineBorder = Polyline(mapView).apply {
                setPoints(shownPoints)
                outlinePaint.color = android.graphics.Color.rgb(0, 0, 20)
                outlinePaint.strokeWidth = 10f
                outlinePaint.strokeCap = Paint.Cap.ROUND
            }
            val markerStart = Marker(mapView).apply {
                position = geoPoints.first()
                icon = context.getDrawable(R.drawable.ic_location_start)
            }
            val markerEnd = Marker(mapView).apply {
                position = geoPoints.last()
                icon = context.getDrawable(R.drawable.ic_location_end)
            }

            mapView.overlays.add(polylineBorder)
            mapView.overlays.add(polyline)
            if (geoPoints.size > 1) mapView.overlays.add(markerStart)
            mapView.overlays.add(markerEnd)

            mapView.invalidate()
        }
    ).also { _ ->
        DisposableEffect(Unit) {
            onDispose {
            }
        }

    }
}

fun List<GeoPoint>.toBoundingBox(): BoundingBox? {
    if (this.isNotEmpty()) {
        // Get the bounding box for the polyline
        val boundingBox = BoundingBox.fromGeoPointsSafe(this)

        val paddingFactor = 0.2

        return BoundingBox(
            boundingBox.latNorth + (boundingBox.latitudeSpan * paddingFactor),
            boundingBox.lonEast + (boundingBox.longitudeSpan * paddingFactor),
            boundingBox.latSouth - (boundingBox.latitudeSpan * paddingFactor),
            boundingBox.lonWest - (boundingBox.longitudeSpan * paddingFactor)
        )

//        mapView.maxZoomLevel = 18.0
//        mapView.minZoomLevel = 4.0
//
//        // Adjust the map view to fit the bounding box
//        mapView.post {
//            mapView.zoomToBoundingBox(expandedBoundingBox, false)
//        }
    }
    return null
}

@Preview(showBackground = true)
@Composable
fun DetailContentPreview() {
    NanHistoryTheme {
        DetailContent("", "")
    }
}