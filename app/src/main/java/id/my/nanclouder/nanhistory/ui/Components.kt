package id.my.nanclouder.nanhistory.ui

import android.media.MediaPlayer
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.lib.readableTimeHours
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException
import java.time.Duration

@Composable
fun ToggleButton(
    onClick: () -> Unit,
    active: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val buttonPadding = PaddingValues(horizontal = 8.dp)
    if (active) Button(
        onClick = onClick,
        content = content,
        contentPadding = buttonPadding
    )
    else OutlinedButton(
        onClick = onClick,
        content = content,
        contentPadding = buttonPadding
    )
}

@Composable
fun RequestMultiplePermissions(
    permissions: List<String>,
    onPermissionsResult: (Map<String, Boolean>) -> Unit
) {
    Log.d("NanHistoryDebug", "Requesting permissions... $permissions")
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result: Map<String, Boolean> ->
        Log.d("NanHistoryDebug", "Result permission request $result")
        onPermissionsResult(result)
    }

    LaunchedEffect(Unit) {
        delay(500L)
        launcher.launch(permissions.toTypedArray())
    }
}

@Composable
fun AudioPlayer(path: String) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) } // progress: 0.0 to 1.0
    var recordDuration by remember { mutableIntStateOf(0) }
    var userIsSeeking by remember { mutableStateOf(false) }

    var audioAvailable by remember { mutableStateOf(true) }

    val context = LocalContext.current

    // Create MediaPlayer instance with the file path
    val mediaPlayer = remember {
        if (android.os.Build.VERSION.SDK_INT >= 34) MediaPlayer(context).apply {
            try {
                setDataSource(path)
                prepare()
            }
            catch (e: IOException) {
                audioAvailable = false
//                path = null
            }
        }
        else {
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
            delay(100L) // Update every half second
        }
    }

    DisposableEffect(Unit) {
        mediaPlayer?.setOnCompletionListener {
            isPlaying = false // Update button when playback ends
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
                colors = SliderDefaults.colors(
                    inactiveTrackColor = Color.Gray
                ),
                valueRange = 0f..recordDuration.toFloat(),
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
        },
        leadingContent = {
            IconButton(
                onClick = { isPlaying = !isPlaying }
            ) {
                if (!isPlaying) Icon(painterResource(R.drawable.ic_play_arrow_filled), "Play")
                else Icon(painterResource(R.drawable.ic_pause_filled), "Pause")
            }
        },
        trailingContent = {
            val progressSeconds = Duration.ofMillis(progress.toLong())
            val durationSeconds = Duration.ofMillis(recordDuration.toLong())
            Text("${readableTimeHours(progressSeconds)}/${readableTimeHours(durationSeconds)}")
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier
            .padding(8.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(8.dp))
    )
    else ListItem(
        headlineContent = {
            if (file.isDirectory) Text("Audio file is invalid (is directory)")
            else Text("Audio file is not found")
        },
        leadingContent = {
            Icon(Icons.Rounded.Warning, "Error")
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier
            .padding(8.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(8.dp))
    )
}