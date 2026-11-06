package id.my.nanclouder.nanhistory.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.my.nanclouder.nanhistory.di.AppContainer
import id.my.nanclouder.nanhistory.domain.model.HistoryTag
import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import id.my.nanclouder.nanhistory.domain.repository.TagRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing tag details and actions in TagDetailActivity.
 */
class TagDetailViewModel(
    tagId: String,
    private val tagRepository: TagRepository,
    private val eventRepository: EventRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    val tag: StateFlow<HistoryTag?> = tagRepository.observeTagById(tagId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun moveToTrash(
        eventIds: List<String>,
        onComplete: () -> Unit = {}
    ) {
        if (eventIds.isEmpty()) return
        viewModelScope.launch(dispatcher) {
            try {
                eventRepository.moveToTrash(eventIds)
            } finally {
                onComplete()
            }
        }
    }

    class Factory(
        private val tagId: String,
        private val container: AppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TagDetailViewModel(
                tagId = tagId,
                tagRepository = container.tagRepository,
                eventRepository = container.eventRepository
            ) as T
        }
    }
}
