package id.my.nanclouder.nanhistory.feature.yearlymap

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

/**
 * Color and width scaling logic for corridor frequency visualization.
 * Uses logarithmic scaling so heavy commute corridors do not wash out occasional routes.
 */
object CorridorColorScale {

    const val MIN_WIDTH_DP: Float = 3.0f
    const val MAX_WIDTH_DP: Float = 7.0f

    /**
     * Calculates the normalized progress [0f..1f] for a segment's event count relative to [maxCount].
     * Uses logarithmic mapping t = ln(count) / ln(maxCount) folded with a 0.08 base threshold
     * for multiple trips, ensuring count == 1 remains distinctly recognizable.
     */
    fun calculateT(count: Int, maxCount: Int): Float {
        if (count <= 1 || maxCount <= 1) return 0.0f
        val logCount = ln(count.toDouble())
        val logMax = ln(maxCount.toDouble())
        val u = (logCount / logMax).toFloat().coerceIn(0.0f, 1.0f)
        return (0.08f + 0.92f * u).coerceIn(0.0f, 1.0f)
    }

    /**
     * Calculates the corridor stroke width in Dp for a given [t] in [0f..1f].
     */
    fun calculateStrokeWidth(t: Float): Dp {
        val clampedT = t.coerceIn(0.0f, 1.0f)
        return (MIN_WIDTH_DP + clampedT * (MAX_WIDTH_DP - MIN_WIDTH_DP)).dp
    }

    /**
     * Calculates color from count and maxCount using HSV color model.
     */
    fun calculateColor(count: Int, maxCount: Int): Color {
        val t = calculateT(count, maxCount)
        return calculateColorFromT(t, hasMultipleEvents = count > 1)
    }

    /**
     * Calculates the Color for a given [t] in [0f..1f].
     */
    fun calculateColorFromT(t: Float, hasMultipleEvents: Boolean = t > 0f): Color {
        val clampedT = t.coerceIn(0.0f, 1.0f)
        val lightness = (clampedT * 1.5f - 0.75f).pow(2) + 0.5f
        val hue = (240f - (clampedT * 220f)).coerceIn(0f, 360f)
        val value = (0.5f * lightness).coerceIn(0f, 1f)
        return hsvToColor(hue, 1.0f, value)
    }

    /**
     * Pre-calculated color stops for MapLibre interpolate expression across t [0f..1f].
     * 9 distinct stops for smooth and accurate GPU gradient interpolation.
     */
    val COLOR_STOPS: List<Pair<Float, Color>> = listOf(
        0.000f to calculateColorFromT(0.000f),
        0.080f to calculateColorFromT(0.080f),
        0.200f to calculateColorFromT(0.200f),
        0.350f to calculateColorFromT(0.350f),
        0.500f to calculateColorFromT(0.500f),
        0.650f to calculateColorFromT(0.650f),
        0.800f to calculateColorFromT(0.800f),
        0.900f to calculateColorFromT(0.900f),
        1.000f to calculateColorFromT(1.000f),
    )

    /**
     * Pure Kotlin HSV to Compose Color conversion.
     */
    fun hsvToColor(hue: Float, saturation: Float, value: Float, alpha: Float = 1.0f): Color {
        val h = (hue % 360f + 360f) % 360f
        val s = saturation.coerceIn(0f, 1f)
        val v = value.coerceIn(0f, 1f)

        val c = v * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = v - c

        val (rPrime, gPrime, bPrime) = when (floor(h / 60f).toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return Color(
            red = (rPrime + m).coerceIn(0f, 1f),
            green = (gPrime + m).coerceIn(0f, 1f),
            blue = (bPrime + m).coerceIn(0f, 1f),
            alpha = alpha.coerceIn(0f, 1f)
        )
    }
}
