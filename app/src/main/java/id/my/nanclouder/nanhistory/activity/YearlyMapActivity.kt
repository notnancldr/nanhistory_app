package id.my.nanclouder.nanhistory.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import id.my.nanclouder.nanhistory.ui.map.YearlyMapView
import id.my.nanclouder.nanhistory.ui.theme.NanHistoryTheme
import id.my.nanclouder.nanhistory.utils.NewUIComponentActivity

class YearlyMapActivity : NewUIComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NanHistoryTheme {
                Scaffold { paddingValues ->
                    YearlyMapView(
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}
