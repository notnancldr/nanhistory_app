package id.my.nanclouder.nanhistory.feature.yearlymap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme

class YearlyMapActivity : ComponentActivity() {
    private var lastKnownNewUI = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lastKnownNewUI = Config.appearanceOldUi.get(this)
        setContent {
            NanHistoryTheme {
                YearlyMapView()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (lastKnownNewUI != Config.appearanceOldUi.get(this)) {
            recreate()
        }
    }
}
