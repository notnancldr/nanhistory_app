package id.my.nanclouder.nanhistory.activity

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.activity.eventDetail.getActivity
import id.my.nanclouder.nanhistory.config.Config
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.lifecycle.ViewModelProvider
import id.my.nanclouder.nanhistory.NanHistoryApplication
import id.my.nanclouder.nanhistory.state.EditEventViewModel
import id.my.nanclouder.nanhistory.utils.DateFormatter
import id.my.nanclouder.nanhistory.utils.DatePickerModal
import id.my.nanclouder.nanhistory.utils.FormattedOutlinedTextField
import id.my.nanclouder.nanhistory.utils.MarkdownOutputTransformation
import id.my.nanclouder.nanhistory.utils.MarkdownVisualTransformation
import id.my.nanclouder.nanhistory.utils.TimeFormatter
import id.my.nanclouder.nanhistory.utils.TimePickerDialog
import id.my.nanclouder.nanhistory.utils.history.EventPoint
import id.my.nanclouder.nanhistory.utils.history.EventRange
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import id.my.nanclouder.nanhistory.utils.history.TransportationType
import id.my.nanclouder.nanhistory.utils.history.iconId
import id.my.nanclouder.nanhistory.utils.signature.generateSignature
import id.my.nanclouder.nanhistory.utils.signature.validateSignature
import id.my.nanclouder.nanhistory.utils.withHaptic
import id.my.nanclouder.nanhistory.ui.ComponentPlaceholder
import id.my.nanclouder.nanhistory.ui.style.DangerButtonColors
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import id.my.nanclouder.nanhistory.utils.AccelerometerChange
import id.my.nanclouder.nanhistory.utils.transportModel.toTransportMode
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZonedDateTime
import java.util.Calendar

class EditEventActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val eventId = intent.getStringExtra("eventId")
        enableEdgeToEdge()
        setContent {
            NanHistoryTheme {
                val previousEventOf = intent.getStringExtra("previousEventOf")
                val nextEventOf = intent.getStringExtra("nextEventOf")

                EditEventView(eventId, previousEventOf, nextEventOf)
            }
        }
    }
}

@Composable
fun EditEventView(eventId: String?, previousEventOf: String?, nextEventOf: String?) {
    val useOldUi = Config.appearanceOldUi.getCache()
    if (!useOldUi) EditEventView_New(eventId, previousEventOf, nextEventOf)
    else EditEventView_Old(eventId)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventView_Old(eventId: String?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val container = (context.applicationContext as NanHistoryApplication).container
    val editEventViewModel: EditEventViewModel = remember(context, eventId) {
        (context as? ComponentActivity)?.let { activity ->
            ViewModelProvider(activity, EditEventViewModel.Factory(eventId, null, null, container))[EditEventViewModel::class.java]
        } ?: EditEventViewModel(eventId, null, null, container.eventRepository, container.saveEventUseCase)
    }

    val oldEventState by editEventViewModel.event.collectAsState()
    val oldEvent = oldEventState
    var newEvent by rememberSaveable { mutableStateOf<HistoryEvent?>(null) }

    val addMode = eventId == null

    var datePickerIsOpened by rememberSaveable { mutableStateOf(false) }
    var timePickerIsOpened by rememberSaveable { mutableStateOf(false) }
    var dateEndPickerIsOpened by rememberSaveable { mutableStateOf(false) }
    var timeEndPickerIsOpened by rememberSaveable { mutableStateOf(false) }

    var invalidSignatureWarning by remember { mutableStateOf(false) }

    val saveEvent = {
        if (newEvent != null) {
            editEventViewModel.saveEvent(newEvent!!, isNew = (oldEvent == null)) {
                context.getActivity()?.setResult(1)
                context.getActivity()?.finish()
            }
        }
    }

    if (!addMode && oldEvent == null) Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ComponentPlaceholder(
                        Modifier.height(24.dp).fillMaxWidth(0.6f)
                    )
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            ComponentPlaceholder(Modifier.height(16.dp).width(128.dp))
        }
    }
    else {
        var eventTitle by rememberSaveable { mutableStateOf(oldEvent?.title ?:"") }
        var eventDescription by rememberSaveable { mutableStateOf(oldEvent?.description ?: "") }
        var rangeEvent by rememberSaveable { mutableStateOf(oldEvent is EventRange) }
        var time by rememberSaveable { mutableStateOf(oldEvent?.time ?: ZonedDateTime.now()) }

        val castedEvent = oldEvent as? EventRange

        var timeEnd by rememberSaveable {
            mutableStateOf(
                castedEvent?.end ?: ZonedDateTime.now()
            )
        }
        var transportationType by rememberSaveable {
            mutableStateOf(
                castedEvent?.transportationType ?: TransportationType.Unspecified
            )
        }

        val markdownHighlightingEnabled by Config.experimentalMarkdownInputHighlighting.getState()
        val markdownVisualTransformation = if (markdownHighlightingEnabled) {
            MarkdownVisualTransformation(
                primaryColor = MaterialTheme.colorScheme.primary,
                codeBackgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                tagColor = MaterialTheme.colorScheme.primary,
                quoteColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            VisualTransformation.None
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = time.toInstant().toEpochMilli()
        )
        val timePickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute
        )
        val dateEndPickerState = rememberDatePickerState(
            initialSelectedDateMillis = timeEnd.toInstant().toEpochMilli()
        )
        val timeEndPickerState = rememberTimePickerState(
            initialHour = timeEnd.hour,
            initialMinute = timeEnd.minute
        )

        val textFieldShape = RoundedCornerShape(16.dp)
        val textFieldModifier = Modifier.fillMaxWidth()

        val confirmButtonEnabled =
            if (oldEvent != null) {
                eventTitle != oldEvent.title ||
                        eventDescription != oldEvent.description ||
                        rangeEvent != (oldEvent is EventRange) ||
                        time != oldEvent.time
            } else {
                eventTitle.isNotBlank()
            }.let {
                if (oldEvent is EventRange) {
                    it || timeEnd != oldEvent.end ||
                            transportationType != oldEvent.transportationType
                }
                else it
            }
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                /* TODO */
                                context.getActivity()?.finish()
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                        }
                    },
                    title = { Text(if (addMode) "Add Event" else "Edit Event") },
                )
            },
            bottomBar = {
                BottomAppBar {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaddingValues(horizontal = 8.dp)),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(
                            onClick = {
                                Log.d("NanHistoryDebug", "SAVE BUTTON")
                                newEvent = (
                                    if (rangeEvent) EventRange(
                                        title = eventTitle,
                                        description = eventDescription,
                                        time = time,
                                        end = timeEnd,
                                        transportationType = transportationType
                                    ) else EventPoint (
                                        title = eventTitle,
                                        description = eventDescription,
                                        time = time,
                                    )
                                    ).apply {
                                    if (oldEvent != null) {
                                        id = oldEvent.id
                                        created = oldEvent.created
                                        favorite = oldEvent.favorite
                                        signature = oldEvent.signature
                                        metadata = oldEvent.metadata
                                        audio = oldEvent.audio
                                        unknownProperties = oldEvent.unknownProperties
                                        locationPath = oldEvent.locationPath
                                    }
//                                if (oldEvent is EventRange && this is EventRange) {
//                                    locations = oldEvent.locations
//                                }
//                                else if (oldEvent is EventPoint && this is EventPoint) {
//                                    location = oldEvent.location
//                                }
//                                else if (oldEvent is EventRange && this is EventPoint && oldEvent.locations.isNotEmpty()) {
//                                    location = oldEvent.locations[oldEvent.locations.keys.first()]!!
//                                }
//                                else if (oldEvent is EventPoint && this is EventRange && oldEvent.location != null) {
//                                    locations = mutableMapOf(oldEvent.time to oldEvent.location!!)
//                                }
                                    }
                                scope.launch {
                                if (oldEvent != null && oldEvent.validateSignature(context = context)
                                    && oldEvent.signature != newEvent!!.generateSignature(context = context))
                                    invalidSignatureWarning = true
                                else
                                    saveEvent()
                                }

                            },
                            enabled = confirmButtonEnabled
                        ) { Text(if (addMode) "Add" else "Edit") }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val optionLabel = @Composable { text: String, painter: Painter ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painter, text, Modifier.padding(end = 24.dp), Color.Gray)
                        Text(text, style = MaterialTheme.typography.labelLarge)
                    }
                }

                OutlinedTextField(
                    modifier = textFieldModifier,
                    value = eventTitle,
                    onValueChange = {
                        eventTitle = it
                    },
                    label = { Text("Title") },
                    shape = textFieldShape,
                    visualTransformation = markdownVisualTransformation
                )
                OutlinedTextField(
                    modifier = textFieldModifier,
                    value = eventDescription,
                    onValueChange = {
                        eventDescription = it
                    },
                    label = { Text("Description") },
                    shape = textFieldShape,
                    visualTransformation = markdownVisualTransformation
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    optionLabel("Range Event", painterResource(R.drawable.ic_arrow_range))
                    Switch(
                        checked = rangeEvent,
                        onCheckedChange = withHaptic<Boolean>(haptic) {
                            rangeEvent = it
                        }
                    )
                }
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        optionLabel("Time", painterResource(R.drawable.ic_calendar_month))
//                    TextButton({ rangeEvent = !rangeEvent }) { Text(if (rangeEvent) "Ranged" else "Point") }
//                Row {
//                    TextButton(
//                        onClick = {
//                            /* TODO */
//                            datePickerIsOpened = true
//                        }
//                    ) {
//                        Text(time.format(DateFormatter))
//                    }
//                    TextButton(
//                        onClick = {
//                            /* TODO */
//                            timePickerIsOpened = true
//                        }
//                    ) {
//                        Text(time.format(TimeFormatter))
//                    }
//                }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (rangeEvent) Text("Start", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                        Row {
                            TextButton(
                                onClick = {
                                    /* TODO */
                                    datePickerIsOpened = true
                                }
                            ) {
                                Text(time.format(DateFormatter))
                            }
                            TextButton(
                                onClick = {
                                    /* TODO */
                                    timePickerIsOpened = true
                                }
                            ) {
                                Text(time.format(TimeFormatter))
                            }
                        }
                    }
                    if (rangeEvent) Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("End", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                        Row {
                            TextButton(
                                onClick = {
                                    /* TODO */
                                    dateEndPickerIsOpened = true
                                }
                            ) {
                                Text(timeEnd.format(DateFormatter))
                            }
                            TextButton(
                                onClick = {
                                    /* TODO */
                                    timeEndPickerIsOpened = true
                                }
                            ) {
                                Text(timeEnd.format(TimeFormatter))
                            }
                        }
                    }
                    if (rangeEvent) {
                        var expanded by remember { mutableStateOf(false) }

                        Box(Modifier.height(8.dp))
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                // The `menuAnchor` modifier must be passed to the text field to handle
                                // expanding/collapsing the menu on click. A read-only text field has
                                // the anchor type `PrimaryNotEditable`.
                                value = transportationType.name,
                                onValueChange = { },
                                modifier = textFieldModifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                readOnly = true,
                                shape = textFieldShape,
                                label = { Text("Transportation") },
                                leadingIcon = {
                                    if (transportationType.iconId != null) Icon(
                                        painterResource(transportationType.iconId!!),
                                        "Transportation"
                                    )
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                // colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                TransportationType.entries.forEach { option ->
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            option.iconId?.let {
                                                Icon(painterResource(it), "Transportation type")
                                            }
                                        },
                                        text = { Text(option.name, style = MaterialTheme.typography.bodyLarge) },
                                        onClick = {
                                            transportationType = option
                                            expanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (datePickerIsOpened) DatePickerModal(
            state = datePickerState,
            onDateSelected = onSelected@{
                if (it == null) return@onSelected
                val timeZoneId = Calendar.getInstance().timeZone.toZoneId()
                time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), timeZoneId)
                    .withHour(time.hour).withMinute(time.minute)
            },
            onDismiss = {
                datePickerIsOpened = false
            }
        )
        if (timePickerIsOpened) TimePickerDialog(
            state = timePickerState,
            onTimeSelected = onSelected@{ hour: Int, minute: Int ->
                time = time.withHour(hour).withMinute(minute)
            },
            onDismiss = {
                timePickerIsOpened = false
            }
        )
        if (dateEndPickerIsOpened) DatePickerModal(
            state = dateEndPickerState,
            onDateSelected = onSelected@{
                if (it == null) return@onSelected
                val timeZoneId = Calendar.getInstance().timeZone.toZoneId()
                timeEnd = ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), timeZoneId)
                    .withHour(time.hour).withMinute(time.minute)
            },
            onDismiss = {
                dateEndPickerIsOpened = false
            }
        )
        if (timeEndPickerIsOpened) TimePickerDialog(
            state = timeEndPickerState,
            onTimeSelected = onSelected@{ hour: Int, minute: Int ->
                timeEnd = time.withHour(hour).withMinute(minute)
            },
            onDismiss = {
                timeEndPickerIsOpened = false
            }
        )

        if (invalidSignatureWarning) AlertDialog(
            onDismissRequest = {
                newEvent = null
                invalidSignatureWarning = false
            },
            title = {
                Text("Invalid Signature Warning!")
            },
            text = {
                Text("You have changed the important property of the event, " +
                        "continue saving this event will leave the signature invalid.")
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        newEvent = null
                        invalidSignatureWarning = false
                    },
                ) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = { saveEvent() },
                    colors = DangerButtonColors
                ) { Text("Continue") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventView_New(eventId: String?, previousEventOf: String?, nextEventOf: String?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val container = (context.applicationContext as NanHistoryApplication).container
    val editEventViewModel: EditEventViewModel = remember(context, eventId, previousEventOf, nextEventOf) {
        (context as? ComponentActivity)?.let { activity ->
            ViewModelProvider(
                activity,
                EditEventViewModel.Factory(eventId, previousEventOf, nextEventOf, container)
            )[EditEventViewModel::class.java]
        } ?: EditEventViewModel(
            eventId,
            previousEventOf,
            nextEventOf,
            container.eventRepository,
            container.saveEventUseCase
        )
    }

    val oldEventState by editEventViewModel.event.collectAsState()
    val oldEvent = oldEventState
    var newEvent by rememberSaveable { mutableStateOf<HistoryEvent?>(null) }

    val addMode = eventId == null

    var siblingEventsLoaded by rememberSaveable { mutableStateOf(false) }
    val needToLoadSibling = previousEventOf != null || nextEventOf != null

    val isLoading = (!addMode && oldEvent == null) || (needToLoadSibling && !siblingEventsLoaded)

    var titleIconClicked by remember { mutableIntStateOf(0) }

    var datePickerIsOpened by rememberSaveable { mutableStateOf(false) }
    var timePickerIsOpened by rememberSaveable { mutableStateOf(false) }
    var dateEndPickerIsOpened by rememberSaveable { mutableStateOf(false) }
    var timeEndPickerIsOpened by rememberSaveable { mutableStateOf(false) }
    var formattingGuideExpanded by rememberSaveable { mutableStateOf(false) }

    var invalidSignatureWarning by remember { mutableStateOf(false) }

    var startTimeFromSibling by remember { mutableStateOf<ZonedDateTime?>(null) }
    var endTimeFromSibling by remember { mutableStateOf<ZonedDateTime?>(null) }

    LaunchedEffect(titleIconClicked) {
        if (titleIconClicked == 10) {
            Toast.makeText(context, "Blank title enabled", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(previousEventOf, nextEventOf) {
        if (!needToLoadSibling) {
            return@LaunchedEffect
        }

        val siblingTimes = editEventViewModel.loadSiblingTimes()
        if (siblingTimes != null) {
            startTimeFromSibling = siblingTimes.first
            endTimeFromSibling = siblingTimes.second
        }

        siblingEventsLoaded = true
    }

    val saveEvent = {
        if (oldEvent != null) {
            val newEventRange = newEvent as? EventRange
            (oldEvent?.metadata?.get("accel_data") as? String)?.let { paths ->
                paths.split(",").forEach a@{ path ->
                    File(path).let { file ->
                        if (!file.exists()) {
                            Log.w("NanHistoryDebug", "File doesn't exist: $path")
                            return@a
                        }

                        val fileData = file.readText().let { data ->
                            val lines = data.split("\n")
                            lines.dropLastWhile {
                                try {
                                    AccelerometerChange.fromString(it)
                                    false
                                }
                                catch (_: Throwable) {
                                    true
                                }
                            } + (newEventRange?.transportationType?.toTransportMode()?.name ?: "")
                        }.joinToString("\n")

                        file.writeText(
                            fileData
                        )
                    }
                }
            }
        }

        if (newEvent != null) {
            newEvent!!.metadata.remove("transport_detection_unconfirmed")

            editEventViewModel.saveEvent(newEvent!!, isNew = (oldEvent == null)) {
                context.getActivity()?.setResult(1)
                context.getActivity()?.finish()
            }
        }
    }

    if (isLoading) Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ComponentPlaceholder(
                        Modifier.height(24.dp).fillMaxWidth(0.6f)
                    )
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            ComponentPlaceholder(Modifier.height(16.dp).width(128.dp))
        }
    }
    else {
        val initialTitle = oldEvent?.let {
            if (it.metadata["unnamed"] as? Boolean != true) it.title
            else null
        } ?: ""
        val eventTitleState = rememberTextFieldState(initialTitle)
        val eventDescriptionState = rememberTextFieldState(oldEvent?.description ?: "")
        val eventTitleText = eventTitleState.text.toString()
        val eventDescriptionText = eventDescriptionState.text.toString()

        var rangeEvent by rememberSaveable {
            mutableStateOf(oldEvent is EventRange || (startTimeFromSibling != null && endTimeFromSibling != null))
        }
        var time by rememberSaveable { mutableStateOf(oldEvent?.time ?: startTimeFromSibling ?: ZonedDateTime.now()) }

        val castedEvent = oldEvent as? EventRange

        var timeEnd by rememberSaveable {
            mutableStateOf(
                castedEvent?.end ?: endTimeFromSibling ?: ZonedDateTime.now()
            )
        }
        var transportationType by rememberSaveable {
            mutableStateOf(
                castedEvent?.transportationType ?: TransportationType.Unspecified
            )
        }

        val markdownHighlightingEnabled by Config.experimentalMarkdownInputHighlighting.getState()
        val isDarkTheme = isSystemInDarkTheme()
        val primaryColor = MaterialTheme.colorScheme.primary
        val quoteColor = MaterialTheme.colorScheme.onSurfaceVariant
        var codeBlockRanges by remember { mutableStateOf<List<IntRange>>(emptyList()) }
        val markdownOutputTransformation = if (markdownHighlightingEnabled) {
            remember(isDarkTheme, primaryColor, quoteColor) {
                MarkdownOutputTransformation(
                    isDarkTheme = isDarkTheme,
                    primaryColor = primaryColor,
                    tagColor = primaryColor,
                    quoteColor = quoteColor,
                    onCodeBlockRangesUpdated = { ranges -> codeBlockRanges = ranges }
                )
            }
        } else {
            null
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = time.toInstant().toEpochMilli()
        )
        val timePickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute
        )
        val dateEndPickerState = rememberDatePickerState(
            initialSelectedDateMillis = timeEnd.toInstant().toEpochMilli()
        )
        val timeEndPickerState = rememberTimePickerState(
            initialHour = timeEnd.hour,
            initialMinute = timeEnd.minute
        )

        val textFieldShape = RoundedCornerShape(12.dp)
        val textFieldModifier = Modifier.fillMaxWidth()

        val confirmButtonEnabled =
            if (oldEvent != null) {
                eventTitleText != oldEvent.title ||
                    eventDescriptionText != oldEvent.description ||
                    rangeEvent != (oldEvent is EventRange) ||
                    time != oldEvent.time
            } else {
                eventTitleText.isNotBlank() || titleIconClicked >= 10
            }.let {
                if (oldEvent is EventRange) {
                    it || timeEnd != oldEvent.end ||
                        transportationType != oldEvent.transportationType
                }
                else it
            }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                context.getActivity()?.finish()
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                        }
                    },
                    title = {
                        Text(
                            if (addMode) "Add Event" else "Edit Event",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                )
            },
            bottomBar = {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaddingValues(horizontal = 8.dp, vertical = 4.dp)),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(
                            onClick = {
                                val resolvedTitle = eventTitleText.ifBlank {
                                    if (titleIconClicked < 10) oldEvent?.title ?: "" else ""
                                }
                                newEvent = (
                                    if (rangeEvent) EventRange(
                                        title = resolvedTitle,
                                        description = eventDescriptionText,
                                        time = time,
                                        end = timeEnd,
                                        transportationType = transportationType
                                    ) else EventPoint(
                                        title = resolvedTitle,
                                        description = eventDescriptionText,
                                        time = time,
                                    )
                                    ).apply {
                                    if (oldEvent != null) {
                                        id = oldEvent.id
                                        created = oldEvent.created
                                        favorite = oldEvent.favorite
                                        signature = oldEvent.signature
                                        metadata = oldEvent.metadata
                                        audio = oldEvent.audio
                                        unknownProperties = oldEvent.unknownProperties
                                        locationPath = oldEvent.locationPath

                                        if (oldEvent.title != title) {
                                            metadata.remove("unnamed")
                                        }
                                    }
                                }

                                scope.launch {
                                    if (oldEvent != null && oldEvent.validateSignature(context = context)
                                        && oldEvent.signature != newEvent!!.generateSignature(
                                            context = context
                                        )
                                    )
                                        invalidSignatureWarning = true
                                    else
                                        saveEvent()
                                }
                            },
                            enabled = confirmButtonEnabled,
                            modifier = Modifier.height(40.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                if (addMode) "Add Event" else "Save Changes",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
            ) {
                item {
                    FormattedOutlinedTextField(
                        state = eventTitleState,
                        modifier = textFieldModifier,
                        label = { Text("Title") },
                        shape = textFieldShape,
                        outputTransformation = markdownOutputTransformation,
                        leadingIcon = {
                            Icon(
                                painterResource(R.drawable.ic_title),
                                "Title",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        titleIconClicked++
                                    }
                            )
                        },
                        placeholder = if (titleIconClicked >= 10) ({
                            Text("Leave blank for blank title")
                        }) else if (oldEvent != null) ({
                            Text(oldEvent.title)
                        }) else null
                    )
                }

                item {
                    FormattedOutlinedTextField(
                        state = eventDescriptionState,
                        modifier = textFieldModifier,
                        label = { Text("Description") },
                        shape = textFieldShape,
                        outputTransformation = markdownOutputTransformation,
                        codeBlockRanges = codeBlockRanges,
                        leadingIcon = {
                            Icon(
                                painterResource(R.drawable.ic_info),
                                "Description",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    )
                }

                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { formattingGuideExpanded = !formattingGuideExpanded },
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Text Formatting Guide",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Applies to Title and Description only",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    if (formattingGuideExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                    "Toggle guide",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            AnimatedVisibility(visible = formattingGuideExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                                    FormattingGuideItem("**text**", "Bold")
                                    FormattingGuideItem("*text* or _text_", "Italic")
                                    FormattingGuideItem("~~text~~", "Strikethrough")
                                    FormattingGuideItem("- item", "Unordered list")
                                    FormattingGuideItem("1. item", "Ordered list")
                                }
                            }
                        }
                    }
                }

                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_arrow_range),
                                    "Range Event",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                                Column {
                                    Text(
                                        "Range Event",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        if (rangeEvent) "Time range" else "Single point in time",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = rangeEvent,
                                onCheckedChange = withHaptic<Boolean>(haptic) {
                                    rangeEvent = it
                                }
                            )
                        }
                    }
                }

                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_schedule),
                                    "Time",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                                Text(
                                    if (rangeEvent) "Start Time" else "Time",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp)),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                ) {
                                    TextButton(
                                        onClick = { datePickerIsOpened = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            time.format(DateFormatter),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp)),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                ) {
                                    TextButton(
                                        onClick = { timePickerIsOpened = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            time.format(TimeFormatter),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (rangeEvent) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_schedule),
                                        "End Time",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        "End Time",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp)),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    ) {
                                        TextButton(
                                            onClick = { dateEndPickerIsOpened = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                timeEnd.format(DateFormatter),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp)),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    ) {
                                        TextButton(
                                            onClick = { timeEndPickerIsOpened = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                timeEnd.format(TimeFormatter),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        var expanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = transportationType.name,
                                onValueChange = { },
                                modifier = textFieldModifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                readOnly = true,
                                shape = textFieldShape,
                                label = { Text("Transportation Type") },
                                leadingIcon = {
                                    if (transportationType.iconId != null) Icon(
                                        painterResource(transportationType.iconId!!),
                                        "Transportation",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                TransportationType.entries.forEach { option ->
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            option.iconId?.let {
                                                Icon(painterResource(it), "Transportation type")
                                            }
                                        },
                                        text = { Text(option.name, style = MaterialTheme.typography.bodyMedium) },
                                        onClick = {
                                            transportationType = option
                                            expanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Box(Modifier.height(8.dp))
                }
            }
        }

        // Date/Time Pickers
        if (datePickerIsOpened) DatePickerModal(
            state = datePickerState,
            onDateSelected = onSelected@{
                if (it == null) return@onSelected
                val timeZoneId = Calendar.getInstance().timeZone.toZoneId()
                time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), timeZoneId)
                    .withHour(time.hour).withMinute(time.minute)
            },
            onDismiss = { datePickerIsOpened = false }
        )

        if (timePickerIsOpened) TimePickerDialog(
            state = timePickerState,
            onTimeSelected = onSelected@{ hour: Int, minute: Int ->
                time = time.withHour(hour).withMinute(minute)
            },
            onDismiss = { timePickerIsOpened = false }
        )

        if (dateEndPickerIsOpened) DatePickerModal(
            state = dateEndPickerState,
            onDateSelected = onSelected@{
                if (it == null) return@onSelected
                val timeZoneId = Calendar.getInstance().timeZone.toZoneId()
                timeEnd = ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), timeZoneId)
                    .withHour(timeEnd.hour).withMinute(timeEnd.minute)
            },
            onDismiss = { dateEndPickerIsOpened = false }
        )

        if (timeEndPickerIsOpened) TimePickerDialog(
            state = timeEndPickerState,
            onTimeSelected = onSelected@{ hour: Int, minute: Int ->
                timeEnd = timeEnd.withHour(hour).withMinute(minute)
            },
            onDismiss = { timeEndPickerIsOpened = false }
        )

        // Invalid Signature Warning Dialog
        if (invalidSignatureWarning) AlertDialog(
            icon = {
                Icon(
                    Icons.Rounded.Warning,
                    "Warning",
                    tint = MaterialTheme.colorScheme.error
                )
            },
            onDismissRequest = {
                newEvent = null
                invalidSignatureWarning = false
            },
            title = {
                Text("Signature Invalid")
            },
            text = {
                Text("You've changed important properties of this event. Saving will invalidate the digital signature.")
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        newEvent = null
                        invalidSignatureWarning = false
                    },
                ) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = { saveEvent() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Continue Saving") }
            }
        )
    }
}

@Composable
private fun FormattingGuideItem(syntax: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp)),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        ) {
            Text(
                syntax,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EditEventPreview() {
    NanHistoryTheme {
        EditEventView(eventId = null, previousEventOf = null, nextEventOf = null)
    }
}

/*
BasicTextField(
                modifier = Modifier.fillMaxWidth(),
                value = eventDescription,
                onValueChange = {
                    eventDescription = it
                },
                textStyle = TextStyle.Default.copy(color = Color.Black),
                decorationBox = { innerTextField ->
                    Text(
                        buildAnnotatedString {
                            val regex = "(?<!\\\\)\\*(.*?)(?<!\\\\)\\*".toRegex() // Detects *text* but not \*text*
                            var lastIndex = 0

                            regex.findAll(eventDescription).forEach { matchResult ->
                                val beforeText = eventDescription.substring(lastIndex, matchResult.range.first)
                                append(beforeText.replace("\\*", "*")) // Replace escaped * with normal *

                                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                                append(matchResult.groupValues[1]) // Bold text inside valid asterisks
                                pop()

                                lastIndex = matchResult.range.last + 1
                            }

                            if (lastIndex < eventDescription.length) {
                                append(eventDescription.substring(lastIndex).replace("\\*", "*")) // Replace escaped * at the end
                            }
                        },
                        color = Color.Black
                    )
                }
            )
* */