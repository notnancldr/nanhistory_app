package id.my.nanclouder.nanhistory.feature.eventdetail

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.domain.model.generateEventId
import id.my.nanclouder.nanhistory.feature.eventdetail.oldUi.OldDetailContent
import id.my.nanclouder.nanhistory.feature.eventdetail.ui.DetailContent
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme

class EventDetailActivity : ComponentActivity() {
    var update: () -> Unit = {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val eventId = intent.getStringExtra("eventId") ?: generateEventId()
        val path = intent.getStringExtra("path") ?: "NULL!"
        setContent {
            NanHistoryTheme {
                DetailContent(eventId, path)
            }
        }
    }
}

fun Context.getActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

@Composable
fun DetailContent(eventId: String, path: String) {
    val useOldUi = Config.appearanceOldUi.getCache()
    if (!useOldUi) DetailContent(eventId, path)
    else OldDetailContent(eventId, path)
}
