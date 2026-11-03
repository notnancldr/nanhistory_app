package id.my.nanclouder.nanhistory.receiver

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity

/**
 * Manages registration and unregistration of Google Play Services Activity Recognition transitions.
 */
object ActivityRecognitionEngine {
    private const val TAG = "ActivityRecEngine"
    private const val REQUEST_CODE = 3001

    /**
     * Builds a [PendingIntent] directed at [ActivityTransitionReceiver].
     */
    fun createPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ActivityTransitionReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    /**
     * Builds the list of [ActivityTransition] for vehicle, bicycle, running, walking, and still transitions.
     */
    fun buildTransitionList(): List<ActivityTransition> {
        val transitions = mutableListOf<ActivityTransition>()
        val startActivities = listOf(
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.RUNNING,
            DetectedActivity.WALKING,
        )
        for (activity in startActivities) {
            transitions.add(
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )
        }
        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        )
        transitions.add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )
        return transitions
    }

    /**
     * Builds the [ActivityTransitionRequest] for vehicle, bicycle, running, walking, and still transitions.
     */
    fun buildTransitionRequest(): ActivityTransitionRequest {
        return ActivityTransitionRequest(buildTransitionList())
    }

    /**
     * Registers activity transitions with Play Services.
     */
    fun register(context: Context) {
        val client = ActivityRecognition.getClient(context)
        val request = buildTransitionRequest()
        val pendingIntent = createPendingIntent(context)
        try {
            client.requestActivityTransitionUpdates(request, pendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Activity transitions registered successfully")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to register activity transitions: ${e.message}")
                }
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied for activity transitions: ${e.message}")
        }
    }

    /**
     * Removes activity transition updates.
     */
    fun unregister(context: Context) {
        val client = ActivityRecognition.getClient(context)
        val pendingIntent = createPendingIntent(context)
        try {
            client.removeActivityTransitionUpdates(pendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Activity transitions unregistered")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to unregister activity transitions: ${e.message}")
                }
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission revoked while unregistering: ${e.message}")
        }
    }
}
