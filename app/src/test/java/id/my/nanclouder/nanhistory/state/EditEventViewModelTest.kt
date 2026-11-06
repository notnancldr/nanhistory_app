package id.my.nanclouder.nanhistory.state

import id.my.nanclouder.nanhistory.domain.model.EventPoint
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import id.my.nanclouder.nanhistory.domain.usecase.SaveEventUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime

class EditEventViewModelTest {

    private class TestEventRepository(
        private val existingEvent: HistoryEvent?,
        private val siblingTimes: Pair<ZonedDateTime, ZonedDateTime>? = null
    ) : EventRepository {
        var savedEvent: HistoryEvent? = null
        var isNewFlag: Boolean? = null

        override fun getTrashedEvents(): Flow<List<HistoryEvent>> = flowOf(emptyList())
        override fun observeEventById(id: String): Flow<HistoryEvent?> = flowOf(existingEvent)
        override suspend fun restoreEvents(ids: List<String>) {}
        override suspend fun permanentlyDeleteEvents(events: List<HistoryEvent>) {}
        override suspend fun getEventById(id: String): HistoryEvent? = existingEvent
        override suspend fun moveToTrash(eventIds: List<String>) {}
        override suspend fun saveEvent(event: HistoryEvent, isNew: Boolean) {
            savedEvent = event
            isNewFlag = isNew
        }
        override suspend fun getEventSiblingTimes(previousEventOf: String?, nextEventOf: String?): Pair<ZonedDateTime, ZonedDateTime>? {
            return siblingTimes
        }
    }

    private lateinit var repo: TestEventRepository
    private lateinit var useCase: SaveEventUseCase
    private val now = ZonedDateTime.now()

    @Before
    fun setup() {
        val sampleEvent = EventPoint(id = "evt-edit", title = "Original Title", time = now)
        repo = TestEventRepository(sampleEvent, now to now.plusHours(1))
        useCase = SaveEventUseCase(repo)
    }

    @Test
    fun saveEvent_triggers_use_case_and_completion_callback() {
        val viewModel = EditEventViewModel(
            eventId = "evt-edit",
            previousEventOf = null,
            nextEventOf = null,
            eventRepository = repo,
            saveEventUseCase = useCase,
            dispatcher = Dispatchers.Unconfined
        )

        var completed = false
        val modified = EventPoint(id = "evt-edit", title = "Modified Title", time = now)

        viewModel.saveEvent(modified, isNew = false) {
            completed = true
        }

        assertEquals("Modified Title", repo.savedEvent?.title)
        assertEquals(false, repo.isNewFlag)
        assertTrue("Callback should be invoked", completed)
    }

    @Test
    fun loadSiblingTimes_delegates_to_repository() = runBlocking {
        val viewModel = EditEventViewModel(
            eventId = null,
            previousEventOf = "prev-id",
            nextEventOf = "next-id",
            eventRepository = repo,
            saveEventUseCase = useCase,
            dispatcher = Dispatchers.Unconfined
        )

        val siblings = viewModel.loadSiblingTimes()
        assertEquals(now to now.plusHours(1), siblings)
    }
}
