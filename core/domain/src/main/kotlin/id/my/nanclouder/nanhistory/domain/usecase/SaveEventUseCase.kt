package id.my.nanclouder.nanhistory.domain.usecase

import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.repository.EventRepository

/**
 * Saves a new or modified event via [EventRepository].
 */
class SaveEventUseCase(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(event: HistoryEvent, isNew: Boolean) {
        eventRepository.saveEvent(event, isNew)
    }
}
