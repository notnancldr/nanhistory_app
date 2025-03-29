package id.my.nanclouder.nanhistory.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.lib.history.HistoryTag
import id.my.nanclouder.nanhistory.lib.copyWith
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsView(tags: List<HistoryTag>, maximum: Int = 3, wrap: Boolean = false, favorite: Boolean = false) {
    if (tags.isEmpty() && !favorite) return

    val backgroundValue = if (isSystemInDarkTheme()) .2f else .95f
    val backgroundSaturation = if (isSystemInDarkTheme()) .2f else .2f
    val onBackgroundValue = if (isSystemInDarkTheme()) .9f else .2f
    val onBackgroundSaturation = if (isSystemInDarkTheme()) .2f else .98f

    val scrollState = rememberScrollState()
    val rowModifier = Modifier.horizontalScroll(state = scrollState)

    val shownTags = mutableListOf<HistoryTag>()
    if (favorite) shownTags.add(HistoryTag(
        name = "Favorite",
        tint = Color(0xFFFF0000)
    ))

    shownTags.addAll(tags)

    val components = @Composable {
        shownTags.take(maximum).forEach { tag ->
            Text(
                tag.name,
                color = tag.tint.copyWith(saturation = onBackgroundSaturation, value = onBackgroundValue),
                modifier = Modifier
                    .background(
                        color = tag.tint.copyWith(saturation = backgroundSaturation, value = backgroundValue),
                        shape = RoundedCornerShape(100.dp)
                    )
                    .padding(PaddingValues(horizontal = 8.dp, vertical = 2.dp))
                    .sizeIn(),
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
        }
        if (tags.size > maximum) {
            Text(
                "${tags.size - maximum}+",
                color = Color.White,
                modifier = Modifier
                    .background(
                        color = Color(0xFF606060),
                        shape = RoundedCornerShape(100.dp)
                    )
                    .padding(PaddingValues(horizontal = 8.dp, vertical = 2.dp)),
            )
        }
    }

    if (wrap) FlowRow(
        modifier = Modifier.padding(PaddingValues(vertical = 8.dp)),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        components()
    }
    else Row(
        modifier = rowModifier.padding(PaddingValues(vertical = 8.dp)),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        components()
    }
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
            maximum = 3
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
            maximum = 999,
            wrap = true
        )
    }
}