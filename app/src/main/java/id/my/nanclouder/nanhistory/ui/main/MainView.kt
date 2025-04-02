package id.my.nanclouder.nanhistory.ui.main

import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.VibratorManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import id.my.nanclouder.nanhistory.EditEventActivity
import id.my.nanclouder.nanhistory.EventDetailActivity
import id.my.nanclouder.nanhistory.ListFilters
import id.my.nanclouder.nanhistory.NanHistoryPages
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.lib.history.EventRange
import id.my.nanclouder.nanhistory.lib.history.HistoryEvent
import id.my.nanclouder.nanhistory.lib.history.HistoryFileData
import id.my.nanclouder.nanhistory.lib.history.delete
import id.my.nanclouder.nanhistory.lib.history.get
import id.my.nanclouder.nanhistory.lib.history.getDateFromFilePath
import id.my.nanclouder.nanhistory.lib.history.getFilePathFromDate
import id.my.nanclouder.nanhistory.lib.history.getList
import id.my.nanclouder.nanhistory.lib.history.getListStream
import id.my.nanclouder.nanhistory.lib.history.save
import id.my.nanclouder.nanhistory.lib.matchOrNull
import id.my.nanclouder.nanhistory.service.RecordService
import id.my.nanclouder.nanhistory.ui.SearchAppBar
import id.my.nanclouder.nanhistory.ui.SelectionAppBar
import id.my.nanclouder.nanhistory.ui.list.EventListHeader
import id.my.nanclouder.nanhistory.ui.list.EventListItem
import id.my.nanclouder.nanhistory.ui.style.DangerButtonColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun MutableList<HistoryFileData>.insertSorted(data: HistoryFileData) {
    val index = binarySearch(data, compareByDescending { it.date })
    val insertIndex = if (index >= 0) index else -index - 1
    add(insertIndex, data)
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun MainView() {
    var appliedFilter by rememberSaveable { mutableStateOf(ListFilters.Recent) }
    var selectedPage by rememberSaveable { mutableStateOf(NanHistoryPages.Events) }

    val filterIndex =
        if (selectedPage != NanHistoryPages.Search) appliedFilter.ordinal
        else 3

    var needUpdate by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val sharedPreferences = LocalContext.current.applicationContext
        .getSharedPreferences("recordEvent", Context.MODE_PRIVATE)

    var loader by remember { mutableStateOf({ _: Boolean -> }) }
    var searchQuery by remember { mutableStateOf("") }

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

    val haptic = LocalHapticFeedback.current
    val vibrator = (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
        .defaultVibrator

    var isRecording by rememberSaveable { mutableStateOf(false) }
    isRecording = RecordService.isRunning(LocalContext.current)

    var selectionMode by remember { mutableStateOf(false) }
    val selectedItems = remember { mutableStateListOf<HistoryEvent>() }
//    val isLoading = remember { mutableStateListOf(*Array(4) { true }) }

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

    var lastUpdate by remember { mutableStateOf(Instant.now()) }

    val fileDataList = remember {
        List(4) { mutableStateListOf<HistoryFileData>() }
    }
    val loaderJob = remember {
        mutableStateListOf(*Array<Job?>(4) { null })
    }
    val loaderActive = remember {
        mutableStateListOf(*Array(4) { false })
    }
    val expanded = remember {
        List(4) { mutableStateListOf<LocalDate>() }
    }
    val viewModels = List(4) {
        remember { mutableStateOf<EventListViewModel?>(null) }
    }

    val isLoading = viewModels[filterIndex].value?.isLoading?.collectAsState()?.value ?: false

    val updateData = { date: LocalDate ->
        viewModels.mapNotNull { it.value }.forEach {
            it.update(context, date)
        }
    }

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
                    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                        if (result.resultCode == 1) {
                            result.data?.getStringExtra("path")?.let { path ->
                                val date = getDateFromFilePath(path) ?: LocalDate.now()
                                updateData(date)
                            }
                        }
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            val intent = Intent(context, EditEventActivity::class.java)
                            intent.putExtra("eventId", "")
                            launcher.launch(intent)
                        },
                    ) {
                        Icon(painterResource(R.drawable.ic_add), "Add event button")
                    }
                    FloatingActionButton(
                        onClick = {
                            val intent = Intent(context, RecordService::class.java)
                            val now = ZonedDateTime.now()
                            if (isRecording) {
                                context.stopService(intent)
                                scope.launch {
                                    delay(100L)
                                    sharedPreferences.edit().putBoolean("isRunning", false).apply()
                                    isRecording = false
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    for (i in 2L downTo 0L)
                                        updateData(LocalDate.now().minusDays(i))

//                                    fileDataList[pageIndex]
//                                        .find { day -> day.date == event.time.toLocalDate() }
//                                        ?.apply {
//                                            events += event
//                                        }
//                                    needUpdate = true
                                }
                            } else {
                                if (recordPermissionState.allPermissionsGranted) {
                                    val event = EventRange(
                                        title = RecordService.titleFormatter.format(now),
                                        description = "",
                                        time = now,
                                        end = now
                                    ).apply {
                                        metadata["recording"] = true
                                    }
                                    event.save(context)
                                    intent.putExtra("eventId", event.id)
                                    intent.putExtra("path", getFilePathFromDate(event.time.toLocalDate()))

//                                    fileDataList[pageIndex]
//                                        .find { day -> day.date == event.time.toLocalDate() }
//                                        ?.apply {
//                                            events += event
//                                        }
                                    needUpdate = true
                                    updateData(event.time.toLocalDate())

                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                    context.startForegroundService(intent)
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
                if (selectionMode) SelectionAppBar(
                    selectedItems, { resetSelectionMode() }
                ) {
                    IconButton(onClick = {
//                        val selected = selectedItems.size
//                        val items = fileDataList[filterIndex].flatMap {
//                            it.events
//                        }
//                        selectedItems.clear()
//                        if (selected < items.size)
//                            selectedItems.addAll(items)
                        viewModels[filterIndex].value?.selectAll()
                    }) {
                        Icon(painterResource(R.drawable.ic_select_all), "Select all")
                    }
                    IconButton(onClick = {
                        Log.d("NanHistoryDebug", "SELECTED ITEMS: ${selectedItems.toList()}")
                        scope.launch {
                            lock = true
                            withContext(Dispatchers.IO) {
                                selectedItems.map { event ->
                                    event.favorite = !event.favorite
                                    event.save(context)
                                    event.time.toLocalDate()
                                }.distinct().forEach(updateData)
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
                else if (selectedPage == NanHistoryPages.Search) SearchAppBar(
                    isLoading = isLoading,
                    onSearch = {
                        viewModels[3].value?.search(context, it)
                    },
                    onCancel = {
                        viewModels[3].value?.cancelLoading()
                    }
                )
                else TopAppBar(
                    title = { Text("Events") },
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch { drawerState.open() }
                            }
                        ) { Icon(Icons.Rounded.Menu, "Open sidebar") }
                    },
                    actions = {
//                        val collapsedAll = expanded[filterIndex].isEmpty()
                        val collapsedAll = false
                        IconButton(
                            onClick = {
                                viewModels[filterIndex].value?.collapseAll()
//                                expanded[filterIndex].clear()
//                                if (collapsedAll)
//                                    expanded[filterIndex].addAll(fileDataList[filterIndex].map { it.date })
                            }
                        ) {
                            if (!collapsedAll)
                                Icon(painterResource(R.drawable.ic_collapse_all), "Collapse all")
                            else
                                Icon(painterResource(R.drawable.ic_expand_all), "Expand all")
                        }
                        Box(Modifier.size(48.dp)) {
                            if (isLoading) CircularProgressIndicator(Modifier.fillMaxSize().padding(8.dp))
                            else IconButton(
                                onClick = {
                                    expanded[filterIndex].clear()
                                    if (collapsedAll)
                                        expanded[filterIndex].addAll(fileDataList[filterIndex].map { it.date })
                                }
                            ) {
                                IconButton(onClick = {
                                    viewModels[filterIndex].value?.reload(context)
                                }) {
                                    Icon(Icons.Rounded.Refresh, "Refresh")
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    val navigationOnClick = { page: NanHistoryPages ->
                        navOnClick@{
                            if (selectedPage == page) return@navOnClick
                            selectedPage = page
                        }
                    }
                    NavigationBarItem(
                        selected = selectedPage == NanHistoryPages.Events,
                        onClick = navigationOnClick(NanHistoryPages.Events),
                        icon = {
                            val icon = painterResource(
                                if (selectedPage == NanHistoryPages.Events) R.drawable.ic_event_filled
                                else R.drawable.ic_event
                            )
                            Icon(icon, "Events")
                        },
                        label = {
                            Text("Events")
                        }
                    )
                    NavigationBarItem(
                        selected = selectedPage == NanHistoryPages.Search,
                        onClick = navigationOnClick(NanHistoryPages.Search),
                        icon = {
                            val icon = painterResource(
                                R.drawable.ic_search
                            )
                            Icon(icon, "Search events")
                        },
                        label = {
                            Text("Search")
                        }
                    )
                }
            },
        ) { paddingValues ->
            val lazyListState = List(4) { rememberLazyListState() }
            var queuedUpdate by remember { mutableStateOf(false) }

            val updateBuffer = remember { mutableStateListOf<HistoryFileData>() }
            val expandedBuffer = remember { mutableStateListOf<LocalDate>() }

            val fromRecent = LocalDate.now() // 30 days = 3600 * 24 * 30
            val fromZero = LocalDate.MIN

            var firstLoad by rememberSaveable { mutableStateOf(false) }

            val filterSearchEvent = { event: HistoryEvent ->
                (
                    event.title.contains(searchQuery, true) ||
                    event.description.contains(searchQuery, true) ||
    //                (searchQuery.startsWith("duration") && )
                    event.time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL))
                        .toString().contains(searchQuery, true)
                ) && searchQuery.isNotBlank()
            }

            var updateFromBuffer = {}
            updateFromBuffer = bufferUpdater@{
                val now = Instant.now()
                if (now.toEpochMilli() - lastUpdate.toEpochMilli() < 100L) {
                    if (!queuedUpdate) scope.launch {
                        queuedUpdate = true
                        delay(100L)
                        updateFromBuffer()
                    }
                    return@bufferUpdater
                }

                lastUpdate = Instant.now()
                queuedUpdate = false
                fileDataList[filterIndex].addAll(updateBuffer)
                fileDataList[filterIndex].sortByDescending { it.date }
                expanded[filterIndex].addAll(expandedBuffer)

                updateBuffer.clear()
                expandedBuffer.clear()
            }

            val insertNoDuplicate: suspend (HistoryFileData) -> Unit = { data: HistoryFileData ->
                if (fileDataList[filterIndex].find { it.date == data.date } == null) {
//                    fileDataList[pageIndex].add(data)
//                    updateBuffer.add(data)
//                    expandedBuffer.add(data.date)

                    fileDataList[filterIndex].insertSorted(data)
                    expanded[filterIndex].add(data.date)

//                    updateFromBuffer()
                }
            }

            loader = loadTop@{ updateExpanded ->
                firstLoad = true
                val previousJob = loaderJob[filterIndex]
                val currentIndex = filterIndex
                val streamDataListEnabled = Config.experimentalStreamList.get(context)
                loaderJob[currentIndex] = scope.launch load@{
                    loaderActive[currentIndex] = true
                    val replaceIfExists = { data: HistoryFileData ->
                        val find = fileDataList[currentIndex].indexOfFirst { it.date == data.date }
                        if (find != -1)
                            fileDataList[currentIndex][find] = data
                        else
                            fileDataList[currentIndex].add(data)
                    }

                    previousJob?.cancelAndJoin()
                    lock = true
                    withContext(Dispatchers.IO) {
//                        if (streamDataListEnabled) fileDataList[currentIndex].clear()
                        if (updateExpanded) expanded[currentIndex].clear()
                        if (!streamDataListEnabled) when (appliedFilter) {
                            ListFilters.Favorite -> HistoryFileData
                                .getList(context)
                                .filter { it.favorite || it.events.find { event -> event.favorite } != null }
                                .sortedByDescending { it.date }

                            ListFilters.All -> HistoryFileData
                                .getList(context)
                                .sortedByDescending { it.date }

//                            ListFilters.Search -> HistoryFileData
//                                .getList(context)
//                                .filter {
//                                    it.events.find { event ->
//                                        filterSearchEvent(event)
//                                    } != null
//                                }
//                                .sortedByDescending { it.date }

                            else -> HistoryFileData
                                .getList(context)
                                .sortedByDescending { it.date }
                        }.let {
                            fileDataList[currentIndex].let { list ->
                                list.clear()
                                list.addAll(it)
                            }
                        }
                        else when (appliedFilter) {
                            ListFilters.Favorite -> HistoryFileData
                                .getListStream(context, from = fromZero).apply {
                                    sortedByDescending { getDateFromFilePath(it.absolutePath) }
                                }.forEachAsync {
                                    if (it.favorite || it.events.find { event -> event.favorite } != null &&
                                        fileDataList[currentIndex].find { day -> day.date == it.date } == null) replaceIfExists(it)
                                }

                            ListFilters.All -> HistoryFileData
                                .getListStream(context, from = fromZero).apply {
                                    sortedByDescending { getDateFromFilePath(it.absolutePath) }
                                }.forEachAsync {
                                    if (fileDataList[currentIndex].find { day -> day.date == it.date } == null)
                                        replaceIfExists(it)
                                }

//                            ListFilters.Search -> {
//                                fileDataList[currentIndex].clear()
//                                if (searchQuery.isNotBlank()) HistoryFileData
//                                    .getListStream(context, from = fromZero).apply {
//                                        sortedByDescending { getDateFromFilePath(it.absolutePath) }
//                                    }.forEachAsync {
//                                        if (it.events.find { event ->
//                                            filterSearchEvent(event) && fileDataList[currentIndex].find { day -> day.date == it.date } == null
//                                        } != null) {
//                                            replaceIfExists(it)
//                                        }
//                                    }
//                                else Unit
//                            }

                            else -> HistoryFileData
                                .getListStream(context, from = fromRecent).apply {
                                    sortedByDescending { getDateFromFilePath(it.absolutePath) }
                                }.forEachAsync { replaceIfExists(it) }
                        }
                    }
                    loaderActive[currentIndex] = false
                    lock = false
//                    isLoading[currentIndex] = false
                }
            }

            BackHandler(
                enabled = selectionMode,
            ) {
                resetSelectionMode()
            }

//            if (!firstLoad) {
//                isLoading[pageIndex] = true
//                loader(true)
//            }

//            val lifecycleOwner = LocalLifecycleOwner.current

//            DisposableEffect(Unit) {
//                val observer = LifecycleEventObserver { _, event ->
//                    if (event == Lifecycle.Event.ON_RESUME) loader(false)
//                }
//                lifecycleOwner.lifecycle.addObserver(observer)
//
//                onDispose {
//                    lifecycleOwner.lifecycle.removeObserver(observer)
//                }
//            }

//            if (selectedItems.isEmpty() && selectionMode) resetSelectionMode()

            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .background(
                        if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.surfaceContainer
                    )
            ) {
                val lazyListStates = List(4) {
                    rememberLazyListState()
                }

                List<@Composable () -> Unit>(4) { index ->
                    @Composable {
                        var viewModel by viewModels[index]
                        EventList(
                            viewModel = viewModel ?: let {
                                viewModel =
                                    if (selectedPage == NanHistoryPages.Events) when (appliedFilter) {
                                        ListFilters.Recent -> EventListViewModel(context, LocalDate.now().minusDays(30))
                                        ListFilters.Favorite -> EventListViewModel(context, favorite = true)
                                        ListFilters.All -> EventListViewModel(context)
                                    } else EventListViewModel(context, searchMode = true)
                                viewModel!!
                            },
                            lazyListState = lazyListStates[index],
                            onSelect = {
                                selectionMode = true
                                selectedItems.clear()
                                selectedItems.addAll(it)
                            },
                            onFilter = {
                                appliedFilter = it
                            },
                            onUpdate = { date ->
                                updateData(date)
                            },
                            filter = appliedFilter,
                            filterEnabled = selectedPage == NanHistoryPages.Events,
                            selectionMode = selectionMode,
                        )
                    }
                }[filterIndex].invoke()
            }
        }
    }
    if (deleteDialogState) {
        AlertDialog(
            icon = {
                Icon(Icons.Rounded.Warning, "Warning")
            },
            onDismissRequest = {
                if (!lock) deleteDialogState = false
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
                            lock = true
                            deleteButtonState = false
                            val selectedItemsCount = selectedItems.size
                            deleteDialogTitle = "Deleting Events"
                            deleteDialogText = "Deleting..."
                            withContext(Dispatchers.IO) {
                                selectedItems.forEachIndexed { index, event ->
                                    event.delete(context)
                                    updateData(event.time.toLocalDate())
                                    deleteDialogText =
                                        "Deleting ${index + 1} of $selectedItemsCount"
                                }
                            }
                            lock = false
                            resetSelectionMode()
                            loader(false)
                            vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                            closeDeleteDialog()
                        }
                    },
                    colors = DangerButtonColors
                ) {
                    Text("Delete")
                }
            }
        )
    }

    if (needUpdate) needUpdate = false
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EventList(
    viewModel: EventListViewModel,
    onSelect: (List<HistoryEvent>) -> Unit,
    onFilter: (ListFilters) -> Unit,
    filter: ListFilters,
    selectionMode: Boolean,
    onUpdate: (LocalDate) -> Unit,
    filterEnabled: Boolean = false,
    lazyListState: LazyListState = rememberLazyListState(),
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
) {
    val context = LocalContext.current
    val expanded = remember { mutableStateListOf<LocalDate>() }
    val selectedItems = remember { mutableStateListOf<HistoryEvent>() }

    val eventList by viewModel.list.collectAsState()

    val haptic = LocalHapticFeedback.current

    if (!selectionMode) selectedItems.clear()

    LazyColumn(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .selectableGroup()
            .fillMaxSize(),
        state = lazyListState,
    ) {
        val expandedCurrent = expanded.toList()

        viewModel.expandStateChange {
            expanded.clear()
            if (it) expanded.addAll(eventList.map { day -> day.date })
        }

        viewModel.selector {
            val selected = selectedItems.size
            val events = eventList.flatMap { day -> day.events }
            selectedItems.clear()
            if (selected < events.size) selectedItems.addAll(events)
            onSelect(selectedItems)
        }

        val filterOnClick = { thisFilter: ListFilters ->
            filterOnClick@{
                if (filter == thisFilter) return@filterOnClick
                onFilter(thisFilter)
            }
        }

        if (filterEnabled) {
            item { Box(Modifier.height(16.dp)) }
            item {
                SingleChoiceSegmentedButtonRow(
                    Modifier
                        .width(264.dp)
                        .height(32.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    ListFilters.entries.forEach {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = it.ordinal,
                                count = ListFilters.entries.size
                            ),
                            onClick = filterOnClick(it),
                            selected = filter == it,
                            icon = { SegmentedButtonDefaults.Icon(false) },
                            label = { Text(it.name, lineHeight = 0.2.em) }
                        )
                    }
                }
            }
        }

        item { Box(Modifier.height(16.dp)) }

        items(eventList, key = { "day${it.date}-${it.hashCode()}" }) { day ->
            val dayExpanded = expandedCurrent.contains(day.date)
            val headerSelected = selectedItems.containsAll(day.events.map { it })

            Column(
                Modifier
                    .padding(bottom = 16.dp)
                    .animateItem()
            ) {
                EventListHeader(
                    historyDay = day.historyDay,
                    selected = headerSelected,
                    expanded = dayExpanded,
                    onExpandButtonClicked = {
                        if (expandedCurrent.contains(day.historyDay.date))
                            expanded.remove(day.historyDay.date)
                        else
                            expanded.add(day.historyDay.date)
                    },
                    eventCount = day.events.size,
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                if (!selectionMode) {
                                    if (dayExpanded)
                                        expanded.remove(day.date)
                                    else
                                        expanded.add(day.date)
                                } else {
                                    if (!headerSelected) selectedItems.addAll(day.events)
                                    else selectedItems.removeAll(day.events)
                                    val distinct = selectedItems.distinct()
                                    selectedItems.clear()
                                    selectedItems.addAll(distinct)
                                    onSelect(selectedItems)
                                }
                            },
                            onLongClick = listHeaderOnLongClick@{
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (headerSelected) selectedItems.removeAll(day.events)
                                else selectedItems.addAll(day.events)
                                onSelect(selectedItems)
                            }
                        )
                ) {
                    day.favorite = it
                    day.save(context)
                    onUpdate(day.date)
                }

                val events = day.events.sortedByDescending { it.time }
                AnimatedVisibility(
                    visible = dayExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        day.events.forEachIndexed { index, _ ->
                            var eventData by remember { mutableStateOf(events[index]) }
                            val recording = matchOrNull<Boolean>(eventData.metadata["recording"]) ?: false
                            val selected = selectedItems.contains(eventData)
                            val lastItem = index + 1 >= events.size

                            val scope = rememberCoroutineScope()

                            scope.launch {
                                delay(2000L)
                                if (recording && !RecordService.isRunning(context)) {
                                    val event = eventData
                                    event.metadata["recording"] = false
                                    event.save(context)
                                    eventData = event
                                }
                            }

                            val launcher =
                                rememberLauncherForActivityResult(
                                    ActivityResultContracts.StartActivityForResult()
                                ) { result ->
                                    if (result.resultCode == 1 || result.resultCode == 2) {
                                        result.data?.getStringExtra("path")
                                            ?.let { path ->
                                                val date = getDateFromFilePath(path)
                                                if (eventData.time.toLocalDate() != date)
                                                    viewModel.update(context, eventData.time.toLocalDate())

                                                viewModel.update(context, date ?: LocalDate.MIN)
                                            }
                                    }
                                }
                            EventListItem(
                                eventData,
                                selected = selected,
                                modifier = Modifier
                                    .clip(
                                        if (lastItem) RoundedCornerShape(
                                            bottomStart = 24.dp, bottomEnd = 24.dp
                                        )
                                        else RectangleShape
                                    )
                                    .combinedClickable(
                                        onClick = listItemOnClick@{
//                                            if (lock) return@listItemOnClick
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
                                                launcher.launch(intent)
                                            } else {
                                                if (recording) return@listItemOnClick
                                                if (selected) selectedItems.remove(eventData)
                                                else selectedItems.add(eventData)
                                                onSelect(selectedItems)
                                            }
                                        },
                                        onLongClick = listItemOnLongClick@{
                                            if (recording) return@listItemOnLongClick
//                                            selectionMode = true
                                            haptic.performHapticFeedback(
                                                HapticFeedbackType.LongPress
                                            )
                                            if (selected) selectedItems.remove(eventData)
                                            else selectedItems.add(eventData)
                                            onSelect(selectedItems)
                                        }
                                    ),
                            )
                        }
                    }
                }
            }
        }
//
//                    listCurrent.filter { it.events.isNotEmpty() }.forEach { day ->
//
////                            item {
//////                                Box(Modifier.height(16.dp))
////                            }
//                    }
        item {
            Box(modifier = Modifier.height(136.dp))
        }
    }
}