package id.my.nanclouder.nanhistory.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.lib.history.HistoryEvent
import id.my.nanclouder.nanhistory.lib.history.HistoryFileData
import id.my.nanclouder.nanhistory.lib.history.delete
import id.my.nanclouder.nanhistory.lib.history.getList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.round

class StorageSettingsActivity : SubSettingsActivity("Storage") {
    @Composable
    override fun ColumnScope.Content() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val eventsColor = Color(0xFF24A6C2)
        val logsColor = Color(0xFF4E9752)
        val otherColor = Color(0xFF5960A2)
        val restColor = Color.Gray

        val filesDir = context.filesDir

        var eventsSize by rememberSaveable { mutableLongStateOf(0L) }
        var logsSize by rememberSaveable { mutableLongStateOf(0L) }
        var otherSize by rememberSaveable { mutableLongStateOf(0L) }

        var isLoading by rememberSaveable { mutableStateOf(true) }

        var deleteDialogState by remember { mutableStateOf(false) }
        var fileToDelete by remember { mutableStateOf<File?>(null) }

        val dirs = remember { mutableStateMapOf<File, Long>() }

        val lazyListState = rememberLazyListState()

        val getSize = { file: File ->
            file.walk().filter { it.isFile }.map { it.length() }.sum()
        }

        val loader = suspend {
            withContext(Dispatchers.IO) {
                eventsSize = getSize(File(filesDir, "history"))
                logsSize = getSize(File(filesDir, "logs"))
                otherSize = getSize(filesDir)

                dirs.clear()

                filesDir.listFiles()?.forEach {
                    dirs[it] = it.walk()
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
            val humanReadable = { size: Float ->
                if (size > 1_000_000) "${round(size / 100_000) / 10} MB"
                else if (size > 1_000) "${round(size / 100) / 10} KB"
                else "$size Bytes"
            }
            val rowLabel =
                @Composable { color: Color, label: String, size: Float ->
                    ListItem(
                        modifier = Modifier
                            .height(56.dp),
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
                        supportingContent = {
                            Text(humanReadable(size))
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Box(Modifier.width(32.dp))
                PieChart(
                    modifier = Modifier
                        .size(96.dp)
                        .padding(8.dp),
                    segments = listOf(
                        eventsSize.toFloat() to eventsColor,
                        logsSize.toFloat() to logsColor,
                        otherSize.toFloat() to otherColor
                    )
                )
                Column(Modifier.padding(start = 16.dp)) {
                    rowLabel(eventsColor, "Events", eventsSize.toFloat())
                    rowLabel(logsColor, "Logs data", logsSize.toFloat())
                    rowLabel(otherColor, "Other", otherSize.toFloat())
                }
            }
            LazyColumn(
                state = lazyListState
            ) {
                itemsIndexed(
                    dirs.keys.sortedByDescending { dirs[it] }.toList(),
                    key = { _, item -> item.name + item.lastModified() + item.hashCode() }
                ) { _, item ->
                    val size = dirs[item]!!
                    ListItem(
                        modifier = Modifier
                            .animateItem(),
                        leadingContent = {
                            Icon(painterResource(R.drawable.ic_folder), "Folder")
                        },
                        headlineContent = {
                            Text(item.name)
                        },
                        supportingContent = {
                            Text(humanReadable(size.toFloat()))
                        },
                        trailingContent = {
                            if (!listOf("history", "logs", "config").contains(item.name)) {
                                IconButton(
                                    onClick = {
                                        fileToDelete = item
                                        deleteDialogState = true
                                    }
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_delete),
                                        "Delete",
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                    )
                }
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
}
