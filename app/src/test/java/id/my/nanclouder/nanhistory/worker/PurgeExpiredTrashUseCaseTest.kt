package id.my.nanclouder.nanhistory.worker

import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.model.EventPoint
import id.my.nanclouder.nanhistory.domain.repository.AttachmentStorageRepository
import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import id.my.nanclouder.nanhistory.domain.usecase.PurgeExpiredTrashUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZonedDateTime

class PurgeExpiredTrashUseCaseTest {

    private class FakeEventRepository : EventRepository {
        val deletedEvents = mutableListOf<HistoryEvent>()
        var expiredToReturn: List<HistoryEvent> = emptyList()

        override fun getTrashedEvents(): Flow<List<HistoryEvent>> = flowOf(emptyList())
        override suspend fun restoreEvents(ids: List<String>) {}
        override suspend fun permanentlyDeleteEvents(events: List<HistoryEvent>) {
            deletedEvents.addAll(events)
        }
        override suspend fun getEventById(id: String): HistoryEvent? = null
        override suspend fun moveToTrash(eventIds: List<String>) {}
        override suspend fun getExpiredTrashEvents(nowEpochMilli: Long): List<HistoryEvent> = expiredToReturn
        override suspend fun getEventsAfter(lastEventTimeEpochMilli: Long): List<HistoryEvent> = emptyList()
    }

    private class FakeAttachmentRepository : AttachmentStorageRepository {
        val deletedForEvents = mutableListOf<HistoryEvent>()

        override suspend fun deleteAttachments(event: HistoryEvent) {
            deletedForEvents.add(event)
        }
        override suspend fun deleteAttachments(events: List<HistoryEvent>) {
            deletedForEvents.addAll(events)
        }
    }

    @Test
    fun `purges expired events by deleting attachments and removing records`() = runBlocking {
        val eventRepo = FakeEventRepository()
        val attachRepo = FakeAttachmentRepository()
        val useCase = PurgeExpiredTrashUseCase(eventRepo, attachRepo)

        val expiredEvent = EventPoint(
            id = "expired-1",
            title = "Old Trash",
            time = ZonedDateTime.now()
        )
        eventRepo.expiredToReturn = listOf(expiredEvent)

        val purged = useCase(nowEpochMilli = 100_000L)

        assertEquals(1, purged.size)
        assertEquals("expired-1", purged.first().id)
        assertEquals(1, attachRepo.deletedForEvents.size)
        assertEquals("expired-1", attachRepo.deletedForEvents.first().id)
        assertEquals(1, eventRepo.deletedEvents.size)
        assertEquals("expired-1", eventRepo.deletedEvents.first().id)
    }

    @Test
    fun `does not invoke deletion if no events are expired`() = runBlocking {
        val eventRepo = FakeEventRepository()
        val attachRepo = FakeAttachmentRepository()
        val useCase = PurgeExpiredTrashUseCase(eventRepo, attachRepo)
        eventRepo.expiredToReturn = emptyList()

        val purged = useCase(nowEpochMilli = 100_000L)

        assertEquals(0, purged.size)
        assertEquals(0, attachRepo.deletedForEvents.size)
        assertEquals(0, eventRepo.deletedEvents.size)
    }
}
