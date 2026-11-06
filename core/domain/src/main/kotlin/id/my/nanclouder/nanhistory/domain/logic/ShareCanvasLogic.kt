package id.my.nanclouder.nanhistory.domain.logic

import id.my.nanclouder.nanhistory.domain.model.EventRange
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.model.LocationData
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pure domain logic for ShareMap canvas normalized positioning, bounds calculation,
 * elevation gain summation, and string token interpolation.
 */
object ShareCanvasLogic {

    private const val DEFAULT_TIME_FORMAT = "yyyy-MM-dd HH:mm"
    private const val FALLBACK_VALUE = "--"

    private val START_TIME_REGEX = Regex("""\{start_time(?::([^}]+))?\}""")
    private val END_TIME_REGEX = Regex("""\{end_time(?::([^}]+))?\}""")

    /**
     * Calculates updated element position when dragged by ([deltaXPx], [deltaYPx]) on a canvas of ([canvasWidthPx], [canvasHeightPx]).
     */
    fun calculateDragPosition(
        xPercent: Float,
        yPercent: Float,
        widthPercent: Float,
        heightPercent: Float,
        deltaXPx: Float,
        deltaYPx: Float,
        canvasWidthPx: Float,
        canvasHeightPx: Float
    ): Pair<Float, Float> {
        val deltaXPercent = if (canvasWidthPx > 0) deltaXPx / canvasWidthPx else 0f
        val deltaYPercent = if (canvasHeightPx > 0) deltaYPx / canvasHeightPx else 0f
        val newX = (xPercent + deltaXPercent).coerceIn(0f, 1f - widthPercent)
        val newY = (yPercent + deltaYPercent).coerceIn(0f, 1f - heightPercent)
        return Pair(newX, newY)
    }

    /**
     * Calculates updated bottom-right corner resize dimensions on a canvas of ([canvasWidthPx], [canvasHeightPx]).
     */
    fun calculateResize(
        widthPercent: Float,
        heightPercent: Float,
        xPercent: Float,
        yPercent: Float,
        deltaXPx: Float,
        deltaYPx: Float,
        canvasWidthPx: Float,
        canvasHeightPx: Float,
        minWidthPercent: Float = 0.08f,
        minHeightPercent: Float = 0.04f
    ): Pair<Float, Float> {
        val deltaWPercent = if (canvasWidthPx > 0) deltaXPx / canvasWidthPx else 0f
        val deltaHPercent = if (canvasHeightPx > 0) deltaYPx / canvasHeightPx else 0f
        val maxW = 1f - xPercent
        val maxH = 1f - yPercent
        val newW = (widthPercent + deltaWPercent).coerceIn(minWidthPercent, maxW)
        val newH = (heightPercent + deltaHPercent).coerceIn(minHeightPercent, maxH)
        return Pair(newW, newH)
    }

    /**
     * Calculates total cumulative positive elevation gain in meters.
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
            runCatching {
                DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT, Locale.getDefault()).format(dateTime)
            }.getOrDefault(FALLBACK_VALUE)
        }
    }
}
