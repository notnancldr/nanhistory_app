package id.my.nanclouder.nanhistory.feature.sharemap.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.my.nanclouder.nanhistory.feature.sharemap.R
import id.my.nanclouder.nanhistory.feature.sharemap.ShareMapViewModel
import id.my.nanclouder.nanhistory.ui.map.maplibre.TripPreviewMap
import id.my.nanclouder.nanhistory.core.common.storage.saveBitmapToPictures
import id.my.nanclouder.nanhistory.feature.sharemap.canvas.ShareCanvasTransformer
import id.my.nanclouder.nanhistory.feature.sharemap.canvas.ShareElementInspector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Duration.Companion.milliseconds

/**
 * Screen-level composable for the dedicated full-screen ShareMap editing studio.
 */
@Composable
fun ShareMapScreen(
    viewModel: ShareMapViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val cardBgColor = MaterialTheme.colorScheme.surface
    val scope = rememberCoroutineScope()
    val canvasGraphicsLayer = rememberGraphicsLayer()

    var cardBoundsInWindow by remember { mutableStateOf<Rect?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    var currentCameraLat by remember(uiState.customMapCenterLat) {
        mutableStateOf(uiState.customMapCenterLat)
    }
    var currentCameraLon by remember(uiState.customMapCenterLon) {
        mutableStateOf(uiState.customMapCenterLon)
    }
    var currentCameraZoom by remember(uiState.customMapZoom) {
        mutableStateOf(uiState.customMapZoom)
    }
    var fitRouteTrigger by remember { mutableIntStateOf(0) }

    val commitCurrentCamera: () -> Unit = {
        val bounds = cardBoundsInWindow
        val density = context.resources.displayMetrics.density
        val wDp = if (bounds != null && density > 0f) bounds.width() / density else 360f
        val hDp = if (bounds != null && density > 0f) bounds.height() / density else 360f
        viewModel.commitMapCamera(
            centerLat = currentCameraLat,
            centerLon = currentCameraLon,
            zoom = currentCameraZoom,
            previewWidthDp = wDp,
            previewHeightDp = hDp
        )
    }

    Scaffold(
        topBar = {
            Column {
                ShareMapTopBar(
                    onBack = onBack,
                    onExport = {
                        if (uiState.isMapAdjustMode) {
                            commitCurrentCamera()
                            viewModel.setMapAdjustMode(false)
                        }
                        viewModel.showExportSheet(true)
                    }
                )
                ShareMapActionBar(
                    canUndo = uiState.canUndo,
                    canRedo = uiState.canRedo,
                    showBackgroundMap = uiState.showBackgroundMap,
                    backgroundMapTheme = uiState.backgroundMapTheme,
                    isMapAdjustMode = uiState.isMapAdjustMode,
                    onUndo = viewModel::undo,
                    onRedo = viewModel::redo,
                    onReset = viewModel::resetLayout,
                    onToggleBackgroundMap = viewModel::toggleBackgroundMap,
                    onSelectMapTheme = viewModel::setBackgroundMapTheme,
                    onToggleMapAdjustMode = {
                        if (uiState.isMapAdjustMode) {
                            commitCurrentCamera()
                            viewModel.setMapAdjustMode(false)
                        } else {
                            viewModel.setMapAdjustMode(true)
                        }
                    }
                )
            }
        },
        bottomBar = {
            val selectedElement = remember(uiState.elements, uiState.selectedElementId) {
                uiState.elements.find { it.id == uiState.selectedElementId }
            }
            ShareMapBottomBar(
                modifier = Modifier.padding(bottom = 4.dp),
                currentAspect = uiState.aspectRatio,
                selectedElement = selectedElement,
                isMapAdjustMode = uiState.isMapAdjustMode,
                onSelectAspect = viewModel::setAspectRatio,
                onAddElement = viewModel::addElement,
                onDuplicateElement = viewModel::duplicateElement,
                onDeleteElement = viewModel::deleteElement,
                onOpenStyle = { viewModel.openStyleSheet(it) },
                onLayerUp = {
                    viewModel.updateElement(it.copy(zIndex = it.zIndex + 1f))
                    viewModel.onTransformCommit()
                },
                onLayerDown = {
                    viewModel.updateElement(it.copy(zIndex = (it.zIndex - 1f).coerceAtLeast(0f)))
                    viewModel.onTransformCommit()
                },
                onFitRoute = {
                    fitRouteTrigger++
                    currentCameraLat = null
                    currentCameraLon = null
                    currentCameraZoom = null
                    viewModel.resetMapCamera()
                },
                onDoneAdjustMap = {
                    commitCurrentCamera()
                    viewModel.setMapAdjustMode(false)
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val cardAspect = uiState.aspectRatio.ratio
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (cardAspect < 1f) 0.85f else 1f)
                            .aspectRatio(cardAspect)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            .onGloballyPositioned { coordinates ->
                                val bounds = coordinates.boundsInWindow()
                                cardBoundsInWindow = Rect(
                                    bounds.left.toInt(),
                                    bounds.top.toInt(),
                                    bounds.right.toInt(),
                                    bounds.bottom.toInt()
                                )
                            }
                    ) {
                        // 1. Background Route Map
                        if (uiState.showBackgroundMap && uiState.locations.isNotEmpty()) {
                            TripPreviewMap(
                                locations = uiState.locations,
                                forcedTheme = uiState.backgroundMapTheme,
                                isInteractive = uiState.isMapAdjustMode,
                                fitRouteTrigger = fitRouteTrigger,
                                initialCenterLat = uiState.customMapCenterLat,
                                initialCenterLon = uiState.customMapCenterLon,
                                initialZoom = uiState.customMapZoom,
                                onCameraChanged = { lat, lon, zoom ->
                                    currentCameraLat = lat
                                    currentCameraLon = lon
                                    currentCameraZoom = zoom
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // 2. Interactive Canvas Transformer
                        ShareCanvasTransformer(
                            elements = uiState.elements,
                            selectedElementId = uiState.selectedElementId,
                            eventTitle = uiState.eventTitle,
                            dateString = uiState.dateString,
                            eventData = uiState.eventData,
                            metricsText = uiState.metrics,
                            locations = uiState.locations,
                            elevationGainMeters = uiState.elevationGainMeters,
                            isMapAdjustMode = uiState.isMapAdjustMode,
                            onSelectElement = viewModel::selectElement,
                            onUpdateElement = viewModel::updateElement,
                            onDeleteElement = viewModel::deleteElement,
                            onOpenStyleSheet = { viewModel.openStyleSheet(it) },
                            onTransformCommit = viewModel::onTransformCommit,
                            isExporting = isCapturing,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (uiState.isMapAdjustMode) {
                                        Modifier.graphicsLayer { alpha = 0.85f }
                                    } else Modifier
                                )
                                .drawWithContent {
                                    canvasGraphicsLayer.record {
                                        this@drawWithContent.drawContent()
                                    }
                                    drawLayer(canvasGraphicsLayer)
                                }
                        )
                    }
                }
            }
        }
    }

    // Modal Style Property Inspector Sheet
    if (uiState.inspectingElement != null) {
        ShareElementInspector(
            element = uiState.inspectingElement!!,
            onUpdateElement = { updated ->
                viewModel.updateElement(updated)
                viewModel.openStyleSheet(updated)
                viewModel.onTransformCommit()
            },
            onDismiss = { viewModel.openStyleSheet(null) }
        )
    }

    // Modal Export Sheet
    if (uiState.showExportSheet) {
        ShareExportSheet(
            isExporting = isCapturing,
            onSaveToPictures = { format, baseDimensionPx ->
                scope.launch {
                    try {
                        isCapturing = true
                        viewModel.selectElement(null)
                        delay(150.milliseconds)

                        val imageBitmap = canvasGraphicsLayer.toImageBitmap()
                        val elementsBitmap = imageBitmap.asAndroidBitmap()

                        val window = (context as? Activity)?.window
                        val compositeBitmap = withContext(Dispatchers.Default) {
                            renderCardBitmap(
                                context, window, cardBoundsInWindow, uiState,
                                elementsBitmap, baseDimensionPx, isDark, cardBgColor
                            )
                        }

                        withContext(Dispatchers.IO) {
                            val fileName = "Trip_${System.currentTimeMillis()}.${format.lowercase()}"
                            saveBitmapToPictures(context, compositeBitmap, fileName, format)
                        }

                        Toast.makeText(
                            context,
                            context.applicationContext.getString(R.string.share_saved_toast),
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.showExportSheet(false)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            context.applicationContext.getString(R.string.share_export_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    } finally {
                        isCapturing = false
                    }
                }
            },
            onShareDirectly = { format, baseDimensionPx ->
                scope.launch {
                    try {
                        isCapturing = true
                        viewModel.selectElement(null)
                        delay(150.milliseconds)

                        val imageBitmap = canvasGraphicsLayer.toImageBitmap()
                        val elementsBitmap = imageBitmap.asAndroidBitmap()

                        val window = (context as? Activity)?.window
                        val compositeBitmap = withContext(Dispatchers.Default) {
                            renderCardBitmap(
                                context, window, cardBoundsInWindow, uiState,
                                elementsBitmap, baseDimensionPx, isDark, cardBgColor
                            )
                        }

                        val uri = withContext(Dispatchers.IO) {
                            val tempFile = File(context.cacheDir, "shared_trip.${format.lowercase()}")
                            FileOutputStream(tempFile).use { out ->
                                val compressFormat = if (format == "JPEG") {
                                    Bitmap.CompressFormat.JPEG
                                } else {
                                    Bitmap.CompressFormat.PNG
                                }
                                compositeBitmap.compress(compressFormat, 95, out)
                            }
                            androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                tempFile
                            )
                        }

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                context.applicationContext.getString(R.string.share_share_image)
                            )
                        )
                        viewModel.showExportSheet(false)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            context.applicationContext.getString(R.string.share_action_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    } finally {
                        isCapturing = false
                    }
                }
            },
            onDismiss = { viewModel.showExportSheet(false) }
        )
    }
}
