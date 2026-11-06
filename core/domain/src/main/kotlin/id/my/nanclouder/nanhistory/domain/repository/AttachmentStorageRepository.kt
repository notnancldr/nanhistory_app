package id.my.nanclouder.nanhistory.domain.repository

import id.my.nanclouder.nanhistory.domain.model.HistoryEvent

/**
 * Domain port for managing on-disk attachments (audio recordings, location files)
 * without depending directly on Android storage APIs.
 */
interface AttachmentStorageRepository {
    suspend fun deleteAttachments(event: HistoryEvent)
    suspend fun deleteAttachments(events: List<HistoryEvent>) {
        events.forEach { deleteAttachments(it) }
    }
}
