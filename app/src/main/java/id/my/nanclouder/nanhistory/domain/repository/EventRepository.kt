package id.my.nanclouder.nanhistory.domain.repository

import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.time.ZonedDateTime

/**
 * Domain port for reading and mutating history events.
 */
interface EventRepository {
    fun getTrashedEvents(): Flow<List<HistoryEvent>>
    fun observeEventById(id: String): Flow<HistoryEvent?> = emptyFlow()
    suspend fun restoreEvents(ids: List<String>) {}
    suspend fun permanentlyDeleteEvents(events: List<HistoryEvent>) {}
    suspend fun getEventById(id: String): HistoryEvent? = null
    suspend fun moveToTrash(eventIds: List<String>) {}
    suspend fun saveEvent(event: HistoryEvent, isNew: Boolean) {}
    suspend fun getEventSiblingTimes(previousEventOf: String?, nextEventOf: String?): Pair<ZonedDateTime, ZonedDateTime>? = null
}

