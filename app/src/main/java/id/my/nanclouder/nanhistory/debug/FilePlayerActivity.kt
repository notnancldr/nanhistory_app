package id.my.nanclouder.nanhistory.debug

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.EventDetailActivity
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.getActivity
import id.my.nanclouder.nanhistory.utils.getAttachmentPath
import id.my.nanclouder.nanhistory.utils.readableSize
import id.my.nanclouder.nanhistory.ui.AudioPlayer
import id.my.nanclouder.nanhistory.ui.ComponentPlaceholder
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class FilePlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NanHistoryTheme {
                FilePlayer(intent)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePlayer(intent: Intent) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val path = intent.getStringExtra("path") ?: ""

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    val loadError = @Composable {
                        Text("LoadError")
                    }
                    val file = File(path)
                    if (file.exists() && file.isFile) {
                        Text(
                            file.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else loadError()
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            context.getActivity()!!.finish()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(PaddingValues(8.dp))
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            AudioPlayer(path)

            FoundEventList(path)
            FileDetail(path)
        }
    }
}

@Composable
fun FoundEventList(path: String) {
    val context = LocalContext.current

    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

    val file = File(path)
    val audioPath = getAttachmentPath(path)

    val foundEvents by dao.getEventsByAudio(audioPath).collectAsState(null)

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(8.dp)
    ) {
        if (foundEvents?.isNotEmpty() == true) {
            ListItem(
                headlineContent = {
                    Text(
                        "${foundEvents!!.size} event${if (foundEvents!!.size > 1) "s" else ""} using this item",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
            Column(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp))
            ) {
                foundEvents!!.forEach { event ->
                    ListItem(
                        headlineContent = {
                            Text(event.title)
                        },
                        modifier = Modifier.clickable {
                            val detailIntent = Intent(context, EventDetailActivity::class.java)
                            detailIntent.putExtra("eventId", event.id)
                            context.startActivity(detailIntent)
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        } else if (foundEvents?.isEmpty() == true) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("No events found")
                Button(
                    onClick = {
                        file.delete()
                        context.getActivity()?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            }
        } else {
            ComponentPlaceholder(Modifier.size(64.dp, 16.dp))
        }
    }
}

@Composable
fun FileDetail(path: String) {
    val context = LocalContext.current
    val db = AppDatabase.getInstance(context)
    val file = File(path)

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(8.dp)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    "File Detail",
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
        ) {
            ListItem(
                overlineContent = {
                    Text("Name")
                },
                headlineContent = {
                    Text(file.name)
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
            ListItem(
                overlineContent = {
                    Text("Size")
                },
                headlineContent = {
                    Text(readableSize(file.length()))
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
            ListItem(
                overlineContent = {
                    Text("Created (from filename)")
                },
                headlineContent = {
                    val instant = Instant.ofEpochMilli(
                        file.name.substringBefore("-").toLong(16)
                    )
                    val dateTime = LocalDateTime.ofInstant(instant, ZonedDateTime.now().zone)
                    Text(dateTime.format(DateTimeFormatter.ISO_DATE_TIME))
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
            ListItem(
                overlineContent = {
                    Text("Modified")
                },
                headlineContent = {
                    val instant = Instant.ofEpochMilli(file.lastModified())
                    val dateTime = LocalDateTime.ofInstant(instant, ZonedDateTime.now().zone)
                    Text(dateTime.format(DateTimeFormatter.ISO_DATE_TIME))
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun FilePlayerPreview() {
    NanHistoryTheme {
        FilePlayer(Intent())
    }
}