package id.my.nanclouder.nanhistory.ui.main

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.my.nanclouder.nanhistory.db.AppDao
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toHistoryEvent
import id.my.nanclouder.nanhistory.db.toHistoryTag
import id.my.nanclouder.nanhistory.utils.history.HistoryDay
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import id.my.nanclouder.nanhistory.utils.history.HistoryTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class EventSelectMode {
    Default, FavoriteEvent, FavoriteDay, Search, Deleted, Tagged
}

class EventListViewModel(
    context: Context,
    val from: LocalDate = LocalDate.MIN,
    val until: LocalDate = LocalDate.parse("9999-12-31"),
    val mode: EventSelectMode = EventSelectMode.Default,
    val tagId: String? = null
) : ViewModel() {
    private val _events = MutableStateFlow<List<HistoryEvent>>(emptyList()) // Holds loaded data
    val events: StateFlow<List<HistoryEvent>> = _events // Expose sorted list

    private val _days = MutableStateFlow<List<HistoryDay>>(emptyList()) // Holds loaded data
    val days: StateFlow<List<HistoryDay>> = _days // Expose sorted list

    private val _tags = MutableStateFlow<List<HistoryTag>>(emptyList()) // Holds loaded data
    val tags: StateFlow<List<HistoryTag>> = _tags

    private val _isLoading = MutableStateFlow(false) // Loading state
    val isLoading: StateFlow<Boolean> = _isLoading // Expose as immutable

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

    private val db: AppDatabase = AppDatabase.getInstance(context)
    private val dao: AppDao = db.appDao()

    init {
        // Start loading files when ViewModel initializes
        load()
        viewModelScope.launch {
            _events.collect {
                _currentEvents = it
            }
        }
        viewModelScope.launch {
            _days.collect {
                _currentDays = it
            }
        }
        viewModelScope.launch {
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
        viewModelScope.launch(Dispatchers.IO) {
            if (tags.isNotEmpty()) AppDatabase.searchWithTagIds(dao, query, tags).collect{
                _events.value = it
            }
            else if (matchWholeWord) AppDatabase.searchMatchWholeWord(dao, query).collect {
                _events.value = it
            }
            else if (query.isNotBlank()) AppDatabase.search(dao, query).collect {
                if (query.isNotBlank())
                    _events.value = it
            }
            else _events.value = emptyList()
        }
        viewModelScope.launch(Dispatchers.IO) {
            dao.getAllTags().collect {
                _tags.value = it.map { tag -> tag.toHistoryTag() }
            }
        }
    }

    private fun load() {
        if (_isLoading.value) return
        _isLoading.value = true
        if (mode == EventSelectMode.Tagged) viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = false
            dao.getEventsByTagIds(listOf(tagId!!)).map{
                it.map { event -> event.toHistoryEvent() }
            }.collect { events ->
                _events.value = events
                Log.d("NanHistoryDebug", "Loaded ${events.size} events")
            }
        }
        else if (mode != EventSelectMode.Search) viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = false
            AppDatabase.getEventsInRange(dao, from, until, mode).collect { events ->
                _events.value = events
                Log.d("NanHistoryDebug", "Loaded ${events.size} events")
            }
        }
        else viewModelScope.launch(Dispatchers.IO) {
            search("")
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = false
            AppDatabase.getDaysInRange(dao, from, until, mode == EventSelectMode.FavoriteDay).collect { days ->
                _days.value = days
                Log.d("NanHistoryDebug", "Loaded ${days.size} days")
            }
//            _isLoading.value = false
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