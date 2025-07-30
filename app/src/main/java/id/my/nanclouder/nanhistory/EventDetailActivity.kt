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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toHistoryEvent
import id.my.nanclouder.nanhistory.lib.Coordinate
import id.my.nanclouder.nanhistory.lib.DateFormatter
import id.my.nanclouder.nanhistory.lib.HistoryLocationData
import id.my.nanclouder.nanhistory.lib.TimeFormatter
import id.my.nanclouder.nanhistory.lib.getLocationData
import id.my.nanclouder.nanhistory.lib.history.EventPoint
import id.my.nanclouder.nanhistory.lib.history.EventRange
import id.my.nanclouder.nanhistory.lib.history.generateEventId
import id.my.nanclouder.nanhistory.lib.history.getFilePathFromDate
import id.my.nanclouder.nanhistory.lib.history.validateSignature
import id.my.nanclouder.nanhistory.lib.shareFile
import id.my.nanclouder.nanhistory.service.RecordService
import id.my.nanclouder.nanhistory.ui.AudioPlayer
import id.my.nanclouder.nanhistory.ui.ComponentPlaceholder
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
import kotlin.math.max
import kotlin.math.round
import kotlin.math.roundToInt

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailContent(eventId: String, path: String) {
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
    val recordRunning by RecordService.RecordState.isRunning.collectAsState()
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
        mutableStateOf<Map<ZonedDateTime, Coordinate>>(mapOf())
    }
    val locationAvailable = eventData?.locationPath != null

    var locationData by remember {
        mutableStateOf<List<HistoryLocationData>>(listOf())
    }

    LaunchedEffect(eventData) {
        eventLocations = eventData?.getLocations(context) ?: mapOf()
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
                        val avgSpeed = (distance / 100f / (duration / 3600f)).roundToInt() / 10f

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

@Composable
fun MapHistoryPreview(locations: Map<ZonedDateTime, Coordinate>, modifier: Modifier = Modifier) {
    val geoPoints = locations.keys.sorted().map {
        GeoPoint(locations[it]!!.latitude, locations[it]!!.longitude)
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
            shownPoints =
                if (geoPoints.size > 2) {
                    val coordinates =
                        geoPoints.dropLast(1).mapIndexed { a, b -> a to b }.filter {
                            // it.first % (2.0.pow((15 - zoomLevelDouble.roundToInt()))) == 0.0
                            it.first % max((locations.size / 100), 1) == 0
                        }.map { it.second }.toMutableList()
                    coordinates.add(geoPoints.last())
                    coordinates.toList()
                } else geoPoints
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