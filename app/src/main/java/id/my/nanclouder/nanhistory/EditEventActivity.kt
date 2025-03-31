package id.my.nanclouder.nanhistory

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.lib.DateFormatter
import id.my.nanclouder.nanhistory.lib.DatePickerModal
import id.my.nanclouder.nanhistory.lib.TimeFormatter
import id.my.nanclouder.nanhistory.lib.TimePickerDialog
import id.my.nanclouder.nanhistory.lib.history.EventPoint
import id.my.nanclouder.nanhistory.lib.history.EventRange
import id.my.nanclouder.nanhistory.lib.history.HistoryEvent
import id.my.nanclouder.nanhistory.lib.history.HistoryFileData
import id.my.nanclouder.nanhistory.lib.history.delete
import id.my.nanclouder.nanhistory.lib.history.generateSignature
import id.my.nanclouder.nanhistory.lib.history.get
import id.my.nanclouder.nanhistory.lib.history.getFilePathFromDate
import id.my.nanclouder.nanhistory.lib.history.save
import id.my.nanclouder.nanhistory.lib.withHaptic
import id.my.nanclouder.nanhistory.ui.style.DangerButtonColors
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import java.time.Instant
import java.time.ZonedDateTime
import java.util.Calendar

class EditEventActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val eventId = intent.getStringExtra("eventId") ?: ""
        val path = intent.getStringExtra("path") ?: "NULL"
        enableEdgeToEdge()
        setContent {
            NanHistoryTheme {
                EditEventView(eventId, path)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventView(eventId: String, path: String) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val oldEvent = remember {
        val fileData = HistoryFileData.get(context, path)
        fileData?.events?.firstOrNull {
            Log.d("NanHistoryDebug", "Event check: ${it.id} == $eventId")
            it.id == eventId
        }
    }
    var newEvent by rememberSaveable { mutableStateOf<HistoryEvent?>(null) }

    val addMode = oldEvent == null

    var eventTitle by rememberSaveable { mutableStateOf(oldEvent?.title ?:"") }
    var eventDescription by rememberSaveable { mutableStateOf(oldEvent?.description ?: "") }
    var rangeEvent by rememberSaveable { mutableStateOf(oldEvent is EventRange) }
    var time by rememberSaveable { mutableStateOf(oldEvent?.time ?: ZonedDateTime.now()) }
    var timeEnd by rememberSaveable {
        mutableStateOf(
            (if (oldEvent is EventRange) oldEvent.end else null)
                ?: ZonedDateTime.now()
        )
    }

    var datePickerIsOpened by rememberSaveable { mutableStateOf(false) }
    var timePickerIsOpened by rememberSaveable { mutableStateOf(false) }
    var dateEndPickerIsOpened by rememberSaveable { mutableStateOf(false) }
    var timeEndPickerIsOpened by rememberSaveable { mutableStateOf(false) }

    var invalidSignatureWarning by remember { mutableStateOf(false) }

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

    val confirmButtonEnabled =
        if (oldEvent != null) {
            eventTitle != oldEvent.title ||
                    eventDescription != oldEvent.description ||
                    rangeEvent != (oldEvent is EventRange) ||
                    time != oldEvent.time
        } else {
            eventTitle.isNotBlank()
        }. let {
            if (oldEvent is EventRange) {
                it || timeEnd != oldEvent.end
            }
            else it
        }

    val saveEvent = {
        oldEvent?.delete(context)
        if (newEvent != null) {
            newEvent!!.save(context)
            val result = Intent().apply {
                putExtra("path", getFilePathFromDate(newEvent!!.time.toLocalDate()))
            }
            context.getActivity()?.setResult(1, result)
            context.getActivity()?.finish()
        }
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
                            newEvent = (
                                if (rangeEvent) EventRange(
                                    title = eventTitle,
                                    description = eventDescription,
                                    time = time,
                                    end = timeEnd,

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
                                }
                                if (oldEvent is EventRange && this is EventRange) {
                                    locations = oldEvent.locations
                                }
                                else if (oldEvent is EventPoint && this is EventPoint) {
                                    location = oldEvent.location
                                }
                                else if (oldEvent is EventRange && this is EventPoint && oldEvent.locations.isNotEmpty()) {
                                    location = oldEvent.locations[oldEvent.locations.keys.first()]!!
                                }
                                else if (oldEvent is EventPoint && this is EventRange && oldEvent.location != null) {
                                    locations = mutableMapOf(oldEvent.time to oldEvent.location!!)
                                }
                            }

                            if (oldEvent != null && oldEvent.generateSignature() != newEvent!!.generateSignature())
                                invalidSignatureWarning = true
                            else
                                saveEvent()

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
                modifier = Modifier.fillMaxWidth(),
                value = eventTitle,
                onValueChange = {
                    eventTitle = it
                },
                label = { Text("Title") },
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = eventDescription,
                onValueChange = {
                    eventDescription = it
                },
                label = { Text("Description") },
                shape = RoundedCornerShape(16.dp)
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

@Preview(showBackground = true)
@Composable
fun EditEventPreview() {
    NanHistoryTheme {
        EditEventView("", "")
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