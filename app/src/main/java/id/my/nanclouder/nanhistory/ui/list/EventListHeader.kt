// EventListHeader.kt
package id.my.nanclouder.nanhistory.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.viewinterop.AndroidView
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toHistoryEvent
import id.my.nanclouder.nanhistory.utils.Coordinate
import id.my.nanclouder.nanhistory.utils.DateFormatter
import id.my.nanclouder.nanhistory.utils.history.HistoryDay
import id.my.nanclouder.nanhistory.ui.tags.TagsView
import id.my.nanclouder.nanhistory.utils.simplifyPoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.time.ZonedDateTime
import kotlin.math.pow

@Composable
fun EventListHeader(
    historyDay: HistoryDay,
    eventCount: Int,
    modifier: Modifier = Modifier,
    selected: Boolean,
    expanded: Boolean? = null,
    onExpandButtonClicked: (() -> Unit)? = null,
    onFavoriteChanged: (Boolean) -> Unit,
) {
    val newUI = Config.appearanceNewUI.getCache()
    if (newUI) EventListHeader_New(
        historyDay = historyDay,
        eventCount = eventCount,
        modifier = modifier,
        selected = selected,
        expanded = expanded,
        onExpandButtonClicked = onExpandButtonClicked,
        onFavoriteChanged = onFavoriteChanged,
    )
    else EventListHeader_Old(
        historyDay = historyDay,
        eventCount = eventCount,
        modifier = modifier,
        selected = selected,
        expanded = expanded,
        onExpandButtonClicked = onExpandButtonClicked,
        onFavoriteChanged = onFavoriteChanged,
    )
}

@Composable
fun EventListHeader_Old(
    historyDay: HistoryDay,
    eventCount: Int,
    modifier: Modifier = Modifier,
    selected: Boolean,
    expanded: Boolean? = null,
    onExpandButtonClicked: (() -> Unit)? = null,
    onFavoriteChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val tagData = historyDay.tags
    val headlineFontSize = 4.em
    val headlineFontWeight = FontWeight.W500
    var favorite by remember { mutableStateOf(historyDay.favorite) }
//    HorizontalDivider(modifier = dividerModifier)
    ListItem(
        modifier = modifier,
        /*
        modifier = modifier
            .clip(
                when (expanded) {
                    false -> RoundedCornerShape(24.dp)
                    true -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    else -> RectangleShape
                }
            ),
        leadingContent = if (expanded != null) ({
            IconButton(
                modifier = Modifier.width(32.dp),
                onClick = onExpandButtonClicked ?: { }
            ) {
                if (expanded) Icon(Icons.Rounded.KeyboardArrowUp, "Collapse")
                else Icon(Icons.Rounded.KeyboardArrowDown, "Expand")
            }
        }) else null,
        */
        headlineContent = {
            val format = DateFormatter
            val dateStr = historyDay.date.format(format)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    dateStr,
                    //                fontSize = headlineFontSize,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = headlineFontWeight,
                    textAlign = TextAlign.Center,
                    color = if (isSystemInDarkTheme()) Color.LightGray else Color.DarkGray
                )
                Box(Modifier.width(8.dp))
                Text(
                    text = "($eventCount)",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray
                )
            }
        },
        trailingContent = {
            IconButton(
                onClick = {
                    onFavoriteChanged(!favorite)
                    favorite = !favorite
                }
            ) {
                if (favorite) Icon(
                    painterResource(R.drawable.ic_favorite_filled), "",
                    tint = Color(0xFFFF7070)
                )
                else Icon(
                    painterResource(R.drawable.ic_favorite), ""
                )
            }
        },
        supportingContent = {
            TagsView(tagData)
        },
        colors = if (selected) ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            headlineColor = MaterialTheme.colorScheme.primary,
        ) else ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shadowElevation = if (expanded == true) 4.dp else ListItemDefaults.Elevation,
//        else ListItemDefaults.colors(
//            containerColor =
//                if (false) MaterialTheme.colorScheme.surfaceContainer
//                else MaterialTheme.colorScheme.surface,
//        ),
    )
}

@Composable
fun EventListHeader_New(
    historyDay: HistoryDay,
    eventCount: Int,
    modifier: Modifier = Modifier,
    selected: Boolean,
    expanded: Boolean? = null,
    onExpandButtonClicked: (() -> Unit)? = null,
    onFavoriteChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val tagData = historyDay.tags
    val headlineFontWeight = FontWeight.W600
    var favorite by remember { mutableStateOf(historyDay.favorite) }

    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

    var isExpanded by remember { mutableStateOf(false) }
    var tripLocations by remember { mutableStateOf<Map<ZonedDateTime, Coordinate>>(mapOf()) }
    var isLoadingMap by remember { mutableStateOf(false) }
    var mapLoadError by remember { mutableStateOf(false) }

    val allEvents by dao.getEventsByDay(historyDay.date).collectAsState(emptyList())

    LaunchedEffect(isExpanded, allEvents) {
        if (isExpanded && tripLocations.isEmpty()) {
            isLoadingMap = true
            mapLoadError = false
            try {
                withContext(Dispatchers.IO) {
                    tripLocations = allEvents.fold(mapOf<ZonedDateTime, Coordinate>()) { map, event ->
                        map + event.toHistoryEvent().getLocations(context)
                    }
                }
            } catch (e: Exception) {
                mapLoadError = true
                e.printStackTrace()
            } finally {
                isLoadingMap = false
            }
        }
    }

    val expandedElevation by animateDpAsState(
        targetValue = if (isExpanded) 6.dp else 2.dp,
        animationSpec = tween(300, easing = EaseInOutCubic),
        label = "elevation"
    )

    val favoriteScale by animateFloatAsState(
        targetValue = if (favorite) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "favoriteScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isExpanded) 0.dp else 16.dp,
                    bottomEnd = if (isExpanded) 0.dp else 16.dp
                )
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            color = if (selected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            else
                MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isExpanded) 0.dp else 16.dp,
                bottomEnd = if (isExpanded) 0.dp else 16.dp
            ),
            shadowElevation = expandedElevation
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Expand Button
                if (expanded != null) {
                    val rotationAngle by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = tween(300, easing = EaseInOutCubic),
                        label = "rotation"
                    )

                    IconButton(
                        modifier = Modifier
                            .size(40.dp),
                        onClick = {
                            isExpanded = !isExpanded
                            onExpandButtonClicked?.invoke()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            modifier = Modifier.rotate(rotationAngle),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Date and Event Count
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    val format = DateFormatter
                    val dateStr = historyDay.date.format(format)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            dateStr,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = headlineFontWeight,
                            color = if (selected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = eventCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Tags
                    if (tagData.isNotEmpty()) {
                        Box(Modifier.height(4.dp))
                        TagsView(tagData)
                    }
                }

                // Favorite Button
                IconButton(
                    modifier = Modifier
                        .size(40.dp)
                        .scale(favoriteScale),
                    onClick = {
                        onFavoriteChanged(!favorite)
                        favorite = !favorite
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (favorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite
                        ),
                        contentDescription = "Favorite",
                        tint = if (favorite)
                            Color(0xFFFF6B6B)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Expanded Trip Map Section
        AnimatedVisibility(
            visible = isExpanded && tripLocations.isNotEmpty(),
            enter = expandVertically(
                animationSpec = tween(300, easing = EaseInOutCubic)
            ) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(
                animationSpec = tween(300, easing = EaseInOutCubic)
            ) + fadeOut(animationSpec = tween(200))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Trip Map",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        TripMapView(
                            locations = tripLocations,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }
            }
        }

        // Loading State
        AnimatedVisibility(
            visible = isExpanded && isLoadingMap,
            enter = expandVertically(
                animationSpec = tween(300, easing = EaseInOutCubic)
            ) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(
                animationSpec = tween(300, easing = EaseInOutCubic)
            ) + fadeOut(animationSpec = tween(200))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(Modifier.height(12.dp))
                    Text(
                        "Loading trip data...",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Error State
        AnimatedVisibility(
            visible = isExpanded && mapLoadError,
            enter = expandVertically(
                animationSpec = tween(300, easing = EaseInOutCubic)
            ) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(
                animationSpec = tween(300, easing = EaseInOutCubic)
            ) + fadeOut(animationSpec = tween(200))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Rounded.Warning,
                        "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Failed to load trip data",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Empty State
        AnimatedVisibility(
            visible = isExpanded && tripLocations.isEmpty() && !isLoadingMap && !mapLoadError,
            enter = expandVertically(
                animationSpec = tween(300, easing = EaseInOutCubic)
            ) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(
                animationSpec = tween(300, easing = EaseInOutCubic)
            ) + fadeOut(animationSpec = tween(200))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No location data available",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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

    val sortedCoordinates = locations.keys.sorted().map { locations[it]!! }
    val simplifiedPoints = simplifyPoints(sortedCoordinates, epsilon = 0.00003)

    if (simplifiedPoints.isEmpty()) return

    val geoPoints = simplifiedPoints.map { GeoPoint(it.latitude, it.longitude) }

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
                outlinePaint.apply {
                    color = android.graphics.Color.rgb(0, 0, 20)
                    strokeWidth = 4f
                    strokeCap = android.graphics.Paint.Cap.ROUND
                }
            }

            val polyline = Polyline(mapView).apply {
                setPoints(geoPoints)
                outlinePaint.apply {
                    color = android.graphics.Color.BLUE
                    strokeWidth = 3f
                    strokeCap = android.graphics.Paint.Cap.ROUND
                }
            }

            mapView.overlays.add(polylineBorder)
            mapView.overlays.add(polyline)

            val endMarker = org.osmdroid.views.overlay.Marker(mapView).apply {
                position = geoPoints.last()
                icon = context.getDrawable(R.drawable.ic_location_end)
            }
            mapView.overlays.add(endMarker)

            if (geoPoints.size > 1) {
                val startMarker = org.osmdroid.views.overlay.Marker(mapView).apply {
                    position = geoPoints.first()
                    icon = context.getDrawable(R.drawable.ic_location_start)
                }
                mapView.overlays.add(startMarker)
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