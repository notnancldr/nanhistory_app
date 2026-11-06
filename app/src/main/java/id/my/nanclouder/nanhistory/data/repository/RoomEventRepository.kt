package id.my.nanclouder.nanhistory.data.repository

import id.my.nanclouder.nanhistory.db.AppDao
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
}
