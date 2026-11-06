package id.my.nanclouder.nanhistory.data.repository

import id.my.nanclouder.nanhistory.db.AppDao
import id.my.nanclouder.nanhistory.db.DayEntity
import id.my.nanclouder.nanhistory.db.toEventEntity
import id.my.nanclouder.nanhistory.db.toHistoryEvent
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime

/**
 * Room-backed implementation of [EventRepository].
 * Uses a dynamic [daoProvider] to ensure callers obtain a valid DAO instance
 * even across database reopen or backup restoration events.
 */
class RoomEventRepository(
    private val daoProvider: () -> AppDao,
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

    override suspend fun moveToTrash(eventIds: List<String>) = withContext(ioDispatcher) {
        if (eventIds.isNotEmpty()) {
            daoProvider().softDeleteEvents(eventIds)
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
}
