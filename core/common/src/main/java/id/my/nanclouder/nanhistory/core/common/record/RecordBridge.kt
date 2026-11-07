package id.my.nanclouder.nanhistory.core.common.record

import android.content.Context

interface RecordController {
    fun startRecording(context: Context)
    fun stopRecording(context: Context)
    fun toggleRecording(context: Context)
    fun restartRecording(context: Context)
}

object RecordBridge {
    private var controller: RecordController? = null

    fun register(controller: RecordController) {
        this.controller = controller
    }

    fun startRecording(context: Context) {
        controller?.startRecording(context)
    }

    fun stopRecording(context: Context) {
        controller?.stopRecording(context)
    }

    fun toggleRecording(context: Context) {
        controller?.toggleRecording(context)
    }

    fun restartRecording(context: Context) {
        controller?.restartRecording(context)
    }
}
