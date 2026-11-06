package id.my.nanclouder.nanhistory.domain.usecase

import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.repository.AttachmentStorageRepository
import id.my.nanclouder.nanhistory.domain.repository.EventRepository

/**
 * Permanently purges events from the database and cleans up their associated
 * physical files (audio recordings and location logs) via [AttachmentStorageRepository].
 */
class PermanentlyDeleteEventsUseCase(
    private val eventRepository: EventRepository,
    private val attachmentStorageRepository: AttachmentStorageRepository
) {
    suspend operator fun invoke(
        events: List<HistoryEvent>,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ) {
        if (events.isEmpty()) return
        val total = events.size
        for ((index, event) in events.withIndex()) {
            attachmentStorageRepository.deleteAttachments(event)
            eventRepository.permanentlyDeleteEvents(listOf(event))
            onProgress?.invoke(index + 1, total)
        }
    }
}
