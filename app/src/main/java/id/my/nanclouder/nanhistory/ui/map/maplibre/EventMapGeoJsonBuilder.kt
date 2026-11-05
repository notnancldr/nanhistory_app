package id.my.nanclouder.nanhistory.ui.map.maplibre

import androidx.compose.ui.graphics.Color
import id.my.nanclouder.nanhistory.activity.eventLocation.common.DataMode
import id.my.nanclouder.nanhistory.activity.eventLocation.common.calculateAccelerationColor
import id.my.nanclouder.nanhistory.activity.eventLocation.common.calculateAccuracyColor
import id.my.nanclouder.nanhistory.activity.eventLocation.common.calculateAltitudeColor
import id.my.nanclouder.nanhistory.activity.eventLocation.common.calculateColor
import id.my.nanclouder.nanhistory.activity.eventLocation.common.calculateTimeColor
import id.my.nanclouder.nanhistory.state.ReplayTelemetry
import id.my.nanclouder.nanhistory.utils.Coordinate
import id.my.nanclouder.nanhistory.utils.HistoryLocationData
import id.my.nanclouder.nanhistory.utils.getLocationData
import id.my.nanclouder.nanhistory.utils.history.LocationData
import java.time.ZonedDateTime
import kotlin.math.roundToInt

/**
 * Builds compact GeoJSON strings for [EventLocationMap] layers with dynamic data mode colors.
 */
object EventMapGeoJsonBuilder {

    fun Color.toHex(): String {
        val r = (red * 255f).roundToInt().coerceIn(0, 255)
        val g = (green * 255f).roundToInt().coerceIn(0, 255)
        val b = (blue * 255f).roundToInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X", r, g, b)
    }

    fun calculateSegmentColor(
        seg: HistoryLocationData,
        dataMode: DataMode,
        firstTime: ZonedDateTime,
        lastTime: ZonedDateTime
    ): Color {
        return when (dataMode) {
            DataMode.SPEED -> calculateColor(seg.speed.roundToInt())
            DataMode.ACCELERATION -> calculateAccelerationColor(seg.acceleration.roundToInt())
            DataMode.TIME -> calculateTimeColor(seg.start, seg.end, firstTime, lastTime)
            DataMode.ACCURACY -> calculateAccuracyColor(seg.locationData.firstOrNull()?.accuracy)
            DataMode.BEARING -> Color.hsl(seg.locationData.firstOrNull()?.bearing ?: 0f, 1f, 0.5f)
            DataMode.ALTITUDE -> calculateAltitudeColor(seg.locationData.firstOrNull()?.altitude)
        }
    }

    fun calculateColorForPoints(
        p1: LocationData,
        p2: LocationData,
        dataMode: DataMode,
        firstTime: ZonedDateTime,
        lastTime: ZonedDateTime
    ): Color {
        return when (dataMode) {
            DataMode.SPEED -> {
                val speed = p1.speed ?: run {
                    val dist = id.my.nanclouder.nanhistory.state.ReplayInterpolator
                        .calculateDistanceMeters(p1.location, p2.location)
                    val dt = (p2.time.toInstant().toEpochMilli() - p1.time.toInstant().toEpochMilli()).coerceAtLeast(1L)
                    (dist / 1000.0 / (dt / 3600000.0)).toFloat()
                }
                calculateColor(speed.roundToInt())
            }
            DataMode.ACCELERATION -> {
                val a = if (p1.speed != null && p2.speed != null) {
                    val dt = (p2.time.toInstant().toEpochMilli() - p1.time.toInstant().toEpochMilli()).coerceAtLeast(1L) / 1000f
                    ((p2.speed - p1.speed) / dt).roundToInt()
                } else 0
                calculateAccelerationColor(a)
            }
            DataMode.TIME -> calculateTimeColor(p1.time, p2.time, firstTime, lastTime)
            DataMode.ACCURACY -> calculateAccuracyColor(p1.accuracy)
            DataMode.BEARING -> Color.hsl(
                p1.bearing ?: id.my.nanclouder.nanhistory.state.ReplayInterpolator
                    .calculateBearing(p1.location, p2.location),
                1f,
                0.5f
            )
            DataMode.ALTITUDE -> calculateAltitudeColor(p1.altitude)
        }
    }

    fun buildTrackGeoJson(
        locations: List<LocationData>,
        segments: List<HistoryLocationData>,
        showData: Boolean,
        cutMode: Boolean,
        dataMode: DataMode
    ): String {
        if (locations.size < 2) return emptyFeatureCollection()

        val sb = StringBuilder(locations.size * 64)
        sb.append("{\"type\":\"FeatureCollection\",\"features\":[")

        if (showData && !cutMode && segments.isNotEmpty()) {
            val firstTime = locations.first().time
            val lastTime = locations.last().time
            segments.forEachIndexed { i, seg ->
                if (i > 0) sb.append(",")
                val colorHex = calculateSegmentColor(seg, dataMode, firstTime, lastTime).toHex()
                sb.append("{\"type\":\"Feature\",\"properties\":{\"segIndex\":").append(i)
                sb.append(",\"speed\":").append(seg.speed)
                sb.append(",\"color\":\"").append(colorHex).append("\"")
                sb.append("},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[")
                seg.points.forEachIndexed { pi, p ->
                    if (pi > 0) sb.append(",")
                    sb.append("[").append(p.longitude).append(",").append(p.latitude).append("]")
                }
                sb.append("]}}")
            }
        } else {
            val defaultColorHex = if (cutMode) "#757575" else "#2196F3"
            sb.append("{\"type\":\"Feature\",\"properties\":{\"color\":\"").append(defaultColorHex).append("\"}")
            sb.append(",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[")
            locations.forEachIndexed { i, loc ->
                if (i > 0) sb.append(",")
                sb.append("[").append(loc.location.longitude).append(",").append(loc.location.latitude).append("]")
            }
            sb.append("]}}")
        }

        sb.append("]}")
        return sb.toString()
    }

    fun buildTraveledGeoJson(
        traveled: List<LocationData>,
        currentCoord: Coordinate?,
        showData: Boolean = false,
        dataMode: DataMode = DataMode.SPEED,
        allLocations: List<LocationData> = emptyList()
    ): String {
        if (traveled.isEmpty() && currentCoord == null) return emptyFeatureCollection()

        val fullPoints = if (currentCoord != null) {
            val lastTime = traveled.lastOrNull()?.time ?: ZonedDateTime.now()
            traveled + LocationData(time = lastTime, location = currentCoord)
        } else {
            traveled
        }

        if (fullPoints.size < 2) return emptyFeatureCollection()

        val sb = StringBuilder(fullPoints.size * 64)
        sb.append("{\"type\":\"FeatureCollection\",\"features\":[")

        if (showData && allLocations.size >= 2) {
            val firstTime = allLocations.first().time
            val lastTime = allLocations.last().time
            for (i in 1 until fullPoints.size) {
                if (i > 1) sb.append(",")
                val p1 = fullPoints[i - 1]
                val p2 = fullPoints[i]
                val colorHex = calculateColorForPoints(p1, p2, dataMode, firstTime, lastTime).toHex()
                sb.append("{\"type\":\"Feature\",\"properties\":{\"color\":\"").append(colorHex).append("\"}")
                sb.append(",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[")
                sb.append("[").append(p1.location.longitude).append(",").append(p1.location.latitude).append("],")
                sb.append("[").append(p2.location.longitude).append(",").append(p2.location.latitude).append("]")
                sb.append("]}}")
            }
        } else {
            appendSingleTraveledFeature(sb, fullPoints)
        }

        sb.append("]}")
        return sb.toString()
    }

    private fun appendSingleTraveledFeature(sb: StringBuilder, points: List<LocationData>) {
        sb.append("{\"type\":\"Feature\",\"properties\":{\"color\":\"#2196F3\"}")
        sb.append(",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[")
        points.forEachIndexed { i, loc ->
            if (i > 0) sb.append(",")
            sb.append("[").append(loc.location.longitude).append(",").append(loc.location.latitude).append("]")
        }
        sb.append("]}}")
    }

    fun buildPolylineGeoJson(coords: List<Coordinate>): String {
        if (coords.size < 2) return emptyFeatureCollection()
        val sb = StringBuilder(coords.size * 32)
        sb.append("{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\"")
        sb.append(",\"properties\":{\"color\":\"#2196F3\"}")
        sb.append(",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[")
        coords.forEachIndexed { i, c ->
            if (i > 0) sb.append(",")
            sb.append("[").append(c.longitude).append(",").append(c.latitude).append("]")
        }
        sb.append("]}}]}")
        return sb.toString()
    }

    fun buildStartEndPointsGeoJson(first: Coordinate?, last: Coordinate?): String {
        if (first == null) return emptyFeatureCollection()
        val sb = StringBuilder()
        sb.append("{\"type\":\"FeatureCollection\",\"features\":[")
        sb.append("{\"type\":\"Feature\",\"properties\":{\"pointType\":\"start\"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[")
        sb.append(first.longitude).append(",").append(first.latitude).append("]}}")
        if (last != null && (last.latitude != first.latitude || last.longitude != first.longitude)) {
            sb.append(",{\"type\":\"Feature\",\"properties\":{\"pointType\":\"end\"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[")
            sb.append(last.longitude).append(",").append(last.latitude).append("]}}")
        }
        sb.append("]}")
        return sb.toString()
    }

    fun buildPointsGeoJson(locations: List<LocationData>): String {
        if (locations.isEmpty()) return emptyFeatureCollection()
        val sb = StringBuilder(locations.size * 64)
        sb.append("{\"type\":\"FeatureCollection\",\"features\":[")
        locations.forEachIndexed { i, loc ->
            if (i > 0) sb.append(",")
            sb.append("{\"type\":\"Feature\",\"properties\":{\"time\":\"")
            sb.append(loc.time.toOffsetDateTime().toString())
            sb.append("\"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[")
            sb.append(loc.location.longitude).append(",").append(loc.location.latitude).append("]}}")
        }
        sb.append("]}")
        return sb.toString()
    }

    fun buildDwellsGeoJson(dwells: List<LocationData>): String {
        if (dwells.isEmpty()) return emptyFeatureCollection()
        val sb = StringBuilder(dwells.size * 64)
        sb.append("{\"type\":\"FeatureCollection\",\"features\":[")
        dwells.forEachIndexed { i, d ->
            if (i > 0) sb.append(",")
            sb.append("{\"type\":\"Feature\",\"properties\":{\"dwell\":true}")
            sb.append(",\"geometry\":{\"type\":\"Point\",\"coordinates\":[")
            sb.append(d.location.longitude).append(",").append(d.location.latitude).append("]}}")
        }
        sb.append("]}")
        return sb.toString()
    }

    fun buildPinsGeoJson(
        firstLocation: LocationData?,
        lastLocation: LocationData?,
        cutStart: ZonedDateTime?,
        cutEnd: ZonedDateTime?,
        locations: List<LocationData>
    ): String {
        val pins = mutableListOf<Pair<Coordinate, ZonedDateTime>>()
        if (firstLocation != null) pins.add(firstLocation.location to firstLocation.time)
        if (lastLocation != null && lastLocation != firstLocation) {
            pins.add(lastLocation.location to lastLocation.time)
        }

        if (cutStart != null) {
            val startLoc = locations.find { it.time == cutStart } ?: firstLocation
            if (startLoc != null) pins.add(startLoc.location to startLoc.time)
        }
        if (cutEnd != null) {
            val endLoc = locations.find { it.time == cutEnd } ?: lastLocation
            if (endLoc != null) pins.add(endLoc.location to endLoc.time)
        }

        if (pins.isEmpty()) return emptyFeatureCollection()
        val sb = StringBuilder(pins.size * 64)
        sb.append("{\"type\":\"FeatureCollection\",\"features\":[")
        pins.forEachIndexed { i, p ->
            if (i > 0) sb.append(",")
            sb.append("{\"type\":\"Feature\",\"properties\":{\"time\":\"")
            sb.append(p.second.toOffsetDateTime().toString())
            sb.append("\"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[")
            sb.append(p.first.longitude).append(",").append(p.first.latitude).append("]}}")
        }
        sb.append("]}")
        return sb.toString()
    }

    fun buildReplayMarkerGeoJson(telemetry: ReplayTelemetry?): String {
        if (telemetry == null) return emptyFeatureCollection()
        val coord = telemetry.coordinate
        return "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\"," +
            "\"properties\":{\"bearing\":${telemetry.bearingDegrees}," +
            "\"accuracy\":${telemetry.accuracyMeters ?: 0f}}," +
            "\"geometry\":{\"type\":\"Point\",\"coordinates\":[${coord.longitude},${coord.latitude}]}}]}"
    }

    private fun emptyFeatureCollection(): String = "{\"type\":\"FeatureCollection\",\"features\":[]}"

    fun toOsmBoundingBox(bounds: org.maplibre.compose.util.VisibleBounds): org.osmdroid.util.BoundingBox {
        fun normalizeLon(lon: Double): Double {
            var l = lon
            while (l > 180.0) l -= 360.0
            while (l < -180.0) l += 360.0
            return l
        }
        val n = bounds.north.coerceIn(-85.0, 85.0)
        val s = bounds.south.coerceIn(-85.0, 85.0)
        val e = normalizeLon(bounds.east)
        val w = normalizeLon(bounds.west)
        return org.osmdroid.util.BoundingBox(n, e, s, w)
    }
}
