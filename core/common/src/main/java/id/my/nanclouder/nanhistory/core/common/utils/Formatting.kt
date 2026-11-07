package id.my.nanclouder.nanhistory.core.common.utils

import java.time.Duration
import kotlin.math.round

fun readableSize(size: Double): String {
    return if (size > 1_000_000_000) "${round(size / 100_000_000) / 10} GB"
    else if (size > 1_000_000) "${round(size / 100_000) / 10} MB"
    else if (size > 1_000) "${round(size / 100) / 10} KB"
    else "${size.toInt()} Bytes"
}

fun readableSize(size: Float) = readableSize(size.toDouble())
fun readableSize(size: Int) = readableSize(size.toDouble())
fun readableSize(size: Long) = readableSize(size.toDouble())

fun readableTime(duration: Duration): String {
    val days = duration.toDays()
    val hours = duration.toHoursPart()
    val minutes = duration.toMinutesPart()
    val seconds = duration.toSecondsPart()

    return if (days > 0) "${days}d ${hours}h"
    else if (hours > 0) "${hours}h ${minutes}m"
    else if (minutes > 0) "${minutes}m ${seconds}s"
    else "${seconds}s"
}
