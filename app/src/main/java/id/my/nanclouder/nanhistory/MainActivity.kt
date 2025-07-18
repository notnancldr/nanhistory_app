package id.my.nanclouder.nanhistory

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import id.my.nanclouder.nanhistory.service.DataProcessService
import id.my.nanclouder.nanhistory.ui.DataProcessDialog
import id.my.nanclouder.nanhistory.ui.main.MainView
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import id.my.nanclouder.nanhistory.worker.AutoDeleteWorker
import org.osmdroid.config.Configuration
import org.osmdroid.library.BuildConfig
import java.time.Instant
import java.util.concurrent.TimeUnit

enum class NanHistoryPages {
    Events, Favorite, Tags, Search
}

enum class ListFilters {
    Recent, Favorite, All/*, Search*/
}

class MainActivity : ComponentActivity() {
    private var update: (() -> Unit) = { }
    private var startTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scheduleAutoDelete()

        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        startTime = Instant.now().epochSecond

        Configuration.getInstance().userAgentValue = BuildConfig.LIBRARY_PACKAGE_NAME

        setContent {
            var setUpdate by remember { mutableStateOf(false) }
            update = {
                setUpdate = !setUpdate
            }

            var showMainView by remember { mutableStateOf(false) }
            var migrationNeeded by remember { mutableStateOf(false) }

            val importState by DataProcessService.ImportState.stage.collectAsState()
            val dataProcessType by DataProcessService.ServiceState.operationType.collectAsState()
            val dataProcessIsRunning by DataProcessService.ServiceState.isRunning.collectAsState()

            LaunchedEffect(dataProcessIsRunning) {
                if (
                    !DataProcessService.checkMigration(this@MainActivity) &&
                    dataProcessType == DataProcessService.OPERATION_UNKNOWN &&
                    !dataProcessIsRunning
                ) {
                    migrationNeeded = true
                    showMainView = false

                    val intent = Intent(this@MainActivity, DataProcessService::class.java)
                    intent.putExtra(DataProcessService.OPERATION_TYPE_EXTRA, DataProcessService.OPERATION_MIGRATE)
                    startService(intent)
                }
                else if (
                    (
                        dataProcessType == DataProcessService.OPERATION_IMPORT ||
                        dataProcessType == DataProcessService.OPERATION_MIGRATE
                    ) && dataProcessIsRunning
                ) {
                    showMainView = false
                }
                else {
                    showMainView = true
                }
            }

            if (migrationNeeded && !showMainView && !dataProcessIsRunning) {
                migrationNeeded = false
                showMainView = true
            }

            NanHistoryTheme {
                if (showMainView) key(setUpdate) {
                    MainView()
                }
                else {
                    Scaffold { padding ->
                        Column(Modifier.padding(padding)) { DataProcessDialog() }
                    }
                }
            }
        }
        requestPermissions(arrayOf(
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ), 100)
        requestPermissions(arrayOf(
            android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        ), 101)
        requestPermissions(arrayOf(
            android.Manifest.permission.WAKE_LOCK,
        ), 102)
    }

    private fun scheduleAutoDelete() {
        val request = PeriodicWorkRequestBuilder<AutoDeleteWorker>(1, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "AutoDeleteWorker",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
    }
}

@Preview(showBackground = true)
@Composable
fun MainContentPreview() {
    NanHistoryTheme {
        MainView()
    }
}