package id.my.nanclouder.nanhistory.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun rememberMentionModalState(): MentionModalState {
    return remember { MentionModalState() }
}

class MentionModalState {
    private val _isOpen = mutableStateOf(false)
    val isOpen: Boolean
        get() = _isOpen.value

    private val _currentMention = mutableStateOf("")
    val currentMention: String
        get() = _currentMention.value

    fun open(mention: String) {
        _currentMention.value = mention
        _isOpen.value = true
    }

    fun close() {
        _isOpen.value = false
        _currentMention.value = ""
    }
}
