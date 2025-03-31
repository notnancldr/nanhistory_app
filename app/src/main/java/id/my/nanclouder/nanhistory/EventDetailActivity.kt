package id.my.nanclouder.nanhistory

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import id.my.nanclouder.nanhistory.lib.DateFormatter
import id.my.nanclouder.nanhistory.lib.TimeFormatter
import id.my.nanclouder.nanhistory.lib.getLocationData
import id.my.nanclouder.nanhistory.lib.history.EventPoint
import id.my.nanclouder.nanhistory.lib.history.EventRange
import id.my.nanclouder.nanhistory.lib.history.HistoryEvent
import id.my.nanclouder.nanhistory.lib.history.HistoryFileData
import id.my.nanclouder.nanhistory.lib.history.delete
import id.my.nanclouder.nanhistory.lib.history.generateEventId
import id.my.nanclouder.nanhistory.lib.history.get
import id.my.nanclouder.nanhistory.lib.history.getFilePathFromDate
import id.my.nanclouder.nanhistory.lib.history.save
import id.my.nanclouder.nanhistory.lib.history.validateSignature
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.pow
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

    var showSignatureInfo by rememberSaveable { mutableStateOf(false) }
    var deleteDialogState by rememberSaveable { mutableStateOf(false) }

    var currentPath by rememberSaveable { mutableStateOf(path) }

    var showInfo by remember { mutableStateOf(false) }

    val getEvent = {
        val fileData = HistoryFileData.get(context, currentPath)
        fileData?.events?.firstOrNull {
            it.id == eventId
        }
    }

    var savedEvent by remember {
        mutableStateOf(getEvent())
    }

    val eventData = savedEvent

    // EventRange only
    val duration = when (eventData) {
        is EventPoint -> null
        is EventRange -> eventData.end.toEpochSecond() - eventData.time.toEpochSecond()
        else -> null
    } ?: 0

    var favorite by rememberSaveable { mutableStateOf(eventData?.favorite) }
    val locationAvailable = when (eventData) {
        is EventRange -> eventData.locations.isNotEmpty()
        is EventPoint -> eventData.location != null
        else -> false
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == 1) {
                result.data?.getStringExtra("path")?.let {
                    currentPath = it
                    savedEvent = getEvent()
                    val intent = Intent().apply {
                        putExtra("path", it)
                    }
                    context.getActivity()?.setResult(1, intent)
                }
            }
            else if (result.resultCode == 2) {
                result.data?.getStringExtra("path")?.let {
                    val intent = Intent().apply {
                        putExtra("path", it)
                    }
                    context.getActivity()?.setResult(2, intent)
                }
            }
        }


    if (eventData != null) Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        eventData.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
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
                    IconButton(
                        onClick = {
                            favorite = !(favorite ?: false)
                            eventData.favorite = favorite!!
                            eventData.modified = ZonedDateTime.now()
                            eventData.save(context)
                        }
                    ) {
                        if (favorite == true) Icon(
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
                            intent.putExtra("path", currentPath)
                            launcher.launch(intent)
                        }
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
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        painterResource(R.drawable.ic_content_cut),
                                        contentDescription = "Cut Event",
                                        tint = Color.Gray
                                    )
                                },
                                text = { Text("Cut Event") },
                                onClick = {
                                    val intent = Intent(context, EventLocationActivity::class.java)
                                        .apply {
                                            putExtra("eventId", eventId)
                                            putExtra("path", currentPath)
                                            putExtra("cutMode", true)
                                        }
                                    launcher.launch(intent)
                                    // TODO
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        painterResource(R.drawable.ic_delete),
                                        "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                text = {
                                    Text(
                                        "Delete",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = { deleteDialogState = true; menuExpanded = false }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            if (locationAvailable) {
                Box(
                    modifier = Modifier
                        .height(200.dp)
                ) {
                    MapHistoryPreview(
                        eventData = eventData,
                        modifier = Modifier.height(200.dp)
                    )
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            val intent = Intent(context, EventLocationActivity::class.java)
                            intent.putExtra("eventId", eventData.id)
                            intent.putExtra(
                                "path",
                                getFilePathFromDate(eventData.time.toLocalDate())
                            )
                            context.startActivity(intent)
                        })
                }
            }

            if (eventData is EventRange && eventData.locations.size > 1) {
                val data = eventData.locations.getLocationData()
                val maxSpeed = round(data.maxOf { it.speed } * 10) / 10
                val distance = data.sumOf { item -> item.distance.toDouble() }
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
                val valid = eventData.validateSignature()
                ListItem(
                    modifier = Modifier.clickable {
                        showSignatureInfo = !showSignatureInfo
                    },
                    leadingContent = {
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
                    },
                    headlineContent = {
                        Text(
                            text =
                            if (valid) "This event is signed"
                            else "The signature of this event is invalid",
                            fontStyle = FontStyle.Italic
                        )
                    },
                    supportingContent = {
                        if (showSignatureInfo)
                            Text(
                                """Signature info:
                         |Signature: ${eventData.signature}
                         |Valid: $valid
                         """.trimMargin()
                            )
                    }
                )
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

                        Text(text, color = Color.Gray)
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

            val sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = false
            )

            if (showInfo) ModalBottomSheet(
                modifier = Modifier
                    .fillMaxHeight(),
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
                    if (eventData is EventRange) ListItem(
                        overlineContent = {
                            Text("Location points")
                        },
                        headlineContent = {
                            Text(eventData.locations.size.toString())
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
                                    Text(value.toString())
                                }
                            )
                        }
                    }
                }
            }

            if (deleteDialogState) {
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
                        Text("This event will be deleted forever and cannot be recovered")
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
                                eventData.delete(context)
                                context.getActivity()?.run {
                                    setResult(1)
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
fun MapHistoryPreview(eventData: HistoryEvent, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val locations = if (eventData is EventPoint)
        mapOf(eventData.time to eventData.location).filter { it.value != null }
    else (eventData as EventRange).locations

    val geoPoints = locations.keys.sorted().map {
        GeoPoint(locations[it]!!.latitude, locations[it]!!.longitude)
    }
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

    AndroidView(
        modifier = modifier
            .fillMaxWidth(),
        factory = { ctx ->
            MapView(ctx).apply {

                setMultiTouchControls(false) // Disable pinch-to-zoom
                isClickable = false // Disable clicks
                isEnabled = false // Disable all interactions

                setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                addMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent?): Boolean {
                        return false // Return true if the event was handled
                    }

                    override fun onZoom(event: ZoomEvent?): Boolean {
                        onResume()
                        return true
                    }
                })


                post {
                    maxZoomLevel = 18.0
                    zoomToBoundingBox(geoPoints.toBoundingBox(), false)
                    setScrollableAreaLimitLatitude(mapCenter.latitude, mapCenter.latitude, 0)
                    setScrollableAreaLimitLongitude(mapCenter.longitude, mapCenter.longitude, 0)
                    minZoomLevel = zoomLevelDouble
                    maxZoomLevel = zoomLevelDouble

                    postOnAnimation {
                        val shownPoints =
                            if (geoPoints.size > 2) {
                                val coordinates =
                                    geoPoints.dropLast(1).mapIndexed { a, b -> a to b }.filter {
                                        it.first % (2.0.pow((15 - zoomLevelDouble.roundToInt()))) == 0.0
                                    }.map { it.second }.toMutableList()
                                coordinates.add(geoPoints.last())
                                coordinates.toList()
                            } else geoPoints

                        val polyline = Polyline(this).apply {
                            setPoints(shownPoints)
                            outlinePaint.color = android.graphics.Color.BLUE
                            outlinePaint.strokeWidth = 8f
                            outlinePaint.strokeCap = Paint.Cap.ROUND
                        }
                        val polylineBorder = Polyline(this).apply {
                            setPoints(shownPoints)
                            outlinePaint.color = android.graphics.Color.rgb(0, 0, 20)
                            outlinePaint.strokeWidth = 10f
                            outlinePaint.strokeCap = Paint.Cap.ROUND
                        }
                        val markerStart = Marker(this).apply {
                            position = geoPoints.first()
                            icon = context.getDrawable(R.drawable.ic_location_start)
                        }
                        val markerEnd = Marker(this).apply {
                            position = geoPoints.last()
                            icon = context.getDrawable(R.drawable.ic_location_end)
                        }

                        overlays.add(polylineBorder)
                        overlays.add(polyline)
                        if (geoPoints.size > 1) overlays.add(markerStart)
                        overlays.add(markerEnd)
                    }
                }
            }
        },
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