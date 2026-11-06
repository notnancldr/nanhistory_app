package id.my.nanclouder.nanhistory.data.repository

import id.my.nanclouder.nanhistory.db.AppDao
import id.my.nanclouder.nanhistory.db.toLocationData
import id.my.nanclouder.nanhistory.db.toLocationEntity
import id.my.nanclouder.nanhistory.domain.model.LocationBounds
import id.my.nanclouder.nanhistory.domain.model.LocationData
import id.my.nanclouder.nanhistory.domain.repository.LocationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Room-backed implementation of [LocationRepository].
 */
class RoomLocationRepository(
    private val daoProvider: () -> AppDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LocationRepository {

    override fun observeLocationsByEventId(eventId: String): Flow<List<LocationData>> {
        return daoProvider().getLocationsByEventId(eventId)
            .map { list -> list.map { it.toLocationData() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun getLocationBoundsByEventId(eventId: String): LocationBounds? = withContext(ioDispatcher) {
        daoProvider().getLocationBoundsByEventId(eventId)?.let {
            LocationBounds(it.minLat, it.maxLat, it.minLon, it.maxLon)
        }
    }

    override suspend fun getFirstLocationByEventId(eventId: String): LocationData? = withContext(ioDispatcher) {
        daoProvider().getFirstLocationByEventId(eventId)?.toLocationData()
    }

    override suspend fun getLastLocationByEventId(eventId: String): LocationData? = withContext(ioDispatcher) {
        daoProvider().getLastLocationByEventId(eventId)?.toLocationData()
    }

    override suspend fun getRenderedLocations(
        id: String,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        step: Int
    ): List<LocationData> = withContext(ioDispatcher) {
        daoProvider().getRenderedLocations(
            id = id,
            minLat = minLat,
            maxLat = maxLat,
            minLon = minLon,
            maxLon = maxLon,
            step = step
        ).map { it.toLocationData() }
    }

    override suspend fun getRenderedYearlyLocations(
        startDate: String?,
        endDate: String?,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        step: Int
    ): List<LocationData> = withContext(ioDispatcher) {
        daoProvider().getRenderedYearlyLocations(
            startDate = startDate,
            endDate = endDate,
            minLat = minLat,
            maxLat = maxLat,
            minLon = minLon,
            maxLon = maxLon,
            step = step
        ).map { it.toLocationData() }
    }

    override suspend fun getLocationsBetweenTimestamps(
        eventId: String,
        startMillis: Long,
        endMillis: Long
    ): List<LocationData> = withContext(ioDispatcher) {
        daoProvider().getLocationsBetweenTimestamps(eventId, startMillis, endMillis)
            .map { it.toLocationData() }
    }

    override suspend fun insertLocations(eventId: String, locations: List<LocationData>) = withContext(ioDispatcher) {
        if (locations.isNotEmpty()) {
            daoProvider().insertLocations(locations.map { it.toLocationEntity(eventId) })
        }
    }

    override suspend fun getLocationBoundsInRange(from: String, until: String): LocationBounds? = withContext(ioDispatcher) {
        daoProvider().getLocationBoundsInRange(from, until)?.let {
            LocationBounds(it.minLat, it.maxLat, it.minLon, it.maxLon)
        }
    }
}
