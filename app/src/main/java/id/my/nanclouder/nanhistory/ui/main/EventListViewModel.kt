package id.my.nanclouder.nanhistory.ui.main

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.my.nanclouder.nanhistory.lib.history.HistoryFileData
import id.my.nanclouder.nanhistory.lib.history.get
import id.my.nanclouder.nanhistory.lib.history.getDateFromFilePath
import id.my.nanclouder.nanhistory.lib.history.getListStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class EventListViewModel(
    context: Context,
    private val from: LocalDate = LocalDate.MIN,
    private val until: LocalDate = LocalDate.MAX,
    private val favorite: Boolean = false,
    private val searchMode: Boolean = false
) : ViewModel() {
    private val _list = MutableStateFlow<List<HistoryFileData>>(emptyList()) // Holds loaded data
    val list: StateFlow<List<HistoryFileData>> = _list // Expose sorted list
    private val _isLoading = MutableStateFlow(false) // Loading state
    val isLoading: StateFlow<Boolean> = _isLoading // Expose as immutable

    private var _searchQuery = MutableStateFlow("")
    var _expandSetter by mutableStateOf({ _: Boolean -> })

//    val list: StateFlow<List<HistoryFileData>> =
//        combine(_list, _searchQuery) { historyList, query ->
//            if (!searchMode) historyList
//            else if (query.isBlank()) emptyList() // Return empty list if no search query
//            else historyList.map { day ->
//                day.apply {
//                    events.removeIf { event -> !(
//                        event.title.contains(query, ignoreCase = true) ||
//                        event.description.contains(query, ignoreCase = true) ||
//                        event.time.format(
//                            DateTimeFormatter.ofLocalizedDateTime(
//                                FormatStyle.FULL
//                            )
//                        )
//                        .contains(query, ignoreCase = true)
//                    )}
//                }
//                day
//            }.filter { it.events.isNotEmpty() }
//        }
//        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList()) // Keeps state and updates UI

    init {
        if (!searchMode) load(context) // Start loading files when ViewModel initializes
    }

    fun search(context: Context, query: String) {
        _searchQuery.value = query
        reload(context)
    }

    fun expandStateChange(setter: (Boolean) -> Unit) {
        _expandSetter = setter
    }

    private fun MutableList<HistoryFileData>.filterItem(day: HistoryFileData) {
        if (day.events.isNotEmpty()) {
            if (searchMode) {
                Log.d("NanHistoryDebug", "SEARCH MODE")
                val query = _searchQuery.value
                if (_searchQuery.value.isNotBlank()) day.let {
                    it.events.removeAll { event ->
                        !(
                            event.title.contains(query, ignoreCase = true) ||
                            event.description.contains(
                                query,
                                ignoreCase = true
                            ) ||
                            event.time.format(
                                DateTimeFormatter.ofLocalizedDateTime(
                                    FormatStyle.FULL
                                )
                            )
                            .contains(query, ignoreCase = true)
                        )
                    }
                    if (it.events.isNotEmpty()) add(it)
                    //            }.filter { it.events.isNotEmpty() }
                }
            }
            else if (!favorite) { add(day); Log.d("NanHistoryDebug", "NOT FAVORITE") }
            else if (day.favorite) { add(day); Log.d("NanHistoryDebug", "IS FAVORITE") }
            else if (day.events.find { event -> event.favorite } != null)
                add(day.apply {
                    events.removeAll { event -> !event.favorite }
                })
        }
    }

    private fun load(context: Context) {
        if (_isLoading.value) return
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            HistoryFileData
                .getListStream(context, from = from, until = until).apply {
                    sortedByDescending { getDateFromFilePath(it.absolutePath) }
                }.forEachAsync {
                    _list.value = _list.value.toMutableList().apply {
                        filterItem(it)
                    }
//                    _list.sortBy { data -> data.date }
                }
            _isLoading.value = false
        }
    }

    fun collapseAll() =_expandSetter.invoke(false)
    fun expandAll() = _expandSetter.invoke(true)
    fun cancelLoading() {
        viewModelScope.cancel()
        _isLoading.value = false
    }

    fun reload(context: Context) {
        if (_isLoading.value) return
        _list.value = emptyList()
        load(context)
    }

    fun update(context: Context, date: LocalDate) {
        val fileData = HistoryFileData.get(context, date)
        val tempList = _list.value.toMutableList()

        tempList.removeIf { date == it.date }
//        if (fileData != null && fileData.events.isNotEmpty())
//            tempList.add(fileData)
        if (fileData != null) tempList.filterItem(fileData)
        _list.value = tempList.toList().sortedByDescending { it.date }
    }
}