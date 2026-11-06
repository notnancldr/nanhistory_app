package id.my.nanclouder.nanhistory.utils.history

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import id.my.nanclouder.nanhistory.domain.model.HistoryTag
import id.my.nanclouder.nanhistory.domain.model.generateTagId

var HistoryTag.tint: Color
    get() = Color(this.tintValue)
    set(value) {
        this.tintValue = value.toArgb().toLong() and 0xFFFFFFFFL
    }

fun HistoryTag(
    name: String,
    tint: Color,
    id: String = generateTagId(),
    description: String = "",
    created: java.time.ZonedDateTime = java.time.ZonedDateTime.now()
): HistoryTag = HistoryTag(
    id = id,
    name = name,
    description = description,
    created = created,
    tintValue = tint.toArgb().toLong() and 0xFFFFFFFFL
)
