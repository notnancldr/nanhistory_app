package id.my.nanclouder.nanhistory.core.common.dataprocess

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

enum class BackupProgressStage {
    Init, Compress, Encrypt, Done, Error, Cancelled
}

enum class ImportProgressStage {
    Init, Decrypt, Extract, Done, Migrate, Error, Cancelled
}

object DataProcessConstants {
    const val DATA_PROCESS_CHANNEL_ID = "backup_and_data_channel"
    const val MIGRATION_CODE_FILEPATH = "migration_code"

    const val ACTION_CANCEL_SERVICE = "ACTION_CANCEL_SERVICE"

    const val OPERATION_UNKNOWN = -1
    const val OPERATION_BACKUP = 0
    const val OPERATION_IMPORT = 1
    const val OPERATION_MIGRATE = 2

    const val OPERATION_TYPE_EXTRA = "operationType"
    const val FILE_URI_EXTRA = "fileUri"
    const val LEGACY_IMPORT_EXTRA = "legacyImport"

    const val CURRENT_MIGRATION_CODE = 2
}

object DataProcessState {
    object ServiceState {
        val progress = MutableStateFlow(0L)
        val progressMax = MutableStateFlow(1L)
        val errorMessage = MutableStateFlow<String?>(null)
        val isRunning = MutableStateFlow(false)
        val operationType = MutableStateFlow(DataProcessConstants.OPERATION_UNKNOWN)
        val isLegacyImport = MutableStateFlow(false)
        val safeToUse = MutableStateFlow(false)
    }

    object BackupState {
        val stage = MutableStateFlow<BackupProgressStage?>(null)
    }

    object ImportState {
        val stage = MutableStateFlow<ImportProgressStage?>(null)
        val migrationName = MutableStateFlow<String?>(null)
    }

    fun checkMigration(context: Context): Boolean {
        val migrationCodeFile = File(context.filesDir, DataProcessConstants.MIGRATION_CODE_FILEPATH)
        return if (migrationCodeFile.exists()) {
            try {
                migrationCodeFile.readText().trim().toInt() == DataProcessConstants.CURRENT_MIGRATION_CODE
            } catch (e: Exception) {
                false
            }
        } else false
    }

    fun updateMigrationCode(context: Context) {
        val migrationCodeFile = File(context.filesDir, DataProcessConstants.MIGRATION_CODE_FILEPATH)
        migrationCodeFile.writeText(DataProcessConstants.CURRENT_MIGRATION_CODE.toString())
    }

    fun resetMigrationCode(context: Context) {
        val migrationCodeFile = File(context.filesDir, DataProcessConstants.MIGRATION_CODE_FILEPATH)
        migrationCodeFile.delete()
    }
}

interface DataProcessController {
    fun startMigration(context: Context)
    fun startBackup(context: Context, fileUri: Uri)
    fun startImport(context: Context, fileUri: Uri, legacyImport: Boolean)
    fun cancelService(context: Context)
}

object DataProcessBridge {
    private var controller: DataProcessController? = null

    fun register(controller: DataProcessController) {
        this.controller = controller
    }

    fun startMigration(context: Context) {
        controller?.startMigration(context)
    }

    fun startBackup(context: Context, fileUri: Uri) {
        controller?.startBackup(context, fileUri)
    }

    fun startImport(context: Context, fileUri: Uri, legacyImport: Boolean = false) {
        controller?.startImport(context, fileUri, legacyImport)
    }

    fun cancel(context: Context) {
        controller?.cancelService(context)
    }
}
