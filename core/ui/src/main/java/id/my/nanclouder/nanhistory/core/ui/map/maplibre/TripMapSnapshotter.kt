package id.my.nanclouder.nanhistory.ui.map.maplibre

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.domain.model.Coordinate
import id.my.nanclouder.nanhistory.domain.model.LocationData
import id.my.nanclouder.nanhistory.core.common.utils.simplifyPoints
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.Anchor
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.DefaultMapRuntime
import org.maplibre.compose.map.MapRuntime
import org.maplibre.compose.map.MapSnapshotter
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

private const val TAG = "TripMapSnapshotter"
private const val SNAPSHOT_CASING_LAYER = "snapshot-casing-layer"
private const val SNAPSHOT_CORE_LAYER = "snapshot-core-layer"
private const val SNAPSHOT_START_PIN_LAYER = "snapshot-start-pin-layer"
private const val SNAPSHOT_END_PIN_LAYER = "snapshot-end-pin-layer"

/**
 * Helper to produce standalone high-resolution map snapshot bitmaps for exports.
 */
object TripMapSnapshotter {

    data class BoundingRegion(
        val centerLon: Double,
        val centerLat: Double,
        val zoom: Double
    )

    fun calculateCenterAndZoom(
        coords: List<Coordinate>,
        widthPx: Int,
        heightPx: Int
    ): BoundingRegion {
        if (coords.isEmpty()) {
            return BoundingRegion(0.0, 0.0, 1.0)
        }
        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLon = Double.MAX_VALUE
        var maxLon = -Double.MAX_VALUE

        for (c in coords) {
            if (c.latitude < minLat) minLat = c.latitude
            if (c.latitude > maxLat) maxLat = c.latitude
            if (c.longitude < minLon) minLon = c.longitude
            if (c.longitude > maxLon) maxLon = c.longitude
        }

        val centerLat = (minLat + maxLat) / 2.0
        val centerLon = (minLon + maxLon) / 2.0

        val latDiff = (maxLat - minLat).coerceAtLeast(0.002)
        val lonDiff = (maxLon - minLon).coerceAtLeast(0.002)

        // Web Mercator tile size at zoom 0 is 512 logical units
        val tileSize = 512.0
        val availWidth = (widthPx * 0.70).coerceAtLeast(100.0)
        val availHeight = (heightPx * 0.70).coerceAtLeast(100.0)

        val zoomLon = kotlin.math.ln((availWidth * 360.0) / (tileSize * lonDiff)) / kotlin.math.ln(2.0)

        val latRadMin = Math.toRadians(minLat.coerceIn(-85.0, 85.0))
        val latRadMax = Math.toRadians(maxLat.coerceIn(-85.0, 85.0))
        val yMin = kotlin.math.ln(kotlin.math.tan(Math.PI / 4.0 + latRadMin / 2.0))
        val yMax = kotlin.math.ln(kotlin.math.tan(Math.PI / 4.0 + latRadMax / 2.0))
        val yDiff = (yMax - yMin).coerceAtLeast(0.0001)

        val zoomLat = kotlin.math.ln((availHeight * 2.0 * Math.PI) / (tileSize * yDiff)) / kotlin.math.ln(2.0)

        val zoom = minOf(zoomLon, zoomLat).coerceIn(2.0, 18.0)

        return BoundingRegion(centerLon, centerLat, zoom)
    }

    fun resolveRegion(
        coords: List<Coordinate>,
        widthPx: Int,
        heightPx: Int,
        customCenterLon: Double? = null,
        customCenterLat: Double? = null,
        customZoom: Double? = null,
        previewWidthPx: Int? = null
    ): BoundingRegion {
        return if (customCenterLon != null && customCenterLat != null && customZoom != null) {
            BoundingRegion(customCenterLon, customCenterLat, customZoom)
        } else {
            calculateCenterAndZoom(coords, widthPx, heightPx)
        }
    }

    /**
     * Renders a standalone high-resolution vector map snapshot bitmap with the trip route.
     * Tries the primary vector base style first, falling back to local asset styles if offline.
     */
    suspend fun captureMapSnapshot(
        locations: List<LocationData>,
        widthPx: Int,
        heightPx: Int,
        isDark: Boolean,
        runtime: MapRuntime? = null,
        coreColor: Color = Color(0xFF38608F),
        customCenterLon: Double? = null,
        customCenterLat: Double? = null,
        customZoom: Double? = null,
        previewWidthDp: Float? = null,
        previewHeightDp: Float? = null
    ): Bitmap? {
        if (locations.isEmpty() || widthPx <= 0 || heightPx <= 0) return null

        val mapRuntime = runtime ?: try {
            DefaultMapRuntime.instance
        } catch (e: Throwable) {
            Log.e(TAG, "Failed obtaining MapRuntime", e)
            return null
        }

        val sortedLocations = locations.sortedBy { it.time }
        val simplifiedCoords = simplifyPoints(sortedLocations.map { it.location }, epsilon = 0.00003)
        if (simplifiedCoords.isEmpty()) return null

        val routeGeoJson = EventMapGeoJsonBuilder.buildPolylineGeoJson(simplifiedCoords)
        val firstCoord = sortedLocations.firstOrNull()?.location
        val lastCoord = sortedLocations.lastOrNull()?.location

        val startPinGeoJson = EventMapGeoJsonBuilder.buildStartEndPointsGeoJson(firstCoord, null)
        val endPinGeoJson = if (firstCoord != null && lastCoord != null && firstCoord != lastCoord) {
            EventMapGeoJsonBuilder.buildStartEndPointsGeoJson(lastCoord, null)
        } else ""

        return tryCapture(
            runtime = mapRuntime,
            baseStyle = BaseMapStyle.getBaseStyle(isDark = isDark, isFallback = false),
            routeGeoJson = routeGeoJson,
            startPinGeoJson = startPinGeoJson,
            endPinGeoJson = endPinGeoJson,
            firstCoord = firstCoord,
            lastCoord = lastCoord,
            simplifiedCoords = simplifiedCoords,
            customCenterLon = customCenterLon,
            customCenterLat = customCenterLat,
            customZoom = customZoom,
            previewWidthDp = previewWidthDp,
            previewHeightDp = previewHeightDp,
            widthPx = widthPx,
            heightPx = heightPx,
            isDark = isDark,
            coreColor = coreColor
        ) ?: tryCapture(
            runtime = mapRuntime,
            baseStyle = BaseMapStyle.getBaseStyle(isDark = isDark, isFallback = true),
            routeGeoJson = routeGeoJson,
            startPinGeoJson = startPinGeoJson,
            endPinGeoJson = endPinGeoJson,
            firstCoord = firstCoord,
            lastCoord = lastCoord,
            simplifiedCoords = simplifiedCoords,
            customCenterLon = customCenterLon,
            customCenterLat = customCenterLat,
            customZoom = customZoom,
            previewWidthDp = previewWidthDp,
            previewHeightDp = previewHeightDp,
            widthPx = widthPx,
            heightPx = heightPx,
            isDark = isDark,
            coreColor = coreColor
        )
    }

    private suspend fun tryCapture(
        runtime: MapRuntime,
        baseStyle: BaseStyle,
        routeGeoJson: String,
        startPinGeoJson: String,
        endPinGeoJson: String,
        firstCoord: Coordinate?,
        lastCoord: Coordinate?,
        simplifiedCoords: List<Coordinate>,
        customCenterLon: Double?,
        customCenterLat: Double?,
        customZoom: Double?,
        previewWidthDp: Float?,
        previewHeightDp: Float?,
        widthPx: Int,
        heightPx: Int,
        isDark: Boolean,
        coreColor: Color
    ): Bitmap? {
        val logicalWidthDp = (previewWidthDp ?: 360f).coerceAtLeast(100f)
        val logicalHeightDp = (previewHeightDp ?: (logicalWidthDp * (heightPx.toFloat() / widthPx.toFloat())))
            .coerceAtLeast(100f)
        val targetDensity = (widthPx.toFloat() / logicalWidthDp).coerceAtLeast(1f)

        var snapshotter: MapSnapshotter? = null
        return try {
            snapshotter = runtime.createSnapshotter(baseStyle = baseStyle) {
                val routeSource = rememberGeoJsonSource(
                    data = GeoJsonData.JsonString(routeGeoJson)
                )
                val startPinSource = rememberGeoJsonSource(
                    data = GeoJsonData.JsonString(startPinGeoJson)
                )
                val endPinSource = rememberGeoJsonSource(
                    data = GeoJsonData.JsonString(endPinGeoJson)
                )

                Anchor.Top {
                    LineLayer(
                        id = SNAPSHOT_CASING_LAYER,
                        source = routeSource,
                        color = const(if (isDark) Color(0xFF1A1A1A) else Color(0xFF001030)),
                        width = const(6.dp),
                        cap = const(LineCap.Round),
                        join = const(LineJoin.Round)
                    )
                    LineLayer(
                        id = SNAPSHOT_CORE_LAYER,
                        source = routeSource,
                        color = const(coreColor),
                        width = const(3.5.dp),
                        cap = const(LineCap.Round),
                        join = const(LineJoin.Round)
                    )
                    if (firstCoord != null) {
                        CircleLayer(
                            id = SNAPSHOT_START_PIN_LAYER,
                            source = startPinSource,
                            radius = const(6.dp),
                            color = const(Color(0xFF4CAF50)),
                            strokeColor = const(Color.White),
                            strokeWidth = const(2.dp)
                        )
                    }
                    if (lastCoord != null && lastCoord != firstCoord) {
                        CircleLayer(
                            id = SNAPSHOT_END_PIN_LAYER,
                            source = endPinSource,
                            radius = const(6.dp),
                            color = const(Color(0xFFF44336)),
                            strokeColor = const(Color.White),
                            strokeWidth = const(2.dp)
                        )
                    }
                }
            }

            val camera = if (customCenterLon != null && customCenterLat != null && customZoom != null) {
                CameraPosition(
                    center = Position(longitude = customCenterLon, latitude = customCenterLat),
                    zoom = customZoom
                )
            } else {
                val region = calculateCenterAndZoom(
                    coords = simplifiedCoords,
                    widthPx = logicalWidthDp.toInt(),
                    heightPx = logicalHeightDp.toInt()
                )
                CameraPosition(
                    center = Position(longitude = region.centerLon, latitude = region.centerLat),
                    zoom = region.zoom
                )
            }

            val imageBitmap = snapshotter.capture(
                size = DpSize(logicalWidthDp.dp, logicalHeightDp.dp)
            ) {
                cameraPosition = camera
                density = Density(targetDensity)
            }

            imageBitmap.asAndroidBitmap()
        } catch (e: Throwable) {
            Log.e(TAG, "Failed capturing snapshot with baseStyle $baseStyle", e)
            null
        } finally {
            try {
                snapshotter?.close()
            } catch (_: Throwable) {
                // Ignore close exceptions
            }
        }
    }
}
