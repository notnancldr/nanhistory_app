package id.my.nanclouder.nanhistory.activity.eventLocation.oldUi

import android.content.Intent
import android.graphics.Paint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.activity.eventDetail.ui.toBoundingBox
import id.my.nanclouder.nanhistory.activity.eventLocation.common.calculateColor
import id.my.nanclouder.nanhistory.utils.Coordinate
import id.my.nanclouder.nanhistory.utils.TimeFormatterWithSecond
import id.my.nanclouder.nanhistory.utils.getLocationData
import id.my.nanclouder.nanhistory.utils.toGeoPoint
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.time.ZonedDateTime
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

@Composable
fun MapHistoryView_Old(
    locations: Map<ZonedDateTime, Coordinate>,
    onPointsSelected: (ZonedDateTime?, ZonedDateTime?) -> Unit,
    modifier: Modifier = Modifier,
    cutMode: Boolean = false,
) {
    val context = LocalContext.current
    val mapKeys = locations.keys.sorted()
    val geoPoints = mapKeys.sorted().map { GeoPoint(locations[it]!!.latitude, locations[it]!!.longitude) }
    val bottomBarScrollState = rememberScrollState()

    var cutTime1 by rememberSaveable { mutableStateOf<ZonedDateTime?>(null) }
    var cutTime2 by rememberSaveable { mutableStateOf<ZonedDateTime?>(null) }
    var cutModeCheck by remember { mutableStateOf(cutMode) }

    var showPoints by rememberSaveable { mutableStateOf(false) }
    var showData by rememberSaveable { mutableStateOf(false) }

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
                    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_location_start)
                    position = geoPoints.first()
                    icon = drawable
                    setOnMarkerClickListener { _, _ ->
                        setCutPoint(locations.keys.first())
                        false
                    }
                }
            val markerEnd =
                Marker(mapViewObj).apply {
                    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_location_end)
                    position = geoPoints.last()
                    icon = drawable
                    setOnMarkerClickListener { _, _ ->
                        setCutPoint(locations.keys.last())
                        false
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
                            outlinePaint.color = calculateColor(it.speed.roundToInt()).toArgb()
                            outlinePaint.strokeWidth = 3f * screenDpi
                            outlinePaint.strokeCap = Paint.Cap.ROUND

                            setOnClickListener { _, _, _ ->
                                selectedTime =
                                    "${TimeFormatterWithSecond.format(it.start)} - ${
                                        TimeFormatterWithSecond.format(it.end)
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
                }
            }

            val currentCutTime1 = cutTime1
            val currentCutTime2 = cutTime2
            if (cutMode && currentCutTime1 != null && currentCutTime2 != null) {
                val startTime = if (currentCutTime1 < currentCutTime2) currentCutTime1 else currentCutTime2
                val endTime = if (currentCutTime1 > currentCutTime2) currentCutTime1 else currentCutTime2
                val selectedArea = mutableListOf<ZonedDateTime>()
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
                if (cutTime1 == null || cutTime2 == null) {
                    for ((shownKey, shownCoordinate) in shownCoordinates.mapIndexed { idx, it ->
                        shownKeys[idx] to it
                    }) {
                        mapViewObj?.overlays?.add(
                            Marker(mapViewObj).apply {
                                val drawable = ContextCompat.getDrawable(context, R.drawable.ic_map_touch_point)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                position = shownCoordinate
                                icon = drawable
                                setOnMarkerClickListener { _, _ ->
                                    setCutPoint(shownKey)
                                    false
                                }
                            }
                        )
                    }
                }
                if (cutTime1 != null) {
                    mapViewObj?.overlays?.add(
                        Marker(mapViewObj).apply {
                            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_map_stop_point)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            position = locations[cutTime1]!!.toGeoPoint()
                            icon = drawable
                        }
                    )
                }
                if (cutTime2 != null) {
                    mapViewObj?.overlays?.add(
                        Marker(mapViewObj).apply {
                            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_map_stop_point)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            position = locations[cutTime2]!!.toGeoPoint()
                            icon = drawable
                        }
                    )
                }
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
        modifier = modifier.fillMaxWidth().fillMaxHeight(),
        factory = { ctx ->
            MapView(ctx).apply {
                setMultiTouchControls(true)
                addMapListener(
                    object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            center = GeoPoint(mapCenter.latitude, mapCenter.longitude)
                            return true
                        }
                        override fun onZoom(event: ZoomEvent?): Boolean {
                            zoomLevel = event?.zoomLevel ?: zoomLevel
                            return true
                        }
                    }
                )
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                if (firstLoad) {
                    setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
                    post { zoomToBoundingBox(shownCoordinates.toBoundingBox(), false) }
                    firstLoad = false
                } else {
                    post {
                        setZoomLevel(zoomLevel)
                        controller.setCenter(center)
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
    ).also { DisposableEffect(Unit) { onDispose {} } }

    if (isSelected) {
        Box(modifier = Modifier.fillMaxWidth().requiredHeight(288.dp)) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
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
                                        Toast.makeText(context, "Selected coordinate unknown", Toast.LENGTH_SHORT).show()
                                        return@handler
                                    }
                                    val gmmIntentUri = "geo:$stringLocation?q=$stringLocation".toUri()
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                        setPackage("com.google.android.apps.maps")
                                    }
                                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(mapIntent)
                                    } else {
                                        Toast.makeText(context, "Google Maps is not installed", Toast.LENGTH_SHORT).show()
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
    }

    Surface(modifier = Modifier.requiredHeight(132.dp)) {
        Row(
            modifier = Modifier.fillMaxSize().padding(bottom = 64.dp, start = 8.dp, end = 8.dp),
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
            if (!cutMode) {
                Row(
                    modifier = Modifier.horizontalScroll(bottomBarScrollState).padding(start = 8.dp).weight(1f)
                ) {
                    Button(
                        onClick = { showPoints = !showPoints; needUpdate = true },
                        colors = if (showPoints) ButtonDefaults.buttonColors() else ButtonDefaults.textButtonColors(),
                        contentPadding = PaddingValues(8.dp)
                    ) { Text("Show Point") }
                    Box(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showData = !showData; needUpdate = true },
                        colors = if (showData) ButtonDefaults.buttonColors() else ButtonDefaults.textButtonColors(),
                        contentPadding = PaddingValues(8.dp)
                    ) { Text("Show Data") }
                }
            } else {
                Row(
                    modifier = Modifier.horizontalScroll(bottomBarScrollState).padding(start = 8.dp).weight(1f)
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
    }
    if (updateRemaining > 0) updateRemaining -= 1
}
