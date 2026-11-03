package id.my.nanclouder.nanhistory.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.CorridorNetworkEntity
import id.my.nanclouder.nanhistory.ui.map.CorridorNetworkBuilder
import id.my.nanclouder.nanhistory.ui.map.MapPoint
import id.my.nanclouder.nanhistory.ui.map.YearlyMapCacheManager
import id.my.nanclouder.nanhistory.ui.map.YearlyMapMergeLogic
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background worker that precomputes and refreshes topologic corridor networks during device idle/charging windows.
 * Saves precomputed networks to disk binary cache and the Room [CorridorNetworkEntity] table.
 */
class CorridorPrecomputeWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(applicationContext)
            val appDao = db.appDao()
            val corridorDao = db.corridorNetworkDao()

            val allEvents = appDao.getEventsInDateRange(null, null)
            if (allEvents.isEmpty()) return@withContext Result.success()

            // Group events by year
            val years = allEvents.map { it.date.year }.distinct()

            for (year in years) {
                if (isStopped) return@withContext Result.retry()

                val rangeKey = "year_$year"
                val existing = corridorDao.getCorridorNetwork(rangeKey, YearlyMapCacheManager.ALGO_VERSION)
                val startDate = "$year-01-01"
                val endDate = "$year-12-31"

                // Check if existing snapshot is stale (updatedAt == 0L) or missing
                if (existing != null && existing.updatedAt > 0L) {
                    continue
                }

                precomputeRange(
                    rangeKey = rangeKey,
                    startDate = startDate,
                    endDate = endDate,
                    appDao = appDao,
                    corridorDao = corridorDao
                )
            }

            // Also check all-time if stale
            val allTimeKey = "all_time"
            val existingAllTime = corridorDao.getCorridorNetwork(allTimeKey, YearlyMapCacheManager.ALGO_VERSION)
            if (existingAllTime == null || existingAllTime.updatedAt == 0L) {
                precomputeRange(
                    rangeKey = allTimeKey,
                    startDate = null,
                    endDate = null,
                    appDao = appDao,
                    corridorDao = corridorDao
                )
            }

            Result.success()
        } catch (e: Throwable) {
            Log.e("CorridorPrecompute", "Failed to precompute corridor networks", e)
            Result.retry()
        }
    }

    private suspend fun precomputeRange(
        rangeKey: String,
        startDate: String?,
        endDate: String?,
        appDao: id.my.nanclouder.nanhistory.db.AppDao,
        corridorDao: id.my.nanclouder.nanhistory.db.CorridorNetworkDao
    ) {
        val bounds = appDao.getLocationBoundsInRange(startDate, endDate) ?: return
        if (bounds.minLat == null || bounds.maxLat == null || bounds.minLon == null || bounds.maxLon == null) {
            return
        }

        val locations = appDao.getRenderedYearlyLocations(
            startDate = startDate,
            endDate = endDate,
            minLat = bounds.minLat,
            maxLat = bounds.maxLat,
            minLon = bounds.minLon,
            maxLon = bounds.maxLon,
            step = 1
        )
        if (locations.size < 2) return

        val tracks = locations.groupBy { it.eventId }.map { (eventId, locs) ->
            Pair(eventId, locs.map { MapPoint(it.latitude, it.longitude, it.timestamp, it.accuracy) })
        }

        val merged = YearlyMapMergeLogic.mergeParallelCorridors(tracks)
        val network = CorridorNetworkBuilder.buildNetwork(merged)

        // Save binary cache file
        YearlyMapCacheManager.saveNetworkBinary(applicationContext, rangeKey, network)

        // Save DB entity
        val baos = ByteArrayOutputStream()
        network.writeTo(baos)
        val entity = CorridorNetworkEntity(
            rangeKey = rangeKey,
            startDate = startDate,
            endDate = endDate,
            version = YearlyMapCacheManager.ALGO_VERSION,
            updatedAt = System.currentTimeMillis(),
            boundsNorth = network.bounds.latNorth,
            boundsSouth = network.bounds.latSouth,
            boundsEast = network.bounds.lonEast,
            boundsWest = network.bounds.lonWest,
            maxCount = network.maxCount,
            totalDistanceMeters = network.totalDistanceMeters,
            networkBlob = baos.toByteArray()
        )
        corridorDao.insertCorridorNetwork(entity)
        Log.d("CorridorPrecompute", "Precomputed and cached corridor network for $rangeKey")
    }

    companion object {
        const val WORK_NAME_PERIODIC: String = "corridor_precompute_periodic"
        const val WORK_NAME_ONETIME: String = "corridor_precompute_onetime"

        /**
         * Schedules periodic background precomputation constrained to charging and idle state.
         */
        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiresDeviceIdle(true)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<CorridorPrecomputeWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /**
         * Enqueues a one-time deferred precomputation job (e.g. after a recording session completes).
         */
        fun scheduleOneTime(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = OneTimeWorkRequestBuilder<CorridorPrecomputeWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_ONETIME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
