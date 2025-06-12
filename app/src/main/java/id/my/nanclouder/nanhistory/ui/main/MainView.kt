package id.my.nanclouder.nanhistory.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.VibratorManager
import android.provider.Settings
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import id.my.nanclouder.nanhistory.EditEventActivity
import id.my.nanclouder.nanhistory.EventDetailActivity
import id.my.nanclouder.nanhistory.ListFilters
import id.my.nanclouder.nanhistory.NanHistoryPages
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.lib.RecordStatus
import id.my.nanclouder.nanhistory.lib.ServiceBroadcast
import id.my.nanclouder.nanhistory.lib.history.HistoryEvent
import id.my.nanclouder.nanhistory.lib.history.HistoryFileData
import id.my.nanclouder.nanhistory.lib.history.MigrationState
import id.my.nanclouder.nanhistory.lib.history.delete
import id.my.nanclouder.nanhistory.lib.history.getDateFromFilePath
import id.my.nanclouder.nanhistory.lib.history.getFilePathFromDate
import id.my.nanclouder.nanhistory.lib.history.migrateLocationData
import id.my.nanclouder.nanhistory.lib.history.save
import id.my.nanclouder.nanhistory.service.RecordService
import id.my.nanclouder.nanhistory.ui.SearchAppBar
import id.my.nanclouder.nanhistory.ui.SelectionAppBar
import id.my.nanclouder.nanhistory.ui.list.EventListHeader
import id.my.nanclouder.nanhistory.ui.list.EventListItem
import id.my.nanclouder.nanhistory.ui.style.DangerButtonColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.round

fun MutableList<HistoryFileData>.insertSorted(data: HistoryFileData) {
    val index = binarySearch(data, compareByDescending { it.date })
    val insertIndex = if (index >= 0) index else -index - 1
    add(insertIndex, data)
}

class ServiceViewModel(context: Context) : ViewModel() {
    private var _context: Context? = null

    private val _serviceStatus = MutableStateFlow(RecordStatus.READY)
    val isServiceRunning: StateFlow<Int> = _serviceStatus.asStateFlow()

    private var _onBusy: ((String, String, Boolean) -> Unit)? = null
    private var _onRunning: ((String, String, Boolean) -> Unit)? = null
    private var _onStop: ((String, String, Boolean) -> Unit)? = null

    private val serviceStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ServiceBroadcast.ACTION_SERVICE_STATUS) {
                val status = intent.getIntExtra(ServiceBroadcast.EXTRA_STATUS, RecordStatus.READY)
                val eventId = intent.getStringExtra(ServiceBroadcast.EXTRA_EVENT_ID) ?: ""
                val eventPath = intent.getStringExtra(ServiceBroadcast.EXTRA_EVENT_PATH) ?: ""
                val eventPoint = intent.getBooleanExtra(ServiceBroadcast.EXTRA_EVENT_POINT, false)
                _serviceStatus.value = status
                when (status) {
                    RecordStatus.RUNNING -> _onRunning?.invoke(eventId, eventPath, eventPoint)
                    RecordStatus.READY -> _onStop?.invoke(eventId, eventPath, eventPoint)
                    RecordStatus.BUSY -> _onBusy?.invoke(eventId, eventPath, eventPoint)
                }
            }
        }
    }

    private fun loadStatus() {
        val context = _context ?: return
        _serviceStatus.value = if (context.getSharedPreferences("recordEvent", Context.MODE_PRIVATE)
            .getBoolean("isRunning", false)) RecordStatus.RUNNING else RecordStatus.READY
    }

    init {
        _context = context
        val filter = IntentFilter(ServiceBroadcast.ACTION_SERVICE_STATUS)
        context.registerReceiver(serviceStatusReceiver, filter, Context.RECEIVER_EXPORTED)
        loadStatus()
    }

    override fun onCleared() {
        val context = _context ?: return
        super.onCleared()
        context.unregisterReceiver(serviceStatusReceiver)
    }

    fun setOnRunning(block: (String, String, Boolean) -> Unit) { _onRunning = block }
    fun setOnStop(block: (String, String, Boolean) -> Unit) { _onStop = block }

    fun reloadStatus() = ::loadStatus
    fun clear() {
        onCleared()
        _context = null
    }
}

class ServiceViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ServiceViewModel(context.applicationContext) as T
    }
}

@SuppressLint("BatteryLife")
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

    var permissionDialogState by remember { mutableStateOf(false) }
    var permissionDialogTitle by remember { mutableStateOf("") }
    var permissionDialogText by remember { mutableStateOf("") }
    var permissionDialogOnConfirm by remember { mutableStateOf({ }) }

    val context = LocalContext.current

    val recordPermissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.POST_NOTIFICATIONS
        ).let {
            if (Config.experimentalAudioRecord.get(context))
                it + Manifest.permission.RECORD_AUDIO
            else it
        }
    ) { permissions ->
        if (permissions.all { it.value }) {
            permissionDialogTitle = "Location Permission Needed"
            permissionDialogText = "Go to \"Permission\" > \"Location\" > \"Allow all the time\""
            permissionDialogState = true
            permissionDialogOnConfirm = { launchRequestBackgroundLocation(context) }
        }
    }

    val recordViewModel = remember { ServiceViewModel(context) }
    var recordButtonDisabled by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            recordViewModel.clear()
        }
    }

    val haptic = LocalHapticFeedback.current
    val vibrator = (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
        .defaultVibrator

//    var isRecording by rememberSaveable { mutableStateOf(false) }
//    isRecording = RecordService.isRunning(LocalContext.current)
    val isRecording = recordViewModel.isServiceRunning.collectAsState()

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

    recordViewModel.setOnStop { _, eventPath, _ ->
        updateData(getDateFromFilePath(eventPath) ?: LocalDate.now())
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        recordButtonDisabled = false
    }
    recordViewModel.setOnRunning { _, eventPath, eventPoint ->
        updateData(getDateFromFilePath(eventPath) ?: LocalDate.now())
        scope.launch {
            delay(500L)
            if (!eventPoint) recordButtonDisabled = false
        }
    }

    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (!recordPermissionState.allPermissionsGranted)
            recordPermissionState.launchMultiplePermissionRequest()
    }

    var migrationState by remember { mutableStateOf(MigrationState(0f, true)) }

    LaunchedEffect(Unit) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val packageName = context.packageName
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            permissionDialogTitle = "Ignore Battery Optimization"
            permissionDialogText = "This app needs to be excluded from battery optimization to work properly. Click \"Allow\" to proceed."
            permissionDialogOnConfirm = {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                batteryOptimizationLauncher.launch(intent)
            }
            permissionDialogState = true
        }
        else if (!recordPermissionState.allPermissionsGranted)
            recordPermissionState.launchMultiplePermissionRequest()

        migrateLocationData(context) { migrationState = it }
        viewModels.forEach { it.value?.reload(context) }
    }

    if (!migrationState.finish) AlertDialog (
        onDismissRequest = {},
        dismissButton = {},
        confirmButton = {},
        text = {
            Column {
                Text("Migrating Data Structures...")
                Box(Modifier.height(8.dp))
                LinearProgressIndicator()
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.End
//                ) {
//                    Text("${round(migrationState.progress * 100)}%")
//                }
            }
        }
    )

    if (permissionDialogState) AlertDialog(
        onDismissRequest = {
            permissionDialogState = false
        },
        title = { Text(permissionDialogTitle) },
        text = { Text(permissionDialogText) },
        confirmButton = {
            Button({ permissionDialogOnConfirm(); permissionDialogState = false }) { Text("Continue") } // TODO
        },
    )

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
                    val interactionSource = remember { MutableInteractionSource() }
                    val viewConfiguration = LocalViewConfiguration.current

                    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                        if (result.resultCode == 1) {
                            result.data?.getStringExtra("path")?.let { path ->
                                val date = getDateFromFilePath(path) ?: LocalDate.now()
                                updateData(date)
                            }
                        }
                    }

                    val record = record@{ eventPoint: Boolean ->
                        if (recordButtonDisabled) return@record
                        Log.d("NanHistoryDebug", "RECORD, eventPoint: $eventPoint")
                        val intent = Intent(context, RecordService::class.java)
                        val now = ZonedDateTime.now()
                        recordButtonDisabled = true
                        if (isRecording.value == RecordStatus.RUNNING) {
                            context.stopService(intent)
                            scope.launch {
                                delay(100L)
                                sharedPreferences.edit()
                                    .putBoolean("isRunning", false)
                                    .remove("eventId")
                                    .apply()
                                delay(100L)
                                recordViewModel.reloadStatus()
                            }
                        } else {
                            if (recordPermissionState.allPermissionsGranted) {
                                if (eventPoint)
                                    intent.putExtra("eventPoint", true)

                                if (!eventPoint) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                else vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))

                                context.startForegroundService(intent)
                            } else {
                                recordButtonDisabled = false
                                recordPermissionState.launchMultiplePermissionRequest()
                            }
                        }
                    }

                    LaunchedEffect(interactionSource) {
                        var isLongClick = false

                        interactionSource.interactions.collectLatest { interaction ->
                            when (interaction) {
                                is PressInteraction.Press -> {
                                    isLongClick = false
                                    delay(viewConfiguration.longPressTimeoutMillis)
                                    isLongClick = true
                                    record(true)
                                }
                                is PressInteraction.Release -> {
                                    if (!isLongClick) record(false)
                                }
                                is PressInteraction.Cancel -> {
                                    isLongClick = false
                                }
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
                        interactionSource = interactionSource,
                        onClick = {
                            // Handled by interactionSource
                        }
                    ) {
                        val color =
                            if (recordButtonDisabled) Color(0x80808080)
                            else if (isRecording.value == RecordStatus.RUNNING) MaterialTheme.colorScheme.error
                            else LocalContentColor.current
                        if (isRecording.value == RecordStatus.RUNNING)
                            Icon(
                                painterResource(R.drawable.ic_stop_filled),
                                "Record event button",
                                tint = color
                            )
                        else
                            Icon(
                                painterResource(R.drawable.ic_circle_filled),
                                "Record event button",
                                tint = color
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
                            if (isLoading) CircularProgressIndicator(
                                Modifier
                                    .fillMaxSize()
                                    .padding(8.dp))
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

//            loader = loadTop@{ updateExpanded ->
//                firstLoad = true
//                val previousJob = loaderJob[filterIndex]
//                val currentIndex = filterIndex
//                val streamDataListEnabled = Config.experimentalStreamList.get(context)
//                loaderJob[currentIndex] = scope.launch load@{
//                    loaderActive[currentIndex] = true
//                    val replaceIfExists = { data: HistoryFileData ->
//                        val find = fileDataList[currentIndex].indexOfFirst { it.date == data.date }
//                        if (find != -1)
//                            fileDataList[currentIndex][find] = data
//                        else
//                            fileDataList[currentIndex].add(data)
//                    }
//
//                    previousJob?.cancelAndJoin()
//                    lock = true
//                    withContext(Dispatchers.IO) {
////                        if (streamDataListEnabled) fileDataList[currentIndex].clear()
//                        if (updateExpanded) expanded[currentIndex].clear()
//                        if (!streamDataListEnabled) when (appliedFilter) {
//                            ListFilters.Favorite -> HistoryFileData
//                                .getList(context)
//                                .filter { it.favorite || it.events.find { event -> event.favorite } != null }
//                                .sortedByDescending { it.date }
//
//                            ListFilters.All -> HistoryFileData
//                                .getList(context)
//                                .sortedByDescending { it.date }
//
////                            ListFilters.Search -> HistoryFileData
////                                .getList(context)
////                                .filter {
////                                    it.events.find { event ->
////                                        filterSearchEvent(event)
////                                    } != null
////                                }
////                                .sortedByDescending { it.date }
//
//                            else -> HistoryFileData
//                                .getList(context)
//                                .sortedByDescending { it.date }
//                        }.let {
//                            fileDataList[currentIndex].let { list ->
//                                list.clear()
//                                list.addAll(it)
//                            }
//                        }
//                        else when (appliedFilter) {
//                            ListFilters.Favorite -> HistoryFileData
//                                .getListStream(context, from = fromZero).apply {
//                                    sortedByDescending { getDateFromFilePath(it.absolutePath) }
//                                }.forEachAsync {
//                                    if (it.favorite || it.events.find { event -> event.favorite } != null &&
//                                        fileDataList[currentIndex].find { day -> day.date == it.date } == null) replaceIfExists(it)
//                                }
//
//                            ListFilters.All -> HistoryFileData
//                                .getListStream(context, from = fromZero).apply {
//                                    sortedByDescending { getDateFromFilePath(it.absolutePath) }
//                                }.forEachAsync {
//                                    if (fileDataList[currentIndex].find { day -> day.date == it.date } == null)
//                                        replaceIfExists(it)
//                                }
//
////                            ListFilters.Search -> {
////                                fileDataList[currentIndex].clear()
////                                if (searchQuery.isNotBlank()) HistoryFileData
////                                    .getListStream(context, from = fromZero).apply {
////                                        sortedByDescending { getDateFromFilePath(it.absolutePath) }
////                                    }.forEachAsync {
////                                        if (it.events.find { event ->
////                                            filterSearchEvent(event) && fileDataList[currentIndex].find { day -> day.date == it.date } == null
////                                        } != null) {
////                                            replaceIfExists(it)
////                                        }
////                                    }
////                                else Unit
////                            }
//
//                            else -> HistoryFileData
//                                .getListStream(context, from = fromRecent).apply {
//                                    sortedByDescending { getDateFromFilePath(it.absolutePath) }
//                                }.forEachAsync { replaceIfExists(it) }
//                        }
//                    }
//                    loaderActive[currentIndex] = false
//                    lock = false
////                    isLoading[currentIndex] = false
//                }
//            }

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

fun launchRequestBackgroundLocation(context: Context) {
    val granted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    if (!granted) {
        val locationPermissionIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(locationPermissionIntent)
    }
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

//    Log.d("NanHistoryDebug", "RECORDING: $isRecording, EVENT ID: $recordEventId")

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
                val sharedPreferences = LocalContext.current.applicationContext
                    .getSharedPreferences("recordEvent", Context.MODE_PRIVATE)
                val recordEventId = sharedPreferences.getString("eventId", "")

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
                            val recording = recordEventId == eventData.id
                            val selected = selectedItems.contains(eventData)
                            val lastItem = index + 1 >= events.size

                            if (eventData.metadata["recording"] == true) {
                                eventData.metadata.remove("recording")
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
                                recording = recording,
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