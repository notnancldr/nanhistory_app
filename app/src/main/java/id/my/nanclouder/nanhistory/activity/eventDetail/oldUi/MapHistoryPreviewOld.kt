package id.my.nanclouder.nanhistory.activity.eventDetail.oldUi

import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.utils.history.LocationData
import id.my.nanclouder.nanhistory.utils.simplifyPoints
import id.my.nanclouder.nanhistory.utils.toGeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * Legacy OSMDroid route preview composable preserved specifically for [OldDetailContent].
 */
@Composable
fun MapHistoryPreviewOld(locations: List<LocationData>, modifier: Modifier = Modifier) {
    val geoPoints = locations.sortedBy { it.time }.map {
        GeoPoint(it.location.latitude, it.location.longitude)
    }

    val context = LocalContext.current
    if (geoPoints.isEmpty()) return

    var renderedPoints by remember { mutableIntStateOf(0) }
    var shownPoints by remember { mutableStateOf<List<GeoPoint>>(listOf()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            shownPoints = simplifyPoints(locations.map { it.location }).map {
                it.toGeoPoint()
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            Log.d("NanHistoryDebug", "RECALCULATE?")
            MapView(ctx).apply {
                setMultiTouchControls(false)
                isClickable = false
                isEnabled = false
                setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                post {
                    maxZoomLevel = 18.0
                    zoomToBoundingBox(geoPoints.toBoundingBox(), false)
                    setScrollableAreaLimitLatitude(mapCenter.latitude, mapCenter.latitude, 0)
                    setScrollableAreaLimitLongitude(mapCenter.longitude, mapCenter.longitude, 0)
                    minZoomLevel = zoomLevelDouble
                    maxZoomLevel = zoomLevelDouble
                }
            }
        },
        update = { mapView ->
            Log.d("NanHistoryDebug", "RECALCULATE")
            renderedPoints = shownPoints.size
            Log.d("NanHistoryDebug", "RENDERED: $renderedPoints")

            val polyline = Polyline(mapView).apply {
                setPoints(shownPoints)
                outlinePaint.color = Color.BLUE
                outlinePaint.strokeWidth = 8f
                outlinePaint.strokeCap = Paint.Cap.ROUND
            }
            val polylineBorder = Polyline(mapView).apply {
                setPoints(shownPoints)
                outlinePaint.color = Color.rgb(0, 0, 20)
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
        val boundingBox = BoundingBox.fromGeoPointsSafe(this)
        val paddingFactor = 0.2
        return BoundingBox(
            boundingBox.latNorth + (boundingBox.latitudeSpan * paddingFactor),
            boundingBox.lonEast + (boundingBox.longitudeSpan * paddingFactor),
            boundingBox.latSouth - (boundingBox.latitudeSpan * paddingFactor),
            boundingBox.lonWest - (boundingBox.longitudeSpan * paddingFactor)
        )
    }
    return null
}
