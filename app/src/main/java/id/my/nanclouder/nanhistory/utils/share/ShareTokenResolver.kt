package id.my.nanclouder.nanhistory.utils.share

import id.my.nanclouder.nanhistory.utils.history.EventRange
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import id.my.nanclouder.nanhistory.utils.history.LocationData
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Utility for resolving template tokens into dynamic event metadata.
 */
object ShareTokenResolver {

    private const val DEFAULT_TIME_FORMAT = "yyyy-MM-dd HH:mm"
    private const val FALLBACK_VALUE = "--"

    private val START_TIME_REGEX = Regex("""\{start_time(?::([^}]+))?\}""")
    private val END_TIME_REGEX = Regex("""\{end_time(?::([^}]+))?\}""")

    /**
     * Calculates total cumulative positive elevation gain in meters.
     * Returns null if no elevation data is present in the locations.
     */
    fun calculateElevationGain(locations: List<LocationData>): Double? {
        val validAltitudes = locations.mapNotNull { it.altitude }
        if (validAltitudes.size < 2) return null

        var gain = 0.0
        var hasValidTransition = false
        var prevAlt: Double? = null

        for (loc in locations) {
            val alt = loc.altitude ?: continue
            val prev = prevAlt
            if (prev != null) {
                if (alt > prev) {
                    gain += (alt - prev)
                }
                hasValidTransition = true
            }
            prevAlt = alt
        }

        return if (hasValidTransition) gain else null
    }

    /**
     * Resolves template string replacing all supported tokens with actual event and metric values.
     */
    fun resolveTokens(
        template: String,
        eventTitle: String,
        dateString: String,
        eventData: HistoryEvent?,
        metricsText: Map<String, String>,
        elevationGainMeters: Double? = null
    ): String {
        if (template.isEmpty()) return template

        var result = template
            .replace("{title}", eventTitle)
            .replace("{date}", dateString)

        // Metrics tokens
        metricsText.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }

        // Elevation gain
        val elevationStr = elevationGainMeters?.let { "${it.roundToInt()} m" } ?: FALLBACK_VALUE
        result = result.replace("{elevation_gain}", elevationStr)

        // Description
        val descriptionStr = eventData?.description ?: ""
        result = result.replace("{description}", descriptionStr)

        val startTime = eventData?.time
        val endTime = (eventData as? EventRange)?.end

        // Resolve {start_time} and {start_time:PATTERN}
        result = START_TIME_REGEX.replace(result) { matchResult ->
            val pattern = matchResult.groupValues.getOrNull(1)?.ifEmpty { null } ?: DEFAULT_TIME_FORMAT
            formatZonedDateTime(startTime, pattern)
        }

        // Resolve {end_time} and {end_time:PATTERN}
        result = END_TIME_REGEX.replace(result) { matchResult ->
            val pattern = matchResult.groupValues.getOrNull(1)?.ifEmpty { null } ?: DEFAULT_TIME_FORMAT
            formatZonedDateTime(endTime, pattern)
        }

        return result
    }

    private fun formatZonedDateTime(dateTime: ZonedDateTime?, pattern: String): String {
        if (dateTime == null) return FALLBACK_VALUE
        return runCatching {
            DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(dateTime)
        }.getOrElse {
            // Fallback to default pattern if custom pattern contains invalid letters
            runCatching {
                DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT, Locale.getDefault()).format(dateTime)
            }.getOrDefault(FALLBACK_VALUE)
        }
    }
}
