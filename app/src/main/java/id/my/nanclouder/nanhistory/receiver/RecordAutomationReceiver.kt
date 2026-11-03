package id.my.nanclouder.nanhistory.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.service.RecordService

/**
 * Handles external automation broadcasts (e.g., Tasker, Automate, Quick Tap) to start, stop, or toggle recording.
 */
class RecordAutomationReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "RecordAutomationReceiver"
        const val ACTION_START_RECORDING = "id.my.nanclouder.nanhistory.action.START_RECORDING"
        const val ACTION_STOP_RECORDING = "id.my.nanclouder.nanhistory.action.STOP_RECORDING"
        const val ACTION_TOGGLE_RECORDING = "id.my.nanclouder.nanhistory.action.TOGGLE_RECORDING"
        const val EXTRA_PASSKEY = "extra_passkey"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!Config.externalAutomationEnabled.get(context)) {
            Log.d(TAG, "External automation is disabled in settings")
            return
        }

        val requiredPasskey = Config.externalAutomationPasskey.get(context)
        if (requiredPasskey.isNotBlank()) {
            val passkey = intent.getStringExtra(EXTRA_PASSKEY)
            if (passkey != requiredPasskey) {
                Log.w(TAG, "Rejected automation broadcast: invalid or missing passkey")
                return
            }
        }

        val serviceIntent = Intent(context, RecordService::class.java)

        when (intent.action) {
            ACTION_START_RECORDING -> {
                serviceIntent.action = RecordService.ACTION_RECORD_START
                serviceIntent.putExtra("trigger_source", "external_automation")
            }
            ACTION_STOP_RECORDING -> {
                serviceIntent.action = RecordService.ACTION_RECORD_STOP
                serviceIntent.putExtra("trigger_source", "external_automation")
            }
            ACTION_TOGGLE_RECORDING -> {
                serviceIntent.action = RecordService.ACTION_RECORD_TOGGLE
                serviceIntent.putExtra("trigger_source", "external_automation")
            }
            else -> return
        }

        try {
            if (RecordService.RecordState.isRunning.value) {
                context.startService(serviceIntent)
            } else {
                context.startForegroundService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start RecordService from automation receiver: ${e.message}")
        }
    }
}
