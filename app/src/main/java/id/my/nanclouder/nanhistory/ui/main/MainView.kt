package id.my.nanclouder.nanhistory.ui.main

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import id.my.nanclouder.nanhistory.EditEventActivity
import id.my.nanclouder.nanhistory.EventDetailActivity
import id.my.nanclouder.nanhistory.NanHistoryPages
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.lib.history.HistoryEvent
import id.my.nanclouder.nanhistory.lib.history.HistoryFileData
import id.my.nanclouder.nanhistory.lib.history.delete
import id.my.nanclouder.nanhistory.lib.history.getFilePathFromDate
import id.my.nanclouder.nanhistory.lib.history.getList
import id.my.nanclouder.nanhistory.lib.history.save
import id.my.nanclouder.nanhistory.service.RecordService
import id.my.nanclouder.nanhistory.ui.SelectionAppBar
import id.my.nanclouder.nanhistory.ui.list.EventListHeader
import id.my.nanclouder.nanhistory.ui.list.EventListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun MainView() {
    var selectedPage by remember { mutableStateOf(NanHistoryPages.Recent) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val sharedPreferences = LocalContext.current.applicationContext
        .getSharedPreferences("recordEvent", Context.MODE_PRIVATE)

    val appBarTitle = when (selectedPage) {
        NanHistoryPages.Recent -> "Recent Events"
        NanHistoryPages.Favorite -> "Favorite Events"
        NanHistoryPages.All -> "All Events"
    }

    var loader by remember { mutableStateOf({ }) }

    val recordPermissionState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            android.Manifest.permission.POST_NOTIFICATIONS
        )
    )

    val context = LocalContext.current

    var isRecording by rememberSaveable { mutableStateOf(false) }
    isRecording = RecordService.isRunning(LocalContext.current)

    var selectionMode by remember { mutableStateOf(false) }
    val selectedItems = remember { mutableStateListOf<String>() }
    var isLoading by rememberSaveable { mutableStateOf(true) }

    var deleteDialogState by remember { mutableStateOf(false) }
    var deleteDialogTitle by remember { mutableStateOf("") }
    var deleteDialogText by remember { mutableStateOf("") }
    var deleteButtonState by remember { mutableStateOf(false) }

    var lock by remember { mutableStateOf(false) }

    val resetSelectionMode = resetSelectionMode@{
        if (lock) return@resetSelectionMode
        selectionMode = false
        selectedItems.clear()
    }

    val openDeleteDialog = {
        deleteDialogTitle =
            "Delete file" + (if (selectedItems.size < 2) "?" else "s?")
        deleteDialogText =
            "Do you want to delete ${selectedItems.size} file${(if (selectedItems.size < 2) "" else "s")}?"
        deleteButtonState = true
        deleteDialogState = true
    }

    val closeDeleteDialog = {
        deleteDialogState = false
        deleteButtonState = false
        deleteDialogTitle = ""
        deleteDialogText = ""
    }

    var fileDataList by remember { mutableStateOf(listOf<HistoryFileData>()) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { DrawerContent() }
    ) {
        Scaffold(
            floatingActionButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            val intent = Intent(context, EditEventActivity::class.java)
                            intent.putExtra("eventId", "")
                            context.startActivity(intent)
                        },
                    ) {
                        Icon(painterResource(R.drawable.ic_add), "Add event button")
                    }
                    FloatingActionButton(
                        onClick = {
                            // TODO
                            val intent = Intent(context, RecordService::class.java)
                            if (isRecording) {
                                context.stopService(intent)
                                scope.launch {
                                    delay(100L)
                                    sharedPreferences.edit().putBoolean("isRunning", false).apply()
                                    isRecording = false
                                    loader()
                                }
                            } else {
                                if (recordPermissionState.allPermissionsGranted) {
                                    context.startForegroundService(intent)
                                    scope.launch {
                                        delay(100L)
                                        loader()
                                    }
                                } else
                                    recordPermissionState.launchMultiplePermissionRequest()

                            }
                            isRecording = !isRecording
                        }
                    ) {
                        if (isRecording)
                            Icon(
                                painterResource(R.drawable.ic_stop_filled),
                                "Record event button",
                                tint = Color(0xFFBB1A1A)
                            )
                        else
                            Icon(
                                painterResource(R.drawable.ic_circle_filled),
                                "Record event button"
                            )
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (selectionMode)
                    SelectionAppBar(selectedItems, { resetSelectionMode() }) {
                        IconButton(onClick = {
                            scope.launch {
                                lock = true
                                withContext(Dispatchers.IO) {
                                    fileDataList.forEach {
                                        it.events
                                            .filter { event -> selectedItems.contains(event.id) }
                                            .forEach { event ->
                                                event.favorite = !event.favorite
                                                event.save(context)
                                            }
                                    }
                                }
                                lock = false
                                resetSelectionMode()
                            }
                        }) {
                            Icon(painterResource(R.drawable.ic_favorite), "Favorite")
                        }
                        IconButton(onClick = { openDeleteDialog() }) {
                            Icon(painterResource(R.drawable.ic_delete), "Delete")
                        }
                    }
                else
                    TopAppBar(
                        title = { Text(appBarTitle) },
                        scrollBehavior = scrollBehavior,
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    scope.launch { drawerState.open() }
                                }
                            ) { Icon(Icons.Rounded.Menu, "Open sidebars") }
                        },
                        actions = {

                        }
                    )
            },
            bottomBar = {
                NavigationBar {
                    val navigationOnClick: ((() -> Unit) -> (() -> Unit)) = { onClick ->
                        {
                            onClick()
                            isLoading = true
                            loader()
                        }
                    }
                    NavigationBarItem(
                        selected = selectedPage == NanHistoryPages.Recent,
                        onClick = navigationOnClick {
                            selectedPage = NanHistoryPages.Recent
                        },
                        icon = {
                            val icon = painterResource(
                                if (selectedPage == NanHistoryPages.Recent) R.drawable.ic_schedule_filled
                                else R.drawable.ic_schedule
                            )
                            Icon(icon, "Recent events")
                        },
                        label = {
                            Text("Recent")
                        }
                    )
                    NavigationBarItem(
                        selected = selectedPage == NanHistoryPages.Favorite,
                        onClick = navigationOnClick {
                            selectedPage = NanHistoryPages.Favorite
                        },
                        icon = {
                            val icon = painterResource(
                                if (selectedPage == NanHistoryPages.Favorite) R.drawable.ic_favorite_filled
                                else R.drawable.ic_favorite
                            )
                            Icon(icon, "Favorite events")
                        },
                        label = {
                            Text("Favorite")
                        }
                    )
                    NavigationBarItem(
                        selected = selectedPage == NanHistoryPages.All,
                        onClick = navigationOnClick {
                            selectedPage = NanHistoryPages.All
                        },
                        icon = {
                            val icon = painterResource(
                                if (selectedPage == NanHistoryPages.All) R.drawable.ic_event_filled
                                else R.drawable.ic_event
                            )
                            Icon(icon, "All events")
                        },
                        label = {
                            Text("All")
                        }
                    )
                }
            },
        ) { paddingValues ->
            val lazyListState = rememberLazyListState()

            val fromRecent = Instant.now().minusSeconds(2_592_000) // 30 days = 3600 * 24 * 30
            val fromZero = Instant.ofEpochSecond(0L)

            var firstLoad by rememberSaveable { mutableStateOf(false) }

            loader = loadTop@{
                firstLoad = true
                scope.launch load@{
                    val data = withContext(Dispatchers.IO) {
                        when (selectedPage) {
                            NanHistoryPages.Favorite -> HistoryFileData
                                .getList(context, from = fromZero)
                                .filter { it.favorite || it.events.find { event -> event.favorite } != null }
                                .sortedByDescending { it.date }

                            NanHistoryPages.All -> HistoryFileData
                                .getList(context, from = fromZero)
                                .sortedByDescending { it.date }

                            else -> HistoryFileData
                                .getList(context, from = fromRecent)
                                .sortedByDescending { it.date }
                        }
                    }
                    fileDataList = data
                    isLoading = false
                }
            }

            BackHandler(
                enabled = selectionMode,
            ) {
                resetSelectionMode()
            }

            if (!firstLoad) {
                isLoading = true
                loader()
            }

            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(Unit) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) loader()
                }
                lifecycleOwner.lifecycle.addObserver(observer)

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            if (selectedItems.isEmpty() && selectionMode) resetSelectionMode()

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
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .selectableGroup()
                            .fillMaxSize(),
                        state = lazyListState,
                    ) {
                        fileDataList.filter { it.events.isNotEmpty() }.forEach { day ->
                            stickyHeader(key = "${day.date}_$selectedPage") {
                                EventListHeader(
                                    historyDay = day.historyDay,
                                    modifier = Modifier.animateItem()
                                ) {
                                    day.favorite = it
                                    day.save(context)
                                }
                            }
                            val events = day.events.sortedByDescending { it.time }.let {
                                if (selectedPage == NanHistoryPages.Favorite && !day.favorite)
                                    it.filter { event -> event.favorite }
                                else
                                    it
                            }
                            items(events.size, key = { "${events[it].id}_$selectedPage" }) {
                                val eventData = events[it]
                                val selected = selectedItems.contains(eventData.id)
                                EventListItem(
                                    eventData,
                                    selected = selected,
                                    modifier = Modifier
                                        .animateItem()
                                        .combinedClickable(
                                            onClick = listItemOnClick@{
                                                if (lock) return@listItemOnClick
                                                if (!selectionMode) {
                                                    val intent =
                                                        Intent(
                                                            context,
                                                            EventDetailActivity::class.java
                                                        )
                                                    intent.putExtra("eventId", eventData.id)
                                                    intent.putExtra(
                                                        "path",
                                                        getFilePathFromDate(eventData.time.toLocalDate())
                                                    )
                                                    context.startActivity(intent)
                                                } else {
                                                    if (selected) selectedItems.remove(eventData.id)
                                                    else selectedItems.add(eventData.id)
                                                }
                                            },
                                            onLongClick = listItemOnLongClick@{
                                                if (lock) return@listItemOnLongClick
                                                selectionMode = true
                                                if (selected) selectedItems.remove(eventData.id)
                                                else selectedItems.add(eventData.id)
                                            }
                                        ),
                                )
                            }
                        }
                        item {
                            Box(modifier = Modifier.height(136.dp))
                        }
                    }
                }
            }
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
                Text(deleteDialogTitle)
            },
            text = {
                Text(deleteDialogText)
            },
            dismissButton = {
                if (deleteButtonState) TextButton(
                    onClick = {
                        deleteDialogState = false
                    }
                ) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                if (deleteButtonState) Button(
                    onClick = {
                        scope.launch {
                            deleteButtonState = false
                            val selectedItemsCount = selectedItems.size
                            deleteDialogTitle = "Deleting Events"
                            deleteDialogText = "Initializing..."
                            withContext(Dispatchers.IO) {
                                val events = mutableListOf<HistoryEvent>()
                                for (day in HistoryFileData.getList(context)) {
                                    events.addAll(day.events)
                                }
                                events.filter { selectedItems.contains(it.id) }
                                    .forEachIndexed { index, event ->
                                        event.delete(context)
                                        deleteDialogText = "Deleting ${index + 1} of $selectedItemsCount"
                                    }
                            }
                            resetSelectionMode()
                            closeDeleteDialog()
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