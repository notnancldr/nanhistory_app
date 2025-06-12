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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import id.my.nanclouder.nanhistory.lib.LogData
import id.my.nanclouder.nanhistory.lib.readableSize
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
    Init, Compress, Encrypt, Done, Error
}

enum class ImportProgressStage {
    Init, Decrypt, Extract, Done, Error
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupView() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current

    var progress by rememberSaveable { mutableLongStateOf(0L) }
    var progressTarget by rememberSaveable { mutableLongStateOf(0L) }
    var backupStage by rememberSaveable { mutableStateOf<BackupProgressStage?>(null) }
    var importStage by rememberSaveable { mutableStateOf<ImportProgressStage?>(null) }

    // Create the launcher
    val launcherBackup = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                var outputStream: OutputStream? = null
                backupStage = BackupProgressStage.Init
                try {
                    data?.data?.let { uri ->
                        withContext(Dispatchers.IO) {
                            val inputDirectory = context.filesDir
                            outputStream = context.contentResolver.openOutputStream(uri)

                            val tempFile = File.createTempFile("backup", ".zip")

                            ZipOutputStream(
                                BufferedOutputStream(
                                    FileOutputStream(
                                        tempFile
                                    )
                                )
                            ).use { zos ->
                                val includedDirs = listOf(
                                    "history",
                                    "logs",
                                    "config",
                                    "audio",
                                    "locations"
                                )
                                val inputFiles = (inputDirectory.listFiles() ?: arrayOf<File>())
                                    .filter { includedDirs.contains(it.name) }
                                    .map {
                                        it.walkTopDown()
                                    }

                                backupStage = BackupProgressStage.Compress
                                progress = 0
                                progressTarget = inputFiles.sumOf {
                                    it.toList().size
                                }.toLong()

                                inputFiles.forEach {
                                    it.forEach { file ->
                                        val zipFileName =
                                            file.absolutePath.removePrefix(inputDirectory.absolutePath)
                                                .removePrefix("/")
                                        val entry =
                                            ZipEntry("$zipFileName${(if (file.isDirectory) "/" else "")}")
                                        zos.putNextEntry(entry)
                                        progress++
                                        if (file.isFile) {
                                            file.inputStream()
                                                .use { fis -> fis.copyTo(zos) }
                                        }
                                    }
                                }
                                zos.flush()
                            }

                            val PVqpACR05YRZx9ni = MessageDigest
                                .getInstance("SHA-512")
                                .digest(wAt2J7GmpkeWSRad.toByteArray())

                            val buffer = mutableListOf<Byte>()

                            backupStage = BackupProgressStage.Encrypt
                            progress = 0
                            progressTarget = tempFile.length()

                            for (indexed in tempFile.readBytes().withIndex()) {
                                val byte = (indexed.value + PVqpACR05YRZx9ni[indexed.index % PVqpACR05YRZx9ni.size] % 256).toByte()
                                buffer.add(byte)
                                if (buffer.size > 1_000_000) {
                                    outputStream?.write(buffer.toByteArray())
                                    buffer.clear()
                                }
                                if (indexed.index % 1000 == 0) progress += 1000
                            }
                            outputStream?.write(buffer.toByteArray())
                            progress = progressTarget

                        }
                        backupStage = BackupProgressStage.Done
                        snackbarHostState.showSnackbar("Backup success")
                    } ?: snackbarHostState.showSnackbar("Backup success")
                } catch (e: Exception) {
                    backupStage = BackupProgressStage.Error
                    snackbarHostState.showSnackbar("An error occurred during backup process")
                    LogData(
                        path = "error/Error ${Instant.now()}.log"
                    ).apply {
                        append("$e\n${e.stackTrace}")
                        save(context)
                    }
                }
                outputStream?.close()
            } else {
                snackbarHostState.showSnackbar("Failed to create file")
            }
        }
    }

    val launcherImport = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                var inputStream: InputStream? = null

                importStage = ImportProgressStage.Init

                try {
                    data?.data?.let { uri ->
                        withContext(Dispatchers.IO) {
                            inputStream = context.contentResolver.openInputStream(uri)

                            val tempFile = File.createTempFile("import", ".zip.enc")
                            inputStream?.use { it.copyTo(FileOutputStream(tempFile)) }

                            // ACTIVATE THIS IF DECRYPTION SYSTEM IS READY
                            val PVqpACR05YRZx9ni = MessageDigest
                                .getInstance("SHA-512")
                                .digest(wAt2J7GmpkeWSRad.toByteArray())

                            val encryptedBytes = tempFile.readBytes()
                            val decryptedBytes = mutableListOf<Byte>()

                            importStage = ImportProgressStage.Decrypt
                            progress = 0
                            progressTarget = tempFile.length()

                            val decryptedFile = File.createTempFile("import", ".zip")
                            decryptedFile.outputStream().use { outputStream ->
                                val buffer = mutableListOf<Byte>()

                                for (indexed in encryptedBytes.withIndex()) {
                                    val byte =
                                        (indexed.value - PVqpACR05YRZx9ni[indexed.index % PVqpACR05YRZx9ni.size] + 256).toByte()
                                    buffer.add(byte)
                                    if (buffer.size > 1_000_000) {
                                        outputStream.write(buffer.toByteArray())
                                        buffer.clear()
                                    }
                                    if (indexed.index % 1000 == 0) progress += 1000
                                }
                                outputStream.write(buffer.toByteArray())
//                                decryptedFile.writeBytes(decryptedBytes.toByteArray())
                            }
                            progress = progressTarget

                            ZipFile(decryptedFile).use { zf ->
                                val entries = zf.entries().toList()
                                val outputDirectory = context.filesDir
                                if (outputDirectory.exists() && outputDirectory.isDirectory)
                                    outputDirectory.listFiles()?.forEach {
                                        if (it.isDirectory) it.deleteRecursively()
                                        else if (it.isFile) it.delete()
                                    }
                                outputDirectory.mkdirs()

                                importStage = ImportProgressStage.Extract
                                progress = 0
                                progressTarget = entries.size.toLong()

                                entries.forEach {
                                    val outputFile = File(outputDirectory, it.name)
//                                    Log.d("NanHistoryDebug", "Zip entry: ${it.name} [${
//                                        if (it.isDirectory) "D" else "F"
//                                    }], ${readableSize(it.size)}")
                                    if (it.isDirectory) {
                                        outputFile.mkdirs()
                                    } else {
//                                        Log.d("NanHistoryDebug", "OPEN: ${outputFile.absolutePath}")
                                        outputFile.outputStream().use { fos ->
//                                            Log.d("NanHistoryDebug", "WRITE: ${outputFile.absolutePath}")
                                            zf.getInputStream(it).copyTo(fos)
                                        }
                                    }
                                    progress++
                                }
                            }
                        }
                        importStage = ImportProgressStage.Done
                        snackbarHostState.showSnackbar("Import success")
                    } ?: snackbarHostState.showSnackbar("No file selected")
                } catch (e: Exception) {
                    importStage = ImportProgressStage.Error
                    snackbarHostState.showSnackbar("An error occurred during import process")
                    LogData(
                        path = "error/Error ${Instant.now()}.log"
                    ).apply {
                        append("$e\n${e.stackTrace}")
                        save(context)
                    }
                }
                inputStream?.close()
            } else {
                snackbarHostState.showSnackbar("Failed to import file")
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
                            val progressPercentage = progress * 100 / progressTargetSafe
                            Text(
                                when (currentStage) {
                                    BackupProgressStage.Init -> "Initializing"
                                    BackupProgressStage.Compress -> "Compressing $progressPercentage% ($progress/$progressTarget)"
                                    BackupProgressStage.Encrypt ->
                                        "Encrypting $progressPercentage% (${readableSize(progress)}/${readableSize(progressTarget)})"
                                    BackupProgressStage.Done -> "Done"
                                    BackupProgressStage.Error -> "Backup failed"
                                }
                            )
                            if (currentStage == BackupProgressStage.Init) LinearProgressIndicator(Modifier.fillMaxWidth())
                            else if (currentStage == BackupProgressStage.Compress || currentStage == BackupProgressStage.Encrypt)
                                LinearProgressIndicator(
                                    progress = { (progress / progressTargetSafe).toFloat() },
                                    modifier = Modifier.fillMaxWidth()
                                )
                        }
                    }
                },
                trailingContent = {
                    Button(
                        enabled = operationEnabled,
                        onClick = { handleBackup(launcherBackup) }
                    ) {
                        Text("Backup Now")
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
                            val progressPercentage = progress * 100 / progressTargetSafe
                            Text(
                                when (currentStage) {
                                    ImportProgressStage.Init -> "Initializing"
                                    ImportProgressStage.Decrypt ->
                                        "Decrypting $progressPercentage% (${readableSize(progress)}/${readableSize(progressTarget)})"
                                    ImportProgressStage.Extract ->
                                        "Extracting $progressPercentage% ($progress/$progressTarget)"
                                    ImportProgressStage.Done -> "Done"
                                    ImportProgressStage.Error -> "Backup failed"
                                }
                            )
                            if (currentStage == ImportProgressStage.Init) LinearProgressIndicator(Modifier.fillMaxWidth())
                            else if (currentStage == ImportProgressStage.Decrypt || currentStage == ImportProgressStage.Extract)
                                LinearProgressIndicator(
                                    progress = { (progress / progressTargetSafe).toFloat() },
                                    modifier = Modifier.fillMaxWidth()
                                )
                        }
                    }
                },
                trailingContent = {
                    Button(
                        enabled = operationEnabled,
                        onClick = { handleImport(launcherImport) }
                    ) {
                        Text("Import Backup")
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