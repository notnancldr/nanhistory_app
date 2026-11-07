package id.my.nanclouder.nanhistory.feature.timeline

import id.my.nanclouder.nanhistory.core.ui.list.EventListViewModel

import id.my.nanclouder.nanhistory.domain.model.EventPoint
import id.my.nanclouder.nanhistory.domain.model.EventSelectMode
import id.my.nanclouder.nanhistory.domain.model.HistoryDay
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.model.HistoryTag
import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import id.my.nanclouder.nanhistory.domain.repository.TagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime

class EventListViewModelTest {

    private class FakeEventRepository(
        private val eventsList: List<HistoryEvent> = emptyList(),
        private val daysList: List<HistoryDay> = emptyList()
    ) : EventRepository {
        var lastSearchQuery: String? = null
        var lastSearchTags: List<String>? = null
        var lastTagIds: List<String>? = null

        override fun getTrashedEvents(): Flow<List<HistoryEvent>> = flowOf(emptyList())

        override fun observeEventsInRange(
            from: LocalDate,
            until: LocalDate,
            mode: EventSelectMode
        ): Flow<List<HistoryEvent>> = flowOf(eventsList)

        override fun observeDaysInRange(
            from: LocalDate,
            until: LocalDate,
            favoriteOnly: Boolean
        ): Flow<List<HistoryDay>> = flowOf(daysList)

        override fun observeEventsByTagIds(tagIds: List<String>): Flow<List<HistoryEvent>> {
            lastTagIds = tagIds
            return flowOf(eventsList)
        }

        override fun searchEvents(
            query: String,
            tagIds: List<String>,
            matchWholeWord: Boolean
        ): Flow<List<HistoryEvent>> {
            lastSearchQuery = query
            lastSearchTags = tagIds
            return flowOf(eventsList)
        }
    }

    private class FakeTagRepository(
        private val tagsList: List<HistoryTag> = emptyList()
    ) : TagRepository {
        override fun observeAllTags(): Flow<List<HistoryTag>> = flowOf(tagsList)
        override fun observeTagById(tagId: String): Flow<HistoryTag?> = flowOf(null)
        override fun observeEventsForTag(tagId: String): Flow<List<HistoryEvent>> = flowOf(emptyList())
        override suspend fun insertTag(tag: HistoryTag) {}
        override suspend fun updateTag(tag: HistoryTag) {}
        override suspend fun deleteTag(tag: HistoryTag) {}
        override suspend fun getTagById(tagId: String): HistoryTag? = null
    }

    private val now = ZonedDateTime.now()
    private val today = LocalDate.now()

    @Test
    fun loadPopulatesEventsAndDays() = runBlocking {
        val sampleEvent = EventPoint(id = "evt-1", title = "Meeting", time = now)
        val sampleDay = HistoryDay(date = today, description = "A good day", favorite = false)

        val eventRepo = FakeEventRepository(eventsList = listOf(sampleEvent), daysList = listOf(sampleDay))
        val tagRepo = FakeTagRepository()

        val viewModel = EventListViewModel(
            context = null,
            from = today.minusDays(1),
            until = today,
            mode = EventSelectMode.Default,
            eventRepository = eventRepo,
            tagRepository = tagRepo,
            ioDispatcher = Dispatchers.Unconfined
        )

        assertEquals(1, viewModel.events.value.size)
        assertEquals("evt-1", viewModel.events.value[0].id)
        assertEquals(1, viewModel.days.value.size)
        assertEquals(today, viewModel.days.value[0].date)
    }

    @Test
    fun searchQueriesRepositoryAndPopulatesTags() = runBlocking {
        val sampleEvent = EventPoint(id = "evt-search", title = "Match", time = now)
        val sampleTag = HistoryTag(id = "tag-1", name = "Work", tintValue = 0xFF00FFL)

        val eventRepo = FakeEventRepository(eventsList = listOf(sampleEvent))
        val tagRepo = FakeTagRepository(tagsList = listOf(sampleTag))

        val viewModel = EventListViewModel(
            context = null,
            mode = EventSelectMode.Search,
            eventRepository = eventRepo,
            tagRepository = tagRepo,
            ioDispatcher = Dispatchers.Unconfined
        )

        viewModel.search("Match", tags = listOf("tag-1"))

        assertEquals("Match", eventRepo.lastSearchQuery)
        assertEquals(listOf("tag-1"), eventRepo.lastSearchTags)
        assertEquals(1, viewModel.events.value.size)
        assertEquals("evt-search", viewModel.events.value[0].id)
        assertEquals(1, viewModel.tags.value.size)
        assertEquals("tag-1", viewModel.tags.value[0].id)
    }

    @Test
    fun taggedModeQueriesByTagId() = runBlocking {
        val sampleEvent = EventPoint(id = "evt-tag", title = "Tagged Event", time = now)
        val eventRepo = FakeEventRepository(eventsList = listOf(sampleEvent))
        val tagRepo = FakeTagRepository()

        val viewModel = EventListViewModel(
            context = null,
            mode = EventSelectMode.Tagged,
            tagId = "tag-xyz",
            eventRepository = eventRepo,
            tagRepository = tagRepo,
            ioDispatcher = Dispatchers.Unconfined
        )

        assertEquals(listOf("tag-xyz"), eventRepo.lastTagIds)
        assertEquals(1, viewModel.events.value.size)
        assertEquals("evt-tag", viewModel.events.value[0].id)
    }

    @Test
    fun callbacksAndClearWorkAsExpected() {
        val eventRepo = FakeEventRepository()
        val tagRepo = FakeTagRepository()

        val viewModel = EventListViewModel(
            context = null,
            eventRepository = eventRepo,
            tagRepository = tagRepo,
            ioDispatcher = Dispatchers.Unconfined
        )

        var navigatedDay: LocalDate? = null
        var navigatedEventId: String? = null

        viewModel.onGotoDay { navigatedDay = it }
        viewModel.onGotoEvent { navigatedEventId = it }

        viewModel.gotoDay(today)
        viewModel.gotoEvent("evt-123")

        assertEquals(today, navigatedDay)
        assertEquals("evt-123", navigatedEventId)

        viewModel.clear()
        assertTrue(viewModel.events.value.isEmpty())
        assertTrue(viewModel.days.value.isEmpty())
    }
}
