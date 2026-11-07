package id.my.nanclouder.nanhistory.core.common.record

import android.content.Context

interface ActivityRecognitionManager {
    fun register(context: Context)
    fun unregister(context: Context)
}

object ActivityRecognitionBridge {
    private var manager: ActivityRecognitionManager? = null

    fun registerManager(manager: ActivityRecognitionManager) {
        this.manager = manager
    }

    fun register(context: Context) {
        manager?.register(context)
    }

    fun unregister(context: Context) {
        manager?.unregister(context)
    }
}
