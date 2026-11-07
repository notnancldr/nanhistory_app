package id.my.nanclouder.nanhistory.feature.eventdetail.oldUi

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
import id.my.nanclouder.nanhistory.core.common.utils.simplifyPoints
import id.my.nanclouder.nanhistory.core.ui.R as CoreUiR
import id.my.nanclouder.nanhistory.domain.model.Coordinate
import id.my.nanclouder.nanhistory.domain.model.LocationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

fun Coordinate.toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)

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

    DisposableEffect(Unit) {
        onDispose {
            shownPoints = emptyList()
        }
    }

    val polyline = remember {
        Polyline().apply {
            outlinePaint.color = Color.RED
            outlinePaint.strokeWidth = 10f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
        }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            overlayManager.tilesOverlay.loadingBackgroundColor = Color.TRANSPARENT
            overlayManager.tilesOverlay.loadingLineColor = Color.TRANSPARENT
            isClickable = false
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                overlays.clear()

                val startMarker = Marker(this).apply {
                    icon = context.getDrawable(CoreUiR.drawable.ic_location_start)
                    position = geoPoints.first()
                }
                val endMarker = Marker(this).apply {
                    icon = context.getDrawable(CoreUiR.drawable.ic_location_end)
                    position = geoPoints.last()
                }

                overlays.add(polyline)
                overlays.add(startMarker)
                overlays.add(endMarker)

                post {
                    try {
                        val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                        zoomToBoundingBox(boundingBox, false, 100)
                    } catch (e: Exception) {
                        Log.e("MapHistoryPreviewOld", "Failed to zoom bounding box", e)
                    }
                }
            }
        },
        update = {
            if (shownPoints.isNotEmpty() && renderedPoints != shownPoints.size) {
                polyline.setPoints(shownPoints)
                it.invalidate()
                renderedPoints = shownPoints.size
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}
