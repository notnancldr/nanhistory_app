package id.my.nanclouder.nanhistory.feature.eventdetail.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.my.nanclouder.nanhistory.core.common.di.EventDetailContainer
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import id.my.nanclouder.nanhistory.domain.usecase.SaveEventUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * ViewModel managing event creation and editing in EditEventActivity.
 */
class EditEventViewModel(
    val eventId: String?,
    private val previousEventOf: String?,
    private val nextEventOf: String?,
    private val eventRepository: EventRepository,
    private val saveEventUseCase: SaveEventUseCase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    val event: StateFlow<HistoryEvent?> = if (eventId != null) {
        eventRepository.observeEventById(eventId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    } else {
        MutableStateFlow(null)
    }

    suspend fun loadSiblingTimes(): Pair<ZonedDateTime, ZonedDateTime>? {
        return eventRepository.getEventSiblingTimes(previousEventOf, nextEventOf)
    }

    fun saveEvent(
        event: HistoryEvent,
        isNew: Boolean,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch(dispatcher) {
            try {
                saveEventUseCase(event, isNew)
            } finally {
                onComplete()
            }
        }
    }

    class Factory(
        private val eventId: String?,
        private val previousEventOf: String?,
        private val nextEventOf: String?,
        private val container: EventDetailContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditEventViewModel(
                eventId = eventId,
                previousEventOf = previousEventOf,
                nextEventOf = nextEventOf,
                eventRepository = container.eventRepository,
                saveEventUseCase = container.saveEventUseCase
            ) as T
        }
    }
}
