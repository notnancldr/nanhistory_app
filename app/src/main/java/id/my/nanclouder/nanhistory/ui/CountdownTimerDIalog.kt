package id.my.nanclouder.nanhistory.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZonedDateTime

@Composable
fun CountdownTimerDialog(
    eventStart: ZonedDateTime,
    eventEnd: ZonedDateTime? = null,
    showDialog: Boolean,
    onDismiss: () -> Unit
) {
    var startTime by remember { mutableStateOf("00:00:00") }
    var startTimeFormatted by remember { mutableStateOf("0h 0m 0s") }
    var endTime by remember { mutableStateOf("00:00:00") }
    var endTimeFormatted by remember { mutableStateOf("0h 0m 0s") }
    var startMode by remember { mutableStateOf("countdown") } // auto-determined
    var endMode by remember { mutableStateOf("countdown") } // auto-determined
    var showStartTimer by remember { mutableStateOf(true) }
    var limitToHours by remember { mutableStateOf(true) }

    LaunchedEffect(showDialog, eventStart, eventEnd, limitToHours) {
        if (!showDialog) return@LaunchedEffect

        while (true) {
            val now = ZonedDateTime.now()

            // Start time: countdown if before start, count up if after start
            val timeFromStart = Duration.between(eventStart, now).seconds
            val startCountSign = if (timeFromStart < 0) "-" else if (timeFromStart > 0) "+" else ""
            startMode = if (timeFromStart < 0) "countdown" else "countup"
            startTime = startCountSign + formatDuration(kotlin.math.abs(timeFromStart), true)
            startTimeFormatted = startCountSign + formatDuration(kotlin.math.abs(timeFromStart), false)

            // End time (if available): countdown if before end, count up if after end
            if (eventEnd != null) {
                val timeUntilEnd = Duration.between(now, eventEnd).seconds
                val endCountSign = if (timeUntilEnd > 0) "-" else if (timeUntilEnd < 0) "+" else ""
                endMode = if (timeUntilEnd > 0) "countdown" else "countup"
                endTime = endCountSign + formatDuration(kotlin.math.abs(timeUntilEnd), true)
                endTimeFormatted = endCountSign + formatDuration(kotlin.math.abs(timeUntilEnd), false)
            }

            delay(1000)
        }
    }

    if (showDialog) {
        AlertDialog(
            icon = {
                Icon(Icons.Rounded.Schedule, "Timer")
            },
            onDismissRequest = onDismiss,
            title = {
                Text("Event Timer")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp)
                ) {
                    // Timer selector buttons
                    if (eventEnd != null) Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { showStartTimer = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showStartTimer) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                                contentColor = if (showStartTimer) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Start", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = { showStartTimer = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!showStartTimer) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                                contentColor = if (!showStartTimer) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("End", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // Time limit selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { limitToHours = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (limitToHours) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    Color.Transparent
                                },
                                contentColor = if (limitToHours) {
                                    MaterialTheme.colorScheme.onTertiary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Hours", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = { limitToHours = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!limitToHours) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    Color.Transparent
                                },
                                contentColor = if (!limitToHours) {
                                    MaterialTheme.colorScheme.onTertiary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Full", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Timer display
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (showStartTimer) "Event Start" else "Event End",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (limitToHours) {
                            Text(
                                if (showStartTimer) startTime else endTime,
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            FormattedDurationDisplay(
                                if (showStartTimer) startTimeFormatted else endTimeFormatted
                            )
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun FormattedDurationDisplay(durationText: String) {
    FlowRow(
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center
    ) {
        val regex = Regex("([-+]*\\d+)([dhms])")
        val matches = regex.findAll(durationText)

        matches.forEachIndexed { index, match ->
            val number = match.groupValues[1]
            val unit = match.groupValues[2]

            Row {
                Text(
                    number,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alignByBaseline()
                )
                Text(
                    unit,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp, end = 8.dp).alignByBaseline()
                )
            }
        }
    }
}

@Composable
private fun PulsingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "scaleAnimation"
    )

    Row(
        modifier = Modifier
            .size(12.dp * scale)
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.error),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {}
}

private fun formatDuration(seconds: Long, limitToHours: Boolean): String {
    val totalSeconds = kotlin.math.abs(seconds)

    return if (limitToHours) {
        // Only show HH:MM:SS, cap hours if needed
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60
        val pad = { num: Long -> num.toString().padStart(2, '0') }
        "${pad(hours)}:${pad(minutes)}:${pad(secs)}"
    } else {
        // Full format with days: "Xd Xh Xm Xs"
        val days = totalSeconds / 86400
        val hours = (totalSeconds % 86400) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60

        buildString {
            if (days > 0) append("${days}d ")
            append("${hours}h ")
            append("${minutes}m ")
            append("${secs}s")
        }
    }
}