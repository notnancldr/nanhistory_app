package id.my.nanclouder.nanhistory.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.lib.backgroundTagColor
import id.my.nanclouder.nanhistory.lib.borderTagColor
import id.my.nanclouder.nanhistory.lib.history.HistoryTag
import id.my.nanclouder.nanhistory.lib.textTagColor
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsView(
    tags: List<HistoryTag>,
    limit: Int = 2,
    wrap: Boolean = false,
    favorite: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    if (tags.isEmpty() && !favorite) return

    var tagLimit = limit

    val scrollState = rememberScrollState()
    val rowModifier = Modifier.horizontalScroll(state = scrollState)

    val shownTags = mutableListOf<HistoryTag>()
//    if (favorite) tagLimit--
//        shownTags.add(HistoryTag(
//            name = "Favorite",
//            tint = Color(0xFFFFFF00)
//        ))

    shownTags.addAll(tags)

    val components: @Composable FlowRowScope.() -> Unit = @Composable {
        if (favorite) Icon(
            painterResource(R.drawable.ic_favorite_filled),
            contentDescription = "Favorite",
            tint = Color(0xFFFF7070),
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        shownTags.take(tagLimit).forEach { tag ->
            Text(
                tag.name,
                color = tag.tint.textTagColor(darkTheme),
                modifier = Modifier
                    .background(
                        color = tag.tint.backgroundTagColor(darkTheme),
                        shape = RoundedCornerShape(100.dp),
                    )
                    .border(1.dp, tag.tint.borderTagColor(darkTheme), RoundedCornerShape(100.dp))
                    .padding(PaddingValues(horizontal = 8.dp, vertical = 4.dp))
                    .sizeIn(),
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                style = MaterialTheme.typography.labelMedium
            )
        }
        if (tags.size > tagLimit) {
            Text(
                "${tags.size - tagLimit}+",
                color = Color.White,
                modifier = Modifier
                    .background(
                        color = Color(0xFF606060),
                        shape = RoundedCornerShape(100.dp)
                    )
                    .padding(PaddingValues(horizontal = 8.dp, vertical = 4.dp)),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }

    FlowRow(
        modifier = Modifier.padding(PaddingValues(vertical = 8.dp)),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterVertically),
        maxLines = if (wrap) Int.MAX_VALUE else 1
    ) {
        components()
    }
//    else Row(
//        modifier = rowModifier.padding(PaddingValues(vertical = 8.dp)),
//        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.spacedBy(8.dp)
//    ) {
//        components()
//    }
}

@Preview(showBackground = true)
@Composable
fun TagsPreview() {
    NanHistoryTheme() {
        TagsView(
            tags = listOf(
                HistoryTag(
                    name = "Red",
                    tint = Color(0xFFFF0000)
                ),
                HistoryTag(
                    name = "Yellow",
                    tint = Color(0xFFFFFF00)
                ),
                HistoryTag(
                    name = "Perjalanan ke Purworejo",
                    tint = Color(0xFF00FF00)
                ),
                HistoryTag(
                    name = "Cyan",
                    tint = Color(0xFF00FFFF)
                ),
                HistoryTag(
                    name = "Blue",
                    tint = Color(0xFF0000FF)
                ),
                HistoryTag(
                    name = "Purple",
                    tint = Color(0xFFFF00FF)
                ),
                HistoryTag(
                    name = "You can't see me",
                    tint = Color(0xFFFF00FF)
                ),
                HistoryTag(
                    name = "You can't see me too",
                    tint = Color(0xFFFF00FF)
                )
            ),
            favorite = true,
            limit = 3
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WrapTagsPreview() {
    NanHistoryTheme() {
        TagsView(
            tags = listOf(
                HistoryTag(
                    name = "Red",
                    tint = Color(0xFFFF0000)
                ),
                HistoryTag(
                    name = "Yellow",
                    tint = Color(0xFFFFFF00)
                ),
                HistoryTag(
                    name = "Perjalanan ke Purworejo",
                    tint = Color(0xFF00FF00)
                ),
                HistoryTag(
                    name = "Cyan",
                    tint = Color(0xFF00FFFF)
                ),
                HistoryTag(
                    name = "Blue",
                    tint = Color(0xFF0000FF)
                ),
                HistoryTag(
                    name = "Purple",
                    tint = Color(0xFFFF00FF)
                ),
                HistoryTag(
                    name = "You can't see me",
                    tint = Color(0xFFFF00FF)
                ),
                HistoryTag(
                    name = "You can't see me too",
                    tint = Color(0xFFFF00FF)
                )
            ),
            favorite = true,
            limit = 999,
            wrap = true
        )
    }
}