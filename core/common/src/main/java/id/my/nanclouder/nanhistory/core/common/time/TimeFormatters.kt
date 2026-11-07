package id.my.nanclouder.nanhistory.core.common.time

import java.time.Duration
import java.time.format.DateTimeFormatter

val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
val TimeFormatterWithSecond: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

fun readableTimeHours(duration: Duration): String {
    val days = duration.toDays()
    val hours = duration.toHoursPart()
    val minutes = duration.toMinutesPart()
    val seconds = duration.toSecondsPart()

    var text = ""
    if (days > 0) text += "${days.toString().padStart(2, '0')}:"
    if (hours > 0) text += "${hours.toString().padStart(2, '0')}:"
    text += "${minutes.toString().padStart(2, '0')}:"
    text += seconds.toString().padStart(2, '0')
    return text
}
