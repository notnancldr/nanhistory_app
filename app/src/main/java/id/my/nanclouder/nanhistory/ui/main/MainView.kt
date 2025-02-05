package id.my.nanclouder.nanhistory.ui.main

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.lib.history.EventRange
import id.my.nanclouder.nanhistory.lib.history.HistoryEvent
import id.my.nanclouder.nanhistory.lib.history.HistoryFileData
import id.my.nanclouder.nanhistory.lib.history.delete
import id.my.nanclouder.nanhistory.lib.history.getDateFromFilePath
import id.my.nanclouder.nanhistory.lib.history.getFilePathFromDate
import id.my.nanclouder.nanhistory.lib.history.getList
import id.my.nanclouder.nanhistory.lib.history.getListStream
import id.my.nanclouder.nanhistory.lib.history.save
import id.my.nanclouder.nanhistory.service.RecordService
import id.my.nanclouder.nanhistory.ui.SearchAppBar
import id.my.nanclouder.nanhistory.ui.SelectionAppBar
import id.my.nanclouder.nanhistory.ui.list.EventListHeader
import id.my.nanclouder.nanhistory.ui.list.EventListItem
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
    var selectedPage by remember { mutableStateOf(NanHistoryPages.Recent) }
    var pageIndex by remember { mutableIntStateOf(0) }

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
        NanHistoryPages.Search -> "Search"
    }

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

    var isRecording by rememberSaveable { mutableStateOf(false) }
    isRecording = RecordService.isRunning(LocalContext.current)

    var selectionMode by remember { mutableStateOf(false) }
    val selectedItems = remember { mutableStateListOf<String>() }
    val isLoading = remember { mutableStateListOf(true, true, true, true) }

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

    val fileDataList = remember { listOf<SnapshotStateList<HistoryFileData>>(
        mutableStateListOf(),
        mutableStateListOf(),
        mutableStateListOf(),
        mutableStateListOf()
    ) }
    val loaderJob = remember { mutableStateListOf<Job?>(
        null, null, null, null
    ) }

    val expanded = remember { listOf<SnapshotStateList<LocalDate>>(
        mutableStateListOf(),
        mutableStateListOf(),
        mutableStateListOf(),
        mutableStateListOf()
    ) }

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
                            val intent = Intent(context, RecordService::class.java)
                            val now = ZonedDateTime.now()
                            if (isRecording) {
                                context.stopService(intent)
                                scope.launch {
                                    delay(100L)
                                    sharedPreferences.edit().putBoolean("isRunning", false).apply()
                                    isRecording = false
                                    loader(false)
                                }
                            } else {
                                if (recordPermissionState.allPermissionsGranted) {
                                    val event = EventRange(
                                        title = RecordService.titleFormatter.format(now),
                                        description = "",
                                        time = now,
                                        end = now
                                    )
                                    event.save(context)
                                    intent.putExtra("eventId", event.id)
                                    intent.putExtra("path", getFilePathFromDate(event.time.toLocalDate()))
                                    fileDataList[pageIndex]
                                        .find { day -> day.date == event.time.toLocalDate() }
                                            ?.events?.add(event)

                                    context.startForegroundService(intent)
                                    scope.launch {
                                        delay(100L)
                                        loader(false)
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
                if (selectionMode) SelectionAppBar(
                    selectedItems, { resetSelectionMode() }
                ) {
                    IconButton(onClick = {
                        scope.launch {
                            lock = true
                            withContext(Dispatchers.IO) {
                                fileDataList[pageIndex].forEach {
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
                else if (selectedPage == NanHistoryPages.Search) SearchAppBar(
                    searchButtonEnabled = !isLoading[pageIndex],
                    onSearch = {
                        isLoading[pageIndex] = true
                        searchQuery = it
                        loader(false)
                    }
                )
                else TopAppBar(
                    title = { Text(appBarTitle) },
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch { drawerState.open() }
                            }
                        ) { Icon(Icons.Rounded.Menu, "Open sidebar") }
                    },
                    actions = {
                        val collapsedAll = expanded[pageIndex].isEmpty()
                        IconButton(
                            onClick = {
                                expanded[pageIndex].clear()
                                if (collapsedAll)
                                    expanded[pageIndex].addAll(fileDataList[pageIndex].map { it.date })
                            }
                        ) {
                            if (!collapsedAll)
                                Icon(painterResource(R.drawable.ic_collapse_all), "Collapse all")
                            else
                                Icon(painterResource(R.drawable.ic_expand_all), "Expand all")
                        }
                        IconButton(
                            onClick = {
                                expanded[pageIndex].clear()
                                if (collapsedAll)
                                    expanded[pageIndex].addAll(fileDataList[pageIndex].map { it.date })
                            }
                        ) {
                            IconButton(enabled = !isLoading[pageIndex], onClick = {
                                isLoading[pageIndex] = true
                                loader(false)
                            }) {
                                Icon(Icons.Rounded.Refresh, "Refresh")
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
                            val streamDataListEnabled = Config.experimentalStreamList.get(context)
                            selectedPage = page
                            pageIndex = page.ordinal
//                            onClick()
//                            isLoading[pageIndex] = true
                            isLoading[pageIndex] = true
                            if (fileDataList[pageIndex].isEmpty() && streamDataListEnabled) {
                                loader(true)
                            }
                            else if (!streamDataListEnabled) {
                                loader(true)
                            }
                        }
                    }
                    NavigationBarItem(
                        selected = selectedPage == NanHistoryPages.Recent,
                        onClick = navigationOnClick(NanHistoryPages.Recent),
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
                        onClick = navigationOnClick(NanHistoryPages.Favorite),
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
                        onClick = navigationOnClick(NanHistoryPages.All),
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
            val lazyListState = listOf(
                rememberLazyListState(),
                rememberLazyListState(),
                rememberLazyListState(),
                rememberLazyListState()
            )

            var lastUpdate by remember { mutableStateOf(Instant.ofEpochMilli(0)) }
            var queuedUpdate by remember { mutableStateOf(false) }

            val updateBuffer = remember { mutableStateListOf<HistoryFileData>() }
            val expandedBuffer = remember { mutableStateListOf<LocalDate>() }

            val fromRecent = Instant.now().minusSeconds(2_592_000) // 30 days = 3600 * 24 * 30
            val fromZero = Instant.ofEpochSecond(0L)

            var firstLoad by rememberSaveable { mutableStateOf(false) }

            val filterSearchEvent = { event: HistoryEvent ->
                event.title.contains(searchQuery, true) ||
                event.description.contains(searchQuery, true) ||
//                (searchQuery.startsWith("duration") && )
                event.time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL))
                    .toString().contains(searchQuery, true)
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
                fileDataList[pageIndex].addAll(updateBuffer)
                fileDataList[pageIndex].sortByDescending { it.date }
                expanded[pageIndex].addAll(expandedBuffer)

                updateBuffer.clear()
                expandedBuffer.clear()
            }

            val insertNoDuplicate: suspend (HistoryFileData) -> Unit = { data: HistoryFileData ->
                if (fileDataList[pageIndex].find { it.date == data.date } == null) {
//                    fileDataList[pageIndex].add(data)
//                    updateBuffer.add(data)
//                    expandedBuffer.add(data.date)

                    fileDataList[pageIndex].insertSorted(data)
                    expanded[pageIndex].add(data.date)

//                    updateFromBuffer()
                }
            }

            loader = loadTop@{ updateExpanded ->
                firstLoad = true
                val previousJob = loaderJob[pageIndex]
                val currentIndex = pageIndex
                val streamDataListEnabled = Config.experimentalStreamList.get(context)
                loaderJob[pageIndex] = scope.launch load@{
                    previousJob?.cancelAndJoin()
                    lock = true
                    withContext(Dispatchers.IO) {
                        if (streamDataListEnabled) fileDataList[currentIndex].clear()
                        if (updateExpanded) expanded[currentIndex].clear()
                        if (!streamDataListEnabled) when (selectedPage) {
                            NanHistoryPages.Favorite -> HistoryFileData
                                .getList(context, from = fromZero)
                                .filter { it.favorite || it.events.find { event -> event.favorite } != null }
                                .sortedByDescending { it.date }

                            NanHistoryPages.All -> HistoryFileData
                                .getList(context, from = fromZero)
                                .sortedByDescending { it.date }

                            NanHistoryPages.Search -> HistoryFileData
                                .getList(context, from = fromZero)
                                .filter {
                                    it.events.find { event ->
                                        filterSearchEvent(event)
                                    } != null
                                }
                                .sortedByDescending { it.date }

                            else -> HistoryFileData
                                .getList(context, from = fromRecent)
                                .sortedByDescending { it.date }
                        }.let {
                            fileDataList[currentIndex].let { list ->
                                list.clear()
                                list.addAll(it)
                            }
                        }
                        else when (selectedPage) {
                            NanHistoryPages.Favorite -> HistoryFileData
                                .getListStream(context, from = fromZero).apply {
                                    sortedByDescending { getDateFromFilePath(it.absolutePath) }
                                }.forEachAsync {
                                    if (it.favorite || it.events.find { event -> event.favorite } != null &&
                                        fileDataList[currentIndex].find { day -> day.date == it.date } == null) fileDataList[currentIndex].add(it)
                                }

                            NanHistoryPages.All -> HistoryFileData
                                .getListStream(context, from = fromZero).apply {
                                    sortedByDescending { getDateFromFilePath(it.absolutePath) }
                                }.forEachAsync {
                                    if (fileDataList[currentIndex].find { day -> day.date == it.date } == null)
                                        fileDataList[currentIndex].add(it)
                                }

                            NanHistoryPages.Search -> HistoryFileData
                                .getListStream(context, from = fromZero).apply {
                                    sortedByDescending { getDateFromFilePath(it.absolutePath) }
                                }.forEachAsync {
                                    if (it.events.find { event ->
                                        filterSearchEvent(event) && fileDataList[currentIndex].find { day -> day.date == it.date } == null
                                    } != null) {
                                        fileDataList[currentIndex].add(it)
                                    }
                                }

                            else -> HistoryFileData
                                .getListStream(context, from = fromRecent).apply {
                                    sortedByDescending { getDateFromFilePath(it.absolutePath) }
                                }.forEachAsync { fileDataList[currentIndex].add(it) }
                        }
                    }
                    lock = false
                    isLoading[currentIndex] = false
                }
            }

            BackHandler(
                enabled = selectionMode,
            ) {
                resetSelectionMode()
            }

            if (!firstLoad) {
                isLoading[pageIndex] = true
                loader(true)
            }

            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(Unit) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) loader(false)
                }
                lifecycleOwner.lifecycle.addObserver(observer)

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            if (selectedItems.isEmpty() && selectionMode) resetSelectionMode()

            if (isLoading[pageIndex] == null) {
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
                        .background(
                            if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.surfaceContainer
                        )
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .selectableGroup()
                            .fillMaxSize(),
                        state = lazyListState[pageIndex],
                    ) {
                        val listCurrent = fileDataList[pageIndex].toList()
                        val expandedCurrent = expanded[pageIndex].toList()
                        item { Box(Modifier.height(16.dp)) }

                        if (isLoading[pageIndex]) item(key = "LoadIndicator") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .padding(top = 0.dp, start = 16.dp, end = 16.dp, bottom = 32.dp)
                                        .animateItem()
                                )
                            }
                        }

                        listCurrent.filter { it.events.isNotEmpty() }.forEach { day ->
                            val dayExpanded = expandedCurrent.contains(day.date)

                            val header: (@Composable LazyItemScope.() -> Unit) = {
                                val selected = selectedItems.containsAll(day.events.map { it.id })
                                val eventIds = day.events.map { it.id }
                                EventListHeader(
                                    historyDay = day.historyDay,
                                    selected = selected,
                                    expanded = dayExpanded,
                                    onExpandButtonClicked = {
                                        if (expandedCurrent.contains(day.historyDay.date))
                                            expanded[pageIndex].remove(day.historyDay.date)
                                        else
                                            expanded[pageIndex].add(day.historyDay.date)
                                    },
                                    modifier = Modifier
                                        .animateItem()
                                        .padding(bottom = if (dayExpanded) 0.dp else 16.dp)
                                        .combinedClickable(
                                            onClick = {
                                                if (!selectionMode) {
                                                    if (dayExpanded)
                                                        expanded[pageIndex].remove(day.date)
                                                    else
                                                        expanded[pageIndex].add(day.date)
                                                } else {
                                                    if (!selected) selectedItems.addAll(eventIds)
                                                    else selectedItems.removeAll(eventIds)
                                                }
                                                val distinct = selectedItems.distinct()
                                                selectedItems.clear()
                                                selectedItems.addAll(distinct)
                                            },
                                            onLongClick = listHeaderOnLongClick@{
                                                if (lock) return@listHeaderOnLongClick
                                                selectionMode = true
                                                if (selected) selectedItems.removeAll(eventIds)
                                                else selectedItems.addAll(eventIds)
                                            }
                                        )
                                ) {
                                    day.favorite = it
                                    day.save(context)
                                }
                            }

                            /*if (dayExpanded) */stickyHeader(key = "${day.date}_$selectedPage", content = header)
                            // else item(key = "${day.date}_$selectedPage", content = header)

                            val events = day.events.sortedByDescending { it.time }.let {
                                if (selectedPage == NanHistoryPages.Favorite && !day.favorite)
                                    it.filter { event -> event.favorite }
                                else if (selectedPage == NanHistoryPages.Search)
                                    it.filter { event -> filterSearchEvent(event) }
                                else
                                    it
                            }
                            if (dayExpanded) {
                                items(events.size, key = { "${events[it].id}_$selectedPage" }) {
                                    val eventData = events[it]
                                    val selected = selectedItems.contains(eventData.id)
                                    val lastItem = it + 1 >= events.size
                                    EventListItem(
                                        eventData,
                                        selected = selected,
                                        modifier = Modifier
                                            .padding(bottom = if (lastItem) 16.dp else 0.dp)
                                            .clip(
                                                if (lastItem) RoundedCornerShape(
                                                    bottomStart = 24.dp, bottomEnd = 24.dp
                                                )
                                                else RectangleShape
                                            )
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
//                            item {
////                                Box(Modifier.height(16.dp))
//                            }
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
                                val events = mutableListOf<HistoryEvent>()
                                for (day in HistoryFileData.getList(context)) {
                                    events.addAll(day.events)
                                }
                                events.filter { selectedItems.contains(it.id) }
                                    .forEachIndexed { index, event ->
                                        event.delete(context)
                                        deleteDialogText =
                                            "Deleting ${index + 1} of $selectedItemsCount"
                                    }
                            }
                            lock = false
                            resetSelectionMode()
                            loader(false)
                            closeDeleteDialog()
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