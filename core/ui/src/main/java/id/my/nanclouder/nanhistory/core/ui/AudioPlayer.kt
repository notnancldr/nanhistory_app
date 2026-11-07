package id.my.nanclouder.nanhistory.core.ui

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.core.common.time.readableTimeHours
import id.my.nanclouder.nanhistory.core.ui.R
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException
import java.time.Duration
import kotlin.math.round

@Composable
fun AudioPlayer(path: String) {
    val useOldUi = Config.appearanceOldUi.getCache()
    if (!useOldUi) AudioPlayer_New(path)
    else AudioPlayer_Old(path)
}

@Composable
fun AudioPlayer_Old(path: String) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var recordDuration by remember { mutableIntStateOf(0) }
    var userIsSeeking by remember { mutableStateOf(false) }
    var audioAvailable by remember { mutableStateOf(true) }

    val context = LocalContext.current

    val mediaPlayer = remember {
        if (android.os.Build.VERSION.SDK_INT >= 34) MediaPlayer(context).apply {
            try {
                setDataSource(path)
                prepare()
            } catch (e: IOException) {
                audioAvailable = false
            }
        } else {
            audioAvailable = false
            null
        }
    }

    LaunchedEffect(Unit) {
        recordDuration = mediaPlayer?.duration ?: 0
    }

    LaunchedEffect(isPlaying, userIsSeeking) {
        if (isPlaying) {
            mediaPlayer?.start()
        } else {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer.pause()
            }
        }
        while (isPlaying && !userIsSeeking) {
            val current = mediaPlayer?.currentPosition
            progress = current ?: 0
            delay(100L)
        }
    }

    DisposableEffect(Unit) {
        mediaPlayer?.setOnCompletionListener {
            isPlaying = false
            progress = 0
        }

        onDispose {
            mediaPlayer?.release()
        }
    }

    val file = File(path)

    if (file.isFile) ListItem(
        headlineContent = {
            Slider(
                value = progress.toFloat(),
                onValueChange = {
                    userIsSeeking = true
                    progress = it.toInt()
                },
                onValueChangeFinished = {
                    mediaPlayer?.seekTo(progress)
                    userIsSeeking = false
                },
                valueRange = 0f..recordDuration.toFloat()
            )
        },
        leadingContent = {
            IconButton(
                onClick = { isPlaying = !isPlaying }
            ) {
                Icon(
                    if (!isPlaying) painterResource(R.drawable.ic_play_arrow_filled)
                    else painterResource(R.drawable.ic_pause_filled),
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }
        },
        supportingContent = {
            val progressSeconds = Duration.ofMillis(progress.toLong())
            val durationSeconds = Duration.ofMillis(recordDuration.toLong())
            Text("${readableTimeHours(progressSeconds)} / ${readableTimeHours(durationSeconds)}")
        }
    )
    else if (!file.exists()) ListItem(
        headlineContent = { Text("Audio file not found at path") },
        leadingContent = { Icon(Icons.Rounded.Warning, "Error") },
        supportingContent = { Text(path) }
    )
    else if (file.isDirectory) ListItem(
        headlineContent = { Text("The audio file is a directory") },
        leadingContent = { Icon(Icons.Rounded.Warning, "Error") },
        supportingContent = { Text(path) }
    )
}

@Composable
fun AudioPlayer_New(path: String) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var recordDuration by remember { mutableIntStateOf(0) }
    var userIsSeeking by remember { mutableStateOf(false) }
    var audioAvailable by remember { mutableStateOf(true) }

    val context = LocalContext.current

    val mediaPlayer = remember {
        if (android.os.Build.VERSION.SDK_INT >= 34) MediaPlayer(context).apply {
            try {
                setDataSource(path)
                prepare()
            } catch (e: IOException) {
                audioAvailable = false
            }
        } else {
            audioAvailable = false
            null
        }
    }

    LaunchedEffect(Unit) {
        recordDuration = mediaPlayer?.duration ?: 0
    }

    LaunchedEffect(isPlaying, userIsSeeking) {
        if (isPlaying) {
            mediaPlayer?.start()
        } else {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer.pause()
            }
        }
        while (isPlaying && !userIsSeeking) {
            val current = mediaPlayer?.currentPosition
            progress = current ?: 0
            delay(100L)
        }
    }

    DisposableEffect(Unit) {
        mediaPlayer?.setOnCompletionListener {
            isPlaying = false
            progress = 0
        }

        onDispose {
            mediaPlayer?.release()
        }
    }

    val file = File(path)
    val progressSeconds = Duration.ofMillis(progress.toLong())
    val durationSeconds = Duration.ofMillis(recordDuration.toLong())
    val progressPercent = if (recordDuration > 0) (progress.toFloat() / recordDuration) * 100 else 0f

    if (file.isFile) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        tonalElevation = 4.dp
                    ) {
                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                if (!isPlaying) painterResource(R.drawable.ic_play_arrow_filled)
                                else painterResource(R.drawable.ic_pause_filled),
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${readableTimeHours(progressSeconds)} / ${readableTimeHours(durationSeconds)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${round(progressPercent)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Slider(
                        value = progress.toFloat(),
                        onValueChange = {
                            userIsSeeking = true
                            progress = it.toInt()
                        },
                        onValueChangeFinished = {
                            mediaPlayer?.seekTo(progress)
                            userIsSeeking = false
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        valueRange = 0f..recordDuration.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .padding(4.dp)
                    )
                }
            }
        }
    } else {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.errorContainer,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    color = MaterialTheme.colorScheme.error
                ) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "Audio file unavailable",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        if (file.isDirectory) "The audio file is a directory"
                        else "Audio file not found at path",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
