package id.my.nanclouder.nanhistory.activity.eventLocation.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.activity.eventLocation.common.DataMode
import id.my.nanclouder.nanhistory.activity.eventLocation.common.calculateColor
import id.my.nanclouder.nanhistory.utils.Coordinate
import id.my.nanclouder.nanhistory.utils.copyWith
import kotlin.math.round
import kotlin.math.roundToInt

@Composable
fun InfoChip(
    icon: Painter,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp), tint = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun SelectedSegmentInfoCard(
    selectedTime: String,
    selectedSpeed: Int,
    selectedAltitude: String?,
    selectedAccuracy: String?,
    location: Coordinate?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    context: Context
) {
    val darkMode = isSystemInDarkTheme()
    val speedColor = calculateColor(selectedSpeed).let { color ->
        if (!darkMode) color.copyWith(value = 0.5f, saturation = 1f)
        else color.copyWith(value = 1f, saturation = 0.4f)
    }.copy(alpha = 0.8f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 4.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val infoScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(infoScrollState),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip(
                    icon = painterResource(R.drawable.ic_schedule),
                    label = selectedTime
                )

                VerticalDivider(
                    modifier = Modifier.height(24.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                InfoChip(
                    icon = painterResource(R.drawable.ic_speed),
                    label = "$selectedSpeed Km/h",
                    color = speedColor
                )

                if (selectedAltitude != null) {
                    VerticalDivider(
                        modifier = Modifier.height(24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    InfoChip(
                        icon = painterResource(R.drawable.ic_expand_all),
                        label = selectedAltitude
                    )
                }

                if (selectedAccuracy != null) {
                    VerticalDivider(
                        modifier = Modifier.height(24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    InfoChip(
                        icon = painterResource(R.drawable.ic_target),
                        label = selectedAccuracy
                    )
                }

                Button(
                    onClick = {
                        val stringLocation = location?.toString()
                        if (stringLocation == null) {
                            Toast.makeText(context, "Selected coordinate unknown", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val gmmIntentUri = "geo:$stringLocation?q=$stringLocation".toUri()
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)
                        } else {
                            Toast.makeText(context, "Google Maps is not installed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        painterResource(R.drawable.ic_location_filled),
                        contentDescription = "Location",
                        modifier = Modifier.size(16.dp).padding(end = 6.dp)
                    )
                    Text("Open", style = MaterialTheme.typography.labelSmall)
                }
            }

            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Rounded.Close,
                    "Close",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EventLocationBottomBar(
    zoomLevel: Double,
    isMaxDetail: Boolean,
    cutMode: Boolean,
    showPoints: Boolean,
    showData: Boolean,
    dataMode: DataMode,
    canUndoCut: Boolean,
    onTogglePoints: () -> Unit,
    onToggleData: () -> Unit,
    onSelectDataMode: (DataMode) -> Unit,
    onUndoCut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomBarScrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_zoom),
                            "Zoom level",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${round(zoomLevel * 10) / 10.0}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = if (isMaxDetail) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .horizontalScroll(bottomBarScrollState)
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!cutMode) {
                        Surface(
                            modifier = Modifier.height(40.dp).clip(RoundedCornerShape(10.dp)),
                            color = if (showPoints) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = if (showPoints) 2.dp else 0.dp
                        ) {
                            Button(
                                onClick = onTogglePoints,
                                modifier = Modifier.height(40.dp).fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = if (showPoints) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_map_touch_point),
                                    "Points",
                                    modifier = Modifier.size(16.dp).padding(end = 6.dp)
                                )
                                Text("Points", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Surface(
                            modifier = Modifier.height(40.dp).clip(RoundedCornerShape(10.dp)),
                            color = if (showData) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = if (showData) 2.dp else 0.dp
                        ) {
                            Row {
                                Button(
                                    onClick = onToggleData,
                                    modifier = Modifier.height(40.dp).fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = if (showData) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_speed),
                                        "Data",
                                        modifier = Modifier.size(16.dp).padding(end = 6.dp)
                                    )
                                    Text("Data", style = MaterialTheme.typography.labelSmall)
                                }

                                AnimatedVisibility(visible = showData) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        DataMode.entries.forEach { mode ->
                                            Surface(
                                                modifier = Modifier.height(32.dp).clip(RoundedCornerShape(8.dp)),
                                                color = if (dataMode == mode) {
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                                } else {
                                                    MaterialTheme.colorScheme.surface
                                                },
                                                tonalElevation = 0.dp
                                            ) {
                                                Button(
                                                    onClick = { onSelectDataMode(mode) },
                                                    modifier = Modifier.height(32.dp).fillMaxWidth(),
                                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color.Transparent,
                                                        contentColor = if (dataMode == mode) {
                                                            MaterialTheme.colorScheme.onPrimaryContainer
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurface
                                                        }
                                                    ),
                                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                                                ) {
                                                    Text(
                                                        mode.name.lowercase().replaceFirstChar { it.uppercaseChar() },
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.height(40.dp).clip(RoundedCornerShape(10.dp)),
                            color = if (canUndoCut) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = if (canUndoCut) 2.dp else 0.dp
                        ) {
                            Button(
                                onClick = onUndoCut,
                                modifier = Modifier.height(40.dp).fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = if (canUndoCut) MaterialTheme.colorScheme.onError
                                    else MaterialTheme.colorScheme.onSurface
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                enabled = canUndoCut
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    "Undo",
                                    modifier = Modifier.size(16.dp).padding(end = 6.dp)
                                )
                                Text("Undo", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
