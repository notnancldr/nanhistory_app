package id.my.nanclouder.nanhistory.feature.trash

import id.my.nanclouder.nanhistory.domain.model.EventPoint
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.repository.AttachmentStorageRepository
import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import id.my.nanclouder.nanhistory.domain.usecase.PermanentlyDeleteEventsUseCase
import id.my.nanclouder.nanhistory.domain.usecase.RestoreEventsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime

class TrashViewModelTest {

    private class TestEventRepository : EventRepository {
        val restored = mutableListOf<String>()
        val deleted = mutableListOf<HistoryEvent>()

        override fun getTrashedEvents(): Flow<List<HistoryEvent>> = flowOf(emptyList())
        override suspend fun restoreEvents(ids: List<String>) { restored.addAll(ids) }
        override suspend fun permanentlyDeleteEvents(events: List<HistoryEvent>) { deleted.addAll(events) }
        override suspend fun getEventById(id: String): HistoryEvent? = null
        override suspend fun moveToTrash(eventIds: List<String>) {}
    }

    private class TestAttachmentRepository : AttachmentStorageRepository {
        val deletedAttachments = mutableListOf<HistoryEvent>()
        override suspend fun deleteAttachments(event: HistoryEvent) {
            deletedAttachments.add(event)
        }
    }

    private lateinit var eventRepo: TestEventRepository
    private lateinit var attachmentRepo: TestAttachmentRepository
    private lateinit var viewModel: TrashViewModel

    @Before
    fun setup() {
        eventRepo = TestEventRepository()
        attachmentRepo = TestAttachmentRepository()
        val restoreUseCase = RestoreEventsUseCase(eventRepo)
        val deleteUseCase = PermanentlyDeleteEventsUseCase(eventRepo, attachmentRepo)
        viewModel = TrashViewModel(
            restoreEventsUseCase = restoreUseCase,
            permanentlyDeleteEventsUseCase = deleteUseCase,
            dispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun restore_updates_progress_and_calls_completion_callback() {
        val event = EventPoint(id = "evt-1", title = "Test", time = ZonedDateTime.now())
        var completed = false

        viewModel.restore(listOf(event)) {
            completed = true
        }

        assertEquals(listOf("evt-1"), eventRepo.restored)
        assertTrue("onComplete should be invoked", completed)
        assertFalse("Progress should finish", viewModel.restoreProgress.value.isRunning)
    }

    @Test
    fun deletePermanently_cleans_files_deletes_events_and_reports_progress() {
        val event1 = EventPoint(id = "evt-1", title = "1", time = ZonedDateTime.now())
        val event2 = EventPoint(id = "evt-2", title = "2", time = ZonedDateTime.now())
        var completed = false

        viewModel.deletePermanently(listOf(event1, event2)) {
            completed = true
        }

        assertEquals(listOf(event1, event2), attachmentRepo.deletedAttachments)
        assertEquals(listOf(event1, event2), eventRepo.deleted)
        assertTrue("onComplete should be invoked", completed)
        assertFalse("Delete progress should be stopped", viewModel.deleteProgress.value.isRunning)
    }
}
