package id.my.nanclouder.nanhistory.activity.shareMap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import id.my.nanclouder.nanhistory.activity.shareMap.ui.ShareMapScreen
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.state.ShareMapViewModel
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme

/**
 * Dedicated activity for the full-screen ShareMap editing studio.
 */
class ShareMapActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val eventId = intent.getStringExtra("eventId") ?: ""

        setContent {
            val context = LocalContext.current
            val db = remember { AppDatabase.getInstance(context.applicationContext) }
            val viewModel = remember(eventId) {
                ShareMapViewModel(
                    eventId = eventId,
                    dao = db.appDao(),
                    context = context.applicationContext
                )
            }

            NanHistoryTheme {
                ShareMapScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}
