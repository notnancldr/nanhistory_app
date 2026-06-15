package id.my.nanclouder.nanhistory.activity.eventDetail

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import id.my.nanclouder.nanhistory.activity.eventDetail.ui.DetailContent
import id.my.nanclouder.nanhistory.activity.eventDetail.oldUi.OldDetailContent
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import id.my.nanclouder.nanhistory.utils.history.generateEventId

class EventDetailActivity : ComponentActivity() {
    var update: () -> Unit = {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val eventId = intent.getStringExtra("eventId") ?: generateEventId()
        val path = intent.getStringExtra("path") ?: "NULL!"
        setContent {
            NanHistoryTheme {
                id.my.nanclouder.nanhistory.activity.eventDetail.DetailContent(eventId, path)
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

@Preview(showBackground = true)
@Composable
fun DetailContentPreview() {
    NanHistoryTheme {
        DetailContent("", "")
    }
}