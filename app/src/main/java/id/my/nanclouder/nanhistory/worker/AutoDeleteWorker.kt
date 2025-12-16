package id.my.nanclouder.nanhistory.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toHistoryEvent
import id.my.nanclouder.nanhistory.utils.history.safeDelete
import java.time.Instant

class AutoDeleteWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    private val notificationChannelId = "AutoDelete"
    private val notificationChannelName = "Auto Delete"

    override suspend fun doWork(): Result {
        val notificationManager = getSystemService(applicationContext, NotificationManager::class.java)
        val notificationChannel = NotificationChannel(
            notificationChannelId,
            notificationChannelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )

        notificationManager?.createNotificationChannel(notificationChannel)

        Log.d("AutoDeleteWorker", "Running auto-delete...")

        val db = AppDatabase.getInstance(applicationContext)
        val dao = db.appDao()

        val deleted = dao.getDeletedEvents()
        var deletedEvents = 0

        val notifyUser = Config.autoDeleteNotifyUser.get(applicationContext)

        for (event in deleted) {
            if ((event.event.deletePermanently ?: Long.MAX_VALUE) <= Instant.now().toEpochMilli()) {
                event.toHistoryEvent().safeDelete(applicationContext)
                deletedEvents++
            }
        }

        if (deletedEvents > 0 && notifyUser) {
            val notification = NotificationCompat.Builder(applicationContext, notificationChannelId)
                .setContentTitle("Events deleted permanently")
                .setContentText("$deletedEvents events deleted permanently after more than 30 days deleted temporarily")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager?.notify(4, notification)
        }

        // Return Result.success(), Result.failure(), or Result.retry()
        return Result.success()
    }
}