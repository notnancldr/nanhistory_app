package id.my.nanclouder.nanhistory.ui.list

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toEventEntity
import id.my.nanclouder.nanhistory.ui.MentionModalHandler
import id.my.nanclouder.nanhistory.ui.MentionModalState
import id.my.nanclouder.nanhistory.utils.TimeFormatter
import id.my.nanclouder.nanhistory.utils.history.EventPoint
import id.my.nanclouder.nanhistory.utils.history.EventRange
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import id.my.nanclouder.nanhistory.utils.readableTimeHours
import id.my.nanclouder.nanhistory.ui.TagDetailDialogState
import id.my.nanclouder.nanhistory.ui.rememberMentionModalState
import id.my.nanclouder.nanhistory.ui.tags.TagsView
import id.my.nanclouder.nanhistory.utils.FormattedText
import id.my.nanclouder.nanhistory.utils.history.TransportationType
import id.my.nanclouder.nanhistory.utils.parseFormattedText
import id.my.nanclouder.nanhistory.utils.transportModel.toTransportMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZonedDateTime
import java.time.Duration

@Composable
fun EventListItem(
    eventData: HistoryEvent,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    recording: Boolean = false,
    tagDetailDialogState: TagDetailDialogState? = null,
) {
    val context = LocalContext.current

    val validCheckViewModel = remember { ValidCheckViewModel(context, eventData) }

    val eventValid by validCheckViewModel.valid.collectAsState()

    val cutEvent = eventData.metadata["original_event_id"] != null

    val timeStart = eventData.time.format(TimeFormatter)
    val timeStr = if (!recording) when (eventData) {
        is EventPoint -> timeStart
        is EventRange -> "$timeStart - ${eventData.end.format(TimeFormatter)}"
        else -> ""
    } else "Started: $timeStart"

    val recordingColors = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        headlineColor = MaterialTheme.colorScheme.error,
        leadingIconColor = MaterialTheme.colorScheme.error,
        trailingIconColor = MaterialTheme.colorScheme.error
    )

    var timeElapsed by remember { mutableStateOf(Duration.ZERO) }

    if (recording) LaunchedEffect(eventData) {
        while (true) {
            timeElapsed = Duration.between(eventData.time.toInstant(), Instant.now())
            Log.d("NanHistoryDebug", "TIME ELAPSED: $timeElapsed")
            delay(1000 - timeElapsed.toMillis() % 1000)
        }
    }

    ListItem(
        modifier = modifier,
        headlineContent = {
            Row (verticalAlignment = Alignment.CenterVertically) {
                val title = if (recording) "Recording (${readableTimeHours(timeElapsed)})" else eventData.title

                val signValidId = "signValid"
                val signCheckId = "signCheck"
                val cutEventId = "cutEvent"

                val annotatedString = buildAnnotatedString annotated@{
                    append(title.trimIndent().replace("\n", " "))
                    if (recording) return@annotated
                    if (eventValid == EventSignatureValid.Valid) {
                        append("  ")
                        appendInlineContent(signValidId, "[icon]") // Placeholder for icon
                    }
                    else if (eventValid == EventSignatureValid.Checking) {
                        append("  ")
                        appendInlineContent(signCheckId, "[icon]") // Placeholder for icon
                    }
                    if (cutEvent) {
                        append("  ")
                        appendInlineContent(cutEventId, "[icon]") // Placeholder for icon
                    }
                }

                val inlineContent = mapOf(
                    signValidId to InlineTextContent(
                        Placeholder(20.sp, 20.sp, PlaceholderVerticalAlign.Center)
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = "Signed",
                            modifier = Modifier
                                .size(16.dp),
                            tint = Color(0xFF008000)
                        )
                    },
                    signCheckId to InlineTextContent(
                        Placeholder(20.sp, 20.sp, PlaceholderVerticalAlign.Center)
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "rotation")

                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 800, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "iconAlpha"
                        )

                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Checking signature",
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(90f)
                                .alpha(alpha),
                            tint = Color(0xFF808080)
                        )
                    },
                    cutEventId to InlineTextContent(
                        Placeholder(20.sp, 20.sp, PlaceholderVerticalAlign.Center)
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_content_cut),
                            contentDescription = "Cut",
                            modifier = Modifier
                                .size(16.dp),
                            tint = Color.Gray
                        )
                    },
                )

                Text(
                    annotatedString,
                    inlineContent = inlineContent,
//                    fontSize = 3.5.em,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (recording) Icon(
                    painterResource(R.drawable.ic_circle_filled),
                    contentDescription = "Recording",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(16.dp)
                )
                else if (selected) Icon(Icons.Rounded.Check, "Selected")
                else if (eventData is EventPoint) Icon(painterResource(R.drawable.ic_circle_filled), "Event Icon", Modifier.size(16.dp))
                else Icon(painterResource(R.drawable.ic_arrow_range), "Event Icon", modifier = Modifier.rotate(90f))
            }
        },
        supportingContent = {
            TagsView(eventData.tags, favorite = eventData.favorite, tagDetailDialogState = tagDetailDialogState) // TODO
        },
        trailingContent = {
            Text(timeStr)
        },
        colors = if (recording) recordingColors
        else if (selected) ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            headlineColor = MaterialTheme.colorScheme.primary,
        ) else ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
    )
}

@Composable
fun TimelineEventItem(
    eventData: HistoryEvent,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    recording: Boolean = false,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    tagDetailDialogState: TagDetailDialogState? = null,
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
    mentionModalState: MutableState<MentionModalState> = mutableStateOf(MentionModalState())
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val validCheckViewModel = remember { ValidCheckViewModel(context, eventData, coroutineDispatcher) }
    val eventValid by validCheckViewModel.valid.collectAsState()

    DisposableEffect(eventData) {
        onDispose {
            validCheckViewModel.cancel()
        }
    }

    val cutEvent = eventData.metadata["original_event_id"] != null

    val timeStart = eventData.time.format(TimeFormatter)
    val timeStr = if (!recording) when (eventData) {
        is EventPoint -> timeStart
        is EventRange -> "$timeStart - ${eventData.end.format(TimeFormatter)}"
        else -> ""
    } else "Started: $timeStart"

    var timeElapsed by remember { mutableStateOf(Duration.ZERO) }

    if (recording) LaunchedEffect(eventData) {
        while (true) {
            timeElapsed = Duration.between(eventData.time.toInstant(), Instant.now())
            delay(1000 - timeElapsed.toMillis() % 1000)
        }
    }

    val timelineLineColor = MaterialTheme.colorScheme.outlineVariant
    val timelineActiveDotColor = if (recording) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.primary
    val timelineDotColor = MaterialTheme.colorScheme.surfaceVariant

    var needTransportConfirmation by remember {
        mutableStateOf(
            eventData.metadata["transport_detection_unconfirmed"] as? Boolean ?: false
        )
    }

    var transportationType by remember { mutableStateOf((eventData as? EventRange)?.transportationType) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp)
                .height(IntrinsicSize.Max),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Timeline Line dan Dot
            Column(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Full vertical line (upper and lower)
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .let {
                            if (!isFirst) it.background(timelineLineColor)
                            else it
                        }
                )

                // Column for dots dan lower/upper lines
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Dot - in the middle
                    Box(
                        modifier = Modifier
                            .size(
                                if (eventData !is EventRange) 16.dp
                                else if (transportationType != TransportationType.Unspecified) 32.dp
                                else 24.dp
                            )
                            .background(
                                color = if (recording) MaterialTheme.colorScheme.errorContainer
                                else if (selected) MaterialTheme.colorScheme.primaryContainer
                                else if (eventData !is EventRange) timelineActiveDotColor
                                else if (transportationType != TransportationType.Unspecified) Color.Transparent
                                else timelineDotColor,
                                shape = CircleShape
                            )
                            .border(
                                width =
                                    if (recording || selected) 3.dp
                                    else if (
                                        eventData is EventRange && transportationType != TransportationType.Unspecified
                                    ) 0.dp
                                    else 2.dp,
                                color =
                                    if (
                                        eventData is EventRange && transportationType != TransportationType.Unspecified
                                    ) Color.Transparent
                                    else timelineActiveDotColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (recording) {
                            Icon(
                                painter = painterResource(R.drawable.ic_circle_filled),
                                contentDescription = "Recording",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        } else if (selected) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = "Selected",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        // else if (eventData is EventPoint) {
                        //     Box(
                        //         modifier = Modifier
                        //             .size(6.dp)
                        //             .background(timelineActiveDotColor, CircleShape)
                        //     )
                        // }
                         else if (eventData is EventRange) {
                             Icon(
                                 painter = painterResource(
                                     transportationType?.iconId ?: R.drawable.ic_arrow_range
                                 ),
                                 contentDescription = "Range Event",
                                 modifier = Modifier
                                     .size(
                                         if (transportationType == TransportationType.Unspecified) 16.dp
                                         else 18.dp
                                     )
                                     .rotate(
                                         if (transportationType == TransportationType.Unspecified) 90f
                                         else 0f
                                     ),
                                 tint = timelineActiveDotColor
                             )
                         }
                    }
                }

                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .let {
                            if (!isLast) it.background(timelineLineColor)
                            else it
                        }
                )
            }

            Box(modifier = Modifier.width(12.dp))

            // Event Content Card
            Column(
                modifier = Modifier
                    .padding(vertical = 4.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp)).let {
                            if (isSystemInDarkTheme())
                                it
                            else
                                it.border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                        },
                    color = if (recording) MaterialTheme.colorScheme.errorContainer
                    else if (selected) MaterialTheme.colorScheme.primaryContainer
                    else if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainer
                    else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp).animateContentSize()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val title = if (recording)
                                "Recording (${readableTimeHours(timeElapsed)})"
                            else eventData.title

                            val signValidId = "signValid"
                            val signCheckId = "signCheck"
                            val cutEventId = "cutEvent"

                            val inlineContent = mapOf(
                                signValidId to InlineTextContent(
                                    Placeholder(16.sp, 16.sp, PlaceholderVerticalAlign.Center)
                                ) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = "Signed",
                                        modifier = Modifier.size(14.dp),
                                        tint = Color(0xFF008000)
                                    )
                                },
                                signCheckId to InlineTextContent(
                                    Placeholder(16.sp, 16.sp, PlaceholderVerticalAlign.Center)
                                ) {
                                    val infiniteTransition =
                                        rememberInfiniteTransition(label = "rotation")
                                    val alpha by infiniteTransition.animateFloat(
                                        initialValue = 0.3f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(
                                                durationMillis = 800,
                                                easing = LinearEasing
                                            ),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "iconAlpha"
                                    )
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "Checking signature",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .rotate(90f)
                                            .alpha(alpha),
                                        tint = Color(0xFF808080)
                                    )
                                },
                                cutEventId to InlineTextContent(
                                    Placeholder(16.sp, 16.sp, PlaceholderVerticalAlign.Center)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_content_cut),
                                        contentDescription = "Cut",
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.Gray
                                    )
                                },
                            )

                            FormattedText(
                                text = title.trimIndent().replace("\n", " "),
                                annotatedString = { annotatedString ->
                                    append(annotatedString)
                                    if (recording) return@FormattedText
                                    if (eventValid == EventSignatureValid.Valid) {
                                        append("  ")
                                        appendInlineContent(signValidId, "[icon]")
                                    } else if (eventValid == EventSignatureValid.Checking) {
                                        append("  ")
                                        appendInlineContent(signCheckId, "[icon]")
                                    }
                                    if (cutEvent) {
                                        append("  ")
                                        appendInlineContent(cutEventId, "[icon]")
                                    }
                                },
                                inlineContent = inlineContent,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                                mentionModalState = mentionModalState,
                                color = if (recording) MaterialTheme.colorScheme.error
                                else if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )

                            Box(modifier = Modifier.width(8.dp))

                            Text(
                                timeStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (recording) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        if (eventData.description.isNotBlank()) {
                            FormattedText(
                                eventData.description,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                mentionModalState = mentionModalState,
                                color = if (recording) MaterialTheme.colorScheme.onErrorContainer
                                else if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(Modifier.height(4.dp))

                        TagsView(
                            eventData.tags,
                            favorite = eventData.favorite,
                            tagDetailDialogState = tagDetailDialogState
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AnimatedVisibility(
                                visible = needTransportConfirmation && !recording
                            ) {
                                Box(Modifier.height(4.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val question =
                                        when ((eventData as? EventRange)?.transportationType) {
                                            TransportationType.Walk -> "Is this correct?\nYou were walking"
                                            else -> "Is this correct?\nYou were using ${(eventData as? EventRange)?.transportationType?.name?.lowercase()}"
                                        }

                                    Text(
                                        text = question,
                                        style = MaterialTheme.typography.bodySmall,
                                    )

                                    Row {
                                        IconButton(
                                            onClick = {
                                                eventData.metadata.remove("transport_detection_unconfirmed")

                                                val eventRange = eventData as? EventRange

                                                scope.launch {
                                                    val db = AppDatabase.getInstance(context)
                                                    val dao = db.appDao()

                                                    (eventData.metadata["accel_data"] as? String)?.let { paths ->
                                                        paths.split(",").forEach a@{ path ->
                                                            File(path).let { file ->
                                                                if (!file.exists()) {
                                                                    Log.w("NanHistoryDebug", "File doesn't exist: $path")
                                                                    return@a
                                                                }

                                                                file.appendText(
                                                                    (eventRange?.transportationType?.toTransportMode()?.name ?: "")
                                                                )
                                                            }
                                                        }
                                                    }

                                                    dao.updateEvent(eventData.toEventEntity())
                                                    needTransportConfirmation = false
                                                }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = "Yes",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                eventData.metadata.remove("transport_detection_unconfirmed")
                                                (eventData as? EventRange)?.transportationType =
                                                    TransportationType.Unspecified
                                                transportationType = TransportationType.Unspecified

                                                scope.launch {
                                                    val db = AppDatabase.getInstance(context)
                                                    val dao = db.appDao()

                                                    needTransportConfirmation = false

                                                    dao.updateEvent(eventData.toEventEntity())
                                                }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Rounded.Close,
                                                contentDescription = "No",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EventListItemPreview() {
    EventListItem(
        EventRange(
            title = "Berangkat",
            description = "",
            time = ZonedDateTime.now(),
            end = ZonedDateTime.now()
        )
    )
}

@Preview(showBackground = true)
@Composable
fun TimelineEventItemPreview() {
    Column {
        TimelineEventItem(
            EventRange(
                title = "Berangkat",
                description = "",
                time = ZonedDateTime.now(),
                end = ZonedDateTime.now()
            )
        )
    }
}