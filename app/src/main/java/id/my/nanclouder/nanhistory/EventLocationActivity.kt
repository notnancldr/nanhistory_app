package id.my.nanclouder.nanhistory

import android.content.Intent
import android.graphics.Paint
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toEventEntity
import id.my.nanclouder.nanhistory.db.toHistoryEvent
import id.my.nanclouder.nanhistory.lib.Coordinate
import id.my.nanclouder.nanhistory.lib.TimeFormatterWithSecond
import id.my.nanclouder.nanhistory.lib.getLocationData
import id.my.nanclouder.nanhistory.lib.history.EventRange
import id.my.nanclouder.nanhistory.lib.history.HistoryEvent
import id.my.nanclouder.nanhistory.lib.history.HistoryFileData
import id.my.nanclouder.nanhistory.lib.history.createLocationFile
import id.my.nanclouder.nanhistory.lib.history.generateEventId
import id.my.nanclouder.nanhistory.lib.history.generateSignature
import id.my.nanclouder.nanhistory.lib.history.get
import id.my.nanclouder.nanhistory.lib.history.getFilePathFromDate
import id.my.nanclouder.nanhistory.lib.history.save
import id.my.nanclouder.nanhistory.lib.history.validateSignature
import id.my.nanclouder.nanhistory.lib.history.writeToLocationFile
import id.my.nanclouder.nanhistory.lib.matchOrNull
import id.my.nanclouder.nanhistory.lib.toGeoPoint
import id.my.nanclouder.nanhistory.ui.ComponentPlaceholder
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import kotlinx.coroutines.launch
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File
import java.time.ZonedDateTime
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

class EventLocationActivity : ComponentActivity() {
    var update: () -> Unit = {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val eventId = intent.getStringExtra("eventId") ?: generateEventId()
        val path = intent.getStringExtra("path") ?: "NULL!"
        val startAsCutMode = intent.getBooleanExtra("cutMode", false)
        setContent {
            var setUpdate by remember { mutableStateOf(false) }
            update = { setUpdate = !setUpdate }
            NanHistoryTheme {
//                Log.d("NanHistoryDebug", "data (eventId) : $eventId")
//                Log.d("NanHistoryDebug", "data (path)    : $path")
                key(setUpdate) { EventLocationView(eventId, startAsCutMode) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        update()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventLocationView(eventId: String, startAsCutMode: Boolean = false) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
//    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

    val eventState by dao.getEventFlowById(eventId).collectAsState(null)
    val eventData = eventState?.toHistoryEvent()

    val recording = matchOrNull<Boolean>(eventData?.metadata?.get("recording")) ?: false

    val eventLocations = eventData?.getLocations(context) ?: mapOf()
    val locationAvailable = eventLocations.isNotEmpty()

    var cutMode by rememberSaveable { mutableStateOf(startAsCutMode) }
    var cutStart by rememberSaveable { mutableStateOf<ZonedDateTime?>(null) }
    var cutEnd by rememberSaveable { mutableStateOf<ZonedDateTime?>(null) }

    Log.d("NanHistoryDebug", "eventData: $eventData")

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    if (eventData != null)
                        Text(if (cutMode) "Cut Event" else "Event Map")
                    else
                        ComponentPlaceholder(Modifier.size(128.dp, 16.dp))
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
                    if (eventData == null) ComponentPlaceholder(Modifier.size(24.dp).padding(8.dp))
                    if (cutMode && eventData is EventRange) IconButton(
                        onClick = {
                            // TODO
                            val event = EventRange(
                                title = "Cut of ${eventData.title}",
                                description = eventData.description +
                                    if (eventData.description.isBlank()) "" else "\n" +
                                    "Cut of ${eventData.title}",
                                time = cutStart!!,
                                favorite = eventData.favorite,
                                tags = eventData.tags,
                                end = cutEnd!!,
                                locationDescriptions = eventData.locationDescriptions.filter {
                                    it.key >= cutStart!! && it.key <= cutEnd!!
                                }.toMutableMap(),
                                metadata = eventData.metadata,
                            ).apply {
                                metadata["original_event_id"] = eventData.id
                                metadata["original_event_time"] = eventData.time.toOffsetDateTime().toString()
                                metadata["original_event_end"] = eventData.end.toOffsetDateTime().toString()
                                if (metadata["root_event_id"] == null)
                                    metadata["root_event_id"] = eventData.id
                                if (metadata["root_event_time"] == null)
                                    metadata["root_event_time"] = eventData.time.toOffsetDateTime().toString()
                                if (metadata["root_event_end"] == null)
                                    metadata["root_event_end"] = eventData.end.toOffsetDateTime().toString()
                            }
                            val locationFile = createLocationFile(context, event.time)
                            val locationsData = eventLocations.filter {
                                it.key >= cutStart!! && it.key <= cutEnd!!
                            }
                            locationsData.writeToLocationFile(locationFile)
                            event.locationPath = locationFile.absolutePath.removePrefix(File(context.filesDir, "locations").absolutePath + "/")

                            if (eventData.audio != null) {
                                val audioFile = File(context.filesDir, "audio/${eventData.audio}")
                                audioFile.parentFile?.let {
                                    val id = generateEventId()
                                    val targetPath = "${it.name}/$id.m4a"
                                    val targetFile = File(it, "/$id.m4a")
                                    Log.d("NanHistoryDebug", "AUDIO COPY | FROM: ${audioFile.absolutePath} | TO: ${targetFile.absolutePath}")
                                    audioFile.copyTo(targetFile)

                                    event.audio = targetPath
                                }
                            }
                            if (eventData.validateSignature(context = context)) event.generateSignature(true, context)

                            val db = AppDatabase.getInstance(context)
                            val dao = db.appDao()

                            scope.launch { dao.insertEvent(event.toEventEntity()) }

                            cutMode = false
                            Toast.makeText(context, "${event.title} has been saved", Toast.LENGTH_SHORT).show()

                            val resultIntent = Intent().apply {
                                putExtra("path", getFilePathFromDate(event.time.toLocalDate()))
                            }

                            context.getActivity()?.setResult(2, resultIntent)
                            if (startAsCutMode) context.getActivity()?.finish()

                            // TODO
                        },
                        enabled = cutStart != null && cutEnd != null
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Confirm",
                        )
                    }
                    if (eventData is EventRange) IconButton({ cutMode = !cutMode }, enabled = !recording) {
                        if (!cutMode) Icon(
                            painterResource(R.drawable.ic_content_cut),
                            "Cut Mode"
                        )
                        else Icon(
                            Icons.Rounded.Close,
                            "Exit Cut Mode"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (locationAvailable) {
                MapHistoryView(
                    locations = eventLocations,
                    onPointsSelected = { time1, time2 ->
                        if (time1 != null && time2 != null) {
                            cutStart = if (time1 < time2) time1 else time2
                            cutEnd = if (time1 > time2) time1 else time2
                        } else {
                            cutStart = time1
                            cutEnd = null
                        }
                    },
                    cutMode = cutMode
                )
            }
            else ComponentPlaceholder(
                Modifier
                    .weight(1f)
                    .padding(8.dp)
            )
        }
    }
}

fun calculateDistance(startPoint: GeoPoint, endPoint: GeoPoint): Float =
    Location("map").apply {
        latitude = startPoint.latitude
        longitude = startPoint.longitude
    }.distanceTo(
        Location("map").apply {
            latitude = endPoint.latitude
            longitude = endPoint.longitude
        }
    )

fun calculateSpeed(startPoint: GeoPoint, endPoint: GeoPoint, timeHours: Float): Float =
    calculateDistance(startPoint, endPoint) / 1000 / timeHours

fun calculateColor(speed: Int): Color {
    val lightness = max(min(speed / 20f, .5f), 0.3f)
    val hue = max(min(speed * 2.5f, 270f), 0f)
    return Color.hsl(hue, 1f, lightness)
}

@Composable
fun MapHistoryView(
    locations: Map<ZonedDateTime, Coordinate>,
    onPointsSelected: (ZonedDateTime?, ZonedDateTime?) -> Unit,
    modifier: Modifier = Modifier,
    cutMode: Boolean = false,
) {
    val context = LocalContext.current

    val mapKeys = locations.keys.sorted()

    val geoPoints = mapKeys.sorted().map {
        GeoPoint(locations[it]!!.latitude, locations[it]!!.longitude)
    }

    val bottomBarScrollState = rememberScrollState()

    // Cut Event Mode: Cut based on location at start and end of the event
    var cutTime1 by rememberSaveable { mutableStateOf<ZonedDateTime?>(null) }
    var cutTime2 by rememberSaveable { mutableStateOf<ZonedDateTime?>(null) }
    var cutModeCheck by remember { mutableStateOf(cutMode) }

    var selectedCutPoint by rememberSaveable { mutableStateOf<ZonedDateTime?>(null) }

    // User Options
    var showPoints by rememberSaveable { mutableStateOf(false) }
    var showData by rememberSaveable { mutableStateOf(false) }

    var needUpdate by remember { mutableStateOf(false) }
    var firstLoad by remember { mutableStateOf(true) }

    var mapViewObj by remember { mutableStateOf<MapView?>(null) }

    if (geoPoints.isEmpty()) return

    Log.d("NanHistoryDebug", "RECOMPOSE!")

    var zoomLevel by remember { mutableDoubleStateOf(0.0) }
    var prevZoomLevel by remember { mutableDoubleStateOf(zoomLevel) }
    var center by remember { mutableStateOf(GeoPoint(0.0, 0.0)) }

    var isSelected by remember { mutableStateOf(false) }

    var selectedSpeed by remember { mutableIntStateOf(0) }
    var selectedTime by remember { mutableStateOf("") }
    var selectedFirstKey by remember { mutableStateOf(ZonedDateTime.now()) }
    var selectedSecondKey by remember { mutableStateOf(ZonedDateTime.now()) }

    var updateRemaining by remember { mutableIntStateOf(0) }

    isSelected = showData && isSelected

    val shownCoordinates =
        if (geoPoints.size > 2) {
            val coordinates = geoPoints.dropLast(1).mapIndexed { a, b -> a to b }.filter {
                it.first % (2.0.pow((15 - zoomLevel.roundToInt()))) == 0.0
            }.map { it.second }.toMutableList()
            coordinates.add(geoPoints.last())
            coordinates.toList()
        }
        else geoPoints

    val shownKeys =
        if (geoPoints.size > 2) {
            val coordinates = mapKeys.dropLast(1).mapIndexed { a, b -> a to b }.filter {
                it.first % (2.0.pow((15 - zoomLevel.roundToInt()))) == 0.0
            }.map { it.second }.toMutableList()
            coordinates.add(mapKeys.last())
            coordinates.toList()
        }
        else mapKeys

    var inMaxDetail by remember { mutableStateOf(false) }
    val maxDetail = shownCoordinates.size == geoPoints.size

    val screenDpi = LocalDensity.current.run { 1.dp.toPx() }

    var updateMap = { }

    val updateCutSelection = {
        onPointsSelected(cutTime1, cutTime2)
        if (cutTime1 == null || cutTime2 == null) updateMap()
        else needUpdate = true
    }

    val setCutPoint = cutPointSetter@{ time: ZonedDateTime ->
        if (time == cutTime1 || time == cutTime2) return@cutPointSetter

        if (cutTime1 == null) cutTime1 = time
        else if (cutTime2 == null) cutTime2 = time
        else return@cutPointSetter

        updateCutSelection()
    }

    val undoCutSelection = undo@{
        if (cutTime2 != null) cutTime2 = null
        else if (cutTime1 != null) cutTime1 = null
        else return@undo

        updateCutSelection()
    }

    if (cutMode != cutModeCheck) {
        cutModeCheck = cutMode
        cutTime1 = null
        cutTime2 = null
        needUpdate = true
    }

    updateMap = updater@{
        if (mapViewObj == null) return@updater

        mapViewObj?.overlays?.clear()

        val polyline = Polyline(mapViewObj).apply {
            setPoints(shownCoordinates)
            outlinePaint.color =
                if (!cutMode) android.graphics.Color.BLUE
                else android.graphics.Color.GRAY
            outlinePaint.strokeWidth = 3f * screenDpi
            outlinePaint.strokeCap = Paint.Cap.ROUND
        }
        val polylineBorder = Polyline(mapViewObj).apply {
            setPoints(shownCoordinates)
            outlinePaint.color = android.graphics.Color.rgb(0, 0, 20)
            outlinePaint.strokeWidth = 4f * screenDpi
            outlinePaint.strokeCap = Paint.Cap.ROUND
        }
        val markerStart = Marker(mapViewObj).apply {
            position = geoPoints.first()
            icon = context.getDrawable(R.drawable.ic_location_start)
            setOnMarkerClickListener { _, _ ->
                setCutPoint(locations.keys.first())
                true
            }
        }
        val markerEnd = Marker(mapViewObj).apply {
            position = geoPoints.last()
            icon = context.getDrawable(R.drawable.ic_location_end)
            setOnMarkerClickListener { _, _ ->
                setCutPoint(locations.keys.last())
                true
            }
        }

        mapViewObj?.overlays?.add(polylineBorder)
        if (!showData || cutMode) {
            mapViewObj?.overlays?.add(polyline)
        }
        else {
            shownKeys
                .associateWith { locations[it]!! }
                .getLocationData()
                .forEach {
                    val coloredPolyline = Polyline(mapViewObj).apply {
                        setPoints(it.points.map { it.toGeoPoint() } )
                        outlinePaint.color = calculateColor(it.speed.roundToInt()).toArgb()
                        outlinePaint.strokeWidth = 3f * screenDpi
                        outlinePaint.strokeCap = Paint.Cap.ROUND

                        setOnClickListener { _, _, _ -> // polyline, mapView, eventPos
                            selectedTime = "${TimeFormatterWithSecond.format(it.start)} - ${TimeFormatterWithSecond.format(it.end)}"
                            selectedSpeed = it.speed.roundToInt()
                            selectedFirstKey = it.start
                            selectedSecondKey = it.end
                            isSelected = true
                            updateRemaining = 1000000
                            updateMap()

                            true
                        }
                    }

                    mapViewObj?.overlays?.add(
                        coloredPolyline
                    )
                }

            if (isSelected && mapViewObj != null) {
                val firstKey = selectedFirstKey
                val secondKey = selectedSecondKey

                val polylinePoints = listOf(
                    locations[firstKey]!!,
                    locations[secondKey]!!
                ).map { GeoPoint(it.latitude, it.longitude) }

                mapViewObj?.overlays?.add(
                    Polyline(mapViewObj).apply {
                        setPoints(polylinePoints)
                        outlinePaint.color = Color.DarkGray.toArgb()
                        outlinePaint.strokeWidth = 7f * screenDpi
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        id = firstKey.toString()
                    }
                )
                mapViewObj?.overlays?.add(
                    Polyline(mapViewObj).apply {
                        setPoints(polylinePoints)
                        outlinePaint.color = calculateColor(selectedSpeed).toArgb()
                        outlinePaint.strokeWidth = 3.5f * screenDpi
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        id = firstKey.toString()
                    }
                )
            }
            else {
                Log.d("NanHistoryDebug", "No selected polyline")
            }
        }

        val currentCutTime1 = cutTime1
        val currentCutTime2 = cutTime2
        if (cutMode && currentCutTime1 != null && currentCutTime2 != null) {
            val startTime =
                if (currentCutTime1 < currentCutTime2) currentCutTime1
                else currentCutTime2
            val endTime =
                if (currentCutTime1 > currentCutTime2) currentCutTime1
                else currentCutTime2
            val selectedArea = mutableListOf<ZonedDateTime>()
//            Log.d("NanHistoryDebug", "START TIME: $startTime, END TIME: $endTime")
            shownKeys
                .forEach selectionIterator@{
                    if (it < startTime || it > endTime)
                        return@selectionIterator
                    selectedArea.add(it)
                }
            val selectedPolyline = Polyline(mapViewObj).apply {
                setPoints(selectedArea.map { locations[it]!!.toGeoPoint() })
                outlinePaint.color = android.graphics.Color.BLUE
                outlinePaint.strokeWidth = 3f * screenDpi
                outlinePaint.strokeCap = Paint.Cap.ROUND
            }
            mapViewObj?.overlays?.add(selectedPolyline)
        }

        if (showPoints || cutMode) {
            if (cutTime1 == null || cutTime2 == null)
                for ((shownKey, shownCoordinate) in shownCoordinates.mapIndexed { idx, it -> shownKeys[idx] to it }) {
                    mapViewObj?.overlays?.add(Marker(mapViewObj).apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        position = shownCoordinate
                        icon = context.getDrawable(R.drawable.ic_map_touch_point)

                        setOnMarkerClickListener { _, _ ->
                            setCutPoint(shownKey)
                            true
                        }
                    })
                }
            if (cutTime1 != null) mapViewObj?.overlays?.add(Marker(mapViewObj).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                position = locations[cutTime1]!!.toGeoPoint()
                icon = context.getDrawable(R.drawable.ic_map_stop_point)
            })
            if (cutTime2 != null) mapViewObj?.overlays?.add(Marker(mapViewObj).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                position = locations[cutTime2]!!.toGeoPoint()
                icon = context.getDrawable(R.drawable.ic_map_stop_point)
            })
        }

        if (shownCoordinates.size > 1) mapViewObj?.overlays?.add(markerStart)
        mapViewObj?.overlays?.add(markerEnd)

        mapViewObj?.invalidate()
    }

    if (maxDetail != inMaxDetail) {
        inMaxDetail = maxDetail
        updateMap()
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        factory = { ctx ->
            MapView(ctx).apply {
                // Configure the MapView
                setMultiTouchControls(true)

                addMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent?): Boolean {
                        Log.d("MapListener", "Map scrolled")
                        center = GeoPoint(mapCenter.latitude, mapCenter.longitude)
                        return true // Return true if the event was handled
                    }

                    override fun onZoom(event: ZoomEvent?): Boolean {
                        zoomLevel = event?.zoomLevel ?: zoomLevel
                        return true
                    }
                })
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                val currentFirstLoad = firstLoad

                if (currentFirstLoad) {
                    setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)

                    post {
                        zoomToBoundingBox(shownCoordinates.toBoundingBox(), false)
                    }
                    firstLoad = false
                }
                else {
                    val currentZoom = zoomLevel
                    val currentCenter = center
                    post {
                        setZoomLevel(currentZoom)
                        controller.setCenter(currentCenter)
                    }
                }

                minZoomLevel = 4.0
                maxZoomLevel = 20.0
                post {
                    updateMap()
                    mapViewObj = this
                }
            }
        },
        update = { mapView ->
            mapViewObj = mapView
            Log.d("NanHistoryDebug", "MAP UPDATE! (needUpdate: $needUpdate)")
            if ((prevZoomLevel - zoomLevel).absoluteValue >= 1.0 || needUpdate) {
                prevZoomLevel = zoomLevel
                updateMap()
                needUpdate = false // TODO
            }
            // Update MapView if needed
        }
    ).also { view ->
        DisposableEffect(Unit) {
            onDispose {
            }
        }

    }

    if (isSelected) Box(
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(288.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val infoScrollState = rememberScrollState()
                Row(
                    Modifier.weight(1f)
                ) {
                    Row(
                        Modifier
                            .horizontalScroll(infoScrollState),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_schedule),
                            contentDescription = "Time",
                            modifier = Modifier.padding(end = 8.dp),
                            tint = Color.Gray
                        )
                        Text(
                            selectedTime,
                            color = Color.Gray
                        )

                        Box(modifier = Modifier.width(16.dp))

                        Icon(
                            painterResource(R.drawable.ic_speed),
                            contentDescription = "Speed",
                            modifier = Modifier.padding(end = 8.dp),
                            tint = Color.Gray
                        )
                        Text(
                            "$selectedSpeed Km/h",
                            color = Color.Gray
                        )

                        Box(modifier = Modifier.width(16.dp))

                        Button(
                            onClick = handler@{
                                val location = locations[selectedFirstKey]
                                val stringLocation = location?.toString()
                                if (stringLocation == null) {
                                    Toast.makeText(
                                        context,
                                        "Selected coordinate unknown",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@handler
                                }
                                val gmmIntentUri =
                                    Uri.parse("geo:$stringLocation?q=$stringLocation") // Replace with your latitude & longitude
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")

                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Google Maps is not installed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_location_filled),
                                contentDescription = "Location",
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text("Open")
                        }
                    }
                }
                IconButton(
                    onClick = {
                        isSelected = false
                    }
                ) {
                    Icon(Icons.Rounded.Close, "Close")
                }
            }
        }
    }

    Surface(
        modifier = Modifier
            .requiredHeight(132.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxWidth()
                .padding(
                    bottom = 64.dp,
                    start = 8.dp,
                    end = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.width(64.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(painterResource(R.drawable.ic_zoom), "Zoom level")
                Text(
                    text = "${round(zoomLevel * 10) / 10.0}",
                    color = if (maxDetail) Color(0xFF00A000) else Color.Unspecified
                )
            }
            Box(modifier = Modifier.width(8.dp))
            if (!cutMode) Row(
                modifier = Modifier
                    .horizontalScroll(bottomBarScrollState)
                    .padding(start = 8.dp)
                    .weight(1f)
            ) {
                val onShowPointsClicked: () -> Unit = {
                    showPoints = !showPoints
                    needUpdate = true
                }
                val onShowMovementSpeedClicked: () -> Unit = {
                    showData = !showData
                    needUpdate = true
                }
                Button(
                    onClick = onShowPointsClicked,
                    colors =
                    if (showPoints) ButtonDefaults.buttonColors()
                    else ButtonDefaults.textButtonColors(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Text("Show Point")
                }
                Box(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onShowMovementSpeedClicked,
                    colors =
                    if (showData) ButtonDefaults.buttonColors()
                    else ButtonDefaults.textButtonColors(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Text("Show Data")
                }
            }
            else Row(
                modifier = Modifier
                    .horizontalScroll(bottomBarScrollState)
                    .padding(start = 8.dp)
                    .weight(1f)
            ) {
                Button(
                    onClick = undoCutSelection,
                    colors = ButtonDefaults.buttonColors(),
                    contentPadding = PaddingValues(8.dp),
                    enabled = cutTime1 != null || cutTime2 != null
                ) {
                    Text("Undo Selection")
                }
            }
        }
    }
    if (updateRemaining > 0)
        updateRemaining -= 1
}

@Preview(showBackground = true)
@Composable
fun EventLocationPreview() {
    NanHistoryTheme {
        EventLocationView("")
    }
}