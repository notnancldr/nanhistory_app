package id.my.nanclouder.nanhistory.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted topologic corridor network snapshot for a specific time range.
 * Stores binary serialized [id.my.nanclouder.nanhistory.ui.map.CorridorNetwork] data in [networkBlob].
 * Added in DB version 5.
 */
@Entity(tableName = "corridor_networks")
data class CorridorNetworkEntity(
    @PrimaryKey
    val rangeKey: String,
    val startDate: String? = null,
    val endDate: String? = null,
    val version: Int,
    val updatedAt: Long,
    val boundsNorth: Double,
    val boundsSouth: Double,
    val boundsEast: Double,
    val boundsWest: Double,
    val maxCount: Int,
    val totalDistanceMeters: Double,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val networkBlob: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CorridorNetworkEntity) return false
        return rangeKey == other.rangeKey &&
            startDate == other.startDate &&
            endDate == other.endDate &&
            version == other.version &&
            updatedAt == other.updatedAt &&
            boundsNorth == other.boundsNorth &&
            boundsSouth == other.boundsSouth &&
            boundsEast == other.boundsEast &&
            boundsWest == other.boundsWest &&
            maxCount == other.maxCount &&
            totalDistanceMeters == other.totalDistanceMeters &&
            networkBlob.contentEquals(other.networkBlob)
    }

    override fun hashCode(): Int {
        var result = rangeKey.hashCode()
        result = 31 * result + (startDate?.hashCode() ?: 0)
        result = 31 * result + (endDate?.hashCode() ?: 0)
        result = 31 * result + version
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + networkBlob.contentHashCode()
        return result
    }
}
