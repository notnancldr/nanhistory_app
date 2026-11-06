package id.my.nanclouder.nanhistory.domain.repository

import id.my.nanclouder.nanhistory.domain.model.EventSelectMode
import id.my.nanclouder.nanhistory.domain.model.HistoryDay
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Domain port for reading and mutating history events and days.
 */
interface EventRepository {
    fun getTrashedEvents(): Flow<List<HistoryEvent>>
    fun observeEventById(id: String): Flow<HistoryEvent?> = emptyFlow()
    fun observeEventsByDay(date: LocalDate): Flow<List<HistoryEvent>> = emptyFlow()
    fun observeEventsInRange(
        from: LocalDate,
        until: LocalDate = LocalDate.now(),
        mode: EventSelectMode = EventSelectMode.Default
    ): Flow<List<HistoryEvent>> = emptyFlow()
    fun observeDaysInRange(
        from: LocalDate,
        until: LocalDate = LocalDate.now(),
        favoriteOnly: Boolean = false
    ): Flow<List<HistoryDay>> = emptyFlow()
    fun observeEventsByTagIds(tagIds: List<String>): Flow<List<HistoryEvent>> = emptyFlow()
    fun searchEvents(
        query: String,
        tagIds: List<String> = emptyList(),
        matchWholeWord: Boolean = false
    ): Flow<List<HistoryEvent>> = emptyFlow()

    suspend fun restoreEvents(ids: List<String>) {}
    suspend fun permanentlyDeleteEvents(events: List<HistoryEvent>) {}
    suspend fun getEventById(id: String): HistoryEvent? = null
    suspend fun getEventsInDateRange(startDate: String?, endDate: String?): List<HistoryEvent> = emptyList()
    suspend fun moveToTrash(eventIds: List<String>) {}
    suspend fun saveEvent(event: HistoryEvent, isNew: Boolean) {}
    suspend fun updateEvent(event: HistoryEvent) {}
    suspend fun toggleFavoriteEvent(eventId: String) {}
    suspend fun toggleFavoriteDay(date: LocalDate) {}
    suspend fun getEventSiblingTimes(previousEventOf: String?, nextEventOf: String?): Pair<ZonedDateTime, ZonedDateTime>? = null
}

