package id.my.nanclouder.nanhistory.core.ui.map

import id.my.nanclouder.nanhistory.core.common.location.getLocationData
import id.my.nanclouder.nanhistory.domain.logic.ReplayTelemetry
import id.my.nanclouder.nanhistory.domain.model.Coordinate
import id.my.nanclouder.nanhistory.domain.model.HistoryLocationData
import id.my.nanclouder.nanhistory.domain.model.LocationData
import id.my.nanclouder.nanhistory.ui.map.maplibre.EventMapGeoJsonBuilder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class EventMapGeoJsonBuilderTest {

    @Test
    fun buildTrackGeoJson_producesValidFeatureCollection() {
        val now = ZonedDateTime.now()
        val locs = listOf(
            LocationData(now, Coordinate(10.0, 20.0)),
            LocationData(now.plusSeconds(10), Coordinate(10.1, 20.1))
        )
        val json = EventMapGeoJsonBuilder.buildTrackGeoJson(
            locations = locs,
            segments = emptyList(),
            showData = false,
            cutMode = false,
            dataMode = DataMode.SPEED
        )

        assertTrue(json.contains("\"type\":\"FeatureCollection\""))
        assertTrue(json.contains("\"type\":\"LineString\""))
        assertTrue(json.contains("[20.0,10.0]"))
        assertTrue(json.contains("[20.1,10.1]"))
        assertTrue(json.contains("\"color\":"))
    }

    @Test
    fun buildTrackGeoJson_coloredDataModes_allProduceHexColors() {
        val now = ZonedDateTime.now()
        val locs = listOf(
            LocationData(
                time = now,
                location = Coordinate(10.0, 20.0),
                altitude = 120.0,
                accuracy = 8f,
                speed = 15f,
                bearing = 45f
            ),
            LocationData(
                time = now.plusSeconds(10),
                location = Coordinate(10.01, 20.01),
                altitude = 150.0,
                accuracy = 12f,
                speed = 25f,
                bearing = 90f
            ),
            LocationData(
                time = now.plusSeconds(20),
                location = Coordinate(10.02, 20.02),
                altitude = 180.0,
                accuracy = 16f,
                speed = 35f,
                bearing = 135f
            )
        )
        val segments = listOf(
            HistoryLocationData(
                speed = 20f,
                acceleration = 150f,
                points = listOf(locs[0].location, locs[1].location),
                locationData = listOf(locs[0], locs[1]),
                start = locs[0].time,
                end = locs[1].time,
                distance = 100f
            ),
            HistoryLocationData(
                speed = 30f,
                acceleration = -50f,
                points = listOf(locs[1].location, locs[2].location),
                locationData = listOf(locs[1], locs[2]),
                start = locs[1].time,
                end = locs[2].time,
                distance = 100f
            )
        )
        assertTrue(segments.isNotEmpty())

        val modes = listOf(
            DataMode.SPEED,
            DataMode.ACCELERATION,
            DataMode.TIME,
            DataMode.ACCURACY,
            DataMode.BEARING,
            DataMode.ALTITUDE
        )

        for (mode in modes) {
            val json = EventMapGeoJsonBuilder.buildTrackGeoJson(
                locations = locs,
                segments = segments,
                showData = true,
                cutMode = false,
                dataMode = mode
            )
            assertTrue(json.contains("\"type\":\"FeatureCollection\""))
            assertTrue(json.contains("\"segIndex\":0"))
            assertTrue("Mode $mode should generate valid hex color", json.contains("\"color\":\"#"))
        }
    }

    @Test
    fun buildTraveledGeoJson_withShowData_producesColoredSegments() {
        val now = ZonedDateTime.now()
        val locs = listOf(
            LocationData(now, Coordinate(1.0, 2.0), speed = 10f),
            LocationData(now.plusSeconds(10), Coordinate(1.01, 2.01), speed = 20f)
        )
        val current = Coordinate(1.02, 2.02)
        val json = EventMapGeoJsonBuilder.buildTraveledGeoJson(
            traveled = locs,
            currentCoord = current,
            showData = true,
            dataMode = DataMode.SPEED,
            allLocations = locs
        )

        assertTrue(json.contains("\"type\":\"FeatureCollection\""))
        assertTrue(json.contains("\"color\":\"#"))
    }

    @Test
    fun buildReplayMarkerGeoJson_includesTelemetryBearingAndCoordinates() {
        val telemetry = ReplayTelemetry(
            coordinate = Coordinate(5.0, 6.0),
            timestamp = ZonedDateTime.now(),
            speedKmh = 50f,
            altitudeMeters = 100.0,
            accuracyMeters = 8f,
            bearingDegrees = 180f,
            distanceMeters = 1200.0
        )
        val json = EventMapGeoJsonBuilder.buildReplayMarkerGeoJson(telemetry)

        assertTrue(json.contains("[6.0,5.0]"))
        assertTrue(json.contains("\"bearing\":180.0"))
        assertTrue(json.contains("\"accuracy\":8.0"))
    }

    @Test
    fun buildDwellsGeoJson_emptyWhenNoDwells() {
        val json = EventMapGeoJsonBuilder.buildDwellsGeoJson(emptyList())
        assertTrue(json.contains("\"features\":[]"))
    }

    @Test
    fun buildPointsGeoJson_containsAllPointsWithTime() {
        val now = ZonedDateTime.now()
        val locs = listOf(
            LocationData(now, Coordinate(0.0, 0.0))
        )
        val json = EventMapGeoJsonBuilder.buildPointsGeoJson(locs)

        assertTrue(json.contains("\"time\":"))
        assertTrue(json.contains("[0.0,0.0]"))
    }
}
