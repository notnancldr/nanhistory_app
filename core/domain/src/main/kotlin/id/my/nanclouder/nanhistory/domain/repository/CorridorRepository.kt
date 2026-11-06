package id.my.nanclouder.nanhistory.domain.repository

import id.my.nanclouder.nanhistory.domain.model.CorridorSnapshot

/**
 * Domain port for storing and retrieving precomputed corridor network graphs.
 */
interface CorridorRepository {
    suspend fun insertCorridorNetwork(snapshot: CorridorSnapshot) {}
    suspend fun getCorridorNetwork(key: String, version: Int): CorridorSnapshot? = null
    suspend fun deleteCorridorNetwork(key: String) {}
    suspend fun deleteAllCorridorNetworks() {}
    suspend fun markCorridorNetworksStale(year: Int?) {}
    suspend fun getAllCorridorNetworks(): List<CorridorSnapshot> = emptyList()
}
