package id.my.nanclouder.nanhistory.ui.list

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.lib.DateFormatter
import id.my.nanclouder.nanhistory.lib.history.HistoryTag
import id.my.nanclouder.nanhistory.lib.history.HistoryDay
import id.my.nanclouder.nanhistory.lib.history.get
import id.my.nanclouder.nanhistory.ui.tags.TagsView

@Composable
fun EventListHeader(
    historyDay: HistoryDay,
    modifier: Modifier = Modifier,
    selected: Boolean,
    expanded: Boolean? = null,
    onExpandButtonClicked: (() -> Unit)? = null,
    onFavoriteChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val tagData = historyDay.tags.mapNotNull { HistoryTag.get(context, it) }
    val headlineFontSize = 4.em
    val headlineFontWeight = FontWeight.W500
    var favorite by remember { mutableStateOf(historyDay.favorite) }
//    HorizontalDivider(modifier = dividerModifier)
    ListItem(
        modifier = modifier
            .clip(
                when (expanded) {
                    false -> RoundedCornerShape(24.dp)
                    true -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    else -> RectangleShape
                }
            ),
        leadingContent = if (expanded != null) ({
            IconButton(
                modifier = Modifier.width(32.dp),
                onClick = onExpandButtonClicked ?: { }
            ) {
                if (expanded) Icon(Icons.Rounded.KeyboardArrowUp, "Collapse")
                else Icon(Icons.Rounded.KeyboardArrowDown, "Expand")
            }
        }) else null,
        headlineContent = {
            val format = DateFormatter
            val dateStr = historyDay.date.format(format)
            Text(
                dateStr,
                fontSize = headlineFontSize,
                fontWeight = headlineFontWeight,
                textAlign = TextAlign.Center
            )
        },
        trailingContent = {
            IconButton(
                onClick = {
                    onFavoriteChanged(!favorite)
                    favorite = !favorite
                }
            ) {
                if (favorite) Icon(
                    painterResource(R.drawable.ic_favorite_filled), "",
                    tint = Color(0xFFFF7070)
                )
                else Icon(
                    painterResource(R.drawable.ic_favorite), ""
                )
            }
        },
        supportingContent = {
            TagsView(tagData)
        },
        colors = if (selected) ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            headlineColor = MaterialTheme.colorScheme.primary,
        ) else ListItemDefaults.colors(
            containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainer
                else MaterialTheme.colorScheme.surface
        ),
        shadowElevation = if (expanded == true) 4.dp else ListItemDefaults.Elevation,
//        else ListItemDefaults.colors(
//            containerColor =
//                if (false) MaterialTheme.colorScheme.surfaceContainer
//                else MaterialTheme.colorScheme.surface,
//        ),
    )
}