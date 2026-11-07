package id.my.nanclouder.nanhistory.feature.maplocation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.core.common.di.CoreContainerProvider
import id.my.nanclouder.nanhistory.domain.model.generateEventId
import id.my.nanclouder.nanhistory.feature.maplocation.oldUi.EventLocationView_Old
import id.my.nanclouder.nanhistory.feature.maplocation.ui.EventLocationScreen
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme

class EventLocationActivity : ComponentActivity() {
    private var update: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val eventId = intent.getStringExtra("eventId") ?: generateEventId()
        val startAsCutMode = intent.getBooleanExtra("cutMode", false)

        setContent {
            var setUpdate by remember { mutableStateOf(false) }
            update = { setUpdate = !setUpdate }
            NanHistoryTheme {
                key(setUpdate) {
                    EventLocationView(eventId, startAsCutMode, onFinish = { finish() })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        update()
    }
}

@Composable
fun EventLocationView(
    eventId: String,
    startAsCutMode: Boolean = false,
    onFinish: () -> Unit = {}
) {
    val useOldUi = Config.appearanceOldUi.getCache()
    if (!useOldUi) {
        val context = LocalContext.current
        val container = (context.applicationContext as CoreContainerProvider).coreContainer
        val viewModel = remember(eventId) {
            EventLocationViewModel(
                eventRepository = container.eventRepository,
                locationRepository = container.locationRepository,
                eventId = eventId,
                startAsCutMode = startAsCutMode
            )
        }
        EventLocationScreen(
            viewModel = viewModel,
            startAsCutMode = startAsCutMode,
            onFinish = onFinish
        )
    } else {
        EventLocationView_Old(
            eventId = eventId,
            startAsCutMode = startAsCutMode,
            onFinish = onFinish
        )
    }
}
