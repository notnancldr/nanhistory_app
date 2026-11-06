package id.my.nanclouder.nanhistory.domain.model

import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.ZonedDateTime

data class LocationData(
    val time: ZonedDateTime,
    val location: Coordinate,
    val speed: Float? = null,
    val bearing: Float? = null,
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val speedAccuracy: Float? = null,
    val bearingAccuracy: Float? = null,
    val verticalAccuracy: Float? = null,
) {
    override fun toString(): String {
        val formatter = DecimalFormat("#.####").apply {
            roundingMode = RoundingMode.HALF_UP
        }

        fun formatNumber(value: Number?): String {
            return value?.let { formatter.format(it.toDouble()) } ?: ""
        }

        return listOf(
            time.toOffsetDateTime().toString(),
            location.toString(),
            formatNumber(accuracy),
            formatNumber(speed),
            formatNumber(bearing),
            formatNumber(altitude),
            formatNumber(speedAccuracy),
            formatNumber(bearingAccuracy),
            formatNumber(verticalAccuracy)
        ).joinToString("|")
    }

    companion object {
        fun fromString(data: String): LocationData {
            val split = data.split("|")

            return LocationData(
                time = ZonedDateTime.parse(split[0]),
                location = split[1].toCoordinate(),
                accuracy = split.getOrNull(2)?.toFloatOrNull(),
                speed = split.getOrNull(3)?.toFloatOrNull(),
                speedAccuracy = split.getOrNull(4)?.toFloatOrNull(),
                bearing = split.getOrNull(5)?.toFloatOrNull(),
                bearingAccuracy = split.getOrNull(6)?.toFloatOrNull(),
                altitude = split.getOrNull(7)?.toDoubleOrNull(),
                verticalAccuracy = split.getOrNull(8)?.toFloatOrNull(),
            )
        }
    }
}
