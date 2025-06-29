package id.my.nanclouder.nanhistory.ui.main

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.my.nanclouder.nanhistory.db.AppDao
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toHistoryEvent
import id.my.nanclouder.nanhistory.lib.history.HistoryDay
import id.my.nanclouder.nanhistory.lib.history.HistoryEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class EventSelectMode {
    Default, Favorite, Search, Deleted, Tagged
}

class EventListViewModel(
    context: Context,
    private val from: LocalDate = LocalDate.MIN,
    private val until: LocalDate = LocalDate.parse("9999-12-31"),
    private val mode: EventSelectMode = EventSelectMode.Default,
    private val tagId: String? = null
) : ViewModel() {
    private val _events = MutableStateFlow<List<HistoryEvent>>(emptyList()) // Holds loaded data
    val events: StateFlow<List<HistoryEvent>> = _events // Expose sorted list

    private val _days = MutableStateFlow<List<HistoryDay>>(emptyList()) // Holds loaded data
    val days: StateFlow<List<HistoryDay>> = _days // Expose sorted list

    private val _isLoading = MutableStateFlow(false) // Loading state
    val isLoading: StateFlow<Boolean> = _isLoading // Expose as immutable

    private val db: AppDatabase = AppDatabase.getInstance(context)
    private val dao: AppDao = db.appDao()

    init {
        // Start loading files when ViewModel initializes
        load()
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }

    fun search(query: String) {
        viewModelScope.launch {
            if (query.isNotBlank()) AppDatabase.search(dao, query).collect {
                if (query.isNotBlank())
                    _events.value = it
            }
            else _events.value = emptyList()
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
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = false
            AppDatabase.getDaysInRange(dao, from, until, mode == EventSelectMode.Favorite).collect { days ->
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