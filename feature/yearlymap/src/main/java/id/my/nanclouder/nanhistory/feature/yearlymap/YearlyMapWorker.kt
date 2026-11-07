package id.my.nanclouder.nanhistory.feature.yearlymap

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.core.common.di.CoreContainerProvider
import id.my.nanclouder.nanhistory.core.common.state.RecordingState
import java.time.LocalDate

class YearlyMapWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        
        // 1. Check Config
        if (!Config.yearlyMapBackgroundProcessing.get(context)) {
            Log.d("YearlyMapWorker", "Background processing disabled in config.")
            return Result.success()
        }
        
        // 2. Determine Year (Current Year for now)
        val year = LocalDate.now().year
        
        Log.d("YearlyMapWorker", "Starting background processing for year $year")
        
        // 3. Load Cache
        val state = YearlyMapCacheManager.loadCache(context, year) ?: YearlyMapPersistentState()
        
        // 4. Check Active Recording
        val isRecording = RecordingState.isRecording.value
        val activeEventId = RecordingState.activeEventId.value
        
        // 5. Fetch New Events via EventRepository
        val container = (context as CoreContainerProvider).coreContainer
        val eventRepository = container.eventRepository
        
        val newEvents = eventRepository.getEventsAfter(state.lastEventTime)
            .filter { event ->
                // Ensure event belongs to the target year
                event.time.year == year
            }
            .filter { event ->
                // Filter out active recording if it matches
                if (isRecording && activeEventId != null) {
                    event.id != activeEventId
                } else {
                    true
                }
            }
            
        if (newEvents.isEmpty()) {
            Log.d("YearlyMapWorker", "No new events to process.")
            return Result.success()
        }
        
        Log.d("YearlyMapWorker", "Processing ${newEvents.size} new events...")
        
        // 6. Process Events
        val precision = Config.yearlyMapPrecision.get(context)

        return try {
            processEventsToState(
                context = context,
                newEvents = newEvents,
                state = state,
                precisionMeters = precision
            )
            
            // 7. Save Cache
            YearlyMapCacheManager.saveCache(context, year, state)
            Log.d("YearlyMapWorker", "Background processing complete.")
            Result.success()
        } catch (e: Exception) {
            Log.e("YearlyMapWorker", "Error processing events", e)
            Result.retry()
        }
    }
}
