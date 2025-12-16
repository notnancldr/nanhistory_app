// ShareMap.kt
package id.my.nanclouder.nanhistory.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon as Material3Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.utils.history.EventRange
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import id.my.nanclouder.nanhistory.utils.history.TransportationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.io.File
import java.io.FileOutputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.createTempFile
import kotlin.math.round
import kotlin.math.roundToInt

@Composable
fun ShareableStatsCard(
    locationData: List<HistoryLocationData>,
    eventLocations: Map<ZonedDateTime, Coordinate>,
    duration: Long,
    eventTitle: String,
    eventData: HistoryEvent,
    widthPx: Float,
    heightPx: Float,
    showTitle: Boolean = true,
    showDescription: Boolean = true,
    showMap: Boolean = true,
    showStats: Boolean = true,
    modifier: Modifier = Modifier
) {
    val maxSpeed = if (locationData.isNotEmpty()) {
        round(locationData.maxOf { it.speed } * 10) / 10
    } else 0f

    val distance = locationData.sumOf { item -> item.distance.toDouble() }
    val avgSpeed = if (duration > 0 && distance > 0) {
        (distance / 100f / (duration / 3600f)).roundToInt() / 10f
    } else 0f

    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm") }

    // Format duration
    val durationText = remember(duration) {
        val durationObj = java.time.Duration.ofSeconds(duration)
        val days = durationObj.toDays()
        val hours = durationObj.toHoursPart()
        val minutes = durationObj.toMinutesPart()
        val seconds = durationObj.toSecondsPart()

        val parts = mutableListOf<String>()
        if (days > 0) parts.add("${days}d")
        if (hours > 0) parts.add("${hours}h")
        if (minutes > 0) parts.add("${minutes}m")
        if (seconds > 0) parts.add("${seconds}s")

        if (parts.isEmpty()) "0s" else parts.joinToString(" ")
    }

    // Get start and end times
    val startTime = remember {
        if (eventData is EventRange) {
            dateTimeFormatter.format(eventData.time)
        } else ""
    }

    val endTime = remember {
        if (eventData is EventRange) {
            dateTimeFormatter.format(eventData.end)
        } else ""
    }

    // Calculate aspect ratio and determine layout orientation
    val aspectRatio = widthPx / heightPx
    val isPortrait = aspectRatio < 1f
    val isSquare = aspectRatio in 0.9f..1.1f
    val isWide = aspectRatio > 1f

    // Responsive padding and spacing based on dimensions
    val basePadding = when {
        widthPx < 512f -> 12.dp
        widthPx < 1080f -> 16.dp
        widthPx < 2160f -> 20.dp
        else -> 24.dp
    }

    val baseSpacing = when {
        widthPx < 512f -> 8.dp
        widthPx < 1080f -> 12.dp
        widthPx < 2160f -> 16.dp
        else -> 20.dp
    }

    // Determine map weight based on aspect ratio
    val mapWeight = when {
        isPortrait -> 1.5f
        isSquare -> 1.0f
        isWide -> 0.8f
        else -> 1.0f
    }

    val statsArrangement = when {
        isWide && widthPx > 2000f -> Arrangement.spacedBy(baseSpacing)
        else -> Arrangement.spacedBy(baseSpacing)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(basePadding),
            verticalArrangement = Arrangement.spacedBy(baseSpacing)
        ) {
            // Title Section
            if (showTitle) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Trip Summary",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isWide && widthPx < 1000f) 1 else 2
                    )
                    Text(
                        eventTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isWide) 1 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (widthPx > 400f) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }

            // Description Section
            if (showDescription && eventData.description.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Description",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        eventData.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (widthPx > 400f) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }

            // Map Section
            if (showMap) {
                if (eventLocations.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(mapWeight)
                            .clip(RoundedCornerShape(12.dp)),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        TripMapView(
                            locations = eventLocations,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(mapWeight)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No map data",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (widthPx > 400f && showStats) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }

            // Stats Grid with Duration
            if (showStats) {
                Column(verticalArrangement = statsArrangement) {
                    // Combined Duration Card with Start and End Times
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Start Time
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Material3Icon(
                                    painter = painterResource(R.drawable.ic_location_start),
                                    contentDescription = "Start Time",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Start",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.sp
                                )
                                Text(
                                    startTime,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    fontSize = 10.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(60.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            )

                            // Duration
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val eventRange = eventData as? EventRange
                                val durationOrTransportation = eventRange?.let {
                                    if (it.transportationType == TransportationType.Unspecified) null
                                    else it.transportationType.name
                                } ?: "Duration"
                                Material3Icon(
                                    painter = painterResource(eventRange?.let {
                                        if (it.transportationType == TransportationType.Unspecified) null
                                        else it.transportationType.iconId
                                    } ?: R.drawable.ic_schedule),
                                    contentDescription = durationOrTransportation,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    durationOrTransportation,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.sp
                                )
                                Text(
                                    durationText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    fontSize = 10.sp
                                )
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(60.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            )

                            // End Time
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Material3Icon(
                                    painter = painterResource(R.drawable.ic_location_end),
                                    contentDescription = "End Time",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "End",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.sp
                                )
                                Text(
                                    endTime,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    fontSize = 10.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Speed and Distance stats
                    if (isWide && widthPx > 1800f) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(baseSpacing)
                        ) {
                            CompactStatItem(
                                label = "Avg Speed",
                                value = "$avgSpeed km/h",
                                modifier = Modifier.weight(1f),
                                iconResId = R.drawable.ic_speed
                            )
                            CompactStatItem(
                                label = "Max Speed",
                                value = "$maxSpeed km/h",
                                modifier = Modifier.weight(1f),
                                iconResId = R.drawable.ic_speed
                            )
                            CompactStatItem(
                                label = "Distance",
                                value = if (distance < 1000) "${round(distance).toInt()} m" else "${round(distance / 100) / 10} Km",
                                modifier = Modifier.weight(1f),
                                iconResId = R.drawable.ic_arrow_range
                            )
                        }
                    } else if (isWide && widthPx > 1200f) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(baseSpacing)
                        ) {
                            CompactStatItem(
                                label = "Avg Speed",
                                value = "$avgSpeed km/h",
                                modifier = Modifier.weight(1f),
                                iconResId = R.drawable.ic_speed
                            )
                            CompactStatItem(
                                label = "Max Speed",
                                value = "$maxSpeed km/h",
                                modifier = Modifier.weight(1f),
                                iconResId = R.drawable.ic_speed
                            )
                        }
                        CompactStatItem(
                            label = "Distance",
                            value = if (distance < 1000) "${round(distance).toInt()} m" else "${round(distance / 100) / 10} Km",
                            modifier = Modifier.fillMaxWidth(),
                            iconResId = R.drawable.ic_arrow_range
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(baseSpacing)
                        ) {
                            CompactStatItem(
                                label = "Avg Speed",
                                value = "$avgSpeed km/h",
                                modifier = Modifier.weight(1f),
                                iconResId = R.drawable.ic_speed
                            )
                            CompactStatItem(
                                label = "Max Speed",
                                value = "$maxSpeed km/h",
                                modifier = Modifier.weight(1f),
                                iconResId = R.drawable.ic_speed
                            )
                        }
                        CompactStatItem(
                            label = "Distance",
                            value = if (distance < 1000) "${round(distance).toInt()} m" else "${round(distance / 100) / 10} Km",
                            modifier = Modifier.fillMaxWidth(),
                            iconResId = R.drawable.ic_arrow_range
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    iconResId: Int? = null
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (iconResId != null) {
                Material3Icon(
                    painter = painterResource(iconResId),
                    contentDescription = label,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ShareStatItem(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    iconResId: Int? = null
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (iconResId != null) {
                Icon(
                    painter = painterResource(iconResId),
                    contentDescription = label,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TripMapView(
    locations: Map<ZonedDateTime, Coordinate>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val geoPoints = locations.keys.sorted().map {
        GeoPoint(locations[it]!!.latitude, locations[it]!!.longitude)
    }

    if (geoPoints.isEmpty()) return

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setMultiTouchControls(false)
                isClickable = false
                isEnabled = false

                setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                post {
                    maxZoomLevel = 18.0
                    try {
                        zoomToBoundingBox(geoPoints.toBoundingBox(), false)
                        setScrollableAreaLimitLatitude(mapCenter.latitude, mapCenter.latitude, 0)
                        setScrollableAreaLimitLongitude(mapCenter.longitude, mapCenter.longitude, 0)
                        minZoomLevel = zoomLevelDouble
                        maxZoomLevel = zoomLevelDouble
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        },
        update = { mapView ->
            mapView.overlays.clear()

            val polylineBorder = Polyline(mapView).apply {
                setPoints(geoPoints)
                outlinePaint.color = android.graphics.Color.rgb(0, 0, 20)
                outlinePaint.strokeWidth = 4f
                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
            }

            val polyline = Polyline(mapView).apply {
                setPoints(geoPoints)
                outlinePaint.color = android.graphics.Color.BLUE
                outlinePaint.strokeWidth = 3f
                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
            }

            mapView.overlays.add(polylineBorder)
            mapView.overlays.add(polyline)

            if (geoPoints.size > 1) {
                val startMarker = org.osmdroid.views.overlay.Marker(mapView).apply {
                    position = geoPoints.first()
                    icon = context.getDrawable(R.drawable.ic_location_start)
                }
                val endMarker = org.osmdroid.views.overlay.Marker(mapView).apply {
                    position = geoPoints.last()
                    icon = context.getDrawable(R.drawable.ic_location_end)
                }
                mapView.overlays.add(startMarker)
                mapView.overlays.add(endMarker)
            }

            mapView.invalidate()
        }
    )
}

private fun List<GeoPoint>.toBoundingBox(): org.osmdroid.util.BoundingBox? {
    if (this.isNotEmpty()) {
        val boundingBox = org.osmdroid.util.BoundingBox.fromGeoPointsSafe(this)
        val paddingFactor = 0.2

        return org.osmdroid.util.BoundingBox(
            boundingBox.latNorth + (boundingBox.latitudeSpan * paddingFactor),
            boundingBox.lonEast + (boundingBox.longitudeSpan * paddingFactor),
            boundingBox.latSouth - (boundingBox.latitudeSpan * paddingFactor),
            boundingBox.lonWest - (boundingBox.longitudeSpan * paddingFactor)
        )
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapShareDialog(
    onDismiss: () -> Unit,
    eventData: HistoryEvent,
    eventLocations: Map<ZonedDateTime, Coordinate>,
    locationData: List<HistoryLocationData>,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localDensity = LocalDensity.current

    var imageFormat by rememberSaveable { mutableStateOf("PNG") }
    var widthInput by rememberSaveable { mutableStateOf("1080") }
    var heightInput by rememberSaveable { mutableStateOf("1920") }
    var densityScale by rememberSaveable { mutableFloatStateOf(100f) }
    var isGenerating by rememberSaveable { mutableStateOf(false) }
    var generationProgress by rememberSaveable { mutableFloatStateOf(0f) }
    var formatExpanded by rememberSaveable { mutableStateOf(false) }
    var presetsExpanded by rememberSaveable { mutableStateOf(false) }

    var showTitleCheckbox by rememberSaveable { mutableStateOf(true) }
    var showDescriptionCheckbox by rememberSaveable { mutableStateOf(true) }
    var showMapCheckbox by rememberSaveable { mutableStateOf(true) }
    var showStatsCheckbox by rememberSaveable { mutableStateOf(true) }

    val parsedWidth = widthInput.toIntOrNull()
    val isValidWidth = parsedWidth != null && parsedWidth in 128..12000
    val actualWidthPx = parsedWidth?.toFloat() ?: 1080f

    val parsedHeight = heightInput.toIntOrNull()
    val isValidHeight = parsedHeight != null && parsedHeight in 128..12000
    val actualHeightPx = parsedHeight?.toFloat() ?: 1920f

    val isActionButtonsEnabled = !isGenerating && isValidWidth && isValidHeight

    val duration = if (eventData is EventRange) {
        eventData.end.toEpochSecond() - eventData.time.toEpochSecond()
    } else 0L

    val graphicsLayer = rememberGraphicsLayer()

    // Calculate DP values for the actual output size
    val targetWidthInDp = with(localDensity) { actualWidthPx.roundToInt().toDp() }
    val targetHeightInDp = with(localDensity) { actualHeightPx.roundToInt().toDp() }

    // Calculate custom density based on output resolution
    // Use 1080px as baseline (standard density = 1.0)
    val baselinePx = 1080f
    val baseDensityScale = actualWidthPx / baselinePx
    val userDensityScale = densityScale / 100f
    val finalDensityScale = baseDensityScale * userDensityScale

    val customDensity = localDensity.density * finalDensityScale
    val customFontScale = localDensity.fontScale * finalDensityScale

    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        icon = {
            Icon(Icons.Rounded.Share, "Share Map")
        },
        title = {
            Text("Share Trip Map", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState())
            ) {
                if (isGenerating) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Generating Image",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Rendering at ${actualWidthPx.toInt()} × ${actualHeightPx.toInt()}px",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        LinearProgressIndicator(
                            progress = { generationProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Text(
                            "${(generationProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Format Selection
                        ExposedDropdownMenuBox(
                            expanded = formatExpanded,
                            onExpandedChange = { formatExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = imageFormat,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Image Format") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) }
                            )
                            ExposedDropdownMenu(
                                expanded = formatExpanded,
                                onDismissRequest = { formatExpanded = false }
                            ) {
                                listOf("PNG", "JPEG").forEach { format ->
                                    DropdownMenuItem(
                                        text = { Text(format) },
                                        onClick = {
                                            imageFormat = format
                                            formatExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Quick Presets Dropdown
                        val presetOptions = listOf(
                            "Portrait (1080×1920 FHD)" to Pair(1080, 1920),
                            "Portrait (2160×3840 4K)" to Pair(2160, 3840),
                            "Landscape (1920×1080 FHD)" to Pair(1920, 1080),
                            "Landscape (3840×2160 4K)" to Pair(3840, 2160),
                            "Square (1080×1080)" to Pair(1080, 1080),
                            "Square (2160×2160 4K)" to Pair(2160, 2160),
                            "Instagram Story (1080×1920)" to Pair(1080, 1920),
                            "Instagram Post (1080×1080)" to Pair(1080, 1080),
                            "Twitter Header (1500×500)" to Pair(1500, 500),
                            "YouTube Thumbnail (1280×720)" to Pair(1280, 720),
                        )

                        ExposedDropdownMenuBox(
                            expanded = presetsExpanded,
                            onExpandedChange = { presetsExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = "Quick Presets",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Presets") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetsExpanded) }
                            )
                            ExposedDropdownMenu(
                                expanded = presetsExpanded,
                                onDismissRequest = { presetsExpanded = false }
                            ) {
                                presetOptions.forEach { (label, dimensions) ->
                                    DropdownMenuItem(
                                        text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                                        onClick = {
                                            widthInput = dimensions.first.toString()
                                            heightInput = dimensions.second.toString()
                                            presetsExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Width Input
                        OutlinedTextField(
                            value = widthInput,
                            onValueChange = { newValue -> widthInput = newValue },
                            label = { Text("Width (px)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = !isValidWidth,
                            supportingText = {
                                Text(
                                    if (!isValidWidth) "Width must be between 128px and 12000px"
                                    else "Min: 128px, Max: 12000px (Default: 1080px)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (!isValidWidth) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )

                        // Height Input
                        OutlinedTextField(
                            value = heightInput,
                            onValueChange = { newValue -> heightInput = newValue },
                            label = { Text("Height (px)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = !isValidHeight,
                            supportingText = {
                                Text(
                                    if (!isValidHeight) "Height must be between 128px and 12000px"
                                    else "Min: 128px, Max: 12000px (Default: 1920px)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (!isValidHeight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )

                        // Density Scale Slider
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Component Scale",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${densityScale.toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Slider(
                                    value = densityScale,
                                    onValueChange = { densityScale = it },
                                    valueRange = 50f..200f,
                                    steps = 29, // Creates steps of 5% (50..200 in steps of 5)
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text(
                                    "Adjust component size relative to image dimensions (50-200%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "Image Components",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )

                                // Title checkbox
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Checkbox(
                                        checked = showTitleCheckbox,
                                        onCheckedChange = { showTitleCheckbox = it },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        "Title",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Description checkbox
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Checkbox(
                                        checked = showDescriptionCheckbox && eventData.description.isNotBlank(),
                                        onCheckedChange = { showDescriptionCheckbox = it },
                                        modifier = Modifier.size(24.dp),
                                        enabled = eventData.description.isNotBlank()
                                    )
                                    Text(
                                        "Description",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Map checkbox
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Checkbox(
                                        checked = showMapCheckbox,
                                        onCheckedChange = { showMapCheckbox = it },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        "Map",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Stats checkbox
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Checkbox(
                                        checked = showStatsCheckbox,
                                        onCheckedChange = { showStatsCheckbox = it },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        "Summary Stats & Duration",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Preview Settings
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Export Settings",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Format: $imageFormat",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Size: ${actualWidthPx.toInt()} × ${actualHeightPx.toInt()}px",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Base Density: ${String.format("%.2f", actualWidthPx / 1080f)}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "User Scale: ${densityScale.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Final Scale: ${String.format("%.2f", (actualWidthPx / 1080f) * (densityScale / 100f))}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Box(Modifier.height(8.dp))
                // Live Preview Section (Scrollable) - This is also the capture source
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Preview & Capture Source (${actualWidthPx.toInt()} × ${actualHeightPx.toInt()}px)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )

                        // Responsive Preview Container - scales based on aspect ratio
                        val aspectRatio = actualWidthPx / actualHeightPx
                        val maxPreviewWidth = 350.dp
                        val maxPreviewHeight = 500.dp

                        val previewWidth = if (aspectRatio > 1f) {
                            // Wider than tall
                            maxPreviewWidth
                        } else {
                            // Taller than wide
                            maxPreviewHeight * aspectRatio
                        }

                        val previewHeight = if (aspectRatio > 1f) {
                            // Wider than tall
                            maxPreviewWidth / aspectRatio
                        } else {
                            // Taller than wide
                            maxPreviewHeight
                        }

                        // Scrollable Preview Container - This renders into the graphics layer
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .size(previewWidth, previewHeight)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .horizontalScroll(rememberScrollState())
                                .verticalScroll(rememberScrollState())
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(targetWidthInDp, targetHeightInDp)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .drawWithContent {
                                        graphicsLayer.record {
                                            this@drawWithContent.drawContent()
                                        }
                                        drawLayer(graphicsLayer)
                                    }
                                    .padding(4.dp)
                            ) {
                                // Apply custom density to the content
                                CompositionLocalProvider(
                                    LocalDensity provides Density(
                                        density = customDensity,
                                        fontScale = customFontScale
                                    )
                                ) {
                                    // The actual content that will be captured
                                    ShareableStatsCard(
                                        locationData = locationData,
                                        eventLocations = eventLocations,
                                        duration = duration,
                                        eventTitle = eventData.title,
                                        eventData = eventData,
                                        widthPx = actualWidthPx,
                                        heightPx = actualHeightPx,
                                        showTitle = showTitleCheckbox,
                                        showDescription = showDescriptionCheckbox,
                                        showMap = showMapCheckbox,
                                        showStats = showStatsCheckbox,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        Text(
                            "Scroll to view the entire image that will be captured",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!isGenerating) onDismiss() }) {
                Text("Cancel")
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Save Button
                Button(
                    onClick = {
                        scope.launch {
                            isGenerating = true
                            try {
                                generationProgress = 0.3f
                                delay(500)

                                val imageBitmap = graphicsLayer.toImageBitmap()
                                generationProgress = 0.6f

                                val bitmap = imageBitmap.asAndroidBitmap()
                                val fileName = "trip_${System.currentTimeMillis()}.${imageFormat.lowercase()}"

                                val uri = saveBitmapToPictures(context, bitmap, fileName, imageFormat)
                                if (uri != null) {
                                    Toast.makeText(context, "Image saved to gallery.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to save image.", Toast.LENGTH_SHORT).show()
                                }

                                generationProgress = 0.9f
                                delay(200)
                                onDismiss()
                            } catch (e: Exception) {
                                Log.e("MapShare", "Error saving map", e)
                                Toast.makeText(context, "Error saving image: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            } finally {
                                isGenerating = false
                                generationProgress = 0f
                            }
                        }
                    },
                    enabled = isActionButtonsEnabled
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(height = 20.dp, width = 28.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text("Save")
                }

                // Share Button
                Button(
                    onClick = {
                        scope.launch {
                            isGenerating = true
                            try {
                                generationProgress = 0.3f
                                delay(500)

                                val imageBitmap = graphicsLayer.toImageBitmap()
                                generationProgress = 0.6f

                                val bitmap = imageBitmap.asAndroidBitmap()
                                val file = File.createTempFile("trip_", ".${imageFormat.lowercase()}")

                                withContext(Dispatchers.IO) {
                                    FileOutputStream(file).use { fos ->
                                        val compressFormat = if (imageFormat == "PNG") {
                                            Bitmap.CompressFormat.PNG
                                        } else {
                                            Bitmap.CompressFormat.JPEG
                                        }
                                        bitmap.compress(compressFormat, 95, fos)
                                    }
                                }

                                Log.d("ShareMap", "File saved at ${file.absolutePath}")

                                generationProgress = 0.9f
                                shareFileAbsolute(context, file.absolutePath)

                                generationProgress = 1f
                                delay(200)
                                onDismiss()
                            } catch (e: Exception) {
                                Log.e("MapShare", "Error sharing map", e)
                                Toast.makeText(context, "Error sharing image: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            } finally {
                                isGenerating = false
                                generationProgress = 0f
                            }
                        }
                    },
                    enabled = isActionButtonsEnabled
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(height = 20.dp, width = 28.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text("Share")
                }
            }
        }
    )
}

/**
 * Saves the given bitmap to the device's Pictures directory under a "NanHistory" album.
 *
 * @param context The application context.
 * @param bitmap The bitmap to save.
 * @param fileName The desired file name (e.g., "my_image.png").
 * @param format The image format ("PNG" or "JPEG").
 * @return The URI of the saved image, or null if saving failed.
 */
fun saveBitmapToPictures(context: Context, bitmap: Bitmap, fileName: String, format: String): Uri? {
    val collection =
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/${format.lowercase()}")
        put(MediaStore.Images.Media.IS_PENDING, 1)
        // Save to a specific album named "NanHistory" within Pictures
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "NanHistory")
    }

    val resolver = context.contentResolver
    var uri: Uri? = null
    try {
        uri = resolver.insert(collection, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                val compressFormat = if (format == "PNG") {
                    Bitmap.CompressFormat.PNG
                } else {
                    Bitmap.CompressFormat.JPEG
                }
                bitmap.compress(compressFormat, 95, outputStream)
            }
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(it, contentValues, null, null)
        }
    } catch (e: Exception) {
        uri?.let { orphanUri ->
            // Clean up if something goes wrong during saving
            resolver.delete(orphanUri, null, null)
        }
        Log.e("saveBitmapToPictures", "Failed to save bitmap", e)
        throw e // Re-throw to be caught by the calling coroutine
    }
    return uri
}