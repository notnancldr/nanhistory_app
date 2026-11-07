package id.my.nanclouder.nanhistory.feature.tag

import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.model.HistoryTag
import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import id.my.nanclouder.nanhistory.domain.repository.TagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TagDetailViewModelTest {

    private class TestTagRepository(private val tag: HistoryTag?) : TagRepository {
        override fun observeAllTags(): Flow<List<HistoryTag>> = flowOf(emptyList())
        override fun observeTagById(tagId: String): Flow<HistoryTag?> = flowOf(tag)
        override fun observeEventsForTag(tagId: String): Flow<List<HistoryEvent>> = flowOf(emptyList())
        override suspend fun insertTag(tag: HistoryTag) {}
        override suspend fun updateTag(tag: HistoryTag) {}
        override suspend fun deleteTag(tag: HistoryTag) {}
        override suspend fun getTagById(tagId: String): HistoryTag? = tag
    }

    private class TestEventRepository : EventRepository {
        val trashedIds = mutableListOf<String>()

        override fun getTrashedEvents(): Flow<List<HistoryEvent>> = flowOf(emptyList())
        override suspend fun restoreEvents(ids: List<String>) {}
        override suspend fun permanentlyDeleteEvents(events: List<HistoryEvent>) {}
        override suspend fun getEventById(id: String): HistoryEvent? = null
        override suspend fun moveToTrash(eventIds: List<String>) {
            trashedIds.addAll(eventIds)
        }
    }

    private lateinit var tagRepo: TestTagRepository
    private lateinit var eventRepo: TestEventRepository
    private lateinit var viewModel: TagDetailViewModel

    @Before
    fun setup() {
        val sampleTag = HistoryTag(id = "tag-1", name = "Work")
        tagRepo = TestTagRepository(sampleTag)
        eventRepo = TestEventRepository()
        viewModel = TagDetailViewModel(
            tagId = "tag-1",
            tagRepository = tagRepo,
            eventRepository = eventRepo,
            dispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun moveToTrash_delegates_to_event_repository_and_invokes_callback() {
        var completed = false

        viewModel.moveToTrash(listOf("e1", "e2")) {
            completed = true
        }

        assertEquals(listOf("e1", "e2"), eventRepo.trashedIds)
        assertTrue("Callback should be invoked", completed)
    }

    @Test
    fun moveToTrash_with_empty_list_does_not_call_repository() {
        var completed = false

        viewModel.moveToTrash(emptyList()) {
            completed = true
        }

        assertEquals(0, eventRepo.trashedIds.size)
    }
}
