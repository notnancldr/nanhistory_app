package id.my.nanclouder.nanhistory

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import id.my.nanclouder.nanhistory.core.common.dataprocess.DataProcessBridge
import id.my.nanclouder.nanhistory.core.common.dataprocess.DataProcessConstants
import id.my.nanclouder.nanhistory.core.common.dataprocess.DataProcessController
import id.my.nanclouder.nanhistory.core.common.di.CoreContainer
import id.my.nanclouder.nanhistory.core.common.di.CoreContainerProvider
import id.my.nanclouder.nanhistory.core.common.navigation.IntentRegistry
import id.my.nanclouder.nanhistory.core.common.security.EventLocationDigest
import id.my.nanclouder.nanhistory.core.common.security.EventSignatureVerifier
import id.my.nanclouder.nanhistory.core.common.security.EventSignatures
import id.my.nanclouder.nanhistory.di.AppContainer
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.navigation.AppIntentProvider
import id.my.nanclouder.nanhistory.service.DataProcessService
import id.my.nanclouder.nanhistory.utils.signature.generateSignature
import id.my.nanclouder.nanhistory.utils.signature.generateSignatureV2
import id.my.nanclouder.nanhistory.utils.signature.validateSignature

class NanHistoryApplication : Application(), CoreContainerProvider {
    lateinit var container: AppContainer
        private set

    override val coreContainer: CoreContainer
        get() = container

    override fun onCreate() {
        super.onCreate()
        IntentRegistry.register(AppIntentProvider())
        id.my.nanclouder.nanhistory.core.common.record.RecordBridge.register(object : id.my.nanclouder.nanhistory.core.common.record.RecordController {
            override fun startRecording(context: Context) {
                val intent = Intent(context, id.my.nanclouder.nanhistory.feature.record.RecordService::class.java).apply {
                    action = id.my.nanclouder.nanhistory.feature.record.RecordService.ACTION_RECORD_START
                }
                context.startForegroundService(intent)
            }

            override fun stopRecording(context: Context) {
                val intent = Intent(context, id.my.nanclouder.nanhistory.feature.record.RecordService::class.java).apply {
                    action = id.my.nanclouder.nanhistory.feature.record.RecordService.ACTION_RECORD_STOP
                }
                context.startForegroundService(intent)
            }

            override fun toggleRecording(context: Context) {
                val intent = Intent(context, id.my.nanclouder.nanhistory.feature.record.RecordService::class.java).apply {
                    action = id.my.nanclouder.nanhistory.feature.record.RecordService.ACTION_RECORD_TOGGLE
                }
                context.startForegroundService(intent)
            }

            override fun restartRecording(context: Context) {
                val intent = Intent(context, id.my.nanclouder.nanhistory.feature.record.RecordService::class.java).apply {
                    action = id.my.nanclouder.nanhistory.feature.record.RecordService.ACTION_SERVICE_RESTART
                }
                context.startService(intent)
            }
        })
        id.my.nanclouder.nanhistory.core.common.worker.WorkSchedulers.register(object : id.my.nanclouder.nanhistory.core.common.worker.WorkScheduler {
            override fun schedulePeriodicWork(context: Context) {
                val request = androidx.work.PeriodicWorkRequestBuilder<id.my.nanclouder.nanhistory.worker.AutoDeleteWorker>(1, java.util.concurrent.TimeUnit.DAYS)
                    .build()
                androidx.work.WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(
                        "AutoDeleteWorker",
                        androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                        request
                    )
                val yearlyMapRequest = androidx.work.OneTimeWorkRequestBuilder<id.my.nanclouder.nanhistory.service.YearlyMapWorker>()
                    .build()
                androidx.work.WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        "YearlyMapWorker",
                        androidx.work.ExistingWorkPolicy.KEEP,
                        yearlyMapRequest
                    )
            }
        })
        id.my.nanclouder.nanhistory.core.common.record.ActivityRecognitionBridge.registerManager(object : id.my.nanclouder.nanhistory.core.common.record.ActivityRecognitionManager {
            override fun register(context: Context) {
                id.my.nanclouder.nanhistory.receiver.ActivityRecognitionEngine.register(context)
            }
            override fun unregister(context: Context) {
                id.my.nanclouder.nanhistory.receiver.ActivityRecognitionEngine.unregister(context)
            }
        })
        id.my.nanclouder.nanhistory.core.common.BuildInfo.buildTime = BuildConfig.BUILD_TIME
        DataProcessBridge.register(object : DataProcessController {
            override fun startMigration(context: Context) {
                val intent = Intent(context, DataProcessService::class.java).apply {
                    putExtra(DataProcessConstants.OPERATION_TYPE_EXTRA, DataProcessConstants.OPERATION_MIGRATE)
                }
                context.startService(intent)
            }

            override fun startBackup(context: Context, fileUri: Uri) {
                val intent = Intent(context, DataProcessService::class.java).apply {
                    putExtra(DataProcessConstants.FILE_URI_EXTRA, fileUri.toString())
                    putExtra(DataProcessConstants.OPERATION_TYPE_EXTRA, DataProcessConstants.OPERATION_BACKUP)
                }
                context.startService(intent)
            }

            override fun startImport(context: Context, fileUri: Uri, legacyImport: Boolean) {
                val intent = Intent(context, DataProcessService::class.java).apply {
                    putExtra(DataProcessConstants.FILE_URI_EXTRA, fileUri.toString())
                    putExtra(DataProcessConstants.OPERATION_TYPE_EXTRA, DataProcessConstants.OPERATION_IMPORT)
                    putExtra(DataProcessConstants.LEGACY_IMPORT_EXTRA, legacyImport)
                }
                context.startService(intent)
            }

            override fun cancelService(context: Context) {
                val intent = Intent(context, DataProcessService::class.java).apply {
                    action = DataProcessConstants.ACTION_CANCEL_SERVICE
                }
                context.startService(intent)
            }
        })
        EventSignatures.register(object : EventSignatureVerifier {
            override suspend fun generateSignature(context: Context, event: HistoryEvent, apply: Boolean): String =
                event.generateSignature(context, apply)

            override suspend fun validateSignature(context: Context, event: HistoryEvent): Boolean =
                event.validateSignature(context)

            override suspend fun generateSignatureV2Incremental(
                context: Context,
                event: HistoryEvent,
                locationDigest: EventLocationDigest?
            ): String {
                return (event.generateSignatureV2(
                    context = context,
                    locationDigest = locationDigest
                )).joinToString("") { "%02x".format(it) }
            }
        })
        container = AppContainer(this)
        id.my.nanclouder.nanhistory.core.common.transport.TransportTrainers.register(object : id.my.nanclouder.nanhistory.core.common.transport.TransportTrainer {
            override suspend fun train(
                context: Context,
                existing: Map<id.my.nanclouder.nanhistory.domain.logic.transport.TransportMode, id.my.nanclouder.nanhistory.domain.logic.transport.CalibrationModel>
            ): id.my.nanclouder.nanhistory.domain.logic.transport.TrainingResult {
                val res = id.my.nanclouder.nanhistory.feature.settings.transportModel.train(context, existing)
                return id.my.nanclouder.nanhistory.domain.logic.transport.TrainingResult(
                    accuracy = res.accuracy,
                    totalSamples = res.totalSamples,
                    correctSamples = res.correctSamples,
                    models = res.models
                )
            }
        })
    }
}

