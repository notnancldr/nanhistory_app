package id.my.nanclouder.nanhistory.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme

class AudioListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NanHistoryTheme {
                FileListView("Audio Records", "files/audio", true)
            }
        }
    }
}


