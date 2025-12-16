package id.my.nanclouder.nanhistory.utils

import android.os.Bundle
import androidx.activity.ComponentActivity
import id.my.nanclouder.nanhistory.config.Config

open class NewUIComponentActivity : ComponentActivity() {
    private var lastKnownNewUI = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lastKnownNewUI = Config.appearanceNewUI.get(this)
    }
    override fun onResume() {
        super.onResume()
        if (lastKnownNewUI != Config.appearanceNewUI.get(this)) {
            recreate()
        }
    }
}