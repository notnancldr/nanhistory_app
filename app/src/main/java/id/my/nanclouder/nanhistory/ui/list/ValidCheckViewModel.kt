package id.my.nanclouder.nanhistory.ui.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import id.my.nanclouder.nanhistory.utils.history.validateSignature
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class EventSignatureValid {
    Checking, Valid, Invalid
}

class ValidCheckViewModel(
    context: Context,
    event: HistoryEvent,
    dispatcher: CoroutineDispatcher? = null,
) : ViewModel() {
    private val _dispatcher = dispatcher ?: Dispatchers.IO
    private var job: Job? = null

    private val _valid = MutableStateFlow(EventSignatureValid.Checking) // Holds loaded data
    val valid: StateFlow<EventSignatureValid> = _valid // Expose sorted list

    init {
        job = viewModelScope.launch(_dispatcher) {
            _valid.value = when (event.validateSignature(context = context)) {
                true -> EventSignatureValid.Valid
                else -> EventSignatureValid.Invalid
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }
}