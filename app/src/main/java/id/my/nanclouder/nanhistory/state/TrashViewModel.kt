package id.my.nanclouder.nanhistory.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.my.nanclouder.nanhistory.di.AppContainer
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.usecase.PermanentlyDeleteEventsUseCase
import id.my.nanclouder.nanhistory.domain.usecase.RestoreEventsUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrashProgressState(
    val isRunning: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val text: String = "",
    val progress: Float? = null
)

/**
 * ViewModel managing TrashActivity operations (restoring and permanently deleting events).
 */
class TrashViewModel(
    private val restoreEventsUseCase: RestoreEventsUseCase,
    private val permanentlyDeleteEventsUseCase: PermanentlyDeleteEventsUseCase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val _deleteProgress = MutableStateFlow(TrashProgressState())
    val deleteProgress: StateFlow<TrashProgressState> = _deleteProgress.asStateFlow()

    private val _restoreProgress = MutableStateFlow(TrashProgressState())
    val restoreProgress: StateFlow<TrashProgressState> = _restoreProgress.asStateFlow()

    fun restore(
        events: List<HistoryEvent>,
        onComplete: () -> Unit = {}
    ) {
        if (events.isEmpty()) return
        viewModelScope.launch(dispatcher) {
            _restoreProgress.value = TrashProgressState(
                isRunning = true,
                text = "Restoring events..."
            )
            try {
                restoreEventsUseCase(events.map { it.id })
            } finally {
                _restoreProgress.value = TrashProgressState(isRunning = false)
                onComplete()
            }
        }
    }

    fun deletePermanently(
        events: List<HistoryEvent>,
        onComplete: () -> Unit = {}
    ) {
        if (events.isEmpty()) return
        viewModelScope.launch(dispatcher) {
            _deleteProgress.value = TrashProgressState(
                isRunning = true,
                total = events.size,
                text = "Deleting items: 0 of ${events.size}",
                progress = 0f
            )
            try {
                permanentlyDeleteEventsUseCase(events) { current, total ->
                    _deleteProgress.value = TrashProgressState(
                        isRunning = true,
                        current = current,
                        total = total,
                        text = "Deleting items: $current of $total",
                        progress = if (total > 0) current.toFloat() / total else null
                    )
                }
            } finally {
                _deleteProgress.value = TrashProgressState(isRunning = false)
                onComplete()
            }
        }
    }

    class Factory(
        private val container: AppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TrashViewModel(
                restoreEventsUseCase = container.restoreEventsUseCase,
                permanentlyDeleteEventsUseCase = container.permanentlyDeleteEventsUseCase
            ) as T
        }
    }
}
