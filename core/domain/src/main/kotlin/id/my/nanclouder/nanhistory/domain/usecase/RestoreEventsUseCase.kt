package id.my.nanclouder.nanhistory.domain.usecase

import id.my.nanclouder.nanhistory.domain.repository.EventRepository

/**
 * Restores soft-deleted events from the trash.
 */
class RestoreEventsUseCase(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventIds: List<String>) {
        if (eventIds.isEmpty()) return
        eventRepository.restoreEvents(eventIds)
    }
}
