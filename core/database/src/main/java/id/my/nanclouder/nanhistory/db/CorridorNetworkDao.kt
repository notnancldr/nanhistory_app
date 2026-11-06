package id.my.nanclouder.nanhistory.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object for persisting and managing precomputed [CorridorNetworkEntity] snapshots.
 * Added in DB version 5.
 */
@Dao
interface CorridorNetworkDao {

    /**
     * Inserts or replaces a precomputed corridor network snapshot.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorridorNetwork(network: CorridorNetworkEntity)

    /**
     * Retrieves a corridor network snapshot matching [key] and [version], or null if absent.
     */
    @Query("SELECT * FROM corridor_networks WHERE rangeKey = :key AND version = :version LIMIT 1")
    suspend fun getCorridorNetwork(key: String, version: Int): CorridorNetworkEntity?

    /**
     * Deletes the corridor network cached under [key].
     */
    @Query("DELETE FROM corridor_networks WHERE rangeKey = :key")
    suspend fun deleteCorridorNetwork(key: String)

    /**
     * Deletes all cached corridor network records.
     */
    @Query("DELETE FROM corridor_networks")
    suspend fun deleteAllCorridorNetworks()

    /**
     * Marks corridor networks for the given [year] and all-time ranges as stale by resetting [updatedAt].
     */
    @Query(
        "UPDATE corridor_networks SET updatedAt = 0 " +
        "WHERE (startDate IS NULL AND endDate IS NULL) " +
        "   OR (:year IS NOT NULL AND (startDate LIKE :yearPrefix OR endDate LIKE :yearPrefix))"
    )
    suspend fun markCorridorNetworksStale(year: Int?, yearPrefix: String = "${year ?: ""}%")

    /**
     * Retrieves all corridor network entities.
     */
    @Query("SELECT * FROM corridor_networks")
    suspend fun getAllCorridorNetworks(): List<CorridorNetworkEntity>
}
