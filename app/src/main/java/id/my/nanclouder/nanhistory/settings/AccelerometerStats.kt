package id.my.nanclouder.nanhistory.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.my.nanclouder.nanhistory.utils.AccelerometerChange
import java.io.File
import kotlin.math.max

@Composable
fun AccelerometerGraph(
    data: List<AccelerometerChange>,
    modifier: Modifier = Modifier,
    showLegend: Boolean = true
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.Black.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text("No Data", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val maxVal = remember(data) {
        max(
            data.maxOfOrNull { it.x } ?: 0f,
            max(
                data.maxOfOrNull { it.y } ?: 0f,
                data.maxOfOrNull { it.z } ?: 0f
            )
        )
    }

    Column(modifier = modifier) {
        if (showLegend) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("X", Color.Red)
                LegendItem("Y", Color.Green)
                LegendItem("Z", Color.Blue)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black.copy(alpha = 0.05f))
        ) {
            val width = size.width
            val height = size.height
            val stepX = width / (data.size - 1).coerceAtLeast(1)

            // Helper to map value to Y coordinate
            fun mapY(value: Float): Float {
                // Scale so maxVal is at top, 0 is at bottom
                // Avoid division by zero
                val safeMax = if (maxVal == 0f) 1f else maxVal
                val normalized = value / safeMax
                return height - (normalized * height)
            }

            // Draw grid lines (horizontal) - e.g. 0%, 50%, 100%
            drawLine(
                Color.Gray.copy(alpha = 0.3f),
                start = Offset(0f, height),
                end = Offset(width, height),
                strokeWidth = 1f
            )
            drawLine(
                Color.Gray.copy(alpha = 0.3f),
                start = Offset(0f, height / 2),
                end = Offset(width, height / 2),
                strokeWidth = 1f
            )
            drawLine(
                Color.Gray.copy(alpha = 0.3f),
                start = Offset(0f, 0f),
                end = Offset(width, 0f),
                strokeWidth = 1f
            )

            // Draw Paths
            val pathX = Path()
            val pathY = Path()
            val pathZ = Path()

            data.forEachIndexed { index, item ->
                val x = index * stepX
                val yX = mapY(item.x)
                val yY = mapY(item.y)
                val yZ = mapY(item.z)

                if (index == 0) {
                    pathX.moveTo(x, yX)
                    pathY.moveTo(x, yY)
                    pathZ.moveTo(x, yZ)
                } else {
                    pathX.lineTo(x, yX)
                    pathY.lineTo(x, yY)
                    pathZ.lineTo(x, yZ)
                }
            }

            drawPath(pathX, Color.Red, style = Stroke(width = 2.dp.toPx()))
            drawPath(pathY, Color.Green, style = Stroke(width = 2.dp.toPx()))
            drawPath(pathZ, Color.Blue, style = Stroke(width = 2.dp.toPx()))
        }
        
        Text(
            text = "Max: ${String.format("%.2f", maxVal)}",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
        )
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = RoundedCornerShape(4.dp))
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

fun computeAverageSample(samples: List<List<AccelerometerChange>>): List<AccelerometerChange> {
    if (samples.isEmpty()) return emptyList()

    // Find the max length to average up to that point, or min length?
    // "Typical Minute" -> usually fixed length samples. 
    // Let's use the average length or max length. 
    // If samples have different lengths, we might have trailing zeros or cut off.
    // For simplicity, let's take the max length and average available points.
    
    val maxLength = samples.maxOf { it.size }
    val result = ArrayList<AccelerometerChange>(maxLength)

    for (i in 0 until maxLength) {
        var sumX = 0f
        var sumY = 0f
        var sumZ = 0f
        var count = 0

        for (sample in samples) {
            if (i < sample.size) {
                sumX += sample[i].x
                sumY += sample[i].y
                sumZ += sample[i].z
                count++
            }
        }

        if (count > 0) {
            result.add(AccelerometerChange(sumX / count, sumY / count, sumZ / count))
        }
    }

    return result
}

fun parseAccelerometerData(data: String): List<List<AccelerometerChange>> {
    val samples = mutableListOf<List<AccelerometerChange>>()
    var currentSample = mutableListOf<AccelerometerChange>()

    data.lines().forEach { line ->
        if (line.trim() == "===") {
            if (currentSample.isNotEmpty()) {
                samples.add(currentSample)
                currentSample = mutableListOf()
            }
        } else if (line.contains(",")) {
            // Check if it's a valid data line (numbers)
            try {
                // Assuming AccelerometerChange.fromString(line) works or manual parse
                // The format is x,y,z
                val parts = line.split(",")
                if (parts.size >= 3) {
                    val x = parts[0].toFloat()
                    val y = parts[1].toFloat()
                    val z = parts[2].toFloat()
                    currentSample.add(AccelerometerChange(x, y, z))
                }
            } catch (e: Exception) {
                // Ignore invalid lines (like Transport Mode tags at the end)
            }
        }
    }
    
    if (currentSample.isNotEmpty()) {
        samples.add(currentSample)
    }

    return samples
}

fun parseAccelerometerFile(file: File): List<List<AccelerometerChange>> {
    if (!file.exists()) return emptyList()
    
    val content = file.readText()
    
    return parseAccelerometerData(content)
}
