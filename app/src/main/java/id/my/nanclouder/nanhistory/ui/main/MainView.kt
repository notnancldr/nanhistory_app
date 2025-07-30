package id.my.nanclouder.nanhistory.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.edit
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
import id.my.nanclouder.nanhistory.lib.history.getFilePathFromDate
import id.my.nanclouder.nanhistory.service.RecordService
import id.my.nanclouder.nanhistory.ui.SearchAppBar
import id.my.nanclouder.nanhistory.ui.SelectionAppBar
import id.my.nanclouder.nanhistory.ui.list.EventListHeader
import id.my.nanclouder.nanhistory.ui.list.EventListItem
import id.my.nanclouder.nanhistory.ui.style.DangerButtonColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZonedDateTime
import androidx.core.net.toUri
import id.my.nanclouder.nanhistory.TagDetailActivity
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toHistoryTag
import id.my.nanclouder.nanhistory.db.toTagEntity
import id.my.nanclouder.nanhistory.lib.DateFormatter
import id.my.nanclouder.nanhistory.lib.history.HistoryTag
import id.my.nanclouder.nanhistory.lib.withHaptic
import id.my.nanclouder.nanhistory.service.RecordTileService
import id.my.nanclouder.nanhistory.state.SelectionState
import id.my.nanclouder.nanhistory.state.rememberSelectionState
import id.my.nanclouder.nanhistory.ui.ColorIcon
import id.my.nanclouder.nanhistory.ui.ComponentPlaceholder
import id.my.nanclouder.nanhistory.ui.SelectableButton
import id.my.nanclouder.nanhistory.ui.TagDetailDialog
import id.my.nanclouder.nanhistory.ui.TagEditorDialog
import id.my.nanclouder.nanhistory.ui.TagPickerDialog
import id.my.nanclouder.nanhistory.ui.rememberTagDetailDialogState
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.stream.consumeAsFlow
import kotlin.math.roundToInt

fun MutableList<HistoryFileData>.insertSorted(data: HistoryFileData) {
    val index = binarySearch(data, compareByDescending { it.date })
    val insertIndex = if (index >= 0) index else -index - 1
    add(insertIndex, data)
}

class ReceiverViewModel(context: Context) : ViewModel() {
    private var _context: Context? = null

    private val _serviceStatus = MutableStateFlow(RecordStatus.READY)
    val isServiceRunning: StateFlow<Int> = _serviceStatus.asStateFlow()

    private var _onBusy: ((String, String, Boolean) -> Unit)? = null
    private var _onRunning: ((String, String, Boolean) -> Unit)? = null
    private var _onStop: ((String, String, Boolean) -> Unit)? = null

    private var _onUpdateData: (() -> Unit)? = null

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
            else if (intent?.action == ServiceBroadcast.ACTION_UPDATE_DATA) {
                _onUpdateData?.invoke()
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

    fun setOnUpdateData(block: () -> Unit) { _onUpdateData = block }

    fun reloadStatus() = ::loadStatus
    fun clear() {
        onCleared()
        _context = null
    }
}

class ServiceViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ReceiverViewModel(context.applicationContext) as T
    }
}

fun requestAddTile(context: Context, onResult: ((Int) -> Unit)? = null) {
    val component = ComponentName(context, RecordTileService::class.java)
    val statusBarService = context.getSystemService(StatusBarManager::class.java)

    Log.d("RecordTileService", "requestAddTile")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        statusBarService.requestAddTileService(
            component,
            "Record Event",
            android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_event_record),
            { }
        ) { result ->
            if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED)
                Toast.makeText(context, "Tile added!", Toast.LENGTH_SHORT).show()

            onResult?.invoke(result)
        }
    }
}

@SuppressLint("BatteryLife")
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun MainView() {
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { NanHistoryPages.entries.size }
    )

    var appliedFilter by rememberSaveable { mutableStateOf(ListFilters.Recent) }
    val selectedPage = NanHistoryPages.entries[pagerState.currentPage]

    var selectedFavorite by rememberSaveable { mutableIntStateOf(0) }

    val filterIndex =
        if (pagerState.currentPage != NanHistoryPages.Search.ordinal) appliedFilter.ordinal
        else 3

    var needUpdate by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val qsSharedPreferences = LocalContext.current.applicationContext
        .getSharedPreferences("qsTile", Context.MODE_PRIVATE)

    var loader by remember { mutableStateOf({ _: Boolean -> }) }
    var searchQuery by remember { mutableStateOf("") }

    var permissionDialogState by remember { mutableStateOf(false) }
    var permissionDialogTitle by remember { mutableStateOf("") }
    var permissionDialogText by remember { mutableStateOf("") }
    var permissionDialogOnConfirm by remember { mutableStateOf({ }) }

    val context = LocalContext.current

    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

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

    val recordStatus by RecordService.RecordState.status.collectAsState()

    val viewModels = List(4) {
        remember { mutableStateOf<EventListViewModel?>(null) }
    }

    val eventsViewModel = rememberEventListViewModel()
    val searchViewModel = rememberEventListViewModel(mode = EventSelectMode.Search)
    val favoriteEventsViewModel = rememberEventListViewModel(mode = EventSelectMode.FavoriteEvent)
    val favoriteDayViewModel = rememberEventListViewModel(mode = EventSelectMode.FavoriteDay)

    val onGoto = { it: LocalDate ->
        scope.launch {
            pagerState.scrollToPage(NanHistoryPages.Events.ordinal)
            eventsViewModel.gotoDay(it)
        }
        Unit
    }

    searchViewModel.onGotoDay {
        scope.launch {
            pagerState.scrollToPage(NanHistoryPages.Events.ordinal)
            eventsViewModel.gotoDay(it)
        }
    }
    favoriteEventsViewModel.onGotoDay(onGoto)
    favoriteDayViewModel.onGotoDay(onGoto)

    val eventsSelectionState = rememberSelectionState<HistoryEvent>()
    val searchSelectionState = rememberSelectionState<HistoryEvent>()
    val tagListSelectionState = rememberSelectionState<HistoryTag>()
    val favoriteEventSelectionState = rememberSelectionState<HistoryEvent>()
    val favoriteDaySelectionState = rememberSelectionState<HistoryEvent>()

    val haptic = LocalHapticFeedback.current
    val vibrator = (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
        .defaultVibrator

    var deleteDialogState by remember { mutableStateOf(false) }
    var deleteDialogTitle by remember { mutableStateOf("") }
    var deleteDialogText by remember { mutableStateOf("") }
    var deleteButtonState by remember { mutableStateOf(false) }

    var tagEditorDialogState by remember { mutableStateOf(false) }
    var tagPickerDialogState by remember { mutableStateOf(false) }

    var lock by remember { mutableStateOf(false) }

    val resetSelectionMode = resetSelectionMode@{
        if (lock) return@resetSelectionMode
        when (selectedPage) {
            NanHistoryPages.Events -> eventsSelectionState.reset()
            NanHistoryPages.Search -> searchSelectionState.reset()
            NanHistoryPages.Tags -> tagListSelectionState.reset()
            NanHistoryPages.Favorite -> {
                if (selectedFavorite == 0) favoriteEventSelectionState.reset()
                else favoriteDaySelectionState.reset()
            }
        }
    }

    val selectAll = selectAll@{
        if (lock) return@selectAll
        when (selectedPage) {
            NanHistoryPages.Events -> {
                eventsSelectionState.clear()
                eventsSelectionState.selectAll(eventsViewModel.events.value)
            }
            NanHistoryPages.Search -> {
                searchSelectionState.clear()
                searchSelectionState.selectAll(searchViewModel.events.value)
            }
            NanHistoryPages.Favorite -> {
                if (selectedFavorite == 0) {
                    favoriteEventSelectionState.clear()
                    favoriteEventSelectionState.selectAll(favoriteEventsViewModel.events.value)
                }
                else {
                    favoriteDaySelectionState.clear()
                    favoriteDaySelectionState.selectAll(favoriteDayViewModel.events.value)
                }
            }
            // NanHistoryPages.Tags -> tagListSelectionState.selectAll()
            else -> {}
        }
    }

    val selectedEvents by when (selectedPage) {
        NanHistoryPages.Events -> eventsSelectionState.selectedItems
        NanHistoryPages.Favorite -> favoriteEventSelectionState.selectedItems
        NanHistoryPages.Search -> searchSelectionState.selectedItems
        else -> listOf(emptyList<HistoryEvent>()).stream().consumeAsFlow()
    }.collectAsState(emptyList())

    val selectedDays by favoriteDaySelectionState.selectedItems.collectAsState(emptyList())
    val selectedTags by tagListSelectionState.selectedItems.collectAsState(emptyList())

    val selectedItemsSize by when (selectedPage) {
        NanHistoryPages.Events -> eventsSelectionState.selectedItems
        NanHistoryPages.Favorite -> {
            if (selectedFavorite == 0) favoriteEventSelectionState.selectedItems
            else favoriteDaySelectionState.selectedItems
        }
        NanHistoryPages.Tags -> tagListSelectionState.selectedItems
        NanHistoryPages.Search -> searchSelectionState.selectedItems
    }.map { it.size }.collectAsState(0)

    val selectionMode by when (selectedPage) {
        NanHistoryPages.Events -> eventsSelectionState.isSelectionMode
        NanHistoryPages.Favorite -> {
            if (selectedFavorite == 0) favoriteEventSelectionState.isSelectionMode
            else favoriteDaySelectionState.isSelectionMode
        }
        NanHistoryPages.Tags -> tagListSelectionState.isSelectionMode
        NanHistoryPages.Search -> searchSelectionState.isSelectionMode
    }.collectAsState()

    val openDeleteDialog = {
        val size = selectedItemsSize
        deleteDialogTitle =
            "Move items to Trash"
        deleteDialogText =
            "Do you want to move $size item${(if (size < 2) "" else "s")} to Trash?"
        deleteButtonState = true
        deleteDialogState = true
    }

    val closeDeleteDialog = {
        deleteDialogState = false
        deleteButtonState = false
        deleteDialogTitle = ""
        deleteDialogText = ""
    }

    val isLoading = viewModels[filterIndex].value?.isLoading?.collectAsState()?.value ?: false

    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (!recordPermissionState.allPermissionsGranted)
            recordPermissionState.launchMultiplePermissionRequest()
    }

    var migrationState by remember { mutableStateOf(MigrationState(0f, true)) }
    var migrationName by remember { mutableStateOf("") }

    var recordTileAdded by remember {
        mutableStateOf(qsSharedPreferences.getBoolean("tileAdded", false))
    }

    LaunchedEffect(Unit) {
        Log.d("NanHistoryDebug", "LaunchedEffect")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val packageName = context.packageName
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            permissionDialogTitle = "Ignore Battery Optimization"
            permissionDialogText = "This app needs to be excluded from battery optimization to work properly. Click \"Allow\" to proceed."
            permissionDialogOnConfirm = {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:$packageName".toUri()
                }
                batteryOptimizationLauncher.launch(intent)
            }
            permissionDialogState = true
        }
        else if (!recordPermissionState.allPermissionsGranted)
            recordPermissionState.launchMultiplePermissionRequest()

        // migrateData(context) { state, name ->
        //     migrationState = state
        //     migrationName = name
        // }

        val addTileRequested = qsSharedPreferences.getBoolean("addTileRequested", false)
        if (!addTileRequested) requestAddTile(context) {
            qsSharedPreferences.edit {
                putBoolean("addTileRequested", true)
            }
            if (it == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED)
                recordTileAdded = true
        }


        // viewModels.forEach { it.value?.reload() }
    }

    if (!migrationState.finish) BasicAlertDialog(
    // BasicAlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .requiredWidthIn(max = 296.dp)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "rotation")

                val iconAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "iconAlpha"
                )

                Icon(
                    painterResource(R.drawable.ic_cloud_download),
                    "Updating Data",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(88.dp)
                        .alpha(iconAlpha)
                )
                Box(Modifier.height(8.dp))
                Text(
                    "Updating Data",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Box(Modifier.height(8.dp))
                Text("Migrating data: $migrationName", textAlign = TextAlign.Center)
                Box(Modifier.height(8.dp))
                val primaryColor = MaterialTheme.colorScheme.primary
                val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .padding(vertical = 4.dp)
                ) {
                    drawLine(
                        color = primaryContainerColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = size.height,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = primaryColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width * migrationState.progress, 0f),
                        strokeWidth = size.height,
                        cap = StrokeCap.Round
                    )
                }
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.End
//                ) {
//                    Text("${round(migrationState.progress * 100)}%")
//                }
            }
        }
    }

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
            containerColor = MaterialTheme.colorScheme.surface,
            floatingActionButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val viewConfiguration = LocalViewConfiguration.current

                    val recordButtonDisabled = recordStatus == RecordStatus.BUSY
                    var previousRecordState by remember { mutableIntStateOf(recordStatus) }

                    if (recordStatus != previousRecordState) {
                        if (recordStatus == RecordStatus.READY && previousRecordState == RecordStatus.BUSY)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        previousRecordState = recordStatus
                    }

                    Log.d("NanHistoryDebug", "RECORD STATUS: $recordStatus")

                    val record = record@{ eventPoint: Boolean ->
                        if (recordStatus == RecordStatus.BUSY) return@record
                        Log.d("NanHistoryDebug", "RECORD, status: $recordStatus")
                        val intent = Intent(context, RecordService::class.java)
                        val now = ZonedDateTime.now()
                        if (recordStatus == RecordStatus.RUNNING) {
                            intent.setAction(RecordService.ACTION_RECORD_STOP)
                            context.startService(intent)
                        } else {
                            if (recordPermissionState.allPermissionsGranted) {
                                if (eventPoint)
                                    intent.putExtra("eventPoint", true)

                                if (!eventPoint) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                else vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))

                                intent.setAction(RecordService.ACTION_RECORD_START)
                                context.startForegroundService(intent)
                            } else {
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
                            context.startActivity(intent)
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
                            else if (recordStatus == RecordStatus.RUNNING) MaterialTheme.colorScheme.error
                            else LocalContentColor.current
                        if (recordStatus == RecordStatus.RUNNING)
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
                    selectedItemsSize, { resetSelectionMode() }
                ) {
                    if (selectedPage == NanHistoryPages.Tags && selectedItemsSize == 1) {
                        IconButton(onClick = { tagEditorDialogState = true }) {
                            Icon(Icons.Rounded.Edit, "Edit tag")
                        }
                    }
                    if (selectedPage != NanHistoryPages.Tags) {
                        IconButton(onClick = {
                            selectAll()
                        }) {
                            Icon(painterResource(R.drawable.ic_select_all), "Select all")
                        }
                        IconButton(onClick = {
                            tagPickerDialogState = true
                        }) {
                            Icon(painterResource(R.drawable.ic_tag), "Add tag")
                        }
                        IconButton(onClick = {
                            // Log.d("NanHistoryDebug", "SELECTED ITEMS: ${selectedItems.toList()}")
                            scope.launch {
                                lock = true
                                if (selectedPage == NanHistoryPages.Events) withContext(Dispatchers.IO) {
                                    eventsSelectionState.selectedItems.value.forEach {
                                        dao.toggleFavoriteEvent(it.id)
                                    }
                                }
                                lock = false
                                resetSelectionMode()
                            }
                        }) {
                            Icon(painterResource(R.drawable.ic_favorite), "Favorite")
                        }
                    }
                    IconButton(onClick = { openDeleteDialog() }) {
                        Icon(painterResource(R.drawable.ic_delete), "Delete")
                    }
                }
                else if (selectedPage == NanHistoryPages.Search) SearchAppBar(
                    isLoading = isLoading,
                    onSearch = {
                        searchViewModel.search(it)
                    },
                    onChange = {
                        searchViewModel.search(it)
                    },
                    onCancel = {
                        searchViewModel.cancelLoading()
                    }
                )
                else TopAppBar(
                    title = { Text(selectedPage.name) },
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch { drawerState.open() }
                            }
                        ) { Icon(Icons.Rounded.Menu, "Open sidebar") }
                    },
                    actions = {
                        if (selectedPage == NanHistoryPages.Tags) {
                            IconButton(onClick = {
                                tagEditorDialogState = true
                            }) {
                                Icon(Icons.Rounded.Add, "Add tag")
                            }
                        }
                        if (!recordTileAdded) IconButton(onClick = {
                            requestAddTile(context) {
                                if (it == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED)
                                    recordTileAdded = true
                            }
                        }) {
                            Icon(painterResource(R.drawable.ic_event_record), "Add record tile to quick settings")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    val navigationOnClick = { page: NanHistoryPages ->
                        navOnClick@{
                            if (selectedPage == page) return@navOnClick
                            // selectedPage = page
                            scope.launch {
                                pagerState.animateScrollToPage(page.ordinal)
                            }
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
                        selected = selectedPage == NanHistoryPages.Favorite,
                        onClick = navigationOnClick(NanHistoryPages.Favorite),
                        icon = {
                            val icon = painterResource(
                                if (selectedPage == NanHistoryPages.Favorite) R.drawable.ic_favorite_filled
                                else R.drawable.ic_favorite
                            )
                            Icon(icon, "Favorite")
                        },
                        label = {
                            Text("Favorite")
                        }
                    )
                    NavigationBarItem(
                        selected = selectedPage == NanHistoryPages.Tags,
                        onClick = navigationOnClick(NanHistoryPages.Tags),
                        icon = {
                            val icon = painterResource(
                                R.drawable.ic_tag
                            )
                            Icon(icon, "Tags")
                        },
                        label = {
                            Text("Tags")
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

            val updateBuffer = remember { mutableStateListOf<HistoryFileData>() }
            val expandedBuffer = remember { mutableStateListOf<LocalDate>() }

            BackHandler(
                enabled = selectionMode,
            ) {
                resetSelectionMode()
            }

            Column(
                modifier = Modifier
                    .padding(paddingValues)
            ) {
                val eventsLazyListState = rememberLazyListState()
                val tagsLazyListState = rememberLazyListState()
                val searchLazyListState = rememberLazyListState()
                val favoriteEventListState = rememberLazyListState()
                val favoriteDayListState = rememberLazyListState()

                HorizontalPager(
                    state = pagerState
                ) { page ->
                    Log.d("NanHistoryDebug", "PAGE: $page")
                    when (page) {
                        NanHistoryPages.Events.ordinal -> key(Unit) {
                            EventList(
                                viewModel = eventsViewModel,
                                lazyListState = eventsLazyListState,
                                selectionState = eventsSelectionState,
                                onEmptyContent = {
                                    Image(
                                        painter = painterResource(
                                            if (isSystemInDarkTheme()) R.drawable.hint_no_event_dark
                                            else R.drawable.hint_no_event
                                        ),
                                        modifier = Modifier
                                            .height(196.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outline,
                                                shape = RoundedCornerShape(16.dp)
                                            ),
                                        contentDescription = null
                                    )

                                    Box(Modifier.height(8.dp))

                                    Text(
                                        text = "No events here",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    val addIconId = "addIcon"
                                    val recordIconId = "recordIcon"

                                    val text = buildAnnotatedString {
                                        append("Record an event by pressing ")
                                        appendInlineContent(recordIconId, "record icon.")
                                        append(" or add new event by clicking ")
                                        appendInlineContent(addIconId, "add icon")
                                    }
                                    val inlineContent = mapOf(
                                        addIconId to InlineTextContent(
                                            Placeholder(
                                                width = 16.sp,
                                                height = 16.sp,
                                                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Add,
                                                contentDescription = "Add icon",
                                                tint = Color.Gray,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        },
                                        recordIconId to InlineTextContent(
                                            Placeholder(
                                                width = 16.sp,
                                                height = 16.sp,
                                                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                                            )
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_circle_filled),
                                                contentDescription = "Record icon",
                                                tint = Color.Gray,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    )

                                    Text(
                                        text = text,
                                        inlineContent = inlineContent,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                        NanHistoryPages.Favorite.ordinal -> {
                            val onClick = { index: Int ->
                                if (selectedFavorite != index) selectedFavorite = index
                            }
                            val selector = @Composable {
                                val onClick1 = { onClick(0) }
                                val onClick2 = { onClick(1) }

                                val buttonContentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)

                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SelectableButton(
                                        selected = selectedFavorite == 0,
                                        onClick = onClick1,
                                        contentPadding = buttonContentPadding,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Events")
                                    }
                                    SelectableButton(
                                        selected = selectedFavorite == 1,
                                        onClick = onClick2,
                                        contentPadding = buttonContentPadding,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Days")
                                    }
                                }
                            }

                            val onEmptyContent: @Composable (ColumnScope.() -> Unit) = {
                                Image(
                                    painter = painterResource(
                                        if (isSystemInDarkTheme()) R.drawable.hint_no_favorite_dark
                                        else R.drawable.hint_no_favorite
                                    ),
                                    modifier = Modifier
                                        .height(196.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    contentDescription = null
                                )

                                Box(Modifier.height(8.dp))

                                Text(
                                    text = "No favorite items",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val favoriteIconId = "favoriteIcon"

                                val text = buildAnnotatedString {
                                    append("You can add favorite items by selecting the items then click ")
                                    appendInlineContent(favoriteIconId, "favorite icon")
                                    append(" or by clicking ")
                                    appendInlineContent(favoriteIconId, "favorite icon")
                                    append(" at top right corner of event detail.")
                                }
                                val inlineContent = mapOf(
                                    favoriteIconId to InlineTextContent(
                                        Placeholder(
                                            width = 16.sp,
                                            height = 16.sp,
                                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                                        )
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_favorite),
                                            contentDescription = "Favorite icon",
                                            tint = Color.Gray,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    },
                                )

                                Text(
                                    text = text,
                                    inlineContent = inlineContent,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column {
                                selector()
                                if (selectedFavorite == 0) EventList(
                                    viewModel = favoriteEventsViewModel,
                                    lazyListState = favoriteEventListState,
                                    selectionState = favoriteEventSelectionState,
                                    onEmptyContent = onEmptyContent
                                )
                                else EventList(
                                    viewModel = favoriteDayViewModel,
                                    lazyListState = favoriteDayListState,
                                    selectionState = favoriteDaySelectionState,
                                    onEmptyContent = onEmptyContent
                                    // topItem = { selector() }
                                )
                            }
                        }
                        NanHistoryPages.Tags.ordinal -> TagList(
                            lazyListState = tagsLazyListState,
                            selectionState = tagListSelectionState
                        )
                        NanHistoryPages.Search.ordinal -> EventList(
                            viewModel = searchViewModel,
                            lazyListState = searchLazyListState,
                            selectionState = searchSelectionState
                        )
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
                        if (selectedPage != NanHistoryPages.Tags) scope.launch {
                            lock = true
                            deleteButtonState = false
                            deleteDialogTitle = "Moving items"
                            deleteDialogText = "Moving..."
                            withContext(Dispatchers.IO) {
                                val selectedItems = when (pagerState.currentPage) {
                                    NanHistoryPages.Events.ordinal -> eventsSelectionState.selectedItems.value
                                    NanHistoryPages.Search.ordinal -> searchSelectionState.selectedItems.value
                                    else -> emptyList()
                                }
                                deleteDialogText =
                                    "Moving items to trash..."
                                AppDatabase.moveToTrash(dao, context, selectedItems.map { it.id })
                            }
                            lock = false
                            resetSelectionMode()
                            vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                            closeDeleteDialog()
                        }
                        else scope.launch {
                            lock = true
                            deleteButtonState = false
                            deleteDialogTitle = "Deleting Tags"
                            deleteDialogText = "Deleting..."
                            withContext(Dispatchers.IO) {
                                val selectedItems = tagListSelectionState.selectedItems.value
                                deleteDialogText =
                                    "Deleting tags..."
                                dao.deleteTags(selectedItems.map { it.toTagEntity() })
                            }
                            lock = false
                            resetSelectionMode()
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

    TagPickerDialog(
        state = tagPickerDialogState,
        onDismissRequest = {
            resetSelectionMode()
            tagPickerDialogState = false
        },
        eventIds = selectedEvents.map { it.id }
    )

    TagEditorDialog(
        state = tagEditorDialogState,
        onDismissRequest = {
            tagEditorDialogState = false
            resetSelectionMode()
        },
        tagId = selectedTags.firstOrNull()?.id
    )

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
    selectionState: SelectionState<HistoryEvent>,
    loadHeaderData: Boolean = true,
    lazyListState: LazyListState = rememberLazyListState(),
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
    topItem: (@Composable LazyItemScope.() -> Unit)? = null,
    onEmptyContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val expanded = remember { mutableStateListOf<LocalDate>() }

    val tagDetailDialogState = rememberTagDetailDialogState()

    val db = remember { AppDatabase.getInstance(context) }
    val dao = remember { db.appDao() }

    val dayList by viewModel.days.collectAsState(viewModel.currentDays)
    val eventListState by viewModel.events.collectAsState(viewModel.currentEvents)

    val dateList = dayList.map { it.date }

    val eventList = eventListState.filter { event -> event.time.toLocalDate() in dateList }

    val haptic = LocalHapticFeedback.current

    val selectionMode by selectionState.isSelectionMode.collectAsState()
    val selectedItems by selectionState.selectedItems.collectAsState()

    var highlightedDay by remember { mutableStateOf<LocalDate?>(null) }

    var gotoDay by remember { mutableStateOf<LocalDate?>(null) }

    if (viewModel.mode == EventSelectMode.Default) viewModel.onGotoDay { date ->
        gotoDay = date
    }

    LaunchedEffect(gotoDay, dayList, eventList) {
        if (gotoDay != null && dayList.isNotEmpty() && eventList.isNotEmpty()) {

            highlightedDay = gotoDay

            val grouped = eventList.groupBy {
                dayList.find { day -> day.date == it.time.toLocalDate() }!!
            }

            // Log.d("NanHistoryDebug", "GROUPED: $grouped")

            var index = 0
            for ((day, events) in grouped) {
                if (day.date == gotoDay) {
                    lazyListState.scrollToItem(index)
                    break
                }
                index += events.size + 1
            }
        }
        delay(200L)
        gotoDay = null
    }

//    Log.d("NanHistoryDebug", "RECORDING: $isRecording, EVENT ID: $recordEventId")

    if (!selectionMode) selectionState.clear()

    if (eventList.isNotEmpty()) Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .selectableGroup()
                .fillMaxSize()
                .zIndex(-1f),
            state = lazyListState,
        ) {
            val expandedCurrent = expanded.toList()

            if (topItem != null) {
                item { Box(Modifier.height(16.dp)) }
                item(content = topItem)
            }

            eventList.groupBy { it.time.toLocalDate() }.forEach { (date, events) ->
                stickyHeader(key = "$date") {
                    val day =
                        if (loadHeaderData) dayList.find { it.date == date }
                        else null

                    val selected = selectedItems.containsAll(events)
                    val highlighted = highlightedDay == date

                    var fading by remember { mutableStateOf(false) }
                    var isHighlighting by remember { mutableStateOf(highlighted) }

                    val highlightedColor = MaterialTheme.colorScheme.primaryContainer

                    val duration = if (fading) 1000 else 0
                    val targetColor = if (fading && highlighted) Color.Transparent else highlightedColor

                    val backgroundColor = if (isHighlighting)
                        animateColorAsState(
                            targetValue = targetColor,
                            animationSpec = tween(durationMillis = duration),
                            finishedListener = {
                                if (!fading) {
                                    fading = true
                                }
                                else {
                                    isHighlighting = false
                                    fading = false
                                }
                            }
                        )
                        else null


                    if (day != null) EventListHeader(
                        historyDay = day,
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null
                            )
                            .combinedClickable(
                                onLongClick = listItemOnLongClick@{
                                    haptic.performHapticFeedback(
                                        HapticFeedbackType.LongPress
                                    )
                                    if (selected) selectionState.deselectAll(events)
                                    else selectionState.selectAll(events)
                                },
                                onClick = {
                                    if (selectionMode) {
                                        if (selected) selectionState.deselectAll(events)
                                        else selectionState.selectAll(events)
                                    }
                                    else if (viewModel.mode != EventSelectMode.Default) {
                                        viewModel.gotoDay(date)
                                    }
                                }
                            )
                            .zIndex(0f)
                            .background(if (isHighlighting) backgroundColor!!.value else Color.Transparent),
                        expanded = expandedCurrent.contains(date),
                        eventCount = events.size,
                        selected = selected,
                    ) {
                        scope.launch { dao.toggleFavoriteDay(date) }
                    }
                    else if (loadHeaderData) ListItem(
                        headlineContent = {
                            ComponentPlaceholder(Modifier
                                .height(16.dp)
                                .width(64.dp))
                        }
                    )
                    else ListItem(
                        headlineContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    date.format(DateFormatter),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Box(Modifier.width(8.dp))
                                Text(
                                    "(${events.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.Gray
                                )
                            }
                        }
                    )
                }
                items(events, key = { "event-${it.id}-${it.hashCode()}" }) { event ->
                    // val dayExpanded = expandedCurrent.contains(day.date)
                    // val headerSelected = selectedItems.containsAll(day.events.map { it })
                    val recordEventId by RecordService.RecordState.eventId.collectAsState()

                    val selected = selectedItems.contains(event)
                    val recording = recordEventId == event.id
                    EventListItem(
                        event,
                        selected = selected,
                        recording = recording,
                        tagDetailDialogState = tagDetailDialogState,
                        modifier = Modifier
                            .animateItem(fadeInSpec = null, fadeOutSpec = null)
                            .combinedClickable(
                                onClick = listItemOnClick@{
//                                            if (lock) return@listItemOnClick
                                    if (!selectionMode) {
                                        val intent =
                                            Intent(
                                                context,
                                                EventDetailActivity::class.java
                                            )
                                        intent.putExtra("eventId", event.id)
                                        intent.putExtra(
                                            "path",
                                            getFilePathFromDate(event.time.toLocalDate())
                                        )
                                        context.startActivity(intent)
                                    } else {
                                        if (recording) return@listItemOnClick
                                        if (selected) selectionState.deselect(event)
                                        else selectionState.select(event)
                                    }
                                },
                                onLongClick = listItemOnLongClick@{
                                    if (recording) return@listItemOnLongClick
//                                            selectionMode = true
                                    haptic.performHapticFeedback(
                                        HapticFeedbackType.LongPress
                                    )
                                    if (selected) selectionState.deselect(event)
                                    else selectionState.select(event)
                                }
                            )
                            .zIndex(-1f),
                    )
                }
            }
            item {
                Box(modifier = Modifier.height(136.dp))
            }
        }

        QuickScroll(lazyListState)
        TagDetailDialog(tagDetailDialogState)
    }
    else Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        content = onEmptyContent ?: {}
    )
}

@Composable
private fun QuickScroll(
    listState: LazyListState
) {
    var isDragging by remember { mutableStateOf(false) }
    val isScrolling = listState.isScrollInProgress || isDragging

    val haptic = LocalHapticFeedback.current

    val targetAlpha = if (isDragging) 1f else if (isScrolling) 0.5f else 0f
    val duration = if (isScrolling) 150 else 500
    val delay = if (isScrolling) 0 else 1000

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(delayMillis = delay, durationMillis = duration)
    )

    val scope = rememberCoroutineScope()
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    var containerHeight by remember { mutableIntStateOf(0) }
    var handleHeight by remember { mutableIntStateOf(0) }

    var previousIndex by remember { mutableIntStateOf(0) }

    var layoutInfo by remember { mutableStateOf<LazyListLayoutInfo?>(null) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo = it }
    }

    val averageItemHeight = remember(layoutInfo) {
        val viewPortHeight = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
        if (viewPortHeight <= 0) {
            1
        } else {
            viewPortHeight / listState.layoutInfo.totalItemsCount
        }
    }

    val onPositionUpdated: (yPos: Int) -> Unit = {
        val itemIndex = (it / averageItemHeight).coerceIn(0, listState.layoutInfo.totalItemsCount)

        if (itemIndex != previousIndex) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            previousIndex = itemIndex
        }
        scope.launch {
            // TODO: or listState.animateScrollToItem(itemIndex)
            listState.scrollToItem(itemIndex)
        }
    }

    val maxOffset = remember(containerHeight, handleHeight) {
        (containerHeight - handleHeight).toFloat()
    }

    val thumbHeight = 48.dp

    val dragHandleShape = RoundedCornerShape(8.dp)
    val needDrawScrollbar = isScrolling || alpha > 0.0f
    val firstVisibleElementIndex = layoutInfo?.visibleItemsInfo?.firstOrNull()?.index


    if (firstVisibleElementIndex != null) {
        val scrollbarOffsetY = firstVisibleElementIndex * averageItemHeight

        if (!isDragging) dragOffsetY = scrollbarOffsetY.toFloat()
        Row(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    containerHeight = it.height
                }
                .graphicsLayer { this.alpha = alpha }
                .zIndex(99f),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .offset {
                        IntOffset(0, dragOffsetY.roundToInt())
                    }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            dragOffsetY = (dragOffsetY + delta).coerceIn(0f, maxOffset)
                            onPositionUpdated(dragOffsetY.roundToInt())
                        },
                        onDragStarted = { isDragging = true },
                        onDragStopped = { isDragging = false }
                    )
                    .defaultMinSize(
                        minWidth = 24.dp,
                        minHeight = thumbHeight
                    )
                    .onSizeChanged {
                        handleHeight = it.height
                    }
                    .padding(8.dp)
                    .zIndex(99f),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    Modifier
                        .height(thumbHeight)
                        .width(4.dp)
                        .clip(dragHandleShape)
                        .background(
                            if (!isDragging) Color.Gray
                            else MaterialTheme.colorScheme.primaryContainer
                        )

                        .zIndex(99f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagList(
    lazyListState: LazyListState,
    selectionState: SelectionState<HistoryTag>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val haptic = LocalHapticFeedback.current

    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

    val selectionMode by selectionState.isSelectionMode.collectAsState(false)
    val selectedItems by selectionState.selectedItems.collectAsState(emptyList())

    val tags by dao.getAllTags().map {
        it.map { tag -> tag.toHistoryTag() }
    }.collectAsState(emptyList())

    if (tags.isNotEmpty()) Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(tags.size) {
                val tagData = tags[it]
                val selected = selectedItems.contains(tagData)
                ListItem(
                    leadingContent = {
                        ColorIcon(tagData.tint)
                    },
                    headlineContent = {
                        Text(tagData.name)
                    },
                    supportingContent = {
                        Text(
                            text = tagData.description,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor =
                            if (selected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent,
                        headlineColor =
                            if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                        supportingColor =
                            if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .combinedClickable(
                            onLongClick = withHaptic(haptic) {
                                selectionState.toggle(tagData)
                            },
                            onClick = {
                                if (selectionMode)
                                    selectionState.toggle(tagData)
                                else {
                                    val intent = Intent(context, TagDetailActivity::class.java)
                                    intent.putExtra("tagId", tagData.id)
                                    context.startActivity(intent)
                                }
                            }
                        )
                )
            }

            item {
                Box(modifier = Modifier.height(136.dp))
            }
        }

        QuickScroll(lazyListState)
    }
    else Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Image(
            painter = painterResource(
                if (isSystemInDarkTheme()) R.drawable.hint_no_tag_dark
                else R.drawable.hint_no_tag
            ),
            modifier = Modifier
                .height(196.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(16.dp)
                ),
            contentDescription = null
        )

        Box(Modifier.height(8.dp))

        Text(
            text = "No tags here",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val addIcon = "addIcon"

        val text = buildAnnotatedString {
            append("Add tag by clicking ")
            appendInlineContent(addIcon, "add icon")
            append(" at top right corner, it's useful to organize your events.")
        }
        val inlineContent = mapOf(
            addIcon to InlineTextContent(
                Placeholder(
                    width = 16.sp,
                    height = 16.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = "Add icon",
                    tint = Color.Gray,
                    modifier = Modifier.fillMaxSize()
                )
            },
        )

        Text(
            text = text,
            inlineContent = inlineContent,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}