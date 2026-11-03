package id.my.nanclouder.nanhistory.ui.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import id.my.nanclouder.nanhistory.utils.signature.validateSignature
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

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
    private var cancelled = false

    private val _valid = MutableStateFlow(EventSignatureValid.Checking) // Holds loaded data
    val valid: StateFlow<EventSignatureValid> = _valid // Expose sorted list

    init {
        job = viewModelScope.launch(_dispatcher) {
            delay(1000.milliseconds)
            if (cancelled) {
                return@launch
            }
            _valid.value = when (event.validateSignature(context = context)) {
                true -> EventSignatureValid.Valid
                else -> EventSignatureValid.Invalid
            }
        }
    }

    fun cancel() {
        cancelled = true
        job?.cancel()
    }
}