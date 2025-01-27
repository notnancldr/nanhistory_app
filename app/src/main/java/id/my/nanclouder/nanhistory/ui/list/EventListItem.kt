package id.my.nanclouder.nanhistory.ui.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.lib.TimeFormatter
import id.my.nanclouder.nanhistory.lib.history.EventPoint
import id.my.nanclouder.nanhistory.lib.history.EventRange
import id.my.nanclouder.nanhistory.lib.history.HistoryEvent
import id.my.nanclouder.nanhistory.lib.history.validateSignature
import id.my.nanclouder.nanhistory.ui.TagsView
import java.time.ZonedDateTime

@Composable
fun EventListItem(eventData: HistoryEvent, selected: Boolean = false, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val timeStr = when (eventData) {
        is EventPoint -> {
            eventData.time.format(TimeFormatter)
        }
        is EventRange -> {
            val start = eventData.time.format(TimeFormatter)
            val end = eventData.end.format(TimeFormatter)
            "$start - $end"
        }
        else -> ""
    }

    ListItem(
        modifier = modifier,
        headlineContent = {
            Row (verticalAlignment = Alignment.CenterVertically) {
                if (eventData.validateSignature())
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = "Signed",
                        modifier = Modifier.padding(end = 8.dp).size(16.dp).requiredSize(16.dp),
                        tint = Color(0xFF008000)
                    )
                Text(
                    eventData.title.trimIndent().replace("\n", " "),
                    fontSize = 3.5.em,
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
                if (selected) Icon(Icons.Rounded.Check, "Selected")
                else if (eventData is EventPoint) Icon(painterResource(R.drawable.ic_circle_filled), "Event Icon", Modifier.size(16.dp))
                else Icon(painterResource(R.drawable.ic_arrow_range), "Event Icon", modifier = Modifier.rotate(90f))
            }
        },
        supportingContent = {
            TagsView(listOf(), favorite = eventData.favorite) // TODO
        },
        trailingContent = {
            Text(timeStr)
        },
        colors = if (selected) ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            headlineColor = MaterialTheme.colorScheme.primary,
        ) else ListItemDefaults.colors(),
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