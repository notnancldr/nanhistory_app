package id.my.nanclouder.nanhistory.data.repository

import android.content.Context
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.db.AppDao
import id.my.nanclouder.nanhistory.db.DayEntity
import id.my.nanclouder.nanhistory.db.EventWithTags
import id.my.nanclouder.nanhistory.db.toEventEntity
import id.my.nanclouder.nanhistory.db.toHistoryDay
import id.my.nanclouder.nanhistory.db.toHistoryEvent
import id.my.nanclouder.nanhistory.domain.model.EventSelectMode
import id.my.nanclouder.nanhistory.domain.model.HistoryDay
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Room-backed implementation of [EventRepository].
 * Uses a dynamic [daoProvider] to ensure callers obtain a valid DAO instance
 * even across database reopen or backup restoration events.
 */
class RoomEventRepository(
    private val daoProvider: () -> AppDao,
    private val context: Context? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : EventRepository {

    override fun getTrashedEvents(): Flow<List<HistoryEvent>> {
        return daoProvider().getDeletedEventsFlow()
            .map { list -> list.map { it.toHistoryEvent() } }
            .flowOn(ioDispatcher)
    }

    override fun observeEventById(id: String): Flow<HistoryEvent?> {
        return daoProvider().getEventFlowById(id)
            .map { it?.toHistoryEvent() }
            .flowOn(ioDispatcher)
    }

    override fun observeEventsByDay(date: LocalDate): Flow<List<HistoryEvent>> {
        return daoProvider().getEventsByDay(date)
            .map { list -> list.map { it.toHistoryEvent() } }
            .flowOn(ioDispatcher)
    }

    override fun observeEventsInRange(
        from: LocalDate,
        until: LocalDate,
        mode: EventSelectMode
    ): Flow<List<HistoryEvent>> {
        val dao = daoProvider()
        return when (mode) {
            EventSelectMode.FavoriteEvent -> dao.getFavoriteEvents()
            EventSelectMode.Deleted -> dao.getDeletedEventsFlow()
            else -> dao.getEventsInRange(from.toString(), until.toString())
        }.map { list -> list.map { it.toHistoryEvent() } }
            .flowOn(ioDispatcher)
    }

    override fun observeDaysInRange(
        from: LocalDate,
        until: LocalDate,
        favoriteOnly: Boolean
    ): Flow<List<HistoryDay>> {
        val dao = daoProvider()
        return if (!favoriteOnly) {
            dao.getDaysInRange(from, until)
        } else {
            dao.getFavoriteDays()
        }.map { list -> list.map { it.toHistoryDay() } }
            .flowOn(ioDispatcher)
    }

    override fun observeEventsByTagIds(tagIds: List<String>): Flow<List<HistoryEvent>> {
        return daoProvider().getEventsByTagIds(tagIds)
            .map { list -> list.map { it.toHistoryEvent() } }
            .flowOn(ioDispatcher)
    }

    override fun searchEvents(
        query: String,
        tagIds: List<String>,
        matchWholeWord: Boolean
    ): Flow<List<HistoryEvent>> {
        val dao = daoProvider()
        val flow: Flow<List<EventWithTags>> = if (tagIds.isNotEmpty()) {
            dao.searchEventsWithTagIds(query, tagIds)
        } else if (matchWholeWord) {
            dao.searchEventsMatchWholeWord(query)
        } else if (query.isNotBlank()) {
            dao.searchEvents(query)
        } else {
            flowOf(emptyList())
        }
        return flow.map { list -> list.map { it.toHistoryEvent() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun restoreEvents(ids: List<String>) = withContext(ioDispatcher) {
        if (ids.isNotEmpty()) {
            daoProvider().restoreEvents(ids)
        }
    }

    override suspend fun permanentlyDeleteEvents(events: List<HistoryEvent>) = withContext(ioDispatcher) {
        if (events.isNotEmpty()) {
            daoProvider().deleteEvents(events.map { it.toEventEntity() })
        }
    }

    override suspend fun getEventById(id: String): HistoryEvent? = withContext(ioDispatcher) {
        daoProvider().getEventById(id)?.toHistoryEvent()
    }

    override suspend fun getEventsInDateRange(
        startDate: String?,
        endDate: String?
    ): List<HistoryEvent> = withContext(ioDispatcher) {
        daoProvider().getEventsInDateRange(startDate, endDate).map { it.toHistoryEvent() }
    }

    override suspend fun moveToTrash(eventIds: List<String>) = withContext(ioDispatcher) {
        if (eventIds.isNotEmpty()) {
            val dao = daoProvider()
            val delete1hour = context?.let {
                Config.developer1hourAutoDelete.get(it) && Config.developerModeEnabled.get(it)
            } ?: false
            if (delete1hour) {
                dao.softDeleteEvents(eventIds, Instant.now().plusSeconds(3600).toEpochMilli())
            } else {
                dao.softDeleteEvents(eventIds)
            }
        }
    }

    override suspend fun saveEvent(event: HistoryEvent, isNew: Boolean) = withContext(ioDispatcher) {
        val dao = daoProvider()
        val date = event.time.toLocalDate()
        if (dao.getDayByDate(date) == null) {
            dao.insertDay(
                DayEntity(
                    date = date,
                    description = "",
                    favorite = false
                )
            )
        }
        if (isNew) {
            dao.insertEvent(event.toEventEntity())
        } else {
            dao.updateEvent(event.toEventEntity())
        }
    }

    override suspend fun updateEvent(event: HistoryEvent) = withContext(ioDispatcher) {
        daoProvider().updateEvent(event.toEventEntity())
    }

    override suspend fun toggleFavoriteEvent(eventId: String) = withContext(ioDispatcher) {
        daoProvider().toggleFavoriteEvent(eventId)
    }

    override suspend fun toggleFavoriteDay(date: LocalDate) = withContext(ioDispatcher) {
        daoProvider().toggleFavoriteDay(date)
    }

    override suspend fun getEventSiblingTimes(
        previousEventOf: String?,
        nextEventOf: String?
    ): Pair<ZonedDateTime, ZonedDateTime>? = withContext(ioDispatcher) {
        val dao = daoProvider()
        var firstSiblingTime: ZonedDateTime? = null
        var secondSiblingTime: ZonedDateTime? = null

        if (previousEventOf != null) {
            val firstSibling = dao.getPreviousEvent(previousEventOf)
            firstSiblingTime = firstSibling?.end ?: firstSibling?.time ?: ZonedDateTime.now()
            val secondSibling = dao.getEventById(previousEventOf)
            secondSiblingTime = secondSibling?.event?.time ?: ZonedDateTime.now()
        }
        if (nextEventOf != null) {
            val firstSibling = dao.getNextEvent(nextEventOf)
            firstSiblingTime = firstSibling?.time ?: ZonedDateTime.now()
            val secondSibling = dao.getEventById(nextEventOf)
            secondSiblingTime = secondSibling?.event?.end ?: secondSibling?.event?.time ?: ZonedDateTime.now()
        }

        if (firstSiblingTime != null && secondSiblingTime != null) {
            val start = if (firstSiblingTime < secondSiblingTime) firstSiblingTime else secondSiblingTime
            val end = if (firstSiblingTime > secondSiblingTime) firstSiblingTime else secondSiblingTime
            start to end
        } else null
    }

    override suspend fun getExpiredTrashEvents(nowEpochMilli: Long): List<HistoryEvent> {
        return withContext(ioDispatcher) {
            daoProvider().getDeletedEvents()
                .filter { (it.event.deletePermanently ?: Long.MAX_VALUE) <= nowEpochMilli }
                .map { it.toHistoryEvent() }
        }
    }

    override suspend fun getEventsAfter(lastEventTimeEpochMilli: Long): List<HistoryEvent> {
        return withContext(ioDispatcher) {
            daoProvider().getEventsAfter(lastEventTimeEpochMilli).map { it.toHistoryEvent() }
        }
    }
}
