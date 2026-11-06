package id.my.nanclouder.nanhistory.domain.repository

import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.model.HistoryTag
import kotlinx.coroutines.flow.Flow

/**
 * Domain port for reading and mutating history tags.
 */
interface TagRepository {
    fun observeAllTags(): Flow<List<HistoryTag>>
    fun observeTagById(tagId: String): Flow<HistoryTag?>
    fun observeEventsForTag(tagId: String): Flow<List<HistoryEvent>>
    suspend fun insertTag(tag: HistoryTag)
    suspend fun updateTag(tag: HistoryTag)
    suspend fun deleteTag(tag: HistoryTag)
    suspend fun deleteTags(tags: List<HistoryTag>) {}
    suspend fun getTagById(tagId: String): HistoryTag?
}
