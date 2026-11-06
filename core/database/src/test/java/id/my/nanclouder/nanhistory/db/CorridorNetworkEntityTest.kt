package id.my.nanclouder.nanhistory.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CorridorNetworkEntityTest {

    @Test
    fun entityEquality_matchesIdenticalData() {
        val blob = byteArrayOf(1, 2, 3, 4, 5)
        val e1 = CorridorNetworkEntity(
            rangeKey = "year_2025",
            startDate = "2025-01-01",
            endDate = "2025-12-31",
            version = 2,
            updatedAt = 1000L,
            boundsNorth = 10.0,
            boundsSouth = -10.0,
            boundsEast = 110.0,
            boundsWest = 100.0,
            maxCount = 15,
            totalDistanceMeters = 50000.0,
            networkBlob = blob
        )

        val e2 = CorridorNetworkEntity(
            rangeKey = "year_2025",
            startDate = "2025-01-01",
            endDate = "2025-12-31",
            version = 2,
            updatedAt = 1000L,
            boundsNorth = 10.0,
            boundsSouth = -10.0,
            boundsEast = 110.0,
            boundsWest = 100.0,
            maxCount = 15,
            totalDistanceMeters = 50000.0,
            networkBlob = byteArrayOf(1, 2, 3, 4, 5)
        )

        assertEquals(e1, e2)
        assertEquals(e1.hashCode(), e2.hashCode())
    }

    @Test
    fun entityEquality_detectsDifferences() {
        val e1 = CorridorNetworkEntity(
            rangeKey = "year_2025",
            version = 2,
            updatedAt = 1000L,
            boundsNorth = 10.0,
            boundsSouth = -10.0,
            boundsEast = 110.0,
            boundsWest = 100.0,
            maxCount = 15,
            totalDistanceMeters = 50000.0,
            networkBlob = byteArrayOf(1, 2, 3)
        )

        val e2 = e1.copy(rangeKey = "year_2024")
        assertNotEquals(e1, e2)

        val e3 = e1.copy(networkBlob = byteArrayOf(1, 2, 4))
        assertNotEquals(e1, e3)
    }
}
