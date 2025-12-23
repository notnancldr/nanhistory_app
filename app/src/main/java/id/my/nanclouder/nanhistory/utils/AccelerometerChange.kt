package id.my.nanclouder.nanhistory.utils

data class AccelerometerChange(
    val x: Float,
    val y: Float,
    val z: Float
) {
    val all
        get() = x + y + z

    override fun toString() = "$x,$y,$z"

    companion object {
        fun fromString(string: String): AccelerometerChange {
            val split = string.split(',')
            return AccelerometerChange(
                split[0].toFloat(),
                split[1].toFloat(),
                split[2].toFloat()
            )
        }
    }
}

fun List<AccelerometerChange>.average() = this
    .fold(AccelerometerChange(0f, 0f, 0f)) { acc, item ->
        AccelerometerChange(
            acc.x + item.x,
            acc.y + item.y,
            acc.z + item.z,
        )
    }.let {
        AccelerometerChange(
            x = it.x / this.size,
            y = it.y / this.size,
            z = it.z / this.size
        )
    }