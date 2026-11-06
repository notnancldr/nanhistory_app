package id.my.nanclouder.nanhistory.domain.usecase

import id.my.nanclouder.nanhistory.domain.model.EventPoint
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.repository.AttachmentStorageRepository
import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZonedDateTime

class PermanentlyDeleteEventsUseCaseTest {

    private class FakeEventRepository : EventRepository {
        val deletedEvents = mutableListOf<HistoryEvent>()

        override fun getTrashedEvents(): Flow<List<HistoryEvent>> = flowOf(emptyList())
        override suspend fun restoreEvents(ids: List<String>) {}
        override suspend fun permanentlyDeleteEvents(events: List<HistoryEvent>) {
            deletedEvents.addAll(events)
        }
        override suspend fun getEventById(id: String): HistoryEvent? = null
        override suspend fun moveToTrash(eventIds: List<String>) {}
    }

    private class FakeAttachmentStorageRepository : AttachmentStorageRepository {
        val cleanedEvents = mutableListOf<HistoryEvent>()

        override suspend fun deleteAttachments(event: HistoryEvent) {
            cleanedEvents.add(event)
        }
    }

    @Test
    fun permanentlyDeleteEvents_cleans_attachments_and_deletes_records() = runBlocking {
        val eventRepo = FakeEventRepository()
        val attachmentRepo = FakeAttachmentStorageRepository()
        val useCase = PermanentlyDeleteEventsUseCase(eventRepo, attachmentRepo)

        val event1 = EventPoint(id = "evt-1", title = "1", time = ZonedDateTime.now())
        val event2 = EventPoint(id = "evt-2", title = "2", time = ZonedDateTime.now())

        val progressUpdates = mutableListOf<Pair<Int, Int>>()
        useCase(listOf(event1, event2)) { current, total ->
            progressUpdates.add(current to total)
        }

        assertEquals(listOf(event1, event2), attachmentRepo.cleanedEvents)
        assertEquals(listOf(event1, event2), eventRepo.deletedEvents)
        assertEquals(listOf(1 to 2, 2 to 2), progressUpdates)
    }

    @Test
    fun permanentlyDeleteEvents_ignores_empty_list() = runBlocking {
        val eventRepo = FakeEventRepository()
        val attachmentRepo = FakeAttachmentStorageRepository()
        val useCase = PermanentlyDeleteEventsUseCase(eventRepo, attachmentRepo)

        useCase(emptyList())

        assertEquals(0, attachmentRepo.cleanedEvents.size)
        assertEquals(0, eventRepo.deletedEvents.size)
    }
}
