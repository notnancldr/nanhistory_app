package id.my.nanclouder.nanhistory.data.repository

import id.my.nanclouder.nanhistory.db.CorridorNetworkDao
import id.my.nanclouder.nanhistory.db.CorridorNetworkEntity
import id.my.nanclouder.nanhistory.domain.model.CorridorSnapshot
import id.my.nanclouder.nanhistory.domain.repository.CorridorRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun CorridorNetworkEntity.toCorridorSnapshot() = CorridorSnapshot(
    rangeKey = rangeKey,
    startDate = startDate,
    endDate = endDate,
    version = version,
    updatedAt = updatedAt,
    boundsNorth = boundsNorth,
    boundsSouth = boundsSouth,
    boundsEast = boundsEast,
    boundsWest = boundsWest,
    maxCount = maxCount,
    totalDistanceMeters = totalDistanceMeters,
    networkBlob = networkBlob
)

fun CorridorSnapshot.toCorridorNetworkEntity() = CorridorNetworkEntity(
    rangeKey = rangeKey,
    startDate = startDate,
    endDate = endDate,
    version = version,
    updatedAt = updatedAt,
    boundsNorth = boundsNorth,
    boundsSouth = boundsSouth,
    boundsEast = boundsEast,
    boundsWest = boundsWest,
    maxCount = maxCount,
    totalDistanceMeters = totalDistanceMeters,
    networkBlob = networkBlob
)

/**
 * Room-backed implementation of [CorridorRepository].
 */
class RoomCorridorRepository(
    private val daoProvider: () -> CorridorNetworkDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CorridorRepository {

    override suspend fun insertCorridorNetwork(snapshot: CorridorSnapshot) = withContext(ioDispatcher) {
        daoProvider().insertCorridorNetwork(snapshot.toCorridorNetworkEntity())
    }

    override suspend fun getCorridorNetwork(key: String, version: Int): CorridorSnapshot? = withContext(ioDispatcher) {
        daoProvider().getCorridorNetwork(key, version)?.toCorridorSnapshot()
    }

    override suspend fun deleteCorridorNetwork(key: String) = withContext(ioDispatcher) {
        daoProvider().deleteCorridorNetwork(key)
    }

    override suspend fun deleteAllCorridorNetworks() = withContext(ioDispatcher) {
        daoProvider().deleteAllCorridorNetworks()
    }

    override suspend fun markCorridorNetworksStale(year: Int?) = withContext(ioDispatcher) {
        daoProvider().markCorridorNetworksStale(year)
    }

    override suspend fun getAllCorridorNetworks(): List<CorridorSnapshot> = withContext(ioDispatcher) {
        daoProvider().getAllCorridorNetworks().map { it.toCorridorSnapshot() }
    }
}
