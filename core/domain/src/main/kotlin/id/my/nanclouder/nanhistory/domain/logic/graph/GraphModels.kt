package id.my.nanclouder.nanhistory.domain.logic.graph

import java.util.UUID

/**
 * Scale mode for rendering multiple series on a single graph.
 */
enum class GraphScaleMode {
    NORMALIZED,
    SHARED_RAW,
    DUAL_AXIS
}

/**
 * Standard telemetry properties available from location history data.
 */
enum class StandardProperty(val key: String, val defaultUnit: String) {
    SPEED("speed", "km/h"),
    ACCELERATION("acceleration", "m/s²"),
    ALTITUDE("altitude", "m"),
    BEARING("bearing", "°"),
    ACCURACY("accuracy", "m"),
    SPEED_ACCURACY("speedAccuracy", "m/s"),
    BEARING_ACCURACY("bearingAccuracy", "°"),
    VERTICAL_ACCURACY("verticalAccuracy", "m"),
    DISTANCE("distance", "m"),
    LATITUDE("latitude", "°"),
    LONGITUDE("longitude", "°");

    companion object {
        fun fromKey(key: String): StandardProperty? {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
        }
    }
}

/**
 * Data source specification for a series.
 */
sealed class SeriesDataSource {
    data class Standard(val property: StandardProperty) : SeriesDataSource()
    data class Custom(val expression: String, val unit: String = "") : SeriesDataSource()
}

/**
 * Configuration for a single data series in a graph.
 */
data class SeriesConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dataSource: SeriesDataSource,
    val colorPresetId: String,
    val customColorStops: Map<Float, Long>? = null
)

/**
 * Configuration for a graph view, containing up to 5 series.
 */
data class GraphConfig(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val series: List<SeriesConfig> = emptyList(),
    val scaleMode: GraphScaleMode = GraphScaleMode.NORMALIZED
)

/**
 * Category for grouping color palettes.
 */
enum class ColorPresetCategory {
    PRESET,
    QUICK,
    CUSTOM
}

/**
 * Color gradient palette configuration with value stops and ARGB color values.
 */
data class ColorPreset(
    val id: String,
    val name: String,
    val category: ColorPresetCategory,
    val stops: Map<Float, Long>
)
