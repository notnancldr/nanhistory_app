package id.my.nanclouder.nanhistory.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import id.my.nanclouder.nanhistory.BackupProgressStage
import id.my.nanclouder.nanhistory.ImportProgressStage
import id.my.nanclouder.nanhistory.R
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.lib.LogData
import id.my.nanclouder.nanhistory.lib.ServiceBroadcast
import id.my.nanclouder.nanhistory.lib.history.migrateData
import id.my.nanclouder.nanhistory.lib.readableSize
import id.my.nanclouder.nanhistory.ENCRYPTION_KEY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class DataProcessService : Service() {
    companion object {
        private const val DATA_PROCESS_CHANNEL_ID = "backup_and_data_channel"
        private const val MIGRATION_CODE_FILEPATH = "migration_code"

        const val ACTION_CANCEL_SERVICE = "ACTION_CANCEL_SERVICE"

        const val OPERATION_UNKNOWN = -1
        const val OPERATION_BACKUP = 0
        const val OPERATION_IMPORT = 1
        const val OPERATION_MIGRATE = 2

        const val OPERATION_TYPE_EXTRA = "operationType"
        const val FILE_URI_EXTRA = "fileUri"

        private const val CURRENT_MIGRATION_CODE = 1

        fun checkMigration(context: Context): Boolean {
            val migrationCodeFile = File(context.filesDir, MIGRATION_CODE_FILEPATH)
            if (migrationCodeFile.exists()) migrationCodeFile.readText().let {
                return it.toInt() == CURRENT_MIGRATION_CODE
            }
            else return false
        }

        fun updateMigrationCode(context: Context) {
            val migrationCodeFile = File(context.filesDir, MIGRATION_CODE_FILEPATH)
            migrationCodeFile.writeText(CURRENT_MIGRATION_CODE.toString())
        }

        fun resetMigrationCode(context: Context) {
            val migrationCodeFile = File(context.filesDir, MIGRATION_CODE_FILEPATH)
            migrationCodeFile.delete()
        }
    }

    object ServiceState {
        val progress = MutableStateFlow(0L)
        val progressMax = MutableStateFlow(1L)
        val errorMessage = MutableStateFlow<String?>(null)
        val isRunning = MutableStateFlow(false)
        val operationType = MutableStateFlow(-1)
    }

    object BackupState {
        val stage = MutableStateFlow<BackupProgressStage?>(null)
    }

    object ImportState {
        val stage = MutableStateFlow<ImportProgressStage?>(null)
        val migrationName = MutableStateFlow<String?>(null)
    }

    inner class MyBinder : Binder() {
        fun getService(): DataProcessService = this@DataProcessService
    }

    private val binder = MyBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var operationType = OPERATION_UNKNOWN

    private var lastNotificationUpdate = Instant.MIN

    private lateinit var notificationManager: NotificationManager

    private val outputStreams = mutableMapOf<String, OutputStream?>()
    private val inputStreams = mutableMapOf<String, InputStream?>()
    private val tempFiles = mutableMapOf<String, File>()

    private var serviceJob: Job? = null

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // When receive action
        when (intent?.action) {
            ACTION_CANCEL_SERVICE -> {
                if (operationType == OPERATION_BACKUP)
                    updateProgress(backupStage = BackupProgressStage.Cancelled)
                else
                    updateProgress(importStage = ImportProgressStage.Cancelled)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        // Create notification and promote to foreground
        val notification = getNotification()

        val fileUri = intent?.getStringExtra(FILE_URI_EXTRA)?.toUri()
        operationType = intent?.getIntExtra(OPERATION_TYPE_EXTRA, -1) ?: -1

        if (operationType == OPERATION_MIGRATE) {
            ServiceState.operationType.value = operationType
            ServiceState.progressMax.value = 1
            ServiceState.progress.value = 0
            ServiceState.errorMessage.value = null

            updateProgress(importStage = ImportProgressStage.Migrate)
            processMigration()

            startForeground(2, getNotification())
            ServiceState.isRunning.value = true
        }
        else if (fileUri != null && operationType != OPERATION_UNKNOWN) {
            ServiceState.operationType.value = operationType
            ServiceState.progressMax.value = 1
            ServiceState.progress.value = 0
            ServiceState.errorMessage.value = null

            if (operationType == OPERATION_BACKUP) {
                updateProgress(backupStage = BackupProgressStage.Init)
                processBackup(fileUri)
            }
            else if (operationType == OPERATION_IMPORT) {
                updateProgress(importStage = ImportProgressStage.Init)
                processImport(fileUri)
            }
            startForeground(2, notification)
            ServiceState.isRunning.value = true
        }
        else {
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        val name = "Backup and Data Update"
        val descriptionText = "Notification channel for backup, import data, and data update"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(DATA_PROCESS_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun getNotification(ongoing: Boolean = true): Notification {
        return NotificationCompat.Builder(this, DATA_PROCESS_CHANNEL_ID)
            .apply {
                val adder = if (operationType == OPERATION_BACKUP) {
                    when (BackupState.stage.value) {
                        BackupProgressStage.Init -> 0
                        BackupProgressStage.Compress -> 0
                        BackupProgressStage.Encrypt -> 50
                        BackupProgressStage.Done -> 50
                        BackupProgressStage.Error -> 0
                        else -> 0
                    }
                }
                else {
                    when (ImportState.stage.value) {
                        ImportProgressStage.Init -> 0
                        ImportProgressStage.Decrypt -> 0
                        ImportProgressStage.Extract -> 50
                        ImportProgressStage.Migrate -> 50
                        ImportProgressStage.Done -> 50
                        ImportProgressStage.Error -> 0
                        else -> 0
                    }
                }
                val percentage = (50 * ServiceState.progress.value / ServiceState.progressMax.value + adder).toInt()
                // Log.d("NanHistoryDebug", "Percentage: $percentage%, $adder")
                setContentTitle(
                    if (operationType == OPERATION_BACKUP) {
                        if (BackupState.stage.value != BackupProgressStage.Error) "Backup Data ($percentage%)"
                        else "Backup Failed"
                    }
                    else if (operationType == OPERATION_IMPORT) {
                        if (
                            ImportState.stage.value != ImportProgressStage.Error &&
                            ImportState.stage.value != ImportProgressStage.Migrate
                        ) "Import Data ($percentage%)"
                        else if (ImportState.stage.value == ImportProgressStage.Migrate) "Migrating Data Structure"
                        else "Import Failed"
                    }
                    else {
                        "Updating Data"
                    }
                )
                setContentText(
                    if (operationType == OPERATION_BACKUP) {
                        when (BackupState.stage.value) {
                            BackupProgressStage.Init -> "Initializing backup"
                            BackupProgressStage.Compress -> "Compressing data (${ServiceState.progress.value}/${ServiceState.progressMax.value})"
                            BackupProgressStage.Encrypt -> "Encrypting data (${
                                readableSize(ServiceState.progress.value)
                            }/${readableSize(ServiceState.progressMax.value)})"
                            BackupProgressStage.Done -> "Backup complete"
                            BackupProgressStage.Error -> ServiceState.errorMessage.value
                            BackupProgressStage.Cancelled -> "Backup cancelled"
                            else -> "Unknown backup stage!"
                        }
                    }
                    else if (operationType == OPERATION_IMPORT) {
                        when (ImportState.stage.value) {
                            ImportProgressStage.Init -> "Initializing import"
                            ImportProgressStage.Decrypt -> "Decrypting data (${
                                readableSize(ServiceState.progress.value)
                            }/${readableSize(ServiceState.progressMax.value)})"
                            ImportProgressStage.Extract -> "Extracting data (${ServiceState.progress.value}/${ServiceState.progressMax.value})"
                            ImportProgressStage.Migrate -> "Migrating data structure"
                            ImportProgressStage.Done -> "Import complete"
                            ImportProgressStage.Error -> ServiceState.errorMessage.value
                            ImportProgressStage.Cancelled -> "Import cancelled"
                            else -> "Unknown import stage!"
                        }
                    }
                    else {
                        ImportState.migrationName.value
                    }
                )
                setSmallIcon(R.drawable.ic_launcher_foreground)
                setSilent(true)

                if (ongoing) {
                    setOngoing(true)

                    // Only show progress bar if backup or import is in progress
                    if (
                        BackupState.stage.value == BackupProgressStage.Init ||
                        ImportState.stage.value == ImportProgressStage.Init ||
                        ImportState.stage.value == ImportProgressStage.Migrate
                    ) {
                        setProgress(100, 0, true)
                    } else {
                        setProgress(
                            100,
                            if (ImportState.stage.value == ImportProgressStage.Migrate) 100 else percentage,
                            ImportState.stage.value == ImportProgressStage.Migrate)
                    }
                }
            }
            .build()
    }

    private fun updateNotification() {
        if (Instant.now().isBefore(lastNotificationUpdate.plusSeconds(1))) return
        lastNotificationUpdate = Instant.now()

        if (
            BackupState.stage.value != BackupProgressStage.Done &&
            ImportState.stage.value != ImportProgressStage.Done &&
            BackupState.stage.value != BackupProgressStage.Error &&
            ImportState.stage.value != ImportProgressStage.Error &&
            BackupState.stage.value != BackupProgressStage.Cancelled &&
            ImportState.stage.value != ImportProgressStage.Cancelled
        ) notificationManager.notify(2, getNotification())
        else if (
            BackupState.stage.value == BackupProgressStage.Cancelled ||
            ImportState.stage.value == ImportProgressStage.Cancelled
        ) notificationManager.notify(3, getNotification(false))
    }

    private fun updateProgress(
        progress: Long? = null,
        max: Long? = null,
        backupStage: BackupProgressStage? = null,
        importStage: ImportProgressStage? = null
    ) {
        if (progress != null) ServiceState.progress.value = progress
        if (max != null) ServiceState.progressMax.value = max
        if (backupStage != null) BackupState.stage.value = backupStage
        if (importStage != null) ImportState.stage.value = importStage

        updateNotification()
    }

    private fun processBackup(fileUri: Uri) {
        val context = applicationContext
        serviceJob = serviceScope.launch {
            val inputDirectory = context.filesDir
            val outputStream = context.contentResolver.openOutputStream(fileUri)
            outputStreams[fileUri.toString()] = outputStream

            try {
                val tempFile = File.createTempFile("backup", ".zip")
                tempFiles[tempFile.absolutePath] = tempFile

                ZipOutputStream(
                    BufferedOutputStream(
                        FileOutputStream(
                            tempFile
                        )
                    )
                ).use { zos ->
                    val includedDirs = listOf(
                        "logs",
                        "config",
                        "audio",
                        "locations"
                    )
                    val inputFiles = (inputDirectory.listFiles() ?: arrayOf<File>())
                        .filter { includedDirs.contains(it.name) }
                        .map {
                            Log.d("NanHistoryDebug", "Walking: ${it.absolutePath}")
                            it.walkTopDown()
                        }
                    val databaseFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)

                    updateProgress(
                        progress = 0,
                        max = inputFiles.sumOf { it.toList().size }.toLong() + 1,
                        backupStage = BackupProgressStage.Compress
                    )

                    val db = AppDatabase.getInstance(applicationContext)

                    db.forceCheckpoint()

                    inputFiles.forEach {
                        for (file in it) {
                            val zipFileName =
                                file.absolutePath.removePrefix(inputDirectory.absolutePath)
                                    .removePrefix("/")
                            val entry =
                                ZipEntry("$zipFileName${(if (file.isDirectory) "/" else "")}")
                            zos.putNextEntry(entry)
                            updateProgress(progress = ServiceState.progress.value + 1)
                            if (file.isFile) {
                                file.inputStream()
                                    .use { fis -> fis.copyTo(zos) }
                            }

                            Log.d(
                                "NanHistoryDebug", "File: ${file.name} [${
                                    if (file.isDirectory) "D" else "F"
                                }], ${readableSize(file.length())}, O: ${entry.name}"
                            )

                            if (!ServiceState.isRunning.value) return@launch
                        }
                    }

                    // Database file
                    val dbEntry = ZipEntry(databaseFile.name)
                    zos.putNextEntry(dbEntry)
                    if (databaseFile.isFile) {
                        databaseFile.inputStream()
                            .use { fis -> fis.copyTo(zos) }
                    }
                    Log.d(
                        "NanHistoryDebug", "File: ${databaseFile.name} [${
                            if (databaseFile.isDirectory) "D" else "F"
                        }], ${readableSize(databaseFile.length())}, O: ${dbEntry.name}"
                    )
                    updateProgress(progress = ServiceState.progress.value + 1)
                    zos.flush()
                }

                val PVqpACR05YRZx9ni = MessageDigest
                    .getInstance("SHA-512")
                    .digest(ENCRYPTION_KEY.toByteArray())

                val buffer = mutableListOf<Byte>()

                updateProgress(0, tempFile.length(), BackupProgressStage.Encrypt)

                var index = 0

                val inputStream = tempFile.inputStream()
                inputStreams["tempFileInputStream"] = inputStream

                val inputBuffer = ByteArray(8 * 1024)

                while (true) {
                    val bytesRead = inputStream.read(inputBuffer)
                    if (bytesRead == -1) break
                    for (byteRead in inputBuffer) {
                        val byte =
                            (byteRead + PVqpACR05YRZx9ni[index % PVqpACR05YRZx9ni.size] % 256).toByte()
                        buffer.add(byte)
                        if (buffer.size > 4000) {
                            outputStream?.write(buffer.toByteArray())
                            outputStream?.flush()
                            buffer.clear()
                        }
                        if (index % 500 == 0) updateProgress(progress = ServiceState.progress.value + 500)
                        if (!ServiceState.isRunning.value) return@launch
                        index++
                    }
                }
                outputStream?.write(buffer.toByteArray())
                ServiceState.progress.value = ServiceState.progressMax.value
                updateProgress(backupStage = BackupProgressStage.Done)

                inputStream.close()
                inputStreams.remove("tempFileInputStream")

                tempFile.delete()
                tempFiles.remove(tempFile.absolutePath)
            } catch(e: Exception) {
                ServiceState.errorMessage.value = e.message
                updateProgress(backupStage = BackupProgressStage.Error)
                LogData(
                    path = "error/Error ${Instant.now()}.log"
                ).apply {
                    append("$e\n${e.stackTrace}")
                    save(context)
                }
            }
            outputStream?.close()
            outputStreams.remove(fileUri.toString())

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun processImport(fileUri: Uri) {
        val context = applicationContext
        serviceJob = serviceScope.launch {
            val inputStream = context.contentResolver.openInputStream(fileUri)
            inputStreams["inputStream"] = inputStream

            try {
                val tempFile = File.createTempFile("import", ".zip.enc")
                tempFiles[tempFile.absolutePath] = tempFile
                inputStream?.use { it.copyTo(FileOutputStream(tempFile)) }

                // ACTIVATE THIS IF DECRYPTION SYSTEM IS READY
                val PVqpACR05YRZx9ni = MessageDigest
                    .getInstance("SHA-512")
                    .digest(ENCRYPTION_KEY.toByteArray())

                val encryptedStream = tempFile.inputStream()
                inputStreams["encryptedInputStream"] = encryptedStream
                // val decryptedBytes = mutableListOf<Byte>()

                updateProgress(
                    progress = 0,
                    max = tempFile.length(),
                    importStage = ImportProgressStage.Decrypt
                )

                val decryptedFile = File.createTempFile("import", ".zip")
                tempFiles[decryptedFile.absolutePath] = decryptedFile
                decryptedFile.outputStream().use { outputStream ->
                    val buffer = mutableListOf<Byte>()

                    var index = 0
                    val inputBuffer = ByteArray(8 * 1024)
                    while (true) {
                        val bytesRead = encryptedStream.read(inputBuffer)
                        if (bytesRead == -1) break
                        for (byteRead in inputBuffer) {
                            val byte =
                                (byteRead - PVqpACR05YRZx9ni[index % PVqpACR05YRZx9ni.size] + 256).toByte()
                            buffer.add(byte)
                            if (buffer.size > 4000) {
                                outputStream.write(buffer.toByteArray())
                                outputStream.flush()
                                buffer.clear()
                            }
                            if (index % 500 == 0) updateProgress(progress = ServiceState.progress.value + 500)
                            if (!ServiceState.isRunning.value) return@launch
                            index++
                        }
                    }
                    outputStream.write(buffer.toByteArray())
                }
                updateProgress(progress = ServiceState.progressMax.value)

                encryptedStream.close()
                inputStreams.remove("encryptedInputStream")

                ZipFile(decryptedFile).use { zf ->
                    val entries = zf.entries().toList()
                    val outputDirectory = context.filesDir
                    if (outputDirectory.exists() && outputDirectory.isDirectory)
                        outputDirectory.listFiles()?.forEach {
                            if (it.isDirectory) it.deleteRecursively()
                            else if (it.isFile) it.delete()
                        }
                    outputDirectory.mkdirs()

                    resetMigrationCode(context)

                    updateProgress(
                        progress = 0,
                        max = entries.size.toLong(),
                        importStage = ImportProgressStage.Extract
                    )

                    entries.forEach {
                        val outputFile = File(outputDirectory, it.name)

                        val outputFilePath: String
                        if (it.isDirectory) {
                            outputFilePath = outputFile.absolutePath
                            outputFile.mkdirs()
                        }
                        else if (it.name == AppDatabase.DATABASE_NAME) {
                            AppDatabase.clearInstance()
                            val databaseFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
                            outputFilePath = databaseFile.absolutePath
                            databaseFile.outputStream().use { fos ->
                                zf.getInputStream(it).copyTo(fos)
                            }
                        }
                        else {
                            outputFilePath = outputFile.absolutePath
                            // Log.d("NanHistoryDebug", "OPEN: ${outputFile.absolutePath}")
                            outputFile.outputStream().use { fos ->
                                // Log.d("NanHistoryDebug", "WRITE: ${outputFile.absolutePath}")
                                zf.getInputStream(it).copyTo(fos)
                            }
                        }

                        Log.d("NanHistoryDebug", "Zip entry: ${it.name} [${
                            if (it.isDirectory) "D" else "F"
                        }], ${readableSize(it.size)}, O: $outputFilePath")

                        updateProgress(progress = ServiceState.progress.value + 1)
                        if (!ServiceState.isRunning.value) return@launch
                    }
                }
                decryptedFile.delete()
                tempFiles.remove(decryptedFile.absolutePath)
                tempFiles.remove(tempFile.absolutePath)
            } catch (e: Exception) {
                ServiceState.errorMessage.value = e.message
                updateProgress(importStage = ImportProgressStage.Error)
                LogData(
                    path = "error/Error ${Instant.now()}.log"
                ).apply {
                    append("$e\n${e.stackTrace}")
                    save(context)
                }
            }
            inputStream?.close()
            inputStreams.remove("inputStream")

            updateProgress(
                progress = 0,
                max = 100,
                importStage = ImportProgressStage.Migrate
            )
            migrateData(context) { state, name ->
                updateProgress(
                    progress = (state.progress * 100).toLong()
                )
                ImportState.migrationName.value = name
            }

            updateProgress(importStage = ImportProgressStage.Done)

            val intent = Intent(ServiceBroadcast.ACTION_UPDATE_DATA)
            sendBroadcast(intent)

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()

            // exitProcess(0)

//            val activityIntent = Intent(this@DataProcessService, MainActivity::class.java)
//            activityIntent.flags =
//                Intent.FLAG_ACTIVITY_CLEAR_TOP or
//                Intent.FLAG_ACTIVITY_NEW_TASK or
//                Intent.FLAG_ACTIVITY_SINGLE_TOP
//            startActivity(activityIntent)
        }
    }

    private fun processMigration() {
        val context = applicationContext
        serviceScope.launch {
            updateProgress(importStage = ImportProgressStage.Migrate)
            migrateData(context) { state, name ->
                ImportState.migrationName.value = name
                updateProgress(
                    progress = (state.progress * 100).toLong(),
                    max = 100L
                )
            }

            updateProgress(
                progress = 100,
                importStage = ImportProgressStage.Done
            )

            updateMigrationCode(applicationContext)

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceState.isRunning.value = false

        if (serviceScope.isActive)
            serviceScope.cancel()

        if (serviceJob?.isActive == true)
            serviceJob?.cancel()

        outputStreams.forEach {
            it.value?.close()
        }
        inputStreams.forEach {
            it.value?.close()
        }
        tempFiles.forEach {
            it.value.delete()
        }
    }
}
