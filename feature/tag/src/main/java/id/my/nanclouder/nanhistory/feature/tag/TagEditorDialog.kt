package id.my.nanclouder.nanhistory.feature.tag

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import id.my.nanclouder.nanhistory.core.ui.tags.TagEditorDialog as CoreTagEditorDialog
import id.my.nanclouder.nanhistory.core.ui.tags.TagPreview as CoreTagPreview
import id.my.nanclouder.nanhistory.domain.model.HistoryTag

@Composable
fun TagEditorDialog(
    state: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    tagId: String? = null
) {
    CoreTagEditorDialog(
        state = state,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        tagId = tagId
    )
}

@Composable
fun TagPreview(
    tag: HistoryTag,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    CoreTagPreview(
        tag = tag,
        darkTheme = darkTheme,
        modifier = modifier
    )
}
