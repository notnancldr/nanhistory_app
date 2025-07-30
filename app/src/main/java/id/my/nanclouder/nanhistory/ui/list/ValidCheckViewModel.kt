package id.my.nanclouder.nanhistory.ui.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.my.nanclouder.nanhistory.lib.history.HistoryEvent
import id.my.nanclouder.nanhistory.lib.history.validateSignature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class EventSignatureValid {
    Checking, Valid, Invalid
}

class ValidCheckViewModel(
    context: Context,
    event: HistoryEvent
) : ViewModel() {
    private val _valid = MutableStateFlow(EventSignatureValid.Checking) // Holds loaded data
    val valid: StateFlow<EventSignatureValid> = _valid // Expose sorted list

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _valid.value = when (event.validateSignature(context = context)) {
                true -> EventSignatureValid.Valid
                else -> EventSignatureValid.Invalid
            }
        }
    }
}