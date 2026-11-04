package id.my.nanclouder.nanhistory.activity.eventLocation.common

import androidx.compose.ui.graphics.Color
import id.my.nanclouder.nanhistory.utils.calculateDistance
import id.my.nanclouder.nanhistory.utils.calculateSpeed
import org.osmdroid.util.GeoPoint
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class DataMode {
    SPEED,
    ACCELERATION,
    TIME,
    ACCURACY,
    BEARING,
    ALTITUDE
}

fun calculateColor(speed: Int): Color {
    val lightness = max(min(speed / 20f, .5f), 0.3f)
    val hue = max(min(speed * 2.5f, 270f), 0f)
    return Color.hsl(hue, 1f, lightness)
}

fun calculateAltitudeColor(altitude: Double?): Color {
    if (altitude == null) {
        return Color(0xFF808080)
    }

    val clampedAltitude = altitude.coerceIn(0.0, 1500.0)
    val normalized = clampedAltitude / 1500.0
    val hue = 180.0 - (normalized * 180.0)
    val saturation = (0.6 + normalized * 0.4).coerceIn(0.6, 1.0)
    val lightness = (0.75 - (normalized * 0.15)).coerceIn(0.35, 0.75)

    return Color.hsl(hue.toFloat(), saturation.toFloat(), lightness.toFloat())
}

fun calculateAccuracyColor(accuracy: Float?): Color {
    if (accuracy == null) {
        return Color(0xFF808080)
    }

    val clampedAccuracy = accuracy.coerceIn(5f, 1000f)
    val normalized = (clampedAccuracy - 5f) / (1000f - 5f)

    val hue =
        when {
            normalized < 0.33f -> {
                val t = normalized / 0.33f
                120f - (t * 60f)
            }
            normalized < 0.66f -> {
                val t = (normalized - 0.33f) / 0.33f
                60f - (t * 30f)
            }
            else -> {
                val t = (normalized - 0.66f) / 0.34f
                30f - (t * 30f)
            }
        }

    val saturation = (0.5f + normalized * 0.5f).coerceIn(0.5f, 1f)
    val lightness = (0.55f - normalized * 0.15f).coerceIn(0.4f, 0.6f)

    return Color.hsl(hue, saturation, lightness)
}

fun calculateAccelerationColor(acceleration: Int): Color {
    val normalized = (acceleration.coerceIn(-10000, 10000) / 10000f)

    val hue =
        when {
            normalized < 0f -> 30f * (1f + normalized)
            normalized > 0f -> 120f + (120f * normalized)
            else -> 60f
        }

    val saturation =
        when {
            abs(normalized) > 0.5f -> 1f
            else -> sqrt(abs(normalized) * 2f).coerceIn(0f, 1f)
        }

    val brightness = 1f - (abs(normalized) * 0.3f)

    return Color.hsv(hue, saturation, brightness)
}

fun calculateTimeColor(
    start: ZonedDateTime,
    end: ZonedDateTime,
    firstTime: ZonedDateTime,
    lastTime: ZonedDateTime
): Color {
    val totalDuration = Duration.between(firstTime, lastTime).seconds.toFloat()
    val segmentTime = Duration.between(firstTime, start).seconds.toFloat()
    val progress = (segmentTime / totalDuration).coerceIn(0f, 1f)

    val blueComponent = 1f - (progress * 0.3f)
    val grayComponent = 0.5f + (progress * 0.3f)

    return Color(
        red = grayComponent * 0.4f,
        green = grayComponent * 0.6f,
        blue = blueComponent,
        alpha = 1f
    )
}

fun calculateBearingArcPoints(
    center: GeoPoint,
    bearing: Float,
    bearingAccuracy: Float,
    radius: Double
): List<GeoPoint> {
    val points = mutableListOf<GeoPoint>()
    points.add(center)
    val startAngle = bearing - bearingAccuracy
    val endAngle = bearing + bearingAccuracy
    val step = 5
    var angle = startAngle
    while (angle <= endAngle) {
        points.add(center.destinationPoint(radius, angle.toDouble()))
        angle += step
    }
    points.add(center.destinationPoint(radius, endAngle.toDouble()))
    points.add(center)
    return points
}
