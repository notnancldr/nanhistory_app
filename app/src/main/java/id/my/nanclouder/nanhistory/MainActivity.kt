package id.my.nanclouder.nanhistory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import id.my.nanclouder.nanhistory.lib.history.migrateLocationData
import id.my.nanclouder.nanhistory.ui.main.MainView
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import org.osmdroid.config.Configuration
import org.osmdroid.library.BuildConfig
import java.time.Instant

enum class NanHistoryPages {
    Events, Tags, Search
}

enum class ListFilters {
    Recent, Favorite, All/*, Search*/
}

class MainActivity : ComponentActivity() {
    private var update: (() -> Unit) = { }
    private var startTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            NanHistoryTheme {
                key(setUpdate) {
                    MainView()
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
}

@Preview(showBackground = true)
@Composable
fun MainContentPreview() {
    NanHistoryTheme {
        MainView()
    }
}