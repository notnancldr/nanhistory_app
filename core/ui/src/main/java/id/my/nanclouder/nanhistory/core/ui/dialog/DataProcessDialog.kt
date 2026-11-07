package id.my.nanclouder.nanhistory.core.ui.dialog

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import id.my.nanclouder.nanhistory.core.common.dataprocess.DataProcessBridge
import id.my.nanclouder.nanhistory.core.common.dataprocess.DataProcessConstants
import id.my.nanclouder.nanhistory.core.common.dataprocess.DataProcessState
import id.my.nanclouder.nanhistory.core.common.dataprocess.ImportProgressStage
import id.my.nanclouder.nanhistory.core.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataProcessDialog() {
    val dataProcessType by DataProcessState.ServiceState.operationType.collectAsState()
    val dataProcessStage by DataProcessState.ImportState.stage.collectAsState()
    val migrationName by DataProcessState.ImportState.migrationName.collectAsState()

    val dataProcessProgress by DataProcessState.ServiceState.progress.collectAsState()
    val dataProcessProgressMax by DataProcessState.ServiceState.progressMax.collectAsState()

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer

    BasicAlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val context = LocalContext.current

                val title = when {
                    dataProcessStage == ImportProgressStage.Migrate -> "Updating Data"
                    dataProcessType == DataProcessConstants.OPERATION_IMPORT -> "Importing Data"
                    else -> "Processing"
                }

                val text = when {
                    dataProcessStage == ImportProgressStage.Migrate -> "Migrating: $migrationName"
                    dataProcessStage == ImportProgressStage.Init -> "Initializing..."
                    dataProcessStage == ImportProgressStage.Decrypt -> "Decrypting your backup..."
                    dataProcessStage == ImportProgressStage.Extract -> "Extracting files..."
                    else -> "Importing data..."
                }

                val infiniteTransition = rememberInfiniteTransition(label = "gradient")

                val offset by infiniteTransition.animateValue(
                    initialValue = 0.dp,
                    targetValue = 128.dp,
                    label = "offset",
                    typeConverter = Dp.VectorConverter,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )

                val tileSize = with(LocalDensity.current) {
                    64.dp.toPx()
                }

                val gradientColors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )

                val gradient = with(LocalDensity.current) {
                    Brush.horizontalGradient(
                        colors = gradientColors,
                        startX = 0f + offset.toPx(),
                        endX = tileSize + offset.toPx(),
                        tileMode = TileMode.Mirror
                    )
                }

                // Animated Icon with Glow Effect
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        contentDescription = "Processing Data",
                        painter = painterResource(R.drawable.ic_cloud_download),
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer(alpha = 0.99f)
                            .drawWithCache {
                                onDrawWithContent {
                                    drawContent()
                                    drawRect(gradient, blendMode = BlendMode.SrcAtop)
                                }
                            },
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Box(modifier = Modifier.height(24.dp))

                // Title
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(modifier = Modifier.height(24.dp))

                // Progress Bar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Percentage Text
                    val progressPercentage = if (dataProcessProgressMax > 0) {
                        ((dataProcessProgress * 100f) / dataProcessProgressMax).toInt()
                    } else {
                        0
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            getProgressLabel(dataProcessStage),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$progressPercentage%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Custom Progress Bar
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                    ) {
                        // Background
                        drawLine(
                            color = primaryContainerColor,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = size.height,
                            cap = StrokeCap.Round
                        )

                        // Progress
                        if (dataProcessProgressMax > 0) {
                            drawLine(
                                color = primaryColor,
                                start = Offset(0f, size.height / 2),
                                end = Offset(size.width * (dataProcessProgress.toFloat() / dataProcessProgressMax), size.height / 2),
                                strokeWidth = size.height,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                Box(modifier = Modifier.height(24.dp))

                // Cancel Button
                if (dataProcessType == DataProcessConstants.OPERATION_IMPORT) {
                    val isCancelDisabled = dataProcessStage == ImportProgressStage.Done ||
                            dataProcessStage == ImportProgressStage.Extract ||
                            dataProcessStage == ImportProgressStage.Migrate

                    Button(
                        enabled = !isCancelDisabled,
                        onClick = {
                            DataProcessBridge.cancel(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            "Cancel",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

private fun getProgressLabel(stage: ImportProgressStage?): String = when (stage) {
    ImportProgressStage.Init -> "Initializing..."
    ImportProgressStage.Decrypt -> "Decrypting backup..."
    ImportProgressStage.Extract -> "Extracting files..."
    ImportProgressStage.Migrate -> "Updating data..."
    else -> "Processing..."
}
