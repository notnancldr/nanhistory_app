package id.my.nanclouder.nanhistory.ui.main

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.my.nanclouder.nanhistory.NanHistoryApplication
import id.my.nanclouder.nanhistory.domain.model.EventSelectMode
import id.my.nanclouder.nanhistory.domain.model.HistoryDay
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.model.HistoryTag
import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import id.my.nanclouder.nanhistory.domain.repository.TagRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

typealias EventSelectMode = id.my.nanclouder.nanhistory.domain.model.EventSelectMode

class EventListViewModel(
    context: Context? = null,
    val from: LocalDate = LocalDate.MIN,
    val until: LocalDate = LocalDate.parse("9999-12-31"),
    val mode: EventSelectMode = EventSelectMode.Default,
    val tagId: String? = null,
    eventRepository: EventRepository? = null,
    tagRepository: TagRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val eventRepository: EventRepository = eventRepository
        ?: (context?.applicationContext as? NanHistoryApplication)?.container?.eventRepository
        ?: error("EventRepository must be provided or context must be NanHistoryApplication")

    private val tagRepository: TagRepository = tagRepository
        ?: (context?.applicationContext as? NanHistoryApplication)?.container?.tagRepository
        ?: error("TagRepository must be provided or context must be NanHistoryApplication")

    private val _events = MutableStateFlow<List<HistoryEvent>>(emptyList())
    val events: StateFlow<List<HistoryEvent>> = _events

    private val _days = MutableStateFlow<List<HistoryDay>>(emptyList())
    val days: StateFlow<List<HistoryDay>> = _days

    private val _tags = MutableStateFlow<List<HistoryTag>>(emptyList())
    val tags: StateFlow<List<HistoryTag>> = _tags

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var _currentEvents = emptyList<HistoryEvent>()
    val currentEvents
        get() = _currentEvents

    private var _currentDays = emptyList<HistoryDay>()
    val currentDays
        get() = _currentDays

    private var _currentTags = emptyList<HistoryTag>()
    val currentTags
        get() = _currentTags

    private var _onGotoDay: ((LocalDate) -> Unit)? = null
    private var _onGotoEvent: ((String) -> Unit)? = null

    init {
        load()
        viewModelScope.launch(ioDispatcher) {
            _events.collect {
                _currentEvents = it
            }
        }
        viewModelScope.launch(ioDispatcher) {
            _days.collect {
                _currentDays = it
            }
        }
        viewModelScope.launch(ioDispatcher) {
            _tags.collect {
                _currentTags = it
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }

    fun onGotoDay(block: (LocalDate) -> Unit) { _onGotoDay = block }

    fun gotoDay(date: LocalDate) {
        _onGotoDay?.invoke(date)
    }

    fun onGotoEvent(block: (String) -> Unit) { _onGotoEvent = block }

    fun gotoEvent(eventId: String) {
        _onGotoEvent?.invoke(eventId)
    }

    fun search(query: String, tags: List<String> = emptyList(), matchWholeWord: Boolean = false) {
        viewModelScope.launch(ioDispatcher) {
            eventRepository.searchEvents(query, tags, matchWholeWord).collect {
                _events.value = it
            }
        }
        viewModelScope.launch(ioDispatcher) {
            tagRepository.observeAllTags().collect {
                _tags.value = it
            }
        }
    }

    private fun load() {
        if (_isLoading.value) return
        _isLoading.value = true
        if (mode == EventSelectMode.Tagged) {
            viewModelScope.launch(ioDispatcher) {
                _isLoading.value = false
                val id = tagId ?: return@launch
                eventRepository.observeEventsByTagIds(listOf(id)).collect { events ->
                    _events.value = events
                    Log.d("NanHistoryDebug", "Loaded ${events.size} events")
                }
            }
        } else if (mode != EventSelectMode.Search) {
            viewModelScope.launch(ioDispatcher) {
                _isLoading.value = false
                eventRepository.observeEventsInRange(from, until, mode).collect { events ->
                    _events.value = events
                    Log.d("NanHistoryDebug", "Loaded ${events.size} events")
                }
            }
        } else {
            viewModelScope.launch(ioDispatcher) {
                search("")
            }
        }
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = false
            eventRepository.observeDaysInRange(from, until, mode == EventSelectMode.FavoriteDay).collect { days ->
                _days.value = days
                Log.d("NanHistoryDebug", "Loaded ${days.size} days")
            }
        }
    }

    fun cancelLoading() {
        viewModelScope.cancel()
        _isLoading.value = false
    }

    fun clear() {
        _events.value = emptyList()
        _days.value = emptyList()
        _isLoading.value = false
    }

    fun reload() {
        if (_isLoading.value) return
        _events.value = emptyList()
        _days.value = emptyList()
        load()
    }
}

@Composable
fun rememberEventListViewModel(
    from: LocalDate = LocalDate.MIN,
    until: LocalDate = LocalDate.parse("9999-12-31"),
    mode: EventSelectMode = EventSelectMode.Default
): EventListViewModel {
    val context = LocalContext.current
    return remember { EventListViewModel(context, from, until, mode) }
}
