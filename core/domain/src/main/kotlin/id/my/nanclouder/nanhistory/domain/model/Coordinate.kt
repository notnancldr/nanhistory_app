package id.my.nanclouder.nanhistory.domain.model

data class Coordinate(
    val latitude: Double,
    val longitude: Double
) {
    override fun toString(): String = "$latitude,$longitude"

    companion object {
        fun fromString(value: String): Coordinate {
            val parts = value.split(",")
            return Coordinate(parts[0].toDouble(), parts[1].toDouble())
        }
    }
}

fun String.toCoordinate(): Coordinate = Coordinate.fromString(this)
