package id.my.nanclouder.nanhistory.activity.eventLocation.ui.replay

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.activity.eventLocation.common.calculateColor
import id.my.nanclouder.nanhistory.state.ReplayTelemetry
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import id.my.nanclouder.nanhistory.utils.Coordinate
import id.my.nanclouder.nanhistory.utils.copyWith
import java.time.ZonedDateTime
import kotlin.math.roundToInt

/**
 * Floating telemetry HUD displayed during route replay.
 */
@Composable
fun ReplayTelemetryHud(
    telemetry: ReplayTelemetry?,
    modifier: Modifier = Modifier
) {
    if (telemetry == null) return
    val darkMode = isSystemInDarkTheme()

    val speedInt = telemetry.speedKmh.roundToInt()
    val speedColor = calculateColor(speedInt).let { color ->
        if (!darkMode) color.copyWith(value = 0.5f, saturation = 1f)
        else color.copyWith(value = 1f, saturation = 0.4f)
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        tonalElevation = 6.dp,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Speed
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Rounded.Speed,
                    contentDescription = stringResource(R.string.property_speed),
                    modifier = Modifier.size(16.dp),
                    tint = speedColor
                )
                Text(
                    text = "${speedInt} km/h",
                    style = MaterialTheme.typography.labelMedium,
                    color = speedColor
                )
            }

            // 2. Altitude
            if (telemetry.altitudeMeters != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painterResource(R.drawable.ic_expand_all),
                        contentDescription = stringResource(R.string.property_altitude),
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${telemetry.altitudeMeters.roundToInt()} m",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3. Distance
            val distanceKm = telemetry.distanceMeters / 1000.0
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Rounded.Straighten,
                    contentDescription = stringResource(R.string.property_distance),
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (distanceKm >= 1.0) {
                        String.format("%.2f km", distanceKm)
                    } else {
                        "${telemetry.distanceMeters.roundToInt()} m"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 4. Accuracy
            if (telemetry.accuracyMeters != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Rounded.GpsFixed,
                        contentDescription = stringResource(R.string.property_accuracy),
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "±${telemetry.accuracyMeters.roundToInt()}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ReplayTelemetryHudPreview() {
    NanHistoryTheme {
        ReplayTelemetryHud(
            telemetry = ReplayTelemetry(
                coordinate = Coordinate(-6.2, 106.8),
                timestamp = ZonedDateTime.now(),
                speedKmh = 42.5f,
                altitudeMeters = 158.0,
                accuracyMeters = 8.5f,
                bearingDegrees = 135f,
                distanceMeters = 3450.0
            )
        )
    }
}
