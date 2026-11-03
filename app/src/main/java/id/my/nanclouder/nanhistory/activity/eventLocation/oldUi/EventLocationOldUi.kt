package id.my.nanclouder.nanhistory.activity.eventLocation.oldUi

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.activity.eventDetail.getActivity
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toEventEntity
import id.my.nanclouder.nanhistory.db.toHistoryEvent
import id.my.nanclouder.nanhistory.db.toLocationEntity
import id.my.nanclouder.nanhistory.ui.ComponentPlaceholder
import id.my.nanclouder.nanhistory.utils.history.EventRange
import id.my.nanclouder.nanhistory.utils.history.LocationData
import id.my.nanclouder.nanhistory.utils.history.generateEventId
import id.my.nanclouder.nanhistory.utils.matchOrNull
import id.my.nanclouder.nanhistory.utils.signature.generateSignature
import id.my.nanclouder.nanhistory.utils.signature.validateSignature
import kotlinx.coroutines.launch
import java.io.File
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventLocationView_Old(eventId: String, startAsCutMode: Boolean = false) {
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

    val eventState by dao.getEventFlowById(eventId).collectAsState(null)
    val eventData = eventState?.toHistoryEvent()

    val recording = matchOrNull<Boolean>(eventData?.metadata?.get("recording")) ?: false

    var eventLocations by remember { mutableStateOf(emptyList<LocationData>()) }
    val locationAvailable = eventLocations.isNotEmpty()

    var cutMode by rememberSaveable { mutableStateOf(startAsCutMode) }
    var cutStart by rememberSaveable { mutableStateOf<ZonedDateTime?>(null) }
    var cutEnd by rememberSaveable { mutableStateOf<ZonedDateTime?>(null) }

    LaunchedEffect(eventData) {
        eventLocations = eventData?.getLocations(context) ?: emptyList()
    }

    Log.d("NanHistoryDebug", "eventData: $eventData")

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    if (eventData != null) Text(if (cutMode) "Cut Event" else "Event Map")
                    else ComponentPlaceholder(Modifier.size(128.dp, 16.dp))
                },
                navigationIcon = {
                    IconButton(onClick = { context.getActivity()!!.finish() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (eventData == null) {
                        ComponentPlaceholder(
                            Modifier
                                .size(24.dp)
                                .padding(8.dp)
                        )
                    }
                    if (cutMode && eventData is EventRange) {
                        IconButton(
                            onClick = {
                                val newEvent =
                                    EventRange(
                                        title = "Cut of ${eventData.title}",
                                        description =
                                            eventData.description +
                                                if (eventData.description.isBlank()) ""
                                                else "\nCut of ${eventData.title}",
                                        time = cutStart!!,
                                        favorite = eventData.favorite,
                                        tags = eventData.tags,
                                        end = cutEnd!!,
                                        locationDescriptions =
                                            eventData.locationDescriptions
                                                .filter {
                                                    it.key >= cutStart!! &&
                                                        it.key <= cutEnd!!
                                                }
                                                .toMutableMap(),
                                        metadata = eventData.metadata,
                                        versionNumber = eventData.versionNumber
                                    )
                                        .apply {
                                            metadata["original_event_id"] = eventData.id
                                            metadata["original_event_time"] =
                                                eventData.time.toOffsetDateTime().toString()
                                            metadata["original_event_end"] =
                                                eventData.end.toOffsetDateTime().toString()
                                            if (metadata["root_event_id"] == null) {
                                                metadata["root_event_id"] = eventData.id
                                            }
                                            if (metadata["root_event_time"] == null) {
                                                metadata["root_event_time"] =
                                                    eventData.time.toOffsetDateTime().toString()
                                            }
                                            if (metadata["root_event_end"] == null) {
                                                metadata["root_event_end"] =
                                                    eventData.end.toOffsetDateTime().toString()
                                            }
                                        }
                                val locationsData =
                                    eventLocations.mapNotNull {
                                        if (it.time >= cutStart!! && it.time <= cutEnd!!) return@mapNotNull null
                                        it.toLocationEntity(newEvent.id)
                                    }

                                if (eventData.audio != null) {
                                    val audioFile = File(context.filesDir, "audio/${eventData.audio}")
                                    audioFile.parentFile?.let {
                                        val id = generateEventId()
                                        val targetPath = "${it.name}/$id.m4a"
                                        val targetFile = File(it, "/$id.m4a")
                                        Log.d(
                                            "NanHistoryDebug",
                                            "AUDIO COPY | FROM: ${audioFile.absolutePath} | TO: ${targetFile.absolutePath}"
                                        )
                                        audioFile.copyTo(targetFile)
                                        newEvent.audio = targetPath
                                    }
                                }

                                val eventDb = AppDatabase.getInstance(context)
                                val eventDao = eventDb.appDao()

                                scope.launch {
                                    if (eventData.validateSignature(context = context)) {
                                        newEvent.generateSignature(context, true)
                                    }
                                    eventDao.insertEvent(newEvent.toEventEntity())
                                    eventDao.insertLocations(locationsData)
                                }

                                cutMode = false
                                Toast.makeText(
                                    context,
                                    "${newEvent.title} has been saved",
                                    Toast.LENGTH_SHORT
                                ).show()

                                if (startAsCutMode) context.getActivity()?.finish()
                            },
                            enabled = cutStart != null && cutEnd != null
                        ) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = "Confirm",
                            )
                        }
                    }
                    if (eventData is EventRange) {
                        IconButton({ cutMode = !cutMode }, enabled = !recording) {
                            if (!cutMode) {
                                Icon(
                                    painterResource(R.drawable.ic_content_cut),
                                    "Cut Mode"
                                )
                            } else {
                                Icon(Icons.Rounded.Close, "Exit Cut Mode")
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (locationAvailable) {
                MapHistoryView_Old(
                    locations = eventLocations.associate { it.time to it.location },
                    onPointsSelected = { time1, time2 ->
                        if (time1 != null && time2 != null) {
                            cutStart = if (time1 < time2) time1 else time2
                            cutEnd = if (time1 > time2) time1 else time2
                        } else {
                            cutStart = time1
                            cutEnd = null
                        }
                    },
                    cutMode = cutMode
                )
            } else {
                ComponentPlaceholder(
                    Modifier
                        .weight(1f)
                        .padding(8.dp)
                )
            }
        }
    }
}
