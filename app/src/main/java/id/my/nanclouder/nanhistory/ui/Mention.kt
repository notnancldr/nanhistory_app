package id.my.nanclouder.nanhistory.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import id.my.nanclouder.nanhistory.state.SelectionState
import id.my.nanclouder.nanhistory.ui.main.EventListViewModel
import id.my.nanclouder.nanhistory.ui.main.EventSelectMode
import id.my.nanclouder.nanhistory.ui.main.rememberEventListViewModel
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentionModal(
    mention: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: EventListViewModel = rememberEventListViewModel(mode = EventSelectMode.Search)
    val selectionState = remember { SelectionState<HistoryEvent>() }

    val selectionMode by selectionState.isSelectionMode.collectAsState(false)
    val selectedCount = remember {
        derivedStateOf { selectionState.selectedItems.value.size }
    }

    val scope = rememberCoroutineScope()

    var isLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(mention) {
        scope.launch {
            viewModel.search("@$mention", matchWholeWord = true)
            isLoaded = true
        }
    }


    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        ),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                AnimatedContent(
                    targetState = selectionMode,
                    label = "AppBarTransition",
                    modifier = Modifier.height(96.dp)
                ) { isSelection ->
                    if (!isSelection) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(
                                            text = "Mention",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = "@$mention",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = onDismiss) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Close",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SelectionAppBar(
                                selectedItems = selectedCount.value,
                                onCancel = {
                                    selectionState.reset()
                                }
                            ) {}
                        }
                    }
                }

                if (isLoaded) {
                    EventList(
                        viewModel = viewModel,
                        selectionState = selectionState,
                        loadHeaderData = true,
                        onEmptyContent = {
                            EmptySearchState(mention = mention)
                        }
                    )
                } else {
                    LoadingSearchState()
                }
            }
        }
    }
}

@Composable
private fun LoadingSearchState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Searching...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptySearchState(mention: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .alpha(0.4f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No events found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No results for \"@$mention\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

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

@Composable
fun MentionModalHandler(
    modalState: MentionModalState,
) {
    if (modalState.isOpen) {
        MentionModal(
            mention = modalState.currentMention,
            onDismiss = { modalState.close() },
            modifier = Modifier
        )
    }
}