package id.my.nanclouder.nanhistory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toHistoryTag
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import id.my.nanclouder.nanhistory.state.rememberSelectionState
import id.my.nanclouder.nanhistory.ui.ComponentPlaceholder
import id.my.nanclouder.nanhistory.ui.EventList
import id.my.nanclouder.nanhistory.ui.LineChart
import id.my.nanclouder.nanhistory.ui.SelectionAppBar
import id.my.nanclouder.nanhistory.ui.TagEditorDialog
import id.my.nanclouder.nanhistory.ui.main.EventListViewModel
import id.my.nanclouder.nanhistory.ui.main.EventSelectMode
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import id.my.nanclouder.nanhistory.utils.DateFormatter
import id.my.nanclouder.nanhistory.utils.history.EventRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Month

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

    var showTagInfo by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val eventList by viewModel.events.collectAsState(emptyList())

    val newUI by Config.appearanceNewUI.getState()

    val listState = rememberLazyListState()

    BackHandler(selectionMode) {
        selectionState.reset()
    }

    viewModel.events

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
                    if (newUI) {
                        IconButton(
                            onClick = {
                                showEditDialog = true
                            }
                        ) {
                            Icon(Icons.Rounded.Edit, "Edit tag")
                        }
                        IconButton(
                            onClick = {
                                showTagInfo = true
                            }
                        ) {
                            Icon(Icons.Rounded.Info, "Details")
                        }
                    }
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

        TagInfoDialog(
            state = showTagInfo,
            onDismissRequest = {
                showTagInfo = false
            },
            tagId = tagId,
            events = eventList
        )

        TagEditorDialog(
            state = showEditDialog,
            onDismissRequest = {
                showEditDialog = false
            },
            tagId = tagId
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagInfoDialog(
    state: Boolean,
    onDismissRequest: () -> Unit,
    tagId: String,
    events: List<HistoryEvent>
) {
    val context = LocalContext.current
    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

    val tag by dao.getTagById(tagId).map { it?.toHistoryTag() }.collectAsState(null)

    if (state) {
        BasicAlertDialog (
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            ),
            modifier = Modifier.fillMaxSize(),
            content = {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tag Detail",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton (
                                onClick = {
                                    onDismissRequest()
                                }
                            ) {
                                Icon(Icons.Rounded.Close, "Close info")
                            }
                        }

                        val historyTag = tag

                        Column {
                            Text(
                                text = "Tag Usage (Times)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Box(Modifier.height(8.dp))
                            if (historyTag != null)
                                TagUsageChart(
                                    events = events,
                                    modifier = Modifier.height(196.dp)
                                )
                            else
                                ComponentPlaceholder(
                                    modifier = Modifier
                                        .height(196.dp)
                                        .fillMaxWidth()
                                )
                        }

                        Column {
                            Text(
                                text = "Tag Usage (Hours)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Box(Modifier.height(8.dp))
                            if (historyTag != null)
                                TagTotalHourChart(
                                    events = events,
                                    modifier = Modifier.height(196.dp)
                                )
                            else
                                ComponentPlaceholder(
                                    modifier = Modifier
                                        .height(196.dp)
                                        .fillMaxWidth()
                                )
                        }

                        // Info Items
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            InfoItem(
                                imageVector = Icons.Rounded.Tag,
                                label = "Name",
                                value = tag?.name,
                            )
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                            )
                            InfoItem(
                                imageVector = Icons.Default.DateRange,
                                label = "Created",
                                value = tag?.created?.format(DateFormatter)
                            )
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                            )
                            InfoItem(
                                imageVector = Icons.Default.Info,
                                label = "Description",
                                value = tag?.description
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun TagUsageChart(
    events: List<HistoryEvent>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

    val usageCounted = events.groupBy { Pair(it.time.year, it.time.month) }
        .let {
            val list = mutableMapOf<Int,MutableMap<Month,Float>>()
            it.forEach { item ->
                if (list[item.key.first] == null) {
                    list[item.key.first] = mutableMapOf(
                        Month.JANUARY to 0f,
                        Month.FEBRUARY to 0f,
                        Month.MARCH to 0f,
                        Month.APRIL to 0f,
                        Month.MAY to 0f,
                        Month.JUNE to 0f,
                        Month.JULY to 0f,
                        Month.AUGUST to 0f,
                        Month.SEPTEMBER to 0f,
                        Month.OCTOBER to 0f,
                        Month.NOVEMBER to 0f,
                        Month.DECEMBER to 0f
                    )
                }

                list[item.key.first]?.put(item.key.second, item.value.size.toFloat())
            }
            list
        }

    val pagerState = rememberPagerState(
        initialPage = usageCounted.size - 1,
        pageCount = {
            usageCounted.size
        }
    )

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth()
    ) { page ->
        val year = usageCounted.keys.toList().sorted()[page]
        Column {
            Text("$year")
            LineChart(
                values = usageCounted[year]?.values?.toList()
                    ?: emptyList(),
                color = MaterialTheme.colorScheme.primary,
                valueLabels = listOf(
                    "Jan",
                    "Feb",
                    "Mar",
                    "Apr",
                    "May",
                    "Jun",
                    "Jul",
                    "Aug",
                    "Sep",
                    "Oct",
                    "Nov",
                    "Dec"
                ),
                showValueLabels = true,
                valueLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TagTotalHourChart(
    events: List<HistoryEvent>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

    val usageCounted = events.groupBy { Pair(it.time.year, it.time.month) }
        .let {
            val list = mutableMapOf<Int,MutableMap<Month,Float>>()
            it.forEach { item ->
                if (list[item.key.first] == null) {
                    list[item.key.first] = mutableMapOf(
                        Month.JANUARY to 0f,
                        Month.FEBRUARY to 0f,
                        Month.MARCH to 0f,
                        Month.APRIL to 0f,
                        Month.MAY to 0f,
                        Month.JUNE to 0f,
                        Month.JULY to 0f,
                        Month.AUGUST to 0f,
                        Month.SEPTEMBER to 0f,
                        Month.OCTOBER to 0f,
                        Month.NOVEMBER to 0f,
                        Month.DECEMBER to 0f
                    )
                }

                list[item.key.first]?.put(
                    item.key.second,
                    (item.value.fold(0L) { acc, value ->
                        acc + (
                            ((value as? EventRange)?.end?.toEpochSecond() ?: value.time.toEpochSecond()) -
                            value.time.toEpochSecond()
                        )
                    }.toFloat() / 3600f)
                )
            }
            list
        }

    val pagerState = rememberPagerState(
        initialPage = usageCounted.size - 1,
        pageCount = {
            usageCounted.size
        }
    )

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth()
    ) { page ->
        val year = usageCounted.keys.toList().sorted()[page]
        Column {
            Text("$year")
            LineChart(
                values = usageCounted[year]?.values?.toList()
                    ?: emptyList(),
                color = MaterialTheme.colorScheme.primary,
                valueLabels = listOf(
                    "Jan",
                    "Feb",
                    "Mar",
                    "Apr",
                    "May",
                    "Jun",
                    "Jul",
                    "Aug",
                    "Sep",
                    "Oct",
                    "Nov",
                    "Dec"
                ),
                showValueLabels = true,
                valueLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoItem(
    imageVector: ImageVector,
    label: String,
    value: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (value != null) Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            else ComponentPlaceholder(
                modifier = Modifier.size(128.dp, 16.dp)
            )
        }
    }
}