package id.my.nanclouder.nanhistory.domain.model

import java.time.ZonedDateTime

data class HistoryLocationData(
    val speed: Float,
    val acceleration: Float,
    val points: List<Coordinate>,
    val locationData: List<LocationData>,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val distance: Float,
)
