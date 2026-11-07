package id.my.nanclouder.nanhistory.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.feature.record.RecordService

/**
 * Receives activity transitions and dispatches start, stop, or still grace commands to [RecordService].
 */
class ActivityTransitionReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ActivityTransReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!Config.autoRecordActivityRecognitionEnabled.get(context)) {
            Log.d(TAG, "Activity recognition auto-record is disabled in config")
            return
        }

        if (!ActivityTransitionResult.hasResult(intent)) {
            return
        }

        val result = ActivityTransitionResult.extractResult(intent) ?: return

        for (event in result.transitionEvents) {
            Log.d(TAG, "Transition event: activity=${event.activityType}, transition=${event.transitionType}")
            when (event.activityType) {
                DetectedActivity.IN_VEHICLE,
                DetectedActivity.ON_BICYCLE,
                DetectedActivity.RUNNING,
                DetectedActivity.WALKING -> {
                    if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                        dispatchStart(context, event.activityType)
                    }
                }
                DetectedActivity.STILL -> {
                    if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                        dispatchStillEnter(context)
                    } else if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                        dispatchStillExit(context)
                    }
                }
            }
        }
    }

    private fun dispatchStart(context: Context, activityType: Int) {
        val serviceIntent = Intent(context, RecordService::class.java).apply {
            action = RecordService.ACTION_RECORD_START
            putExtra("trigger_source", "activity_recognition_$activityType")
        }
        startServiceSafe(context, serviceIntent)
    }

    private fun dispatchStillEnter(context: Context) {
        val serviceIntent = Intent(context, RecordService::class.java).apply {
            action = RecordService.ACTION_STILL_ENTER
        }
        startServiceSafe(context, serviceIntent)
    }

    private fun dispatchStillExit(context: Context) {
        val serviceIntent = Intent(context, RecordService::class.java).apply {
            action = RecordService.ACTION_STILL_EXIT
        }
        startServiceSafe(context, serviceIntent)
    }

    private fun startServiceSafe(context: Context, intent: Intent) {
        try {
            if (RecordService.RecordState.isRunning.value) {
                context.startService(intent)
            } else {
                context.startForegroundService(intent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to dispatch intent to RecordService: ${e.message}")
        }
    }
}
