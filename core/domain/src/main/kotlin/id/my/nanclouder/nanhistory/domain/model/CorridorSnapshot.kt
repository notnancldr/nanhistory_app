package id.my.nanclouder.nanhistory.domain.model

/**
 * Pure domain representation of a cached topologic corridor network snapshot.
 */
data class CorridorSnapshot(
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
    val networkBlob: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CorridorSnapshot) return false
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
        result = 31 * result + boundsNorth.hashCode()
        result = 31 * result + boundsSouth.hashCode()
        result = 31 * result + boundsEast.hashCode()
        result = 31 * result + boundsWest.hashCode()
        result = 31 * result + maxCount
        result = 31 * result + totalDistanceMeters.hashCode()
        result = 31 * result + networkBlob.contentHashCode()
        return result
    }
}
