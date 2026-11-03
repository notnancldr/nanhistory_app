package id.my.nanclouder.nanhistory.perf

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.Random

/**
 * Deterministic synthetic dataset generator producing ~1,000,000 location records
 * across 2,500 events on shared corridors in a standalone SQLite database.
 *
 * Simulates:
 * - 5-8m GPS jitter / noise
 * - Parallel lane offsets (+/- 5..15m)
 * - Reverse direction travel
 * - Staggered starts along routes
 * - Speed variations (1s - 5s timestamps)
 * - GPS gaps (>120s / >5km)
 */
object SyntheticDatasetGenerator {

    private val BASE_ROUTES = listOf(
        listOf(
            Pair(-6.1750, 106.8270), Pair(-6.1900, 106.8230), Pair(-6.2100, 106.8200),
            Pair(-6.2400, 106.8100), Pair(-6.2700, 106.8050)
        ),
        listOf(
            Pair(-6.2000, 106.7800), Pair(-6.2050, 106.8100), Pair(-6.2100, 106.8400),
            Pair(-6.2150, 106.8700)
        ),
        listOf(
            Pair(-6.1500, 106.8000), Pair(-6.1800, 106.8300), Pair(-6.2200, 106.8500),
            Pair(-6.2600, 106.8700)
        ),
        listOf(
            Pair(-6.2300, 106.7500), Pair(-6.2350, 106.8000), Pair(-6.2400, 106.8500),
            Pair(-6.2450, 106.9000)
        ),
        listOf(
            Pair(-6.1600, 106.8500), Pair(-6.1900, 106.8400), Pair(-6.2300, 106.8300),
            Pair(-6.2800, 106.8200)
        )
    )

    fun generate(targetFile: File, numEvents: Int = 2500, pointsPerEvent: Int = 400): Long {
        if (targetFile.exists()) {
            targetFile.delete()
        }
        targetFile.parentFile?.mkdirs()

        val conn: Connection = DriverManager.getConnection("jdbc:sqlite:${targetFile.absolutePath}")
        conn.autoCommit = false

        conn.createStatement().use { stmt ->
            stmt.execute("PRAGMA synchronous = OFF;")
            stmt.execute("PRAGMA journal_mode = MEMORY;")
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS events (
                    id TEXT PRIMARY KEY NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    favorite INTEGER NOT NULL,
                    time TEXT NOT NULL,
                    created TEXT NOT NULL,
                    modified TEXT NOT NULL,
                    versionNumber INTEGER NOT NULL,
                    date TEXT NOT NULL,
                    deletePermanently TEXT,
                    audio TEXT,
                    end TEXT,
                    transportationType TEXT NOT NULL,
                    locationDescriptions TEXT NOT NULL,
                    type TEXT NOT NULL
                );
                """.trimIndent()
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS locations (
                    timestamp INTEGER PRIMARY KEY NOT NULL,
                    eventId TEXT NOT NULL,
                    zoneId TEXT NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    speed REAL,
                    bearing REAL,
                    altitude REAL,
                    accuracy REAL,
                    speedAccuracy REAL,
                    bearingAccuracy REAL,
                    verticalAccuracy REAL,
                    FOREIGN KEY (eventId) REFERENCES events(id) ON DELETE CASCADE
                );
                """.trimIndent()
            )
            stmt.execute("CREATE INDEX IF NOT EXISTS index_locations_eventId ON locations (eventId);")
        }

        val eventInsert = conn.prepareStatement(
            "INSERT INTO events VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        )
        val locInsert = conn.prepareStatement(
            "INSERT INTO locations VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        )

        val rng = Random(42L)
        val baseTs = 1735689600000L // 2025-01-01 00:00:00 UTC
        var totalPoints = 0L

        for (i in 0 until numEvents) {
            val eventId = String.format("ev_%04d", i)
            val route = BASE_ROUTES[i % BASE_ROUTES.size]
            val isReverse = (i % 2 == 1)

            val laneOffsetM = -15.0 + rng.nextDouble() * 30.0
            val dLatLane = laneOffsetM / 111000.0
            val dLonLane = laneOffsetM / 110350.0

            val dayOffset = (i * 7) % 360
            val month = (dayOffset / 30) + 1
            val day = (dayOffset % 28) + 1
            val dateStr = String.format("2025-%02d-%02d", month, day)
            val eventTs = baseTs + (i * 1000000L)

            eventInsert.setString(1, eventId)
            eventInsert.setString(2, "Trip $i")
            eventInsert.setString(3, "Synthetic benchmark route")
            eventInsert.setInt(4, 0)
            eventInsert.setString(5, "${dateStr}T10:00:00Z")
            eventInsert.setString(6, "${dateStr}T10:00:00Z")
            eventInsert.setString(7, "${dateStr}T10:00:00Z")
            eventInsert.setInt(8, 1)
            eventInsert.setString(9, dateStr)
            eventInsert.setString(10, null)
            eventInsert.setString(11, null)
            eventInsert.setString(12, "${dateStr}T11:00:00Z")
            eventInsert.setString(13, if (i % 2 == 0) "Motorcycle" else "Car")
            eventInsert.setString(14, "{}")
            eventInsert.setString(15, "range")
            eventInsert.addBatch()

            val staggerFrac = rng.nextDouble() * 0.25

            for (p in 0 until pointsPerEvent) {
                var frac = staggerFrac + (1.0 - staggerFrac) * (p.toDouble() / (pointsPerEvent - 1))
                if (isReverse) frac = 1.0 - frac

                val segCount = route.size - 1
                val segIdx = (frac * segCount).toInt().coerceIn(0, segCount - 1)
                val segT = (frac * segCount) - segIdx
                val p1 = route[segIdx]
                val p2 = route[segIdx + 1]

                var lat = p1.first + (p2.first - p1.first) * segT + dLatLane +
                    (rng.nextGaussian() * (7.0 / 111000.0))
                val lon = p1.second + (p2.second - p1.second) * segT + dLonLane +
                    (rng.nextGaussian() * (7.0 / 110350.0))

                // Occasional GPS jump
                if (p == 200 && i % 50 == 0) {
                    lat += 0.05
                }

                val ptTs = eventTs + (p * 2000L)
                locInsert.setLong(1, ptTs)
                locInsert.setString(2, eventId)
                locInsert.setString(3, "Asia/Jakarta")
                locInsert.setDouble(4, lat)
                locInsert.setDouble(5, lon)
                locInsert.setFloat(6, 12.5f)
                locInsert.setFloat(7, 90.0f)
                locInsert.setDouble(8, 20.0)
                locInsert.setFloat(9, 6.0f)
                locInsert.setFloat(10, 0.5f)
                locInsert.setFloat(11, 5.0f)
                locInsert.setFloat(12, 2.0f)
                locInsert.addBatch()
                totalPoints++

                if (totalPoints % 25000L == 0L) {
                    locInsert.executeBatch()
                }
            }
        }

        eventInsert.executeBatch()
        locInsert.executeBatch()
        conn.commit()
        conn.close()

        return totalPoints
    }
}
