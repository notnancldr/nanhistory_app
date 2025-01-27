package id.my.nanclouder.nanhistory

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import id.my.nanclouder.nanhistory.lib.LogData
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupView() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current

    // Create the launcher
    val launcherBackup = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            var outputStream: OutputStream? = null
            try {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Creating backup...",
                        duration = SnackbarDuration.Short
                    )
                }

                data?.data?.let { uri ->

                    val inputDirectory = context.filesDir
                    outputStream = context.contentResolver.openOutputStream(uri)

                    val tempFile = File.createTempFile("backup", ".zip")

                    ZipOutputStream(
                        BufferedOutputStream(
                            FileOutputStream(
                                tempFile
                            )
//                            outputStream
                        )
                    ).use { zos ->
                        inputDirectory.walkTopDown().forEach { file ->
                            val zipFileName =
                                file.absolutePath.removePrefix(inputDirectory.absolutePath)
                                    .removePrefix("/")
                            val entry =
                                ZipEntry("$zipFileName${(if (file.isDirectory) "/" else "")}")
                            zos.putNextEntry(entry)
                            if (file.isFile) {
                                file.inputStream()
                                    .use { fis -> fis.copyTo(zos) }
                            }
                        }
                        zos.flush()
                    }
//                    outputStream.copyTo(file)

                    // ACTIVATE THIS IF ENCRYPTION SYSTEM IS READY
                    val PVqpACR05YRZx9ni = MessageDigest
                        .getInstance("SHA-512")
                        .digest(wAt2J7GmpkeWSRad.toByteArray())


                    val fileBytes = mutableListOf<Byte>()
                    for (indexed in tempFile.readBytes().withIndex()) {
                        fileBytes.add((indexed.value + PVqpACR05YRZx9ni[indexed.index % PVqpACR05YRZx9ni.size] % 256).toByte())
                    }
                    outputStream?.write(fileBytes.toByteArray())



                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Backup success",
                            duration = SnackbarDuration.Short
                        )
                    }
                } ?: scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Backup success",
                        duration = SnackbarDuration.Short
                    )
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "An error occurred during backup process",
                        duration = SnackbarDuration.Short
                    )
                }
                LogData(
                    path = "error/Error ${Instant.now()}.log"
                ).apply {
                    append("$e\n${e.stackTrace}")
                    save(context)
                }
            }
            outputStream?.close()
        }
        else {
            scope.launch {
                snackbarHostState.showSnackbar("Failed to create file")
            }
        }
    }

    val launcherImport = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            var inputStream: InputStream? = null
            try {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Importing backup...",
                        duration = SnackbarDuration.Short
                    )
                }

                data?.data?.let { uri ->

                    inputStream = context.contentResolver.openInputStream(uri)

                    val tempFile = File.createTempFile("import", ".zip.enc")
                    inputStream?.use { it.copyTo(FileOutputStream(tempFile)) }

                    // ACTIVATE THIS IF DECRYPTION SYSTEM IS READY
                    val PVqpACR05YRZx9ni = MessageDigest
                        .getInstance("SHA-512")
                        .digest(wAt2J7GmpkeWSRad.toByteArray())

                    val encryptedBytes = tempFile.readBytes()
                    val decryptedBytes = mutableListOf<Byte>()

                    for (indexed in encryptedBytes.withIndex()) {
                        decryptedBytes.add((indexed.value - PVqpACR05YRZx9ni[indexed.index % PVqpACR05YRZx9ni.size] + 256).toByte())
                    }

                    val decryptedFile = File.createTempFile("import", ".zip")
                    decryptedFile.writeBytes(decryptedBytes.toByteArray())

                    ZipInputStream(
                        BufferedInputStream(
                            FileInputStream(
                                decryptedFile
                            )
                        )
                    ).use { zis ->
                        var entry: ZipEntry?
                        val outputDirectory = context.filesDir
                        if (outputDirectory.exists() && outputDirectory.isDirectory)
                            outputDirectory.listFiles()?.forEach {
                                if (it.isDirectory) it.deleteRecursively()
                                else if (it.isFile) it.delete()
                            }
                        outputDirectory.mkdirs()

                        while (zis.nextEntry.also { entry = it } != null) {
                            val outputFile = File(outputDirectory, entry!!.name)
                            if (entry!!.isDirectory) {
                                outputFile.mkdirs()
                            } else {
                                outputFile.outputStream().use { fos ->
                                    zis.copyTo(fos)
                                }
                            }
                        }
                    }

                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Import success",
                            duration = SnackbarDuration.Short
                        )
                    }

                } ?: scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "No file selected",
                        duration = SnackbarDuration.Short
                    )
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "An error occurred during import process",
                        duration = SnackbarDuration.Short
                    )
                }
                LogData(
                    path = "error/Error ${Instant.now()}.log"
                ).apply {
                    append("$e\n${e.stackTrace}")
                    save(context)
                }
            }
            inputStream?.close()
        } else {
            scope.launch {
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
            ListItem(
                leadingContent = {
                    Icon(painterResource(R.drawable.ic_cloud_upload), "Backup")
                },
                headlineContent = {
                    Text("Data Backup")
                },
                supportingContent = {
                    Text("Backup all data in 'history' directory.")
                },
                trailingContent = {
                    Button(
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
                    Text("Import backup data to 'history' directory,")
                },
                trailingContent = {
                    Button(onClick = { handleImport(launcherImport) }) {
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
        type = "application/octet-stream" // Specify the file type (e.g., text/plain, application/pdf, etc.)
        putExtra(Intent.EXTRA_TITLE, DateTimeFormatter.ofPattern("'Backup' yyyyMMdd-HHmmssSSSS'.enc'").format(now)) // Suggested filename
    }
    launcher.launch(intent)
}

fun handleImport(launcher: ActivityResultLauncher<Intent>) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "application/octet-stream" // Specify the file type (e.g., text/plain, application/pdf, etc.)
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