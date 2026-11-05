package id.my.nanclouder.nanhistory.utils.share

import id.my.nanclouder.nanhistory.utils.Coordinate
import id.my.nanclouder.nanhistory.utils.history.EventRange
import id.my.nanclouder.nanhistory.utils.history.LocationData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ShareTokenResolverTest {

    @Test
    fun calculateElevationGain_sumsPositiveDifferences() {
        val time = ZonedDateTime.of(2026, 5, 1, 10, 0, 0, 0, ZoneId.of("UTC"))
        val c = Coordinate(0.0, 0.0)
        val locations = listOf(
            LocationData(location = c, time = time, altitude = 100.0),
            LocationData(location = c, time = time, altitude = 150.0),
            LocationData(location = c, time = time, altitude = 120.0),
            LocationData(location = c, time = time, altitude = 170.0)
        )
        // 100 -> 150 (+50), 150 -> 120 (0), 120 -> 170 (+50) => Total = 100.0
        val gain = ShareTokenResolver.calculateElevationGain(locations)
        assertEquals(100.0, gain!!, 0.001)
    }

    @Test
    fun calculateElevationGain_returnsNullWhenNoAltitudeData() {
        val time = ZonedDateTime.of(2026, 5, 1, 10, 0, 0, 0, ZoneId.of("UTC"))
        val c = Coordinate(0.0, 0.0)
        val locations = listOf(
            LocationData(location = c, time = time, altitude = null),
            LocationData(location = c, time = time, altitude = null)
        )
        assertNull(ShareTokenResolver.calculateElevationGain(locations))
    }

    @Test
    fun resolveTokens_replacesTokensCorrectly() {
        val start = ZonedDateTime.of(2026, 5, 1, 8, 30, 0, 0, ZoneId.of("UTC"))
        val end = ZonedDateTime.of(2026, 5, 1, 11, 45, 0, 0, ZoneId.of("UTC"))

        val event = EventRange(
            title = "Morning Ride",
            description = "Cruising along the coast",
            time = start,
            end = end
        )

        val template = "Trip: {title}\nDesc: {description}\nFrom {start_time:HH:mm} to {end_time:HH:mm}\n" +
            "Gain: {elevation_gain}\nDist: {distance}"
        val metrics = mapOf("distance" to "45.2 km")

        val resolved = ShareTokenResolver.resolveTokens(
            template = template,
            eventTitle = event.title,
            dateString = "May 01, 2026",
            eventData = event,
            metricsText = metrics,
            elevationGainMeters = 230.4
        )

        val expected = "Trip: Morning Ride\nDesc: Cruising along the coast\nFrom 08:30 to 11:45\n" +
            "Gain: 230 m\nDist: 45.2 km"
        assertEquals(expected, resolved)
    }

    @Test
    fun resolveTokens_handlesMissingDataGracefully() {
        val template = "Start: {start_time}\nEnd: {end_time}\nGain: {elevation_gain}"
        val resolved = ShareTokenResolver.resolveTokens(
            template = template,
            eventTitle = "Instant Event",
            dateString = "May 01, 2026",
            eventData = null,
            metricsText = emptyMap(),
            elevationGainMeters = null
        )

        val expected = "Start: --\nEnd: --\nGain: --"
        assertEquals(expected, resolved)
    }
}
