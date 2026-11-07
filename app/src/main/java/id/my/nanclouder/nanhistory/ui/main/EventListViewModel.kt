package id.my.nanclouder.nanhistory.ui.main

import androidx.compose.runtime.Composable
import id.my.nanclouder.nanhistory.domain.model.EventSelectMode
import java.time.LocalDate

typealias EventSelectMode = id.my.nanclouder.nanhistory.domain.model.EventSelectMode
typealias EventListViewModel = id.my.nanclouder.nanhistory.core.ui.list.EventListViewModel

@Composable
fun rememberEventListViewModel(
    from: LocalDate = LocalDate.MIN,
    until: LocalDate = LocalDate.parse("9999-12-31"),
    mode: EventSelectMode = EventSelectMode.Default
): EventListViewModel {
    return id.my.nanclouder.nanhistory.core.ui.list.rememberEventListViewModel(from, until, mode)
}
