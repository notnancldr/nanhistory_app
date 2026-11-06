package id.my.nanclouder.nanhistory.domain.repository

import id.my.nanclouder.nanhistory.domain.model.LocationBounds
import id.my.nanclouder.nanhistory.domain.model.LocationData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Domain port for reading, querying, and storing GPS location data.
 */
interface LocationRepository {
    fun observeLocationsByEventId(eventId: String): Flow<List<LocationData>> = emptyFlow()
    suspend fun getLocationBoundsByEventId(eventId: String): LocationBounds? = null
    suspend fun getFirstLocationByEventId(eventId: String): LocationData? = null
    suspend fun getLastLocationByEventId(eventId: String): LocationData? = null
    suspend fun getRenderedLocations(
        id: String,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        step: Int
    ): List<LocationData> = emptyList()
    suspend fun getLocationsBetweenTimestamps(
        eventId: String,
        startMillis: Long,
        endMillis: Long
    ): List<LocationData> = emptyList()
    suspend fun insertLocations(eventId: String, locations: List<LocationData>) {}
    suspend fun getLocationBoundsInRange(from: String, until: String): LocationBounds? = null
}
