package id.my.nanclouder.nanhistory.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SelectionState<T> {
    private val _selectedItems = MutableStateFlow<List<T>>(emptyList())
    val selectedItems: StateFlow<List<T>> = _selectedItems

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode

    private var _onSelectionChanged: ((List<T>) -> Unit)? = null

    private fun invokeOnSelectionChanged() {
        _onSelectionChanged?.invoke(_selectedItems.value)
    }

    fun onSelectionChanged(block: (List<T>) -> Unit) {
        _onSelectionChanged = block
        invokeOnSelectionChanged()
    }

    fun select(item: T) {
        _isSelectionMode.value = true
        _selectedItems.value += item
        invokeOnSelectionChanged()
    }

    fun selectAll(items: List<T>) {
        _isSelectionMode.value = true
        _selectedItems.value += items
        invokeOnSelectionChanged()
    }

    fun deselect(item: T) {
        _selectedItems.value -= item
        invokeOnSelectionChanged()
    }

    fun deselectAll(items: List<T>) {
        _selectedItems.value -= items
        invokeOnSelectionChanged()
    }

    fun toggle(item: T) {
        if (_selectedItems.value.contains(item)) deselect(item)
        else select(item)
        invokeOnSelectionChanged()
    }

    fun clear() {
        _selectedItems.value = emptyList()
        invokeOnSelectionChanged()
    }

    fun reset() {
        _isSelectionMode.value = false
        clear()
    }
}

@Composable
fun <T> rememberSelectionState(): SelectionState<T> {
    return remember { SelectionState() }
}