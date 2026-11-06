package id.my.nanclouder.nanhistory.domain.usecase

import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.repository.AttachmentStorageRepository
import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import java.time.Instant

/**
 * Permanently deletes trash items whose 30-day retention deadline has expired.
 * Deletes associated audio and location attachments and removes records from the database.
 */
class PurgeExpiredTrashUseCase(
    private val eventRepository: EventRepository,
    private val attachmentStorageRepository: AttachmentStorageRepository
) {
    suspend operator fun invoke(nowEpochMilli: Long = Instant.now().toEpochMilli()): List<HistoryEvent> {
        val expired = eventRepository.getExpiredTrashEvents(nowEpochMilli)
        if (expired.isNotEmpty()) {
            attachmentStorageRepository.deleteAttachments(expired)
            eventRepository.permanentlyDeleteEvents(expired)
        }
        return expired
    }
}
