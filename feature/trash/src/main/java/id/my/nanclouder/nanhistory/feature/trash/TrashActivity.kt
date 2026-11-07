package id.my.nanclouder.nanhistory.feature.trash

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import id.my.nanclouder.nanhistory.core.common.di.CoreContainerProvider
import id.my.nanclouder.nanhistory.core.common.di.TrashContainer
import id.my.nanclouder.nanhistory.core.common.utils.getActivity
import id.my.nanclouder.nanhistory.core.ui.R
import id.my.nanclouder.nanhistory.core.ui.list.EventListViewModel
import id.my.nanclouder.nanhistory.core.ui.selection.SelectionAppBar
import id.my.nanclouder.nanhistory.core.ui.selection.rememberSelectionState
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import id.my.nanclouder.nanhistory.domain.model.EventSelectMode
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.core.ui.list.SimpleEventList

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
    val eventListViewModel = EventListViewModel(
        context = context,
        mode = EventSelectMode.Deleted,
    )
    val eventList by eventListViewModel.events.collectAsState(emptyList())

    val container = (context.applicationContext as CoreContainerProvider).coreContainer as TrashContainer
    val trashViewModel: TrashViewModel = remember(context) {
        (context as? ComponentActivity)?.let { activity ->
            ViewModelProvider(activity, TrashViewModel.Factory(container))[TrashViewModel::class.java]
        } ?: TrashViewModel(container.restoreEventsUseCase, container.permanentlyDeleteEventsUseCase)
    }

    val deleteProgress by trashViewModel.deleteProgress.collectAsState()
    val restoreProgress by trashViewModel.restoreProgress.collectAsState()

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
                                    deleteDialogProgress = null
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
            SimpleEventList(
                events = eventList,
                selectionState = selectionState,
                lazyListState = listState,
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
                if (!deleteDialogLock && !deleteProgress.isRunning)
                    deleteDialogState = false
            },
            title = {
                Text(deleteDialogTitle)
            },
            text = {
                val currentText = if (deleteProgress.isRunning) deleteProgress.text else deleteDialogText
                val progress = if (deleteProgress.isRunning) deleteProgress.progress else deleteDialogProgress
                Column {
                    Text(currentText)
                    if (progress != null) LinearProgressIndicator(
                        progress = { progress }
                    )
                }
            },
            dismissButton = {
                if (!deleteDialogLock && !deleteProgress.isRunning) TextButton(
                    onClick = {
                        deleteDialogState = false
                    },
                ) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                if (!deleteDialogLock && !deleteProgress.isRunning) Button(
                    onClick = {
                        deleteDialogLock = true
                        trashViewModel.deletePermanently(selectedItems) {
                            deleteDialogState = false
                            deleteDialogLock = false
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
                if (!restoreDialogLock && !restoreProgress.isRunning)
                    restoreDialogState = false
            },
            title = {
                Text(restoreDialogTitle)
            },
            text = {
                val currentText = if (restoreProgress.isRunning) restoreProgress.text else restoreDialogText
                Text(currentText)
            },
            dismissButton = {
                if (!restoreDialogLock && !restoreProgress.isRunning) TextButton(
                    onClick = {
                        restoreDialogState = false
                    },
                ) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                if (!restoreDialogLock && !restoreProgress.isRunning) Button(
                    onClick = {
                        restoreDialogLock = true
                        trashViewModel.restore(selectedItems) {
                            restoreDialogState = false
                            restoreDialogLock = false
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
