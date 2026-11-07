package id.my.nanclouder.nanhistory.core.common.state

import id.my.nanclouder.nanhistory.core.common.record.RecordStatus
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Global recording state observable across modules without coupling to RecordService.
 */
object RecordingState {
    val activeEventId = MutableStateFlow<String?>(null)
    val eventId get() = activeEventId
    val isRecording = MutableStateFlow(false)
    val isRunning = MutableStateFlow(false)
    val status = MutableStateFlow(RecordStatus.READY)
}
