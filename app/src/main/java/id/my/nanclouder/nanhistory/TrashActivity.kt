package id.my.nanclouder.nanhistory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import id.my.nanclouder.nanhistory.utils.history.safeDelete
import id.my.nanclouder.nanhistory.state.rememberSelectionState
import id.my.nanclouder.nanhistory.ui.EventList
import id.my.nanclouder.nanhistory.ui.SelectionAppBar
import id.my.nanclouder.nanhistory.ui.main.EventListViewModel
import id.my.nanclouder.nanhistory.ui.main.EventSelectMode
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            NanHistoryTheme {
                TrashView()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashView() {
    val context = LocalContext.current
    val viewModel = EventListViewModel(
        context = context,
        mode = EventSelectMode.Deleted,
    )

    val scope = rememberCoroutineScope()

    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

    val selectionState = rememberSelectionState<HistoryEvent>()
    val selectionMode by selectionState.isSelectionMode.collectAsState()
    val selectedItems by selectionState.selectedItems.collectAsState()

    var deleteDialogState by remember { mutableStateOf(false) }
    var deleteDialogLock by remember { mutableStateOf(false) }
    var deleteDialogTitle by remember { mutableStateOf("") }
    var deleteDialogText by remember { mutableStateOf("") }
    var deleteDialogProgress by remember { mutableStateOf<Float?>(null) }

    var restoreDialogState by remember { mutableStateOf(false) }
    var restoreDialogLock by remember { mutableStateOf(false) }
    var restoreDialogTitle by remember { mutableStateOf("") }
    var restoreDialogText by remember { mutableStateOf("") }
    var restoreDialogProgress by remember { mutableStateOf<Float?>(null) }

    val listState = rememberLazyListState()

    BackHandler(selectionMode) {
        selectionState.reset()
    }

    Scaffold(
        topBar = {
            if (!selectionMode) TopAppBar(
                title = {
                    Text("Trash")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            context.getActivity()?.finish()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                actions = {

                }
            )
            else SelectionAppBar(
                selectedItems = selectedItems.size,
                onCancel = { selectionState.reset() },
                actions = {
                    var dropdownExpanded by remember { mutableStateOf(false) }
                    // TODO: 'delete permanently' button
                    IconButton(
                        onClick = {
                            restoreDialogTitle = "Restore events?"
                            restoreDialogText =
                                "${selectedItems.size} event(s) will be restored"
                            restoreDialogLock = false
                            restoreDialogState = true
                        }
                    ) {
                        Icon(painterResource(R.drawable.ic_undo), "Restore")
                    }
                    // TODO: 'delete permanently' button
                    Box {
                        IconButton(
                            onClick = {
                                dropdownExpanded = !dropdownExpanded
                            }
                        ) {
                            Icon(Icons.Default.MoreVert, "More")
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = {
                                dropdownExpanded = false
                            }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        painterResource(R.drawable.ic_delete), "Delete"
                                    )
                                },
                                text = {
                                    Text("Delete forever")
                                },
                                onClick = {
                                    dropdownExpanded = false
                                    deleteDialogTitle = "Delete forever?"
                                    deleteDialogText =
                                        "${selectedItems.size} event(s) will be deleted FOREVER"
                                    deleteDialogLock = false
                                    deleteDialogState = true
                                },
                                colors = MenuItemColors(
                                    textColor = MaterialTheme.colorScheme.error,
                                    leadingIconColor = MaterialTheme.colorScheme.error,
                                    trailingIconColor = MaterialTheme.colorScheme.error,
                                    disabledTextColor = Color.Gray,
                                    disabledLeadingIconColor = Color.Gray,
                                    disabledTrailingIconColor = Color.Gray
                                )
                            )
                        }
                    }
                }
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            EventList(
                viewModel = viewModel,
                selectionState = selectionState,
                lazyListState = listState,
                loadHeaderData = false,
                topItem = {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            "Every item in Trash will be deleted permanently after 30 days. You can either restore it or let it deleted. You can also delete it permanently now.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        }

        if (deleteDialogState) AlertDialog(
            onDismissRequest = {
                if (!deleteDialogLock)
                    deleteDialogState = false
            },
            title = {
                Text(deleteDialogTitle)
            },
            text = {
                val progress = deleteDialogProgress
                Column {
                    Text(deleteDialogText)
                    if (progress != null) LinearProgressIndicator(
                        progress = { progress }
                    )
                }
            },
            dismissButton = {
                if (!deleteDialogLock) TextButton(
                    onClick = {
                        deleteDialogState = false
                    },
                ) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                if (!deleteDialogLock) Button(
                    onClick = {
                        deleteDialogLock = true
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                for ((index, selectedItem) in selectedItems.withIndex()) {
                                    selectedItem.safeDelete(context)
                                    deleteDialogText =
                                        "Deleting items: ${index + 1} of ${selectedItems.size}"
                                    deleteDialogProgress = (index + 1f) / selectedItems.size
                                }
                            }
                            deleteDialogState = false
                            deleteDialogProgress = null
                            selectionState.reset()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            }
        )
        if (restoreDialogState) AlertDialog(
            onDismissRequest = {
                if (!restoreDialogLock)
                    restoreDialogState = false
            },
            title = {
                Text(restoreDialogTitle)
            },
            text = {
                Text(restoreDialogText)
            },
            dismissButton = {
                if (!restoreDialogLock) TextButton(
                    onClick = {
                        restoreDialogState = false
                    },
                ) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                if (!restoreDialogLock) Button(
                    onClick = {
                        restoreDialogLock = true
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                restoreDialogText = "Restoring events..."
                                dao.restoreEvents(selectedItems.map { it.id })
                            }
                            restoreDialogState = false
                            restoreDialogProgress = null
                            selectionState.reset()
                        }
                    }
                ) {
                    Text("Restore")
                }
            }
        )
    }
}