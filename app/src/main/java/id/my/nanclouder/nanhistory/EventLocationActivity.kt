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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toEventEntity
import id.my.nanclouder.nanhistory.db.toHistoryEvent
import id.my.nanclouder.nanhistory.ui.ComponentPlaceholder
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import id.my.nanclouder.nanhistory.utils.Coordinate
import id.my.nanclouder.nanhistory.utils.TimeFormatterWithSecond
import id.my.nanclouder.nanhistory.utils.copyWith
import id.my.nanclouder.nanhistory.utils.getLocationData
import id.my.nanclouder.nanhistory.utils.history.EventRange
import id.my.nanclouder.nanhistory.utils.history.LocationData
import id.my.nanclouder.nanhistory.utils.history.appendToLocationFile
import id.my.nanclouder.nanhistory.utils.history.createLocationFile
import id.my.nanclouder.nanhistory.utils.history.generateEventId
import id.my.nanclouder.nanhistory.utils.history.generateSignature
import id.my.nanclouder.nanhistory.utils.history.getFilePathFromDate
import id.my.nanclouder.nanhistory.utils.history.validateSignature
import id.my.nanclouder.nanhistory.utils.matchOrNull
import id.my.nanclouder.nanhistory.utils.toGeoPoint
import java.io.File
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

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

fun calculateDistance(startPoint: GeoPoint, endPoint: GeoPoint): Float =
    Location("map")
        .apply {
            latitude = startPoint.latitude
            longitude = startPoint.longitude
        }
        .distanceTo(
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

fun calculateAltitudeColor(altitude: Double?): Color {
    if (altitude == null) {
        return Color(0xFF808080)
    }

    // Clamp altitude to 0-1500m range
    val clampedAltitude = altitude.coerceIn(0.0, 1500.0)

    // Normalize altitude to 0-1 range
    val normalized = clampedAltitude / 1500.0

    // Color mapping: Blue (0m) -> Cyan -> Green -> Yellow -> Orange -> Red (1500m)
    val hue = 180.0 - (normalized * 180.0)

    // Saturation increases with altitude (deeper colors at higher elevations)
    val saturation = (0.6 + normalized * 0.4).coerceIn(0.6, 1.0)

    // Lightness decreases slightly at extremes for more visual impact
    val lightness = (0.75 - (normalized * 0.15)).coerceIn(0.35, 0.75)

    return Color.hsl(hue.toFloat(), saturation.toFloat(), lightness.toFloat())
}

fun calculateAccuracyColor(accuracy: Float?): Color {
    if (accuracy == null) {
        return Color(0xFF808080)
    }

    // Clamp accuracy to 5-1000 range
    val clampedAccuracy = accuracy.coerceIn(5f, 1000f)

    // Normalize: 5 (best) -> 0, 1000 (worst) -> 1
    val normalized = (clampedAccuracy - 5f) / (1000f - 5f)

    // Color mapping: Green (5m) -> Yellow -> Orange -> Red (1000m)
    val hue =
        when {
            normalized < 0.33f -> {
                // Green to Yellow (5-335m)
                val t = normalized / 0.33f
                120f - (t * 60f) // 120° to 60°
            }

            normalized < 0.66f -> {
                // Yellow to Orange (335-668m)
                val t = (normalized - 0.33f) / 0.33f
                60f - (t * 30f) // 60° to 30°
            }

            else -> {
                // Orange to Red (668-1000m)
                val t = (normalized - 0.66f) / 0.34f
                30f - (t * 30f) // 30° to 0°
            }
        }

    // Saturation increases with worse accuracy (more vibrant at extremes)
    val saturation = (0.5f + normalized * 0.5f).coerceIn(0.5f, 1f)

    // Lightness slightly decreases for worse accuracy (darker reds are more urgent)
    val lightness = (0.55f - normalized * 0.15f).coerceIn(0.4f, 0.6f)

    return Color.hsl(hue, saturation, lightness)
}

@Composable
fun EventLocationView(eventId: String, startAsCutMode: Boolean = false) {
    val newUI = Config.appearanceNewUI.getCache()
    if (newUI) EventLocationView_New(eventId, startAsCutMode)
    else EventLocationView_Old(eventId, startAsCutMode)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventLocationView_Old(eventId: String, startAsCutMode: Boolean = false) {
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

    val eventLocations = eventData?.getLocations(context) ?: emptyList()
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
                    if (eventData != null) Text(if (cutMode) "Cut Event" else "Event Map")
                    else ComponentPlaceholder(Modifier.size(128.dp, 16.dp))
                },
                navigationIcon = {
                    IconButton(onClick = { context.getActivity()!!.finish() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (eventData == null)
                        ComponentPlaceholder(Modifier
                            .size(24.dp)
                            .padding(8.dp))
                    if (cutMode && eventData is EventRange)
                        IconButton(
                            onClick = {
                                // TODO
                                val event =
                                    EventRange(
                                        title =
                                            "Cut of ${eventData.title}",
                                        description =
                                            eventData
                                                .description +
                                                    if (eventData
                                                            .description
                                                            .isBlank()
                                                    )
                                                        ""
                                                    else
                                                        "\n" +
                                                                "Cut of ${eventData.title}",
                                        time = cutStart!!,
                                        favorite =
                                            eventData.favorite,
                                        tags = eventData.tags,
                                        end = cutEnd!!,
                                        locationDescriptions =
                                            eventData
                                                .locationDescriptions
                                                .filter {
                                                    it.key >=
                                                            cutStart!! &&
                                                            it.key <=
                                                            cutEnd!!
                                                }
                                                .toMutableMap(),
                                        metadata =
                                            eventData.metadata,
                                        versionNumber =
                                            eventData
                                                .versionNumber
                                    )
                                        .apply {
                                            metadata["original_event_id"] =
                                                eventData.id
                                            metadata[
                                                "original_event_time"] =
                                                eventData
                                                    .time
                                                    .toOffsetDateTime()
                                                    .toString()
                                            metadata["original_event_end"] =
                                                eventData
                                                    .end
                                                    .toOffsetDateTime()
                                                    .toString()
                                            if (metadata["root_event_id"] ==
                                                null
                                            )
                                                metadata[
                                                    "root_event_id"] =
                                                    eventData.id
                                            if (metadata[
                                                    "root_event_time"] ==
                                                null
                                            )
                                                metadata[
                                                    "root_event_time"] =
                                                    eventData
                                                        .time
                                                        .toOffsetDateTime()
                                                        .toString()
                                            if (metadata[
                                                    "root_event_end"] ==
                                                null
                                            )
                                                metadata[
                                                    "root_event_end"] =
                                                    eventData
                                                        .end
                                                        .toOffsetDateTime()
                                                        .toString()
                                        }
                                val locationFile =
                                    createLocationFile(context, event.time)
                                val locationsData =
                                    eventLocations.filter {
                                        it.time >= cutStart!! &&
                                                it.time <= cutEnd!!
                                    }
                                locationFile.delete()
                                locationsData.appendToLocationFile(locationFile)
                                event.locationPath =
                                    locationFile.absolutePath.removePrefix(
                                        File(context.filesDir, "locations")
                                            .absolutePath + "/"
                                    )

                                if (eventData.audio != null) {
                                    val audioFile =
                                        File(
                                            context.filesDir,
                                            "audio/${eventData.audio}"
                                        )
                                    audioFile.parentFile?.let {
                                        val id = generateEventId()
                                        val targetPath = "${it.name}/$id.m4a"
                                        val targetFile = File(it, "/$id.m4a")
                                        Log.d(
                                            "NanHistoryDebug",
                                            "AUDIO COPY | FROM: ${audioFile.absolutePath} | TO: ${targetFile.absolutePath}"
                                        )
                                        audioFile.copyTo(targetFile)

                                        event.audio = targetPath
                                    }
                                }
                                if (eventData.validateSignature(context = context))
                                    event.generateSignature(context, true)

                                val db = AppDatabase.getInstance(context)
                                val dao = db.appDao()

                                scope.launch {
                                    dao.insertEvent(event.toEventEntity())
                                }

                                cutMode = false
                                Toast.makeText(
                                    context,
                                    "${event.title} has been saved",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()

                                val resultIntent =
                                    Intent().apply {
                                        putExtra(
                                            "path",
                                            getFilePathFromDate(
                                                event.time.toLocalDate()
                                            )
                                        )
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
                    if (eventData is EventRange)
                        IconButton({ cutMode = !cutMode }, enabled = !recording) {
                            if (!cutMode)
                                Icon(
                                    painterResource(R.drawable.ic_content_cut),
                                    "Cut Mode"
                                )
                            else Icon(Icons.Rounded.Close, "Exit Cut Mode")
                        }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()) {
            if (locationAvailable) {
                MapHistoryView_Old(
                    locations = eventLocations.associate { it.time to it.location },
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
            } else ComponentPlaceholder(Modifier
                .weight(1f)
                .padding(8.dp))
        }
    }
}

@Composable
fun MapHistoryView_Old(
    locations: Map<ZonedDateTime, Coordinate>,
    onPointsSelected: (ZonedDateTime?, ZonedDateTime?) -> Unit,
    modifier: Modifier = Modifier,
    cutMode: Boolean = false,
) {
    val context = LocalContext.current

    val mapKeys = locations.keys.sorted()

    val geoPoints =
        mapKeys.sorted().map { GeoPoint(locations[it]!!.latitude, locations[it]!!.longitude) }

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
            val coordinates =
                geoPoints
                    .dropLast(1)
                    .mapIndexed { a, b -> a to b }
                    .filter {
                        it.first % (2.0.pow((15 - zoomLevel.roundToInt()))) == 0.0
                    }
                    .map { it.second }
                    .toMutableList()
            coordinates.add(geoPoints.last())
            coordinates.toList()
        } else geoPoints

    val shownKeys =
        if (geoPoints.size > 2) {
            val coordinates =
                mapKeys.dropLast(1)
                    .mapIndexed { a, b -> a to b }
                    .filter {
                        it.first % (2.0.pow((15 - zoomLevel.roundToInt()))) == 0.0
                    }
                    .map { it.second }
                    .toMutableList()
            coordinates.add(mapKeys.last())
            coordinates.toList()
        } else mapKeys

    var inMaxDetail by remember { mutableStateOf(false) }
    val maxDetail = shownCoordinates.size == geoPoints.size

    val screenDpi = LocalDensity.current.run { 1.dp.toPx() }

    var updateMap = {}

    val updateCutSelection = {
        onPointsSelected(cutTime1, cutTime2)
        if (cutTime1 == null || cutTime2 == null) updateMap() else needUpdate = true
    }

    val setCutPoint =
        cutPointSetter@{ time: ZonedDateTime ->
            if (time == cutTime1 || time == cutTime2) return@cutPointSetter

            if (cutTime1 == null) cutTime1 = time
            else if (cutTime2 == null) cutTime2 = time else return@cutPointSetter

            updateCutSelection()
        }

    val undoCutSelection =
        undo@{
            if (cutTime2 != null) cutTime2 = null
            else if (cutTime1 != null) cutTime1 = null else return@undo

            updateCutSelection()
        }

    if (cutMode != cutModeCheck) {
        cutModeCheck = cutMode
        cutTime1 = null
        cutTime2 = null
        needUpdate = true
    }

    updateMap =
        updater@{
            if (mapViewObj == null) return@updater

            mapViewObj?.overlays?.clear()

            val polyline =
                Polyline(mapViewObj).apply {
                    setPoints(shownCoordinates)
                    outlinePaint.color =
                        if (!cutMode) android.graphics.Color.BLUE
                        else android.graphics.Color.GRAY
                    outlinePaint.strokeWidth = 3f * screenDpi
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                }
            val polylineBorder =
                Polyline(mapViewObj).apply {
                    setPoints(shownCoordinates)
                    outlinePaint.color = android.graphics.Color.rgb(0, 0, 20)
                    outlinePaint.strokeWidth = 4f * screenDpi
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                }
            val markerStart =
                Marker(mapViewObj).apply {
                    position = geoPoints.first()
                    icon = context.getDrawable(R.drawable.ic_location_start)
                    setOnMarkerClickListener { _, _ ->
                        setCutPoint(locations.keys.first())
                        true
                    }
                }
            val markerEnd =
                Marker(mapViewObj).apply {
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
            } else {
                shownKeys.associateWith { locations[it]!! }.getLocationData().forEach {
                    val coloredPolyline =
                        Polyline(mapViewObj).apply {
                            setPoints(it.points.map { point -> point.toGeoPoint() })
                            outlinePaint.color =
                                calculateColor(it.speed.roundToInt()).toArgb()
                            outlinePaint.strokeWidth = 3f * screenDpi
                            outlinePaint.strokeCap = Paint.Cap.ROUND

                            setOnClickListener { _, _, _ -> // polyline, mapView, eventPos
                                selectedTime =
                                    "${TimeFormatterWithSecond.format(it.start)} - ${
                                        TimeFormatterWithSecond.format(
                                            it.end
                                        )
                                    }"
                                selectedSpeed = it.speed.roundToInt()
                                selectedFirstKey = it.start
                                selectedSecondKey = it.end
                                isSelected = true
                                updateRemaining = 1000000
                                updateMap()

                                true
                            }
                        }

                    mapViewObj?.overlays?.add(coloredPolyline)
                }

                if (isSelected && mapViewObj != null) {
                    val firstKey = selectedFirstKey
                    val secondKey = selectedSecondKey

                    val polylinePoints =
                        listOf(locations[firstKey]!!, locations[secondKey]!!).map {
                            GeoPoint(it.latitude, it.longitude)
                        }

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
                } else {
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
                //            Log.d("NanHistoryDebug", "START TIME: $startTime, END TIME:
                // $endTime")
                shownKeys.forEach selectionIterator@{
                    if (it < startTime || it > endTime) return@selectionIterator
                    selectedArea.add(it)
                }
                val selectedPolyline =
                    Polyline(mapViewObj).apply {
                        setPoints(selectedArea.map { locations[it]!!.toGeoPoint() })
                        outlinePaint.color = android.graphics.Color.BLUE
                        outlinePaint.strokeWidth = 3f * screenDpi
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                    }
                mapViewObj?.overlays?.add(selectedPolyline)
            }

            if (showPoints || cutMode) {
                if (cutTime1 == null || cutTime2 == null)
                    for ((shownKey, shownCoordinate) in
                    shownCoordinates.mapIndexed { idx, it ->
                        shownKeys[idx] to it
                    }) {
                        mapViewObj?.overlays?.add(
                            Marker(mapViewObj).apply {
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                position = shownCoordinate
                                icon =
                                    context.getDrawable(
                                        R.drawable.ic_map_touch_point
                                    )

                                setOnMarkerClickListener { _, _ ->
                                    setCutPoint(shownKey)
                                    true
                                }
                            }
                        )
                    }
                if (cutTime1 != null)
                    mapViewObj?.overlays?.add(
                        Marker(mapViewObj).apply {
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            position = locations[cutTime1]!!.toGeoPoint()
                            icon = context.getDrawable(R.drawable.ic_map_stop_point)
                        }
                    )
                if (cutTime2 != null)
                    mapViewObj?.overlays?.add(
                        Marker(mapViewObj).apply {
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            position = locations[cutTime2]!!.toGeoPoint()
                            icon = context.getDrawable(R.drawable.ic_map_stop_point)
                        }
                    )
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

                addMapListener(
                    object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            Log.d("MapListener", "Map scrolled")
                            center =
                                GeoPoint(
                                    mapCenter.latitude,
                                    mapCenter.longitude
                                )
                            return true // Return true if the event was handled
                        }

                        override fun onZoom(event: ZoomEvent?): Boolean {
                            zoomLevel = event?.zoomLevel ?: zoomLevel
                            return true
                        }
                    }
                )
                zoomController.setVisibility(
                    CustomZoomButtonsController.Visibility.NEVER
                )

                val currentFirstLoad = firstLoad

                if (currentFirstLoad) {
                    setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)

                    post { zoomToBoundingBox(shownCoordinates.toBoundingBox(), false) }
                    firstLoad = false
                } else {
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
    )
        .also { view -> DisposableEffect(Unit) { onDispose {} } }

    if (isSelected)
        Box(
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
                    Row(Modifier.weight(1f)) {
                        Row(
                            Modifier.horizontalScroll(infoScrollState),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_schedule),
                                contentDescription = "Time",
                                modifier = Modifier.padding(end = 8.dp),
                                tint = Color.Gray
                            )
                            Text(selectedTime, color = Color.Gray)

                            Box(modifier = Modifier.width(16.dp))

                            Icon(
                                painterResource(R.drawable.ic_speed),
                                contentDescription = "Speed",
                                modifier = Modifier.padding(end = 8.dp),
                                tint = Color.Gray
                            )
                            Text("$selectedSpeed Km/h", color = Color.Gray)

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
                                        )
                                            .show()
                                        return@handler
                                    }
                                    val gmmIntentUri =
                                        Uri.parse(
                                            "geo:$stringLocation?q=$stringLocation"
                                        ) // Replace with your latitude &
                                    // longitude
                                    val mapIntent =
                                        Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    mapIntent.setPackage(
                                        "com.google.android.apps.maps"
                                    )

                                    if (mapIntent.resolveActivity(
                                            context.packageManager
                                        ) != null
                                    ) {
                                        context.startActivity(mapIntent)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Google Maps is not installed",
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
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
                    IconButton(onClick = { isSelected = false }) {
                        Icon(Icons.Rounded.Close, "Close")
                    }
                }
            }
        }

    Surface(modifier = Modifier.requiredHeight(132.dp)) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .fillMaxWidth()
                    .padding(bottom = 64.dp, start = 8.dp, end = 8.dp),
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
            if (!cutMode)
                Row(
                    modifier =
                        Modifier
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
                    ) { Text("Show Point") }
                    Box(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onShowMovementSpeedClicked,
                        colors =
                            if (showData) ButtonDefaults.buttonColors()
                            else ButtonDefaults.textButtonColors(),
                        contentPadding = PaddingValues(8.dp)
                    ) { Text("Show Data") }
                }
            else
                Row(
                    modifier =
                        Modifier
                            .horizontalScroll(bottomBarScrollState)
                            .padding(start = 8.dp)
                            .weight(1f)
                ) {
                    Button(
                        onClick = undoCutSelection,
                        colors = ButtonDefaults.buttonColors(),
                        contentPadding = PaddingValues(8.dp),
                        enabled = cutTime1 != null || cutTime2 != null
                    ) { Text("Undo Selection") }
                }
        }
    }
    if (updateRemaining > 0) updateRemaining -= 1
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventLocationView_New(eventId: String, startAsCutMode: Boolean = false) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

    val eventState by dao.getEventFlowById(eventId).collectAsState(null)
    val eventData = eventState?.toHistoryEvent()

    val recording = matchOrNull<Boolean>(eventData?.metadata?.get("recording")) ?: false

    val eventLocations = eventData?.getLocations(context) ?: emptyList()
    val locationAvailable = eventLocations.isNotEmpty()

    var cutMode by rememberSaveable { mutableStateOf(startAsCutMode) }
    var cutStart by rememberSaveable { mutableStateOf<Int?>(null) }
    var cutEnd by rememberSaveable { mutableStateOf<Int?>(null) }

    Log.d("NanHistoryDebug", "eventData: $eventData")

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    if (eventData != null)
                        Text(
                            if (cutMode) "Cut Event" else "Event Map",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    else ComponentPlaceholder(Modifier.size(128.dp, 16.dp))
                },
                navigationIcon = {
                    IconButton(onClick = { context.getActivity()!!.finish() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (eventData == null)
                        ComponentPlaceholder(Modifier
                            .size(24.dp)
                            .padding(8.dp))
                    if (cutMode && eventData is EventRange)
                        IconButton(
                            onClick = {
                                val event =
                                    EventRange(
                                        title =
                                            "Cut of ${eventData.title}",
                                        description =
                                            eventData
                                                .description +
                                                    if (eventData
                                                            .description
                                                            .isBlank()
                                                    )
                                                        ""
                                                    else
                                                        "\n" +
                                                                "Cut of ${eventData.title}",
                                        time =
                                            eventLocations[
                                                cutStart!!]
                                                .time,
                                        favorite =
                                            eventData.favorite,
                                        tags = eventData.tags,
                                        end =
                                            eventLocations[
                                                cutEnd!!]
                                                .time,
                                        locationDescriptions =
                                            eventData
                                                .locationDescriptions
                                                .keys
                                                .mapIndexed { a,
                                                              b ->
                                                    a to b
                                                }
                                                .associate { (
                                                                 idx,
                                                                 key)
                                                    ->
                                                    idx to
                                                            (key to
                                                                    eventData
                                                                        .locationDescriptions[
                                                                        key]!!)
                                                }
                                                .filter {
                                                    it.key >=
                                                            cutStart!! &&
                                                            it.key <=
                                                            cutEnd!!
                                                }
                                                .map {
                                                    it.value
                                                }
                                                .toMap()
                                                .toMutableMap(),
                                        metadata =
                                            eventData.metadata,
                                    )
                                        .apply {
                                            metadata["original_event_id"] =
                                                eventData.id
                                            metadata[
                                                "original_event_time"] =
                                                eventData
                                                    .time
                                                    .toOffsetDateTime()
                                                    .toString()
                                            metadata["original_event_end"] =
                                                eventData
                                                    .end
                                                    .toOffsetDateTime()
                                                    .toString()
                                            if (metadata["root_event_id"] ==
                                                null
                                            )
                                                metadata[
                                                    "root_event_id"] =
                                                    eventData.id
                                            if (metadata[
                                                    "root_event_time"] ==
                                                null
                                            )
                                                metadata[
                                                    "root_event_time"] =
                                                    eventData
                                                        .time
                                                        .toOffsetDateTime()
                                                        .toString()
                                            if (metadata[
                                                    "root_event_end"] ==
                                                null
                                            )
                                                metadata[
                                                    "root_event_end"] =
                                                    eventData
                                                        .end
                                                        .toOffsetDateTime()
                                                        .toString()
                                        }
                                val locationFile =
                                    createLocationFile(context, event.time)
                                val locationsData =
                                    eventLocations
                                        .withIndex()
                                        .filter {
                                            it.index >= cutStart!! &&
                                                    it.index <= cutEnd!!
                                        }
                                        .map { it.value }
                                locationFile.delete()
                                locationsData.appendToLocationFile(locationFile)
                                event.locationPath =
                                    locationFile.absolutePath.removePrefix(
                                        File(context.filesDir, "locations")
                                            .absolutePath + "/"
                                    )

                                if (eventData.audio != null) {
                                    val audioFile =
                                        File(
                                            context.filesDir,
                                            "audio/${eventData.audio}"
                                        )
                                    audioFile.parentFile?.let {
                                        val id = generateEventId()
                                        val targetPath = "${it.name}/$id.m4a"
                                        val targetFile = File(it, "/$id.m4a")
                                        Log.d(
                                            "NanHistoryDebug",
                                            "AUDIO COPY | FROM: ${audioFile.absolutePath} | TO: ${targetFile.absolutePath}"
                                        )
                                        audioFile.copyTo(targetFile)

                                        event.audio = targetPath
                                    }
                                }
                                if (eventData.validateSignature(context = context))
                                    event.generateSignature(context, true)

                                val db = AppDatabase.getInstance(context)
                                val dao = db.appDao()

                                scope.launch {
                                    dao.insertEvent(event.toEventEntity())
                                }

                                cutMode = false
                                Toast.makeText(
                                    context,
                                    "${event.title} has been saved",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()

                                val resultIntent =
                                    Intent().apply {
                                        putExtra(
                                            "path",
                                            getFilePathFromDate(
                                                event.time.toLocalDate()
                                            )
                                        )
                                    }

                                context.getActivity()?.setResult(2, resultIntent)
                                if (startAsCutMode) context.getActivity()?.finish()
                            },
                            enabled = cutStart != null && cutEnd != null
                        ) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = "Confirm",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    if (eventData is EventRange)
                        IconButton(
                            onClick = { cutMode = !cutMode },
                            enabled = !recording
                        ) {
                            if (cutMode)
                                Icon(
                                    Icons.Rounded.Close,
                                    "Exit Cut Mode",
                                    tint =
                                        if (cutMode)
                                            MaterialTheme.colorScheme
                                                .error
                                        else
                                            MaterialTheme.colorScheme
                                                .primary
                                )
                            else
                                Icon(
                                    painterResource(R.drawable.ic_content_cut),
                                    "Exit Cut Mode",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                        }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(paddingValues)
                    .fillMaxSize()
        ) {
            if (locationAvailable) {
                MapHistoryView_New(
                    locations = eventLocations,
                    onPointsSelected = handleSelect@{ time1, time2 ->
                        if (!cutMode) return@handleSelect
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
            } else ComponentPlaceholder(Modifier
                .weight(1f)
                .padding(8.dp))
        }
    }
}

enum class DataMode {
    SPEED,
    ACCELERATION,
    TIME,
    ACCURACY,
    BEARING,
    ALTITUDE
}

@Composable
fun MapHistoryView_New(
    locations: List<LocationData>,
    onPointsSelected: (Int?, Int?) -> Unit,
    modifier: Modifier = Modifier,
    cutMode: Boolean = false,
) {
    val context = LocalContext.current

    val geoPoints = locations.map { GeoPoint(it.location.latitude, it.location.longitude) }

    val bottomBarScrollState = rememberScrollState()

    var cutIndex1 by rememberSaveable { mutableStateOf<Int?>(null) }
    var cutIndex2 by rememberSaveable { mutableStateOf<Int?>(null) }
    var cutModeCheck by remember { mutableStateOf(cutMode) }

    var selectedCutPoint by rememberSaveable { mutableStateOf<ZonedDateTime?>(null) }

    var showPoints by rememberSaveable { mutableStateOf(false) }
    var showData by rememberSaveable { mutableStateOf(false) }
    var dataMode by rememberSaveable {
        mutableStateOf<DataMode>(DataMode.SPEED)
    } // DataMode.SPEED, DataMode.ACCELERATION, DataMode.TIME

    var needUpdate by remember { mutableStateOf(false) }
    var firstLoad by remember { mutableStateOf(true) }

    var mapViewObj by remember { mutableStateOf<MapView?>(null) }

    if (geoPoints.isEmpty()) return

    var zoomLevel by remember { mutableDoubleStateOf(0.0) }
    var prevZoomLevel by remember { mutableDoubleStateOf(zoomLevel) }
    var center by remember { mutableStateOf(GeoPoint(0.0, 0.0)) }

    var isSelected by remember { mutableStateOf(false) }

    var selectedSpeed by remember { mutableIntStateOf(0) }
    var selectedTime by remember { mutableStateOf("") }
    var selectedFirstKey by remember { mutableIntStateOf(0) }
    var selectedSecondKey by remember { mutableIntStateOf(0) }

    var updateRemaining by remember { mutableIntStateOf(0) }

    isSelected = showData && isSelected

    val shownCoordinates =
        if (geoPoints.size > 2) {
            val coordinates =
                geoPoints
                    .dropLast(1)
                    .mapIndexed { a, b -> a to b }
                    .filter {
                        it.first % (2.0.pow((15 - zoomLevel.roundToInt()))) == 0.0
                    }
                    .toMap()
                    .toMutableMap()
            coordinates[locations.size - 1] = geoPoints.last()
            coordinates
        } else geoPoints.withIndex().associate { it.index to it.value }

    val shownLocations =
        if (geoPoints.size > 2) {
            val coordinates =
                locations
                    .dropLast(1)
                    .mapIndexed { a, b -> a to b }
                    .filter {
                        it.first % (2.0.pow((15 - zoomLevel.roundToInt()))) == 0.0
                    }
                    .toMap()
                    .toMutableMap()
            coordinates[locations.size - 1] = locations.last()
            coordinates
        } else locations.withIndex().associate { it.index to it.value }

    var inMaxDetail by remember { mutableStateOf(false) }
    val maxDetail = shownCoordinates.size == geoPoints.size

    val screenDpi = LocalDensity.current.run { 1.dp.toPx() }

    var updateMap = {}

    val updateCutSelection = {
        onPointsSelected(cutIndex1, cutIndex2)
        if (cutIndex1 == null || cutIndex2 == null) updateMap() else needUpdate = true
    }

    val setCutPoint =
        cutPointSetter@{ index: Int ->
            if (index == cutIndex1 || index == cutIndex2) return@cutPointSetter

            if (cutIndex1 == null) cutIndex1 = index
            else if (cutIndex2 == null) cutIndex2 = index else return@cutPointSetter

            updateCutSelection()
        }

    val undoCutSelection =
        undo@{
            if (cutIndex2 != null) cutIndex2 = null
            else if (cutIndex1 != null) cutIndex1 = null else return@undo

            updateCutSelection()
        }

    if (cutMode != cutModeCheck) {
        cutModeCheck = cutMode
        cutIndex1 = null
        cutIndex2 = null
        needUpdate = true
    }

    updateMap =
        updater@{
            if (mapViewObj == null) return@updater

            mapViewObj?.overlays?.clear()

            val polyline =
                Polyline(mapViewObj).apply {
                    setPoints(shownCoordinates.values.toList())
                    outlinePaint.color =
                        if (!cutMode) android.graphics.Color.BLUE
                        else android.graphics.Color.GRAY
                    outlinePaint.strokeWidth = 3f * screenDpi
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                }
            val polylineBorder =
                Polyline(mapViewObj).apply {
                    setPoints(shownCoordinates.values.toList())
                    outlinePaint.color = android.graphics.Color.rgb(0, 0, 20)
                    outlinePaint.strokeWidth = 4f * screenDpi
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                }
            val markerStart =
                Marker(mapViewObj).apply {
                    position = geoPoints.first()
                    icon = context.getDrawable(R.drawable.ic_location_start)
                    setOnMarkerClickListener { _, _ ->
                        if (cutMode) setCutPoint(0)
                        true
                    }
                }
            val markerEnd =
                Marker(mapViewObj).apply {
                    position = geoPoints.last()
                    icon = context.getDrawable(R.drawable.ic_location_end)
                    setOnMarkerClickListener { _, _ ->
                        setCutPoint(locations.size - 1)
                        true
                    }
                }

            mapViewObj?.overlays?.add(polylineBorder)
            if (!showData || cutMode) {
                mapViewObj?.overlays?.add(polyline)
            } else {
                shownLocations.map { it.value }.getLocationData().forEach {
                    val indices =
                        shownLocations
                            .filter { location ->
                                location.value.time == it.start ||
                                        location.value.time == it.end
                            }
                            .map { location -> location.key }

                    if (dataMode in listOf(DataMode.SPEED, DataMode.ACCELERATION, DataMode.TIME)
                    ) {
                        val coloredPolyline =
                            Polyline(mapViewObj).apply {
                                setPoints(it.points.map { point -> point.toGeoPoint() })
                                outlinePaint.color =
                                    when (dataMode) {
                                        DataMode.SPEED ->
                                            calculateColor(it.speed.roundToInt())
                                                .toArgb()

                                        DataMode.ACCELERATION ->
                                            calculateAccelerationColor(
                                                it.acceleration
                                                    .roundToInt()
                                            )
                                                .toArgb()

                                        DataMode.TIME ->
                                            calculateTimeColor(
                                                it.start,
                                                it.end,
                                                locations.first().time,
                                                locations.last().time
                                            )
                                                .toArgb()

                                        else ->
                                            calculateColor(it.speed.roundToInt())
                                                .toArgb()
                                    }
                                outlinePaint.strokeWidth = 3f * screenDpi
                                outlinePaint.strokeCap = Paint.Cap.ROUND

                                setOnClickListener { _, _, _ ->
                                    selectedTime =
                                        "${TimeFormatterWithSecond.format(it.start)} - ${
                                            TimeFormatterWithSecond.format(
                                                it.end
                                            )
                                        }"
                                    selectedSpeed = it.speed.roundToInt()
                                    selectedFirstKey = indices.first()
                                    selectedSecondKey = indices.last()
                                    isSelected = true
                                    updateRemaining = 1000000
                                    updateMap()
                                    true
                                }
                            }

                        mapViewObj?.overlays?.add(coloredPolyline)
                    } else {
                        val points =
                            it.locationData.map { loc -> loc.location }.let { locationData
                                ->
                                listOf(
                                    locationData.first().toGeoPoint(),
                                    GeoPoint(
                                        (locationData.first().latitude +
                                                locationData.last().latitude) / 2.0,
                                        (locationData.first().longitude +
                                                locationData.last().longitude) /
                                                2.0,
                                    ),
                                    locationData.last().toGeoPoint()
                                )
                            }

                        val coloredPolyline1 =
                            Polyline(mapViewObj).apply {
                                setPoints(points.take(2))
                                outlinePaint.color = when (dataMode) {
                                    DataMode.ACCURACY -> calculateAccuracyColor(
                                        it.locationData.first().accuracy
                                    ).toArgb()

                                    DataMode.BEARING -> Color.hsl(
                                        it.locationData.first().bearing ?: 0f,
                                        1f,
                                        0.5f
                                    ).toArgb()

                                    DataMode.ALTITUDE -> calculateAltitudeColor(
                                        it.locationData.first().altitude
                                    ).toArgb()

                                    else -> calculateColor(it.speed.roundToInt()).toArgb()
                                }
                                outlinePaint.strokeWidth = 3f * screenDpi
                                outlinePaint.strokeCap = Paint.Cap.ROUND

                                setOnClickListener { _, _, _ ->
                                    selectedTime =
                                        "${TimeFormatterWithSecond.format(it.start)} - ${
                                            TimeFormatterWithSecond.format(
                                                it.end
                                            )
                                        }"
                                    selectedSpeed = it.speed.roundToInt()
                                    selectedFirstKey = indices.first()
                                    selectedSecondKey = indices.last()
                                    isSelected = true
                                    updateRemaining = 1000000
                                    updateMap()
                                    true
                                }
                            }

                        val coloredPolyline2 =
                            Polyline(mapViewObj).apply {
                                setPoints(points.takeLast(2))
                                outlinePaint.color = when (dataMode) {
                                    DataMode.ACCURACY -> calculateAccuracyColor(
                                        it.locationData.last().accuracy
                                    ).toArgb()

                                    DataMode.BEARING -> Color.hsl(
                                        it.locationData.last().bearing ?: 0f,
                                        1f,
                                        0.5f
                                    ).toArgb()

                                    DataMode.ALTITUDE -> calculateAltitudeColor(
                                        it.locationData.last().altitude
                                    ).toArgb()

                                    else -> calculateColor(it.speed.roundToInt()).toArgb()
                                }
                                outlinePaint.strokeWidth = 3f * screenDpi
                                outlinePaint.strokeCap = Paint.Cap.ROUND

                                setOnClickListener { _, _, _ ->
                                    selectedTime =
                                        "${TimeFormatterWithSecond.format(it.start)} - ${
                                            TimeFormatterWithSecond.format(
                                                it.end
                                            )
                                        }"
                                    selectedSpeed = it.speed.roundToInt()
                                    selectedFirstKey = indices.first()
                                    selectedSecondKey = indices.last()
                                    isSelected = true
                                    updateRemaining = 1000000
                                    updateMap()
                                    true
                                }
                            }

                        mapViewObj?.overlays?.add(coloredPolyline1)
                        mapViewObj?.overlays?.add(coloredPolyline2)
                    }
                }

                if (isSelected && mapViewObj != null) {
                    val firstKey = selectedFirstKey
                    val secondKey = selectedSecondKey

                    val polylinePoints =
                        listOf(locations[firstKey], locations[secondKey]).map {
                            GeoPoint(it.location.latitude, it.location.longitude)
                        }

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
                            outlinePaint.color =
                                when (dataMode) {
                                    DataMode.SPEED ->
                                        calculateColor(selectedSpeed).toArgb()

                                    DataMode.ACCELERATION ->
                                        calculateAccelerationColor(selectedSpeed)
                                            .toArgb()

                                    DataMode.TIME ->
                                        calculateTimeColor(
                                            locations[selectedFirstKey]
                                                .time,
                                            locations[selectedSecondKey]
                                                .time,
                                            locations.first().time,
                                            locations.last().time
                                        )
                                            .toArgb()

                                    else -> calculateColor(selectedSpeed).toArgb()
                                }
                            outlinePaint.strokeWidth = 3.5f * screenDpi
                            outlinePaint.strokeCap = Paint.Cap.ROUND
                            id = firstKey.toString()
                        }
                    )
                }
            }

            val currentCutIndex1 = cutIndex1
            val currentCutIndex2 = cutIndex2
            if (cutMode && currentCutIndex1 != null && currentCutIndex2 != null) {
                val startIndex =
                    if (currentCutIndex1 < currentCutIndex2) currentCutIndex1
                    else currentCutIndex2
                val endIndex =
                    if (currentCutIndex1 > currentCutIndex2) currentCutIndex1
                    else currentCutIndex2
                val selectedArea = mutableListOf<Int>()
                shownLocations.forEach selectionIterator@{ (index, _) ->
                    if (index !in startIndex..endIndex) return@selectionIterator
                    selectedArea.add(index)
                }
                val selectedPolyline =
                    Polyline(mapViewObj).apply {
                        setPoints(selectedArea.map { locations[it].location.toGeoPoint() })
                        outlinePaint.color = android.graphics.Color.BLUE
                        outlinePaint.strokeWidth = 3f * screenDpi
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                    }
                mapViewObj?.overlays?.add(selectedPolyline)
            }

            if (showPoints || cutMode) {
                if (cutIndex1 == null || cutIndex2 == null)
                    for ((shownKey, shownCoordinate) in shownLocations) {
                        mapViewObj?.overlays?.add(
                            Marker(mapViewObj).apply {
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                position = shownCoordinate.location.toGeoPoint()
                                icon =
                                    context.getDrawable(
                                        R.drawable.ic_map_touch_point
                                    )

                                setOnMarkerClickListener { _, _ ->
                                    if (cutMode) setCutPoint(shownKey)
                                    true
                                }
                            }
                        )

                        if (dataMode == DataMode.ACCURACY &&
                            shownCoordinate.accuracy != null &&
                            shownCoordinate.accuracy > 15
                        ) {
                            mapViewObj?.overlays?.add(
                                Polygon(mapViewObj).apply {
                                    val accuracyColor =
                                        calculateAccuracyColor(
                                            shownCoordinate.accuracy
                                        )
                                    points =
                                        Polygon.pointsAsCircle(
                                            shownCoordinate.location
                                                .toGeoPoint(),
                                            shownCoordinate.accuracy.toDouble()
                                        )
                                    fillPaint.color =
                                        accuracyColor.copy(alpha = 0.3f).toArgb()
                                    outlinePaint.strokeWidth = 2f
                                    outlinePaint.color =
                                        accuracyColor.copy(alpha = 0.5f).toArgb()
                                }
                            )
                        }
                        if (dataMode == DataMode.BEARING &&
                            shownCoordinate.bearing != null
                        ) {
                            mapViewObj?.overlays?.add(
                                Polygon(mapViewObj).apply {
                                    val accuracyColor = Color.hsl(shownCoordinate.bearing, 1f, 0.5f)
                                    points =
                                        calculateBearingArcPoints(
                                            shownCoordinate.location.toGeoPoint(),
                                            shownCoordinate.bearing,
                                            (shownCoordinate.bearingAccuracy ?: 0f).coerceIn(10f..360f),
                                            (shownCoordinate.accuracy?.toDouble() ?: 0.0).coerceIn(10.0..360.0)
                                        )
                                    fillPaint.color = accuracyColor.copy(alpha = 0.3f).toArgb()
                                    outlinePaint.strokeWidth = 4f
                                    outlinePaint.color = Color.DarkGray.toArgb()
                                }
                            )
                        }
                    }

                if (cutIndex1 != null)
                    mapViewObj?.overlays?.add(
                        Marker(mapViewObj).apply {
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            position = locations[cutIndex1!!].location.toGeoPoint()
                            icon = context.getDrawable(R.drawable.ic_map_stop_point)
                        }
                    )
                if (cutIndex2 != null)
                    mapViewObj?.overlays?.add(
                        Marker(mapViewObj).apply {
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            position = locations[cutIndex2!!].location.toGeoPoint()
                            icon = context.getDrawable(R.drawable.ic_map_stop_point)
                        }
                    )
            }

            if (shownCoordinates.size > 1) mapViewObj?.overlays?.add(markerStart)
            mapViewObj?.overlays?.add(markerEnd)

            mapViewObj?.invalidate()
        }

    if (maxDetail != inMaxDetail) {
        inMaxDetail = maxDetail
        updateMap()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setMultiTouchControls(true)

                    addMapListener(
                        object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean {
                                center =
                                    GeoPoint(
                                        mapCenter.latitude,
                                        mapCenter.longitude
                                    )
                                return true
                            }

                            override fun onZoom(event: ZoomEvent?): Boolean {
                                zoomLevel = event?.zoomLevel ?: zoomLevel
                                return true
                            }
                        }
                    )
                    zoomController.setVisibility(
                        CustomZoomButtonsController.Visibility.NEVER
                    )

                    val currentFirstLoad = firstLoad

                    if (currentFirstLoad) {
                        setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)

                        post {
                            zoomToBoundingBox(
                                shownCoordinates.map { it.value }.toBoundingBox(),
                                false
                            )
                        }
                        firstLoad = false
                    } else {
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
                if ((prevZoomLevel - zoomLevel).absoluteValue >= 1.0 || needUpdate) {
                    prevZoomLevel = zoomLevel
                    updateMap()
                    needUpdate = false
                }
            }
        )
            .also { _ -> DisposableEffect(Unit) { onDispose {} } }

        // Selected Segment Info Card - Overlaid on map
        if (isSelected) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 4.dp,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val infoScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(infoScrollState),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InfoChip(
                            icon = painterResource(R.drawable.ic_schedule),
                            label = selectedTime
                        )

                        VerticalDivider(
                            modifier = Modifier.height(24.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        val darkMode = isSystemInDarkTheme()

                        InfoChip(
                            icon = painterResource(R.drawable.ic_speed),
                            label = "$selectedSpeed Km/h",
                            color =
                                calculateColor(selectedSpeed)
                                    .let { color ->
                                        if (!darkMode)
                                            color.copyWith(
                                                value = 0.5f,
                                                saturation = 1f
                                            )
                                        else
                                            color.copyWith(
                                                value = 1f,
                                                saturation = 0.4f
                                            )
                                    }
                                    .copy(alpha = 0.8f)
                        )

                        VerticalDivider(
                            modifier = Modifier.height(24.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        val selectedAltitude =
                            locations.let {
                                val first = it[selectedFirstKey]
                                val second = it[selectedSecondKey]

                                if (first.altitude == null) return@let null
                                if (second.altitude == null) return@let null

                                "${(first.altitude * 10.0).roundToInt() / 10.0} - ${(second.altitude * 10.0).roundToInt() / 10.0} m"
                            }

                        if (selectedAltitude != null) {
                            InfoChip(
                                icon = painterResource(R.drawable.ic_expand_all),
                                label = selectedAltitude
                            )

                            VerticalDivider(
                                modifier = Modifier.height(24.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        val selectedAccuracy =
                            locations.let {
                                val first = it[selectedFirstKey]
                                val second = it[selectedSecondKey]

                                if (first.accuracy == null) return@let null
                                if (second.accuracy == null) return@let null

                                "${(first.accuracy * 10.0).roundToInt() / 10.0} - ${(second.accuracy * 10.0).roundToInt() / 10.0} m"
                            }

                        if (selectedAccuracy != null) {
                            InfoChip(
                                icon = painterResource(R.drawable.ic_target),
                                label = selectedAccuracy
                            )

                            VerticalDivider(
                                modifier = Modifier.height(24.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        Button(
                            onClick = handler@{
                                val location = locations[selectedFirstKey]
                                val stringLocation = location?.toString()
                                if (stringLocation == null) {
                                    Toast.makeText(
                                        context,
                                        "Selected coordinate unknown",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                    return@handler
                                }
                                val gmmIntentUri =
                                    Uri.parse(
                                        "geo:$stringLocation?q=$stringLocation"
                                    )
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")

                                if (mapIntent.resolveActivity(context.packageManager) !=
                                    null
                                ) {
                                    context.startActivity(mapIntent)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Google Maps is not installed",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                            },
                            modifier = Modifier.height(40.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_location_filled),
                                contentDescription = "Location",
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 6.dp)
                            )
                            Text("Open", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    IconButton(onClick = { isSelected = false }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Rounded.Close,
                            "Close",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Control Bar - Bottom aligned with modern design
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_zoom),
                                "Zoom level",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "${round(zoomLevel * 10) / 10.0}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight =
                                    if (maxDetail)
                                        androidx.compose.ui.text.font.FontWeight.Bold
                                    else androidx.compose.ui.text.font.FontWeight.Normal
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .horizontalScroll(bottomBarScrollState)
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!cutMode) {
                            Surface(
                                modifier =
                                    Modifier
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                color =
                                    if (showPoints) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainer,
                                tonalElevation = if (showPoints) 2.dp else 0.dp
                            ) {
                                Button(
                                    onClick = {
                                        showPoints = !showPoints
                                        needUpdate = true
                                    },
                                    modifier = Modifier
                                        .height(40.dp)
                                        .fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = Color.Transparent,
                                            contentColor =
                                                if (showPoints)
                                                    MaterialTheme.colorScheme
                                                        .onPrimary
                                                else
                                                    MaterialTheme.colorScheme
                                                        .onSurface
                                        ),
                                    elevation =
                                        ButtonDefaults.buttonElevation(
                                            defaultElevation = 0.dp,
                                            pressedElevation = 0.dp
                                        )
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_map_touch_point),
                                        "Points",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(end = 6.dp)
                                    )
                                    Text("Points", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Surface(
                                modifier =
                                    Modifier
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                color =
                                    if (showData) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainer,
                                tonalElevation = if (showData) 2.dp else 0.dp
                            ) {
                                Row {
                                    Button(
                                        onClick = {
                                            showData = !showData
                                            needUpdate = true
                                        },
                                        modifier = Modifier
                                            .height(40.dp)
                                            .fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = 14.dp),
                                        colors =
                                            ButtonDefaults.buttonColors(
                                                containerColor = Color.Transparent,
                                                contentColor =
                                                    if (showData)
                                                        MaterialTheme
                                                            .colorScheme
                                                            .onPrimary
                                                    else
                                                        MaterialTheme
                                                            .colorScheme
                                                            .onSurface
                                            ),
                                        elevation =
                                            ButtonDefaults.buttonElevation(
                                                defaultElevation = 0.dp,
                                                pressedElevation = 0.dp
                                            )
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.ic_speed),
                                            "Data",
                                            modifier = Modifier
                                                .size(16.dp)
                                                .padding(end = 6.dp)
                                        )
                                        Text("Data", style = MaterialTheme.typography.labelSmall)
                                    }

                                    AnimatedVisibility(visible = showData) {
                                        Row(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        horizontal = 12.dp,
                                                        vertical = 8.dp
                                                    ),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            DataMode.entries.forEach { mode ->
                                                Surface(
                                                    modifier =
                                                        Modifier
                                                            .height(32.dp)
                                                            .clip(
                                                                RoundedCornerShape(
                                                                    8.dp
                                                                )
                                                            ),
                                                    color =
                                                        if (dataMode == mode)
                                                            MaterialTheme.colorScheme
                                                                .primaryContainer
                                                                .copy(alpha = 0.2f)
                                                        else
                                                            MaterialTheme.colorScheme
                                                                .surface,
                                                    tonalElevation = 0.dp
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            dataMode = mode
                                                            needUpdate = true
                                                        },
                                                        modifier =
                                                            Modifier
                                                                .height(32.dp)
                                                                .fillMaxWidth(),
                                                        contentPadding =
                                                            PaddingValues(
                                                                horizontal = 10.dp
                                                            ),
                                                        colors =
                                                            ButtonDefaults.buttonColors(
                                                                containerColor =
                                                                    Color.Transparent,
                                                                contentColor =
                                                                    if (dataMode ==
                                                                        mode
                                                                    )
                                                                        MaterialTheme
                                                                            .colorScheme
                                                                            .onPrimaryContainer
                                                                    else
                                                                        MaterialTheme
                                                                            .colorScheme
                                                                            .onSurface
                                                            ),
                                                        elevation =
                                                            ButtonDefaults.buttonElevation(
                                                                defaultElevation = 0.dp,
                                                                pressedElevation = 0.dp
                                                            )
                                                    ) {
                                                        Text(
                                                            mode.name.lowercase()
                                                                .replaceFirstChar {
                                                                    it.uppercaseChar()
                                                                },
                                                            style =
                                                                MaterialTheme.typography
                                                                    .labelSmall
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Surface(
                                modifier =
                                    Modifier
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                color =
                                    if (cutIndex1 != null || cutIndex2 != null)
                                        MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation =
                                    if (cutIndex1 != null || cutIndex2 != null) 2.dp
                                    else 0.dp
                            ) {
                                Button(
                                    onClick = undoCutSelection,
                                    modifier = Modifier
                                        .height(40.dp)
                                        .fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = Color.Transparent,
                                            contentColor =
                                                if (cutIndex1 != null ||
                                                    cutIndex2 != null
                                                )
                                                    MaterialTheme.colorScheme
                                                        .onError
                                                else
                                                    MaterialTheme.colorScheme
                                                        .onSurface
                                        ),
                                    elevation =
                                        ButtonDefaults.buttonElevation(
                                            defaultElevation = 0.dp,
                                            pressedElevation = 0.dp
                                        ),
                                    enabled = cutIndex1 != null || cutIndex2 != null
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        "Undo",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(end = 6.dp)
                                    )
                                    Text("Undo", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (updateRemaining > 0) updateRemaining -= 1
}

// Helper function to calculate acceleration color (green to red)
fun calculateAccelerationColor(acceleration: Int): Color {
    Log.d("AccelerationColor", "acceleration: $acceleration")

    // Normalize acceleration to -1 to 1 range
    val normalized = (acceleration.coerceIn(-10000, 10000) / 10000f)

    val hue =
        when {
            normalized < 0f -> {
                // Red (0°) to Orange (30°) to Yellow (60°)
                30f * (1f + normalized)
            }

            normalized > 0f -> {
                // Green (120°) to Blue (240°)
                120f + (120f * normalized)
            }

            else -> {
                // Yellow for center (will be desaturated to gray)
                60f
            }
        }

    // Saturation curve: fully saturated at extremes, low in center
    // Using quadratic easing: saturation = abs(normalized)^0.5 for smooth transition
    val saturation =
        when {
            kotlin.math.abs(normalized) > 0.5f -> 1f // Fully saturated at extremes
            else ->
                kotlin.math
                    .sqrt(kotlin.math.abs(normalized) * 2f)
                    .coerceIn(0f, 1f) // Smooth transition to center
        }

    // Brightness curve: darker at extremes, brighter in center
    val brightness = 1f - (kotlin.math.abs(normalized) * 0.3f) // Ranges from 1.0 to 0.7

    return Color.hsv(hue, saturation, brightness)
}

// Helper function to calculate time-based color (blue to gray gradient)
fun calculateTimeColor(
    start: ZonedDateTime,
    end: ZonedDateTime,
    firstTime: ZonedDateTime,
    lastTime: ZonedDateTime
): Color {
    val totalDuration = Duration.between(firstTime, lastTime).seconds.toFloat()
    val segmentTime = Duration.between(firstTime, start).seconds.toFloat()
    val progress = (segmentTime / totalDuration).coerceIn(0f, 1f)

    // Gradient from blue to gray
    val blueComponent = 1f - (progress * 0.3f)
    val grayComponent = 0.5f + (progress * 0.3f)

    return Color(
        red = grayComponent * 0.4f,
        green = grayComponent * 0.6f,
        blue = blueComponent,
        alpha = 1f
    )
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.painter.Painter,
    label: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp), tint = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Preview(showBackground = true)
@Composable
fun EventLocationPreview() {
    NanHistoryTheme { EventLocationView("") }
}

fun calculateBearingArcPoints(
    center: GeoPoint,
    bearing: Float,
    bearingAccuracy: Float,
    radius: Double
): List<GeoPoint> {
    val points = mutableListOf<GeoPoint>()
    points.add(center)
    val startAngle = bearing - bearingAccuracy
    val endAngle = bearing + bearingAccuracy
    val step = 5 // degrees
    var angle = startAngle
    while (angle <= endAngle) {
        points.add(center.destinationPoint(radius, angle.toDouble()))
        angle += step
    }
    points.add(center.destinationPoint(radius, endAngle.toDouble()))
    points.add(center)
    return points
}
