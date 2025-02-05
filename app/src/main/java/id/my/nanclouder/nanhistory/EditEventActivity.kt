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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
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
import id.my.nanclouder.nanhistory.lib.history.get
import id.my.nanclouder.nanhistory.lib.history.getFilePathFromDate
import id.my.nanclouder.nanhistory.lib.history.save
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

    val fileData = HistoryFileData.get(context, path)
//    Log.d("NanHistoryDebug", "File data: $fileData")
    val eventData = remember {
        fileData?.events?.firstOrNull {
            Log.d("NanHistoryDebug", "Event check: ${it.id} == $eventId")
            it.id == eventId
        }
    }

    val addMode = eventData == null

    var eventTitle by rememberSaveable { mutableStateOf(eventData?.title ?:"") }
    var eventDescription by rememberSaveable { mutableStateOf(eventData?.description ?: "") }
    var rangeEvent by rememberSaveable { mutableStateOf(eventData is EventRange) }
    var time by rememberSaveable { mutableStateOf(eventData?.time ?: ZonedDateTime.now()) }
    var timeEnd by rememberSaveable {
        mutableStateOf(
            (if (eventData is EventRange) eventData.end else null)
                ?: ZonedDateTime.now()
        )
    }

    var datePickerIsOpened by rememberSaveable { mutableStateOf(false) }
    var timePickerIsOpened by rememberSaveable { mutableStateOf(false) }
    var dateEndPickerIsOpened by rememberSaveable { mutableStateOf(false) }
    var timeEndPickerIsOpened by rememberSaveable { mutableStateOf(false) }

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
        if (eventData != null) {
            eventTitle != eventData.title ||
                    eventDescription != eventData.description ||
                    rangeEvent != (eventData is EventRange) ||
                    time != eventData.time
        } else {
            eventTitle.isNotBlank()
        }. let {
            if (eventData is EventRange) {
                it || timeEnd != eventData.end
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
                            val event = (
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
                                if (eventData != null) {
                                    id = eventData.id
                                    created = eventData.created
                                    favorite = eventData.favorite
                                    signature = eventData.signature
                                }
                                if (eventData is EventRange && this is EventRange) {
                                    locations = eventData.locations
                                }
                                else if (eventData is EventPoint && this is EventPoint) {
                                    location = eventData.location
                                }
                                else if (eventData is EventRange && this is EventPoint && eventData.locations.isNotEmpty()) {
                                    location = eventData.locations[eventData.locations.keys.first()]!!
                                }
                                else if (eventData is EventPoint && this is EventRange && eventData.location != null) {
                                    locations = mutableMapOf(eventData.time to eventData.location!!)
                                }
                            }
                            eventData?.delete(context)
                            event.save(context)
                            val result = Intent().apply {
                                putExtra("path", getFilePathFromDate(event.time.toLocalDate()))
                            }
                            context.getActivity()?.setResult(1, result)
                            context.getActivity()?.finish()
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
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = eventTitle,
                onValueChange = {
                    eventTitle = it
                },
                label = { Text("Title") }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = eventDescription,
                onValueChange = {
                    eventDescription = it
                },
                label = { Text("Description") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Range Event")
                Switch(
                    checked = rangeEvent,
                    onCheckedChange = {
                        rangeEvent = it
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (rangeEvent) "Start" else "Time")
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("End")
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
}

@Preview(showBackground = true)
@Composable
fun EditEventPreview() {
    NanHistoryTheme {
        EditEventView("", "")
    }
}