package id.my.nanclouder.nanhistory.core.common.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global recording state observable across modules without coupling to RecordService.
 */
object RecordingState {
    val activeEventId = MutableStateFlow<String?>(null)
    val isRecording = MutableStateFlow(false)
}
