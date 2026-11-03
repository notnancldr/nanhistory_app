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
import id.my.nanclouder.nanhistory.state.TimeRangeSelection
import id.my.nanclouder.nanhistory.ui.map.CorridorColorScale
import id.my.nanclouder.nanhistory.ui.map.CorridorRender
import id.my.nanclouder.nanhistory.ui.map.MapZoomUtils.osmEquivalentZoom
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.serialization.json.JsonPrimitive
import org.maplibre.compose.expressions.dsl.Feature
import org.maplibre.compose.expressions.dsl.asNumber
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.interpolate
import org.maplibre.compose.expressions.dsl.linear
import org.maplibre.compose.expressions.dsl.zoom
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.layers.Anchor
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.StyleLoadState
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.VisibleBounds
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position

private const val CORRIDOR_CASING_LAYER_ID = "yearly-corridor-casing-line-layer"
private const val CORRIDOR_LAYER_ID = "yearly-corridor-line-layer"

/**
 * Isolated Compose MapLibre component for aggregate trip corridor visualization.
 * This file, [BaseMapStyle], and [MapStyleConfig] are the ONLY files importing MapLibre Compose.
 */
@Composable
fun CorridorMap(
    render: CorridorRender,
    timeRange: TimeRangeSelection,
    initialBounds: org.osmdroid.util.BoundingBox?,
    onViewportChanged: (org.osmdroid.util.BoundingBox, Double) -> Unit,
    onEdgeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var isFallbackActive by remember { mutableStateOf(false) }

    val baseStyle = remember(isDark, isFallbackActive) {
        BaseMapStyle.getBaseStyle(isDark = isDark, isFallback = isFallbackActive)
    }

    val mapState = rememberMapState(
        baseStyle = baseStyle
    ) {
        val geoJsonData = remember(render.version) {
            GeoJsonData.JsonString(render.geoJson)
        }
        val source = rememberGeoJsonSource(
            data = geoJsonData
        )

        Anchor.Top {
            val casingColor = if (isDark) Color(0x66000000) else Color(0x80FFFFFF)
            LineLayer(
                id = CORRIDOR_CASING_LAYER_ID,
                source = source,
                cap = const(LineCap.Round),
                join = const(LineJoin.Round),
                sortKey = Feature["t"].asNumber(),
                color = const(casingColor),
                width = interpolate(
                    type = linear(),
                    input = zoom(),
                    6 to interpolate(
                        type = linear(),
                        input = Feature["t"].asNumber(),
                        0.0f to const(3.0.dp),
                        1.0f to const(5.0.dp)
                    ),
                    14 to interpolate(
                        type = linear(),
                        input = Feature["t"].asNumber(),
                        0.0f to const(6.0.dp),
                        1.0f to const(14.0.dp)
                    )
                )
            )

            LineLayer(
                id = CORRIDOR_LAYER_ID,
                source = source,
                cap = const(LineCap.Round),
                join = const(LineJoin.Round),
                sortKey = Feature["t"].asNumber(),
                color = interpolate(
                    type = linear(),
                    input = Feature["t"].asNumber(),
                    CorridorColorScale.COLOR_STOPS[0].first to const(CorridorColorScale.COLOR_STOPS[0].second),
                    CorridorColorScale.COLOR_STOPS[1].first to const(CorridorColorScale.COLOR_STOPS[1].second),
                    CorridorColorScale.COLOR_STOPS[2].first to const(CorridorColorScale.COLOR_STOPS[2].second),
                    CorridorColorScale.COLOR_STOPS[3].first to const(CorridorColorScale.COLOR_STOPS[3].second),
                    CorridorColorScale.COLOR_STOPS[4].first to const(CorridorColorScale.COLOR_STOPS[4].second),
                    CorridorColorScale.COLOR_STOPS[5].first to const(CorridorColorScale.COLOR_STOPS[5].second),
                    CorridorColorScale.COLOR_STOPS[6].first to const(CorridorColorScale.COLOR_STOPS[6].second),
                    CorridorColorScale.COLOR_STOPS[7].first to const(CorridorColorScale.COLOR_STOPS[7].second),
                    CorridorColorScale.COLOR_STOPS[8].first to const(CorridorColorScale.COLOR_STOPS[8].second)
                ),
                width = interpolate(
                    type = linear(),
                    input = zoom(),
                    6 to interpolate(
                        type = linear(),
                        input = Feature["t"].asNumber(),
                        0.0f to const(1.0.dp),
                        1.0f to const(3.0.dp)
                    ),
                    14 to interpolate(
                        type = linear(),
                        input = Feature["t"].asNumber(),
                        0.0f to const(4.0.dp),
                        1.0f to const(12.0.dp)
                    )
                ),
                hitPadding = 12.dp,
                onClick = { features ->
                    val highestFeature = features.maxByOrNull { f ->
                        val tElem = f.properties?.get("t")
                        (tElem as? JsonPrimitive)?.content?.toFloatOrNull() ?: 0f
                    } ?: features.firstOrNull()

                    val edgeId = (highestFeature?.properties?.get("id") as? JsonPrimitive)?.content
                        ?: highestFeature?.id?.content
                    if (edgeId != null) {
                        onEdgeSelected(edgeId)
                    }
                    ClickResult.Consume
                }
            )
        }
    }

    LaunchedEffect(mapState) {
        snapshotFlow { mapState.style.loadState }
            .collect { state ->
                if (state is StyleLoadState.Failed && !isFallbackActive) {
                    isFallbackActive = true
                }
            }
    }

    LaunchedEffect(mapState) {
        snapshotFlow { mapState.viewport }
            .filterNotNull()
            .collect { vp ->
                val osmBox = vp.visibleBounds.toOsmBoundingBox()
                val osmZoom = osmEquivalentZoom(mapState.cameraPosition.zoom)
                onViewportChanged(osmBox, osmZoom)
            }
    }

    LaunchedEffect(timeRange, initialBounds) {
        val bounds = initialBounds ?: return@LaunchedEffect
        if (bounds.latNorth - bounds.latSouth < 0.0001 || bounds.lonEast - bounds.lonWest < 0.0001) {
            return@LaunchedEffect
        }
        val targetBbox = BoundingBox(
            southwest = Position(bounds.lonWest, bounds.latSouth),
            northeast = Position(bounds.lonEast, bounds.latNorth)
        )
        try {
            mapState.fitCameraToBounds(targetBbox)
        } catch (_: Throwable) {
            // Guard against camera animation cancellation on rapid range switching
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

private fun VisibleBounds.toOsmBoundingBox(): org.osmdroid.util.BoundingBox {
    fun normalizeLon(lon: Double): Double {
        var l = lon
        while (l > 180.0) l -= 360.0
        while (l < -180.0) l += 360.0
        return l
    }
    val n = north.coerceIn(-85.0, 85.0)
    val s = south.coerceIn(-85.0, 85.0)
    val e = normalizeLon(east)
    val w = normalizeLon(west)
    return org.osmdroid.util.BoundingBox(n, e, s, w)
}
