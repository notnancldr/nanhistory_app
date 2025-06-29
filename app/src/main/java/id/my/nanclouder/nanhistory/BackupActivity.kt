package id.my.nanclouder.nanhistory

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.my.nanclouder.nanhistory.lib.LogData
import id.my.nanclouder.nanhistory.lib.readableSize
import id.my.nanclouder.nanhistory.service.BackupService
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

const val wAt2J7GmpkeWSRad = "6KwXgzCInQAjdwxtyVG5ZfNtk82BhNSC9KQN9mynQYXcQ6Ut4mAlVS1iGMejg09P"

class BackupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            NanHistoryTheme {
                // A surface container using the 'background' color from the theme
                BackupView()
            }
        }
    }
}

enum class BackupProgressStage {
    Init, Compress, Encrypt, Done, Error, Cancelled
}

enum class ImportProgressStage {
    Init, Decrypt, Extract, Done, Migrate, Error, Cancelled
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupView() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current

//    var progress by rememberSaveable { mutableLongStateOf(0L) }
//    var progressTarget by rememberSaveable { mutableLongStateOf(0L) }
//    var backupStage by rememberSaveable { mutableStateOf<BackupProgressStage?>(null) }

    val progress by BackupService.ServiceState.progress.collectAsState()
    val progressTarget by BackupService.ServiceState.progressMax.collectAsState()
    val backupStage by BackupService.BackupState.stage.collectAsState()
    val importStage by BackupService.ImportState.stage.collectAsState()
    val currentOperation by BackupService.ServiceState.operationType.collectAsState()

    val serviceIsRunning by BackupService.ServiceState.isRunning.collectAsState()

//    if (importStage == ImportProgressStage.Done) {
//        val intent = Intent(context, MainActivity::class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_CLEAR_TASK)
//        val stageNull = {
//            BackupService.ImportState.stage.value = null
//        }
//
//        stageNull()
//        context.startActivity(intent)
//        context.getActivity()?.finish()
//    }

    // Create the launcher
    val launcherBackup = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                data?.data?.let { uri ->
                    val intent = Intent(context, BackupService::class.java)
                    intent.putExtra(BackupService.FILE_URI_EXTRA, uri.toString())
                    intent.putExtra(BackupService.OPERATION_TYPE_EXTRA, BackupService.OPERATION_BACKUP)
                    context.startForegroundService(intent)

//                    snackbarHostState.showSnackbar("Backup success")
                } ?: snackbarHostState.showSnackbar("No file selected")

            } else {
                snackbarHostState.showSnackbar("Backup canceled")
            }
        }
    }

    val launcherImport = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data

                data?.data?.let { uri ->
                    val intent = Intent(context, BackupService::class.java)
                    intent.putExtra(BackupService.FILE_URI_EXTRA, uri.toString())
                    intent.putExtra(BackupService.OPERATION_TYPE_EXTRA, BackupService.OPERATION_IMPORT)
                    context.startForegroundService(intent)
                } ?: snackbarHostState.showSnackbar("No file selected")

            } else {
                snackbarHostState.showSnackbar("Import canceled")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("Backup Options")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            context.getActivity()!!.finish()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
        ) {
            val operationEnabled = (backupStage == null || backupStage == BackupProgressStage.Done) &&
                    (importStage == null || importStage == ImportProgressStage.Done)
            ListItem(
                leadingContent = {
                    Icon(painterResource(R.drawable.ic_cloud_upload), "Backup")
                },
                headlineContent = {
                    Text("Data Backup")
                },
                supportingContent = {
                    val currentStage = backupStage
                    if (currentStage == null) Text("Backup important data in 'history' directory.")
                    else {
                        Column {
                            val progressTargetSafe = if (progressTarget == 0L) 1L else progressTarget
                            val progressPercentage = progress * 100f / progressTargetSafe
                            val progressPercentageInt = progressPercentage.toInt()
                            Text(
                                when (currentStage) {
                                    BackupProgressStage.Init -> "Initializing"
                                    BackupProgressStage.Compress -> "Compressing ${progressPercentageInt}% ($progress/$progressTarget)"
                                    BackupProgressStage.Encrypt ->
                                        "Encrypting $progressPercentageInt% (${readableSize(progress)}/${readableSize(progressTarget)})"
                                    BackupProgressStage.Done -> "Done"
                                    BackupProgressStage.Error -> "Backup failed"
                                    BackupProgressStage.Cancelled -> "Backup cancelled"
                                }
                            )
                            if (currentStage == BackupProgressStage.Init) LinearProgressIndicator(Modifier.fillMaxWidth())
                            else if (currentStage == BackupProgressStage.Compress || currentStage == BackupProgressStage.Encrypt) {
                                val primaryColor = MaterialTheme.colorScheme.primary
                                val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .padding(vertical = 4.dp)
                                ) {
                                    drawLine(
                                        color = primaryContainerColor,
                                        start = Offset(0f, 0f),
                                        end = Offset(size.width, 0f),
                                        strokeWidth = size.height,
                                        cap = StrokeCap.Round
                                    )
                                    drawLine(
                                        color = primaryColor,
                                        start = Offset(0f, 0f),
                                        end = Offset(size.width * progressPercentage / 100, 0f),
                                        strokeWidth = size.height,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }
                },
                trailingContent = {
                    Button(
                        enabled = !(serviceIsRunning && currentOperation == BackupService.OPERATION_IMPORT),
                        onClick = {
                            if (!serviceIsRunning) {
                                handleBackup(launcherBackup)
                            }
                            else {
                                val cancelIntent = Intent(context, BackupService::class.java).apply {
                                    action = BackupService.ACTION_CANCEL_SERVICE
                                }
                                context.startService(cancelIntent)
                            }
                        },
                        colors =
                            if (serviceIsRunning) {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            }
                            else {
                                ButtonDefaults.buttonColors()
                            }
                    ) {
                        Text(if (serviceIsRunning) "Cancel" else "Backup Now")
                    }
                }
            )
            ListItem(
                leadingContent = {
                    Icon(painterResource(R.drawable.ic_cloud_download), "Import")
                },
                headlineContent = {
                    Text("Import Backup Data")
                },
                supportingContent = {
                    val currentStage = importStage
                    if (currentStage == null) Text("Import backup data to 'history' directory,")
                    else {
                        Column {
                            val progressTargetSafe = if (progressTarget == 0L) 1L else progressTarget
                            val progressPercentage = progress * 100f / progressTargetSafe
                            val progressPercentageInt = progressPercentage.toInt()
                            Text(
                                when (currentStage) {
                                    ImportProgressStage.Init -> "Initializing"
                                    ImportProgressStage.Decrypt ->
                                        "Decrypting $progressPercentageInt% (${readableSize(progress)}/${readableSize(progressTarget)})"
                                    ImportProgressStage.Extract ->
                                        "Extracting $progressPercentageInt% ($progress/$progressTarget)"
                                    ImportProgressStage.Migrate -> "Migrating data structure"
                                    ImportProgressStage.Done -> "Done"
                                    ImportProgressStage.Error -> "Backup failed"
                                    ImportProgressStage.Cancelled -> "Backup cancelled"
                                }
                            )
                            if (currentStage == ImportProgressStage.Init) LinearProgressIndicator(Modifier.fillMaxWidth())
                            else if (currentStage == ImportProgressStage.Decrypt || currentStage == ImportProgressStage.Extract) {
                                val primaryColor = MaterialTheme.colorScheme.primary
                                val primaryContainerColor =
                                    MaterialTheme.colorScheme.primaryContainer
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .padding(vertical = 4.dp)
                                ) {
                                    drawLine(
                                        color = primaryContainerColor,
                                        start = Offset(0f, 0f),
                                        end = Offset(size.width, 0f),
                                        strokeWidth = size.height,
                                        cap = StrokeCap.Round
                                    )
                                    drawLine(
                                        color = primaryColor,
                                        start = Offset(0f, 0f),
                                        end = Offset(size.width * progressPercentage / 100, 0f),
                                        strokeWidth = size.height,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }
                },
                trailingContent = {
                    Button(
                        enabled = !(serviceIsRunning && currentOperation == BackupService.OPERATION_BACKUP),
                        onClick = {
                            if (!serviceIsRunning) {
                                handleImport(launcherImport)
                            }
                            else {
                                val cancelIntent = Intent(context, BackupService::class.java).apply {
                                    action = BackupService.ACTION_CANCEL_SERVICE
                                }
                                context.startService(cancelIntent)
                            }
                        },
                        colors =
                            if (serviceIsRunning) {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            } else {
                                ButtonDefaults.buttonColors()
                            }
                    ) {
                        Text(if (serviceIsRunning) "Cancel" else "Import Now")
                    }
                }
            )
        }
    }
}

fun handleBackup(launcher: ActivityResultLauncher<Intent>) {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        val now = ZonedDateTime.now()
        addCategory(Intent.CATEGORY_OPENABLE)
        type =
            "application/octet-stream" // Specify the file type (e.g., text/plain, application/pdf, etc.)
        putExtra(
            Intent.EXTRA_TITLE,
            DateTimeFormatter.ofPattern("'Backup' yyyyMMdd-HHmmssSSSS'.enc'").format(now)
        ) // Suggested filename
    }
    launcher.launch(intent)
}

fun handleImport(launcher: ActivityResultLauncher<Intent>) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type =
            "application/octet-stream" // Specify the file type (e.g., text/plain, application/pdf, etc.)
    }
    launcher.launch(intent)
}

@Preview(showBackground = true)
@Composable
fun BackupPreview() {
    NanHistoryTheme {
        BackupView()
    }
}