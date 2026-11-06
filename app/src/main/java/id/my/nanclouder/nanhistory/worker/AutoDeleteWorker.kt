package id.my.nanclouder.nanhistory.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import id.my.nanclouder.nanhistory.NanHistoryApplication
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.domain.usecase.PurgeExpiredTrashUseCase

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

        Log.d("AutoDeleteWorker", "Running auto-delete via PurgeExpiredTrashUseCase...")

        val container = (applicationContext as NanHistoryApplication).container
        val purgeUseCase = PurgeExpiredTrashUseCase(
            eventRepository = container.eventRepository,
            attachmentStorageRepository = container.attachmentStorageRepository
        )

        val purgedEvents = purgeUseCase()
        val deletedCount = purgedEvents.size

        val notifyUser = Config.autoDeleteNotifyUser.get(applicationContext)

        if (deletedCount > 0 && notifyUser) {
            val notification = NotificationCompat.Builder(applicationContext, notificationChannelId)
                .setContentTitle("Events deleted permanently")
                .setContentText("$deletedCount events deleted permanently after more than 30 days deleted temporarily")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager?.notify(4, notification)
        }

        return Result.success()
    }
}
