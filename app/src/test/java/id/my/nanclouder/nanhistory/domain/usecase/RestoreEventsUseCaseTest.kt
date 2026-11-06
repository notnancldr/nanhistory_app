package id.my.nanclouder.nanhistory.domain.usecase

import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RestoreEventsUseCaseTest {

    private class FakeEventRepository : EventRepository {
        val restoredIds = mutableListOf<String>()

        override fun getTrashedEvents(): Flow<List<HistoryEvent>> = flowOf(emptyList())

        override suspend fun restoreEvents(ids: List<String>) {
            restoredIds.addAll(ids)
        }

        override suspend fun permanentlyDeleteEvents(events: List<HistoryEvent>) {}

        override suspend fun getEventById(id: String): HistoryEvent? = null
    }

    @Test
    fun restoreEvents_delegates_non_empty_ids_to_repository() = runBlocking {
        val repo = FakeEventRepository()
        val useCase = RestoreEventsUseCase(repo)

        useCase(listOf("evt-1", "evt-2"))

        assertEquals(listOf("evt-1", "evt-2"), repo.restoredIds)
    }

    @Test
    fun restoreEvents_ignores_empty_list() = runBlocking {
        val repo = FakeEventRepository()
        val useCase = RestoreEventsUseCase(repo)

        useCase(emptyList())

        assertEquals(0, repo.restoredIds.size)
    }
}
