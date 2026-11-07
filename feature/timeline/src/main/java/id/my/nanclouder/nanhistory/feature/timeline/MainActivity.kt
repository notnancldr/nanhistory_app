package id.my.nanclouder.nanhistory.feature.timeline

import android.Manifest
import android.os.Build
import android.os.Bundle
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
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.core.common.dataprocess.DataProcessBridge
import id.my.nanclouder.nanhistory.core.common.dataprocess.DataProcessConstants
import id.my.nanclouder.nanhistory.core.common.dataprocess.DataProcessState
import id.my.nanclouder.nanhistory.core.common.worker.WorkSchedulers
import id.my.nanclouder.nanhistory.core.ui.NewUIComponentActivity
import id.my.nanclouder.nanhistory.core.ui.dialog.DataProcessDialog
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import id.my.nanclouder.nanhistory.feature.timeline.ui.main.MainView
import org.osmdroid.config.Configuration
import org.osmdroid.library.BuildConfig
import java.time.Instant

enum class NanHistoryPages {
    Events, Favorite, Tags, Search
}

enum class ListFilters {
    Recent, Favorite, All/*, Search*/
}

class MainActivity : NewUIComponentActivity() {
    private var update: (() -> Unit) = { }
    private var startTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WorkSchedulers.schedule(this)
        Config.prepareCache(this)

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

            val safeToUse by DataProcessState.ServiceState.safeToUse.collectAsState()

            val dataProcessType by DataProcessState.ServiceState.operationType.collectAsState()
            val dataProcessIsRunning by DataProcessState.ServiceState.isRunning.collectAsState()

            LaunchedEffect(safeToUse, dataProcessIsRunning) {
                if (
                    !DataProcessState.checkMigration(this@MainActivity) &&
                    dataProcessType == DataProcessConstants.OPERATION_UNKNOWN &&
                    !dataProcessIsRunning
                ) {
                    migrationNeeded = true
                    showMainView = false

                    DataProcessBridge.startMigration(this@MainActivity)
                }
                else if (
                    (
                        dataProcessType == DataProcessConstants.OPERATION_IMPORT ||
                        dataProcessType == DataProcessConstants.OPERATION_MIGRATE
                    ) && dataProcessIsRunning && !safeToUse
                ) {
                    showMainView = false
                }
                else {
                    showMainView = true
                }

                if (migrationNeeded && !showMainView && (safeToUse || !dataProcessIsRunning)) {
                    migrationNeeded = false
                    showMainView = true
                }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
            ), 100)
        }
        else {
            requestPermissions(arrayOf(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ), 100)
        }
        requestPermissions(arrayOf(
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        ), 101)
        requestPermissions(arrayOf(
            Manifest.permission.WAKE_LOCK,
        ), 102)
    }

    override fun onResume() {
        super.onResume()

        // Refresh config cache
        Config.prepareCache(this)
    }
}

@Preview(showBackground = true)
@Composable
fun MainContentPreview() {
    NanHistoryTheme {
        MainView()
    }
}