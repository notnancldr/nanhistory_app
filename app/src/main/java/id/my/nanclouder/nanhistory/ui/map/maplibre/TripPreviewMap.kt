package id.my.nanclouder.nanhistory.ui.map.maplibre

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.utils.history.LocationData
import id.my.nanclouder.nanhistory.utils.simplifyPoints
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.layers.Anchor
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.StyleLoadState
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position

private const val PREVIEW_CASING_LAYER = "trip-preview-casing-layer"
private const val PREVIEW_CORE_LAYER = "trip-preview-core-layer"
private const val PREVIEW_START_PIN_LAYER = "trip-preview-start-pin-layer"
private const val PREVIEW_END_PIN_LAYER = "trip-preview-end-pin-layer"

/**
 * Reusable, lightweight, non-interactive MapLibre Compose preview for trip routes.
 *
 * Used across modern Event Detail and Day List Header screens.
 * Camera gestures are disabled, and the route is auto-framed with 20% margin.
 */
@Composable
fun TripPreviewMap(
    locations: List<LocationData>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    if (locations.isEmpty()) {
        Box(modifier = modifier)
        return
    }

    val isDark = isSystemInDarkTheme()
    var isFallbackActive by remember { mutableStateOf(false) }

    val baseStyle = remember(isDark, isFallbackActive) {
        BaseMapStyle.getBaseStyle(isDark = isDark, isFallback = isFallbackActive)
    }

    val sortedLocations = remember(locations) { locations.sortedBy { it.time } }
    val simplifiedCoords = remember(sortedLocations) {
        simplifyPoints(sortedLocations.map { it.location }, epsilon = 0.00003)
    }

    val routeGeoJson = remember(simplifiedCoords) {
        EventMapGeoJsonBuilder.buildPolylineGeoJson(simplifiedCoords)
    }

    val firstCoord = sortedLocations.firstOrNull()?.location
    val lastCoord = sortedLocations.lastOrNull()?.location

    val startPinGeoJson = remember(firstCoord) {
        EventMapGeoJsonBuilder.buildStartEndPointsGeoJson(firstCoord, null)
    }

    val endPinGeoJson = remember(firstCoord, lastCoord) {
        if (firstCoord != null && lastCoord != null && firstCoord != lastCoord) {
            EventMapGeoJsonBuilder.buildStartEndPointsGeoJson(lastCoord, null)
        } else ""
    }

    val targetBounds = remember(simplifiedCoords) {
        if (simplifiedCoords.isEmpty()) return@remember null
        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLon = Double.MAX_VALUE
        var maxLon = -Double.MAX_VALUE

        for (c in simplifiedCoords) {
            if (c.latitude < minLat) minLat = c.latitude
            if (c.latitude > maxLat) maxLat = c.latitude
            if (c.longitude < minLon) minLon = c.longitude
            if (c.longitude > maxLon) maxLon = c.longitude
        }

        val latSpan = (maxLat - minLat).coerceAtLeast(0.002)
        val lonSpan = (maxLon - minLon).coerceAtLeast(0.002)
        val padFactor = 0.2

        val south = (minLat - latSpan * padFactor).coerceIn(-85.0, 85.0)
        val north = (maxLat + latSpan * padFactor).coerceIn(-85.0, 85.0)
        val west = (minLon - lonSpan * padFactor).coerceIn(-180.0, 180.0)
        val east = (maxLon + lonSpan * padFactor).coerceIn(-180.0, 180.0)

        BoundingBox(
            southwest = Position(west, south),
            northeast = Position(east, north)
        )
    }

    val mapState = rememberMapState(baseStyle = baseStyle) {
        val routeSource = rememberGeoJsonSource(
            data = remember(routeGeoJson) { GeoJsonData.JsonString(routeGeoJson) }
        )

        val startPinSource = rememberGeoJsonSource(
            data = remember(startPinGeoJson) { GeoJsonData.JsonString(startPinGeoJson) }
        )

        val endPinSource = rememberGeoJsonSource(
            data = remember(endPinGeoJson) { GeoJsonData.JsonString(endPinGeoJson) }
        )

        Anchor.Top {
            // Track Casing
            LineLayer(
                id = PREVIEW_CASING_LAYER,
                source = routeSource,
                color = const(if (isDark) Color(0xFF1A1A1A) else Color(0xFF001030)),
                width = const(6.dp),
                cap = const(LineCap.Round),
                join = const(LineJoin.Round)
            )

            // Track Core Stroke
            LineLayer(
                id = PREVIEW_CORE_LAYER,
                source = routeSource,
                color = const(MaterialTheme.colorScheme.primary),
                width = const(3.5.dp),
                cap = const(LineCap.Round),
                join = const(LineJoin.Round)
            )

            // Start Pin Marker (Green)
            if (firstCoord != null) {
                CircleLayer(
                    id = PREVIEW_START_PIN_LAYER,
                    source = startPinSource,
                    radius = const(6.dp),
                    color = const(Color(0xFF4CAF50)),
                    strokeColor = const(Color.White),
                    strokeWidth = const(2.dp)
                )
            }

            // End Pin Marker (Accent / Red)
            if (lastCoord != null && lastCoord != firstCoord) {
                CircleLayer(
                    id = PREVIEW_END_PIN_LAYER,
                    source = endPinSource,
                    radius = const(6.dp),
                    color = const(Color(0xFFF44336)),
                    strokeColor = const(Color.White),
                    strokeWidth = const(2.dp)
                )
            }
        }
    }

    LaunchedEffect(mapState) {
        snapshotFlow { mapState.style.loadState }.collect { state ->
            if (state is StyleLoadState.Failed && !isFallbackActive) {
                isFallbackActive = true
            }
        }
    }

    LaunchedEffect(targetBounds) {
        val bounds = targetBounds ?: return@LaunchedEffect
        try {
            mapState.fitCameraToBounds(bounds)
        } catch (_: Throwable) {
            // Guard against camera animation interruption
        }
    }

    Box(modifier = modifier) {
        MaplibreMap(
            state = mapState,
            interactions = MapInteractions {
                camera {
                    pan { enabled = false }
                    zoom { enabled = false }
                    rotate { enabled = false }
                    pitch { enabled = false }
                }
            },
            overlay = {},
            modifier = Modifier.fillMaxSize()
        )

        if (onClick != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onClick() }
            )
        }
    }
}
