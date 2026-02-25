package id.my.nanclouder.nanhistory.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toHistoryEvent
import id.my.nanclouder.nanhistory.ui.map.YearlyMapCacheManager
import id.my.nanclouder.nanhistory.ui.map.YearlyMapPersistentState
import id.my.nanclouder.nanhistory.ui.map.processEventsToState
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
            return Result.success() // Should we count this as success? Yes, work done (nothing to do).
        }
        
        // 2. Determine Year (Current Year for now)
        val year = LocalDate.now().year
        
        Log.d("YearlyMapWorker", "Starting background processing for year $year")
        
        // 3. Load Cache
        val state = YearlyMapCacheManager.loadCache(context, year) ?: YearlyMapPersistentState()
        
        // 4. Check Active Recording
        val isRecording = RecordService.RecordState.isRecording.value
        val activeEventId = RecordService.RecordState.eventId.value
        
        // 5. Fetch New Events
        val db = AppDatabase.getInstance(context)
        val dao = db.appDao()
        
        // We only care about events AFTER the last processed time
        // And strictly completed events (endTime != null is implicit in event structure but we filter in code)
        // Also filtering out active event
        
        val newEvents = dao.getEventsAfter(state.lastEventTime).map { it.toHistoryEvent() }
            .filter { event ->
                // Ensure event belongs to the target year
                event.time.year == year
            }
            .filter { event ->
                // Filter out active recording if it matches
                if (isRecording && activeEventId != null && event.id == activeEventId) {
                    false
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
        
        try {
            processEventsToState(context, newEvents, state, precision) { progress ->
                // distinct progress update if needed
            }
            
            // 7. Save Cache
            YearlyMapCacheManager.saveCache(context, year, state)
            Log.d("YearlyMapWorker", "Background processing complete.")
            
            return Result.success()
            
        } catch (e: Exception) {
            Log.e("YearlyMapWorker", "Error processing events", e)
            return Result.retry()
        }
    }
}
