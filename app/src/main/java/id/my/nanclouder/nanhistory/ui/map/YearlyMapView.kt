package id.my.nanclouder.nanhistory.ui.map

import android.content.Context
import android.graphics.Paint
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.db.AppDatabase
import java.time.Year
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import kotlin.math.max
import kotlin.math.min
import android.content.Intent
import id.my.nanclouder.nanhistory.EventDetailActivity
import id.my.nanclouder.nanhistory.ui.ComponentPlaceholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearlyMapView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var selectedYear by remember { mutableIntStateOf(Year.now().value) }
    var expandedYearSelector by remember { mutableStateOf(false) }
    
    // Map State
    var mapViewObj by remember { mutableStateOf<MapView?>(null) }
    
    // Data Loading State
    var isLoading by remember { mutableStateOf(false) }
    var yearlyMapData by remember { mutableStateOf<YearlyMapData?>(null) }
    
    // Configuration
    val precision by Config.yearlyMapPrecision.getState()
    val simplification by Config.yearlyMapSimplification.getState()
    
    // Bottom Sheet State
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedSegmentEventIds by remember { mutableStateOf<List<String>>(emptyList()) }
    val sheetState = rememberModalBottomSheetState()
    
    // Database
    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()
    
    val years = (2020..Year.now().value).toList().reversed()
    val screenDpi = LocalDensity.current.run { 1.dp.toPx() }

    // Helper to calculate color based on frequency
    fun calculateFrequencyColor(count: Int, maxCount: Int): Int {
        val normalized = if (maxCount > 1) (count - 1).toFloat() / (maxCount - 1) else 0f
        // Blue (Low) -> Cyan -> Green -> Yellow -> Red (High)
        // Hue: 240 (Blue) -> 0 (Red)
        val hue = 240f - (normalized * 240f)
        return Color.hsl(hue, 1f, 0.5f).toArgb()
    }

    // State for map and data
    var progress by remember { mutableFloatStateOf(0f) }
    var firstLocationSet by remember { mutableStateOf(false) }

    LaunchedEffect(selectedYear, precision, simplification) {
        isLoading = true
        progress = 0f
        yearlyMapData = null
        firstLocationSet = false
        mapViewObj?.overlays?.clear()
        mapViewObj?.invalidate()
        
        withContext(Dispatchers.IO) {
            loadYearlyMapData(context, dao, selectedYear, precision, simplification)
                .collect { state ->
                    withContext(Dispatchers.Main) {
                        if (state.data != null) {
                            yearlyMapData = state.data
                        }
                        progress = state.progress
                        
                        if (!firstLocationSet && state.firstLocation != null) {
                            mapViewObj?.controller?.setCenter(state.firstLocation)
                            mapViewObj?.controller?.setZoom(12.0)
                            firstLocationSet = true
                        }
                        
                        if (state.isComplete) {
                            isLoading = false
                        }
                    }
                }
        }
    }

    // Effect to update map when data changes (or map view becomes available)
    LaunchedEffect(yearlyMapData, mapViewObj) {
        val map = mapViewObj ?: return@LaunchedEffect
        val data = yearlyMapData ?: return@LaunchedEffect
        
        // We only clear if we are starting fresh or need full redraw.
        // But since we are incrementally updating 'yearlyMapData', we might be re-drawing everything.
        // Optimizing this for incremental addition would be better, but re-drawing 
        // the overlays is okay for now if not too frequent (we throttled emissions).
        
        map.overlays.clear()
        
        // Find max frequency for normalization
        val maxFreq = data.segments.values.maxOfOrNull { it.size } ?: 1
        
        data.segments.forEach { (segment, eventIds) ->
            val polyline = Polyline(map).apply {
                addPoint(segment.start)
                addPoint(segment.end)
                
                val color = calculateFrequencyColor(eventIds.size, maxFreq)
                outlinePaint.color = color
                
                // Thicker stroke for more frequent paths
                // Base 3dp, max +4dp
                val thickness = 3f + (if (maxFreq > 1) (eventIds.size - 1).toFloat() / (maxFreq - 1) * 4f else 0f)
                outlinePaint.strokeWidth = thickness * screenDpi
                outlinePaint.strokeCap = Paint.Cap.ROUND
                
                setOnClickListener { _, _, _ ->
                    selectedSegmentEventIds = eventIds
                    showBottomSheet = true
                    true
                }
            }
            map.overlays.add(polyline)
        }
        
        map.invalidate()
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Events on this path (${selectedSegmentEventIds.size})",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn {
                    items(selectedSegmentEventIds) { eventId ->
                        // We need to fetch event details. Ideally passed in data or fetched here.
                        // For now, we only have ID. Let's create a small item that fetches info or just ID.
                        // Actually, we can fetch basic info in Logic or just launch detail.
                        // Let's implement a simple row that loads the event title.
                        var eventTitle by remember { mutableStateOf("Loading...") }
                        var eventTime by remember { mutableStateOf("") }
                        
                        LaunchedEffect(eventId) {
                            val event = dao.getEventById(eventId)
                            eventTitle = event?.event?.title ?: "Unknown Event"
                            eventTime = event?.event?.time?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) ?: ""
                        }
                        
                        ListItem(
                            headlineContent = { Text(eventTitle) },
                            supportingContent = { Text(eventTime) },
                            modifier = Modifier.clickable {
                                val intent = Intent(context, EventDetailActivity::class.java).apply {
                                    putExtra("eventId", eventId)
                                }
                                context.startActivity(intent)
                            }
                        )
                        HorizontalDivider()
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { expandedYearSelector = true }
                            .padding(8.dp)
                    ) {
                        Text("Trips in $selectedYear")
                        Icon(Icons.Rounded.ArrowDropDown, "Select Year")
                        
                        DropdownMenu(
                            expanded = expandedYearSelector,
                            onDismissRequest = { expandedYearSelector = false }
                        ) {
                            years.forEach { year ->
                                DropdownMenuItem(
                                    text = { Text(year.toString()) },
                                    onClick = {
                                        selectedYear = year
                                        expandedYearSelector = false
                                    }
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { context.getActivity()?.finish() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = modifier.padding(paddingValues).fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
                        setMultiTouchControls(true)
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                        minZoomLevel = 3.0
                        maxZoomLevel = 20.0
                        controller.setZoom(5.0)
                        controller.setCenter(GeoPoint(0.0, 0.0)) // Default center
                        mapViewObj = this
                    }
                },
                update = { _ ->
                    // Update logic is handled in SideEffect/LaunchedEffect
                }
            )
            
            if (isLoading) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                )
            }
            
            // Transport Stats Overlay
            if (yearlyMapData != null && !isLoading && yearlyMapData!!.transportStats.isNotEmpty()) {
                 // Should we show stats? Maybe in a small card at bottom?
                 // Let's add a smallstats card
                 Card(
                     modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .fillMaxWidth(0.8f),
                     colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                 ) {
                     Column(modifier = Modifier.padding(12.dp)) {
                         Text("Transport Modes", style = MaterialTheme.typography.titleSmall)
                         Spacer(modifier = Modifier.height(4.dp))
                         // Simple text list or bar chart
                         yearlyMapData!!.transportStats.entries.sortedByDescending { it.value }.take(3).forEach { (type, percent) ->
                             Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                             ) {
                                 Text(type.name, style = MaterialTheme.typography.bodySmall)
                                 Text("${(percent * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                             }
                         }
                     }
                 }
            }
        }
    }
}

private fun Context.getActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.getActivity()
    else -> null
}
