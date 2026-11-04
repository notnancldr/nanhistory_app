package id.my.nanclouder.nanhistory.ui.map.maplibre

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.activity.eventLocation.common.DataMode
import id.my.nanclouder.nanhistory.state.ReplayUiState
import id.my.nanclouder.nanhistory.ui.map.MapZoomUtils.osmEquivalentZoom
import id.my.nanclouder.nanhistory.utils.HistoryLocationData
import id.my.nanclouder.nanhistory.utils.getLocationData
import id.my.nanclouder.nanhistory.utils.history.LocationData
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.serialization.json.JsonPrimitive
import org.maplibre.compose.expressions.dsl.Feature
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToColor
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.interaction.ClickResult
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
import java.time.ZonedDateTime
import kotlin.math.roundToInt

private const val TRACK_CASING_LAYER = "event-track-casing-layer"
private const val TRACK_CORE_LAYER = "event-track-core-layer"
private const val TRAVELED_CASING_LAYER = "event-traveled-casing-layer"
private const val TRAVELED_CORE_LAYER = "event-traveled-core-layer"
private const val CUT_SLICE_LAYER = "event-cut-slice-layer"
private const val TOUCH_POINTS_LAYER = "event-touch-points-layer"
private const val DWELL_POINTS_LAYER = "event-dwell-points-layer"
private const val PINS_LAYER = "event-pins-layer"
private const val REPLAY_HALO_LAYER = "event-replay-halo-layer"
private const val REPLAY_MARKER_RING_LAYER = "event-replay-marker-ring-layer"
private const val REPLAY_MARKER_DOT_LAYER = "event-replay-marker-dot-layer"

/**
 * Isolated Compose MapLibre component for single-event inspection, cut mode, and location replay.
 */
@Composable
fun EventLocationMap(
    locations: List<LocationData>,
    initialBounds: org.osmdroid.util.BoundingBox?,
    firstLocation: LocationData?,
    lastLocation: LocationData?,
    cutMode: Boolean,
    cutStart: ZonedDateTime?,
    cutEnd: ZonedDateTime?,
    showPoints: Boolean,
    showData: Boolean,
    dataMode: DataMode,
    replayState: ReplayUiState?,
    onSelectCutPoint: (ZonedDateTime) -> Unit,
    onSegmentClick: (HistoryLocationData) -> Unit,
    onViewportChanged: (org.osmdroid.util.BoundingBox, Double) -> Unit,
    onFollowCameraDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var isFallbackActive by remember { mutableStateOf(false) }

    val baseStyle = remember(isDark, isFallbackActive) {
        BaseMapStyle.getBaseStyle(isDark = isDark, isFallback = isFallbackActive)
    }

    val segments = remember(locations, showData, dataMode) {
        if (showData && !cutMode && locations.size >= 2) {
            locations.getLocationData()
        } else emptyList()
    }

    val trackGeoJson = remember(locations, segments, showData, cutMode, dataMode) {
        EventMapGeoJsonBuilder.buildTrackGeoJson(locations, segments, showData, cutMode, dataMode)
    }

    val traveledGeoJson = remember(
        replayState?.traveledLocations,
        replayState?.currentTelemetry,
        showData,
        dataMode
    ) {
        val traveled = replayState?.traveledLocations ?: emptyList()
        val currentCoord = replayState?.currentTelemetry?.coordinate
        EventMapGeoJsonBuilder.buildTraveledGeoJson(
            traveled = traveled,
            currentCoord = currentCoord,
            showData = showData,
            dataMode = dataMode,
            allLocations = locations
        )
    }

    val cutSliceGeoJson = remember(cutMode, cutStart, cutEnd, locations) {
        if (cutMode && cutStart != null && cutEnd != null) {
            val slice = locations.filter { it.time in cutStart..cutEnd }
            EventMapGeoJsonBuilder.buildPolylineGeoJson(slice.map { it.location })
        } else "{ \"type\": \"FeatureCollection\", \"features\": [] }"
    }

    val showTouchPoints = (!cutMode && showPoints) || (cutMode && (cutStart == null || cutEnd == null))
    val pointsGeoJson = remember(locations, showTouchPoints) {
        if (showTouchPoints) {
            EventMapGeoJsonBuilder.buildPointsGeoJson(locations)
        } else "{ \"type\": \"FeatureCollection\", \"features\": [] }"
    }

    val dwellsGeoJson = remember(replayState?.dwellPoints) {
        val dwells = replayState?.dwellPoints ?: emptyList()
        EventMapGeoJsonBuilder.buildDwellsGeoJson(dwells)
    }

    val pinsGeoJson = remember(firstLocation, lastLocation, cutStart, cutEnd, locations) {
        EventMapGeoJsonBuilder.buildPinsGeoJson(firstLocation, lastLocation, cutStart, cutEnd, locations)
    }

    val replayMarkerGeoJson = remember(replayState?.currentTelemetry) {
        val telemetry = replayState?.currentTelemetry
        EventMapGeoJsonBuilder.buildReplayMarkerGeoJson(telemetry)
    }

    val mapState = rememberMapState(baseStyle = baseStyle) {
        val trackSource = rememberGeoJsonSource(data = remember(trackGeoJson) { GeoJsonData.JsonString(trackGeoJson) })
        val traveledSource = rememberGeoJsonSource(
            data = remember(traveledGeoJson) { GeoJsonData.JsonString(traveledGeoJson) }
        )
        val cutSliceSource = rememberGeoJsonSource(
            data = remember(cutSliceGeoJson) { GeoJsonData.JsonString(cutSliceGeoJson) }
        )
        val pointsSource = rememberGeoJsonSource(
            data = remember(pointsGeoJson) { GeoJsonData.JsonString(pointsGeoJson) }
        )
        val dwellsSource = rememberGeoJsonSource(
            data = remember(dwellsGeoJson) { GeoJsonData.JsonString(dwellsGeoJson) }
        )
        val pinsSource = rememberGeoJsonSource(data = remember(pinsGeoJson) { GeoJsonData.JsonString(pinsGeoJson) })
        val replaySource = rememberGeoJsonSource(
            data = remember(replayMarkerGeoJson) { GeoJsonData.JsonString(replayMarkerGeoJson) }
        )

        Anchor.Top {
            val casingColor = if (isDark) Color(0x66000000) else Color(0x80FFFFFF)
            val baseTrackColor = if (cutMode) Color(0xFF757575) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

            // 1. Base Track Casing & Core
            LineLayer(
                id = TRACK_CASING_LAYER,
                source = trackSource,
                cap = const(LineCap.Round),
                join = const(LineJoin.Round),
                color = const(casingColor),
                width = const(6.dp)
            )
            LineLayer(
                id = TRACK_CORE_LAYER,
                source = trackSource,
                cap = const(LineCap.Round),
                join = const(LineJoin.Round),
                color = Feature["color"].convertToColor(const(baseTrackColor)),
                width = const(4.dp),
                hitPadding = 12.dp,
                onClick = { features ->
                    val clickedId = (features.firstOrNull()?.properties?.get("segIndex") as? JsonPrimitive)
                        ?.content?.toIntOrNull()
                    if (clickedId != null && clickedId in segments.indices) {
                        onSegmentClick(segments[clickedId])
                    }
                    ClickResult.Consume
                }
            )

            // 2. Traveled Route (Highlighted Replay Trail)
            if (replayState?.isActive == true) {
                LineLayer(
                    id = TRAVELED_CASING_LAYER,
                    source = traveledSource,
                    cap = const(LineCap.Round),
                    join = const(LineJoin.Round),
                    color = const(Color.White.copy(alpha = 0.8f)),
                    width = const(7.dp)
                )
                LineLayer(
                    id = TRAVELED_CORE_LAYER,
                    source = traveledSource,
                    cap = const(LineCap.Round),
                    join = const(LineJoin.Round),
                    color = Feature["color"].convertToColor(const(MaterialTheme.colorScheme.primary)),
                    width = const(4.5.dp)
                )
            }

            // 3. Cut Mode Selection Slice
            if (cutMode && cutStart != null && cutEnd != null) {
                LineLayer(
                    id = CUT_SLICE_LAYER,
                    source = cutSliceSource,
                    cap = const(LineCap.Round),
                    join = const(LineJoin.Round),
                    color = const(MaterialTheme.colorScheme.primary),
                    width = const(5.dp)
                )
            }

            // 4. Touch Points (for Cut Point Selection)
            CircleLayer(
                id = TOUCH_POINTS_LAYER,
                source = pointsSource,
                radius = const(6.dp),
                color = const(MaterialTheme.colorScheme.primaryContainer),
                strokeColor = const(MaterialTheme.colorScheme.onPrimaryContainer),
                strokeWidth = const(1.5.dp),
                hitPadding = 12.dp,
                onClick = { features ->
                    val timeStr = (features.firstOrNull()?.properties?.get("time") as? JsonPrimitive)?.content
                    val time = timeStr?.let { ZonedDateTime.parse(it) }
                    if (time != null && cutMode) {
                        onSelectCutPoint(time)
                    }
                    ClickResult.Consume
                }
            )

            // 5. Stationary Dwell Points
            CircleLayer(
                id = DWELL_POINTS_LAYER,
                source = dwellsSource,
                radius = const(8.dp),
                color = const(Color(0xFFFF9800)),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp)
            )

            // 6. Pins (Start, End, Cut markers)
            CircleLayer(
                id = PINS_LAYER,
                source = pinsSource,
                radius = const(9.dp),
                color = const(MaterialTheme.colorScheme.surface),
                strokeColor = const(MaterialTheme.colorScheme.primary),
                strokeWidth = const(3.dp),
                hitPadding = 12.dp,
                onClick = { features ->
                    val timeStr = (features.firstOrNull()?.properties?.get("time") as? JsonPrimitive)?.content
                    val time = timeStr?.let { ZonedDateTime.parse(it) }
                    if (time != null && cutMode && (cutStart == null || cutEnd == null)) {
                        onSelectCutPoint(time)
                    }
                    ClickResult.Consume
                }
            )

            // 7. Animated Replay Marker with Accuracy Halo
            if (replayState?.isActive == true && replayState.currentTelemetry != null) {
                val acc = (replayState.currentTelemetry.accuracyMeters ?: 15f).coerceIn(12f, 40f).dp
                CircleLayer(
                    id = REPLAY_HALO_LAYER,
                    source = replaySource,
                    radius = const(acc),
                    color = const(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    strokeColor = const(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    strokeWidth = const(1.dp)
                )
                CircleLayer(
                    id = REPLAY_MARKER_RING_LAYER,
                    source = replaySource,
                    radius = const(9.dp),
                    color = const(Color.White),
                    strokeColor = const(MaterialTheme.colorScheme.primary),
                    strokeWidth = const(3.dp)
                )
                CircleLayer(
                    id = REPLAY_MARKER_DOT_LAYER,
                    source = replaySource,
                    radius = const(4.dp),
                    color = const(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }

    LaunchedEffect(mapState) {
        snapshotFlow { mapState.style.loadState }.collect { state ->
            if (state is StyleLoadState.Failed && !isFallbackActive) isFallbackActive = true
        }
    }

    LaunchedEffect(mapState) {
        snapshotFlow { mapState.viewport }
            .filterNotNull()
            .collect { vp ->
                val osmBox = EventMapGeoJsonBuilder.toOsmBoundingBox(vp.visibleBounds)
                val osmZoom = osmEquivalentZoom(mapState.cameraPosition.zoom)
                onViewportChanged(osmBox, osmZoom)
            }
    }

    LaunchedEffect(initialBounds) {
        val bounds = initialBounds ?: return@LaunchedEffect
        if (bounds.latNorth - bounds.latSouth < 0.0001 || bounds.lonEast - bounds.lonWest < 0.0001) return@LaunchedEffect
        val targetBbox = BoundingBox(
            southwest = Position(bounds.lonWest, bounds.latSouth),
            northeast = Position(bounds.lonEast, bounds.latNorth)
        )
        try {
            mapState.fitCameraToBounds(targetBbox)
        } catch (_: Throwable) {}
    }

    LaunchedEffect(replayState?.isFollowCamera, replayState?.currentTelemetry) {
        val telemetry = replayState?.currentTelemetry ?: return@LaunchedEffect
        if (replayState.isFollowCamera) {
            val targetPos = Position(telemetry.coordinate.longitude, telemetry.coordinate.latitude)
            mapState.setCameraPosition(
                mapState.cameraPosition.copy(
                    center = targetPos,
                    zoom = maxOf(mapState.cameraPosition.zoom, 14.5)
                )
            )
        }
    }

    Box(modifier = modifier) {
        MaplibreMap(
            state = mapState,
            interactions = MapInteractions {
                camera {
                    rotate { enabled = false }
                    pitch { enabled = false }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isFallbackActive) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.base_map_unavailable),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
