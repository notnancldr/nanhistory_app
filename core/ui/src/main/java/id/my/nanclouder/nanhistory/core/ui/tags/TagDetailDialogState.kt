package id.my.nanclouder.nanhistory.core.ui.tags

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class TagDetailDialogState {
    private var _isOpen by mutableStateOf(false)
    private var _tagId by mutableStateOf<String?>(null)

    val isOpen get() = _isOpen
    val tagId get() = _tagId

    fun open(tagId: String) {
        _tagId = tagId
        _isOpen = true
    }

    fun close() {
        _isOpen = false
        _tagId = null
    }
}

@Composable
fun rememberTagDetailDialogState(): TagDetailDialogState =
    remember { TagDetailDialogState() }
