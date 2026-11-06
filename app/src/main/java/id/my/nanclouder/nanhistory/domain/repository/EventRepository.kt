package id.my.nanclouder.nanhistory.domain.repository

import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import kotlinx.coroutines.flow.Flow

/**
 * Domain port for reading and mutating history events.
 */
interface EventRepository {
    fun getTrashedEvents(): Flow<List<HistoryEvent>>
    suspend fun restoreEvents(ids: List<String>)
    suspend fun permanentlyDeleteEvents(events: List<HistoryEvent>)
    suspend fun getEventById(id: String): HistoryEvent?
    suspend fun moveToTrash(eventIds: List<String>)
}
