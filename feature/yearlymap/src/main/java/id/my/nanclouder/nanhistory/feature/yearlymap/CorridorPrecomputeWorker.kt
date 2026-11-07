package id.my.nanclouder.nanhistory.feature.yearlymap

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
import id.my.nanclouder.nanhistory.core.common.di.CoreContainerProvider
import id.my.nanclouder.nanhistory.domain.model.CorridorSnapshot
import id.my.nanclouder.nanhistory.domain.repository.CorridorRepository
import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import id.my.nanclouder.nanhistory.domain.repository.LocationRepository
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background worker that precomputes and refreshes topologic corridor networks during device idle/charging windows.
 * Saves precomputed networks to disk binary cache and the Room [CorridorNetworkEntity] table via [CorridorRepository].
 */
class CorridorPrecomputeWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val container = (applicationContext as CoreContainerProvider).coreContainer
            val eventRepository = container.eventRepository
            val corridorRepository = container.corridorRepository ?: return@withContext Result.success()
            val locationRepository = container.locationRepository

            val allEvents = eventRepository.getEventsInDateRange(null, null)
            if (allEvents.isEmpty()) return@withContext Result.success()

            // Group events by year
            val years = allEvents.map { it.time.year }.distinct()

            for (year in years) {
                if (isStopped) return@withContext Result.retry()

                val rangeKey = "year_$year"
                val existing = corridorRepository.getCorridorNetwork(rangeKey, YearlyMapCacheManager.ALGO_VERSION)
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
                    locationRepository = locationRepository,
                    corridorRepository = corridorRepository
                )
            }

            // Also check all-time if stale
            val allTimeKey = "all_time"
            val existingAllTime = corridorRepository.getCorridorNetwork(allTimeKey, YearlyMapCacheManager.ALGO_VERSION)
            if (existingAllTime == null || existingAllTime.updatedAt == 0L) {
                precomputeRange(
                    rangeKey = allTimeKey,
                    startDate = null,
                    endDate = null,
                    locationRepository = locationRepository,
                    corridorRepository = corridorRepository
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
        locationRepository: LocationRepository,
        corridorRepository: CorridorRepository
    ) {
        val bounds = locationRepository.getLocationBoundsInRange(startDate, endDate) ?: return
        val minLat = bounds.minLat ?: return
        val maxLat = bounds.maxLat ?: return
        val minLon = bounds.minLon ?: return
        val maxLon = bounds.maxLon ?: return

        val locations = locationRepository.getRenderedYearlyLocations(
            startDate = startDate,
            endDate = endDate,
            minLat = minLat,
            maxLat = maxLat,
            minLon = minLon,
            maxLon = maxLon,
            step = 1
        )
        if (locations.size < 2) return

        val tracks = locations.groupBy { it.eventId ?: "" }.map { (eventId, locs) ->
            Pair(eventId, locs.map { MapPoint(it.location.latitude, it.location.longitude, it.time.toInstant().toEpochMilli(), it.accuracy) })
        }

        val merged = YearlyMapMergeLogic.mergeParallelCorridors(tracks)
        val network = CorridorNetworkBuilder.buildNetwork(merged)

        // Save binary cache file
        YearlyMapCacheManager.saveNetworkBinary(applicationContext, rangeKey, network)

        // Save snapshot via repository
        val baos = ByteArrayOutputStream()
        network.writeTo(baos)
        val snapshot = CorridorSnapshot(
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
        corridorRepository.insertCorridorNetwork(snapshot)
        Log.d("CorridorPrecompute", "Precomputed and cached corridor network for $rangeKey")
    }

    companion object {
        const val WORK_NAME_PERIODIC: String = "corridor_precompute_periodic"
        const val WORK_NAME_ONETIME: String = "corridor_precompute_onetime"

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
