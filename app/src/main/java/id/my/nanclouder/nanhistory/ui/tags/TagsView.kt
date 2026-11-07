package id.my.nanclouder.nanhistory.ui.tags

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import id.my.nanclouder.nanhistory.domain.model.HistoryTag
import id.my.nanclouder.nanhistory.ui.TagDetailDialogState

@Composable
fun TagsView(
    tags: List<HistoryTag>,
    limit: Int = 2,
    wrap: Boolean = false,
    favorite: Boolean = false,
    clickable: Boolean = true,
    darkTheme: Boolean = isSystemInDarkTheme(),
    tagDetailDialogState: TagDetailDialogState? = null
) {
    id.my.nanclouder.nanhistory.core.ui.tags.TagsView(
        tags = tags,
        limit = limit,
        wrap = wrap,
        favorite = favorite,
        clickable = clickable,
        darkTheme = darkTheme,
        tagDetailDialogState = tagDetailDialogState
    )
}
