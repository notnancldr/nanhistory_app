package id.my.nanclouder.nanhistory.data.repository

import id.my.nanclouder.nanhistory.db.AppDao
import id.my.nanclouder.nanhistory.db.toHistoryEvent
import id.my.nanclouder.nanhistory.db.toHistoryTag
import id.my.nanclouder.nanhistory.db.toTagEntity
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.model.HistoryTag
import id.my.nanclouder.nanhistory.domain.repository.TagRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Room-backed implementation of [TagRepository].
 * Uses a dynamic [daoProvider] to ensure callers obtain a valid DAO instance
 * even across database reset or backup restoration.
 */
class RoomTagRepository(
    private val daoProvider: () -> AppDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TagRepository {

    override fun observeAllTags(): Flow<List<HistoryTag>> {
        return daoProvider().getAllTags()
            .map { list -> list.map { it.toHistoryTag() } }
            .flowOn(ioDispatcher)
    }

    override fun observeTagById(tagId: String): Flow<HistoryTag?> {
        return daoProvider().getTagById(tagId)
            .map { it?.toHistoryTag() }
            .flowOn(ioDispatcher)
    }

    override fun observeEventsForTag(tagId: String): Flow<List<HistoryEvent>> {
        return daoProvider().getEventsByTagIds(listOf(tagId))
            .map { list -> list.map { it.toHistoryEvent() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun insertTag(tag: HistoryTag) = withContext(ioDispatcher) {
        daoProvider().insertTag(tag.toTagEntity())
    }

    override suspend fun updateTag(tag: HistoryTag) = withContext(ioDispatcher) {
        daoProvider().updateTag(tag.toTagEntity())
    }

    override suspend fun deleteTag(tag: HistoryTag) = withContext(ioDispatcher) {
        daoProvider().deleteTag(tag.toTagEntity())
    }

    override suspend fun getTagById(tagId: String): HistoryTag? = withContext(ioDispatcher) {
        daoProvider().getTagById(tagId).firstOrNull()?.toHistoryTag()
    }
}
