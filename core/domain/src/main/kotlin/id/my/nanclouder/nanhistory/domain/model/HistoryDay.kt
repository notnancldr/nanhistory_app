package id.my.nanclouder.nanhistory.domain.model

import java.time.LocalDate

data class HistoryDay(
    var date: LocalDate,
    var description: String?,
    var favorite: Boolean = false,
    var tags: List<HistoryTag> = listOf(),
    var metadata: MutableMap<String, Any> = mutableMapOf(),
)
