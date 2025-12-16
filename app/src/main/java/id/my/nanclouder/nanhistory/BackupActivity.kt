package id.my.nanclouder.nanhistory

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.utils.readableSize
import id.my.nanclouder.nanhistory.service.DataProcessService
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import id.my.nanclouder.nanhistory.utils.LegacyImport
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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

@Composable
fun BackupView() {
    val newUI = Config.appearanceNewUI.getCache()
    if (newUI) BackupView_New() else BackupView_Old()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupView_New() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val progress by DataProcessService.ServiceState.progress.collectAsState()
    val progressTarget by DataProcessService.ServiceState.progressMax.collectAsState()
    val backupStage by DataProcessService.BackupState.stage.collectAsState()
    val importStage by DataProcessService.ImportState.stage.collectAsState()
    val currentOperation by DataProcessService.ServiceState.operationType.collectAsState()
    val serviceIsRunning by DataProcessService.ServiceState.isRunning.collectAsState()

    var importConfirmation by remember { mutableStateOf(false) }
    var importPath by remember { mutableStateOf<String?>(null) }
    var legacyImport by remember { mutableStateOf(false) }

    val launcherBackup = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                data?.data?.let { uri ->
                    val intent = Intent(context, DataProcessService::class.java)
                    intent.putExtra(DataProcessService.FILE_URI_EXTRA, uri.toString())
                    intent.putExtra(DataProcessService.OPERATION_TYPE_EXTRA, DataProcessService.OPERATION_BACKUP)
                    context.startForegroundService(intent)
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
                    importPath = uri.toString()
                    legacyImport = false
                    importConfirmation = true
                } ?: snackbarHostState.showSnackbar("No file selected")
            } else {
                snackbarHostState.showSnackbar("Import canceled")
            }
        }
    }

    val launcherLegacyImport = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                data?.data?.let { uri ->
                    importPath = uri.toString()
                    legacyImport = true
                    importConfirmation = true
                } ?: snackbarHostState.showSnackbar("No file selected")
            } else {
                snackbarHostState.showSnackbar("Import canceled")
            }
        }
    }

    if (currentOperation == DataProcessService.OPERATION_IMPORT && serviceIsRunning) {
        val activityIntent = Intent(context, MainActivity::class.java)
        activityIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        context.startActivity(activityIntent)
        context.getActivity()?.finish()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Backup & Restore",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { context.getActivity()!!.finish() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val operationEnabled = (backupStage == null || backupStage == BackupProgressStage.Done) &&
                    (importStage == null || importStage == ImportProgressStage.Done)

            // Backup Card
            BackupCard(
                stage = backupStage,
                progress = progress,
                progressTarget = progressTarget,
                isOperationRunning = serviceIsRunning && currentOperation == DataProcessService.OPERATION_BACKUP,
                onBackupClick = {
                    if (!serviceIsRunning) {
                        handleBackup(launcherBackup)
                    } else {
                        val cancelIntent = Intent(context, DataProcessService::class.java).apply {
                            action = DataProcessService.ACTION_CANCEL_SERVICE
                        }
                        context.startService(cancelIntent)
                    }
                }
            )

            // Import Card
            ImportCard(
                stage = importStage,
                progress = progress,
                progressTarget = progressTarget,
                isOperationRunning = serviceIsRunning && currentOperation == DataProcessService.OPERATION_IMPORT && !legacyImport,
                onImportClick = {
                    if (!serviceIsRunning) {
                        handleImport(launcherImport)
                    }
                }
            )

            ImportLegacyCard(
                stage = importStage,
                progress = progress,
                progressTarget = progressTarget,
                isOperationRunning = serviceIsRunning && currentOperation == DataProcessService.OPERATION_IMPORT && legacyImport,
                onImportClick = {
                    if (!serviceIsRunning) {
                        handleLegacyImport(launcherLegacyImport)
                    }
                }
            )
        }
    }

    if (importConfirmation) {
        AlertDialog(
            onDismissRequest = { importConfirmation = false },
            title = {
                Text(
                    "Import Data",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Importing this data will overwrite the existing data. Are you sure you want to continue?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        importConfirmation = false
                        val intent = Intent(context, DataProcessService::class.java)
                        intent.putExtra(DataProcessService.FILE_URI_EXTRA, importPath ?: "")
                        intent.putExtra(DataProcessService.OPERATION_TYPE_EXTRA, DataProcessService.OPERATION_IMPORT)
                        intent.putExtra(DataProcessService.LEGACY_IMPORT_EXTRA, legacyImport)
                        context.startForegroundService(intent)
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { importConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BackupCard(
    stage: BackupProgressStage?,
    progress: Long,
    progressTarget: Long,
    isOperationRunning: Boolean,
    onBackupClick: () -> Unit
) {
    val isActive = stage != null && stage != BackupProgressStage.Done && stage != BackupProgressStage.Error && stage != BackupProgressStage.Cancelled

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.CloudUpload,
                        "Backup",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Data Backup",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (stage == null) "Create backup file and store it locally" else getBackupStatusText(stage),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Progress Indicator
            if (stage != null && stage != BackupProgressStage.Done && stage != BackupProgressStage.Error && stage != BackupProgressStage.Cancelled) {
                val progressTargetSafe = if (progressTarget == 0L) 1L else progressTarget
                val progressPercentage = progress * 100f / progressTargetSafe
                val progressPercentageInt = progressPercentage.toInt()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        when (stage) {
                            BackupProgressStage.Init -> "Initializing..."
                            BackupProgressStage.Compress -> "Compressing ($progressPercentageInt%)"
                            BackupProgressStage.Encrypt -> "Encrypting ($progressPercentageInt%) • ${readableSize(progress)}/${readableSize(progressTarget)}"
                            else -> ""
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (stage == BackupProgressStage.Init) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                        )
                    } else {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
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

            // Button
            Button(
                onClick = onBackupClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = if (isOperationRunning) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(
                    if (isOperationRunning) "Cancel Backup" else "Backup Now",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ImportCard(
    stage: ImportProgressStage?,
    progress: Long,
    progressTarget: Long,
    isOperationRunning: Boolean,
    onImportClick: () -> Unit
) {
    val isActive = stage != null && stage != ImportProgressStage.Done && stage != ImportProgressStage.Error && stage != ImportProgressStage.Cancelled

    val secondaryColor = MaterialTheme.colorScheme.secondary
    val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.CloudDownload,
                        "Import",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Import Backup",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (stage == null) "Restore history data from backup file" else getImportStatusText(stage),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Progress Indicator
            if (stage != null && stage != ImportProgressStage.Done && stage != ImportProgressStage.Error && stage != ImportProgressStage.Cancelled) {
                val progressTargetSafe = if (progressTarget == 0L) 1L else progressTarget
                val progressPercentage = progress * 100f / progressTargetSafe
                val progressPercentageInt = progressPercentage.toInt()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        when (stage) {
                            ImportProgressStage.Init -> "Initializing..."
                            ImportProgressStage.Decrypt -> "Decrypting ($progressPercentageInt%) • ${readableSize(progress)}/${readableSize(progressTarget)}"
                            ImportProgressStage.Extract -> "Extracting ($progressPercentageInt%)"
                            ImportProgressStage.Migrate -> "Migrating data structure..."
                            else -> ""
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (stage == ImportProgressStage.Init) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                        )
                    } else {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        ) {
                            drawLine(
                                color = secondaryContainerColor,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = size.height,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = secondaryColor,
                                start = Offset(0f, 0f),
                                end = Offset(size.width * progressPercentage / 100, 0f),
                                strokeWidth = size.height,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            // Button
            Button(
                onClick = onImportClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = if (isOperationRunning) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                }
            ) {
                Text(
                    if (isOperationRunning) "Cancel Import" else "Import Now",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ImportLegacyCard(
    stage: ImportProgressStage?,
    progress: Long,
    progressTarget: Long,
    isOperationRunning: Boolean,
    onImportClick: () -> Unit
) {
    val isActive = stage != null && stage != ImportProgressStage.Done && stage != ImportProgressStage.Error && stage != ImportProgressStage.Cancelled

    val secondaryColor = MaterialTheme.colorScheme.secondary
    val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.CloudDownload,
                        "Import",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Import Legacy Backup",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (stage == null) "Restore history data from Digital History backup data" else getImportStatusText(stage),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Progress Indicator
            if (stage != null && stage != ImportProgressStage.Done && stage != ImportProgressStage.Error && stage != ImportProgressStage.Cancelled) {
                val progressTargetSafe = if (progressTarget == 0L) 1L else progressTarget
                val progressPercentage = progress * 100f / progressTargetSafe
                val progressPercentageInt = progressPercentage.toInt()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        when (stage) {
                            ImportProgressStage.Init -> "Initializing..."
                            ImportProgressStage.Decrypt -> "Decrypting ($progressPercentageInt%) • ${readableSize(progress)}/${readableSize(progressTarget)}"
                            ImportProgressStage.Extract -> "Extracting ($progressPercentageInt%)"
                            ImportProgressStage.Migrate -> "Migrating data structure..."
                            else -> ""
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (stage == ImportProgressStage.Init) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                        )
                    } else {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        ) {
                            drawLine(
                                color = secondaryContainerColor,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = size.height,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = secondaryColor,
                                start = Offset(0f, 0f),
                                end = Offset(size.width * progressPercentage / 100, 0f),
                                strokeWidth = size.height,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            // Button
            Button(
                onClick = onImportClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = if (isOperationRunning) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                }
            ) {
                Text(
                    if (isOperationRunning) "Cancel Import" else "Import Now",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun getBackupStatusText(stage: BackupProgressStage): String = when (stage) {
    BackupProgressStage.Init -> "Getting ready..."
    BackupProgressStage.Compress -> "Processing files..."
    BackupProgressStage.Encrypt -> "Securing data..."
    BackupProgressStage.Done -> "Backup complete"
    BackupProgressStage.Error -> "Backup failed"
    BackupProgressStage.Cancelled -> "Backup cancelled"
}

private fun getImportStatusText(stage: ImportProgressStage): String = when (stage) {
    ImportProgressStage.Init -> "Getting ready..."
    ImportProgressStage.Decrypt -> "Reading backup..."
    ImportProgressStage.Extract -> "Unpacking data..."
    ImportProgressStage.Migrate -> "Updating data..."
    ImportProgressStage.Done -> "Import complete"
    ImportProgressStage.Error -> "Import failed"
    ImportProgressStage.Cancelled -> "Import cancelled"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupView_Old() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current

//    var progress by rememberSaveable { mutableLongStateOf(0L) }
//    var progressTarget by rememberSaveable { mutableLongStateOf(0L) }
//    var backupStage by rememberSaveable { mutableStateOf<BackupProgressStage?>(null) }

    val progress by DataProcessService.ServiceState.progress.collectAsState()
    val progressTarget by DataProcessService.ServiceState.progressMax.collectAsState()
    val backupStage by DataProcessService.BackupState.stage.collectAsState()
    val importStage by DataProcessService.ImportState.stage.collectAsState()
    val currentOperation by DataProcessService.ServiceState.operationType.collectAsState()

    val serviceIsRunning by DataProcessService.ServiceState.isRunning.collectAsState()

    var importConfirmation by remember { mutableStateOf(false) }
    var importPath by remember { mutableStateOf<String?>(null) }

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
                    val intent = Intent(context, DataProcessService::class.java)
                    intent.putExtra(DataProcessService.FILE_URI_EXTRA, uri.toString())
                    intent.putExtra(DataProcessService.OPERATION_TYPE_EXTRA, DataProcessService.OPERATION_BACKUP)
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
                    importPath = uri.toString()
                    importConfirmation = true
                } ?: snackbarHostState.showSnackbar("No file selected")

            } else {
                snackbarHostState.showSnackbar("Import canceled")
            }
        }
    }

    val launcherLegacyImport = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data

                data?.data?.let { uri ->
                    importPath = uri.toString()
                    importConfirmation = true
                } ?: snackbarHostState.showSnackbar("No file selected")

            } else {
                snackbarHostState.showSnackbar("Import canceled")
            }
        }
    }

    if (
        currentOperation == DataProcessService.OPERATION_IMPORT &&
        serviceIsRunning
    ) {
        val activityIntent = Intent(context, MainActivity::class.java)
        activityIntent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_SINGLE_TOP

        context.startActivity(activityIntent)
        context.getActivity()?.finish()
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
                    if (currentStage == null) Text("Create backup file and store it in local storage")
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
                        enabled = !(serviceIsRunning && currentOperation == DataProcessService.OPERATION_IMPORT),
                        onClick = {
                            if (!serviceIsRunning) {
                                handleBackup(launcherBackup)
                            }
                            else {
                                val cancelIntent = Intent(context, DataProcessService::class.java).apply {
                                    action = DataProcessService.ACTION_CANCEL_SERVICE
                                }
                                context.startService(cancelIntent)
                            }
                        },
                        colors =
                            if (serviceIsRunning) {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
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
                    if (currentStage == null) Text("Restore history data from local backup file")
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
                        enabled = !serviceIsRunning,
                        onClick = {
                            if (!serviceIsRunning) {
                                handleImport(launcherImport)
                            }
                        },
                        colors =
                            if (serviceIsRunning) {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            } else {
                                ButtonDefaults.buttonColors()
                            }
                    ) {
                        Text("Import Now")
                    }
                }
            )
            if (importConfirmation) AlertDialog(
                onDismissRequest = {
                    importConfirmation = false
                },
                title = {
                    Text("Import Data")
                },
                text = {
                    Text("Importing this data will overwrite the existing data, are you sure want to continue?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            importConfirmation = false
                            val intent = Intent(context, DataProcessService::class.java)
                            intent.putExtra(DataProcessService.FILE_URI_EXTRA, importPath ?: "")
                            intent.putExtra(DataProcessService.OPERATION_TYPE_EXTRA, DataProcessService.OPERATION_IMPORT)
                            context.startForegroundService(intent)
                        }
                    ) {
                        Text("Import")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            importConfirmation = false
                        }
                    ) {
                        Text("Cancel")
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
            "application/octet-stream"
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
            "application/octet-stream"
    }
    launcher.launch(intent)
}

fun handleLegacyImport(launcher: ActivityResultLauncher<Intent>, legacyImport: Boolean = false) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        // addCategory(Intent.CATEGORY_OPENABLE)
        // type =
        //     "application/octet-stream"
        val initialUri = Uri.parse(
            "content://com.android.externalstorage.documents/tree/primary:Documents/DigitalHistory"
        )
        putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
    }
    launcher.launch(intent)
}

fun startLegacyImport(context: Context, uri: Uri) {
    try {
        val intent = Intent(context, DataProcessService::class.java)
        intent.putExtra(DataProcessService.FILE_URI_EXTRA, uri.toString())
        intent.putExtra(
            DataProcessService.OPERATION_TYPE_EXTRA,
            DataProcessService.OPERATION_IMPORT
        )
        intent.putExtra(
            DataProcessService.LEGACY_IMPORT_EXTRA,
            true
        )
        context.startForegroundService(intent)
    } catch (e: Throwable) {
        Log.d("NanHistoryDebug", e.message ?: "Unknown error")
        e.printStackTrace()
    }
}

@Preview(showBackground = true)
@Composable
fun BackupPreview() {
    NanHistoryTheme {
        BackupView()
    }
}