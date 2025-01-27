package id.my.nanclouder.nanhistory

import android.graphics.Paint
import android.location.Location
import android.os.Bundle
import android.util.Log
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowBack
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import id.my.nanclouder.nanhistory.lib.Coordinate
import id.my.nanclouder.nanhistory.lib.TimeFormatterWithSecond
import id.my.nanclouder.nanhistory.lib.getLocationData
import id.my.nanclouder.nanhistory.lib.history.EventPoint
import id.my.nanclouder.nanhistory.lib.history.EventRange
import id.my.nanclouder.nanhistory.lib.history.HistoryEvent
import id.my.nanclouder.nanhistory.lib.history.HistoryFileData
import id.my.nanclouder.nanhistory.lib.history.generateEventId
import id.my.nanclouder.nanhistory.lib.history.get
import id.my.nanclouder.nanhistory.lib.toGeoPoint
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
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
        setContent {
            var setUpdate by remember { mutableStateOf(false) }
            update = { setUpdate = !setUpdate }
            NanHistoryTheme {
//                Log.d("NanHistoryDebug", "data (eventId) : $eventId")
//                Log.d("NanHistoryDebug", "data (path)    : $path")
                key(setUpdate) { EventLocationView(eventId, path) }
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
fun EventLocationView(eventId: String, path: String) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
//    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current

    val eventData: HistoryEvent? = remember {
        val fileData = HistoryFileData.get(context, path)
//        Log.d("NanHistoryDebug", "File data: $fileData")
        fileData?.events?.firstOrNull {
//            Log.d("NanHistoryDebug", "Event check: ${it.id} == $eventId -> ${it.id == eventId}")
            it.id == eventId
        }
    }

    val locationAvailable = when (eventData) {
        is EventRange -> eventData.locations.isNotEmpty()
        is EventPoint -> eventData.location != null
        else -> false
    }

    Log.d("NanHistoryDebug", "eventData: $eventData")

    if (eventData != null) Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("Event Map")
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
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
        ) {
            if (locationAvailable) {
                val locations = when (eventData) {
                    is EventRange -> eventData.locations
                    is EventPoint -> if (eventData.location != null)
                        mapOf(eventData.time to eventData.location!!) else mapOf()

                    else -> mapOf()
                }
                MapHistoryView(locations = locations)
            }
        }
    }
    else Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
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
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Event not found")
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

//fun calculateSpeed(startPoint: Coordinate, endPoint: Coordinate, timeHours: Float): Float =
//    calculateSpeed(
//        startPoint = GeoPoint(startPoint.latitude, startPoint.longitude),
//        endPoint = GeoPoint(endPoint.latitude, endPoint.longitude),
//        timeHours = timeHours
//    )

fun calculateColor(speed: Int): Color {
    val lightness = max(min(speed / 20f, .5f), 0.3f)
    val hue = max(min(speed * 2.5f, 270f), 0f)
    return Color.hsl(hue, 1f, lightness)
}

@Composable
fun MapHistoryView(locations: Map<ZonedDateTime, Coordinate>, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val mapKeys = locations.keys.sorted()

    val geoPoints = mapKeys.sorted().map {
        GeoPoint(locations[it]!!.latitude, locations[it]!!.longitude)
    }

    val bottomBarScrollState = rememberScrollState()

    // User Options
    var showPoints by rememberSaveable { mutableStateOf(false) }
    var showMovementSpeed by rememberSaveable { mutableStateOf(false) }

    var needUpdate by remember { mutableStateOf(false) }
    var firstLoad by remember { mutableStateOf(true) }

    var mapViewObj by remember { mutableStateOf<MapView?>(null) }

    if (geoPoints.isEmpty()) return

    Log.d("NanHistoryDebug", "GeoPoints: $geoPoints, Locations: $locations")

    var zoomLevel by remember { mutableDoubleStateOf(0.0) }
    var prevZoomLevel by remember { mutableDoubleStateOf(zoomLevel) }
    var center by remember { mutableStateOf(GeoPoint(0.0, 0.0)) }

    var isSelected by remember { mutableStateOf(false) }

    var selectedSpeed by remember { mutableIntStateOf(0) }
    var selectedTime by remember { mutableStateOf("") }
    var selectedFirstKey by remember { mutableStateOf(ZonedDateTime.now()) }
    var selectedSecondKey by remember { mutableStateOf(ZonedDateTime.now()) }

    var updateRemaining by remember { mutableIntStateOf(0) }

    isSelected = showMovementSpeed && isSelected

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

    var updateMap = { }

    updateMap = updater@{
        if (mapViewObj == null) return@updater

        mapViewObj?.overlays?.clear()

        val polyline = Polyline(mapViewObj).apply {
            setPoints(shownCoordinates)
            outlinePaint.color = android.graphics.Color.BLUE
            outlinePaint.strokeWidth = 8f
            outlinePaint.strokeCap = Paint.Cap.ROUND
        }
        val polylineBorder = Polyline(mapViewObj).apply {
            setPoints(shownCoordinates)
            outlinePaint.color = android.graphics.Color.rgb(0, 0, 20)
            outlinePaint.strokeWidth = 10f
            outlinePaint.strokeCap = Paint.Cap.ROUND
        }
        val markerStart = Marker(mapViewObj).apply {
            position = shownCoordinates.first()
            icon = context.getDrawable(R.drawable.ic_location_start)
        }
        val markerEnd = Marker(mapViewObj).apply {
            position = shownCoordinates.last()
            icon = context.getDrawable(R.drawable.ic_location_end)
        }

        mapViewObj?.overlays?.add(polylineBorder)
        if (!showMovementSpeed) {
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
                        outlinePaint.strokeWidth = 8f
                        outlinePaint.strokeCap = Paint.Cap.ROUND

                        setOnClickListener { _, _, _ -> // polyline, mapView, eventPos
                            selectedTime = "${TimeFormatterWithSecond.format(it.start)} - ${TimeFormatterWithSecond.format(it.end)}"
                            selectedSpeed = it.speed.roundToInt()
                            selectedFirstKey = it.start
                            selectedSecondKey = it.end
                            isSelected = true
                            updateRemaining = 1000000
                            updateMap()
//                            mapViewObj?.setZoomLevel(zoomLevel)
//                            needUpdate = true

                            true
                        }
                    }

                    mapViewObj?.overlays?.add(
                        coloredPolyline
                    )
                }
//            for (key in shownKeys) {
//                if (prevKey != null) {
//                    val currentPrevKey = prevKey
//                    val points = listOf(
//                        locations[prevKey]!!,
//                        locations[key]!!
//                    ).map { GeoPoint( it.latitude, it.longitude ) }
//
//                    val time = (key.toInstant().toEpochMilli() - prevKey.toInstant().toEpochMilli())
//                    val timeHours = time / 3600000f
//
//                    val speed = calculateSpeed(points.first(), points.last(), timeHours)
//
//                    Log.d("NanHistoryDebug", "Speed: $speed, Time: $time | $timeHours ($prevKey :: $key)")
//
//                    val coloredPolyline = Polyline(mapViewObj).apply {
//                        setPoints(points)
//                        outlinePaint.color = calculateColor(speed.roundToInt()).toArgb()
//                        outlinePaint.strokeWidth = 8f
//                        outlinePaint.strokeCap = Paint.Cap.ROUND
//
//                        setOnClickListener { _, _, _ -> // polyline, mapView, eventPos
//                            selectedTime = "${TimeFormatterWithSecond.format(currentPrevKey)} - ${TimeFormatterWithSecond.format(key)}"
//                            selectedSpeed = speed.roundToInt()
//                            selectedFirstKey = currentPrevKey
//                            selectedSecondKey = key
//                            isSelected = true
//                            updateRemaining = 1000000
//                            updateMap()
////                            mapViewObj?.setZoomLevel(zoomLevel)
////                            needUpdate = true
//
//                            true
//                        }
//                    }
//
//                    mapViewObj?.overlays?.add(
//                        coloredPolyline
//                    )
//                }
//                prevKey = key
//            }
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
                        outlinePaint.strokeWidth = 17f
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        id = firstKey.toString()
                    }
                )
                mapViewObj?.overlays?.add(
                    Polyline(mapViewObj).apply {
                        setPoints(polylinePoints)
                        outlinePaint.color = calculateColor(selectedSpeed).toArgb()
                        outlinePaint.strokeWidth = 9f
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        id = firstKey.toString()
                    }
                )
            }
            else {
                Log.d("NanHistoryDebug", "No selected polyline")
            }
        }

        if (showPoints) {
            for (shownCoordinate in shownCoordinates) {
                mapViewObj?.overlays?.add(Marker(mapViewObj).apply {
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    position = shownCoordinate
                    icon = context.getDrawable(R.drawable.ic_map_point)
                })
            }
        }

        if (shownCoordinates.size > 1) mapViewObj?.overlays?.add(markerStart)
        mapViewObj?.overlays?.add(markerEnd)
    }


    if (!needUpdate) {
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

                    post {
                        updateMap()
                        mapViewObj = this
                    }
                }
            },
            update = { mapView ->
                mapViewObj = mapView
                if ((prevZoomLevel - zoomLevel).absoluteValue >= 1.0 || needUpdate) {
                    updateMap()
                    prevZoomLevel = zoomLevel
                }
                // Update MapView if needed
            }
        ).also { view ->
            DisposableEffect(Unit) {
                onDispose {
                }
            }

        }
    }
    else {
        needUpdate = false
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
                Row {
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
                    color = if (shownCoordinates.size == geoPoints.size) Color(0xFF00A000) else Color.Unspecified
                )
            }
            Box(modifier = Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .horizontalScroll(bottomBarScrollState)
                    .padding(start = 8.dp)
            ) {
                val onShowPointsClicked: () -> Unit = {
                    showPoints = !showPoints
                    needUpdate = true
                }
                val onShowMovementSpeedClicked: () -> Unit = {
                    showMovementSpeed = !showMovementSpeed
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
                    if (showMovementSpeed) ButtonDefaults.buttonColors()
                    else ButtonDefaults.textButtonColors(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Text("Movement speed")
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
        EventLocationView("", "")
    }
}