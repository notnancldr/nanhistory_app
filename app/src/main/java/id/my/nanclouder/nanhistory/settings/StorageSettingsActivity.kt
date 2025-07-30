package id.my.nanclouder.nanhistory.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.EventDetailActivity
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.debug.FilePlayerActivity
import id.my.nanclouder.nanhistory.debug.FilePreviewActivity
import id.my.nanclouder.nanhistory.getActivity
import id.my.nanclouder.nanhistory.lib.getAttachmentPath
import id.my.nanclouder.nanhistory.lib.getAudioFiles
import id.my.nanclouder.nanhistory.lib.getLocationFiles
import id.my.nanclouder.nanhistory.lib.readableSize
import id.my.nanclouder.nanhistory.lib.searchEventsByAudioPath
import id.my.nanclouder.nanhistory.lib.searchEventsByLocationPath
import id.my.nanclouder.nanhistory.ui.ComponentPlaceholder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class StorageSettingsActivity : SubSettingsActivity("Storage") {
    @Composable
    override fun ColumnScope.Content() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val eventsColor = Color(0xFF7FB86A)
        val logsColor = Color(0xFF24AD98)
        val cacheColor = Color(0xFF558EAD)
        val audioColor = Color(0xFF5960A2)
        val otherColor = Color(0xFF906DB3)

        val restColor = Color.Gray

        var dbSize by rememberSaveable { mutableLongStateOf(0L) }
        var logsSize by rememberSaveable { mutableLongStateOf(0L) }
        var cacheSize by rememberSaveable { mutableLongStateOf(0L) }
        var audioSize by rememberSaveable { mutableLongStateOf(0L) }
        var otherSize by rememberSaveable { mutableLongStateOf(0L) }
        var dataSize by rememberSaveable { mutableLongStateOf(0L) }

        var isLoading by rememberSaveable { mutableStateOf(true) }

        var deleteDialogState by remember { mutableStateOf(false) }
        var fileToDelete by remember { mutableStateOf<File?>(null) }

        val cacheDirs = remember { mutableStateMapOf<File, Long>() }
        val dataDirs = remember { mutableStateMapOf<File, Long>() }

        val lazyListState = rememberLazyListState()

        val getSize = { file: File ->
            file.walk().filter { it.isFile }.map { it.length() }.sum()
        }

        var unlinkedItemsState by remember { mutableStateOf<List<File>?>(null) }
        var scanningUnlinked by remember { mutableStateOf(false) }

        val unlinkedItems = unlinkedItemsState

        val loader = suspend {
            withContext(Dispatchers.IO) {
                dbSize = getSize(File(dataDir, "databases"))
                logsSize = getSize(File(filesDir, "logs"))
                cacheSize = getSize(cacheDir)
                audioSize = getSize(File(filesDir, "audio"))
                dataSize = getSize(dataDir)
                otherSize = dataSize - (dbSize + logsSize + cacheSize + audioSize)

                cacheDirs.clear()
                dataDirs.clear()

                filesDir.listFiles()?.forEach {
                    dataDirs[it] = it.walk()
                        .filter { file -> file.isFile }
                        .map { file -> file.length() }.sum()
                }
                cacheDir.listFiles()?.forEach {
                    cacheDirs[it] = it.walk()
                        .filter { file -> file.isFile }
                        .map { file -> file.length() }.sum()
                }

                isLoading = false
            }
        }

        LaunchedEffect(Unit) {
            loader()
        }

        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(32.dp)
                )
            }
        } else {
            val rowLabel =
                @Composable { color: Color, label: String, size: Float ->
                    ListItem(
                        modifier = Modifier,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.ic_circle_filled),
                                "Label color",
                                tint = color
                            )
                        },
                        headlineContent = {
                            Text(label)
                        },
                        trailingContent = {
                            Text(readableSize(size), Modifier.width(56.dp), textAlign = TextAlign.End)
                        },
                        supportingContent = {
                            LinearProgressIndicator(
                                progress = { size / dataSize }
                            )
                        }
                    )
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        modifier = Modifier
//                            .padding(8.dp)
//                    ) {
//                        Box(
//                            modifier = Modifier
//                                .background(color = color)
//                                .size(24.dp)
//                        )
//                        Text(label, modifier = Modifier.padding(start = 8.dp))
//                    }
                }
            LazyColumn(
                state = lazyListState
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BarChart(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(16.dp),
                                segments = listOf(
                                    dbSize.toFloat() to eventsColor,
                                    logsSize.toFloat() to logsColor,
                                    cacheSize.toFloat() to cacheColor,
                                    audioSize.toFloat() to audioColor,
                                    otherSize.toFloat() to otherColor
                                )
                            )
                            Box(Modifier.width(16.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Used storage", fontWeight = FontWeight.Medium)
                                Text(
                                    readableSize(getSize(dataDir)),
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column(Modifier
                            .padding(16.dp)
                            .clip(RoundedCornerShape(16.dp))) {
                            rowLabel(eventsColor, "Database", dbSize.toFloat())
                            rowLabel(logsColor, "Logs data", logsSize.toFloat())
                            rowLabel(cacheColor, "Cache", cacheSize.toFloat())
                            rowLabel(audioColor, "Audio", audioSize.toFloat())
                            rowLabel(otherColor, "Other", otherSize.toFloat())
                        }
                    }
                }
//                item {
//                    ListItem(
//                        headlineContent = {
//                            Text("cache (${cacheDirs.size})", fontWeight = FontWeight.Medium)
//                        }
//                    )
//                }
//                itemsIndexed(
//                    cacheDirs.keys.sortedByDescending { cacheDirs[it] }.toList(),
//                    key = { _, item -> item.name + item.lastModified() + item.hashCode() }
//                ) { _, item ->
//                    val size = cacheDirs[item]!!
//                    ListItem(
//                        modifier = Modifier
//                            .animateItem(),
//                        leadingContent = {
//                            Icon(painterResource(R.drawable.ic_folder), "Folder")
//                        },
//                        headlineContent = {
//                            Text(item.name)
//                        },
//                        supportingContent = {
//                            Text(readableSize(size.toFloat()))
//                        },
//                        trailingContent = {
//                            IconButton(
//                                onClick = {
//                                    fileToDelete = item
//                                    deleteDialogState = true
//                                }
//                            ) {
//                                Icon(
//                                    painterResource(R.drawable.ic_delete),
//                                    "Delete",
//                                    tint = Color.Red
//                                )
//                            }
//                        }
//                    )
//                }
                val cacheAvailable = cacheDir.listFiles()?.isNotEmpty() == true
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    cacheDir.deleteRecursively()
                                }
                                loader()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        enabled = cacheAvailable
                    ) {
                        Text("Clear Cache")
                    }
                }

                item {
                    Button(
                        onClick = {
                            scanningUnlinked = true
                            unlinkedItemsState = null
                            scope.launch(Dispatchers.IO) {
                                unlinkedItemsState = scanForUnlinkedFiles()
                                scanningUnlinked = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        enabled = !scanningUnlinked
                    ) {
                        Text("Scan for unlinked files")
                    }
                }

                if (unlinkedItems != null || scanningUnlinked) item {
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
                        if (unlinkedItems?.isNotEmpty() == true) {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        "${unlinkedItems.size} unlinked file${if (unlinkedItems.size > 1) "s" else ""} found",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                ),
                                trailingContent = {
                                    Button(
                                        onClick = {
                                            scope.launch(Dispatchers.IO) {
                                                unlinkedItems.forEach {
                                                    it.delete()
                                                    unlinkedItemsState = (unlinkedItemsState ?: emptyList()) - it
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        )
                                    ) {
                                        Text("Delete All")
                                    }
                                }
                            )
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .height(256.dp)
                                    .verticalScroll(rememberScrollState())
                                    .background(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                unlinkedItems.forEach { file ->
                                    ListItem(
                                        headlineContent = {
                                            Text(file.name)
                                        },
                                        supportingContent = {
                                            Text("${file.parentFile?.parentFile?.name}/${file.parentFile?.name}")
                                        },
                                        trailingContent = {
                                            Text(readableSize(file.length()))
                                        },
                                        modifier = if (Config.developerModeEnabled.get(context)) Modifier.clickable {
                                            val audioExtensions = listOf(
                                                "m4a", "mp3", "aac", "3gp"
                                            )
                                            if (file.extension in audioExtensions) {
                                                val intent = Intent(
                                                    context,
                                                    FilePlayerActivity::class.java
                                                )
                                                intent.putExtra("path", file.absolutePath)
                                                context.startActivity(intent)
                                            } else {
                                                val intent = Intent(
                                                    context,
                                                    FilePreviewActivity::class.java
                                                )
                                                intent.putExtra("path", file.absolutePath)
                                                context.startActivity(intent)
                                            }
                                        } else Modifier,
                                        colors = ListItemDefaults.colors(
                                            containerColor = Color.Transparent
                                        )
                                    )
                                }
                            }
                        } else if (unlinkedItems?.isEmpty() == true) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("No unlinked events found")
                            }
                        } else {
                            ComponentPlaceholder(Modifier.size(128.dp, 24.dp))
                        }
                    }
                }

//                item {
//                    ListItem(
//                        headlineContent = {
//                            Text("files (${dataDirs.size})", fontWeight = FontWeight.Medium)
//                        }
//                    )
//                }
//                itemsIndexed(
//                    dataDirs.keys.sortedByDescending { dataDirs[it] }.toList(),
//                    key = { _, item -> item.name + item.lastModified() + item.hashCode() }
//                ) { _, item ->
//                    val size = dataDirs[item]!!
//                    val notDeletable = listOf(
//                        "history",
//                        "logs",
//                        "config",
//                        "audio",
//                        "locations",
//                        "migration_code"
//                    )
//
//                    ListItem(
//                        modifier = Modifier
//                            .animateItem(),
//                        leadingContent = {
//                            Icon(painterResource(R.drawable.ic_folder), "Folder")
//                        },
//                        headlineContent = {
//                            Text(item.name)
//                        },
//                        supportingContent = {
//                            Text(readableSize(size.toFloat()))
//                        },
//                        trailingContent = {
//                            if (!notDeletable.contains(item.name)) {
//                                IconButton(
//                                    onClick = {
//                                        fileToDelete = item
//                                        deleteDialogState = true
//                                    }
//                                ) {
//                                    Icon(
//                                        painterResource(R.drawable.ic_delete),
//                                        "Delete",
//                                        tint = Color.Red
//                                    )
//                                }
//                            }
//                        }
//                    )
//                }
            }
            if (deleteDialogState) {
                AlertDialog(
                    icon = {
                        Icon(Icons.Rounded.Warning, "Warning")
                    },
                    onDismissRequest = {
                        deleteDialogState = false
                    },
                    title = {
                        Text("Delete folder")
                    },
                    text = {
                        Text("This actions is irreversible!")
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                deleteDialogState = false
                            }
                        ) {
                            Text("Cancel")
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        fileToDelete?.deleteRecursively()
                                    }
                                    deleteDialogState = false
                                    loader()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF910C0C),
                                contentColor = Color.White,
                                disabledContainerColor = Color.Gray,
                                disabledContentColor = Color.White
                            ),
                        ) {
                            Text("Delete")
                        }
                    }
                )
            }
        }
    }
    private suspend fun scanForUnlinkedFiles(): List<File> {
        val files = mutableListOf<File>()

        val audioFiles = getAudioFiles(applicationContext)
        val locationFiles = getLocationFiles(applicationContext)
        withContext(Dispatchers.IO) {
            for (file in audioFiles) {
                if (!file.exists()) continue
                val events = searchEventsByAudioPath(
                    applicationContext,
                    getAttachmentPath(file)
                )
                if (events.isEmpty()) files.add(file)
            }

            for (file in locationFiles) {
                if (!file.exists()) continue
                val events = searchEventsByLocationPath(
                    applicationContext,
                    getAttachmentPath(file)
                )
                if (events.isEmpty()) files.add(file)
            }
        }
        return files
    }
}
