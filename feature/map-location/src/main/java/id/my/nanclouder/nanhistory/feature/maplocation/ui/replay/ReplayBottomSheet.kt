package id.my.nanclouder.nanhistory.feature.maplocation.ui.replay

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.core.common.time.DateFormatter
import id.my.nanclouder.nanhistory.domain.logic.ReplayInterpolator
import id.my.nanclouder.nanhistory.feature.maplocation.R
import id.my.nanclouder.nanhistory.feature.maplocation.ReplayUiState
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import id.my.nanclouder.nanhistory.core.common.time.TimeFormatterWithSecond
import java.time.ZonedDateTime
import kotlin.math.roundToInt

/**
 * Bottom control card containing the scrubber, playback controls, and discrete speed slider.
 */
@Composable
fun ReplayBottomSheet(
    replayState: ReplayUiState,
    onTogglePlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onStepTime: (Long) -> Unit,
    onStepWaypoint: (Int) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onToggleFollowCamera: () -> Unit,
    onToggleImmersive: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = ReplayInterpolator.SPEED_PRESETS
    val currentSpeedIdx = presets.indexOf(replayState.speedMultiplier).takeIf { it >= 0 } ?: 2

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Header: Time + Date Display & Utility Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val virtualTime = replayState.currentVirtualTime
                    if (virtualTime != null) {
                        Text(
                            text = virtualTime.format(TimeFormatterWithSecond),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = virtualTime.format(DateFormatter),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.replay_mode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Follow Camera Toggle
                    IconButton(
                        onClick = onToggleFollowCamera,
                        colors = if (replayState.isFollowCamera) {
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    ) {
                        Icon(
                            Icons.Rounded.MyLocation,
                            contentDescription = if (replayState.isFollowCamera) {
                                stringResource(R.string.replay_follow_camera)
                            } else {
                                stringResource(R.string.replay_free_camera)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Immersive Fullscreen Toggle
                    IconButton(onClick = onToggleImmersive) {
                        Icon(
                            imageVector = if (replayState.isImmersive) {
                                Icons.Rounded.FullscreenExit
                            } else {
                                Icons.Rounded.Fullscreen
                            },
                            contentDescription = stringResource(R.string.replay_immersive),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Close Replay Button
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.replay_exit),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Scrubber Slider
            Slider(
                value = replayState.progress,
                onValueChange = onSeek,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Rewind 10s
                IconButton(onClick = { onStepTime(-10) }) {
                    Icon(
                        Icons.Rounded.FastRewind,
                        contentDescription = stringResource(R.string.replay_step_back_10),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Prev Waypoint
                IconButton(onClick = { onStepWaypoint(-1) }) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = stringResource(R.string.replay_prev_point),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Play / Pause FAB Button
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (replayState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (replayState.isPlaying) {
                            stringResource(R.string.replay_pause)
                        } else {
                            stringResource(R.string.replay_play)
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Next Waypoint
                IconButton(onClick = { onStepWaypoint(1) }) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.replay_next_point),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Fast Forward 10s
                IconButton(onClick = { onStepTime(10) }) {
                    Icon(
                        Icons.Rounded.FastForward,
                        contentDescription = stringResource(R.string.replay_step_forward_10),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Discrete Speed Slider Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val speedText = if (replayState.speedMultiplier >= 1f) {
                    replayState.speedMultiplier.toInt().toString()
                } else {
                    replayState.speedMultiplier.toString()
                }
                Text(
                    text = stringResource(R.string.replay_speed_label, speedText),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(80.dp)
                )

                Slider(
                    value = currentSpeedIdx.toFloat(),
                    onValueChange = { floatVal ->
                        val idx = floatVal.roundToInt().coerceIn(0, presets.size - 1)
                        onSpeedChanged(presets[idx])
                    },
                    valueRange = 0f..(presets.size - 1).toFloat(),
                    steps = presets.size - 2,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ReplayBottomSheetPreview() {
    NanHistoryTheme {
        ReplayBottomSheet(
            replayState = ReplayUiState(
                isActive = true,
                isPlaying = true,
                progress = 0.35f,
                currentVirtualTime = ZonedDateTime.now(),
                speedMultiplier = 2.0f,
                isFollowCamera = true
            ),
            onTogglePlayPause = {},
            onSeek = {},
            onStepTime = {},
            onStepWaypoint = {},
            onSpeedChanged = {},
            onToggleFollowCamera = {},
            onToggleImmersive = {},
            onClose = {}
        )
    }
}
