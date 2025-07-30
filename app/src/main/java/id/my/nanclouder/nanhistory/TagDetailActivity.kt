package id.my.nanclouder.nanhistory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toHistoryTag
import id.my.nanclouder.nanhistory.lib.history.HistoryEvent
import id.my.nanclouder.nanhistory.state.rememberSelectionState
import id.my.nanclouder.nanhistory.ui.ComponentPlaceholder
import id.my.nanclouder.nanhistory.ui.SelectionAppBar
import id.my.nanclouder.nanhistory.ui.main.EventList
import id.my.nanclouder.nanhistory.ui.main.EventListViewModel
import id.my.nanclouder.nanhistory.ui.main.EventSelectMode
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TagDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            NanHistoryTheme {
                TaggedEventList(intent.getStringExtra("tagId") ?: "")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaggedEventList(tagId: String) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

    val viewModel = remember {
        EventListViewModel(
            context = context,
            mode = EventSelectMode.Tagged,
            tagId = tagId
        )
    }

    val tagData by dao.getTagById(tagId)
        .map { it?.toHistoryTag() }.collectAsState(null)

    val selectionState = rememberSelectionState<HistoryEvent>()
    val selectionMode by selectionState.isSelectionMode.collectAsState()
    val selectedItems by selectionState.selectedItems.collectAsState()

    var deleteDialogState by remember { mutableStateOf(false) }
    var deleteDialogLock by remember { mutableStateOf(false) }
    var deleteDialogTitle by remember { mutableStateOf("") }
    var deleteDialogText by remember { mutableStateOf("") }
    var deleteDialogProgress by remember { mutableStateOf<Float?>(null) }

    val listState = rememberLazyListState()

    BackHandler(selectionMode) {
        selectionState.reset()
    }

    Scaffold(
        topBar = {
            if (!selectionMode) TopAppBar(
                title = {
                    if (tagData != null)
                        Text(tagData?.name ?: "")
                    else
                        ComponentPlaceholder(Modifier.size(72.dp, 16.dp))
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
                                    Text("Delete")
                                },
                                onClick = {
                                    dropdownExpanded = false
                                    deleteDialogTitle = "Delete event(s)?"
                                    deleteDialogText =
                                        "${selectedItems.size} event(s) will be moved into deleted events"
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
                loadHeaderData = false
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
                                AppDatabase.moveToTrash(dao, context, selectedItems.map { it.id })
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
    }
}