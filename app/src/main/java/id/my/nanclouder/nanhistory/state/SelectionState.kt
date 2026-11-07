package id.my.nanclouder.nanhistory.state

import androidx.compose.runtime.Composable

typealias SelectionState<T> = id.my.nanclouder.nanhistory.core.ui.selection.SelectionState<T>

@Composable
fun <T> rememberSelectionState(): SelectionState<T> {
    return id.my.nanclouder.nanhistory.core.ui.selection.rememberSelectionState()
}
