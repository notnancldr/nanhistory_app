package id.my.nanclouder.nanhistory.activity.eventLocation.ui

import android.graphics.Paint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.activity.eventLocation.common.DataMode
import id.my.nanclouder.nanhistory.activity.eventLocation.common.calculateAccelerationColor
import id.my.nanclouder.nanhistory.activity.eventLocation.common.calculateAccuracyColor
import id.my.nanclouder.nanhistory.activity.eventLocation.common.calculateAltitudeColor
import id.my.nanclouder.nanhistory.activity.eventLocation.common.calculateBearingArcPoints
import id.my.nanclouder.nanhistory.activity.eventLocation.common.calculateColor
import id.my.nanclouder.nanhistory.activity.eventLocation.common.calculateTimeColor
import id.my.nanclouder.nanhistory.utils.Coordinate
import id.my.nanclouder.nanhistory.utils.TimeFormatterWithSecond
import id.my.nanclouder.nanhistory.utils.getLocationData
import id.my.nanclouder.nanhistory.utils.history.LocationData
import id.my.nanclouder.nanhistory.utils.toGeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import java.time.ZonedDateTime
import kotlin.math.roundToInt

@Composable
fun MapHistoryView(
    locations: List<LocationData>,
    initialBounds: BoundingBox?,
    firstLocation: LocationData?,
    lastLocation: LocationData?,
    cutMode: Boolean,
    cutStart: ZonedDateTime?,
    cutEnd: ZonedDateTime?,
    onSelectCutPoint: (ZonedDateTime) -> Unit,
    onUndoCut: () -> Unit,
    onViewportChanged: (BoundingBox, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val screenDpi = LocalDensity.current.run { 1.dp.toPx() }

    var showPoints by rememberSaveable { mutableStateOf(false) }
    var showData by rememberSaveable { mutableStateOf(false) }
    var dataMode by rememberSaveable { mutableStateOf(DataMode.SPEED) }

    var zoomLevel by remember { mutableDoubleStateOf(15.0) }
    var isSelected by remember { mutableStateOf(false) }
    var selectedSpeed by remember { mutableIntStateOf(0) }
    var selectedTime by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf<Coordinate?>(null) }
    var selectedAltitude by remember { mutableStateOf<String?>(null) }
    var selectedAccuracy by remember { mutableStateOf<String?>(null) }

    var firstLoad by remember { mutableStateOf(true) }
    var mapViewObj by remember { mutableStateOf<MapView?>(null) }

    val updateMapOverlays = {
        val map = mapViewObj
        if (map != null) {
            map.overlays.clear()
            val geoPoints = locations.map { it.location.toGeoPoint() }

            if (geoPoints.isNotEmpty()) {
                val polylineBorder = Polyline(map).apply {
                    setPoints(geoPoints)
                    outlinePaint.color = android.graphics.Color.rgb(0, 0, 20)
                    outlinePaint.strokeWidth = 4f * screenDpi
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                }
                map.overlays.add(polylineBorder)

                if (!showData || cutMode) {
                    val polyline = Polyline(map).apply {
                        setPoints(geoPoints)
                        outlinePaint.color = if (!cutMode) android.graphics.Color.BLUE else android.graphics.Color.GRAY
                        outlinePaint.strokeWidth = 3f * screenDpi
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                    }
                    map.overlays.add(polyline)
                } else {
                    locations.getLocationData().forEach { seg ->
                        val coloredPolyline = Polyline(map).apply {
                            setPoints(seg.points.map { it.toGeoPoint() })
                            outlinePaint.color = when (dataMode) {
                                DataMode.SPEED -> calculateColor(seg.speed.roundToInt()).toArgb()
                                DataMode.ACCELERATION -> calculateAccelerationColor(seg.acceleration.roundToInt()).toArgb()
                                DataMode.TIME -> calculateTimeColor(
                                    seg.start, seg.end,
                                    locations.first().time, locations.last().time
                                ).toArgb()
                                DataMode.ACCURACY -> calculateAccuracyColor(seg.locationData.first().accuracy).toArgb()
                                DataMode.BEARING -> Color.hsl(seg.locationData.first().bearing ?: 0f, 1f, 0.5f).toArgb()
                                DataMode.ALTITUDE -> calculateAltitudeColor(seg.locationData.first().altitude).toArgb()
                            }
                            outlinePaint.strokeWidth = 3f * screenDpi
                            outlinePaint.strokeCap = Paint.Cap.ROUND

                            setOnClickListener { _, _, _ ->
                                selectedTime = "${TimeFormatterWithSecond.format(seg.start)} - ${
                                    TimeFormatterWithSecond.format(seg.end)
                                }"
                                selectedSpeed = seg.speed.roundToInt()
                                val firstData = seg.locationData.firstOrNull()
                                val lastData = seg.locationData.lastOrNull()
                                selectedLocation = firstData?.location
                                selectedAltitude = if (firstData?.altitude != null && lastData?.altitude != null) {
                                    "${(firstData.altitude * 10.0).roundToInt() / 10.0} - " +
                                        "${(lastData.altitude * 10.0).roundToInt() / 10.0} m"
                                } else null
                                selectedAccuracy = if (firstData?.accuracy != null && lastData?.accuracy != null) {
                                    "${(firstData.accuracy * 10.0).roundToInt() / 10.0} - " +
                                        "${(lastData.accuracy * 10.0).roundToInt() / 10.0} m"
                                } else null
                                isSelected = true
                                true
                            }
                        }
                        map.overlays.add(coloredPolyline)
                    }
                }
            }

            if (cutMode && cutStart != null && cutEnd != null) {
                val cutSlice = locations.filter { it.time in cutStart..cutEnd }
                if (cutSlice.isNotEmpty()) {
                    val selectedPolyline = Polyline(map).apply {
                        setPoints(cutSlice.map { it.location.toGeoPoint() })
                        outlinePaint.color = android.graphics.Color.BLUE
                        outlinePaint.strokeWidth = 3f * screenDpi
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                    }
                    map.overlays.add(selectedPolyline)
                }
            }

            val showTouchPoints = (!cutMode && showPoints) || (cutMode && (cutStart == null || cutEnd == null))
            if (showTouchPoints) {
                for (item in locations) {
                    map.overlays.add(
                        Marker(map).apply {
                            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_map_touch_point)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            position = item.location.toGeoPoint()
                            icon = drawable
                            setOnMarkerClickListener { _, _ ->
                                if (cutMode) {
                                    onSelectCutPoint(item.time)
                                    true
                                } else false
                            }
                        }
                    )
                }
            }

            if (cutStart != null) {
                val startLoc = locations.find { it.time == cutStart } ?: firstLocation
                if (startLoc != null) {
                    map.overlays.add(
                        Marker(map).apply {
                            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_map_stop_point)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            position = startLoc.location.toGeoPoint()
                            icon = drawable
                        }
                    )
                }
            }

            if (cutEnd != null) {
                val endLoc = locations.find { it.time == cutEnd } ?: lastLocation
                if (endLoc != null) {
                    map.overlays.add(
                        Marker(map).apply {
                            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_map_stop_point)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            position = endLoc.location.toGeoPoint()
                            icon = drawable
                        }
                    )
                }
            }

            if (firstLocation != null) {
                map.overlays.add(
                    Marker(map).apply {
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_location_start)
                        position = firstLocation.location.toGeoPoint()
                        setOnMarkerClickListener { _, _ ->
                            if (cutMode && (cutStart == null || cutEnd == null)) {
                                onSelectCutPoint(firstLocation.time)
                                true
                            } else false
                        }
                    }
                )
            }

            if (lastLocation != null) {
                map.overlays.add(
                    Marker(map).apply {
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_location_end)
                        position = lastLocation.location.toGeoPoint()
                        setOnMarkerClickListener { _, _ ->
                            if (cutMode && (cutStart == null || cutEnd == null)) {
                                onSelectCutPoint(lastLocation.time)
                                true
                            } else false
                        }
                    }
                )
            }

            map.invalidate()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewObj?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewObj?.onPause()
                Lifecycle.Event.ON_DESTROY -> mapViewObj?.onDetach()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewObj?.onDetach()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = ctx.packageName
                MapView(ctx).apply {
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    minZoomLevel = 4.0
                    maxZoomLevel = 20.0
                    setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)

                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            val box = boundingBox
                            if (box != null) {
                                onViewportChanged(box, zoomLevelDouble)
                            }
                            return true
                        }

                        override fun onZoom(event: ZoomEvent?): Boolean {
                            val newZoom = event?.zoomLevel ?: zoomLevelDouble
                            zoomLevel = newZoom
                            val box = boundingBox
                            if (box != null) {
                                onViewportChanged(box, newZoom)
                            }
                            return true
                        }
                    })

                    mapViewObj = this
                    if (initialBounds != null && firstLoad) {
                        post {
                            zoomToBoundingBox(initialBounds, false)
                            firstLoad = false
                        }
                    }
                }
            },
            update = { map ->
                mapViewObj = map
                if (firstLoad && initialBounds != null) {
                    map.post {
                        map.zoomToBoundingBox(initialBounds, false)
                        firstLoad = false
                    }
                }
                updateMapOverlays()
            }
        )

        if (isSelected) {
            SelectedSegmentInfoCard(
                selectedTime = selectedTime,
                selectedSpeed = selectedSpeed,
                selectedAltitude = selectedAltitude,
                selectedAccuracy = selectedAccuracy,
                location = selectedLocation,
                onClose = { isSelected = false },
                modifier = Modifier.align(Alignment.TopCenter),
                context = context
            )
        }

        EventLocationBottomBar(
            zoomLevel = zoomLevel,
            isMaxDetail = zoomLevel >= 15.0,
            cutMode = cutMode,
            showPoints = showPoints,
            showData = showData,
            dataMode = dataMode,
            canUndoCut = cutStart != null || cutEnd != null,
            onTogglePoints = { showPoints = !showPoints; updateMapOverlays() },
            onToggleData = { showData = !showData; updateMapOverlays() },
            onSelectDataMode = { dataMode = it; updateMapOverlays() },
            onUndoCut = onUndoCut,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
