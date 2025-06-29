package id.my.nanclouder.nanhistory.ui.list

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.viewModelFactory
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.lib.TimeFormatter
import id.my.nanclouder.nanhistory.lib.history.EventPoint
import id.my.nanclouder.nanhistory.lib.history.EventRange
import id.my.nanclouder.nanhistory.lib.history.HistoryEvent
import id.my.nanclouder.nanhistory.lib.history.validateSignature
import id.my.nanclouder.nanhistory.lib.matchOrNull
import id.my.nanclouder.nanhistory.lib.readableTimeHours
import id.my.nanclouder.nanhistory.ui.tags.TagsView
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZonedDateTime
import java.time.Duration
import java.util.Date

@Composable
fun EventListItem(
    eventData: HistoryEvent,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    recording: Boolean = false
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
                    modifier = Modifier.padding(end = 8.dp).size(16.dp)
                )
                else if (selected) Icon(Icons.Rounded.Check, "Selected")
                else if (eventData is EventPoint) Icon(painterResource(R.drawable.ic_circle_filled), "Event Icon", Modifier.size(16.dp))
                else Icon(painterResource(R.drawable.ic_arrow_range), "Event Icon", modifier = Modifier.rotate(90f))
            }
        },
        supportingContent = {
            TagsView(eventData.tags, favorite = eventData.favorite) // TODO
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