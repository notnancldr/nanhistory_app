package id.my.nanclouder.nanhistory.domain.model

import java.time.Instant
import java.time.ZonedDateTime
import kotlin.random.Random

fun generateTagId(instant: Instant = Instant.now()): String =
    "TAG-${instant.toEpochMilli().toString(16)}-${Random.nextLong().toString(16)}"

data class HistoryTag(
    var id: String = generateTagId(),
    var name: String,
    var description: String = "",
    var created: ZonedDateTime = ZonedDateTime.now(),
    var tintValue: Long = 0xFF808080L
) {
    val tintArgb: Int get() = tintValue.toInt()

    companion object
}
