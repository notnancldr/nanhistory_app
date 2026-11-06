package id.my.nanclouder.nanhistory.domain.usecase

import id.my.nanclouder.nanhistory.domain.model.EventPoint
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class SaveEventUseCaseTest {

    private class TestEventRepository : EventRepository {
        var savedEvent: HistoryEvent? = null
        var isNewFlag: Boolean? = null

        override fun getTrashedEvents(): Flow<List<HistoryEvent>> = flowOf(emptyList())
        override fun observeEventById(id: String): Flow<HistoryEvent?> = flowOf(null)
        override suspend fun restoreEvents(ids: List<String>) {}
        override suspend fun permanentlyDeleteEvents(events: List<HistoryEvent>) {}
        override suspend fun getEventById(id: String): HistoryEvent? = null
        override suspend fun moveToTrash(eventIds: List<String>) {}
        override suspend fun saveEvent(event: HistoryEvent, isNew: Boolean) {
            savedEvent = event
            isNewFlag = isNew
        }
        override suspend fun getEventSiblingTimes(previousEventOf: String?, nextEventOf: String?): Pair<ZonedDateTime, ZonedDateTime>? = null
    }

    @Test
    fun saveEvent_delegates_to_repository() = runBlocking {
        val repo = TestEventRepository()
        val useCase = SaveEventUseCase(repo)
        val event = EventPoint(id = "e1", title = "Saved Event", time = ZonedDateTime.now())

        useCase(event, isNew = true)

        assertEquals("e1", repo.savedEvent?.id)
        assertTrue(repo.isNewFlag == true)
    }
}
