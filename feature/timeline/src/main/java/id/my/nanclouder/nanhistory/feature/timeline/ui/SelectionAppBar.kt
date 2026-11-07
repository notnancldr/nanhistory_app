package id.my.nanclouder.nanhistory.feature.timeline.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable

@Composable
fun SelectionAppBar(
    selectedItems: Int,
    onCancel: () -> Unit,
    actions: @Composable (RowScope.() -> Unit)
) {
    id.my.nanclouder.nanhistory.core.ui.selection.SelectionAppBar(
        selectedItems = selectedItems,
        onCancel = onCancel,
        actions = actions
    )
}
