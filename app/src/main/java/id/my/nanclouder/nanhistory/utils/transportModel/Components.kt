// Components.kt
package id.my.nanclouder.nanhistory.utils.transportModel

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Card
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.DialogProperties
import id.my.nanclouder.nanhistory.utils.transportModel.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

@Composable
fun SamplesPerModeSection(
    samplesPerMode: Map<TransportMode, Int>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Samples Per Mode",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (samplesPerMode.isEmpty()) {
                Text(
                    text = "No samples available yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    samplesPerMode.forEach { (mode, samples) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mode.name,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "$samples",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccuracyGraph(
    accuracyHistory: List<Float>,
    currentAccuracy: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (accuracyHistory.isEmpty()) {
            drawText("No data yet", size.width / 2, size.height / 2)
            return@Canvas
        }

        val padding = 40f
        val graphWidth = size.width - padding * 2
        val graphHeight = size.height - padding * 2

        // Draw grid lines
        val gridColor = Color.White.copy(alpha = 0.1f)
        val gridStroke = Stroke(width = 1f)

        // Horizontal grid lines (accuracy levels)
        for (i in 0..10) {
            val y = padding + (graphHeight / 10f) * i
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(padding, y),
                end = androidx.compose.ui.geometry.Offset(size.width - padding, y),
                strokeWidth = 1f
            )
            // Draw percentage labels
            drawText(
                text = "${100 - (i * 10)}%",
                x = padding - 35f,
                y = y + 5f,
                textSize = 12.sp.toPx()
            )
        }

        // Draw axes
        val axisColor = Color.White.copy(alpha = 0.3f)
        drawLine(
            color = axisColor,
            start = androidx.compose.ui.geometry.Offset(padding, padding),
            end = androidx.compose.ui.geometry.Offset(padding, size.height - padding),
            strokeWidth = 2f
        )
        drawLine(
            color = axisColor,
            start = androidx.compose.ui.geometry.Offset(padding, size.height - padding),
            end = androidx.compose.ui.geometry.Offset(size.width - padding, size.height - padding),
            strokeWidth = 2f
        )

        // Draw accuracy curve
        val pointCount = accuracyHistory.size
        val pointSpacing = graphWidth / (pointCount - 1).coerceAtLeast(1)

        for (i in 0 until pointCount - 1) {
            val x1 = padding + i * pointSpacing
            val y1 = size.height - padding - (accuracyHistory[i] * graphHeight)

            val x2 = padding + (i + 1) * pointSpacing
            val y2 = size.height - padding - (accuracyHistory[i + 1] * graphHeight)

            drawLine(
                color = Color(0xFF00D9FF),
                start = androidx.compose.ui.geometry.Offset(x1, y1),
                end = androidx.compose.ui.geometry.Offset(x2, y2),
                strokeWidth = 3f
            )
        }

        // Draw current accuracy point
        val lastX = padding + (pointCount - 1) * pointSpacing
        val lastY = size.height - padding - (currentAccuracy * graphHeight)

        drawCircle(
            color = Color(0xFF00FF88),
            radius = 6f,
            center = androidx.compose.ui.geometry.Offset(lastX, lastY)
        )

        // Draw current accuracy value label
        drawText(
            text = "${(currentAccuracy * 100).roundToInt()}%",
            x = lastX - 20f,
            y = lastY - 20f,
            textSize = 14.sp.toPx(),
            color = Color(0xFF00FF88)
        )
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ModelParametersSection(
    models: Map<TransportMode, CalibrationModel>
) {
    var expandedMode by remember { mutableStateOf<TransportMode?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Model Parameters",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (models.isEmpty()) {
                Text(
                    text = "No models trained yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransportMode.entries.forEach { mode ->
                        val model = models[mode]
                        if (model != null) {
                            TransportModeParameterItem(
                                mode = mode,
                                model = model,
                                isExpanded = expandedMode == mode,
                                onToggle = {
                                    expandedMode = if (expandedMode == mode) null else mode
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransportModeParameterItem(
    mode: TransportMode,
    model: CalibrationModel,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mode.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Range Parameters Section
                    Text(
                        text = "Range Parameters",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ParameterRow("Avg Speed", "${model.avgSpeedMin.roundTo(2)} - ${model.avgSpeedMax.roundTo(2)}")
                        ParameterRow("Top Speed", "${model.topSpeedMin.roundTo(2)} - ${model.topSpeedMax.roundTo(2)}")
                        ParameterRow("Avg Accel", "${model.avgAccelMin.roundTo(2)} - ${model.avgAccelMax.roundTo(2)}")
                        ParameterRow("Stop Duration", "${model.stopDurationMin.roundTo(2)} - ${model.stopDurationMax.roundTo(2)}")
                        ParameterRow("Distance", "${model.distanceMin.roundTo(2)} - ${model.distanceMax.roundTo(2)}")
                        ParameterRow("Speed Variance", "${model.speedVarianceMin.roundTo(2)} - ${model.speedVarianceMax.roundTo(2)}")
                        ParameterRow("Accel Variance", "${model.accelVarianceMin.roundTo(2)} - ${model.accelVarianceMax.roundTo(2)}")
                        ParameterRow("Path Complexity", "${model.pathComplexityMin.roundTo(2)} - ${model.pathComplexityMax.roundTo(2)}")

                        ParameterRow("Coasting 500", "${model.maxCoastingDuration05Min.roundTo(2)} - ${model.maxCoastingDuration05Max.roundTo(2)}")
                        ParameterRow("Coasting 1000", "${model.maxCoastingDuration10Min.roundTo(2)} - ${model.maxCoastingDuration10Max.roundTo(2)}")
                        ParameterRow("Coasting 2000", "${model.maxCoastingDuration20Min.roundTo(2)} - ${model.maxCoastingDuration20Max.roundTo(2)}")
                        ParameterRow("Coasting 4000", "${model.maxCoastingDuration40Min.roundTo(2)} - ${model.maxCoastingDuration40Max.roundTo(2)}")
                    }

                    // Ideal Values Section
                    if (model.idealAvgSpeed != null || model.idealTopSpeed != null) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "Ideal Values",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (model.idealAvgSpeed != null) {
                                ParameterRow("Ideal Avg Speed", model.idealAvgSpeed.roundTo(2))
                            }
                            if (model.idealTopSpeed != null) {
                                ParameterRow("Ideal Top Speed", model.idealTopSpeed.roundTo(2))
                            }
                            if (model.idealAvgAccel != null) {
                                ParameterRow("Ideal Avg Accel", model.idealAvgAccel.roundTo(2))
                            }
                            if (model.idealStopDuration != null) {
                                ParameterRow("Ideal Stop Duration", model.idealStopDuration.roundTo(2))
                            }
                            if (model.idealDistance != null) {
                                ParameterRow("Ideal Distance", model.idealDistance.roundTo(2))
                            }

                            if (model.idealMaxCoastingDuration05 != null) {
                                ParameterRow("Ideal Coasting 500", model.idealMaxCoastingDuration05.roundTo(2))
                            }
                            if (model.idealMaxCoastingDuration10 != null) {
                                ParameterRow("Ideal Coasting 1000", model.idealMaxCoastingDuration10.roundTo(2))
                            }
                            if (model.idealMaxCoastingDuration20 != null) {
                                ParameterRow("Ideal Coasting 2000", model.idealMaxCoastingDuration20.roundTo(2))
                            }
                            if (model.idealMaxCoastingDuration40 != null) {
                                ParameterRow("Ideal Coasting 4000", model.idealMaxCoastingDuration40.roundTo(2))
                            }
                        }
                    }

                    // Feature Importance Weights Section
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "Feature Importance Weights",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeightBar("Avg Speed", model.avgSpeedWeight)
                        WeightBar("Top Speed", model.topSpeedWeight)
                        WeightBar("Avg Accel", model.avgAccelWeight)
                        WeightBar("Stop Duration", model.stopDurationWeight)
                        WeightBar("Distance", model.distanceWeight)
                        WeightBar("Speed Variance", model.speedVarianceWeight)
                        WeightBar("Accel Variance", model.accelVarianceWeight)
                        WeightBar("Path Complexity", model.pathComplexityWeight)
                        WeightBar("Coasting 500", model.maxCoastingDuration05Weight)
                        WeightBar("Coasting 1000", model.maxCoastingDuration10Weight)
                        WeightBar("Coasting 2000", model.maxCoastingDuration20Weight)
                        WeightBar("Coasting 4000", model.maxCoastingDuration40Weight)
                    }

                    // Model Parameters Section
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "Model Parameters",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ParameterRow(
                            "Mode Bias",
                            String.format("%.3f", model.modeBias),
                            isHighlight = model.modeBias > 0.01f || model.modeBias < -0.01f
                        )
                        ParameterRow(
                            "Confidence Threshold",
                            String.format("%.2f%%", model.confidenceThreshold * 100),
                            isHighlight = true
                        )
                        ParameterRow(
                            "Weight",
                            model.weight.roundTo(2).toString(),
                            isHighlight = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeightBar(
    label: String,
    weight: Float
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = String.format("%.2f", weight),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Visual weight bar
        val normalizedWeight = (weight / 2.0f).coerceIn(0f, 1f) // Normalize to 0-2 range
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(3.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(normalizedWeight)
                    .background(
                        color = when {
                            weight > 1.2f -> Color(0xFF00FF88) // High importance
                            weight > 0.8f -> Color(0xFF00D9FF) // Normal
                            else -> Color(0xFFFFB800)           // Low importance
                        },
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

@Composable
fun ParameterRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun EvaluationResultCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = color
                )
            }
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportModelTrainingModal() {
    var state by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .clickable {
                state = true
            }
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Detection model training",
                style = MaterialTheme.typography.titleSmall
            )
        }
    }

    if (state) BasicAlertDialog(
        onDismissRequest = {
            state = false
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        TransportModelTrainingScreen()
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / 1000 / 60) % 60
    val hours = millis / 1000 / 60 / 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

private fun DrawScope.drawText(
    text: String,
    x: Float,
    y: Float,
    textSize: Float = 12.sp.toPx(),
    color: Color = Color.White
) {
    // This is a simplified text drawing. In production, use TextMeasurer
    // For now, this demonstrates the concept
}

private fun Float.roundTo(decimals: Int): String {
    val multiplier = 10.0.pow(decimals.toDouble()).toInt()
    return (kotlin.math.round(this * multiplier) / multiplier).toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportModelTrainingScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val trainer = remember { mutableStateListOf(TransportModelTrainer(context, scope)) }
    val trainingState = remember { mutableStateMapOf(0 to TrainingState()) }

    var selectedProcessCount by remember { mutableIntStateOf(1) }

    var parallelProcessCount by remember { mutableIntStateOf(1) }
    var selectedProcessIndex by remember { mutableIntStateOf(0) }

    var lockOnTheBest by remember { mutableStateOf(false) }

    val currentTrainingState = trainingState[selectedProcessIndex]

    var selectedStrategy by remember { mutableStateOf(ScoringStrategy.RANGE_BASED) }
    var showStrategyMenu by remember { mutableStateOf(false) }
    var evaluationResult by remember { mutableStateOf<TrainingResult?>(null) }
    var showEvaluationDialog by remember { mutableStateOf(false) }

    var showModelManagement by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    var weightCap by remember { mutableFloatStateOf(64f) }
    var useWeightCap by remember { mutableStateOf(false) }

    var targetAcc by remember { mutableFloatStateOf(64f) }
    var useTargetAcc by remember { mutableStateOf(false) }

    var learningRate by remember { mutableFloatStateOf(0.05f) }
    var bufferSize by remember { mutableIntStateOf(8) }
    var cleanCache by remember { mutableStateOf(false) }

    var evaluateJob by remember { mutableStateOf<Job?>(null) }

    val isRunning = trainingState.any { it.value.isRunning }

    var isLoading by remember { mutableStateOf(false) }

    // Load cache
    LaunchedEffect(Unit) {
        isLoading = true
        TrainingDataCache.cache ?: getTrainingData(context)
        isLoading = false
    }

    fun registerTrainingUpdates() {
        trainer.forEachIndexed { index, trainer ->
            trainer.setStateListener { newState ->
                trainingState[index] = newState
            }
        }
    }

    if (lockOnTheBest) {
        val bestAcc = trainingState.maxBy { it.value.currentAccuracy }
        if (selectedProcessIndex != bestAcc.key)
            selectedProcessIndex = bestAcc.key
    }

    LaunchedEffect(Unit) {
        registerTrainingUpdates()
    }

    // Listen to training updates
    LaunchedEffect(trainer, parallelProcessCount) {
        selectedProcessIndex = min(selectedProcessIndex, trainer.size - 1)
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            trainer.forEach { trainer ->
                trainer.stopTraining()
            }
        }
    }

    if (isLoading) Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
    }
    else Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.surface
            )
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Model Training",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Strategy Selector Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Scoring Strategy",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Box {
                        Button(
                            onClick = { showStrategyMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                Icons.Rounded.Settings,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 8.dp)
                            )
                            Text(selectedStrategy.name)
                        }

                        DropdownMenu(
                            expanded = showStrategyMenu,
                            onDismissRequest = { showStrategyMenu = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            ScoringStrategy.entries.forEach { strategy ->
                                DropdownMenuItem(
                                    text = { Text(strategy.name) },
                                    onClick = {
                                        selectedStrategy = strategy
                                        showStrategyMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Weight cap
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(bottom = 16.dp).padding(horizontal = 16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Weight Cap",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Switch(
                            checked = useWeightCap,
                            onCheckedChange = {
                                useWeightCap = it
                            },
                            enabled = !isRunning
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = if (!useWeightCap) Modifier.alpha(0.5f)
                        else Modifier
                    ) {
                        Slider(
                            value = weightCap,
                            onValueChange = {
                                weightCap = it
                            },
                            valueRange = 8f..256f,
                            steps = 30,
                            modifier = Modifier.weight(1f),
                            enabled = useWeightCap && !isRunning
                        )
                        Text(
                            text = "${weightCap.roundToInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.wrapContentWidth()
                        )
                    }
                }
            }

            // Accuracy target
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(bottom = 16.dp).padding(horizontal = 16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Target Accuracy",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Switch(
                            checked = useTargetAcc,
                            onCheckedChange = {
                                useTargetAcc = it
                            },
                            enabled = !isRunning
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = if (!useTargetAcc) Modifier.alpha(0.5f)
                        else Modifier
                    ) {
                        Slider(
                            value = targetAcc,
                            onValueChange = {
                                targetAcc = it
                            },
                            valueRange = 1f..100f,
                            steps = 98,
                            modifier = Modifier.weight(1f),
                            enabled = useTargetAcc && !isRunning
                        )
                        Text(
                            text = "${targetAcc.roundToInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.wrapContentWidth()
                        )
                    }
                }
            }

            // Learning Rate (0f == no adaptive learning)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Learning Rate",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Slider(
                            value = learningRate,
                            onValueChange = {
                                learningRate = it
                            },
                            valueRange = 0f..1f,
                            steps = 98,
                            modifier = Modifier.weight(1f),
                            enabled = !isRunning
                        )
                        Text(
                            text = "${round(learningRate * 100) / 100}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.wrapContentWidth()
                        )
                    }
                }
            }

            // Buffer size (0 == no buffering)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Buffer Size",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Slider(
                            value = bufferSize.toFloat(),
                            onValueChange = {
                                bufferSize = it.toInt()
                            },
                            valueRange = 1f..32f,
                            steps = 29,
                            modifier = Modifier.weight(1f),
                            enabled = !isRunning
                        )
                        Text(
                            text = "$bufferSize",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.wrapContentWidth()
                        )
                    }
                }
            }


            // Clean cache (false == use cache)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Clean Cache",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Switch(
                            checked = cleanCache,
                            onCheckedChange = {
                                cleanCache = it
                            },
                            enabled = !isRunning
                        )
                    }
                }
            }

            // Process count
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Process Count",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Slider(
                            value = selectedProcessCount.toFloat(),
                            onValueChange = {
                                selectedProcessCount = it.toInt()
                            },
                            valueRange = 1f..64f,
                            steps = 61,
                            modifier = Modifier.weight(1f),
                            enabled = !isRunning
                        )
                        Text(
                            text = "$selectedProcessCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.wrapContentWidth()
                        )
                    }

                    Button(
                        onClick = {
                            trainer.forEach {
                                it.stopTraining()
                            }
                            trainer.clear()
                            trainingState.clear()
                            trainingState[0] = TrainingState()
                            repeat(selectedProcessCount) {
                                trainer.add(TransportModelTrainer(context, scope))
                            }
                            parallelProcessCount = trainer.size

                            registerTrainingUpdates()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isRunning
                    ) {
                        Text("Init $selectedProcessCount process${if (selectedProcessCount > 1) "es" else ""}")
                    }
                }
            }

            // Accuracy Graph Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((300 + max(0, (parallelProcessCount / 3) * 64 - 128)).dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = if (parallelProcessCount == 1) "Process" else "Processes ($parallelProcessCount)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (isRunning && parallelProcessCount > 1) TextButton(
                            onClick = {
                                lockOnTheBest = !lockOnTheBest
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor =
                                    if (lockOnTheBest) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Best Accuracy Lock")
                        }
                    }

                    val currentBestTraining = trainingState.maxBy { it.value.currentAccuracy }

                    if (trainer.size > 1) for (keys in trainingState.keys.chunked(3)) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            val trainingStates = keys.associateWith { trainingState[it] }.filterNot { it.value == null }

                            for ((index, item) in trainingStates) {
                                item!!
                                val graphBg =
                                    if (selectedProcessIndex == index)
                                        MaterialTheme.colorScheme.surfaceVariant
                                    else if (index == currentBestTraining.key)
                                        MaterialTheme.colorScheme.primary
                                    else if (item.currentAccuracy >= (targetAcc / 100f) && useTargetAcc && !item.isRunning)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else if (!item.isRunning)
                                        MaterialTheme.colorScheme.errorContainer
                                    else Color.Transparent

                                Column(
                                    modifier = Modifier
                                        .clickable {
                                            selectedProcessIndex = index
                                        }
                                        .background(graphBg.copy(alpha = 0.2f))
                                        .weight(1f)
                                ) {
                                    AccuracyGraph(
                                        accuracyHistory = item.accuracyHistory,
                                        currentAccuracy = item.currentAccuracy,
                                        modifier = Modifier
                                            .fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                    else AccuracyGraph(
                        accuracyHistory = currentTrainingState!!.accuracyHistory,
                        currentAccuracy = currentTrainingState.currentAccuracy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }

            if (currentTrainingState != null) {
                // Stats Cards Grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val currentAcc = (currentTrainingState.currentAccuracy * 100).roundToInt()
                    val bestAcc = (trainingState.maxOf { it.value.currentAccuracy } * 100).roundToInt()

                    StatCard(
                        label = "Current Accuracy",
                        value = "$currentAcc%",
                        icon = Icons.AutoMirrored.Rounded.TrendingUp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        label = "Iteration",
                        value = currentTrainingState.iteration.toString(),
                        icon = Icons.Rounded.Repeat,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Correct / Total",
                        value = "${currentTrainingState.correctSamples} / ${currentTrainingState.totalSamples}",
                        icon = Icons.Rounded.CheckCircle,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        label = "Duration",
                        value = formatDuration(currentTrainingState.trainingDuration),
                        icon = Icons.Rounded.Schedule,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Control Button
            Button(
                onClick = {
                    if (isRunning) {
                        trainer.forEachIndexed { index, process ->
                            process.stopTraining()
                            trainingState[index]?.copy(isRunning = false)?.let {
                                trainingState[index] = it
                            }
                        }
                    } else {
                        trainer.forEach { process ->
                            scope.launch {
                                process.startTraining(
                                    strategy = selectedStrategy,
                                    weightCap = if (useWeightCap) weightCap else null,
                                    targetAccuracy = if (useTargetAcc) targetAcc.roundToInt() else null,
                                    learningRate = learningRate,
                                    bufferSize = bufferSize,
                                    cleanCache = cleanCache,
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (isRunning) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
                )
            ) {
                Icon(
                    if (isRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp)
                )
                Text(
                    if (isRunning) "Stop Training" else "Start Training",
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 16.sp
                )
            }

            if (currentTrainingState != null) {
                OutlinedButton(
                    onClick = {
                        evaluateJob = scope.launch {
                            val evalResult = evaluate(
                                currentTrainingState.models,
                                getTrainingData(context),
                                selectedStrategy
                            )
                            evaluationResult = evalResult
                            showEvaluationDialog = true

                            delay(10)
                            evaluateJob = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = evaluateJob == null
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = if (evaluateJob == null) "Evaluate"
                        else "Evaluating...",
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 14.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clearModels(context)
                            val currentStates = trainingState.toMap()
                            currentStates.forEach { (index, state) ->
                                trainingState[index] = state.copy(models = emptyMap())
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp)
                        )
                        Text(
                            text = "Clear Models",
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            resetModels(context)
                            val currentStates = trainingState.toMap()
                            currentStates.forEach { (index, state) ->
                                trainingState[index] = state.copy(models = DEFAULT_TRANSPORT_MODE_DETECTION_MODEL)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp)
                        )
                        Text(
                            "Reset",
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 14.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showModelManagement = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp)
                        )
                        Text(
                            "Load Model",
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 14.sp
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            showSaveDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Save,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp)
                        )
                        Text(
                            "Save Model",
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 14.sp
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        saveCalibrationModels(context, currentTrainingState.models)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp)
                    )
                    Text(
                        "Apply Model",
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 14.sp
                    )
                }

                // Model Parameters Card (Collapsible)
                ModelParametersSection(models = currentTrainingState.models)

                SamplesPerModeSection(samplesPerMode = currentTrainingState.totalSamplesPerMode)

                // Evaluation Result Dialog
                if (showEvaluationDialog && evaluationResult != null) {
                    AlertDialog(
                        onDismissRequest = { showEvaluationDialog = false },
                        title = {
                            Text(
                                "Evaluation Results",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                EvaluationResultCard(
                                    label = "Overall Accuracy",
                                    value = "${(evaluationResult!!.accuracy * 100).roundToInt()}%",
                                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                EvaluationResultCard(
                                    label = "Correct Predictions",
                                    value = "${evaluationResult!!.correctSamples} / ${evaluationResult!!.totalSamples}",
                                    icon = Icons.Rounded.CheckCircle,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Evaluation Details",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Strategy: ${selectedStrategy.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Total Samples: ${evaluationResult!!.totalSamples}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { showEvaluationDialog = false }
                            ) {
                                Text("Close")
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            }

            // Status indicator
            if (isRunning) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Animated pulsing dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = "Training in progress...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }

    // Save Model Dialog
    if (showSaveDialog) {
        if (currentTrainingState != null) {
            SaveModelDialog(
                currentAccuracy = currentTrainingState.currentAccuracy,
                totalSamples = currentTrainingState.totalSamples,
                correctSamples = currentTrainingState.correctSamples,
                strategy = selectedStrategy,
                models = currentTrainingState.models,
                onDismiss = { showSaveDialog = false },
                onSave = { modelName ->
                    scope.launch {
                        ModelManager.saveModel(
                            context,
                            modelName,
                            currentTrainingState.currentAccuracy,
                            currentTrainingState.totalSamples,
                            currentTrainingState.correctSamples,
                            selectedStrategy,
                            currentTrainingState.models
                        )
                    }
                }
            )
        }
    }

    // Model Management Dialog
    if (showModelManagement) {
        ModelManagementDialog(
            onDismiss = { showModelManagement = false },
            onLoadModel = { models ->
                saveCalibrationModels(context, models)
                currentTrainingState?.copy(models = models)?.let {
                    trainingState[selectedProcessIndex] = it
                }
            },
            context = context
        )
    }
}

@Composable
fun SaveModelDialog(
    currentAccuracy: Float,
    totalSamples: Int,
    correctSamples: Int,
    strategy: ScoringStrategy,
    models: Map<TransportMode, CalibrationModel>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var modelName by remember { mutableStateOf("Model_${System.currentTimeMillis() / 1000}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Model") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Current Accuracy: ${(currentAccuracy * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyMedium
                )
                TextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("Model Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(modelName)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ModelManagementDialog(
    onDismiss: () -> Unit,
    onLoadModel: (Map<TransportMode, CalibrationModel>) -> Unit,
    context: Context
) {
    val allModels = remember {
        mutableStateOf(ModelManager.getSavedModelsSortedByAccuracy(context))
    }
    var deletingModel by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Saved Models") },
        text = {
            if (allModels.value.isEmpty()) {
                Text(
                    "No saved models yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allModels.value.forEach { metadata ->
                        SavedModelItem(
                            metadata = metadata,
                            onLoad = {
                                val savedModel = ModelManager.loadModel(context, metadata.name)
                                if (savedModel != null) {
                                    onLoadModel(savedModel.models)
                                    onDismiss()
                                }
                            },
                            onDelete = {
                                deletingModel = metadata.name
                            },
                            onCopy = {
                                ModelManager.copyFullModelToClipboard(context, metadata.name)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        modifier = Modifier.fillMaxWidth(0.9f)
    )

    // Delete confirmation dialog
    if (deletingModel != null) {
        AlertDialog(
            onDismissRequest = { deletingModel = null },
            title = { Text("Delete Model?") },
            text = { Text("Are you sure you want to delete ${deletingModel}?") },
            confirmButton = {
                Button(
                    onClick = {
                        ModelManager.deleteModel(context, deletingModel!!)
                        allModels.value = ModelManager.getSavedModelsSortedByAccuracy(context)
                        deletingModel = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingModel = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SavedModelItem(
    metadata: SavedModelMetadata,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = metadata.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = metadata.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${(metadata.accuracy * 100).roundToInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${metadata.correctSamples}/${metadata.totalSamples}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = metadata.strategy.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            OutlinedButton(
                onClick = onCopy,
                modifier = Modifier
                    .height(36.dp)
                    .fillMaxWidth(),
            ) {
                Icon(
                    Icons.Rounded.FileCopy,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 4.dp)
                )
                Text("Copy Model", fontSize = 12.sp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onLoad,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                    Text("Load", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                    Text("Delete", fontSize = 12.sp)
                }
            }
        }
    }
}
