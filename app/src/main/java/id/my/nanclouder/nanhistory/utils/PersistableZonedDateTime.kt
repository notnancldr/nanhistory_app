package id.my.nanclouder.nanhistory.utils

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class PersistableZonedDateTime(
    val utcDateTime: Long,
    val zoneId: String
)


fun ZonedDateTime.toPersistable() = PersistableZonedDateTime(
    utcDateTime = this.toInstant().toEpochMilli(),
    zoneId = this.zone.id
)

fun PersistableZonedDateTime.toZonedDateTime(): ZonedDateTime? =
    ZonedDateTime.ofInstant(Instant.ofEpochMilli(utcDateTime), ZoneId.of(zoneId))